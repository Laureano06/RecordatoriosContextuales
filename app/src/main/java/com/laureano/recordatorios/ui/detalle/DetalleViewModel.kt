package com.laureano.recordatorios.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.Dependencias
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.ui.navegacion.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class Encontrado(val recordatorio: Recordatorio) : DetalleUiState
    data object NoEncontrado : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}

class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    // La navegación solo pasa el id; el recordatorio se busca en el repositorio
    private val id: Long = checkNotNull(savedStateHandle[Rutas.ARG_ID])

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun onReintentar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { DetalleUiState.Cargando }
            val nuevoEstado = try {
                repositorio.obtenerPorId(id)
                    ?.let { DetalleUiState.Encontrado(it) }
                    ?: DetalleUiState.NoEncontrado
            } catch (e: IOException) {
                DetalleUiState.Error(e.message ?: "Ocurrió un error inesperado")
            }
            _uiState.update { nuevoEstado }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DetalleViewModel(createSavedStateHandle(), Dependencias.repositorio) }
        }
    }
}
