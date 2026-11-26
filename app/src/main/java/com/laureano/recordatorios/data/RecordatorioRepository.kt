package com.laureano.recordatorios.data

import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import kotlinx.coroutines.flow.StateFlow

// Datos que el formulario manda a guardar (el id lo asigna el repositorio)
data class NuevoRecordatorio(
    val titulo: String,
    val descripcion: String?,
    val categoria: Categoria,
    val prioridad: Prioridad
)

// Los ViewModels dependen de esta interfaz, no de la implementación
interface RecordatorioRepository {
    val categorias: List<Categoria>

    // Cambia cada vez que se guarda algo, para que el listado se recargue
    val actualizaciones: StateFlow<Int>

    suspend fun obtenerTodos(): List<Recordatorio>
    suspend fun obtenerPorId(id: Long): Recordatorio?
    suspend fun guardar(nuevo: NuevoRecordatorio): Recordatorio
}
