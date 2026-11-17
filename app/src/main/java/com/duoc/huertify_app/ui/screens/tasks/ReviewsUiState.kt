package com.duoc.huertify_app.ui.screens.tasks

import com.duoc.huertify_app.data.local.TaskWithNames

typealias ReviewsState = ReviewsUiState

/**
 * Estado UI para la pantalla de revisiones de tareas.
 */
data class ReviewsUiState(
    val pending: List<TaskWithNames> = emptyList(),
    val reviewed: List<TaskWithNames> = emptyList(),
    val isLoading: Boolean = true
)
