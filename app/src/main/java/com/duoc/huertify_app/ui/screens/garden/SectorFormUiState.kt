package com.duoc.huertify_app.ui.screens.garden

typealias SectorFormState = SectorFormUiState

/**
 * Estado UI para el formulario de sector.
 */
data class SectorFormUiState(
    val id: Long = 0,
    val code: String = "",
    val name: String = "",
    val location: String = "",
    val area: String = "",
    val description: String = "",
    val codeError: String? = null,
    val nameError: String? = null,
    val locationError: String? = null,
    val areaError: String? = null,
    val descriptionError: String? = null,
    val saving: Boolean = false
) {
    val isEdit get() = id != 0L
}
