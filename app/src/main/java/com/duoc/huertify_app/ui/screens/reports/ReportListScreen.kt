package com.duoc.huertify_app.ui.screens.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes

@Composable
fun ReportListScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> ReportListViewModel(c.reportRepository, c.authRepository) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar)

    Scaffold(
        topBar = { HuertifyTopBar("Problemas del huerto", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Routes.REPORT_FORM) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Reportar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val reports = uiState.reports ?: return@Scaffold
        if (reports.isEmpty()) {
            EmptyState(Icons.Filled.ReportProblem, "No hay problemas reportados", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(reports, key = { _, item -> item.report.id }) { index, item ->
                var visible by remember(item.report.id) {
                    mutableStateOf(false)
                }

                LaunchedEffect(item.report.id) {
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
                    val r = item.report
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    r.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusChip(r.severity.label, r.severity.color())
                            }
                            Text(
                                "${item.sectorName ?: "General"} · ${item.reporterName} · ${r.createdAt.formatTimestamp()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(r.description, modifier = Modifier.padding(vertical = 8.dp))
                            r.photoUri?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = "Foto del problema",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (r.resolved) "Resuelto" else "Pendiente",
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = r.resolved,
                                    onCheckedChange = { vm.toggleResolved(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}
