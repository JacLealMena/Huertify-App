package com.duoc.huertify_app.ui.screens.garden

import com.duoc.huertify_app.data.local.Sector

/**
 * Estado UI para el listado de sectores.
 */
data class SectorListUiState(
    val sectors: List<Sector>? = null,
    val isLoading: Boolean = true
)
