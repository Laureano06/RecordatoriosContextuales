package com.laureano.recordatorios.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laureano.recordatorios.R
import com.laureano.recordatorios.data.DatosSimulados
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.EstadoPantalla
import com.laureano.recordatorios.domain.FiltroEstado
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.domain.filtrarPorEstado
import com.laureano.recordatorios.domain.filtrarPorPrioridad
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecordatorios(
    estado: EstadoPantalla,
    coloresPorCategoria: Map<Categoria, Long>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.titulo_pantalla)) })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (estado) {
                EstadoPantalla.Cargando -> CircularProgressIndicator()
                EstadoPantalla.Vacio -> Text(stringResource(R.string.lista_vacia))
                is EstadoPantalla.Error -> Text(estado.mensaje)
                is EstadoPantalla.Exito -> ContenidoRecordatorios(
                    recordatorios = estado.recordatorios,
                    coloresPorCategoria = coloresPorCategoria
                )
            }
        }
    }
}

@Composable
private fun ContenidoRecordatorios(
    recordatorios: List<Recordatorio>,
    coloresPorCategoria: Map<Categoria, Long>
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibles, key = { it.id }) { recordatorio ->
                    TarjetaRecordatorio(
                        recordatorio = recordatorio,
                        colorCategoria = coloresPorCategoria[recordatorio.categoria]
                            ?.let { Color(it) } ?: Color.DarkGray
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
private fun PantallaRecordatoriosPreview() {
    RecordatoriosContextualesTheme {
        PantallaRecordatorios(
            estado = DatosSimulados.cargar(),
            coloresPorCategoria = DatosSimulados.coloresPorCategoria
        )
    }
}