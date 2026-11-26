package com.laureano.recordatorios.ui.preferencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laureano.recordatorios.R
import com.laureano.recordatorios.domain.Orden
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

@Composable
fun RutaPreferencias(
    onVolver: () -> Unit,
    viewModel: PreferenciasViewModel = viewModel(factory = PreferenciasViewModel.Factory)
) {
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    PantallaPreferencias(
        preferencias = preferencias,
        onVolver = onVolver,
        onCambiarTema = viewModel::onCambiarTema,
        onCambiarMostrarCompletados = viewModel::onCambiarMostrarCompletados,
        onCambiarOrden = viewModel::onCambiarOrden
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPreferencias(
    preferencias: Preferencias,
    onVolver: () -> Unit,
    onCambiarTema: (Tema) -> Unit,
    onCambiarMostrarCompletados: (Boolean) -> Unit,
    onCambiarOrden: (Orden) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_preferencias)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
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
                .padding(vertical = 8.dp)
        ) {
            GrupoOpciones(
                titulo = stringResource(R.string.pref_tema),
                opciones = Tema.entries,
                seleccionada = preferencias.tema,
                textoDe = { it.etiqueta },
                onSeleccionar = onCambiarTema
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferencias.mostrarCompletados,
                        role = Role.Switch,
                        onValueChange = onCambiarMostrarCompletados
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pref_mostrar_completados),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = preferencias.mostrarCompletados, onCheckedChange = null)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            GrupoOpciones(
                titulo = stringResource(R.string.pref_orden),
                opciones = Orden.entries,
                seleccionada = preferencias.orden,
                textoDe = { it.etiqueta },
                onSeleccionar = onCambiarOrden
            )
        }
    }
}

@Composable
private fun <T> GrupoOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T,
    textoDe: (T) -> String,
    onSeleccionar: (T) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        opciones.forEach { opcion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = opcion == seleccionada,
                        role = Role.RadioButton,
                        onClick = { onSeleccionar(opcion) }
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(selected = opcion == seleccionada, onClick = null)
                Text(textoDe(opcion), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaPreferenciasPreview() {
    RecordatoriosContextualesTheme {
        PantallaPreferencias(
            preferencias = Preferencias(),
            onVolver = {},
            onCambiarTema = {},
            onCambiarMostrarCompletados = {},
            onCambiarOrden = {}
        )
    }
}
