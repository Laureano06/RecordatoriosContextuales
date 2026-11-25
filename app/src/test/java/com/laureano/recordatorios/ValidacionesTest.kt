package com.laureano.recordatorios

import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.domain.ResultadoValidacion
import com.laureano.recordatorios.domain.validar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ValidacionesTest {
    private val ahora = LocalDateTime.of(2026, 11, 25, 10, 0)

    private val base = Recordatorio(
        id = 1,
        titulo = "Entregar informe",
        categoria = Categoria(1, "Trabajo"),
        prioridad = Prioridad.Alta
    )

    @Test
    fun recordatorioCorrecto_esValido() {
        assertEquals(ResultadoValidacion.Valido, base.validar(ahora))
    }

    @Test
    fun tituloVacio_esInvalido() {
        assertEquals(
            ResultadoValidacion.Invalido(listOf("El título es obligatorio")),
            base.copy(titulo = "   ").validar(ahora)
        )
    }

    @Test
    fun avisoPasado_esInvalido() {
        assertEquals(
            ResultadoValidacion.Invalido(listOf("El aviso debe tener una fecha futura")),
            base.copy(fechaAviso = ahora.minusDays(1)).validar(ahora)
        )
    }
}
