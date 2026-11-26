package com.laureano.recordatorios

import com.laureano.recordatorios.data.remote.ClienteRemoto
import com.laureano.recordatorios.data.remote.FalloRemoto
import com.laureano.recordatorios.data.remote.RecordatorioApi
import com.laureano.recordatorios.data.remote.ResultadoRemoto
import com.laureano.recordatorios.data.remote.llamarApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

// Comprueba cómo la capa de datos interpreta cada respuesta HTTP con un servidor real de prueba
class ServicioRemotoTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: RecordatorioApi

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = ClienteRemoto.crearApi(servidor.url("/").toString(), timeoutSegundos = 1)
    }

    @After
    fun cerrar() {
        runCatching { servidor.shutdown() }
    }

    @Test
    fun respuesta200SeConvierteEnDto() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":1,"uuid":"u1","titulo":"Pagar la luz","categoriaId":4,
                "prioridad":"MEDIA","estado":"PENDIENTE","actualizadoEn":5}]"""
            )
        )
        val resultado = llamarApi { api.listar() }
        assertTrue(resultado is ResultadoRemoto.Exito)
        assertEquals("Pagar la luz", (resultado as ResultadoRemoto.Exito).datos.first().titulo)
    }

    @Test
    fun respuesta400EsSolicitudInvalida() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(400))
        assertEquals(
            ResultadoRemoto.Fallo(FalloRemoto.SolicitudInvalida(400)),
            llamarApi { api.listar() }
        )
    }

    @Test
    fun respuesta500EsErrorDelServidor() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(503))
        assertEquals(
            ResultadoRemoto.Fallo(FalloRemoto.ErrorServidor(503)),
            llamarApi { api.listar() }
        )
    }

    @Test
    fun respuestaLentaEsTimeout() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(200).setBody("[]").setHeadersDelay(3, TimeUnit.SECONDS)
        )
        assertEquals(ResultadoRemoto.Fallo(FalloRemoto.Timeout), llamarApi { api.listar() })
    }

    @Test
    fun servidorApagadoEsSinConexion() = runTest {
        servidor.shutdown()
        assertEquals(ResultadoRemoto.Fallo(FalloRemoto.SinConexion), llamarApi { api.listar() })
    }
}
