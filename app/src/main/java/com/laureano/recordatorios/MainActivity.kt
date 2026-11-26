package com.laureano.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
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
                NavegacionApp()
            }
        }
    }
}
