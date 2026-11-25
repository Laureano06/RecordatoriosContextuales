package com.laureano.recordatorios.domain

sealed class Prioridad(val etiqueta: String) {
    data object Baja : Prioridad("Baja")
    data object Media : Prioridad("Media")
    data object Alta : Prioridad("Alta")
}