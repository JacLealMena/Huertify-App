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
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.Maintenance
import com.duoc.huertify_app.data.local.MaintenanceWithSector
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.MaintenanceRepository
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DateField
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.formatted
import com.duoc.huertify_app.ui.common.huertifyViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class MaintenanceForm(
    val open: Boolean = false,
    val sectorId: Long? = null,
    val title: String = "",
    val notes: String = "",
    val date: LocalDate? = null,
    val sectorError: String? = null,
    val titleError: String? = null,
    val dateError: String? = null
)

data class MaintenanceState(
    val items: List<MaintenanceWithSector>? = null,
    val sectors: List<Sector> = emptyList(),
    val form: MaintenanceForm = MaintenanceForm()
)

class MaintenanceViewModel(
    private val maintenances: MaintenanceRepository,
    private val auth: AuthRepository,
    garden: GardenRepository
) : BaseViewModel() {
    private val form = MutableStateFlow(MaintenanceForm())

    val state = combine(maintenances.observeAll(), garden.observeSectors(), form) { items, sectors, f ->
        MaintenanceState(items, sectors, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MaintenanceState())

    fun openForm() = form.update { MaintenanceForm(open = true) }
    fun closeForm() = form.update { MaintenanceForm() }
    fun onSector(v: Sector) = form.update { it.copy(sectorId = v.id, sectorError = null) }
    fun onTitle(v: String) = form.update { it.copy(title = v, titleError = null) }
    fun onNotes(v: String) = form.update { it.copy(notes = v) }
    fun onDate(v: LocalDate) = form.update { it.copy(date = v, dateError = null) }

    fun save() {
        val f = form.value
        val validated = f.copy(
            sectorError = Validators.required(f.sectorId, "Selecciona el sector"),
            titleError = Validators.requiredText(f.title, "El mantenimiento", min = 3, max = 60),
            dateError = Validators.scheduledDate(f.date, LocalDate.now(), allowPast = false)
        )
        form.value = validated
        if (listOf(validated.sectorError, validated.titleError, validated.dateError).any { it != null }) return
        launchSafe {
            maintenances.schedule(
                Maintenance(sectorId = f.sectorId!!, title = f.title.trim(), notes = f.notes.trim(), scheduledDate = f.date!!.toEpochDay()),
                auth.requireUser()
            )
            form.value = MaintenanceForm()
            send(UiEvent.ShowMessage("Mantenimiento programado"))
        }
    }

    fun toggleDone(m: Maintenance) = launchSafe {
        maintenances.setDone(m, !m.done, auth.requireUser())
        send(UiEvent.ShowMessage(if (!m.done) "Mantenimiento completado" else "Mantenimiento reabierto"))
    }

    fun delete(m: Maintenance) = launchSafe {
        maintenances.delete(m, auth.requireUser())
        send(UiEvent.ShowMessage("Mantenimiento eliminado"))
    }
}

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
