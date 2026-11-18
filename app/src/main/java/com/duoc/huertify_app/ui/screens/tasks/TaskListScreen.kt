package com.duoc.huertify_app.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.duoc.huertify_app.domain.model.TaskType
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatEpochDay
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
<<<<<<< HEAD:app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/TaskListScreen.kt
=======
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
                    (q.isEmpty() || listOfNotNull(item.task.title, item.task.type.label, item.sectorName, item.cropName)
                        .any { it.lowercase().contains(q) })
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
>>>>>>> 80f66d5 (backend con xampp y phpmyadmin):app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/PantallaListaTareas.kt

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
                    placeholder = { Text("Buscar por título, tipo, sector o cultivo") },
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
                    leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.error,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error
                    )
                )
                TaskStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.filter.status == status,
                        onClick = { vm.onStatus(status) },
                        label = { Text(status.label) },
                        leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(status.color())) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = status.color().copy(alpha = 0.2f),
                            selectedLabelColor = status.color()
                        )
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
                    itemsIndexed(tasks, key = { _, item -> item.task.id }) { index, item ->
                        var visible by remember(item.task.id) {
                            mutableStateOf(false)
                        }

                        LaunchedEffect(item.task.id) {
                            visible = true
                        }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 400,
                                    delayMillis = index * 100
                                )
                            )
                        ) {
                            TaskCard(item, showAssignee = true) {
                                onNavigate(Routes.taskDetail(item.task.id))
                            }
                        }
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
    val accent = if (overdue) MaterialTheme.colorScheme.error else task.status.color()
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja de color que indica el estado (rojo si está atrasada).
            Box(Modifier.fillMaxHeight().width(6.dp).background(accent))
            Column(Modifier.padding(14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(task.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.type.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
                StatusChip(if (overdue) "Atrasada · ${task.status.label}" else task.status.label, accent)
                HorizontalDivider()
                CardLine(Icons.Filled.Yard, listOfNotNull(item.sectorName ?: "Sin sector", item.cropName).joinToString(" · "))
                if (showAssignee) {
                    CardLine(Icons.Filled.Person, item.assigneeName + (task.groupName?.let { " · $it" } ?: ""))
                }
                CardLine(
                    if (overdue) Icons.Filled.Warning else Icons.Filled.Event,
                    "Programada: ${task.scheduledDate.formatEpochDay()}",
                    if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CardLine(icon: ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** Ícono representativo de cada tipo de tarea. */
fun TaskType.icon(): ImageVector = when (this) {
    TaskType.PREPARACION -> Icons.Filled.Agriculture
    TaskType.SIEMBRA -> Icons.Filled.Spa
    TaskType.TRASPLANTE -> Icons.Filled.SwapHoriz
    TaskType.RIEGO -> Icons.Filled.WaterDrop
    TaskType.LIMPIEZA -> Icons.Filled.CleaningServices
    TaskType.DESMALEZADO -> Icons.Filled.ContentCut
    TaskType.OBSERVACION -> Icons.Filled.Visibility
    TaskType.MANTENCION -> Icons.Filled.Build
    TaskType.REVISION_SUELO -> Icons.Filled.Landscape
    TaskType.REGISTRO_CRECIMIENTO -> Icons.AutoMirrored.Filled.TrendingUp
    TaskType.COSECHA -> Icons.Filled.ShoppingBasket
    TaskType.OTRO -> Icons.AutoMirrored.Filled.Assignment
}
