package com.laureano.recordatorios.domain

import java.time.LocalDateTime

data class Recordatorio(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
    val descripcion: String? = null,
    val fechaAviso: LocalDateTime? = null,
    val ubicacion: String? = null
)

// Devuelve una copia: el original no se modifica
fun Recordatorio.completar(fecha: LocalDateTime): Recordatorio =
    copy(estado = EstadoRecordatorio.Completado(fecha))