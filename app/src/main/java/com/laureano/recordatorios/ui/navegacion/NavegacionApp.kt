package com.laureano.recordatorios.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.laureano.recordatorios.ui.crear.RutaCrear
import com.laureano.recordatorios.ui.detalle.RutaDetalle
import com.laureano.recordatorios.ui.listado.RutaListado

object Rutas {
    const val LISTADO = "listado"
    const val CREAR = "crear"
    const val ARG_ID = "id"
    const val DETALLE = "detalle/{$ARG_ID}"

    fun detalle(id: Long) = "detalle/$id"
}

// El NavController vive acá; las pantallas solo reciben lambdas
@Composable
fun NavegacionApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Rutas.LISTADO) {
        composable(Rutas.LISTADO) {
            RutaListado(
                onCrearRecordatorio = {
                    navController.navigate(Rutas.CREAR) { launchSingleTop = true }
                },
                onRecordatorioClick = { id ->
                    navController.navigate(Rutas.detalle(id)) { launchSingleTop = true }
                }
            )
        }
        composable(Rutas.CREAR) {
            // Al guardar o cancelar se vuelve al listado que ya estaba en la pila
            RutaCrear(
                onGuardar = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })
        ) {
            RutaDetalle(onVolver = { navController.popBackStack() })
        }
    }
}
