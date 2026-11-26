package com.laureano.recordatorios.data.preferencias

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.laureano.recordatorios.domain.Orden
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
import com.laureano.recordatorios.domain.leerPreferencias
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// El delegado garantiza una única instancia de DataStore para este archivo
val Context.dataStorePreferencias: DataStore<Preferences> by preferencesDataStore(name = "preferencias")

// Solo configuraciones simples. Los recordatorios se guardan en Room
class PreferenciasRepository(private val dataStore: DataStore<Preferences>) {

    private object Claves {
        val TEMA = stringPreferencesKey("tema")
        val MOSTRAR_COMPLETADOS = booleanPreferencesKey("mostrar_completados")
        val ORDEN = stringPreferencesKey("orden")
    }

    val preferencias: Flow<Preferencias> = dataStore.data
        .catch { error ->
            // Si el archivo no se puede leer, se usan los valores predeterminados
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { guardadas ->
            leerPreferencias(
                tema = guardadas[Claves.TEMA],
                mostrarCompletados = guardadas[Claves.MOSTRAR_COMPLETADOS],
                orden = guardadas[Claves.ORDEN]
            )
        }

    suspend fun cambiarTema(tema: Tema) {
        dataStore.edit { it[Claves.TEMA] = tema.name }
    }

    suspend fun cambiarMostrarCompletados(mostrar: Boolean) {
        dataStore.edit { it[Claves.MOSTRAR_COMPLETADOS] = mostrar }
    }

    suspend fun cambiarOrden(orden: Orden) {
        dataStore.edit { it[Claves.ORDEN] = orden.name }
    }
}
