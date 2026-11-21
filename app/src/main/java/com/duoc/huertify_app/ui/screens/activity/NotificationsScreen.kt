package com.duoc.huertify_app.ui.screens.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.domain.Alert
import com.duoc.huertify_app.domain.AlertLevel
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes

@Composable
fun NotificationsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ ->
        NotificationsViewModel(c.authRepository, c.taskRepository, c.reportRepository)
    }
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { HuertifyTopBar("Avisos", onBack) }) { padding ->
        val list = uiState.alerts ?: return@Scaffold
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
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
            disabledContainerColor = color.copy(alpha = 0.1f)
        ),
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
