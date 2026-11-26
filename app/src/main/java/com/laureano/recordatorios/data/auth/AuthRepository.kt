package com.laureano.recordatorios.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

data class Usuario(val uid: String, val email: String)

sealed interface EstadoSesion {
    data class SinSesion(val motivo: String? = null) : EstadoSesion
    data class Activa(val usuario: Usuario) : EstadoSesion
}

sealed interface ResultadoAuth {
    data object Exito : ResultadoAuth
    data class Error(val mensaje: String) : ResultadoAuth
}

// La interfaz depende de esto; los detalles de Firebase Auth quedan en la implementación
interface AuthRepository {
    val estado: StateFlow<EstadoSesion>
    suspend fun registrar(email: String, contrasenia: String): ResultadoAuth
    suspend fun iniciarSesion(email: String, contrasenia: String): ResultadoAuth
    fun cerrarSesion()

    // Comprueba con Firebase que la cuenta siga existiendo y habilitada
    suspend fun verificarSesion()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    alcance: CoroutineScope
) : AuthRepository {

    // Explica por qué se cerró la sesión cuando no fue la persona quien la cerró
    private val motivoCierre = MutableStateFlow<String?>(null)

    private val usuarioActual = callbackFlow {
        val oyente = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(oyente)
        awaitClose { auth.removeAuthStateListener(oyente) }
    }

    // Firebase guarda la sesión por su cuenta: la app no guarda contraseñas ni tokens
    override val estado: StateFlow<EstadoSesion> =
        combine(usuarioActual, motivoCierre) { usuario, motivo -> estadoDe(usuario, motivo) }
            .stateIn(alcance, SharingStarted.Eagerly, estadoDe(auth.currentUser, null))

    private fun estadoDe(usuario: FirebaseUser?, motivo: String?): EstadoSesion =
        if (usuario == null) {
            EstadoSesion.SinSesion(motivo)
        } else {
            EstadoSesion.Activa(Usuario(usuario.uid, usuario.email.orEmpty()))
        }

    override suspend fun registrar(email: String, contrasenia: String) = ejecutar {
        auth.createUserWithEmailAndPassword(email.trim(), contrasenia).await()
    }

    override suspend fun iniciarSesion(email: String, contrasenia: String) = ejecutar {
        auth.signInWithEmailAndPassword(email.trim(), contrasenia).await()
    }

    override fun cerrarSesion() {
        motivoCierre.value = null
        auth.signOut()
    }

    override suspend fun verificarSesion() {
        val usuario = auth.currentUser ?: return
        try {
            usuario.reload().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            // La cuenta se borró o se deshabilitó desde la consola
            motivoCierre.value = "Tu sesión ya no es válida. Iniciá sesión de nuevo."
            auth.signOut()
        } catch (e: FirebaseNetworkException) {
            // Sin red se mantiene la sesión guardada: la app sigue funcionando offline
        }
    }

    private suspend fun ejecutar(operacion: suspend () -> Unit): ResultadoAuth =
        try {
            operacion()
            motivoCierre.value = null
            ResultadoAuth.Exito
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoAuth.Error(mensajeDe(e))
        }
}

// Los mensajes no revelan si un email existe o no
fun mensajeDe(error: Exception): String = when (error) {
    is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres"
    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese email"
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> "Email o contraseña incorrectos"
    is FirebaseNetworkException -> "Sin conexión. Probá de nuevo cuando tengas red"
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Esperá unos minutos"
    is FirebaseAuthException -> "No se pudo completar la operación (${error.errorCode})"
    else -> "No se pudo completar la operación"
}
