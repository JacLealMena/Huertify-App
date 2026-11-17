package com.duoc.huertify_app.ui.screens.garden

import java.time.LocalDate

typealias CropFormState = CropFormUiState

/**
 * Estado UI para el formulario de cultivo.
 */
data class CropFormUiState(
    val id: Long = 0,
    val name: String = "",
    val variety: String = "",
    val plantingDate: LocalDate? = null,
    val harvestDate: LocalDate? = null,
    val watering: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val varietyError: String? = null,
    val plantingError: String? = null,
    val harvestError: String? = null,
    val wateringError: String? = null,
    val notesError: String? = null,
    val saving: Boolean = false
) {
    val isEdit get() = id != 0L
    val hasErrors get() = listOf(nameError, varietyError, plantingError, harvestError, wateringError, notesError).any { it != null }
}
