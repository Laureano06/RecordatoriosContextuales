package com.laureano.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.Dependencias
import com.laureano.recordatorios.data.NuevoRecordatorio
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.ErroresFormulario
import com.laureano.recordatorios.domain.FormularioRecordatorio
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.validar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface EstadoEnvio {
    data object Inactivo : EstadoEnvio
    data object Enviando : EstadoEnvio
    data class Exito(val id: Long) : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

data class CrearUiState(
    val formulario: FormularioRecordatorio = FormularioRecordatorio(),
    val errores: ErroresFormulario = ErroresFormulario(),
    val esValido: Boolean = false,
    // El error del título se muestra recién cuando el usuario empezó a escribir
    val tituloTocado: Boolean = false,
    val envio: EstadoEnvio = EstadoEnvio.Inactivo
) {
    val enviando: Boolean get() = envio is EstadoEnvio.Enviando
    val puedeGuardar: Boolean get() = esValido && !enviando
}

class CrearViewModel(private val repositorio: RecordatorioRepository) : ViewModel() {

    val categorias: List<Categoria> = repositorio.categorias
    val prioridades: List<Prioridad> = listOf(Prioridad.Alta, Prioridad.Media, Prioridad.Baja)

    private val _uiState = MutableStateFlow(CrearUiState())
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    fun onTituloChange(valor: String) =
        actualizar(tocarTitulo = true) { copy(titulo = valor) }

    fun onDescripcionChange(valor: String) = actualizar { copy(descripcion = valor) }

    fun onCategoriaChange(valor: Categoria) = actualizar { copy(categoria = valor) }

    fun onPrioridadChange(valor: Prioridad) = actualizar { copy(prioridad = valor) }

    // Cada cambio produce un estado nuevo con las validaciones recalculadas
    private fun actualizar(
        tocarTitulo: Boolean = false,
        cambio: FormularioRecordatorio.() -> FormularioRecordatorio
    ) {
        _uiState.update { estado ->
            val formulario = estado.formulario.cambio()
            val errores = formulario.validar(categorias)
            val tocado = estado.tituloTocado || tocarTitulo
            estado.copy(
                formulario = formulario,
                errores = errores.copy(
                    titulo = errores.titulo.takeIf { tocado },
                    // Antes de elegir no se marca como error, solo se pide
                    categoria = errores.categoria.takeIf { formulario.categoria != null },
                    prioridad = errores.prioridad.takeIf { formulario.prioridad != null }
                ),
                esValido = !errores.hayErrores,
                tituloTocado = tocado,
                envio = if (estado.envio is EstadoEnvio.Error) EstadoEnvio.Inactivo else estado.envio
            )
        }
    }

    fun onGuardar() {
        val estado = _uiState.value
        if (!estado.puedeGuardar) return
        val formulario = estado.formulario
        val categoria = formulario.categoria ?: return
        val prioridad = formulario.prioridad ?: return

        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
        viewModelScope.launch {
            val resultado = try {
                val creado = repositorio.guardar(
                    NuevoRecordatorio(
                        titulo = formulario.titulo.trim(),
                        descripcion = formulario.descripcion.trim().ifBlank { null },
                        categoria = categoria,
                        prioridad = prioridad
                    )
                )
                EstadoEnvio.Exito(creado.id)
            } catch (e: IOException) {
                EstadoEnvio.Error(e.message ?: "No se pudo guardar")
            }
            _uiState.update { it.copy(envio = resultado) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrearViewModel(Dependencias.repositorio) }
        }
    }
}
