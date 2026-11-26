package com.laureano.recordatorios.ui.preferencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.preferencias.PreferenciasRepository
import com.laureano.recordatorios.domain.Orden
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
import com.laureano.recordatorios.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PreferenciasViewModel(private val repositorio: PreferenciasRepository) : ViewModel() {

    val preferencias: StateFlow<Preferencias> = repositorio.preferencias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Preferencias())

    // Las escrituras en DataStore son suspendidas y no bloquean el hilo principal
    fun onCambiarTema(tema: Tema) {
        viewModelScope.launch { repositorio.cambiarTema(tema) }
    }

    fun onCambiarMostrarCompletados(mostrar: Boolean) {
        viewModelScope.launch { repositorio.cambiarMostrarCompletados(mostrar) }
    }

    fun onCambiarOrden(orden: Orden) {
        viewModelScope.launch { repositorio.cambiarOrden(orden) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PreferenciasViewModel(contenedor().preferencias) }
        }
    }
}
