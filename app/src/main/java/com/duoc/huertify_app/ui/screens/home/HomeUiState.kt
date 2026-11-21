package com.duoc.huertify_app.ui.screens.home

import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.domain.WeatherSummary
import com.duoc.huertify_app.domain.model.TaskStatus

/**
 * Estado UI para la información del clima.
 */
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val summary: WeatherSummary) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

/**
 * Estado UI principal del panel de inicio.
 */
data class HomeUiState(
    val user: User? = null,
    val sectorCount: Int = 0,
    val cropCount: Int = 0,
    val tasks: List<TaskWithNames> = emptyList(),
    val pendingValidation: Int = 0,
    val overdue: Int = 0,
    val openReports: Int = 0,
    val alertCount: Int = 0
) {
    fun countBy(status: TaskStatus) = tasks.count { it.task.status == status }
}
