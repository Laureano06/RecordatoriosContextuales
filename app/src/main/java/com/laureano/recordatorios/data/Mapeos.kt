package com.laureano.recordatorios.data

import com.laureano.recordatorios.data.local.EstadoSyncLocal
import com.laureano.recordatorios.data.local.RecordatorioEntity
import com.laureano.recordatorios.data.remote.RecordatorioDto
import com.laureano.recordatorios.domain.Categoria
import com.laureano.recordatorios.domain.Categorias
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.EstadoSync
import com.laureano.recordatorios.domain.Prioridad
import com.laureano.recordatorios.domain.Recordatorio
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// Mapeos explícitos entre las tres capas: DTO (servicio) <-> Entity (Room) <-> dominio (pantallas)

const val ESTADO_PENDIENTE = "PENDIENTE"
const val ESTADO_COMPLETADO = "COMPLETADO"
const val ESTADO_CANCELADO = "CANCELADO"

fun LocalDateTime.aMilis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.aFecha(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun Prioridad.codigo(): String = when (this) {
    Prioridad.Alta -> "ALTA"
    Prioridad.Media -> "MEDIA"
    Prioridad.Baja -> "BAJA"
}

fun prioridadDesdeCodigo(codigo: String?): Prioridad? = when (codigo) {
    "ALTA" -> Prioridad.Alta
    "MEDIA" -> Prioridad.Media
    "BAJA" -> Prioridad.Baja
    else -> null
}

private fun EstadoRecordatorio.codigo(): String = when (this) {
    EstadoRecordatorio.Pendiente -> ESTADO_PENDIENTE
    is EstadoRecordatorio.Completado -> ESTADO_COMPLETADO
    is EstadoRecordatorio.Cancelado -> ESTADO_CANCELADO
}

private fun estadoDesde(codigo: String, fecha: Long?, motivo: String?): EstadoRecordatorio? =
    when (codigo) {
        ESTADO_PENDIENTE -> EstadoRecordatorio.Pendiente
        ESTADO_COMPLETADO -> EstadoRecordatorio.Completado((fecha ?: 0L).aFecha())
        ESTADO_CANCELADO -> EstadoRecordatorio.Cancelado(motivo)
        else -> null
    }

// Columnas de estado que se guardan en la Entity
data class ColumnasEstado(val estado: String, val fecha: Long?, val motivo: String?)

fun EstadoRecordatorio.aColumnas(): ColumnasEstado = when (this) {
    EstadoRecordatorio.Pendiente -> ColumnasEstado(codigo(), null, null)
    is EstadoRecordatorio.Completado -> ColumnasEstado(codigo(), fecha.aMilis(), null)
    is EstadoRecordatorio.Cancelado -> ColumnasEstado(codigo(), null, motivo)
}

// Entity -> dominio
fun RecordatorioEntity.aDominio(): Recordatorio = Recordatorio(
    id = id,
    titulo = titulo,
    categoria = Categorias.porId(categoriaId) ?: Categoria(categoriaId, "Sin categoría"),
    prioridad = prioridadDesdeCodigo(prioridad) ?: Prioridad.Media,
    estado = estadoDesde(estado, fechaEstado, motivoCancelacion) ?: EstadoRecordatorio.Pendiente,
    descripcion = descripcion,
    fechaAviso = fechaAviso?.aFecha(),
    ubicacion = ubicacion,
    estadoSync = when {
        estadoSync == EstadoSyncLocal.SINCRONIZADO -> EstadoSync.Sincronizado
        ultimoError != null -> EstadoSync.ConError
        else -> EstadoSync.Pendiente
    }
)

// Entity -> DTO (lo que se envía al servicio)
fun RecordatorioEntity.aDto(): RecordatorioDto = RecordatorioDto(
    id = remotoId,
    uuid = uuid,
    titulo = titulo,
    descripcion = descripcion,
    categoriaId = categoriaId,
    prioridad = prioridad,
    estado = estado,
    fechaEstado = fechaEstado,
    motivoCancelacion = motivoCancelacion,
    fechaAviso = fechaAviso,
    ubicacion = ubicacion,
    actualizadoEn = actualizadoEn
)

// DTO -> Entity. Devuelve null si al DTO le falta algo obligatorio o trae valores desconocidos
fun RecordatorioDto.aEntityOrNull(): RecordatorioEntity? {
    val remotoId = id ?: return null
    val uuid = uuid?.takeIf { it.isNotBlank() } ?: return null
    val titulo = titulo?.takeIf { it.isNotBlank() } ?: return null
    val categoriaId = categoriaId?.takeIf { Categorias.porId(it) != null } ?: return null
    val prioridad = prioridad?.takeIf { prioridadDesdeCodigo(it) != null } ?: return null
    val estado = estado?.takeIf { estadoDesde(it, fechaEstado, motivoCancelacion) != null }
        ?: return null
    return RecordatorioEntity(
        uuid = uuid,
        remotoId = remotoId,
        titulo = titulo,
        descripcion = descripcion,
        categoriaId = categoriaId,
        prioridad = prioridad,
        estado = estado,
        fechaEstado = fechaEstado,
        motivoCancelacion = motivoCancelacion,
        fechaAviso = fechaAviso,
        ubicacion = ubicacion,
        actualizadoEn = actualizadoEn ?: 0L,
        estadoSync = EstadoSyncLocal.SINCRONIZADO
    )
}
