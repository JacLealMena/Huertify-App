package com.duoc.huertify_app.ui.screens.reports

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.domain.model.ProblemType
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.UiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReportFormViewModel(
    private val reports: ReportRepository,
    private val auth: AuthRepository,
    private val images: ImageStorage,
    private val garden: GardenRepository,
    private val savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(ReportFormUiState())
    val state: StateFlow<ReportFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            garden.observeSectors().collect { s -> _state.update { it.copy(sectors = s) } }
        }
    }

    fun onType(v: ProblemType) = _state.update { it.copy(type = v, typeError = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v, descriptionError = null) }
    fun onSector(v: Sector?) {
        _state.update { it.copy(sector = v, crop = null, crops = emptyList()) }
        if (v != null) {
            viewModelScope.launch {
                garden.observeCrops(v.id).collect { c -> _state.update { it.copy(crops = c) } }
            }
        }
    }
    fun onCrop(v: Crop?) = _state.update { it.copy(crop = v) }

    fun prepareCameraUri(): Uri = images.newCameraUri().also { savedState["pending_photo"] = it.toString() }
    fun onCameraResult(success: Boolean) {
        if (success) _state.update { it.copy(photoUri = savedState.get<String>("pending_photo")) }
    }
    fun onGalleryPicked(uri: Uri) = launchSafe {
        val stored = images.importFromGallery(uri)
        _state.update { it.copy(photoUri = stored.toString()) }
    }

    fun submit() {
        val s = _state.value
        val validated = s.copy(
            typeError = Validators.required(s.type, "Indica el tipo de problema"),
            descriptionError = Validators.requiredText(s.description, "La descripción", min = 10, max = 400)
        )
        _state.value = validated
        if (listOf(validated.typeError, validated.descriptionError).any { it != null }) {
            showError("Corrige los campos marcados antes de enviar")
            return
        }
        _state.update { it.copy(saving = true) }
        launchSafe(onFinally = { _state.update { it.copy(saving = false) } }) {
            val user = auth.requireUser()
            reports.createReport(
                ProblemReport(
                    reporterId = user.id,
                    sectorId = s.sector?.id,
                    cropId = s.crop?.id,
                    type = s.type!!,
                    description = s.description.trim(),
                    photoUri = s.photoUri
                ),
                user
            )
            send(UiEvent.SuccessAndBack("Reporte enviado al docente"))
        }
    }
}
