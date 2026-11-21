package com.duoc.huertify_app.ui.screens.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.data.local.ActivityWithUser
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, _ -> HistoryViewModel(c.authRepository, c.activityRepository) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { HuertifyTopBar("Historial de actividades", onBack) }) { padding ->
        val list = uiState.entries ?: return@Scaffold
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
