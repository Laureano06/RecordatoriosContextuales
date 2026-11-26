package com.laureano.recordatorios.ui.preferencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.BuildConfig
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.data.auth.AuthRepository
import com.laureano.recordatorios.data.auth.EstadoSesion
import com.laureano.recordatorios.data.auth.Usuario
import com.laureano.recordatorios.data.preferencias.PreferenciasRepository
import com.laureano.recordatorios.domain.Orden
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
import com.laureano.recordatorios.ui.contenedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EstadoPrueba {
    data object Inactiva : EstadoPrueba
    data object EnCurso : EstadoPrueba
    data class Resultado(val lectura: String, val escritura: String) : EstadoPrueba
}

class PreferenciasViewModel(
    private val repositorio: PreferenciasRepository,
    private val auth: AuthRepository,
    private val recordatorios: RecordatorioRepository
) : ViewModel() {

    val preferencias: StateFlow<Preferencias> = repositorio.preferencias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Preferencias())

    val usuario: StateFlow<Usuario?> = auth.estado
        .map { (it as? EstadoSesion.Activa)?.usuario }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // La prueba de acceso ajeno solo existe en la variante de desarrollo
    val herramientasPrueba: Boolean = BuildConfig.HERRAMIENTAS_PRUEBA

    private val _uidAjeno = MutableStateFlow("")
    val uidAjeno: StateFlow<String> = _uidAjeno.asStateFlow()

    private val _prueba = MutableStateFlow<EstadoPrueba>(EstadoPrueba.Inactiva)
    val prueba: StateFlow<EstadoPrueba> = _prueba.asStateFlow()

    private val _cerrando = MutableStateFlow(false)
    val cerrando: StateFlow<Boolean> = _cerrando.asStateFlow()

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

    fun onCerrarSesion() {
        if (_cerrando.value) return
        _cerrando.value = true
        viewModelScope.launch {
            // Primero se limpia la copia local sincronizada; después se cierra la sesión
            recordatorios.prepararCierreDeSesion()
            auth.cerrarSesion()
        }
    }

    fun onUidAjenoChange(valor: String) = _uidAjeno.update { valor.trim() }

    fun onProbarAccesoAjeno() {
        val uid = _uidAjeno.value
        if (uid.isEmpty() || _prueba.value is EstadoPrueba.EnCurso) return
        _prueba.value = EstadoPrueba.EnCurso
        viewModelScope.launch {
            val r = recordatorios.probarAccesoAjeno(uid)
            _prueba.value = EstadoPrueba.Resultado(r.lectura, r.escritura)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                PreferenciasViewModel(c.preferencias, c.auth, c.recordatorios)
            }
        }
    }
}
