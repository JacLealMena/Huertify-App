package com.duoc.huertify_app.ui.screens.reports

import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.domain.model.Severity

typealias ReportFormState = ReportFormUiState

/**
 * Estado UI para el formulario de reporte de problemas.
 */
data class ReportFormUiState(
    val title: String = "",
    val description: String = "",
    val severity: Severity? = null,
    val sector: Sector? = null,
    val photoUri: String? = null,
    val sectors: List<Sector> = emptyList(),
    val titleError: String? = null,
    val descriptionError: String? = null,
    val severityError: String? = null,
    val saving: Boolean = false
)
