package com.duoc.huertify_app.ui.screens.reports

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.local.CropWithSector
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.domain.model.ProblemType
import com.duoc.huertify_app.domain.model.ReportStatus
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.InfoBanner
import com.duoc.huertify_app.ui.common.InfoLine
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.PhotoPicker
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- Formulario de reporte ----------

data class ReportFormState(
    val type: ProblemType? = null,
    val description: String = "",
    val sector: Sector? = null,
    val cropId: Long? = null,
    val photoUri: String? = null,
    val sectors: List<Sector> = emptyList(),
    val crops: List<CropWithSector> = emptyList(),
    val typeError: String? = null,
    val descriptionError: String? = null,
    val saving: Boolean = false
) {
    val availableCrops get() = crops.filter { sector == null || it.crop.sectorId == sector.id }
}

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
        viewModelScope.launch {
            combine(garden.observeSectors(), garden.observeAllCrops()) { s, c -> s to c }.collect { (s, c) ->
                _state.update { it.copy(sectors = s, crops = c) }
            }
        }
    }

    fun onType(v: ProblemType) = _state.update { it.copy(type = v, typeError = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v, descriptionError = null) }
    fun onSector(v: Sector?) = _state.update {
        val keepCrop = it.crops.any { c -> c.crop.id == it.cropId && (v == null || c.crop.sectorId == v.id) }
        it.copy(sector = v, cropId = if (keepCrop) it.cropId else null)
    }
    fun onCrop(v: CropWithSector?) = _state.update {
        it.copy(cropId = v?.crop?.id, sector = v?.let { c -> it.sectors.firstOrNull { s -> s.id == c.crop.sectorId } } ?: it.sector)
    }

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
            descriptionError = Validators.requiredText(s.description, "La descripción", min = 10, max = 300)
        )
        _state.value = validated
        if (validated.typeError != null || validated.descriptionError != null) {
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
                    cropId = s.cropId,
                    type = s.type!!,
                    description = s.description.trim(),
                    photoUri = s.photoUri
                ),
                user
            )
            send(UiEvent.SuccessAndBack("Problema informado al docente"))
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
    val noCrop: CropWithSector? = null

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
            Text("Tipo de problema *", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProblemType.entries.forEach { type ->
                    FilterChip(selected = state.type == type, onClick = { vm.onType(type) }, label = { Text(type.label) })
                }
            }
            state.typeError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            DropdownField(
                label = "Sector afectado",
                options = listOf(noSector) + state.sectors,
                selected = state.sector,
                optionLabel = { it?.name ?: "General / no aplica" },
                onSelected = vm::onSector,
                error = null,
                leadingIcon = Icons.Filled.Yard,
                modifier = Modifier.padding(top = 8.dp)
            )
            DropdownField(
                label = "Cultivo afectado",
                options = listOf(noCrop) + state.availableCrops,
                selected = state.crops.firstOrNull { it.crop.id == state.cropId },
                optionLabel = { it?.let { c -> "${c.crop.name} · ${c.sectorName}" } ?: "No aplica" },
                onSelected = vm::onCrop,
                error = null,
                leadingIcon = Icons.Filled.Grass
            )
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción breve *", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 3,
                supportingText = "Ej: hojas mordidas en las lechugas, manguera rota… (${state.description.length}/300)"
            )
            Text("Fotografía (opcional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Fotografía solo plantas, herramientas o el sector. No incluyas personas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PhotoPicker(state.photoUri, null, vm::prepareCameraUri, vm::onCameraResult, vm::onGalleryPicked)
            LoadingButton("Enviar reporte", state.saving, vm::submit, Modifier.padding(top = 12.dp), Icons.AutoMirrored.Filled.Send)
        }
    }
}

// ---------- Listado y seguimiento de reportes ----------

data class ReportListState(
    val user: User? = null,
    val reports: List<ReportWithNames>? = null,
    val editingId: Long? = null,
    val newStatus: ReportStatus = ReportStatus.EN_REVISION,
    val comment: String = ""
) {
    val isAdmin get() = user?.role == Role.ADMIN
}

/** El administrador ve y gestiona todos los reportes; el estudiante consulta los suyos y el comentario del docente. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportListViewModel(private val reports: ReportRepository, private val auth: AuthRepository) : BaseViewModel() {
    private val editor = MutableStateFlow(Editor())

    private data class Editor(val id: Long? = null, val status: ReportStatus = ReportStatus.EN_REVISION, val comment: String = "")

    val state = auth.currentUser.filterNotNull().flatMapLatest { user ->
        val source = if (user.role == Role.ADMIN) reports.observeAll() else reports.observeForReporter(user.id)
        combine(source, editor) { list, e -> ReportListState(user, list, e.id, e.status, e.comment) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportListState())

    fun openEditor(item: ReportWithNames) = editor.update {
        val next = ReportStatus.entries.getOrNull(item.report.status.ordinal + 1) ?: item.report.status
        Editor(item.report.id, next, item.report.adminComment.orEmpty())
    }
    fun closeEditor() = editor.update { Editor() }
    fun onStatus(v: ReportStatus) = editor.update { it.copy(status = v) }
    fun onComment(v: String) = editor.update { it.copy(comment = v) }

    fun saveFollowUp() {
        val e = editor.value
        val id = e.id ?: return
        if (e.comment.length > 300) {
            showError("El comentario no puede superar los 300 caracteres")
            return
        }
        editor.value = Editor()
        launchSafe {
            reports.updateStatus(id, e.status, e.comment, auth.requireUser())
            send(UiEvent.ShowMessage("Reporte actualizado a \"${e.status.label}\""))
        }
    }
}

@Composable
fun ReportListScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> ReportListViewModel(c.reportRepository, c.authRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar)

    Scaffold(
        topBar = { HuertifyTopBar(if (state.isAdmin) "Problemas del huerto" else "Mis reportes", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Routes.REPORT_FORM) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Reportar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val reports = state.reports ?: return@Scaffold
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
                ReportCard(item, state.isAdmin) { vm.openEditor(item) }
            }
        }
    }

    val editing = state.reports?.firstOrNull { it.report.id == state.editingId }
    if (editing != null) {
        AlertDialog(
            onDismissRequest = vm::closeEditor,
            title = { Text("Seguimiento del reporte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${editing.report.type.label}: ${editing.report.description}", style = MaterialTheme.typography.bodySmall)
                    Text("Nuevo estado", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportStatus.entries.forEach { status ->
                            FilterChip(
                                selected = state.newStatus == status,
                                onClick = { vm.onStatus(status) },
                                label = { Text(status.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = status.color().copy(alpha = 0.2f),
                                    selectedLabelColor = status.color()
                                )
                            )
                        }
                    }
                    ValidatedTextField(
                        value = state.comment,
                        onValueChange = vm::onComment,
                        label = "Comentario para el estudiante",
                        error = null,
                        singleLine = false,
                        minLines = 3,
                        supportingText = "Será visible para quien reportó"
                    )
                }
            },
            confirmButton = { TextButton(onClick = vm::saveFollowUp) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = vm::closeEditor) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ReportCard(item: ReportWithNames, isAdmin: Boolean, onFollowUp: () -> Unit) {
    val r = item.report
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = r.status.color())
                Text(r.type.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
                StatusChip(r.status.label, r.status.color())
            }
            Text(r.description, style = MaterialTheme.typography.bodyLarge)
            r.photoUri?.let {
                AsyncImage(
                    model = it, contentDescription = "Foto del problema", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                )
            }
            HorizontalDivider()
            InfoLine(Icons.Filled.Yard, "Sector / cultivo", listOfNotNull(item.sectorName ?: "General", item.cropName).joinToString(" · "))
            InfoLine(Icons.Filled.Person, "Reportado por", item.reporterName)
            InfoLine(Icons.Filled.Event, "Fecha del reporte", r.createdAt.formatTimestamp())
            r.attendedAt?.let { InfoLine(Icons.Filled.EventAvailable, "Atendido / cerrado", it.formatTimestamp()) }
            if (r.adminComment != null) {
                InfoBanner(Icons.AutoMirrored.Filled.Comment, "Comentario del docente: ${r.adminComment}", MaterialTheme.colorScheme.primary)
            } else if (!isAdmin) {
                Text(
                    "El docente aún no ha comentado este reporte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isAdmin) {
                OutlinedButton(onClick = onFollowUp, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.EditNote, contentDescription = null)
                    Text(" Cambiar estado / comentar")
                }
            }
        }
    }
}
