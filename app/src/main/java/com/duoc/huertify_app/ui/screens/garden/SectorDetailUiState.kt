package com.duoc.huertify_app.ui.screens.garden

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.SectorWithInfo

typealias SectorDetailState = SectorDetailUiState

/**
 * Estado UI para el detalle de un sector y sus cultivos.
 */
data class SectorDetailUiState(
    val sectorInfo: SectorWithInfo? = null,
    val crops: List<Crop> = emptyList(),
    val loaded: Boolean = false
) {
    val sector get() = sectorInfo?.sector
}
