package com.duoc.huertify_app.ui.screens.garden

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.Crop
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
import java.time.LocalDate

class CropFormViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    private val sectorId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_SECTOR_ID))
    private val _state = MutableStateFlow(CropFormUiState())
    val state: StateFlow<CropFormUiState> = _state.asStateFlow()

    init {
        val cropId = savedState.get<Long>(Routes.ARG_CROP_ID) ?: 0L
        if (cropId > 0) viewModelScope.launch {
            garden.findCrop(cropId)?.let { c ->
                _state.value = CropFormUiState(
                    id = c.id, name = c.name, variety = c.variety, plantingDate = c.planting,
                    harvestDate = c.harvest, watering = c.wateringFrequencyDays.toString(), notes = c.notes
                )
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onVariety(v: String) = _state.update { it.copy(variety = v, varietyError = null) }
    fun onWatering(v: String) = _state.update { it.copy(watering = v, wateringError = null) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v, notesError = null) }

    fun onPlanting(date: LocalDate) = _state.update {
        it.copy(
            plantingDate = date,
            plantingError = null,
            harvestError = it.harvestDate?.let { h -> Validators.harvestDate(h, date) }
        )
    }

    fun onHarvest(date: LocalDate) = _state.update {
        it.copy(harvestDate = date, harvestError = Validators.harvestDate(date, it.plantingDate))
    }

    fun save() {
        val s = _state.value
        val validated = s.copy(
            nameError = Validators.requiredText(s.name, "El nombre del cultivo", min = 2, max = 40),
            varietyError = Validators.optionalText(s.variety, "La variedad", 40),
            plantingError = Validators.plantingDate(s.plantingDate, LocalDate.now()),
            harvestError = Validators.harvestDate(s.harvestDate, s.plantingDate),
            wateringError = Validators.wateringFrequency(s.watering),
            notesError = Validators.optionalText(s.notes, "Las notas", 200)
        )
        _state.value = validated
        if (validated.hasErrors) {
            showError("Corrige los campos marcados antes de guardar")
            return
        }
        _state.update { it.copy(saving = true) }
        launchSafe(onFinally = { _state.update { it.copy(saving = false) } }) {
            garden.saveCrop(
                Crop(
                    id = s.id,
                    sectorId = sectorId,
                    name = s.name.trim(),
                    variety = s.variety.trim(),
                    plantingDate = s.plantingDate!!.toEpochDay(),
                    harvestDate = s.harvestDate!!.toEpochDay(),
                    wateringFrequencyDays = s.watering.trim().toInt(),
                    notes = s.notes.trim()
                ),
                auth.requireUser()
            )
            send(UiEvent.SuccessAndBack(if (s.isEdit) "Cultivo actualizado" else "Cultivo registrado"))
        }
    }
}
