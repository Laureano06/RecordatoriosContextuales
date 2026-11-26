package com.laureano.recordatorios.ui.listado

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laureano.recordatorios.R
import com.laureano.recordatorios.data.DatosSimulados
import com.laureano.recordatorios.data.EstadoSincronizacion
import com.laureano.recordatorios.domain.FiltroEstado
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.domain.filtrarPorEstado
import com.laureano.recordatorios.domain.filtrarPorPrioridad
import com.laureano.recordatorios.ui.componentes.IndicadorCarga
import com.laureano.recordatorios.ui.componentes.MensajeEstado
import com.laureano.recordatorios.ui.componentes.TarjetaRecordatorio
import com.laureano.recordatorios.ui.componentes.formatearHora
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

private val filtrosEstado: List<FiltroEstado> = listOf(
    FiltroEstado.Todos,
    FiltroEstado.Pendientes,
    FiltroEstado.Completados,
    FiltroEstado.Cancelados
)

// null significa "cualquier prioridad"
private val filtrosPrioridad: List<Prioridad?> = listOf(
    null,
    Prioridad.Alta,
    Prioridad.Media,
    Prioridad.Baja
)

// Conecta el ViewModel con la pantalla
@Composable
fun RutaListado(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onAbrirPreferencias: () -> Unit,
    viewModel: ListadoViewModel = viewModel(factory = ListadoViewModel.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val sincronizacion by viewModel.sincronizacion.collectAsStateWithLifecycle()
    PantallaListado(
        estado = estado,
        sincronizacion = sincronizacion,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onAbrirPreferencias = onAbrirPreferencias,
        onReintentar = viewModel::onReintentar,
        onSincronizar = viewModel::onSincronizar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListado(
    estado: ListadoUiState,
    sincronizacion: InfoSincronizacion,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onAbrirPreferencias: () -> Unit,
    onReintentar: () -> Unit,
    onSincronizar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_pantalla)) },
                actions = {
                    IconButton(onClick = onSincronizar) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sincronizar),
                            contentDescription = stringResource(R.string.sincronizar)
                        )
                    }
                    IconButton(onClick = onAbrirPreferencias) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ajustes),
                            contentDescription = stringResource(R.string.titulo_preferencias)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCrearRecordatorio,
                icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                text = { Text(stringResource(R.string.nuevo_recordatorio)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FranjaSincronizacion(sincronizacion = sincronizacion, onSincronizar = onSincronizar)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // when exhaustivo: si se agrega un estado nuevo, no compila hasta manejarlo
                when (estado) {
                    ListadoUiState.Cargando -> IndicadorCarga()
                    ListadoUiState.Vacio -> MensajeEstado(
                        mensaje = stringResource(R.string.lista_vacia),
                        textoAccion = stringResource(R.string.accion_crear),
                        onAccion = onCrearRecordatorio
                    )
                    is ListadoUiState.Error -> MensajeEstado(
                        mensaje = estado.mensaje,
                        esError = true,
                        textoAccion = stringResource(R.string.reintentar),
                        onAccion = onReintentar
                    )
                    is ListadoUiState.Datos -> ContenidoListado(
                        recordatorios = estado.recordatorios,
                        onRecordatorioClick = onRecordatorioClick
                    )
                }
            }
        }
    }
}

// Muestra si los datos locales están al día con el servicio
@Composable
private fun FranjaSincronizacion(
    sincronizacion: InfoSincronizacion,
    onSincronizar: () -> Unit
) {
    val pendientes = sincronizacion.pendientes
    val textoPendientes =
        pluralStringResource(R.plurals.cambios_pendientes, pendientes, pendientes)
    val estado = sincronizacion.estado
    val esError = estado is EstadoSincronizacion.Fallida
    val texto = when (estado) {
        EstadoSincronizacion.Inactiva ->
            if (pendientes > 0) textoPendientes else stringResource(R.string.sync_inactiva)
        EstadoSincronizacion.EnCurso -> stringResource(R.string.sync_en_curso)
        is EstadoSincronizacion.Completada ->
            if (pendientes > 0) {
                textoPendientes
            } else {
                stringResource(R.string.sync_completa, estado.hora.formatearHora())
            }
        is EstadoSincronizacion.Fallida ->
            if (pendientes > 0) "${estado.fallo.mensaje}. $textoPendientes" else estado.fallo.mensaje
    }
    val fondo = if (esError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val colorTexto = if (esError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (estado is EstadoSincronizacion.EnCurso) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        Text(
            text = texto,
            color = colorTexto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        )
        if (esError) {
            TextButton(onClick = onSincronizar) { Text(stringResource(R.string.reintentar)) }
        }
    }
}

@Composable
private fun ContenidoListado(
    recordatorios: List<Recordatorio>,
    onRecordatorioClick: (Long) -> Unit
) {
    // rememberSaveable conserva los filtros al girar el dispositivo
    var indiceEstado by rememberSaveable { mutableIntStateOf(0) }
    var indicePrioridad by rememberSaveable { mutableIntStateOf(0) }

    val visibles = recordatorios
        .filtrarPorEstado(filtrosEstado[indiceEstado])
        .filtrarPorPrioridad(filtrosPrioridad[indicePrioridad])

    val textoTodas = stringResource(R.string.todas_las_prioridades)

    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        FilaFiltros(
            etiquetas = filtrosEstado.map { it.etiqueta },
            seleccionado = indiceEstado,
            onSeleccionar = { indiceEstado = it }
        )
        FilaFiltros(
            etiquetas = filtrosPrioridad.map { it?.etiqueta ?: textoTodas },
            seleccionado = indicePrioridad,
            onSeleccionar = { indicePrioridad = it }
        )
        if (visibles.isEmpty()) {
            Text(
                text = stringResource(R.string.sin_resultados),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                // Espacio abajo para que el botón flotante no tape la última tarjeta
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibles, key = { it.id }) { recordatorio ->
                    TarjetaRecordatorio(
                        recordatorio = recordatorio,
                        onClick = { onRecordatorioClick(recordatorio.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaFiltros(
    etiquetas: List<String>,
    seleccionado: Int,
    onSeleccionar: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        etiquetas.forEachIndexed { indice, etiqueta ->
            FilterChip(
                selected = indice == seleccionado,
                onClick = { onSeleccionar(indice) },
                label = { Text(etiqueta) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaListadoPreview() {
    RecordatoriosContextualesTheme {
        PantallaListado(
            estado = ListadoUiState.Datos(DatosSimulados.recordatorios.take(5)),
            sincronizacion = InfoSincronizacion(pendientes = 2),
            onCrearRecordatorio = {},
            onRecordatorioClick = {},
            onAbrirPreferencias = {},
            onReintentar = {},
            onSincronizar = {}
        )
    }
}
