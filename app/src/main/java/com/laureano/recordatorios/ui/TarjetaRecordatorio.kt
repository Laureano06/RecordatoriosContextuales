package com.laureano.recordatorios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laureano.recordatorios.R
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.Recordatorio
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun LocalDateTime.formatear(): String = format(formatoFecha)

@Composable
fun TarjetaRecordatorio(
    recordatorio: Recordatorio,
    colorCategoria: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = recordatorio.titulo,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recordatorio.categoria.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(colorCategoria, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = stringResource(
                        R.string.prioridad_valor,
                        recordatorio.prioridad.etiqueta
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = textoEstado(recordatorio.estado),
                style = MaterialTheme.typography.bodyMedium
            )
            // Los datos opcionales solo se muestran si existen
            recordatorio.descripcion?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            recordatorio.fechaAviso?.let {
                Text(
                    text = stringResource(R.string.aviso_valor, it.formatear()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            recordatorio.ubicacion?.let {
                Text(
                    text = stringResource(R.string.ubicacion_valor, it),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun textoEstado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> stringResource(R.string.estado_pendiente)
    is EstadoRecordatorio.Completado ->
        stringResource(R.string.estado_completado, estado.fecha.formatear())
    is EstadoRecordatorio.Cancelado ->
        stringResource(
            R.string.estado_cancelado,
            estado.motivo ?: stringResource(R.string.sin_motivo)
        )
}