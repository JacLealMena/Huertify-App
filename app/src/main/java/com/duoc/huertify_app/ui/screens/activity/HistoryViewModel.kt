package com.duoc.huertify_app.ui.screens.activity

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.repository.ActivityRepository
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    auth: AuthRepository,
    activity: ActivityRepository
) : BaseViewModel() {

    val uiState: StateFlow<HistoryUiState> = auth.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            val flow = if (user.role == Role.ADMIN) {
                activity.observeAll()
            } else {
                activity.observeForUser(user.id)
            }
            flow.map { entries -> HistoryUiState(entries = entries, isLoading = false) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(isLoading = true)
        )
}
