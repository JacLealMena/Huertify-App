package com.duoc.huertify_app.ui.screens.activity

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.AlertBuilder
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
    auth: AuthRepository,
    tasks: TaskRepository,
    reports: ReportRepository
) : BaseViewModel() {

    val uiState: StateFlow<NotificationsUiState> = auth.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            val taskFlow = if (user.role == Role.ADMIN) {
                tasks.observeAll()
            } else {
                tasks.observeForUser(user.id)
            }
            combine(taskFlow, reports.observeAll()) { t, r ->
                AlertBuilder.build(user.role, t, r)
            }
        }
        .map { alerts -> NotificationsUiState(alerts = alerts, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationsUiState(isLoading = true)
        )
}
