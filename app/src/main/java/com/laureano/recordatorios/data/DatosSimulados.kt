package com.laureano.recordatorios.data

import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.EstadoPantalla
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import com.laureano.recordatorios.domain.ResultadoValidacion
import com.laureano.recordatorios.domain.validar
import java.time.LocalDateTime

object DatosSimulados {
    private val ahora: LocalDateTime = LocalDateTime.now()

    private val trabajo = Categoria(1, "Trabajo")
    private val estudio = Categoria(2, "Estudio")
    private val salud = Categoria(3, "Salud")
    private val hogar = Categoria(4, "Hogar")

    // List: conserva el orden. El último es inválido a propósito
    val recordatorios: List<Recordatorio> = listOf(
        Recordatorio(
            id = 1,
            titulo = "Enviar informe mensual",
            categoria = trabajo,
            prioridad = Prioridad.Alta,
            descripcion = "Incluir las métricas del último mes",
            fechaAviso = ahora.plusDays(1)
        ),
        Recordatorio(
            id = 2,
            titulo = "Reunión con el equipo",
            categoria = trabajo,
            prioridad = Prioridad.Media,
            ubicacion = "Oficina central"
        ),
        Recordatorio(
            id = 3,
            titulo = "Entregar Actividad 1",
            categoria = estudio,
            prioridad = Prioridad.Alta,
            fechaAviso = ahora.plusDays(2)
        ),
        Recordatorio(
            id = 4,
            titulo = "Repasar sealed classes",
            categoria = estudio,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Completado(ahora.minusDays(1))
        ),
        Recordatorio(
            id = 5,
            titulo = "Turno con el dentista",
            categoria = salud,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Cancelado("Se reprogramó"),
            ubicacion = "Consultorio"
        ),
        Recordatorio(
            id = 6,
            titulo = "Comprar vitaminas",
            categoria = salud,
            prioridad = Prioridad.Baja
        ),
        Recordatorio(
            id = 7,
            titulo = "Pagar la luz",
            categoria = hogar,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Completado(ahora.minusDays(3))
        ),
        Recordatorio(
            id = 8,
            titulo = "",
            categoria = hogar,
            prioridad = Prioridad.Baja
        )
    )

    // Set: las categorías repetidas quedan una sola vez
    val categorias: Set<Categoria> = recordatorios.map { it.categoria }.toSet()

    // Map: asocia cada categoría con un color
    val coloresPorCategoria: Map<Categoria, Long> = mapOf(
        trabajo to 0xFF1565C0,
        estudio to 0xFF6A1B9A,
        salud to 0xFF2E7D32,
        hogar to 0xFFBF360C
    )

    // Solo llegan a la pantalla los recordatorios que pasan las validaciones
    fun cargar(): EstadoPantalla {
        val validos = recordatorios.filter { it.validar(ahora) is ResultadoValidacion.Valido }
        return if (validos.isEmpty()) EstadoPantalla.Vacio else EstadoPantalla.Exito(validos)
    }
}
