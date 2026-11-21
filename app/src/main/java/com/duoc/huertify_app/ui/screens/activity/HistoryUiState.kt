package com.duoc.huertify_app.ui.screens.activity

import com.duoc.huertify_app.data.local.ActivityWithUser

/**
 * Estado UI para la pantalla de historial de actividades.
 */
data class HistoryUiState(
    val entries: List<ActivityWithUser>? = null,
    val isLoading: Boolean = true
)
