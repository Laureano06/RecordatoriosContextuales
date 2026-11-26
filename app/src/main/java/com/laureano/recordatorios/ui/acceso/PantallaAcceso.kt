package com.laureano.recordatorios.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laureano.recordatorios.R
import com.laureano.recordatorios.ui.theme.RecordatoriosContextualesTheme

@Composable
fun RutaAcceso(
    motivoCierre: String?,
    viewModel: AccesoViewModel = viewModel(factory = AccesoViewModel.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PantallaAcceso(
        estado = estado,
        motivoCierre = motivoCierre,
        onEmailChange = viewModel::onEmailChange,
        onContraseniaChange = viewModel::onContraseniaChange,
        onCambiarModo = viewModel::onCambiarModo,
        onEnviar = viewModel::onEnviar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAcceso(
    estado: AccesoUiState,
    motivoCierre: String?,
    onEmailChange: (String) -> Unit,
    onContraseniaChange: (String) -> Unit,
    onCambiarModo: () -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val esRegistro = estado.modo == ModoAcceso.Registro
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(if (esRegistro) R.string.acceso_crear_cuenta else R.string.acceso_iniciar),
                style = MaterialTheme.typography.headlineSmall
            )
            if (motivoCierre != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = motivoCierre,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .padding(16.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
            OutlinedTextField(
                value = estado.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.acceso_email)) },
                singleLine = true,
                enabled = !estado.enCurso,
                isError = estado.errorEmail != null,
                supportingText = { Text(estado.errorEmail ?: "") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { estado.errorEmail?.let { this.error(it) } }
            )
            OutlinedTextField(
                value = estado.contrasenia,
                onValueChange = onContraseniaChange,
                label = { Text(stringResource(R.string.acceso_contrasenia)) },
                singleLine = true,
                enabled = !estado.enCurso,
                isError = estado.errorContrasenia != null,
                supportingText = {
                    Text(estado.errorContrasenia ?: if (esRegistro) stringResource(R.string.acceso_minimo) else "")
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { estado.errorContrasenia?.let { this.error(it) } }
            )
            val error = estado.estado as? EstadoAcceso.Error
            if (error != null) {
                Text(
                    text = error.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
            Button(
                onClick = onEnviar,
                enabled = estado.puedeEnviar,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.enCurso) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.acceso_en_curso),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    Text(stringResource(if (esRegistro) R.string.acceso_boton_registro else R.string.acceso_boton_iniciar))
                }
            }
            TextButton(
                onClick = onCambiarModo,
                enabled = !estado.enCurso,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(if (esRegistro) R.string.acceso_ya_tengo else R.string.acceso_no_tengo))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaAccesoPreview() {
    RecordatoriosContextualesTheme {
        PantallaAcceso(
            estado = AccesoUiState(email = "ana@ejemplo.com", estado = EstadoAcceso.Error("Email o contraseña incorrectos")),
            motivoCierre = null,
            onEmailChange = {},
            onContraseniaChange = {},
            onCambiarModo = {},
            onEnviar = {}
        )
    }
}
