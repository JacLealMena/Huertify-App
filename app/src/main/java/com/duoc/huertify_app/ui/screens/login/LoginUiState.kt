package com.duoc.huertify_app.ui.screens.login

/**
 * Estado UI para la pantalla de inicio de sesión.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordVisible: Boolean = false,
    val loading: Boolean = false
)
