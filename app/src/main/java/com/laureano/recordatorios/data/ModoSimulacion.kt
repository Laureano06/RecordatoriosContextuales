package com.laureano.recordatorios.data

// Permite forzar cada estado de las pantallas para probarlos
enum class ModoSimulacion(val etiqueta: String) {
    Normal("Datos normales"),
    Vacio("Lista vacía"),
    ErrorCarga("Error al cargar"),
    ErrorGuardado("Error al guardar")
}

interface ControlSimulacion {
    var modo: ModoSimulacion
}
