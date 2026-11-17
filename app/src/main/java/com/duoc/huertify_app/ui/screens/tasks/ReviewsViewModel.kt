package com.duoc.huertify_app.ui.screens.tasks

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ReviewsViewModel(tasks: TaskRepository) : BaseViewModel() {
    val uiState: StateFlow<ReviewsUiState> = tasks.observeAll().map { all ->
        ReviewsUiState(
            pending = all.filter { it.task.status == TaskStatus.PENDIENTE_VALIDACION }.sortedBy { it.task.updatedAt },
            reviewed = all.filter { it.task.status == TaskStatus.VALIDADA || it.task.status == TaskStatus.RECHAZADA }
                .sortedByDescending { it.task.updatedAt }
                .take(20),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewsUiState(isLoading = true))

    val state = tasks.observeAll().map { all ->
        ReviewsUiState(
            pending = all.filter { it.task.status == TaskStatus.PENDIENTE_VALIDACION }.sortedBy { it.task.updatedAt },
            reviewed = all.filter { it.task.status == TaskStatus.VALIDADA || it.task.status == TaskStatus.RECHAZADA }
                .sortedByDescending { it.task.updatedAt }
                .take(20)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
