package com.laureano.recordatorios.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Valores de la columna estadoSync: qué operación falta enviar al servicio
object EstadoSyncLocal {
    const val SINCRONIZADO = "SINCRONIZADO"
    const val PENDIENTE_CREAR = "PENDIENTE_CREAR"
    const val PENDIENTE_ACTUALIZAR = "PENDIENTE_ACTUALIZAR"
    const val PENDIENTE_ELIMINAR = "PENDIENTE_ELIMINAR"
}

@Entity(
    tableName = "recordatorios",
    // El uuid identifica el recordatorio en el dispositivo y en el servicio: evita duplicados
    indices = [Index(value = ["uuid"], unique = true)]
)
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val remotoId: Long? = null,
    val titulo: String,
    val descripcion: String? = null,
    val categoriaId: Long,
    val prioridad: String,
    val estado: String,
    val fechaEstado: Long? = null,
    val motivoCancelacion: String? = null,
    val fechaAviso: Long? = null,
    val ubicacion: String? = null,
    // Fecha de la última modificación (milisegundos). También sirve como versión
    val actualizadoEn: Long,
    val estadoSync: String,
    val ultimoError: String? = null,
    val intentos: Int = 0
)
