package com.duoc.huertify_app.ui.screens.garden

import com.duoc.huertify_app.data.local.MaintenanceWithSector
import com.duoc.huertify_app.data.local.Sector
import java.time.LocalDate

typealias MaintenanceState = MaintenanceUiState

/**
 * Estado para el formulario de programación de mantenimiento.
 */
data class MaintenanceForm(
    val open: Boolean = false,
    val sectorId: Long? = null,
    val title: String = "",
    val notes: String = "",
    val date: LocalDate? = null,
    val sectorError: String? = null,
    val titleError: String? = null,
    val dateError: String? = null
)

/**
 * Estado UI principal para la pantalla de mantenimientos.
 */
data class MaintenanceUiState(
    val items: List<MaintenanceWithSector>? = null,
    val sectors: List<Sector> = emptyList(),
    val form: MaintenanceForm = MaintenanceForm()
)
