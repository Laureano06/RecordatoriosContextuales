package com.laureano.recordatorios.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

// Avisa cuando el dispositivo gana o pierde acceso a internet
class MonitorConectividad(context: Context) {

    private val conectividad = context.getSystemService(ConnectivityManager::class.java)

    private fun hayRed(): Boolean {
        val capacidades = conectividad.getNetworkCapabilities(conectividad.activeNetwork)
        return capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    val hayConexion: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(hayRed())
            }
        }
        trySend(hayRed())
        conectividad.registerDefaultNetworkCallback(callback)
        awaitClose { conectividad.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
