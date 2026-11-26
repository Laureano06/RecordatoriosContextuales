package com.laureano.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {

    // Los eliminados pendientes de sincronizar no se muestran
    @Query("SELECT * FROM recordatorios WHERE estadoSync != 'PENDIENTE_ELIMINAR' ORDER BY actualizadoEn DESC")
    fun observarTodos(): Flow<List<RecordatorioEntity>>

    // Filtro por estado: se usa para ocultar los completados
    @Query(
        "SELECT * FROM recordatorios WHERE estadoSync != 'PENDIENTE_ELIMINAR' " +
            "AND estado != :estadoExcluido ORDER BY actualizadoEn DESC"
    )
    fun observarExcluyendoEstado(estadoExcluido: String): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND estadoSync != 'PENDIENTE_ELIMINAR'")
    fun observarPorId(id: Long): Flow<RecordatorioEntity?>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE estadoSync != 'SINCRONIZADO'")
    fun observarCantidadPendientes(): Flow<Int>

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun buscarPorId(id: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE uuid = :uuid")
    suspend fun buscarPorUuid(uuid: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE estadoSync != 'SINCRONIZADO' ORDER BY actualizadoEn")
    suspend fun pendientes(): List<RecordatorioEntity>

    @Query("SELECT * FROM recordatorios WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun sincronizados(): List<RecordatorioEntity>

    @Insert
    suspend fun insertar(recordatorio: RecordatorioEntity): Long

    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    // Si el registro cambió mientras se enviaba (otra versión), queda pendiente otra vez
    @Query(
        "UPDATE recordatorios SET remotoId = :remotoId, ultimoError = NULL, intentos = 0, " +
            "estadoSync = CASE " +
            "WHEN estadoSync = 'PENDIENTE_ELIMINAR' THEN estadoSync " +
            "WHEN actualizadoEn = :version THEN 'SINCRONIZADO' " +
            "ELSE 'PENDIENTE_ACTUALIZAR' END " +
            "WHERE id = :id"
    )
    suspend fun confirmarSincronizacion(id: Long, remotoId: Long, version: Long)

    // El servicio rechazó el dato: hay que revisarlo antes de reintentar
    @Query("UPDATE recordatorios SET ultimoError = :error, intentos = intentos + 1 WHERE id = :id")
    suspend fun registrarError(id: Long, error: String)

    // Falló por red o por el servidor: sigue pendiente y se reintenta solo
    @Query("UPDATE recordatorios SET intentos = intentos + 1 WHERE id = :id")
    suspend fun sumarIntento(id: Long)
}
