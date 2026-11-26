package com.laureano.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laureano.recordatorios.data.auth.EstadoSesion
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
import com.laureano.recordatorios.ui.SesionViewModel
import com.laureano.recordatorios.ui.acceso.RutaAcceso
import com.laureano.recordatorios.ui.navegacion.NavegacionApp
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencias = (application as RecordatoriosApp).contenedor.preferencias.preferencias
        setContent {
            // null hasta leer DataStore: así no se ve un parpadeo con el tema equivocado
            val prefs: Preferencias? by preferencias.collectAsStateWithLifecycle(initialValue = null)
            val actuales = prefs ?: return@setContent
            val oscuro = when (actuales.tema) {
                Tema.Claro -> false
                Tema.Oscuro -> true
                Tema.Sistema -> isSystemInDarkTheme()
            }
            RecordatoriosContextualesTheme(darkTheme = oscuro) {
                RaizApp()
            }
        }
    }
}

@Composable
private fun RaizApp(sesion: SesionViewModel = viewModel(factory = SesionViewModel.Factory)) {
    val estado by sesion.estado.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        sesion.onVolverAPrimerPlano()
        onPauseOrDispose { }
    }

    when (val actual = estado) {
        is EstadoSesion.SinSesion -> RutaAcceso(motivoCierre = actual.motivo)
        // key(uid): con otra cuenta se crea una navegación nueva, sin ViewModels ni datos de la anterior
        is EstadoSesion.Activa -> key(actual.usuario.uid) { NavegacionApp() }
    }
}
