package com.laureano.recordatorios.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.auth.AuthRepository
import com.laureano.recordatorios.data.auth.EstadoSesion
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Decide qué mostrar: la pantalla de acceso o la app de la cuenta activa
class SesionViewModel(private val auth: AuthRepository) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = auth.estado

    // Al volver a la app se comprueba que la sesión guardada siga siendo válida
    fun onVolverAPrimerPlano() {
        viewModelScope.launch { auth.verificarSesion() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SesionViewModel(contenedor().auth) }
        }
    }
}
