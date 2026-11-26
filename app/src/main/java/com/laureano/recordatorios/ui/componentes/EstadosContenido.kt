package com.laureano.recordatorios.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laureano.recordatorios.R

// Se usa en el listado y en el detalle mientras llegan los datos
@Composable
fun IndicadorCarga(modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.cargando)
    CircularProgressIndicator(
        modifier = modifier.semantics { contentDescription = descripcion }
    )
}

// Mensaje para los estados vacío y error, con una acción opcional
@Composable
fun MensajeEstado(
    mensaje: String,
    modifier: Modifier = Modifier,
    esError: Boolean = false,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = mensaje,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = if (esError) MaterialTheme.colorScheme.error else Color.Unspecified,
            // El lector de pantalla anuncia el error apenas aparece
            modifier = Modifier.semantics {
                if (esError) liveRegion = LiveRegionMode.Polite
            }
        )
        if (textoAccion != null && onAccion != null) {
            Button(onClick = onAccion) { Text(textoAccion) }
        }
    }
}
