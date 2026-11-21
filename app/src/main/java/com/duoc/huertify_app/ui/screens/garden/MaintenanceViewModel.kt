package com.duoc.huertify_app.ui.screens.garden

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.Maintenance
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.MaintenanceRepository
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.UiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class MaintenanceViewModel(
    private val maintenances: MaintenanceRepository,
    private val auth: AuthRepository,
    garden: GardenRepository
) : BaseViewModel() {
    private val form = MutableStateFlow(MaintenanceForm())

    val state: StateFlow<MaintenanceUiState> = combine(maintenances.observeAll(), garden.observeSectors(), form) { items, sectors, f ->
        MaintenanceUiState(items, sectors, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MaintenanceUiState())

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
