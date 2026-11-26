package com.laureano.recordatorios.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.ControlSimulacion
import com.laureano.recordatorios.data.Dependencias
import com.laureano.recordatorios.data.ModoSimulacion
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.domain.Recordatorio
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface ListadoUiState {
    data object Cargando : ListadoUiState
    data object Vacio : ListadoUiState
    data class Datos(val recordatorios: List<Recordatorio>) : ListadoUiState
    data class Error(val mensaje: String) : ListadoUiState
}

class ListadoViewModel(
    private val repositorio: RecordatorioRepository,
    private val simulador: ControlSimulacion
) : ViewModel() {

    // Privado y mutable adentro; afuera solo se puede leer
    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Cargando)
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    private val _modo = MutableStateFlow(simulador.modo)
    val modo: StateFlow<ModoSimulacion> = _modo.asStateFlow()

    private var carga: Job? = null

    init {
        // Carga al inicio y cada vez que se guarda un recordatorio nuevo
        viewModelScope.launch {
            repositorio.actualizaciones.collect { cargar() }
        }
    }

    fun onReintentar() = cargar()

    fun onCambiarModo(nuevo: ModoSimulacion) {
        simulador.modo = nuevo
        _modo.update { nuevo }
        cargar()
    }

    private fun cargar() {
        carga?.cancel()
        carga = viewModelScope.launch {
            _uiState.update { ListadoUiState.Cargando }
            val nuevoEstado = try {
                val lista = repositorio.obtenerTodos()
                if (lista.isEmpty()) ListadoUiState.Vacio else ListadoUiState.Datos(lista)
            } catch (e: IOException) {
                ListadoUiState.Error(e.message ?: "Ocurrió un error inesperado")
            }
            _uiState.update { nuevoEstado }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ListadoViewModel(Dependencias.repositorio, Dependencias.simulador) }
        }
    }
}
