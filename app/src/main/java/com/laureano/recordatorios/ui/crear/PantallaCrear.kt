package com.laureano.recordatorios.ui.crear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laureano.recordatorios.R
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.FormularioRecordatorio
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.ui.componentes.CampoTexto
import com.laureano.recordatorios.ui.componentes.SelectorOpciones
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

@Composable
fun RutaCrear(
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    viewModel: CrearViewModel = viewModel(factory = CrearViewModel.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Cuando el guardado sale bien, se vuelve al listado
    LaunchedEffect(estado.envio) {
        if (estado.envio is EstadoEnvio.Exito) onGuardar()
    }

    PantallaCrear(
        estado = estado,
        categorias = viewModel.categorias,
        prioridades = viewModel.prioridades,
        onTituloChange = viewModel::onTituloChange,
        onDescripcionChange = viewModel::onDescripcionChange,
        onCategoriaChange = viewModel::onCategoriaChange,
        onPrioridadChange = viewModel::onPrioridadChange,
        onGuardar = viewModel::onGuardar,
        onCancelar = onCancelar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrear(
    estado: CrearUiState,
    categorias: List<Categoria>,
    prioridades: List<Prioridad>,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (Categoria) -> Unit,
    onPrioridadChange: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formulario = estado.formulario
    val editable = !estado.enviando

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (estado.esEdicion) R.string.editar_recordatorio else R.string.nuevo_recordatorio
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(
                            painter = painterResource(R.drawable.ic_atras),
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampoTexto(
                valor = formulario.titulo,
                onValorChange = onTituloChange,
                etiqueta = stringResource(R.string.campo_titulo),
                mensajeError = estado.errores.titulo,
                maximo = 60,
                habilitado = editable
            )
            CampoTexto(
                valor = formulario.descripcion,
                onValorChange = onDescripcionChange,
                etiqueta = stringResource(R.string.campo_descripcion),
                mensajeError = estado.errores.descripcion,
                maximo = 200,
                unaLinea = false,
                habilitado = editable
            )
            SelectorOpciones(
                etiqueta = stringResource(R.string.campo_categoria),
                opciones = categorias,
                seleccionada = formulario.categoria,
                textoDe = { it.nombre },
                onSeleccionar = onCategoriaChange,
                mensajeError = estado.errores.categoria,
                habilitado = editable
            )
            SelectorOpciones(
                etiqueta = stringResource(R.string.campo_prioridad),
                opciones = prioridades,
                seleccionada = formulario.prioridad,
                textoDe = { it.etiqueta },
                onSeleccionar = onPrioridadChange,
                mensajeError = estado.errores.prioridad,
                habilitado = editable
            )

            val envio = estado.envio
            if (envio is EstadoEnvio.Error) {
                Text(
                    text = envio.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    enabled = editable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancelar))
                }
                // Deshabilitado si el formulario no es válido o se está enviando
                Button(
                    onClick = onGuardar,
                    enabled = estado.puedeGuardar,
                    modifier = Modifier.weight(1f)
                ) {
                    if (estado.enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(R.string.guardando),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else {
                        Text(stringResource(R.string.guardar))
                    }
                }
            }
            if (!estado.esValido && editable) {
                Text(
                    text = stringResource(R.string.ayuda_formulario),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaCrearPreview() {
    RecordatoriosContextualesTheme {
        PantallaCrear(
            estado = CrearUiState(
                formulario = FormularioRecordatorio(titulo = "Pa"),
                errores = com.laureano.recordatorios.domain.ErroresFormulario(
                    titulo = "El título debe tener al menos 4 caracteres"
                )
            ),
            categorias = listOf(Categoria(1, "Trabajo"), Categoria(2, "Estudio")),
            prioridades = listOf(Prioridad.Alta, Prioridad.Media, Prioridad.Baja),
            onTituloChange = {},
            onDescripcionChange = {},
            onCategoriaChange = {},
            onPrioridadChange = {},
            onGuardar = {},
            onCancelar = {}
        )
    }
}
