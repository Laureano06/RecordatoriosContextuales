package com.laureano.recordatorios.ui.preferencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laureano.recordatorios.R
import com.laureano.recordatorios.data.auth.Usuario
import com.laureano.recordatorios.domain.Orden
import com.laureano.recordatorios.domain.Preferencias
import com.laureano.recordatorios.domain.Tema
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

@Composable
fun RutaPreferencias(
    onVolver: () -> Unit,
    viewModel: PreferenciasViewModel = viewModel(factory = PreferenciasViewModel.Factory)
) {
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val cerrando by viewModel.cerrando.collectAsStateWithLifecycle()
    val uidAjeno by viewModel.uidAjeno.collectAsStateWithLifecycle()
    val prueba by viewModel.prueba.collectAsStateWithLifecycle()
    PantallaPreferencias(
        preferencias = preferencias,
        usuario = usuario,
        cerrando = cerrando,
        herramientasPrueba = viewModel.herramientasPrueba,
        uidAjeno = uidAjeno,
        prueba = prueba,
        onVolver = onVolver,
        onCambiarTema = viewModel::onCambiarTema,
        onCambiarMostrarCompletados = viewModel::onCambiarMostrarCompletados,
        onCambiarOrden = viewModel::onCambiarOrden,
        onCerrarSesion = viewModel::onCerrarSesion,
        onUidAjenoChange = viewModel::onUidAjenoChange,
        onProbarAccesoAjeno = viewModel::onProbarAccesoAjeno
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPreferencias(
    preferencias: Preferencias,
    usuario: Usuario?,
    cerrando: Boolean,
    herramientasPrueba: Boolean,
    uidAjeno: String,
    prueba: EstadoPrueba,
    onVolver: () -> Unit,
    onCambiarTema: (Tema) -> Unit,
    onCambiarMostrarCompletados: (Boolean) -> Unit,
    onCambiarOrden: (Orden) -> Unit,
    onCerrarSesion: () -> Unit,
    onUidAjenoChange: (String) -> Unit,
    onProbarAccesoAjeno: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_preferencias)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            painter = painterResource(R.drawable.ic_atras),
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (usuario != null) {
                Titulo(stringResource(R.string.pref_cuenta))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(usuario.email, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.pref_cerrar_sesion_ayuda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onCerrarSesion, enabled = !cerrando) {
                        Text(stringResource(if (cerrando) R.string.pref_cerrando else R.string.pref_cerrar_sesion))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            GrupoOpciones(
                titulo = stringResource(R.string.pref_tema),
                opciones = Tema.entries,
                seleccionada = preferencias.tema,
                textoDe = { it.etiqueta },
                onSeleccionar = onCambiarTema
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferencias.mostrarCompletados,
                        role = Role.Switch,
                        onValueChange = onCambiarMostrarCompletados
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pref_mostrar_completados),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = preferencias.mostrarCompletados, onCheckedChange = null)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            GrupoOpciones(
                titulo = stringResource(R.string.pref_orden),
                opciones = Orden.entries,
                seleccionada = preferencias.orden,
                textoDe = { it.etiqueta },
                onSeleccionar = onCambiarOrden
            )
            if (herramientasPrueba && usuario != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PruebaSeguridad(usuario, uidAjeno, prueba, onUidAjenoChange, onProbarAccesoAjeno)
            }
        }
    }
}

// Solo en la variante de desarrollo: intenta leer y escribir los datos de otra cuenta
@Composable
private fun PruebaSeguridad(
    usuario: Usuario,
    uidAjeno: String,
    prueba: EstadoPrueba,
    onUidAjenoChange: (String) -> Unit,
    onProbar: () -> Unit
) {
    Titulo(stringResource(R.string.prueba_titulo))
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectionContainer {
            Text(
                text = stringResource(R.string.prueba_mi_uid, usuario.uid),
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedTextField(
            value = uidAjeno,
            onValueChange = onUidAjenoChange,
            label = { Text(stringResource(R.string.prueba_uid_ajeno)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = onProbar,
            enabled = uidAjeno.isNotEmpty() && prueba !is EstadoPrueba.EnCurso
        ) {
            Text(stringResource(R.string.prueba_boton))
        }
        when (prueba) {
            EstadoPrueba.Inactiva -> Unit
            EstadoPrueba.EnCurso -> Text(stringResource(R.string.prueba_en_curso))
            is EstadoPrueba.Resultado -> {
                Text(prueba.lectura, color = MaterialTheme.colorScheme.error)
                Text(prueba.escritura, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun <T> GrupoOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T,
    textoDe: (T) -> String,
    onSeleccionar: (T) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        Titulo(titulo)
        opciones.forEach { opcion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = opcion == seleccionada,
                        role = Role.RadioButton,
                        onClick = { onSeleccionar(opcion) }
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(selected = opcion == seleccionada, onClick = null)
                Text(textoDe(opcion), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaPreferenciasPreview() {
    RecordatoriosContextualesTheme {
        PantallaPreferencias(
            preferencias = Preferencias(),
            usuario = Usuario("abc123", "ana@ejemplo.com"),
            cerrando = false,
            herramientasPrueba = true,
            uidAjeno = "",
            prueba = EstadoPrueba.Inactiva,
            onVolver = {},
            onCambiarTema = {},
            onCambiarMostrarCompletados = {},
            onCambiarOrden = {},
            onCerrarSesion = {},
            onUidAjenoChange = {},
            onProbarAccesoAjeno = {}
        )
    }
}
