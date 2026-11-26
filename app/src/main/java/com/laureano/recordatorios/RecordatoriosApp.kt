package com.laureano.recordatorios

import android.app.Application
import android.content.Context
import com.laureano.recordatorios.data.MonitorConectividad
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.data.RecordatorioRepositoryLocalFirst
import com.laureano.recordatorios.data.local.BaseDatos
import com.laureano.recordatorios.data.preferencias.PreferenciasRepository
import com.laureano.recordatorios.data.preferencias.dataStorePreferencias
import com.laureano.recordatorios.data.remote.ClienteRemoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class RecordatoriosApp : Application() {
    lateinit var contenedor: ContenedorApp
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorApp(this)
    }
}

// Crea una sola vez cada dependencia y las comparte con los ViewModels
class ContenedorApp(context: Context) {

    // Vive mientras vive la aplicación: las sincronizaciones no dependen de una pantalla
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferencias = PreferenciasRepository(context.dataStorePreferencias)

    val recordatorios: RecordatorioRepository = RecordatorioRepositoryLocalFirst(
        dao = BaseDatos.obtener(context).recordatorioDao(),
        api = ClienteRemoto.crearApi(
            urlBase = BuildConfig.API_URL_BASE,
            registrarPeticiones = BuildConfig.DEBUG
        ),
        preferencias = preferencias.preferencias,
        alcance = alcance
    )

    init {
        // Al abrir la app y cada vez que vuelve la conexión se sincroniza
        alcance.launch {
            MonitorConectividad(context).hayConexion
                .filter { it }
                .collect { recordatorios.sincronizar() }
        }
    }
}
