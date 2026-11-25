package com.laureano.recordatorios.domain

import java.time.LocalDateTime

sealed class ResultadoValidacion {
    data object Valido : ResultadoValidacion()
    data class Invalido(val errores: List<String>) : ResultadoValidacion()
}

// Cada función devuelve null si el dato es válido, o el mensaje de error
fun validarTitulo(titulo: String, minimo: Int = 4, maximo: Int = 60): String? {
    val limpio = titulo.trim()
    return when {
        limpio.isEmpty() -> "El título es obligatorio"
        limpio.length < minimo -> "El título debe tener al menos $minimo caracteres"
        limpio.length > maximo -> "El título no puede superar los $maximo caracteres"
        else -> null
    }
}

fun validarCategoria(categoria: Categoria): String? =
    if (categoria.nombre.isBlank()) "La categoría debe tener un nombre" else null

fun validarDescripcion(descripcion: String?, maximo: Int = 200): String? =
    if ((descripcion?.length ?: 0) > maximo) {
        "La descripción no puede superar los $maximo caracteres"
    } else {
        null
    }

fun validarUbicacion(ubicacion: String?): String? =
    if (ubicacion != null && ubicacion.isBlank()) "La ubicación no puede quedar en blanco" else null

fun validarFechaAviso(
    fechaAviso: LocalDateTime?,
    estado: EstadoRecordatorio,
    ahora: LocalDateTime
): String? = when {
    fechaAviso == null -> null
    estado != EstadoRecordatorio.Pendiente -> null
    !fechaAviso.isAfter(ahora) -> "El aviso debe tener una fecha futura"
    else -> null
}

// Reúne todas las reglas y devuelve un único resultado
fun Recordatorio.validar(ahora: LocalDateTime): ResultadoValidacion {
    val errores = listOfNotNull(
        validarTitulo(titulo),
        validarCategoria(categoria),
        validarDescripcion(descripcion),
        validarUbicacion(ubicacion),
        validarFechaAviso(fechaAviso, estado, ahora)
    )
    return if (errores.isEmpty()) {
        ResultadoValidacion.Valido
    } else {
        ResultadoValidacion.Invalido(errores)
    }
}