package com.duoc.huertify_app.ui.screens.reports

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.ReportRepository
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

    val reportList = reports.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleResolved(item: ReportWithNames) = launchSafe {
        val resolved = !item.report.resolved
        reports.setResolved(item.report, resolved, auth.requireUser())
        send(UiEvent.ShowMessage(if (resolved) "Reporte marcado como resuelto" else "Reporte reabierto"))
    }
}
