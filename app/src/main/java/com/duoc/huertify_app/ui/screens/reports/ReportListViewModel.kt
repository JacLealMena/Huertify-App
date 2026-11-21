package com.duoc.huertify_app.ui.screens.reports

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.domain.model.ReportStatus
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.UiEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ReportListViewModel(
    private val reports: ReportRepository,
    private val auth: AuthRepository
) : BaseViewModel() {

    val uiState: StateFlow<ReportListUiState> = reports.observeAll()
        .map { list -> ReportListUiState(reports = list, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportListUiState(isLoading = true)
        )

    fun updateStatus(item: ReportWithNames, status: ReportStatus, comment: String? = null) = launchSafe {
        reports.updateStatus(item.report.id, status, comment, auth.requireUser())
        send(UiEvent.ShowMessage("Reporte actualizado a ${status.label}"))
    }
}
