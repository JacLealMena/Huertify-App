package com.duoc.huertify_app.ui.screens.login

import com.duoc.huertify_app.data.SeedData
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LoginViewModel(private val auth: AuthRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

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
        launchSafe(onFinally = { _state.update { it.copy(loading = false) } }) {
            auth.login(s.email, s.password)
        }
    }
}
