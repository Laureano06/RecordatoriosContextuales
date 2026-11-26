package com.laureano.recordatorios.data

import com.laureano.recordatorios.data.local.EstadoSyncLocal
import com.laureano.recordatorios.data.local.RecordatorioDao
import com.laureano.recordatorios.data.local.RecordatorioEntity
import com.laureano.recordatorios.data.remote.FalloRemoto
import com.laureano.recordatorios.data.remote.FuenteRemota
import com.laureano.recordatorios.data.remote.RecordatorioRemoto
import com.laureano.recordatorios.data.remote.ResultadoRemoto
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.Categorias
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.domain.ordenar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

// Local-first: toda operación se guarda primero en Room y después se intenta enviar a Firestore.
// Todo queda asociado al uid de la cuenta activa
@OptIn(ExperimentalCoroutinesApi::class)
class RecordatorioRepositoryLocalFirst(
    private val dao: RecordatorioDao,
    private val remota: FuenteRemota,
    // uid de la sesión activa, o null si no hay sesión
    private val uidActivo: StateFlow<String?>,
    private val preferencias: Flow<Preferencias>,
    private val alcance: CoroutineScope,
    private val sincronizarAlGuardar: Boolean = true,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val generarUuid: () -> String = { UUID.randomUUID().toString() }
) : RecordatorioRepository {

    override val categorias: List<Categoria> = Categorias.todas

    private val _estadoSincronizacion =
        MutableStateFlow<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
    override val estadoSincronizacion: StateFlow<EstadoSincronizacion> =
        _estadoSincronizacion.asStateFlow()

    // Solo una sincronización a la vez, aunque varios eventos la pidan juntos
    private val candado = Mutex()

    private fun uidRequerido(): String = uidActivo.value ?: throw SinSesionException()

    // Cada vez que cambia la cuenta, las consultas se rehacen con el nuevo uid
    private fun <T> porCuenta(sinSesion: T, consulta: (String) -> Flow<T>): Flow<T> =
        uidActivo.flatMapLatest { uid -> if (uid == null) flowOf(sinSesion) else consulta(uid) }

    override fun observarListado(): Flow<List<Recordatorio>> =
        porCuenta(emptyList<RecordatorioEntity>()) { uid ->
            preferencias
                .map { it.mostrarCompletados }
                .distinctUntilChanged()
                .flatMapLatest { mostrar ->
                    if (mostrar) {
                        dao.observarTodos(uid)
                    } else {
                        dao.observarExcluyendoEstado(uid, ESTADO_COMPLETADO)
                    }
                }
        }.combine(preferencias) { entidades, prefs ->
            entidades.map { it.aDominio() }.ordenar(prefs.orden)
        }

    override fun observarRecordatorio(id: Long): Flow<Recordatorio?> =
        porCuenta(null) { uid -> dao.observarPorId(id, uid).map { it?.aDominio() } }

    override fun observarCantidadPendientes(): Flow<Int> =
        porCuenta(0) { uid -> dao.observarCantidadPendientes(uid) }

    override suspend fun crear(datos: DatosRecordatorio): Long {
        val uid = uidRequerido()
        val id = dao.insertar(
            RecordatorioEntity(
                uid = uid,
                uuid = generarUuid(),
                titulo = datos.titulo,
                descripcion = datos.descripcion,
                categoriaId = datos.categoria.id,
                prioridad = datos.prioridad.codigo(),
                estado = ESTADO_PENDIENTE,
                actualizadoEn = reloj(),
                estadoSync = EstadoSyncLocal.PENDIENTE_CREAR
            )
        )
        sincronizarEnSegundoPlano()
        return id
    }

    // buscarPorId filtra por uid: aunque la pantalla pida un id ajeno, no se encuentra
    private suspend fun propio(id: Long): RecordatorioEntity? = dao.buscarPorId(id, uidRequerido())

    override suspend fun actualizar(id: Long, datos: DatosRecordatorio) {
        val actual = propio(id) ?: return
        dao.actualizar(
            actual.copy(
                titulo = datos.titulo,
                descripcion = datos.descripcion,
                categoriaId = datos.categoria.id,
                prioridad = datos.prioridad.codigo(),
                actualizadoEn = reloj(),
                estadoSync = estadoTrasModificar(actual)
            )
        )
        sincronizarEnSegundoPlano()
    }

    override suspend fun cambiarEstado(id: Long, estado: EstadoRecordatorio) {
        val actual = propio(id) ?: return
        val columnas = estado.aColumnas()
        dao.actualizar(
            actual.copy(
                estado = columnas.estado,
                fechaEstado = columnas.fecha,
                motivoCancelacion = columnas.motivo,
                actualizadoEn = reloj(),
                estadoSync = estadoTrasModificar(actual)
            )
        )
        sincronizarEnSegundoPlano()
    }

    override suspend fun eliminar(id: Long) {
        val actual = propio(id) ?: return
        // Se oculta enseguida; el borrado definitivo ocurre al sincronizar
        dao.actualizar(
            actual.copy(actualizadoEn = reloj(), estadoSync = EstadoSyncLocal.PENDIENTE_ELIMINAR)
        )
        sincronizarEnSegundoPlano()
    }

    // Si nunca llegó a la nube, sigue siendo una creación pendiente
    private fun estadoTrasModificar(actual: RecordatorioEntity): String =
        if (actual.estadoSync == EstadoSyncLocal.PENDIENTE_CREAR) {
            EstadoSyncLocal.PENDIENTE_CREAR
        } else {
            EstadoSyncLocal.PENDIENTE_ACTUALIZAR
        }

    private fun sincronizarEnSegundoPlano() {
        if (sincronizarAlGuardar) alcance.launch { sincronizar() }
    }

    override suspend fun sincronizar(): EstadoSincronizacion {
        val uid = uidActivo.value ?: return EstadoSincronizacion.Inactiva
        // Si ya hay una en curso, no se inicia otra: así nada se envía dos veces
        if (!candado.tryLock()) return _estadoSincronizacion.value
        try {
            _estadoSincronizacion.value = EstadoSincronizacion.EnCurso
            val falloSubida = subirPendientes(uid)
            val falloDescarga =
                if (falloSubida?.reintentable == true) falloSubida else descargar(uid)
            val fallo = falloDescarga ?: falloSubida
            val ahora = reloj().aFecha()
            val resultado = if (fallo == null) {
                EstadoSincronizacion.Completada(ahora)
            } else {
                EstadoSincronizacion.Fallida(fallo, ahora)
            }
            _estadoSincronizacion.value = resultado
            return resultado
        } finally {
            candado.unlock()
        }
    }

    // Devuelve el primer fallo encontrado, o null si todo se envió
    private suspend fun subirPendientes(uid: String): FalloRemoto? {
        var ultimoFallo: FalloRemoto? = null
        for (pendiente in dao.pendientes(uid)) {
            val fallo = enviar(pendiente) ?: continue
            // Sin red o con el servicio caído no tiene sentido seguir con los demás
            if (fallo.reintentable) {
                dao.sumarIntento(pendiente.id)
                return fallo
            }
            dao.registrarError(pendiente.id, fallo.mensaje)
            ultimoFallo = fallo
        }
        return ultimoFallo
    }

    private suspend fun enviar(entidad: RecordatorioEntity): FalloRemoto? {
        val resultado = if (entidad.estadoSync == EstadoSyncLocal.PENDIENTE_ELIMINAR) {
            remota.eliminar(entidad.uid, entidad.uuid)
        } else {
            // Crear y modificar escriben el documento con id = uuid: un reintento no duplica
            remota.guardar(entidad.aRemoto())
        }
        return when (resultado) {
            is ResultadoRemoto.Fallo -> resultado.fallo
            is ResultadoRemoto.Exito -> {
                if (entidad.estadoSync == EstadoSyncLocal.PENDIENTE_ELIMINAR) {
                    dao.eliminarPorId(entidad.id)
                } else {
                    // Se actualiza el mismo registro: nunca se inserta uno nuevo
                    dao.confirmarSincronizacion(entidad.id, entidad.actualizadoEn)
                }
                null
            }
        }
    }

    private suspend fun descargar(uid: String): FalloRemoto? =
        when (val r = remota.listar(uid)) {
            is ResultadoRemoto.Fallo -> r.fallo
            is ResultadoRemoto.Exito -> {
                // Los documentos de otra cuenta o con valores desconocidos se descartan
                val remotos = r.datos.mapNotNull { it.aEntityOrNull(uid) }
                for (remoto in remotos) {
                    val local = dao.buscarPorUuid(remoto.uuid)
                    when {
                        local == null -> dao.insertar(remoto)
                        local.uid != uid -> Unit
                        // Los cambios locales pendientes tienen prioridad sobre la nube
                        local.estadoSync == EstadoSyncLocal.SINCRONIZADO ->
                            dao.actualizar(remoto.copy(id = local.id))
                    }
                }
                // Lo que se borró en la nube (por ejemplo, desde otro dispositivo) también se borra acá
                val uuidsRemotos = remotos.map { it.uuid }.toSet()
                dao.sincronizados(uid)
                    .filter { it.uuid !in uuidsRemotos }
                    .forEach { dao.eliminarPorId(it.id) }
                null
            }
        }

    override suspend fun prepararCierreDeSesion() {
        val uid = uidActivo.value ?: return
        // Intenta enviar lo pendiente antes de irse; lo que no se pueda enviar queda guardado
        sincronizar()
        dao.eliminarSincronizados(uid)
        _estadoSincronizacion.value = EstadoSincronizacion.Inactiva
    }

    override suspend fun probarAccesoAjeno(uidAjeno: String): ResultadoPruebaAcceso {
        uidRequerido()
        val lectura = when (val r = remota.listar(uidAjeno)) {
            is ResultadoRemoto.Exito -> "Lectura permitida: ${r.datos.size} documentos"
            is ResultadoRemoto.Fallo -> "Lectura rechazada: ${r.fallo.mensaje}"
        }
        val intruso = RecordatorioRemoto(
            uid = uidAjeno,
            uuid = generarUuid(),
            titulo = "Intento de escritura ajena",
            descripcion = null,
            categoriaId = Categorias.trabajo.id,
            prioridad = "ALTA",
            estado = ESTADO_PENDIENTE,
            fechaEstado = null,
            motivoCancelacion = null,
            fechaAviso = null,
            ubicacion = null,
            actualizadoEn = reloj()
        )
        val escritura = when (val r = remota.guardar(intruso)) {
            is ResultadoRemoto.Exito -> "Escritura permitida"
            is ResultadoRemoto.Fallo -> "Escritura rechazada: ${r.fallo.mensaje}"
        }
        return ResultadoPruebaAcceso(lectura, escritura)
    }
}
