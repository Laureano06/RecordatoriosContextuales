package com.laureano.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.laureano.recordatorios.ui.navegacion.NavegacionApp
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecordatoriosContextualesTheme {
                NavegacionApp()
            }
        }
    }
}
