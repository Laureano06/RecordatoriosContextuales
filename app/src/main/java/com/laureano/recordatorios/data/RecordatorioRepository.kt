package com.laureano.recordatorios.data

import com.laureano.recordatorios.data.remote.FalloRemoto
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

// Datos que el formulario manda a guardar (el id lo asigna Room)
data class DatosRecordatorio(
    val titulo: String,
    val descripcion: String?,
    val categoria: Categoria,
    val prioridad: Prioridad
)

sealed interface EstadoSincronizacion {
    data object Inactiva : EstadoSincronizacion
    data object EnCurso : EstadoSincronizacion
    data class Completada(val hora: LocalDateTime) : EstadoSincronizacion
    data class Fallida(val fallo: FalloRemoto, val hora: LocalDateTime) : EstadoSincronizacion
}

// Los ViewModels dependen de esta interfaz. Room es la fuente de verdad de las pantallas
interface RecordatorioRepository {
    val categorias: List<Categoria>
    val estadoSincronizacion: StateFlow<EstadoSincronizacion>

    // Lista lista para mostrar: aplica las preferencias de DataStore (completados y orden)
    fun observarListado(): Flow<List<Recordatorio>>
    fun observarRecordatorio(id: Long): Flow<Recordatorio?>
    fun observarCantidadPendientes(): Flow<Int>

    suspend fun crear(datos: DatosRecordatorio): Long
    suspend fun actualizar(id: Long, datos: DatosRecordatorio)
    suspend fun cambiarEstado(id: Long, estado: EstadoRecordatorio)
    suspend fun eliminar(id: Long)

    // Envía los cambios pendientes y trae los del servicio
    suspend fun sincronizar(): EstadoSincronizacion
}
