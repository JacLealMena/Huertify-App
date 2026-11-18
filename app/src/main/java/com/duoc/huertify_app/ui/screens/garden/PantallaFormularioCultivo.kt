package com.duoc.huertify_app.ui.screens.garden

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.domain.model.CropStatus
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DateField
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.PhotoPicker
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CropFormState(
    val id: Long = 0,
    val name: String = "",
    val variety: String = "",
    val description: String = "",
    val plantingDate: LocalDate? = null,
    val transplantDate: LocalDate? = null,
    val harvestDate: LocalDate? = null,
    val watering: String = "",
    val status: CropStatus = CropStatus.PLANIFICADO,
    val photoUri: String? = null,
    val notes: String = "",
    val nameError: String? = null,
    val varietyError: String? = null,
    val descriptionError: String? = null,
    val plantingError: String? = null,
    val transplantError: String? = null,
    val harvestError: String? = null,
    val wateringError: String? = null,
    val notesError: String? = null,
    val saving: Boolean = false
) {
    val isEdit get() = id != 0L
    val hasErrors
        get() = listOf(
            nameError, varietyError, descriptionError, plantingError, transplantError, harvestError, wateringError, notesError
        ).any { it != null }
}

class CropFormViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    private val images: ImageStorage,
    private val savedState: SavedStateHandle
) : BaseViewModel() {
    private val sectorId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_SECTOR_ID))
    private val _state = MutableStateFlow(CropFormState())
    val state = _state.asStateFlow()

    init {
        val cropId = savedState.get<Long>(Routes.ARG_CROP_ID) ?: 0L
        if (cropId > 0) viewModelScope.launch {
            garden.findCrop(cropId)?.let { c ->
                _state.value = CropFormState(
                    id = c.id, name = c.name, variety = c.variety, description = c.description,
                    plantingDate = c.planting, transplantDate = c.transplant, harvestDate = c.harvest,
                    watering = c.wateringFrequencyDays.toString(), status = c.status, photoUri = c.photoUri, notes = c.notes
                )
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onVariety(v: String) = _state.update { it.copy(variety = v, varietyError = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v, descriptionError = null) }
    fun onWatering(v: String) = _state.update { it.copy(watering = v, wateringError = null) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v, notesError = null) }
    fun onStatus(v: CropStatus) = _state.update { it.copy(status = v) }

    fun onPlanting(date: LocalDate) = _state.update {
        // Revalida cosecha y trasplante en vivo para mantener la coherencia entre las fechas.
        it.copy(
            plantingDate = date,
            plantingError = null,
            harvestError = it.harvestDate?.let { h -> Validators.harvestDate(h, date) },
            transplantError = Validators.transplantDate(it.transplantDate, date, it.harvestDate)
        )
    }

    fun onTransplant(date: LocalDate?) = _state.update {
        it.copy(transplantDate = date, transplantError = Validators.transplantDate(date, it.plantingDate, it.harvestDate))
    }

    fun onHarvest(date: LocalDate) = _state.update {
        it.copy(
            harvestDate = date,
            harvestError = Validators.harvestDate(date, it.plantingDate),
            transplantError = Validators.transplantDate(it.transplantDate, it.plantingDate, date)
        )
    }

    fun prepareCameraUri(): Uri = images.newCameraUri().also { savedState[KEY_PENDING_PHOTO] = it.toString() }

    fun onCameraResult(success: Boolean) {
        val pending = savedState.get<String>(KEY_PENDING_PHOTO)
        if (success && pending != null) _state.update { it.copy(photoUri = pending) }
    }

    fun onGalleryPicked(uri: Uri) = launchSafe {
        val stored = images.importFromGallery(uri)
        _state.update { it.copy(photoUri = stored.toString()) }
    }

    fun save() {
        val s = _state.value
        val validated = s.copy(
            nameError = Validators.requiredText(s.name, "El nombre del cultivo", min = 2, max = 40),
            varietyError = Validators.optionalText(s.variety, "La variedad", 40),
            descriptionError = Validators.optionalText(s.description, "La descripción", 200),
            plantingError = Validators.plantingDate(s.plantingDate, LocalDate.now()),
            transplantError = Validators.transplantDate(s.transplantDate, s.plantingDate, s.harvestDate),
            harvestError = Validators.harvestDate(s.harvestDate, s.plantingDate),
            wateringError = Validators.wateringFrequency(s.watering),
            notesError = Validators.optionalText(s.notes, "Las observaciones", 200)
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
                    description = s.description.trim(),
                    plantingDate = s.plantingDate!!.toEpochDay(),
                    transplantDate = s.transplantDate?.toEpochDay(),
                    harvestDate = s.harvestDate!!.toEpochDay(),
                    wateringFrequencyDays = s.watering.trim().toInt(),
                    status = s.status,
                    photoUri = s.photoUri,
                    notes = s.notes.trim()
                ),
                auth.requireUser()
            )
            send(UiEvent.SuccessAndBack(if (s.isEdit) "Cultivo actualizado" else "Cultivo registrado"))
        }
    }

    private companion object {
        const val KEY_PENDING_PHOTO = "pending_crop_photo"
    }
}

@Composable
fun CropFormScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, h -> CropFormViewModel(c.gardenRepository, c.authRepository, c.imageStorage, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar, onBack)

    Scaffold(
        topBar = { HuertifyTopBar(if (state.isEdit) "Editar cultivo" else "Nuevo cultivo", onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ValidatedTextField(state.name, vm::onName, "Nombre común del cultivo *", state.nameError, leadingIcon = Icons.Filled.Grass)
            ValidatedTextField(state.variety, vm::onVariety, "Variedad", state.varietyError, leadingIcon = Icons.Filled.Spa)
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 2
            )
            DropdownField(
                label = "Estado de desarrollo *",
                options = CropStatus.entries,
                selected = state.status,
                optionLabel = { it.label },
                onSelected = vm::onStatus,
                error = null,
                leadingIcon = Icons.AutoMirrored.Filled.TrendingUp
            )
            DateField("Fecha de siembra *", state.plantingDate, vm::onPlanting, state.plantingError)
            DateField("Fecha de trasplante (si corresponde)", state.transplantDate, { vm.onTransplant(it) }, state.transplantError)
            if (state.transplantDate != null) {
                TextButton(onClick = { vm.onTransplant(null) }) { Text("Quitar fecha de trasplante") }
            }
            DateField("Fecha estimada de cosecha *", state.harvestDate, vm::onHarvest, state.harvestError)
            ValidatedTextField(
                state.watering, vm::onWatering, "Frecuencia de riego (días) *", state.wateringError,
                leadingIcon = Icons.Filled.WaterDrop,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = "Cada cuántos días se debe regar; se usa para calcular el próximo riego"
            )
            ValidatedTextField(
                state.notes, vm::onNotes, "Observaciones generales", state.notesError,
                leadingIcon = Icons.AutoMirrored.Filled.Notes, singleLine = false, minLines = 2
            )
            Text("Fotografía referencial (opcional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Usa una foto ficticia de la planta, sin personas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PhotoPicker(state.photoUri, null, vm::prepareCameraUri, vm::onCameraResult, vm::onGalleryPicked)
            LoadingButton("Guardar", state.saving, vm::save, Modifier.padding(top = 12.dp), Icons.Filled.Save)
        }
    }
}
