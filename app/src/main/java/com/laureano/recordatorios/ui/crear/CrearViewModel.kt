package com.laureano.recordatorios.ui.crear

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laureano.recordatorios.data.DatosRecordatorio
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.ErroresFormulario
import com.laureano.recordatorios.domain.FormularioRecordatorio
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.validar
import com.laureano.recordatorios.ui.contenedor
import com.laureano.recordatorios.ui.navegacion.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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
    val esEdicion: Boolean = false,
    val envio: EstadoEnvio = EstadoEnvio.Inactivo
) {
    val enviando: Boolean get() = envio is EstadoEnvio.Enviando
    val puedeGuardar: Boolean get() = esValido && !enviando
}

// Sirve para crear (sin id) y para editar (con el id que llega por la ruta)
class CrearViewModel(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val idEdicion: Long? =
        savedStateHandle.get<Long>(Rutas.ARG_ID)?.takeIf { it != Rutas.SIN_ID }

    val categorias: List<Categoria> = repositorio.categorias
    val prioridades: List<Prioridad> = listOf(Prioridad.Alta, Prioridad.Media, Prioridad.Baja)

    private val _uiState = MutableStateFlow(CrearUiState(esEdicion = idEdicion != null))
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    init {
        if (idEdicion != null) cargarParaEditar(idEdicion)
    }

    private fun cargarParaEditar(id: Long) {
        viewModelScope.launch {
            val actual = repositorio.observarRecordatorio(id).first() ?: return@launch
            actualizar(tocarTitulo = true) {
                FormularioRecordatorio(
                    titulo = actual.titulo,
                    descripcion = actual.descripcion.orEmpty(),
                    categoria = actual.categoria,
                    prioridad = actual.prioridad
                )
            }
        }
    }

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
        val datos = DatosRecordatorio(
            titulo = formulario.titulo.trim(),
            descripcion = formulario.descripcion.trim().ifBlank { null },
            categoria = categoria,
            prioridad = prioridad
        )

        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
        viewModelScope.launch {
            // Se guarda en Room; el envío al servicio lo resuelve el repositorio después
            val resultado = try {
                val id = idEdicion?.also { repositorio.actualizar(it, datos) }
                    ?: repositorio.crear(datos)
                EstadoEnvio.Exito(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EstadoEnvio.Error("No se pudo guardar en el dispositivo. Probá de nuevo.")
            }
            _uiState.update { it.copy(envio = resultado) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrearViewModel(createSavedStateHandle(), contenedor().recordatorios) }
        }
    }
}
