package com.laureano.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laureano.recordatorios.R
import com.laureano.recordatorios.data.DatosSimulados
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.EstadoSync
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.ui.componentes.EtiquetaCategoria
import com.laureano.recordatorios.ui.componentes.IndicadorCarga
import com.laureano.recordatorios.ui.componentes.MensajeEstado
import com.laureano.recordatorios.ui.componentes.formatear
import com.laureano.recordatorios.ui.componentes.textoEstado
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

@Composable
fun RutaDetalle(
    onVolver: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val eliminado by viewModel.eliminado.collectAsStateWithLifecycle()

    // Después de eliminar se vuelve al listado
    LaunchedEffect(eliminado) {
        if (eliminado) onVolver()
    }

    PantallaDetalle(
        estado = if (eliminado) DetalleUiState.Cargando else estado,
        onVolver = onVolver,
        onReintentar = viewModel::onReintentar,
        onEditar = onEditar,
        onCompletar = viewModel::onCompletar,
        onEliminar = viewModel::onEliminar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalle(
    estado: DetalleUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    onEditar: (Long) -> Unit,
    onCompletar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    val encontrado = estado as? DetalleUiState.Encontrado

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_detalle)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            painter = painterResource(R.drawable.ic_atras),
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                },
                actions = {
                    if (encontrado != null) {
                        IconButton(onClick = { onEditar(encontrado.recordatorio.id) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_editar),
                                contentDescription = stringResource(R.string.editar)
                            )
                        }
                        IconButton(onClick = { confirmarEliminar = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_eliminar),
                                contentDescription = stringResource(R.string.eliminar)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (estado) {
                DetalleUiState.Cargando -> IndicadorCarga()
                DetalleUiState.NoEncontrado -> MensajeEstado(
                    mensaje = stringResource(R.string.no_encontrado),
                    textoAccion = stringResource(R.string.volver),
                    onAccion = onVolver
                )
                is DetalleUiState.Error -> MensajeEstado(
                    mensaje = estado.mensaje,
                    esError = true,
                    textoAccion = stringResource(R.string.reintentar),
                    onAccion = onReintentar
                )
                is DetalleUiState.Encontrado -> ContenidoDetalle(
                    recordatorio = estado.recordatorio,
                    onCompletar = onCompletar
                )
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(stringResource(R.string.confirmar_eliminar_titulo)) },
            text = { Text(stringResource(R.string.confirmar_eliminar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    onEliminar()
                }) { Text(stringResource(R.string.eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@Composable
private fun ContenidoDetalle(recordatorio: Recordatorio, onCompletar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = recordatorio.titulo, style = MaterialTheme.typography.headlineSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EtiquetaCategoria(recordatorio.categoria)
            Text(
                text = stringResource(R.string.prioridad_valor, recordatorio.prioridad.etiqueta),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilaDato(stringResource(R.string.dato_estado), textoEstado(recordatorio.estado))
                FilaDato(
                    stringResource(R.string.dato_descripcion),
                    recordatorio.descripcion ?: stringResource(R.string.sin_dato)
                )
                FilaDato(
                    stringResource(R.string.dato_aviso),
                    recordatorio.fechaAviso?.formatear() ?: stringResource(R.string.sin_dato)
                )
                FilaDato(
                    stringResource(R.string.dato_ubicacion),
                    recordatorio.ubicacion ?: stringResource(R.string.sin_dato)
                )
                FilaDato(stringResource(R.string.dato_sincronizacion), recordatorio.estadoSync.etiqueta)
            }
        }
        if (recordatorio.estado is EstadoRecordatorio.Pendiente) {
            Button(onClick = onCompletar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.marcar_completado))
            }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Column {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaDetallePreview() {
    RecordatoriosContextualesTheme {
        PantallaDetalle(
            estado = DetalleUiState.Encontrado(
                DatosSimulados.recordatorios.first().copy(estadoSync = EstadoSync.Pendiente)
            ),
            onVolver = {},
            onReintentar = {},
            onEditar = {},
            onCompletar = {},
            onEliminar = {}
        )
    }
}
