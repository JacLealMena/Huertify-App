package com.duoc.huertify_app.ui.screens.garden

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SectorDetailViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    val sectorId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_SECTOR_ID))

    val state: StateFlow<SectorDetailUiState> = combine(garden.observeSector(sectorId), garden.observeCrops(sectorId)) { sectorInfo, crops ->
        SectorDetailUiState(sectorInfo, crops, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SectorDetailUiState())

    fun deleteSector() {
        val sector = state.value.sector ?: return
        launchSafe {
            garden.deleteSector(sector, auth.requireUser())
            send(UiEvent.SuccessAndBack("Sector \"${sector.name}\" eliminado"))
        }
    }

    fun deleteCrop(crop: Crop) = launchSafe {
        garden.deleteCrop(crop, auth.requireUser())
        send(UiEvent.ShowMessage("Cultivo \"${crop.name}\" eliminado"))
    }
}
