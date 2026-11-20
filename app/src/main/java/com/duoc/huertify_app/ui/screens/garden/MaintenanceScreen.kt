package com.duoc.huertify_app.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.data.local.Maintenance
import com.duoc.huertify_app.data.local.MaintenanceWithSector
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DateField
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.formatted
import com.duoc.huertify_app.ui.common.huertifyViewModel
import java.time.LocalDate

@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, _ -> MaintenanceViewModel(c.maintenanceRepository, c.authRepository, c.gardenRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar)

    Scaffold(
        topBar = { HuertifyTopBar("Mantenimientos", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::openForm,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Programar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val items = state.items ?: return@Scaffold
        if (items.isEmpty()) {
            EmptyState(Icons.Filled.Build, "No hay mantenimientos programados.\nPrograma el primero con el botón +", Modifier.padding(padding))
            return@Scaffold
        }
        val pending = items.filter { !it.maintenance.done }
        val done = items.filter { it.maintenance.done }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionTitle("Programados (${pending.size})") }
            items(pending, key = { it.maintenance.id }) { MaintenanceCard(it, vm::toggleDone, vm::delete) }
            if (done.isNotEmpty()) {
                item { SectionTitle("Completados (${done.size})") }
                items(done, key = { it.maintenance.id }) { MaintenanceCard(it, vm::toggleDone, vm::delete) }
            }
        }
    }

    val f = state.form
    if (f.open) {
        AlertDialog(
            onDismissRequest = vm::closeForm,
            title = { Text("Programar mantenimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DropdownField(
                        label = "Sector *",
                        options = state.sectors,
                        selected = state.sectors.firstOrNull { it.id == f.sectorId },
                        optionLabel = { it.name },
                        onSelected = vm::onSector,
                        error = f.sectorError,
                        leadingIcon = Icons.Filled.Yard
                    )
                    ValidatedTextField(f.title, vm::onTitle, "Mantenimiento *", f.titleError, leadingIcon = Icons.Filled.Title)
                    DateField("Fecha *", f.date, vm::onDate, f.dateError)
                    ValidatedTextField(
                        f.notes, vm::onNotes, "Notas", null,
                        leadingIcon = Icons.AutoMirrored.Filled.Notes, singleLine = false, minLines = 2
                    )
                }
            },
            confirmButton = { TextButton(onClick = vm::save) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = vm::closeForm) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MaintenanceCard(item: MaintenanceWithSector, onToggle: (Maintenance) -> Unit, onDelete: (Maintenance) -> Unit) {
    val m = item.maintenance
    val overdue = !m.done && m.scheduled.isBefore(LocalDate.now())
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = m.done, onCheckedChange = { onToggle(m) })
            Column(Modifier.weight(1f)) {
                Text(
                    m.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (m.done) TextDecoration.LineThrough else null
                )
                Text(
                    "${item.sectorName} · ${m.scheduled.formatted()}" + if (overdue) " · Atrasado" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (m.notes.isNotBlank()) Text(m.notes, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onDelete(m) }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar mantenimiento") }
        }
    }
}
