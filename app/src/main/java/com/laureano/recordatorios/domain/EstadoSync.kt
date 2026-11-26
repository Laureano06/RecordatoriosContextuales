package com.laureano.recordatorios.domain

// Lo que la interfaz necesita saber de la sincronización de cada recordatorio
enum class EstadoSync(val etiqueta: String) {
    Sincronizado("Sincronizado"),
    Pendiente("Pendiente de sincronizar"),
    ConError("Error al sincronizar, se reintentará")
}
