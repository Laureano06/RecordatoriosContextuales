package com.laureano.recordatorios.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.laureano.recordatorios.R
import com.laureano.recordatorios.data.DatosSimulados
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.EstadoRecordatorio
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun LocalDateTime.formatear(): String = format(formatoFecha)

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDateTime.formatearHora(): String = format(formatoHora)

fun colorDeCategoria(categoria: Categoria): Color =
    DatosSimulados.coloresPorCategoria[categoria]?.let { Color(it) } ?: Color.DarkGray

@Composable
fun textoEstado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> stringResource(R.string.estado_pendiente)
    is EstadoRecordatorio.Completado ->
        stringResource(R.string.estado_completado, estado.fecha.formatear())
    is EstadoRecordatorio.Cancelado ->
        stringResource(
            R.string.estado_cancelado,
            estado.motivo ?: stringResource(R.string.sin_motivo)
        )
}
