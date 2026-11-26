package com.laureano.recordatorios

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.laureano.recordatorios.data.MonitorConectividad
import com.laureano.recordatorios.data.RecordatorioRepository
import com.laureano.recordatorios.data.RecordatorioRepositoryLocalFirst
import com.laureano.recordatorios.data.auth.AuthRepository
import com.laureano.recordatorios.data.auth.EstadoSesion
import com.laureano.recordatorios.data.auth.FirebaseAuthRepository
import com.laureano.recordatorios.data.local.BaseDatos
import com.laureano.recordatorios.data.preferencias.PreferenciasRepository
import com.laureano.recordatorios.data.preferencias.dataStorePreferencias
import com.laureano.recordatorios.data.remote.FirestoreFuenteRemota
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    // La configuración sale de firebase.properties (no está en el repositorio)
    private val firebase: FirebaseApp = FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .build()
    )

    private val firestore = FirebaseFirestore.getInstance(firebase).apply {
        // Room ya es la copia local: Firestore no guarda otra en disco
        firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings { }) }
    }

    val auth: AuthRepository = FirebaseAuthRepository(FirebaseAuth.getInstance(firebase), alcance)

    val preferencias = PreferenciasRepository(context.dataStorePreferencias)

    private val monitor = MonitorConectividad(context)

    private val uidActivo = auth.estado
        .map { (it as? EstadoSesion.Activa)?.usuario?.uid }
        .distinctUntilChanged()
        .stateIn(alcance, SharingStarted.Eagerly, (auth.estado.value as? EstadoSesion.Activa)?.usuario?.uid)

    val recordatorios: RecordatorioRepository = RecordatorioRepositoryLocalFirst(
        dao = BaseDatos.obtener(context).recordatorioDao(),
        remota = FirestoreFuenteRemota(firestore, BuildConfig.COLECCION_USUARIOS, monitor::hayRed),
        uidActivo = uidActivo,
        preferencias = preferencias.preferencias,
        alcance = alcance
    )

    init {
        // Sincroniza al iniciar sesión (otro dispositivo recupera sus datos) y cuando vuelve la red
        alcance.launch {
            combine(uidActivo, monitor.hayConexion) { uid, red -> uid to red }
                .collect { (uid, red) -> if (uid != null && red) recordatorios.sincronizar() }
        }
    }
}
