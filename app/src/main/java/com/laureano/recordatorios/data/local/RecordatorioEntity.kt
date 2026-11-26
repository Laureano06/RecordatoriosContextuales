package com.laureano.recordatorios.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Valores de la columna estadoSync: qué operación falta enviar a la nube
object EstadoSyncLocal {
    const val SINCRONIZADO = "SINCRONIZADO"
    const val PENDIENTE_CREAR = "PENDIENTE_CREAR"
    const val PENDIENTE_ACTUALIZAR = "PENDIENTE_ACTUALIZAR"
    const val PENDIENTE_ELIMINAR = "PENDIENTE_ELIMINAR"
}

@Entity(
    tableName = "recordatorios",
    indices = [
        // El uuid es también el id del documento en Firestore: evita duplicados
        Index(value = ["uuid"], unique = true),
        Index(value = ["uid"])
    ]
)
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Cuenta dueña del recordatorio (uid de Firebase Authentication)
    val uid: String,
    val uuid: String,
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
