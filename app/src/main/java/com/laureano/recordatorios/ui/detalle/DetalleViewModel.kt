package com.laureano.recordatorios.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.ui.contenedor
import com.laureano.recordatorios.ui.navegacion.Rutas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

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

    // La navegación solo pasa el id; los datos se leen de Room
    private val id: Long = checkNotNull(savedStateHandle[Rutas.ARG_ID])

    private val intento = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetalleUiState> = intento
        .flatMapLatest {
            repositorio.observarRecordatorio(id)
                .map { it?.let(DetalleUiState::Encontrado) ?: DetalleUiState.NoEncontrado }
                .catch { emit(DetalleUiState.Error("No se pudo leer el recordatorio")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)

    // Avisa a la pantalla que ya puede volver al listado
    private val _eliminado = MutableStateFlow(false)
    val eliminado: StateFlow<Boolean> = _eliminado.asStateFlow()

    fun onReintentar() = intento.update { it + 1 }

    fun onCompletar() {
        viewModelScope.launch {
            repositorio.cambiarEstado(id, EstadoRecordatorio.Completado(LocalDateTime.now()))
        }
    }

    fun onEliminar() {
        _eliminado.update { true }
        viewModelScope.launch { repositorio.eliminar(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DetalleViewModel(createSavedStateHandle(), contenedor().recordatorios) }
        }
    }
}
