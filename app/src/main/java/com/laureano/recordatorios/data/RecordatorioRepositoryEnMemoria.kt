package com.laureano.recordatorios.data

import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.Recordatorio
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

// Guarda los recordatorios en memoria y simula la demora de una red
class RecordatorioRepositoryEnMemoria(
    iniciales: List<Recordatorio>,
    override val categorias: List<Categoria>,
    private val demoraMs: Long = 800
) : RecordatorioRepository, ControlSimulacion {

    private val datos = MutableStateFlow(iniciales)

    private val _actualizaciones = MutableStateFlow(0)
    override val actualizaciones: StateFlow<Int> = _actualizaciones.asStateFlow()

    override var modo: ModoSimulacion = ModoSimulacion.Normal

    override suspend fun obtenerTodos(): List<Recordatorio> {
        delay(demoraMs)
        return when (modo) {
            ModoSimulacion.ErrorCarga -> throw IOException("No se pudieron cargar los recordatorios")
            ModoSimulacion.Vacio -> emptyList()
            ModoSimulacion.Normal, ModoSimulacion.ErrorGuardado -> datos.value
        }
    }

    override suspend fun obtenerPorId(id: Long): Recordatorio? {
        delay(demoraMs / 2)
        if (modo == ModoSimulacion.ErrorCarga) {
            throw IOException("No se pudo cargar el recordatorio")
        }
        return datos.value.find { it.id == id }
    }

    override suspend fun guardar(nuevo: NuevoRecordatorio): Recordatorio {
        delay(demoraMs)
        if (modo == ModoSimulacion.ErrorGuardado) {
            throw IOException("No se pudo guardar el recordatorio. Probá de nuevo.")
        }
        val creado = Recordatorio(
            id = (datos.value.maxOfOrNull { it.id } ?: 0) + 1,
            titulo = nuevo.titulo,
            categoria = nuevo.categoria,
            prioridad = nuevo.prioridad,
            descripcion = nuevo.descripcion
        )
        // El más nuevo queda primero en la lista
        datos.update { listOf(creado) + it }
        _actualizaciones.update { it + 1 }
        return creado
    }
}
