package com.duoc.huertify_app.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
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
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes

@Composable
fun ReviewsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> ReviewsViewModel(c.taskRepository) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { HuertifyTopBar("Revisiones", onBack) }) { padding ->
        val s = uiState
        if (s.pending.isEmpty() && s.reviewed.isEmpty() && !s.isLoading) {
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
            itemsIndexed(s.pending, key = { _, item -> item.task.id }) { index, item ->
                var visible by remember("pending-${item.task.id}") {
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

            if (s.reviewed.isNotEmpty()) {
                item { SectionTitle("Revisadas recientemente") }
                itemsIndexed(s.reviewed, key = { _, item -> "r${item.task.id}" }) { index, item ->
                    var visible by remember("reviewed-${item.task.id}") {
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
