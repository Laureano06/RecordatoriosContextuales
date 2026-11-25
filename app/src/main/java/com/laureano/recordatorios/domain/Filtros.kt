package com.laureano.recordatorios.domain

sealed class FiltroEstado(val etiqueta: String) {
    data object Todos : FiltroEstado("Todos")
    data object Pendientes : FiltroEstado("Pendientes")
    data object Completados : FiltroEstado("Completados")
    data object Cancelados : FiltroEstado("Cancelados")
}

fun List<Recordatorio>.filtrarPorEstado(filtro: FiltroEstado): List<Recordatorio> =
    filter { recordatorio ->
        when (filtro) {
            FiltroEstado.Todos -> true
            FiltroEstado.Pendientes -> recordatorio.estado is EstadoRecordatorio.Pendiente
            FiltroEstado.Completados -> recordatorio.estado is EstadoRecordatorio.Completado
            FiltroEstado.Cancelados -> recordatorio.estado is EstadoRecordatorio.Cancelado
        }
    }

// null significa "cualquier prioridad"
fun List<Recordatorio>.filtrarPorPrioridad(prioridad: Prioridad?): List<Recordatorio> =
    if (prioridad == null) this else filter { it.prioridad == prioridad }

