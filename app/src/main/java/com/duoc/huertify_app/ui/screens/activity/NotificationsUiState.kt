package com.duoc.huertify_app.ui.screens.activity

import com.duoc.huertify_app.domain.Alert

/**
 * Estado UI para la pantalla de avisos / notificaciones.
 */
data class NotificationsUiState(
    val alerts: List<Alert>? = null,
    val isLoading: Boolean = true
)
