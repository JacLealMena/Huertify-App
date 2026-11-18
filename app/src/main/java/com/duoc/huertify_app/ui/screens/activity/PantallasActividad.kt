package com.duoc.huertify_app.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.duoc.huertify_app.data.local.ActivityWithUser
import com.duoc.huertify_app.data.local.CropWithSector
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.ActivityRepository
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.MaintenanceRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.Alert
import com.duoc.huertify_app.domain.AlertBuilder
import com.duoc.huertify_app.domain.AlertCategory
import com.duoc.huertify_app.domain.AlertLevel
import com.duoc.huertify_app.domain.model.ActivityKind
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// ---------- Historial ----------

/** Filtros rápidos del historial. */
enum class HistoryView(val label: String) {
    TODO("Todo"), FOTOS("Con fotografía"), VALIDACIONES("Validaciones"), PROBLEMAS("Problemas")
}

data class HistoryFilter(val sectorId: Long? = null, val cropId: Long? = null, val view: HistoryView = HistoryView.TODO)

data class HistoryState(
    val entries: List<ActivityWithUser>? = null,
    val sectors: List<Sector> = emptyList(),
    val crops: List<CropWithSector> = emptyList(),
    val filter: HistoryFilter = HistoryFilter()
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(auth: AuthRepository, activity: ActivityRepository, garden: GardenRepository) : BaseViewModel() {
    private val filter = MutableStateFlow(HistoryFilter())

    // El administrador ve toda la actividad; el estudiante solo la propia y la de sus tareas.
    private val source = auth.currentUser.filterNotNull().flatMapLatest { user: User ->
        if (user.role == Role.ADMIN) activity.observeAll() else activity.observeForUser(user.id)
    }

    val state = combine(source, garden.observeSectors(), garden.observeAllCrops(), filter) { all, sectors, crops, f ->
        HistoryState(
            entries = all.filter { e ->
                val a = e.activity
                (f.sectorId == null || a.sectorId == f.sectorId) &&
                    (f.cropId == null || a.cropId == f.cropId) &&
                    when (f.view) {
                        HistoryView.TODO -> true
                        HistoryView.FOTOS -> a.photoUri != null
                        HistoryView.VALIDACIONES -> a.kind == ActivityKind.VALIDACION || a.kind == ActivityKind.RECHAZO
                        HistoryView.PROBLEMAS -> a.kind == ActivityKind.PROBLEMA
                    }
            },
            sectors = sectors,
            crops = crops,
            filter = f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun onSector(s: Sector?) = filter.update {
        val keepCrop = it.cropId != null && state.value.crops.any { c -> c.crop.id == it.cropId && c.crop.sectorId == s?.id }
        it.copy(sectorId = s?.id, cropId = if (keepCrop) it.cropId else null)
    }
    fun onCrop(c: CropWithSector?) = filter.update { it.copy(cropId = c?.crop?.id, sectorId = c?.crop?.sectorId ?: it.sectorId) }
    fun onView(v: HistoryView) = filter.update { it.copy(view = v) }
}

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, _ -> HistoryViewModel(c.authRepository, c.activityRepository, c.gardenRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val noSector: Sector? = null
    val noCrop: CropWithSector? = null

    Scaffold(topBar = { HuertifyTopBar("Historial de actividades", onBack) }) { padding ->
        val list = state.entries ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DropdownField(
                        label = "Sector",
                        options = listOf(noSector) + state.sectors,
                        selected = state.sectors.firstOrNull { it.id == state.filter.sectorId },
                        optionLabel = { it?.name ?: "Todos los sectores" },
                        onSelected = vm::onSector,
                        error = null,
                        leadingIcon = Icons.Filled.Yard
                    )
                    DropdownField(
                        label = "Cultivo",
                        options = listOf(noCrop) + state.crops.filter { state.filter.sectorId == null || it.crop.sectorId == state.filter.sectorId },
                        selected = state.crops.firstOrNull { it.crop.id == state.filter.cropId },
                        optionLabel = { it?.let { c -> "${c.crop.name} · ${c.sectorName}" } ?: "Todos los cultivos" },
                        onSelected = vm::onCrop,
                        error = null,
                        leadingIcon = Icons.Filled.Grass
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryView.entries.forEach { v ->
                            FilterChip(selected = state.filter.view == v, onClick = { vm.onView(v) }, label = { Text(v.label) })
                        }
                    }
                    Text(
                        "${list.size} registro(s), del más reciente al más antiguo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
            if (list.isEmpty()) {
                item { EmptyState(Icons.Filled.History, "No hay actividad que coincida con los filtros") }
            }
            items(list, key = { it.activity.id }) { entry -> HistoryRow(entry) }
        }
    }
}

@Composable
private fun HistoryRow(entry: ActivityWithUser) {
    val (icon, color) = entry.activity.kind.visual()
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.activity.message, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${entry.userName} · ${entry.activity.timestamp.formatTimestamp()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            entry.activity.photoUri?.let {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = it, contentDescription = "Fotografía del registro", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
                )
            }
        }
    }
    HorizontalDivider()
}

private fun ActivityKind.visual(): Pair<ImageVector, Color> = when (this) {
    ActivityKind.GENERAL -> Icons.Filled.Info to Color(0xFF757575)
    ActivityKind.SECTOR -> Icons.Filled.Yard to Color(0xFF6D4C41)
    ActivityKind.CULTIVO -> Icons.Filled.Grass to Color(0xFF2E7D32)
    ActivityKind.TAREA -> Icons.AutoMirrored.Filled.Assignment to Color(0xFF1E88E5)
    ActivityKind.ENTREGA -> Icons.AutoMirrored.Filled.Send to Color(0xFFF9A825)
    ActivityKind.VALIDACION -> Icons.Filled.CheckCircle to Color(0xFF2E7D32)
    ActivityKind.RECHAZO -> Icons.Filled.Close to Color(0xFFC62828)
    ActivityKind.PROBLEMA -> Icons.Filled.ReportProblem to Color(0xFFEF6C00)
    ActivityKind.MANTENIMIENTO -> Icons.Filled.Build to Color(0xFF455A64)
}

// ---------- Avisos y recordatorios ----------

data class NotificationsState(val alerts: List<Alert>, val isAdmin: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
    auth: AuthRepository,
    tasks: TaskRepository,
    reports: ReportRepository,
    garden: GardenRepository,
    maintenances: MaintenanceRepository
) : BaseViewModel() {
    val state = auth.currentUser.filterNotNull().flatMapLatest { user ->
        val own = if (user.role == Role.ADMIN) tasks.observeAll() else tasks.observeForUser(user.id)
        combine(own, reports.observeAll(), garden.observeAllCrops(), tasks.observeAll(), maintenances.observeAll()) { t, r, c, all, m ->
            NotificationsState(
                AlertBuilder.build(user.role, t, r, c, all.map { it.task }, m),
                user.role == Role.ADMIN
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ ->
        NotificationsViewModel(c.authRepository, c.taskRepository, c.reportRepository, c.gardenRepository, c.maintenanceRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { HuertifyTopBar("Avisos y recordatorios", onBack) }) { padding ->
        val s = state ?: return@Scaffold
        if (s.alerts.isEmpty()) {
            EmptyState(Icons.Filled.NotificationsNone, "No tienes avisos ni recordatorios pendientes 🎉", Modifier.padding(padding))
            return@Scaffold
        }
        val notices = s.alerts.filter { it.category == AlertCategory.AVISO }
        val reminders = s.alerts.filter { it.category == AlertCategory.RECORDATORIO }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (notices.isNotEmpty()) {
                item { SectionTitle("Avisos (${notices.size})") }
                items(notices) { alert -> AlertCard(alert, alert.destination(s.isAdmin), onNavigate) }
            }
            if (reminders.isNotEmpty()) {
                item { SectionTitle("Recordatorios: riego, cosecha y mantenimiento (${reminders.size})") }
                items(reminders) { alert -> AlertCard(alert, alert.destination(s.isAdmin), onNavigate) }
            }
        }
    }
}

/** A dónde lleva tocar el aviso: la tarea o, para el administrador, el sector relacionado. */
private fun Alert.destination(isAdmin: Boolean): String? = when {
    taskId != null -> Routes.taskDetail(taskId)
    sectorId != null && isAdmin -> Routes.sectorDetail(sectorId)
    else -> null
}

@Composable
private fun AlertCard(alert: Alert, destination: String?, onNavigate: (String) -> Unit) {
    val (icon, color) = when (alert.level) {
        AlertLevel.CRITICAL -> Icons.Filled.Error to Color(0xFFC62828)
        AlertLevel.WARNING -> Icons.Filled.Warning to Color(0xFFEF6C00)
        AlertLevel.INFO -> Icons.Filled.Info to Color(0xFF1E88E5)
    }
    Card(
        onClick = { destination?.let(onNavigate) },
        enabled = destination != null,
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
