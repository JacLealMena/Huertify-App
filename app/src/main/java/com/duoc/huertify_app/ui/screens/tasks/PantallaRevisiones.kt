package com.duoc.huertify_app.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReviewsState(val pending: List<TaskWithNames> = emptyList(), val reviewed: List<TaskWithNames> = emptyList())

class ReviewsViewModel(tasks: TaskRepository) : BaseViewModel() {
    val state = tasks.observeAll().map { all ->
        ReviewsState(
            pending = all.filter { it.task.status == TaskStatus.PENDIENTE_VALIDACION }.sortedBy { it.task.updatedAt },
            reviewed = all.filter { it.task.status == TaskStatus.VALIDADA || it.task.status == TaskStatus.RECHAZADA }
                .sortedByDescending { it.task.updatedAt }
                .take(20)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ReviewsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> ReviewsViewModel(c.taskRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { HuertifyTopBar("Revisiones", onBack) }) { padding ->
        val s = state ?: return@Scaffold
        if (s.pending.isEmpty() && s.reviewed.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.FactCheck, "No hay entregas para revisar", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle("Pendientes de validación (${s.pending.size})") }
            if (s.pending.isEmpty()) {
                item { Text("Todas las entregas están revisadas ✔", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(s.pending, key = { it.task.id }) { TaskCard(it, showAssignee = true) { onNavigate(Routes.taskDetail(it.task.id)) } }
            if (s.reviewed.isNotEmpty()) {
                item { SectionTitle("Revisadas recientemente") }
                items(s.reviewed, key = { "r${it.task.id}" }) { TaskCard(it, showAssignee = true) { onNavigate(Routes.taskDetail(it.task.id)) } }
            }
        }
    }
}
