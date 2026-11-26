package com.laureano.recordatorios.ui.listado

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.laureano.recordatorios.data.ModoSimulacion
import com.laureano.recordatorios.domain.FiltroEstado
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.domain.filtrarPorEstado
import com.laureano.recordatorios.domain.filtrarPorPrioridad
import com.laureano.recordatorios.ui.componentes.IndicadorCarga
import com.laureano.recordatorios.ui.componentes.MensajeEstado
import com.laureano.recordatorios.ui.componentes.TarjetaRecordatorio
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
    viewModel: ListadoViewModel = viewModel(factory = ListadoViewModel.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val modo by viewModel.modo.collectAsStateWithLifecycle()
    PantallaListado(
        estado = estado,
        modo = modo,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::onReintentar,
        onCambiarModo = viewModel::onCambiarModo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListado(
    estado: ListadoUiState,
    modo: ModoSimulacion,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onReintentar: () -> Unit,
    onCambiarModo: (ModoSimulacion) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_pantalla)) },
                actions = { MenuSimulacion(modo = modo, onCambiarModo = onCambiarModo) }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
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

    Column(modifier = Modifier.fillMaxSize()) {
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

// Menú para forzar cada estado y poder probarlos
@Composable
private fun MenuSimulacion(
    modo: ModoSimulacion,
    onCambiarModo: (ModoSimulacion) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_mas),
                contentDescription = stringResource(R.string.simular_estado)
            )
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Text(
                text = stringResource(R.string.simular_estado),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ModoSimulacion.entries.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.etiqueta) },
                    leadingIcon = { RadioButton(selected = opcion == modo, onClick = null) },
                    onClick = {
                        abierto = false
                        onCambiarModo(opcion)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaListadoDatosPreview() {
    RecordatoriosContextualesTheme {
        PantallaListado(
            estado = ListadoUiState.Datos(DatosSimulados.recordatorios.take(5)),
            modo = ModoSimulacion.Normal,
            onCrearRecordatorio = {},
            onRecordatorioClick = {},
            onReintentar = {},
            onCambiarModo = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaListadoErrorPreview() {
    RecordatoriosContextualesTheme {
        PantallaListado(
            estado = ListadoUiState.Error("No se pudieron cargar los recordatorios"),
            modo = ModoSimulacion.ErrorCarga,
            onCrearRecordatorio = {},
            onRecordatorioClick = {},
            onReintentar = {},
            onCambiarModo = {}
        )
    }
}
