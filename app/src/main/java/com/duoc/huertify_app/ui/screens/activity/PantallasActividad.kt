package com.duoc.huertify_app.ui.screens.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.ActivityWithUser
import com.duoc.huertify_app.data.repository.ActivityRepository
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.Alert
import com.duoc.huertify_app.domain.AlertBuilder
import com.duoc.huertify_app.domain.AlertLevel
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// ---------- Historial ----------

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(auth: AuthRepository, activity: ActivityRepository) : BaseViewModel() {
    // El administrador ve toda la actividad; el estudiante solo la propia y la de sus tareas.
    val entries = auth.currentUser.filterNotNull().flatMapLatest { user ->
        if (user.role == Role.ADMIN) activity.observeAll() else activity.observeForUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, _ -> HistoryViewModel(c.authRepository, c.activityRepository) }
    val entries by vm.entries.collectAsStateWithLifecycle()

    Scaffold(topBar = { HuertifyTopBar("Historial de actividades", onBack) }) { padding ->
        val list = entries ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.History, "Aún no hay actividad registrada", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
            itemsIndexed(list, key = { _, entry -> entry.activity.id }) { index, entry ->
                var visible by remember(entry.activity.id) {
                    mutableStateOf(false)
                }

                LaunchedEffect(entry.activity.id) {
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
                    HistoryRow(entry)
                }
            }

        }
    }
}

@Composable
private fun HistoryRow(entry: ActivityWithUser) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(entry.activity.message, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${entry.userName} · ${entry.activity.timestamp.formatTimestamp()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

// ---------- Avisos / notificaciones ----------

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(auth: AuthRepository, tasks: TaskRepository, reports: ReportRepository) : BaseViewModel() {
    val alerts = auth.currentUser.filterNotNull().flatMapLatest { user ->
        val taskFlow = if (user.role == Role.ADMIN) tasks.observeAll() else tasks.observeForUser(user.id)
        combine(taskFlow, reports.observeAll()) { t, r -> AlertBuilder.build(user.role, t, r) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> NotificationsViewModel(c.authRepository, c.taskRepository, c.reportRepository) }
    val alerts by vm.alerts.collectAsStateWithLifecycle()

    Scaffold(topBar = { HuertifyTopBar("Avisos", onBack) }) { padding ->
        val list = alerts ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.NotificationsNone, "No tienes avisos pendientes 🎉", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(list) { index, alert ->
                var visible by remember(index) {
                    mutableStateOf(false)
                }

                LaunchedEffect(index) {
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
                    AlertCard(alert) {
                        alert.taskId?.let {
                            onNavigate(Routes.taskDetail(it))
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun AlertCard(alert: Alert, onClick: () -> Unit) {
    val (icon, color) = when (alert.level) {
        AlertLevel.CRITICAL -> Icons.Filled.Error to Color(0xFFC62828)
        AlertLevel.WARNING -> Icons.Filled.Warning to Color(0xFFF9A825)
        AlertLevel.INFO -> Icons.Filled.Info to Color(0xFF1E88E5)
    }
    Card(
        onClick = onClick,
        enabled = alert.taskId != null,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f), disabledContainerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(alert.title, style = MaterialTheme.typography.titleSmall, color = color)
                Text(alert.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
