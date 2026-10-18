package com.duoc.huertify_app.ui.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.data.SeedData
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.huertifyViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordVisible: Boolean = false,
    val loading: Boolean = false
)

class LoginViewModel(private val auth: AuthRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null) }
    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun fillDemo(account: SeedData.DemoAccount) =
        _state.update { LoginUiState(email = account.email, password = account.password) }

    fun login() {
        val s = _state.value
        val emailError = Validators.email(s.email)
        val passwordError = Validators.password(s.password)
        _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
        if (emailError != null || passwordError != null) {
            showError("Revisa los campos marcados")
            return
        }
        _state.update { it.copy(loading = true) }
        // La navegación ocurre al cambiar la sesión (ver HuertifyNavHost).
        launchSafe(onFinally = { _state.update { it.copy(loading = false) } }) {
            auth.login(s.email, s.password)
        }
    }
}

@Composable
fun LoginScreen() {
    val vm = huertifyViewModel { c, _ -> LoginViewModel(c.authRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar)

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Filled.Yard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
            Text("Huertify", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Gestión colaborativa del huerto escolar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            ValidatedTextField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                label = "Correo electrónico",
                error = state.emailError,
                leadingIcon = Icons.Filled.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            ValidatedTextField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                label = "Contraseña",
                error = state.passwordError,
                leadingIcon = Icons.Filled.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingContent = {
                    IconButton(onClick = vm::togglePasswordVisibility) {
                        Icon(
                            if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            LoadingButton("Iniciar sesión", loading = state.loading, onClick = vm::login, icon = Icons.AutoMirrored.Filled.Login)

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cuentas de prueba (toca para completar)", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    SeedData.accounts.forEachIndexed { index, account ->
                        if (index > 0) HorizontalDivider()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.fillDemo(account) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text("${account.name} · ${account.role.label}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${account.email} / ${account.password}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
