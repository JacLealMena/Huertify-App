package com.duoc.huertify_app.ui.screens.tasks

import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.domain.model.TaskStatus

typealias TaskListState = TaskListUiState

data class TaskFilter(
    val status: TaskStatus? = null,
    val student: User? = null,
    val onlyOverdue: Boolean = false,
    val query: String = ""
)

/**
 * Estado UI para el listado de tareas.
 */
data class TaskListUiState(
    val tasks: List<TaskWithNames>? = null,
    val students: List<User> = emptyList(),
    val filter: TaskFilter = TaskFilter()
)
