package com.duoc.huertify_app.ui.screens.garden

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SectorFormViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(SectorFormUiState())
    val state: StateFlow<SectorFormUiState> = _state.asStateFlow()

    init {
        val id = savedState.get<Long>(Routes.ARG_SECTOR_ID) ?: 0L
        if (id > 0) viewModelScope.launch {
            garden.findSector(id)?.let { s ->
                _state.value = SectorFormUiState(s.id, s.name, s.location, s.areaM2.toString().replace('.', ','), s.description)
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onLocation(v: String) = _state.update { it.copy(location = v, locationError = null) }
    fun onArea(v: String) = _state.update { it.copy(area = v, areaError = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v, descriptionError = null) }

    fun save() {
        val s = _state.value
        val validated = s.copy(
            nameError = Validators.requiredText(s.name, "El nombre", min = 3, max = 40),
            locationError = Validators.requiredText(s.location, "La ubicación", min = 3, max = 80),
            areaError = Validators.positiveDecimal(s.area, "La superficie"),
            descriptionError = Validators.optionalText(s.description, "La descripción", 200)
        )
        _state.value = validated
        if (listOf(validated.nameError, validated.locationError, validated.areaError, validated.descriptionError).any { it != null }) {
            showError("Corrige los campos marcados antes de guardar")
            return
        }
        _state.update { it.copy(saving = true) }
        launchSafe(onFinally = { _state.update { it.copy(saving = false) } }) {
            if (garden.isSectorNameTaken(s.name, s.id)) {
                _state.update { it.copy(nameError = "Ya existe un sector con este nombre") }
                showError("El nombre del sector ya está en uso")
                return@launchSafe
            }
            garden.saveSector(
                Sector(
                    id = s.id,
                    name = s.name.trim(),
                    location = s.location.trim(),
                    areaM2 = s.area.trim().replace(',', '.').toDouble(),
                    description = s.description.trim()
                ),
                auth.requireUser()
            )
            send(UiEvent.SuccessAndBack(if (s.isEdit) "Sector actualizado" else "Sector creado"))
        }
    }
}
