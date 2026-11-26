package com.laureano.recordatorios.data.remote

// Documento de Firestore en users/{uid}/reminders/{uuid}.
// El id del documento es el uuid del recordatorio: un reintento escribe el mismo documento
data class RecordatorioRemoto(
    val uid: String,
    val uuid: String,
    val titulo: String,
    val descripcion: String?,
    val categoriaId: Long,
    val prioridad: String,
    val estado: String,
    val fechaEstado: Long?,
    val motivoCancelacion: String?,
    val fechaAviso: Long?,
    val ubicacion: String?,
    val actualizadoEn: Long
)

// Mapeo manual (sin reflexión), así R8 puede optimizar el release sin romper nada
fun RecordatorioRemoto.aMapa(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "uuid" to uuid,
    "titulo" to titulo,
    "descripcion" to descripcion,
    "categoriaId" to categoriaId,
    "prioridad" to prioridad,
    "estado" to estado,
    "fechaEstado" to fechaEstado,
    "motivoCancelacion" to motivoCancelacion,
    "fechaAviso" to fechaAviso,
    "ubicacion" to ubicacion,
    "actualizadoEn" to actualizadoEn
)

// Devuelve null si el documento no tiene la forma esperada
fun remotoDesdeMapa(datos: Map<String, Any?>): RecordatorioRemoto? {
    fun texto(clave: String) = datos[clave] as? String
    fun numero(clave: String) = (datos[clave] as? Number)?.toLong()
    return RecordatorioRemoto(
        uid = texto("uid") ?: return null,
        uuid = texto("uuid") ?: return null,
        titulo = texto("titulo") ?: return null,
        descripcion = texto("descripcion"),
        categoriaId = numero("categoriaId") ?: return null,
        prioridad = texto("prioridad") ?: return null,
        estado = texto("estado") ?: return null,
        fechaEstado = numero("fechaEstado"),
        motivoCancelacion = texto("motivoCancelacion"),
        fechaAviso = numero("fechaAviso"),
        ubicacion = texto("ubicacion"),
        actualizadoEn = numero("actualizadoEn") ?: 0L
    )
}
