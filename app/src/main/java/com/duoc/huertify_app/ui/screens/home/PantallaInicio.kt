package com.duoc.huertify_app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import com.duoc.huertify_app.ui.common.formatted
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.data.repository.WeatherRepository
import com.duoc.huertify_app.domain.WeatherSummary
import com.duoc.huertify_app.domain.AlertBuilder
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import com.duoc.huertify_app.ui.screens.tasks.TaskCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val summary: WeatherSummary) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val auth: AuthRepository,
    garden: GardenRepository,
    tasks: TaskRepository,
    reports: ReportRepository,
    private val weatherRepository: WeatherRepository
) : BaseViewModel() {

    private val _weather = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    init {
        loadWeather()
    }

    fun loadWeather() {
        _weather.value = WeatherUiState.Loading
        viewModelScope.launch {
            _weather.value = weatherRepository.getForecast().fold(
                onSuccess = { WeatherUiState.Success(it) },
                onFailure = { WeatherUiState.Error(it.message ?: "No se pudo obtener el clima") }
            )
        }
    }

    val state: StateFlow<HomeUiState> = auth.currentUser.filterNotNull().flatMapLatest { user ->
        val taskFlow = if (user.role == Role.ADMIN) tasks.observeAll() else tasks.observeForUser(user.id)
        combine(
            taskFlow,
            garden.observeSectors(),
            garden.observeCropCount(),
            reports.observeAll()
        ) { taskList, sectors, crops, reportList ->
            HomeUiState(
                user = user,
                sectorCount = sectors.size,
                cropCount = crops,
                tasks = taskList,
                pendingValidation = taskList.count { it.task.status == TaskStatus.PENDIENTE_VALIDACION },
                overdue = taskList.count { it.task.isOverdue() },
                openReports = reportList.count { !it.report.resolved },
                alertCount = AlertBuilder.build(user.role, taskList, reportList).size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun logout() = launchSafe { auth.logout() }
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ ->
        HomeViewModel(c.authRepository, c.gardenRepository, c.taskRepository, c.reportRepository, c.weatherRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val user = state.user ?: return

    Scaffold(
        topBar = {
            HuertifyTopBar(title = if (user.role == Role.ADMIN) "Panel docente" else "Mis tareas") {
                IconButton(onClick = { onNavigate(Routes.NOTIFICATIONS) }) {
                    BadgedBox(badge = { if (state.alertCount > 0) Badge { Text("${state.alertCount}") } }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Avisos")
                    }
                }
                IconButton(onClick = vm::logout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Hola, ${user.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(user.role.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { WeatherCard(weather, onRetry = vm::loadWeather) }
            if (user.role == Role.ADMIN) {
                adminContent(state, onNavigate)
            } else {
                studentContent(state, onNavigate)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adminContent(state: HomeUiState, onNavigate: (String) -> Unit) {
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Sectores", "${state.sectorCount}", Modifier.weight(1f))
            StatCard("Cultivos", "${state.cropCount}", Modifier.weight(1f))
            StatCard("Tareas", "${state.tasks.size}", Modifier.weight(1f))
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Por validar", "${state.pendingValidation}", Modifier.weight(1f), Color(0xFFF9A825))
            StatCard("Atrasadas", "${state.overdue}", Modifier.weight(1f), Color(0xFFC62828))
            StatCard("Reportes", "${state.openReports}", Modifier.weight(1f), Color(0xFF6D4C41))
        }
    }
    item { SectionTitle("Gestión") }
    item { MenuCard(Icons.Filled.Yard, "Sectores y cultivos", "Crear, editar y eliminar sectores del huerto") { onNavigate(Routes.SECTORS) } }
    item { MenuCard(Icons.AutoMirrored.Filled.Assignment, "Tareas", "Crear, asignar, editar y filtrar tareas") { onNavigate(Routes.TASKS) } }
    item {
        MenuCard(Icons.AutoMirrored.Filled.FactCheck, "Revisiones", "Validar o rechazar entregas (${state.pendingValidation} pendientes)") {
            onNavigate(Routes.REVIEWS)
        }
    }
    item { MenuCard(Icons.Filled.ReportProblem, "Problemas del huerto", "Reportes de plagas, daños o riego") { onNavigate(Routes.REPORTS) } }
    item { MenuCard(Icons.Filled.History, "Historial de actividades", "Registro de todo lo realizado") { onNavigate(Routes.HISTORY) } }
}

private fun androidx.compose.foundation.lazy.LazyListScope.studentContent(state: HomeUiState, onNavigate: (String) -> Unit) {
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Pendientes", "${state.countBy(TaskStatus.PENDIENTE)}", Modifier.weight(1f))
            StatCard("En curso", "${state.countBy(TaskStatus.EN_DESARROLLO)}", Modifier.weight(1f), Color(0xFF1E88E5))
            StatCard("Atrasadas", "${state.overdue}", Modifier.weight(1f), Color(0xFFC62828))
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuCard(Icons.Filled.ReportProblem, "Reportar problema", null, Modifier.weight(1f)) { onNavigate(Routes.REPORT_FORM) }
            MenuCard(Icons.Filled.History, "Mi historial", null, Modifier.weight(1f)) { onNavigate(Routes.HISTORY) }
        }
    }
    val active = state.tasks.filter { it.task.status != TaskStatus.VALIDADA }
    val done = state.tasks.filter { it.task.status == TaskStatus.VALIDADA }
    item { SectionTitle("Tareas asignadas (${active.size})") }
    if (active.isEmpty()) {
        item { Text("No tienes tareas activas. ¡Buen trabajo! 🌱", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    itemsIndexed(active, key = { _, item -> item.task.id }) { index, item ->
        var visible by remember("active-${item.task.id}") {
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
            TaskCard(item, showAssignee = false) {
                onNavigate(Routes.taskDetail(item.task.id))
            }
        }
    }

    if (done.isNotEmpty()) {
        item { SectionTitle("Validadas (${done.size})") }

        itemsIndexed(done, key = { _, item -> "done-${item.task.id}" }) { index, item ->
            var visible by remember("done-${item.task.id}") {
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
                TaskCard(item, showAssignee = false) {
                    onNavigate(Routes.taskDetail(item.task.id))
                }
            }
        }

    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(state: WeatherUiState, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(8.dp))
                Text("Clima del huerto", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (state !is WeatherUiState.Loading) {
                    IconButton(onClick = onRetry) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar clima") }
                }
            }
            when (state) {
                WeatherUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Consultando pronóstico…")
                }
                is WeatherUiState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                is WeatherUiState.Success -> {
                    val w = state.summary
                    Text(
                        "${"%.0f".format(w.currentTemp)}°C · ${w.currentDescription}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(w.wateringAdvice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        w.days.forEachIndexed { index, day ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    when (index) { 0 -> "Hoy"; 1 -> "Mañana"; else -> day.date.formatted().substring(0, 5) },
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text("${"%.0f".format(day.minTemp)}° / ${"%.0f".format(day.maxTemp)}°", style = MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("${"%.1f".format(day.precipitationMm)} mm", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
