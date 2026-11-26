package com.laureano.recordatorios

import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.FormularioRecordatorio
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.validar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormularioTest {
    private val trabajo = Categoria(1, "Trabajo")
    private val categorias = listOf(trabajo)

    @Test
    fun formularioCompletoEsValido() {
        val formulario = FormularioRecordatorio("Pagar la luz", "", trabajo, Prioridad.Alta)
        assertFalse(formulario.validar(categorias).hayErrores)
    }

    @Test
    fun tituloCortoConEspaciosEsInvalido() {
        val errores = FormularioRecordatorio("  ab  ", "", trabajo, Prioridad.Alta).validar(categorias)
        assertNotNull(errores.titulo)
    }

    @Test
    fun descripcionDeMasDe200EsInvalida() {
        val errores = FormularioRecordatorio("Pagar la luz", "x".repeat(201), trabajo, Prioridad.Alta)
            .validar(categorias)
        assertNotNull(errores.descripcion)
    }

    @Test
    fun categoriaFueraDeLasOpcionesEsInvalida() {
        val otra = Categoria(9, "Otra")
        val errores = FormularioRecordatorio("Pagar la luz", "", otra, Prioridad.Alta).validar(categorias)
        assertEquals("La categoría no es válida", errores.categoria)
    }

    @Test
    fun faltanCategoriaYPrioridad() {
        val errores = FormularioRecordatorio(titulo = "Pagar la luz").validar(categorias)
        assertTrue(errores.categoria != null && errores.prioridad != null)
    }
}
