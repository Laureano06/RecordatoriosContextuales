package com.laureano.recordatorios

import com.laureano.recordatorios.domain.Orden
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
import com.laureano.recordatorios.domain.leerPreferencias
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenciasTest {

    @Test
    fun sinDatosGuardadosUsaLosPredeterminados() {
        assertEquals(Preferencias(), leerPreferencias(null, null, null))
    }

    @Test
    fun valoresGuardadosSeRespetan() {
        val prefs = leerPreferencias("Oscuro", false, "Prioridad")
        assertEquals(Preferencias(Tema.Oscuro, false, Orden.Prioridad), prefs)
    }

    @Test
    fun valorDesconocidoVuelveAlPredeterminadoSinPerderLosDemas() {
        val prefs = leerPreferencias("Violeta", false, "Aleatorio")
        assertEquals(Tema.Sistema, prefs.tema)
        assertEquals(false, prefs.mostrarCompletados)
        assertEquals(Orden.Recientes, prefs.orden)
    }
}
