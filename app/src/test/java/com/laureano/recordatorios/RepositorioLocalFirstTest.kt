package com.laureano.recordatorios

import com.laureano.recordatorios.data.DatosRecordatorio
import com.laureano.recordatorios.data.EstadoSincronizacion
import com.laureano.recordatorios.data.RecordatorioRepositoryLocalFirst
import com.laureano.recordatorios.data.local.EstadoSyncLocal
import com.laureano.recordatorios.data.local.RecordatorioDao
import com.laureano.recordatorios.data.local.RecordatorioEntity
import com.laureano.recordatorios.data.remote.FalloRemoto
import com.laureano.recordatorios.data.remote.RecordatorioApi
import com.laureano.recordatorios.data.remote.RecordatorioDto
import com.laureano.recordatorios.domain.Categorias
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Prioridad
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RepositorioLocalFirstTest {

    private val datos = DatosRecordatorio("Llamar al plomero", null, Categorias.hogar, Prioridad.Media)

    private fun TestScope.crearRepositorio(dao: DaoEnMemoria, api: ApiFalsa) =
        RecordatorioRepositoryLocalFirst(
            dao = dao,
            api = api,
            preferencias = flowOf(Preferencias()),
            alcance = this,
            sincronizarAlGuardar = false
        )

    @Test
    fun sinConexionSeGuardaLocalYQuedaPendiente() = runTest {
        val dao = DaoEnMemoria()
        val api = ApiFalsa().apply { sinConexion = true }
        val repo = crearRepositorio(dao, api)

        repo.crear(datos)
        val estado = repo.sincronizar()

        val guardado = dao.filas.value.single()
        assertEquals(EstadoSyncLocal.PENDIENTE_CREAR, guardado.estadoSync)
        assertTrue(estado is EstadoSincronizacion.Fallida)
        assertEquals(FalloRemoto.SinConexion, (estado as EstadoSincronizacion.Fallida).fallo)
    }

    @Test
    fun alVolverLaConexionSeEnviaUnaSolaVezYNoSeDuplica() = runTest {
        val dao = DaoEnMemoria()
        val api = ApiFalsa().apply { sinConexion = true }
        val repo = crearRepositorio(dao, api)
        repo.crear(datos)
        repo.sincronizar()

        api.sinConexion = false
        repo.sincronizar()
        repo.sincronizar()

        assertEquals(1, api.altas)
        assertEquals(1, api.servidor.size)
        val fila = dao.filas.value.single()
        assertEquals(EstadoSyncLocal.SINCRONIZADO, fila.estadoSync)
        assertEquals(api.servidor.single().id, fila.remotoId)
    }

    @Test
    fun dosSincronizacionesSimultaneasNoEnvianDosVeces() = runTest {
        val dao = DaoEnMemoria()
        val api = ApiFalsa().apply { demoraMs = 500 }
        val repo = crearRepositorio(dao, api)
        repo.crear(datos)

        // Simula varios eventos que piden sincronizar al mismo tiempo
        List(3) { async { repo.sincronizar() } }.awaitAll()

        assertEquals(1, api.altas)
        assertEquals(1, dao.filas.value.size)
    }

    @Test
    fun descargarDosVecesNoDuplicaLosDelServicio() = runTest {
        val dao = DaoEnMemoria()
        val api = ApiFalsa()
        api.servidor += RecordatorioDto(
            id = 10, uuid = "remoto-1", titulo = "Comprar pan", categoriaId = 4,
            prioridad = "BAJA", estado = "PENDIENTE", actualizadoEn = 1
        )
        val repo = crearRepositorio(dao, api)

        repo.sincronizar()
        repo.sincronizar()

        assertEquals(1, dao.filas.value.size)
        assertEquals("Comprar pan", dao.filas.value.single().titulo)
    }

    @Test
    fun errorDelServidorConservaLosDatosLocales() = runTest {
        val dao = DaoEnMemoria()
        val api = ApiFalsa().apply { codigoError = 500 }
        val repo = crearRepositorio(dao, api)
        repo.crear(datos)

        val estado = repo.sincronizar() as EstadoSincronizacion.Fallida

        assertEquals(FalloRemoto.ErrorServidor(500), estado.fallo)
        val fila = dao.filas.value.single()
        assertEquals(EstadoSyncLocal.PENDIENTE_CREAR, fila.estadoSync)
        assertEquals(1, fila.intentos)
        // No es un error del dato: se muestra como pendiente, no como error
        assertNull(fila.ultimoError)
    }

    @Test
    fun datoRechazadoQuedaMarcadoConErrorYNoFrenaLosDemas() = runTest {
        val dao = DaoEnMemoria()
        val api = ApiFalsa().apply { codigoError = 400 }
        val repo = crearRepositorio(dao, api)
        repo.crear(datos)
        repo.crear(datos.copy(titulo = "Regar las plantas"))

        val estado = repo.sincronizar() as EstadoSincronizacion.Fallida

        assertEquals(FalloRemoto.SolicitudInvalida(400), estado.fallo)
        assertTrue(dao.filas.value.all { it.ultimoError != null && it.intentos == 1 })
    }

    @Test
    fun eliminarAlgoNuncaEnviadoNoLlamaAlServicio() = runTest {
        val dao = DaoEnMemoria()
        val api = ApiFalsa().apply { sinConexion = true }
        val repo = crearRepositorio(dao, api)
        val id = repo.crear(datos)

        repo.eliminar(id)
        api.sinConexion = false
        repo.sincronizar()

        assertNull(dao.buscarPorId(id))
        assertEquals(0, api.altas)
    }
}

// Servicio falso que guarda en memoria y respeta el uuid como el servicio real
private class ApiFalsa : RecordatorioApi {
    val servidor = mutableListOf<RecordatorioDto>()
    var altas = 0
    var sinConexion = false
    var codigoError: Int? = null
    var demoraMs = 0L
    private var siguienteId = 100L

    private suspend fun comprobar() {
        delay(demoraMs)
        if (sinConexion) throw IOException("Sin red")
    }

    private fun <T> error(): Response<T>? =
        codigoError?.let { Response.error(it, "".toResponseBody()) }

    override suspend fun listar(): Response<List<RecordatorioDto>> {
        comprobar()
        return error() ?: Response.success(servidor.toList())
    }

    override suspend fun crear(recordatorio: RecordatorioDto): Response<RecordatorioDto> {
        comprobar()
        error<RecordatorioDto>()?.let { return it }
        servidor.find { it.uuid == recordatorio.uuid }?.let { return Response.success(it) }
        altas++
        val creado = recordatorio.copy(id = siguienteId++)
        servidor += creado
        return Response.success(creado)
    }

    override suspend fun actualizar(id: Long, recordatorio: RecordatorioDto): Response<RecordatorioDto> {
        comprobar()
        error<RecordatorioDto>()?.let { return it }
        servidor.replaceAll { if (it.id == id) recordatorio.copy(id = id) else it }
        return Response.success(recordatorio.copy(id = id))
    }

    override suspend fun eliminar(id: Long): Response<Unit> {
        comprobar()
        error<Unit>()?.let { return it }
        servidor.removeAll { it.id == id }
        return Response.success(Unit)
    }
}

// DAO en memoria con el mismo comportamiento que las consultas de Room
private class DaoEnMemoria : RecordatorioDao {
    val filas = MutableStateFlow<List<RecordatorioEntity>>(emptyList())
    private var siguienteId = 1L

    private fun visibles() = filas.value.filter { it.estadoSync != EstadoSyncLocal.PENDIENTE_ELIMINAR }

    override fun observarTodos(): Flow<List<RecordatorioEntity>> =
        filas.map { visibles().sortedByDescending { it.actualizadoEn } }

    override fun observarExcluyendoEstado(estadoExcluido: String): Flow<List<RecordatorioEntity>> =
        filas.map { visibles().filter { it.estado != estadoExcluido } }

    override fun observarPorId(id: Long): Flow<RecordatorioEntity?> =
        filas.map { visibles().find { it.id == id } }

    override fun observarCantidadPendientes(): Flow<Int> =
        filas.map { lista -> lista.count { it.estadoSync != EstadoSyncLocal.SINCRONIZADO } }

    override suspend fun buscarPorId(id: Long) = filas.value.find { it.id == id }

    override suspend fun buscarPorUuid(uuid: String) = filas.value.find { it.uuid == uuid }

    override suspend fun pendientes() =
        filas.value.filter { it.estadoSync != EstadoSyncLocal.SINCRONIZADO }

    override suspend fun sincronizados() =
        filas.value.filter { it.estadoSync == EstadoSyncLocal.SINCRONIZADO }

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

    override suspend fun confirmarSincronizacion(id: Long, remotoId: Long, version: Long) {
        filas.value = filas.value.map {
            if (it.id != id) {
                it
            } else {
                val estado = when {
                    it.estadoSync == EstadoSyncLocal.PENDIENTE_ELIMINAR -> it.estadoSync
                    it.actualizadoEn == version -> EstadoSyncLocal.SINCRONIZADO
                    else -> EstadoSyncLocal.PENDIENTE_ACTUALIZAR
                }
                it.copy(remotoId = remotoId, ultimoError = null, intentos = 0, estadoSync = estado)
            }
        }
    }

    override suspend fun sumarIntento(id: Long) {
        filas.value = filas.value.map { if (it.id == id) it.copy(intentos = it.intentos + 1) else it }
    }

    override suspend fun registrarError(id: Long, error: String) {
        filas.value = filas.value.map {
            if (it.id == id) it.copy(ultimoError = error, intentos = it.intentos + 1) else it
        }
    }
}
