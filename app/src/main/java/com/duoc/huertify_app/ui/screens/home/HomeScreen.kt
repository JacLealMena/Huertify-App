package com.duoc.huertify_app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.formatted
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import com.duoc.huertify_app.ui.screens.tasks.TaskCard

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ ->
        HomeViewModel(c.authRepository, c.gardenRepository, c.taskRepository, c.reportRepository, c.maintenanceRepository, c.weatherRepository)
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
            contentPadding = PaddingValues(16.dp),
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

private fun LazyListScope.adminContent(state: HomeUiState, onNavigate: (String) -> Unit) {
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
    item {
        MenuCard(Icons.Filled.ReportProblem, "Problemas del huerto", "Seguimiento de reportes (${state.openReports} por revisar)") {
            onNavigate(Routes.REPORTS)
        }
    }
    item { MenuCard(Icons.Filled.Build, "Mantenimientos", "Programar mantenimientos de los sectores") { onNavigate(Routes.MAINTENANCE) } }
    item {
        MenuCard(Icons.Filled.Notifications, "Avisos y recordatorios", "Riegos, cosechas, mantenimientos y atrasos (${state.alertCount})") {
            onNavigate(Routes.NOTIFICATIONS)
        }
    }
    item { MenuCard(Icons.Filled.History, "Historial de actividades", "Registro cronológico con fotos y validaciones") { onNavigate(Routes.HISTORY) } }
}

private fun LazyListScope.studentContent(state: HomeUiState, onNavigate: (String) -> Unit) {
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
    item {
        MenuCard(Icons.AutoMirrored.Filled.Assignment, "Mis reportes", "Estado de tus reportes y comentarios del docente") {
            onNavigate(Routes.REPORTS)
        }
    }
    val active = state.tasks.filter { it.task.status != TaskStatus.VALIDADA }
    val done = state.tasks.filter { it.task.status == TaskStatus.VALIDADA }
    item { SectionTitle("Tareas asignadas (${active.size})") }
    if (active.isEmpty()) {
        item { Text("No tienes tareas activas. ¡Buen trabajo! 🌱", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    items(active, key = { it.task.id }) { TaskCard(it, showAssignee = false) { onNavigate(Routes.taskDetail(it.task.id)) } }
    if (done.isNotEmpty()) {
        item { SectionTitle("Validadas (${done.size})") }
        items(done, key = { it.task.id }) { TaskCard(it, showAssignee = false) { onNavigate(Routes.taskDetail(it.task.id)) } }
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
