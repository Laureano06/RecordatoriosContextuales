package com.laureano.recordatorios.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

// Fallos del servicio cloud ya traducidos: la interfaz nunca ve excepciones de Firebase
sealed interface FalloRemoto {
    val mensaje: String

    // Sin red, timeout o servicio caído: vale la pena reintentar más tarde
    val reintentable: Boolean

    data object SinConexion : FalloRemoto {
        override val mensaje = "Sin conexión con la nube"
        override val reintentable = true
    }

    data object Timeout : FalloRemoto {
        override val mensaje = "La nube tardó demasiado en responder"
        override val reintentable = true
    }

    // Las reglas de Firestore rechazaron la operación
    data object SinPermiso : FalloRemoto {
        override val mensaje = "La nube rechazó el acceso (sin permiso)"
        override val reintentable = false
    }

    data object SesionInvalida : FalloRemoto {
        override val mensaje = "La sesión no es válida. Iniciá sesión de nuevo"
        override val reintentable = false
    }

    data class SolicitudInvalida(val detalle: String) : FalloRemoto {
        override val mensaje = "La nube rechazó los datos"
        override val reintentable = false
    }

    data class ErrorServidor(val detalle: String) : FalloRemoto {
        override val mensaje = "Error del servicio en la nube"
        override val reintentable = true
    }
}

sealed interface ResultadoRemoto<out T> {
    data class Exito<T>(val datos: T) : ResultadoRemoto<T>
    data class Fallo(val fallo: FalloRemoto) : ResultadoRemoto<Nothing>
}

// Códigos de estado de Firestore (gRPC), los mismos que FirebaseFirestoreException.Code.value()
object CodigoFirestore {
    const val INVALID_ARGUMENT = 3
    const val DEADLINE_EXCEEDED = 4
    const val PERMISSION_DENIED = 7
    const val FAILED_PRECONDITION = 9
    const val OUT_OF_RANGE = 11
    const val UNAVAILABLE = 14
    const val UNAUTHENTICATED = 16
}

// Un solo lugar decide qué significa cada código de Firestore
fun falloDesdeCodigo(codigo: Int): FalloRemoto = when (codigo) {
    CodigoFirestore.PERMISSION_DENIED -> FalloRemoto.SinPermiso
    CodigoFirestore.UNAUTHENTICATED -> FalloRemoto.SesionInvalida
    CodigoFirestore.UNAVAILABLE -> FalloRemoto.SinConexion
    CodigoFirestore.DEADLINE_EXCEEDED -> FalloRemoto.Timeout
    CodigoFirestore.INVALID_ARGUMENT,
    CodigoFirestore.FAILED_PRECONDITION,
    CodigoFirestore.OUT_OF_RANGE -> FalloRemoto.SolicitudInvalida("código $codigo")
    else -> FalloRemoto.ErrorServidor("código $codigo")
}

// Ejecuta una operación de Firebase con límite de tiempo y traduce sus errores
suspend fun <T> llamarNube(timeoutMs: Long = 10_000, operacion: suspend () -> T): ResultadoRemoto<T> =
    try {
        ResultadoRemoto.Exito(withTimeout(timeoutMs) { operacion() })
    } catch (e: TimeoutCancellationException) {
        // Sin red, Firestore deja la escritura en espera: se corta y queda pendiente
        ResultadoRemoto.Fallo(FalloRemoto.Timeout)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        ResultadoRemoto.Fallo(falloDesdeCodigo(e.code.value()))
    } catch (e: FirebaseNetworkException) {
        ResultadoRemoto.Fallo(FalloRemoto.SinConexion)
    }
