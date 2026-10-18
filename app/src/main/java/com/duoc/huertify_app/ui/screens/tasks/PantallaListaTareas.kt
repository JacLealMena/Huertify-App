package com.duoc.huertify_app.ui.screens.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatEpochDay
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class TaskFilter(
    val status: TaskStatus? = null,
    val student: User? = null,
    val onlyOverdue: Boolean = false,
    val query: String = ""
)

data class TaskListState(
    val tasks: List<TaskWithNames>? = null,
    val students: List<User> = emptyList(),
    val filter: TaskFilter = TaskFilter()
)

class TaskListViewModel(tasks: TaskRepository, auth: AuthRepository) : BaseViewModel() {
    private val filter = MutableStateFlow(TaskFilter())

    val state = combine(tasks.observeAll(), auth.observeStudents(), filter) { all, students, f ->
        val q = f.query.trim().lowercase()
        TaskListState(
            tasks = all.filter { item ->
                (f.status == null || item.task.status == f.status) &&
                    (f.student == null || item.task.assigneeId == f.student.id) &&
                    (!f.onlyOverdue || item.task.isOverdue()) &&
                    (q.isEmpty() || item.task.title.lowercase().contains(q) || item.task.type.label.lowercase().contains(q))
            },
            students = students,
            filter = f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListState())

    fun onStatus(status: TaskStatus?) = filter.update { it.copy(status = if (it.status == status) null else status) }
    fun onStudent(student: User?) = filter.update { it.copy(student = student) }
    fun onToggleOverdue() = filter.update { it.copy(onlyOverdue = !it.onlyOverdue) }
    fun onQuery(q: String) = filter.update { it.copy(query = q) }
}

@Composable
fun TaskListScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> TaskListViewModel(c.taskRepository, c.authRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val allStudents: User? = null

    Scaffold(
        topBar = { HuertifyTopBar("Tareas", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Routes.taskForm()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva tarea") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = vm::onQuery,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Buscar por título o tipo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownField(
                    label = "Responsable",
                    options = listOf(allStudents) + state.students,
                    selected = state.filter.student,
                    optionLabel = { it?.name ?: "Todos los estudiantes" },
                    onSelected = vm::onStudent,
                    error = null,
                    leadingIcon = Icons.Filled.Person,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filter.onlyOverdue,
                    onClick = vm::onToggleOverdue,
                    label = { Text("Atrasadas") },
                    leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, Modifier.size(18.dp)) }
                )
                TaskStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.filter.status == status,
                        onClick = { vm.onStatus(status) },
                        label = { Text(status.label) }
                    )
                }
            }
            val tasks = state.tasks ?: return@Column
            if (tasks.isEmpty()) {
                EmptyState(Icons.AutoMirrored.Filled.Assignment, "No hay tareas que coincidan con los filtros")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.task.id }) { item ->
                        TaskCard(item, showAssignee = true) { onNavigate(Routes.taskDetail(item.task.id)) }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(item: TaskWithNames, showAssignee: Boolean, onClick: () -> Unit) {
    val task = item.task
    val overdue = task.isOverdue()
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(task.status.label, task.status.color())
            }
            Text(
                "${task.type.label} · ${item.sectorName ?: "Sin sector"}" + if (showAssignee) " · ${item.assigneeName}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    if (overdue) Icons.Filled.Warning else Icons.Filled.Event,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    task.scheduledDate.formatEpochDay() + if (overdue) " · Atrasada" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
