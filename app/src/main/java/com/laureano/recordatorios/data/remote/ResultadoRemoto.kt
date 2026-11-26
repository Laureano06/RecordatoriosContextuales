package com.laureano.recordatorios.data.remote

import com.google.gson.JsonParseException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.cancellation.CancellationException

// Todas las respuestas del servicio se interpretan acá, nunca en las pantallas
sealed interface FalloRemoto {
    val mensaje: String

    // Sin red, timeout o error del servidor: vale la pena reintentar más tarde
    val reintentable: Boolean

    data object SinConexion : FalloRemoto {
        override val mensaje = "Sin conexión con el servicio"
        override val reintentable = true
    }

    data object Timeout : FalloRemoto {
        override val mensaje = "El servicio tardó demasiado en responder"
        override val reintentable = true
    }

    data class SolicitudInvalida(val codigo: Int) : FalloRemoto {
        override val mensaje = "El servicio rechazó los datos (HTTP $codigo)"
        override val reintentable = false
    }

    data object NoEncontrado : FalloRemoto {
        override val mensaje = "El recordatorio ya no existe en el servicio (HTTP 404)"
        override val reintentable = false
    }

    data class ErrorServidor(val codigo: Int) : FalloRemoto {
        override val mensaje = "Error del servidor (HTTP $codigo)"
        override val reintentable = true
    }

    data class Inesperado(val detalle: String) : FalloRemoto {
        override val mensaje = "Respuesta inesperada del servicio"
        override val reintentable = true
    }
}

sealed interface ResultadoRemoto<out T> {
    data class Exito<T>(val datos: T) : ResultadoRemoto<T>
    data class Fallo(val fallo: FalloRemoto) : ResultadoRemoto<Nothing>
}

fun falloPorCodigo(codigo: Int): FalloRemoto = when (codigo) {
    404 -> FalloRemoto.NoEncontrado
    408 -> FalloRemoto.Timeout
    in 400..499 -> FalloRemoto.SolicitudInvalida(codigo)
    else -> FalloRemoto.ErrorServidor(codigo)
}

// Para respuestas que deben traer cuerpo (listar, crear, actualizar)
suspend fun <T> llamarApi(llamada: suspend () -> Response<T>): ResultadoRemoto<T> =
    capturarErrores {
        val respuesta = llamada()
        val cuerpo = respuesta.body()
        when {
            !respuesta.isSuccessful -> ResultadoRemoto.Fallo(falloPorCodigo(respuesta.code()))
            cuerpo == null -> ResultadoRemoto.Fallo(FalloRemoto.Inesperado("Respuesta vacía"))
            else -> ResultadoRemoto.Exito(cuerpo)
        }
    }

// Para respuestas sin cuerpo (eliminar)
suspend fun llamarApiSinCuerpo(llamada: suspend () -> Response<*>): ResultadoRemoto<Unit> =
    capturarErrores {
        val respuesta = llamada()
        if (respuesta.isSuccessful) {
            ResultadoRemoto.Exito(Unit)
        } else {
            ResultadoRemoto.Fallo(falloPorCodigo(respuesta.code()))
        }
    }

private suspend fun <T> capturarErrores(
    bloque: suspend () -> ResultadoRemoto<T>
): ResultadoRemoto<T> =
    try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: InterruptedIOException) {
        // SocketTimeoutException y el timeout total de OkHttp heredan de esta clase
        ResultadoRemoto.Fallo(FalloRemoto.Timeout)
    } catch (e: IOException) {
        ResultadoRemoto.Fallo(FalloRemoto.SinConexion)
    } catch (e: JsonParseException) {
        ResultadoRemoto.Fallo(FalloRemoto.Inesperado(e.message ?: "JSON inválido"))
    }
