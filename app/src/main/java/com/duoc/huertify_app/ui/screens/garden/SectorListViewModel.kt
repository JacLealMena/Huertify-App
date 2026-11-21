package com.duoc.huertify_app.ui.screens.garden

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SectorListViewModel(garden: GardenRepository) : BaseViewModel() {
    val uiState: StateFlow<SectorListUiState> = garden.observeSectors()
        .map { sectors -> SectorListUiState(sectors = sectors, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SectorListUiState(isLoading = true)
        )

    val sectors = garden.observeSectors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
