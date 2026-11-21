package com.duoc.huertify_app.ui.screens.reports

import com.duoc.huertify_app.data.local.ReportWithNames

/**
 * Estado UI para la lista de reportes de problemas.
 */
data class ReportListUiState(
    val reports: List<ReportWithNames>? = null,
    val isLoading: Boolean = true
)
