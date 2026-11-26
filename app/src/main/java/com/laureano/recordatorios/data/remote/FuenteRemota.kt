package com.laureano.recordatorios.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

// Lo que el repositorio necesita de la nube. Los ViewModels no conocen esta interfaz
interface FuenteRemota {
    suspend fun listar(uid: String): ResultadoRemoto<List<RecordatorioRemoto>>
    suspend fun guardar(recordatorio: RecordatorioRemoto): ResultadoRemoto<Unit>
    suspend fun eliminar(uid: String, uuid: String): ResultadoRemoto<Unit>
}

class FirestoreFuenteRemota(
    private val firestore: FirebaseFirestore,
    // "users" en producción y "dev_users" en desarrollo
    private val coleccionUsuarios: String,
    // Sin red, Firestore deja las escrituras en espera: se avisa enseguida en lugar de esperar
    private val hayRed: () -> Boolean = { true }
) : FuenteRemota {

    private suspend fun <T> conRed(operacion: suspend () -> T): ResultadoRemoto<T> =
        if (hayRed()) llamarNube(operacion = operacion) else ResultadoRemoto.Fallo(FalloRemoto.SinConexion)

    private fun recordatoriosDe(uid: String) =
        firestore.collection(coleccionUsuarios).document(uid).collection("reminders")

    override suspend fun listar(uid: String) = conRed {
        // Source.SERVER: Room ya es la copia local, acá interesa el dato del servidor
        recordatoriosDe(uid).get(Source.SERVER).await()
            .documents
            .mapNotNull { documento -> documento.data?.let(::remotoDesdeMapa) }
    }

    override suspend fun guardar(recordatorio: RecordatorioRemoto) = conRed {
        // set() sobre el uuid: crear y modificar escriben siempre el mismo documento
        recordatoriosDe(recordatorio.uid).document(recordatorio.uuid)
            .set(recordatorio.aMapa()).await()
        Unit
    }

    override suspend fun eliminar(uid: String, uuid: String) = conRed {
        recordatoriosDe(uid).document(uuid).delete().await()
        Unit
    }
}
