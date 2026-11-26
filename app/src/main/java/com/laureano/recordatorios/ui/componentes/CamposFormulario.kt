package com.laureano.recordatorios.ui.componentes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Campo de texto controlado: el valor y el error vienen de afuera
@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    maximo: Int? = null,
    unaLinea: Boolean = true,
    habilitado: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = mensajeError != null,
        enabled = habilitado,
        singleLine = unaLinea,
        minLines = if (unaLinea) 1 else 3,
        supportingText = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = mensajeError ?: "", modifier = Modifier.weight(1f))
                if (maximo != null) Text("${valor.length}/$maximo")
            }
        },
        // El error también queda disponible para el lector de pantalla
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (mensajeError != null) this.error(mensajeError) }
    )
}

// Grupo de opciones donde se elige una sola (categoría o prioridad)
@Composable
fun <T> SelectorOpciones(
    etiqueta: String,
    opciones: List<T>,
    seleccionada: T?,
    textoDe: (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    habilitado: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = if (mensajeError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            opciones.forEach { opcion ->
                FilterChip(
                    selected = opcion == seleccionada,
                    onClick = { onSeleccionar(opcion) },
                    label = { Text(textoDe(opcion)) },
                    enabled = habilitado,
                    modifier = Modifier.semantics { role = Role.RadioButton }
                )
            }
        }
        if (mensajeError != null) {
            Text(
                text = mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { this.error(mensajeError) }
            )
        }
    }
}
