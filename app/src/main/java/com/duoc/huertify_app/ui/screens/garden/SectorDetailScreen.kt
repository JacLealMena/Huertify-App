package com.duoc.huertify_app.ui.screens.garden

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.ConfirmDialog
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.formatArea
import com.duoc.huertify_app.ui.common.formatted
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes

@Composable
fun SectorDetailScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, h -> SectorDetailViewModel(c.gardenRepository, c.authRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDeleteSector by rememberSaveable { mutableStateOf(false) }
    var cropToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    CollectEvents(vm.events, snackbar, onBack)

    Scaffold(
        topBar = {
            HuertifyTopBar(state.sector?.name ?: "Sector", onBack) {
                IconButton(onClick = { onNavigate(Routes.sectorForm(vm.sectorId)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar sector")
                }
                IconButton(onClick = { confirmDeleteSector = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar sector")
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Routes.cropForm(vm.sectorId)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo cultivo") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val sector = state.sector
        if (!state.loaded || sector == null) return@Scaffold
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("📍 ${sector.location}")
                Text("📐 ${sector.areaM2.formatArea()}")
                if (sector.description.isNotBlank()) Text(sector.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SectionTitle("Cultivos (${state.crops.size})") }
            if (state.crops.isEmpty()) {
                item { Text("Este sector aún no tiene cultivos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.crops, key = { it.id }) { crop ->
                ElevatedCard(onClick = { onNavigate(Routes.cropForm(vm.sectorId, crop.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Grass, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                crop.name + if (crop.variety.isNotBlank()) " (${crop.variety})" else "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Siembra ${crop.planting.formatted()} · Cosecha ${crop.harvest.formatted()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text("Riego cada ${crop.wateringFrequencyDays} día(s)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { cropToDeleteId = crop.id }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar cultivo")
                        }
                    }
                }
            }
        }
    }

    if (confirmDeleteSector) {
        ConfirmDialog(
            title = "Eliminar sector",
            text = "Se eliminarán también sus ${state.crops.size} cultivo(s). Las tareas asociadas quedarán sin sector. ¿Continuar?",
            confirmLabel = "Eliminar",
            onConfirm = vm::deleteSector,
            onDismiss = { confirmDeleteSector = false }
        )
    }
    state.crops.firstOrNull { it.id == cropToDeleteId }?.let { crop ->
        ConfirmDialog(
            title = "Eliminar cultivo",
            text = "¿Eliminar \"${crop.name}\" de este sector?",
            confirmLabel = "Eliminar",
            onConfirm = { vm.deleteCrop(crop) },
            onDismiss = { cropToDeleteId = null }
        )
    }
}
