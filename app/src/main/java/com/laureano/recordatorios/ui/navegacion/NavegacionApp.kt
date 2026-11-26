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
import com.laureano.recordatorios.ui.preferencias.RutaPreferencias

object Rutas {
    const val LISTADO = "listado"
    const val ARG_ID = "id"
    const val SIN_ID = -1L

    // El mismo formulario sirve para crear (sin id) y para editar (con id)
    const val FORMULARIO = "formulario?$ARG_ID={$ARG_ID}"
    const val DETALLE = "detalle/{$ARG_ID}"
    const val PREFERENCIAS = "preferencias"

    fun crear() = "formulario"
    fun editar(id: Long) = "formulario?$ARG_ID=$id"
    fun detalle(id: Long) = "detalle/$id"
}

// El NavController vive acá; las pantallas solo reciben lambdas
@Composable
fun NavegacionApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Rutas.LISTADO) {
        composable(Rutas.LISTADO) {
            RutaListado(
                onCrearRecordatorio = {
                    navController.navigate(Rutas.crear()) { launchSingleTop = true }
                },
                onRecordatorioClick = { id ->
                    navController.navigate(Rutas.detalle(id)) { launchSingleTop = true }
                },
                onAbrirPreferencias = {
                    navController.navigate(Rutas.PREFERENCIAS) { launchSingleTop = true }
                }
            )
        }
        composable(
            route = Rutas.FORMULARIO,
            arguments = listOf(
                navArgument(Rutas.ARG_ID) {
                    type = NavType.LongType
                    defaultValue = Rutas.SIN_ID
                }
            )
        ) {
            // Al guardar o cancelar se vuelve a la pantalla anterior, sin duplicarla
            RutaCrear(
                onGuardar = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })
        ) {
            RutaDetalle(
                onVolver = { navController.popBackStack() },
                onEditar = { id ->
                    navController.navigate(Rutas.editar(id)) { launchSingleTop = true }
                }
            )
        }
        composable(Rutas.PREFERENCIAS) {
            RutaPreferencias(onVolver = { navController.popBackStack() })
        }
    }
}
