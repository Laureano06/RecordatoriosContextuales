package com.laureano.recordatorios.domain

// Categorías fijas de la aplicación; Room y el servicio guardan solo el id
object Categorias {
    val trabajo = Categoria(1, "Trabajo")
    val estudio = Categoria(2, "Estudio")
    val salud = Categoria(3, "Salud")
    val hogar = Categoria(4, "Hogar")

    val todas: List<Categoria> = listOf(trabajo, estudio, salud, hogar)

    fun porId(id: Long): Categoria? = todas.find { it.id == id }
}
