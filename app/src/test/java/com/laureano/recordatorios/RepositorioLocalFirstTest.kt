package com.laureano.recordatorios

import com.laureano.recordatorios.data.DatosRecordatorio
import com.laureano.recordatorios.data.EstadoSincronizacion
import com.laureano.recordatorios.data.RecordatorioRepositoryLocalFirst
import com.laureano.recordatorios.data.SinSesionException
import com.laureano.recordatorios.data.local.EstadoSyncLocal
import com.laureano.recordatorios.data.local.RecordatorioDao
import com.laureano.recordatorios.data.local.RecordatorioEntity
import com.laureano.recordatorios.data.remote.FalloRemoto
import com.laureano.recordatorios.data.remote.FuenteRemota
import com.laureano.recordatorios.data.remote.RecordatorioRemoto
import com.laureano.recordatorios.data.remote.ResultadoRemoto
import com.laureano.recordatorios.domain.Categorias
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Prioridad
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorioLocalFirstTest {

    private val datos = DatosRecordatorio("Llamar al plomero", null, Categorias.hogar, Prioridad.Media)

    private class Escenario(val dao: DaoEnMemoria, val nube: NubeFalsa, val sesion: MutableStateFlow<String?>)

    private fun TestScope.crear(uid: String? = "ana", nube: NubeFalsa = NubeFalsa()):
        Pair<RecordatorioRepositoryLocalFirst, Escenario> {
        val escenario = Escenario(DaoEnMemoria(), nube, MutableStateFlow(uid))
        val repo = RecordatorioRepositoryLocalFirst(
            dao = escenario.dao,
            remota = nube,
            uidActivo = escenario.sesion,
            preferencias = flowOf(Preferencias()),
            alcance = this,
            sincronizarAlGuardar = false
        )
        return repo to escenario
    }

    @Test
    fun sinConexionSeGuardaEnRoomYQuedaPendiente() = runTest {
        val (repo, e) = crear(nube = NubeFalsa().apply { fallo = FalloRemoto.SinConexion })
        repo.crear(datos)

        val estado = repo.sincronizar() as EstadoSincronizacion.Fallida

        assertEquals(FalloRemoto.SinConexion, estado.fallo)
        val fila = e.dao.filas.value.single()
        assertEquals(EstadoSyncLocal.PENDIENTE_CREAR, fila.estadoSync)
        assertEquals("ana", fila.uid)
        assertNull(fila.ultimoError)
    }

    @Test
    fun alVolverLaRedSeEscribeUnSoloDocumento() = runTest {
        val (repo, e) = crear(nube = NubeFalsa().apply { fallo = FalloRemoto.SinConexion })
        repo.crear(datos)
        repo.sincronizar()

        e.nube.fallo = null
        repo.sincronizar()
        repo.sincronizar()

        assertEquals(1, e.nube.documentos.size)
        assertEquals(EstadoSyncLocal.SINCRONIZADO, e.dao.filas.value.single().estadoSync)
    }

    @Test
    fun modificarYReintentarSobrescribeElMismoDocumento() = runTest {
        val (repo, e) = crear()
        val id = repo.crear(datos)
        repo.sincronizar()

        repo.actualizar(id, datos.copy(titulo = "Llamar al gasista"))
        repo.sincronizar()

        assertEquals(1, e.nube.documentos.size)
        assertEquals("Llamar al gasista", e.nube.documentos.values.single().titulo)
        assertEquals(2, e.nube.escrituras)
    }

    @Test
    fun variasSincronizacionesSimultaneasEscribenUnaSolaVez() = runTest {
        val (repo, e) = crear(nube = NubeFalsa().apply { demoraMs = 500 })
        repo.crear(datos)

        List(3) { async { repo.sincronizar() } }.awaitAll()

        assertEquals(1, e.nube.escrituras)
    }

    @Test
    fun otroDispositivoRecuperaLosDatosSinDuplicar() = runTest {
        val nube = NubeFalsa()
        val (telefono1, _) = crear(nube = nube)
        telefono1.crear(datos)
        telefono1.sincronizar()

        // Segundo teléfono: Room vacío, misma cuenta
        val (telefono2, e2) = crear(nube = nube)
        telefono2.sincronizar()
        telefono2.sincronizar()

        assertEquals(1, e2.dao.filas.value.size)
        assertEquals("Llamar al plomero", e2.dao.filas.value.single().titulo)
    }

    @Test
    fun unaCuentaNoVeNiModificaRecordatoriosDeOtra() = runTest {
        val (repo, e) = crear()
        val idDeAna = repo.crear(datos)

        e.sesion.value = "bruno"
        assertTrue(repo.observarListado().first().isEmpty())
        assertNull(repo.observarRecordatorio(idDeAna).first())

        // Aunque la pantalla pida el id de Ana, el repositorio no lo encuentra
        repo.eliminar(idDeAna)
        assertEquals(EstadoSyncLocal.PENDIENTE_CREAR, e.dao.filas.value.single().estadoSync)
    }

    @Test
    fun laNubeRechazaElAccesoYElDatoQuedaMarcado() = runTest {
        val (repo, e) = crear(nube = NubeFalsa().apply { fallo = FalloRemoto.SinPermiso })
        repo.crear(datos)

        val estado = repo.sincronizar() as EstadoSincronizacion.Fallida

        assertEquals(FalloRemoto.SinPermiso, estado.fallo)
        assertTrue(e.dao.filas.value.single().ultimoError != null)
    }

    @Test
    fun alCerrarSesionSeBorraLoSincronizadoYSeConservaLoPendiente() = runTest {
        val (repo, e) = crear()
        repo.crear(datos)
        repo.sincronizar()
        e.nube.fallo = FalloRemoto.SinConexion
        repo.crear(datos.copy(titulo = "Comprar lamparitas"))

        repo.prepararCierreDeSesion()

        val restante = e.dao.filas.value.single()
        assertEquals("Comprar lamparitas", restante.titulo)
        assertEquals(EstadoSyncLocal.PENDIENTE_CREAR, restante.estadoSync)
    }

    @Test(expected = SinSesionException::class)
    fun sinSesionNoSePuedeCrear() = runTest {
        val (repo, _) = crear(uid = null)
        repo.crear(datos)
    }

    @Test
    fun eliminarSeReflejaEnLaNube() = runTest {
        val (repo, e) = crear()
        val id = repo.crear(datos)
        repo.sincronizar()

        repo.eliminar(id)
        repo.sincronizar()

        assertTrue(e.nube.documentos.isEmpty())
        assertTrue(e.dao.filas.value.isEmpty())
    }
}

// Firestore falso: guarda documentos por uid/uuid, igual que la ruta users/{uid}/reminders/{uuid}
private class NubeFalsa : FuenteRemota {
    val documentos = linkedMapOf<Pair<String, String>, RecordatorioRemoto>()
    var escrituras = 0
    var fallo: FalloRemoto? = null
    var demoraMs = 0L

    override suspend fun listar(uid: String): ResultadoRemoto<List<RecordatorioRemoto>> {
        delay(demoraMs)
        fallo?.let { return ResultadoRemoto.Fallo(it) }
        return ResultadoRemoto.Exito(documentos.filterKeys { it.first == uid }.values.toList())
    }

    override suspend fun guardar(recordatorio: RecordatorioRemoto): ResultadoRemoto<Unit> {
        delay(demoraMs)
        fallo?.let { return ResultadoRemoto.Fallo(it) }
        escrituras++
        documentos[recordatorio.uid to recordatorio.uuid] = recordatorio
        return ResultadoRemoto.Exito(Unit)
    }

    override suspend fun eliminar(uid: String, uuid: String): ResultadoRemoto<Unit> {
        delay(demoraMs)
        fallo?.let { return ResultadoRemoto.Fallo(it) }
        documentos.remove(uid to uuid)
        return ResultadoRemoto.Exito(Unit)
    }
}

// DAO en memoria con el mismo comportamiento que las consultas de Room
private class DaoEnMemoria : RecordatorioDao {
    val filas = MutableStateFlow<List<RecordatorioEntity>>(emptyList())
    private var siguienteId = 1L

    private fun visibles(uid: String) =
        filas.value.filter { it.uid == uid && it.estadoSync != EstadoSyncLocal.PENDIENTE_ELIMINAR }

    override fun observarTodos(uid: String): Flow<List<RecordatorioEntity>> =
        filas.map { visibles(uid).sortedByDescending { it.actualizadoEn } }

    override fun observarExcluyendoEstado(uid: String, estadoExcluido: String) =
        filas.map { visibles(uid).filter { it.estado != estadoExcluido } }

    override fun observarPorId(id: Long, uid: String) = filas.map { visibles(uid).find { it.id == id } }

    override fun observarCantidadPendientes(uid: String) =
        filas.map { l -> l.count { it.uid == uid && it.estadoSync != EstadoSyncLocal.SINCRONIZADO } }

    override suspend fun buscarPorId(id: Long, uid: String) = filas.value.find { it.id == id && it.uid == uid }

    override suspend fun buscarPorUuid(uuid: String) = filas.value.find { it.uuid == uuid }

    override suspend fun pendientes(uid: String) =
        filas.value.filter { it.uid == uid && it.estadoSync != EstadoSyncLocal.SINCRONIZADO }

    override suspend fun sincronizados(uid: String) =
        filas.value.filter { it.uid == uid && it.estadoSync == EstadoSyncLocal.SINCRONIZADO }

    override suspend fun insertar(recordatorio: RecordatorioEntity): Long {
        check(filas.value.none { it.uuid == recordatorio.uuid }) { "uuid repetido" }
        val id = siguienteId++
        filas.value = filas.value + recordatorio.copy(id = id)
        return id
    }

    override suspend fun actualizar(recordatorio: RecordatorioEntity) {
        filas.value = filas.value.map { if (it.id == recordatorio.id) recordatorio else it }
    }

    override suspend fun eliminarPorId(id: Long) {
        filas.value = filas.value.filter { it.id != id }
    }

    override suspend fun eliminarSincronizados(uid: String) {
        filas.value = filas.value.filterNot { it.uid == uid && it.estadoSync == EstadoSyncLocal.SINCRONIZADO }
    }

    override suspend fun confirmarSincronizacion(id: Long, version: Long) {
        filas.value = filas.value.map {
            if (it.id != id) {
                it
            } else {
                val estado = when {
                    it.estadoSync == EstadoSyncLocal.PENDIENTE_ELIMINAR -> it.estadoSync
                    it.actualizadoEn == version -> EstadoSyncLocal.SINCRONIZADO
                    else -> EstadoSyncLocal.PENDIENTE_ACTUALIZAR
                }
                it.copy(ultimoError = null, intentos = 0, estadoSync = estado)
            }
        }
    }

    override suspend fun registrarError(id: Long, error: String) {
        filas.value = filas.value.map {
            if (it.id == id) it.copy(ultimoError = error, intentos = it.intentos + 1) else it
        }
    }

    override suspend fun sumarIntento(id: Long) {
        filas.value = filas.value.map { if (it.id == id) it.copy(intentos = it.intentos + 1) else it }
    }
}
