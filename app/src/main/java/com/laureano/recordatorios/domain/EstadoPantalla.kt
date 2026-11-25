package com.laureano.recordatorios.domain

sealed class EstadoPantalla {
    data object Cargando : EstadoPantalla()
    data object Vacio : EstadoPantalla()
    data class Exito(val recordatorios: List<Recordatorio>) : EstadoPantalla()
    data class Error(val mensaje: String) : EstadoPantalla()
}