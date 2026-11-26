package com.laureano.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Todas las consultas filtran por uid: una cuenta nunca lee filas de otra
@Dao
interface RecordatorioDao {

    @Query(
        "SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync != 'PENDIENTE_ELIMINAR' " +
            "ORDER BY actualizadoEn DESC"
    )
    fun observarTodos(uid: String): Flow<List<RecordatorioEntity>>

    // Filtro por estado: se usa para ocultar los completados
    @Query(
        "SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync != 'PENDIENTE_ELIMINAR' " +
            "AND estado != :estadoExcluido ORDER BY actualizadoEn DESC"
    )
    fun observarExcluyendoEstado(uid: String, estadoExcluido: String): Flow<List<RecordatorioEntity>>

    @Query(
        "SELECT * FROM recordatorios WHERE id = :id AND uid = :uid " +
            "AND estadoSync != 'PENDIENTE_ELIMINAR'"
    )
    fun observarPorId(id: Long, uid: String): Flow<RecordatorioEntity?>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE uid = :uid AND estadoSync != 'SINCRONIZADO'")
    fun observarCantidadPendientes(uid: String): Flow<Int>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND uid = :uid")
    suspend fun buscarPorId(id: Long, uid: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE uuid = :uuid")
    suspend fun buscarPorUuid(uuid: String): RecordatorioEntity?

    @Query(
        "SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync != 'SINCRONIZADO' " +
            "ORDER BY actualizadoEn"
    )
    suspend fun pendientes(uid: String): List<RecordatorioEntity>

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync = 'SINCRONIZADO'")
    suspend fun sincronizados(uid: String): List<RecordatorioEntity>

    @Insert
    suspend fun insertar(recordatorio: RecordatorioEntity): Long

    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    // Al cerrar sesión se borra lo que ya está en la nube; lo pendiente se conserva
    @Query("DELETE FROM recordatorios WHERE uid = :uid AND estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados(uid: String)

    // Si el registro cambió mientras se enviaba (otra versión), queda pendiente otra vez
    @Query(
        "UPDATE recordatorios SET ultimoError = NULL, intentos = 0, " +
            "estadoSync = CASE " +
            "WHEN estadoSync = 'PENDIENTE_ELIMINAR' THEN estadoSync " +
            "WHEN actualizadoEn = :version THEN 'SINCRONIZADO' " +
            "ELSE 'PENDIENTE_ACTUALIZAR' END " +
            "WHERE id = :id"
    )
    suspend fun confirmarSincronizacion(id: Long, version: Long)

    // La nube rechazó el dato: hay que revisarlo antes de reintentar
    @Query("UPDATE recordatorios SET ultimoError = :error, intentos = intentos + 1 WHERE id = :id")
    suspend fun registrarError(id: Long, error: String)

    // Falló por red o por el servicio: sigue pendiente y se reintenta solo
    @Query("UPDATE recordatorios SET intentos = intentos + 1 WHERE id = :id")
    suspend fun sumarIntento(id: Long)
}
