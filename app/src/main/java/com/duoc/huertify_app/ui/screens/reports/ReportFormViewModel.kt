package com.duoc.huertify_app.ui.screens.reports

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.domain.model.Severity
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
    garden: GardenRepository,
    private val savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(ReportFormUiState())
    val state: StateFlow<ReportFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { garden.observeSectors().collect { s -> _state.update { it.copy(sectors = s) } } }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v, titleError = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v, descriptionError = null) }
    fun onSeverity(v: Severity) = _state.update { it.copy(severity = v, severityError = null) }
    fun onSector(v: Sector?) = _state.update { it.copy(sector = v) }

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
            titleError = Validators.requiredText(s.title, "El título", min = 5, max = 60),
            descriptionError = Validators.requiredText(s.description, "La descripción", min = 10, max = 400),
            severityError = Validators.required(s.severity, "Indica la gravedad del problema")
        )
        _state.value = validated
        if (listOf(validated.titleError, validated.descriptionError, validated.severityError).any { it != null }) {
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
                    title = s.title.trim(),
                    description = s.description.trim(),
                    severity = s.severity!!,
                    photoUri = s.photoUri
                ),
                user
            )
            send(UiEvent.SuccessAndBack("Reporte enviado al docente"))
        }
    }
}
