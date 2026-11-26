package com.laureano.recordatorios.data.remote

// Contrato del servicio. Todo es nullable porque el servidor puede mandar datos incompletos:
// el mapeo valida y descarta lo que no sirve antes de guardarlo en Room
data class RecordatorioDto(
    val id: Long? = null,
    val uuid: String? = null,
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoriaId: Long? = null,
    val prioridad: String? = null,
    val estado: String? = null,
    val fechaEstado: Long? = null,
    val motivoCancelacion: String? = null,
    val fechaAviso: Long? = null,
    val ubicacion: String? = null,
    val actualizadoEn: Long? = null
)
