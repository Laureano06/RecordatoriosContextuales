package com.laureano.recordatorios

import com.laureano.recordatorios.data.DatosSimulados
import com.laureano.recordatorios.domain.EstadoPantalla
import com.laureano.recordatorios.domain.FiltroEstado
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.filtrarPorEstado
import com.laureano.recordatorios.domain.filtrarPorPrioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColeccionesTest {
    private val validos = (DatosSimulados.cargar() as EstadoPantalla.Exito).recordatorios

    @Test
    fun recordatorioInvalido_noSeCarga() {
        assertEquals(8, DatosSimulados.recordatorios.size)
        assertEquals(7, validos.size)
    }

    @Test
    fun categorias_noSeRepiten() {
        assertEquals(4, DatosSimulados.categorias.size)
    }

    @Test
    fun filtros_devuelvenLoEsperado() {
        assertEquals(4, validos.filtrarPorEstado(FiltroEstado.Pendientes).size)
        assertEquals(3, validos.filtrarPorPrioridad(Prioridad.Alta).size)
    }

    @Test
    fun cadaCategoria_tieneColor() {
        assertTrue(DatosSimulados.categorias.all { it in DatosSimulados.coloresPorCategoria })
    }
}

