package com.laureano.recordatorios.data

import com.laureano.recordatorios.data.local.EstadoSyncLocal
import com.laureano.recordatorios.data.local.RecordatorioDao
import com.laureano.recordatorios.data.local.RecordatorioEntity
import com.laureano.recordatorios.data.remote.FalloRemoto
import com.laureano.recordatorios.data.remote.RecordatorioApi
import com.laureano.recordatorios.data.remote.RecordatorioDto
import com.laureano.recordatorios.data.remote.ResultadoRemoto
import com.laureano.recordatorios.data.remote.llamarApi
import com.laureano.recordatorios.data.remote.llamarApiSinCuerpo
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
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import retrofit2.Response
import java.util.UUID

// Local-first: toda operación se guarda primero en Room y después se intenta enviar al servicio
class RecordatorioRepositoryLocalFirst(
    private val dao: RecordatorioDao,
    private val api: RecordatorioApi,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarListado(): Flow<List<Recordatorio>> =
        preferencias
            .distinctUntilChangedBy { it.mostrarCompletados }
            .flatMapLatest { prefs ->
                if (prefs.mostrarCompletados) {
                    dao.observarTodos()
                } else {
                    dao.observarExcluyendoEstado(ESTADO_COMPLETADO)
                }
            }
            .combine(preferencias) { entidades, prefs ->
                entidades.map { it.aDominio() }.ordenar(prefs.orden)
            }

    override fun observarRecordatorio(id: Long): Flow<Recordatorio?> =
        dao.observarPorId(id).map { it?.aDominio() }

    override fun observarCantidadPendientes(): Flow<Int> = dao.observarCantidadPendientes()

    override suspend fun crear(datos: DatosRecordatorio): Long {
        val id = dao.insertar(
            RecordatorioEntity(
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

    override suspend fun actualizar(id: Long, datos: DatosRecordatorio) {
        val actual = dao.buscarPorId(id) ?: return
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
        val actual = dao.buscarPorId(id) ?: return
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
        val actual = dao.buscarPorId(id) ?: return
        // Se oculta enseguida; el borrado definitivo ocurre al sincronizar
        dao.actualizar(
            actual.copy(actualizadoEn = reloj(), estadoSync = EstadoSyncLocal.PENDIENTE_ELIMINAR)
        )
        sincronizarEnSegundoPlano()
    }

    // Si nunca llegó al servicio, sigue siendo una creación pendiente
    private fun estadoTrasModificar(actual: RecordatorioEntity): String =
        if (actual.remotoId == null) {
            EstadoSyncLocal.PENDIENTE_CREAR
        } else {
            EstadoSyncLocal.PENDIENTE_ACTUALIZAR
        }

    private fun sincronizarEnSegundoPlano() {
        if (sincronizarAlGuardar) alcance.launch { sincronizar() }
    }

    override suspend fun sincronizar(): EstadoSincronizacion {
        // Si ya hay una en curso, no se inicia otra: así nada se envía dos veces
        if (!candado.tryLock()) return _estadoSincronizacion.value
        try {
            _estadoSincronizacion.value = EstadoSincronizacion.EnCurso
            val falloSubida = subirPendientes()
            val falloDescarga =
                if (falloSubida?.reintentable == true) falloSubida else descargar()
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
    private suspend fun subirPendientes(): FalloRemoto? {
        var ultimoFallo: FalloRemoto? = null
        for (pendiente in dao.pendientes()) {
            val fallo = enviar(pendiente) ?: continue
            // Sin red o con el servidor caído no tiene sentido seguir con los demás
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
        val remotoId = entidad.remotoId
        return when {
            entidad.estadoSync == EstadoSyncLocal.PENDIENTE_ELIMINAR -> {
                if (remotoId == null) {
                    dao.eliminarPorId(entidad.id)
                    null
                } else {
                    when (val r = llamarApiSinCuerpo { api.eliminar(remotoId) }) {
                        is ResultadoRemoto.Exito -> dao.eliminarPorId(entidad.id).let { null }
                        // Si el servicio ya no lo tiene, el objetivo se cumplió igual
                        is ResultadoRemoto.Fallo ->
                            if (r.fallo == FalloRemoto.NoEncontrado) {
                                dao.eliminarPorId(entidad.id).let { null }
                            } else {
                                r.fallo
                            }
                    }
                }
            }
            remotoId == null -> confirmar(entidad) { api.crear(entidad.aDto()) }
            else -> confirmar(entidad) { api.actualizar(remotoId, entidad.aDto()) }
        }
    }

    private suspend fun confirmar(
        entidad: RecordatorioEntity,
        llamada: suspend () -> Response<RecordatorioDto>
    ): FalloRemoto? = when (val r = llamarApi(llamada)) {
        is ResultadoRemoto.Fallo -> r.fallo
        is ResultadoRemoto.Exito -> {
            val remotoId = r.datos.id
            if (remotoId == null) {
                FalloRemoto.Inesperado("El servicio no devolvió el id")
            } else {
                // Se actualiza el mismo registro: nunca se inserta uno nuevo
                dao.confirmarSincronizacion(entidad.id, remotoId, entidad.actualizadoEn)
                null
            }
        }
    }

    private suspend fun descargar(): FalloRemoto? =
        when (val r = llamarApi { api.listar() }) {
            is ResultadoRemoto.Fallo -> r.fallo
            is ResultadoRemoto.Exito -> {
                // Los DTO incompletos o con valores desconocidos se descartan
                val remotos = r.datos.mapNotNull { it.aEntityOrNull() }
                for (remoto in remotos) {
                    val local = dao.buscarPorUuid(remoto.uuid)
                    when {
                        local == null -> dao.insertar(remoto)
                        // Los cambios locales pendientes tienen prioridad sobre el servicio
                        local.estadoSync == EstadoSyncLocal.SINCRONIZADO ->
                            dao.actualizar(remoto.copy(id = local.id))
                    }
                }
                // Lo que se borró en el servicio también se borra acá
                val uuidsRemotos = remotos.map { it.uuid }.toSet()
                dao.sincronizados()
                    .filter { it.uuid !in uuidsRemotos }
                    .forEach { dao.eliminarPorId(it.id) }
                null
            }
        }
}
