package com.laureano.recordatorios.domain

enum class Tema(val etiqueta: String) {
    Sistema("Según el sistema"),
    Claro("Claro"),
    Oscuro("Oscuro")
}

enum class Orden(val etiqueta: String) {
    Recientes("Más recientes primero"),
    Prioridad("Prioridad (alta primero)"),
    Titulo("Título (A a Z)")
}

// Modelo propio: la interfaz nunca ve las claves de DataStore
data class Preferencias(
    val tema: Tema = Tema.Sistema,
    val mostrarCompletados: Boolean = true,
    val orden: Orden = Orden.Recientes
)

// Convierte los valores guardados. Si falta un dato o es desconocido, usa el predeterminado
fun leerPreferencias(tema: String?, mostrarCompletados: Boolean?, orden: String?): Preferencias {
    val porDefecto = Preferencias()
    return Preferencias(
        tema = Tema.entries.find { it.name == tema } ?: porDefecto.tema,
        mostrarCompletados = mostrarCompletados ?: porDefecto.mostrarCompletados,
        orden = Orden.entries.find { it.name == orden } ?: porDefecto.orden
    )
}

private fun Prioridad.peso(): Int = when (this) {
    Prioridad.Alta -> 0
    Prioridad.Media -> 1
    Prioridad.Baja -> 2
}

// La lista llega de Room ordenada por fecha de actualización (más reciente primero)
fun List<Recordatorio>.ordenar(orden: Orden): List<Recordatorio> = when (orden) {
    Orden.Recientes -> this
    Orden.Prioridad -> sortedBy { it.prioridad.peso() }
    Orden.Titulo -> sortedBy { it.titulo.lowercase() }
}
