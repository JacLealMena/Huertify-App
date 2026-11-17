package com.duoc.huertify_app.ui.screens.tasks

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class TaskListViewModel(tasks: TaskRepository, auth: AuthRepository) : BaseViewModel() {
    private val filter = MutableStateFlow(TaskFilter())

    val state: StateFlow<TaskListUiState> = combine(tasks.observeAll(), auth.observeStudents(), filter) { all, students, f ->
        val q = f.query.trim().lowercase()
        TaskListUiState(
            tasks = all.filter { item ->
                (f.status == null || item.task.status == f.status) &&
                    (f.student == null || item.task.assigneeId == f.student.id) &&
                    (!f.onlyOverdue || item.task.isOverdue()) &&
                    (q.isEmpty() || item.task.title.lowercase().contains(q) || item.task.type.label.lowercase().contains(q))
            },
            students = students,
            filter = f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun onStatus(status: TaskStatus?) = filter.update { it.copy(status = if (it.status == status) null else status) }
    fun onStudent(student: User?) = filter.update { it.copy(student = student) }
    fun onToggleOverdue() = filter.update { it.copy(onlyOverdue = !it.onlyOverdue) }
    fun onQuery(q: String) = filter.update { it.copy(query = q) }
}
