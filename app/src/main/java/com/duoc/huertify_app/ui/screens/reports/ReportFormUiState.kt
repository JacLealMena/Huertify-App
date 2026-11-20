package com.duoc.huertify_app.ui.screens.reports

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.domain.model.ProblemType

typealias ReportFormState = ReportFormUiState

/**
 * Estado UI para el formulario de reporte de problemas.
 */
data class ReportFormUiState(
    val type: ProblemType? = null,
    val description: String = "",
    val sector: Sector? = null,
    val crop: Crop? = null,
    val photoUri: String? = null,
    val sectors: List<Sector> = emptyList(),
    val crops: List<Crop> = emptyList(),
    val typeError: String? = null,
    val descriptionError: String? = null,
    val saving: Boolean = false
)
