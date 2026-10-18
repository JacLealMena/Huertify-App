package com.duoc.huertify_app.ui.screens.reports

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.domain.model.Severity
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.PhotoPicker
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- Formulario de reporte ----------

data class ReportFormState(
    val title: String = "",
    val description: String = "",
    val severity: Severity? = null,
    val sector: Sector? = null,
    val photoUri: String? = null,
    val sectors: List<Sector> = emptyList(),
    val titleError: String? = null,
    val descriptionError: String? = null,
    val severityError: String? = null,
    val saving: Boolean = false
)

class ReportFormViewModel(
    private val reports: ReportRepository,
    private val auth: AuthRepository,
    private val images: ImageStorage,
    garden: GardenRepository,
    private val savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(ReportFormState())
    val state = _state.asStateFlow()

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

@Composable
fun ReportFormScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, h -> ReportFormViewModel(c.reportRepository, c.authRepository, c.imageStorage, c.gardenRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar, onBack)
    val noSector: Sector? = null

    Scaffold(
        topBar = { HuertifyTopBar("Reportar problema", onBack) },
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
            ValidatedTextField(state.title, vm::onTitle, "Título del problema *", state.titleError, leadingIcon = Icons.Filled.Title)
            DropdownField(
                label = "Sector afectado",
                options = listOf(noSector) + state.sectors,
                selected = state.sector,
                optionLabel = { it?.name ?: "General / no aplica" },
                onSelected = vm::onSector,
                error = null,
                leadingIcon = Icons.Filled.Yard
            )
            Text("Gravedad *", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Severity.entries.forEach { sev ->
                    FilterChip(selected = state.severity == sev, onClick = { vm.onSeverity(sev) }, label = { Text(sev.label) })
                }
            }
            state.severityError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción *", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 3,
                supportingText = "Ej: plaga de pulgones en las lechugas, manguera rota…"
            )
            Text("Fotografía (opcional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            PhotoPicker(state.photoUri, null, vm::prepareCameraUri, vm::onCameraResult, vm::onGalleryPicked)
            LoadingButton("Enviar reporte", state.saving, vm::submit, Modifier.padding(top = 12.dp), Icons.AutoMirrored.Filled.Send)
        }
    }
}

// ---------- Listado de reportes (admin) ----------

class ReportListViewModel(private val reports: ReportRepository, private val auth: AuthRepository) : BaseViewModel() {
    val reportList = reports.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleResolved(item: ReportWithNames) = launchSafe {
        val resolved = !item.report.resolved
        reports.setResolved(item.report, resolved, auth.requireUser())
        send(UiEvent.ShowMessage(if (resolved) "Reporte marcado como resuelto" else "Reporte reabierto"))
    }
}

@Composable
fun ReportListScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> ReportListViewModel(c.reportRepository, c.authRepository) }
    val list by vm.reportList.collectAsStateWithLifecycle()
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
        val reports = list ?: return@Scaffold
        if (reports.isEmpty()) {
            EmptyState(Icons.Filled.ReportProblem, "No hay problemas reportados", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reports, key = { it.report.id }) { item ->
                val r = item.report
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                                model = it, contentDescription = "Foto del problema", contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (r.resolved) "Resuelto" else "Pendiente", modifier = Modifier.weight(1f))
                            Switch(checked = r.resolved, onCheckedChange = { vm.toggleResolved(item) })
                        }
                    }
                }
            }
        }
    }
}
