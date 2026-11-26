package com.laureano.recordatorios.domain

// Valores que el usuario va cargando en el formulario
data class FormularioRecordatorio(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null
)

// null en un campo significa que no tiene error
data class ErroresFormulario(
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null,
    val prioridad: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(titulo, descripcion, categoria, prioridad).any { it != null }
}

fun FormularioRecordatorio.validar(categoriasValidas: List<Categoria>): ErroresFormulario =
    ErroresFormulario(
        titulo = validarTitulo(titulo),
        descripcion = validarDescripcion(descripcion),
        categoria = when {
            categoria == null -> "Elegí una categoría"
            categoria !in categoriasValidas -> "La categoría no es válida"
            else -> null
        },
        prioridad = if (prioridad == null) "Elegí una prioridad" else null
    )
