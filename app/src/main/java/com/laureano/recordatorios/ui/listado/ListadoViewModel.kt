package com.laureano.recordatorios.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.EstadoSincronizacion
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.ui.contenedor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ListadoUiState {
    data object Cargando : ListadoUiState
    data object Vacio : ListadoUiState
    data class Datos(val recordatorios: List<Recordatorio>) : ListadoUiState
    data class Error(val mensaje: String) : ListadoUiState
}

// Lo que muestra la franja de sincronización arriba de la lista
data class InfoSincronizacion(
    val estado: EstadoSincronizacion = EstadoSincronizacion.Inactiva,
    val pendientes: Int = 0
)

class ListadoViewModel(private val repositorio: RecordatorioRepository) : ViewModel() {

    // Cambiar este valor vuelve a suscribirse a Room (sirve para Reintentar)
    private val intento = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ListadoUiState> = intento
        .flatMapLatest {
            repositorio.observarListado()
                .map<List<Recordatorio>, ListadoUiState> { lista ->
                    if (lista.isEmpty()) ListadoUiState.Vacio else ListadoUiState.Datos(lista)
                }
                .onStart { emit(ListadoUiState.Cargando) }
                .catch { emit(ListadoUiState.Error("No se pudieron leer los recordatorios guardados")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState.Cargando)

    val sincronizacion: StateFlow<InfoSincronizacion> = combine(
        repositorio.estadoSincronizacion,
        repositorio.observarCantidadPendientes()
    ) { estado, pendientes -> InfoSincronizacion(estado, pendientes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InfoSincronizacion())

    fun onReintentar() = intento.update { it + 1 }

    fun onSincronizar() {
        viewModelScope.launch { repositorio.sincronizar() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ListadoViewModel(contenedor().recordatorios) }
        }
    }
}
