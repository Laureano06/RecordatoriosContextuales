package com.laureano.recordatorios.domain

import java.time.LocalDateTime

sealed class EstadoRecordatorio {
    data object Pendiente : EstadoRecordatorio()
    data class Completado(val fecha: LocalDateTime) : EstadoRecordatorio()
    data class Cancelado(val motivo: String?) : EstadoRecordatorio()
}