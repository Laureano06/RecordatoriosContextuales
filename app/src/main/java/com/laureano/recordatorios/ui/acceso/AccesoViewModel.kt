package com.laureano.recordatorios.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.auth.AuthRepository
import com.laureano.recordatorios.data.auth.ResultadoAuth
import com.laureano.recordatorios.ui.contenedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModoAcceso { IniciarSesion, Registro }

sealed interface EstadoAcceso {
    data object Inactivo : EstadoAcceso

    // Autenticación en curso: se deshabilita el formulario
    data object EnCurso : EstadoAcceso
    data class Error(val mensaje: String) : EstadoAcceso
}

data class AccesoUiState(
    val modo: ModoAcceso = ModoAcceso.IniciarSesion,
    val email: String = "",
    // Solo vive en memoria mientras se escribe; nunca se guarda
    val contrasenia: String = "",
    val errorEmail: String? = null,
    val errorContrasenia: String? = null,
    val estado: EstadoAcceso = EstadoAcceso.Inactivo
) {
    val enCurso: Boolean get() = estado is EstadoAcceso.EnCurso
    val puedeEnviar: Boolean
        get() = !enCurso && email.isNotBlank() && contrasenia.isNotEmpty() &&
            errorEmail == null && errorContrasenia == null
}

private val patronEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun validarEmail(email: String): String? =
    if (email.isBlank() || patronEmail.matches(email.trim())) null else "Ingresá un email válido"

fun validarContrasenia(contrasenia: String, modo: ModoAcceso): String? =
    if (modo == ModoAcceso.Registro && contrasenia.isNotEmpty() && contrasenia.length < 6) {
        "Mínimo 6 caracteres"
    } else {
        null
    }

class AccesoViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AccesoUiState())
    val uiState: StateFlow<AccesoUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) = _uiState.update {
        it.copy(email = valor, errorEmail = validarEmail(valor), estado = EstadoAcceso.Inactivo)
    }

    fun onContraseniaChange(valor: String) = _uiState.update {
        it.copy(
            contrasenia = valor,
            errorContrasenia = validarContrasenia(valor, it.modo),
            estado = EstadoAcceso.Inactivo
        )
    }

    fun onCambiarModo() = _uiState.update {
        val modo = if (it.modo == ModoAcceso.IniciarSesion) ModoAcceso.Registro else ModoAcceso.IniciarSesion
        it.copy(
            modo = modo,
            errorContrasenia = validarContrasenia(it.contrasenia, modo),
            estado = EstadoAcceso.Inactivo
        )
    }

    fun onEnviar() {
        val estado = _uiState.value
        if (!estado.puedeEnviar) return
        _uiState.update { it.copy(estado = EstadoAcceso.EnCurso) }
        viewModelScope.launch {
            val resultado = when (estado.modo) {
                ModoAcceso.IniciarSesion -> auth.iniciarSesion(estado.email, estado.contrasenia)
                ModoAcceso.Registro -> auth.registrar(estado.email, estado.contrasenia)
            }
            // Si sale bien, la sesión cambia y la app pasa sola al listado
            _uiState.update {
                when (resultado) {
                    // Se vacía el formulario: la próxima persona no ve el email anterior
                    ResultadoAuth.Exito -> AccesoUiState()
                    is ResultadoAuth.Error -> it.copy(estado = EstadoAcceso.Error(resultado.mensaje))
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AccesoViewModel(contenedor().auth) }
        }
    }
}
