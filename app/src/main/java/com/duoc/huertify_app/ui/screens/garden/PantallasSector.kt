package com.duoc.huertify_app.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.SectorWithInfo
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.WateringSchedule
import com.duoc.huertify_app.domain.model.SectorStatus
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.ConfirmDialog
import com.duoc.huertify_app.ui.common.DetailSection
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.InfoLine
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatArea
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.formatted
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ---------- Listado de sectores ----------

class SectorListViewModel(garden: GardenRepository) : BaseViewModel() {
    private val statusFilter = MutableStateFlow<SectorStatus?>(null)

    val state = combine(garden.observeSectorsWithInfo(), statusFilter) { all, filter ->
        SectorListState(all.filter { filter == null || it.sector.status == filter }, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onFilter(status: SectorStatus) = statusFilter.update { if (it == status) null else status }
}

data class SectorListState(val sectors: List<SectorWithInfo>, val filter: SectorStatus?)

@Composable
fun SectorListScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> SectorListViewModel(c.gardenRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { HuertifyTopBar("Sectores del huerto", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Routes.sectorForm()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo sector") }
            )
        }
    ) { padding ->
        val s = state ?: return@Scaffold
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectorStatus.entries.forEach { status ->
                        FilterChip(
                            selected = s.filter == status,
                            onClick = { vm.onFilter(status) },
                            label = { Text(status.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = status.color().copy(alpha = 0.2f),
                                selectedLabelColor = status.color()
                            )
                        )
                    }
                }
            }
            if (s.sectors.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.Yard,
                        if (s.filter == null) "Aún no hay sectores.\nCrea el primero con el botón +" else "No hay sectores en estado \"${s.filter.label}\""
                    )
                }
            }
            items(s.sectors, key = { it.sector.id }) { item -> SectorCard(item) { onNavigate(Routes.sectorDetail(item.sector.id)) } }
        }
    }
}

@Composable
private fun SectorCard(item: SectorWithInfo, onClick: () -> Unit) {
    val sector = item.sector
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Yard, contentDescription = null, tint = sector.status.color(), modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${sector.code} · ${sector.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusChip(sector.status.label, sector.status.color())
                }
                Text(
                    "${sector.location} · ${sector.areaM2.formatArea()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.Grass, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${item.cropCount} cultivo(s) · Responsable: ${item.responsibleName ?: "sin asignar"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ---------- Formulario de sector ----------

data class SectorFormState(
    val id: Long = 0,
    val original: Sector? = null,
    val code: String = "",
    val name: String = "",
    val location: String = "",
    val area: String = "",
    val description: String = "",
    val status: SectorStatus = SectorStatus.DISPONIBLE,
    val responsibleId: Long? = null,
    val users: List<User> = emptyList(),
    val codeError: String? = null,
    val nameError: String? = null,
    val locationError: String? = null,
    val areaError: String? = null,
    val descriptionError: String? = null,
    val responsibleError: String? = null,
    val saving: Boolean = false
) {
    val isEdit get() = id != 0L
    val hasErrors get() = listOf(codeError, nameError, locationError, areaError, descriptionError, responsibleError).any { it != null }
}

class SectorFormViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(SectorFormState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { auth.observeUsers().collect { u -> _state.update { it.copy(users = u) } } }
        val id = savedState.get<Long>(Routes.ARG_SECTOR_ID) ?: 0L
        if (id > 0) viewModelScope.launch {
            garden.findSector(id)?.let { s ->
                _state.update {
                    it.copy(
                        id = s.id, original = s, code = s.code, name = s.name, location = s.location,
                        area = s.areaM2.toString().replace('.', ','), description = s.description,
                        status = s.status, responsibleId = s.responsibleId
                    )
                }
            }
        }
    }

    fun onCode(v: String) = _state.update { it.copy(code = v.uppercase(), codeError = null) }
    fun onName(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onLocation(v: String) = _state.update { it.copy(location = v, locationError = null) }
    fun onArea(v: String) = _state.update { it.copy(area = v, areaError = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v, descriptionError = null) }
    fun onStatus(v: SectorStatus) = _state.update { it.copy(status = v) }
    fun onResponsible(v: User) = _state.update { it.copy(responsibleId = v.id, responsibleError = null) }

    fun save() {
        val s = _state.value
        val validated = s.copy(
            codeError = Validators.sectorCode(s.code),
            nameError = Validators.requiredText(s.name, "El nombre", min = 3, max = 40),
            locationError = Validators.requiredText(s.location, "La ubicación", min = 3, max = 80),
            areaError = Validators.positiveDecimal(s.area, "La superficie"),
            descriptionError = Validators.requiredText(s.description, "La descripción", min = 5, max = 200),
            responsibleError = Validators.required(s.responsibleId, "Selecciona un responsable")
        )
        _state.value = validated
        if (validated.hasErrors) {
            showError("Corrige los campos marcados antes de guardar")
            return
        }
        _state.update { it.copy(saving = true) }
        launchSafe(onFinally = { _state.update { it.copy(saving = false) } }) {
            if (garden.isSectorCodeTaken(s.code, s.id)) {
                _state.update { it.copy(codeError = "Ya existe un sector con este código") }
                showError("El código del sector ya está en uso")
                return@launchSafe
            }
            if (garden.isSectorNameTaken(s.name, s.id)) {
                _state.update { it.copy(nameError = "Ya existe un sector con este nombre") }
                showError("El nombre del sector ya está en uso")
                return@launchSafe
            }
            val base = s.original ?: Sector(code = "", name = "", location = "", areaM2 = 0.0, description = "")
            garden.saveSector(
                base.copy(
                    code = s.code.trim(),
                    name = s.name.trim(),
                    location = s.location.trim(),
                    areaM2 = s.area.trim().replace(',', '.').toDouble(),
                    description = s.description.trim(),
                    status = s.status,
                    responsibleId = s.responsibleId
                ),
                auth.requireUser()
            )
            send(UiEvent.SuccessAndBack(if (s.isEdit) "Cambios del sector guardados" else "Sector \"${s.name.trim()}\" creado"))
        }
    }
}

@Composable
fun SectorFormScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, h -> SectorFormViewModel(c.gardenRepository, c.authRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar, onBack)

    Scaffold(
        topBar = { HuertifyTopBar(if (state.isEdit) "Editar sector" else "Nuevo sector", onBack) },
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
            ValidatedTextField(
                state.code, vm::onCode, "Código *", state.codeError, leadingIcon = Icons.Filled.Badge,
                supportingText = "Ej: S-05",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
            ValidatedTextField(state.name, vm::onName, "Nombre del sector *", state.nameError, leadingIcon = Icons.Filled.Yard)
            ValidatedTextField(state.location, vm::onLocation, "Ubicación dentro del huerto *", state.locationError, leadingIcon = Icons.Filled.Place)
            ValidatedTextField(
                state.area, vm::onArea, "Superficie (m²) *", state.areaError,
                leadingIcon = Icons.Filled.SquareFoot,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción general *", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 3,
                supportingText = "${state.description.length}/200"
            )
            DropdownField(
                label = "Responsable *",
                options = state.users,
                selected = state.users.firstOrNull { it.id == state.responsibleId },
                optionLabel = { "${it.name} (${it.role.label})" },
                onSelected = vm::onResponsible,
                error = state.responsibleError,
                leadingIcon = Icons.Filled.Person
            )
            Text("Estado del sector *", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectorStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.status == status,
                        onClick = { vm.onStatus(status) },
                        label = { Text(status.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = status.color().copy(alpha = 0.2f),
                            selectedLabelColor = status.color()
                        )
                    )
                }
            }
            state.original?.let {
                Text(
                    "Creado: ${it.createdAt.formatTimestamp()} · Última actualización: ${it.updatedAt.formatTimestamp()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            LoadingButton("Guardar", state.saving, vm::save, Modifier.padding(top = 12.dp), Icons.Filled.Save)
        }
    }
}

// ---------- Detalle de sector con sus cultivos ----------

data class SectorDetailState(
    val info: SectorWithInfo? = null,
    val crops: List<Crop> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val loaded: Boolean = false
)

class SectorDetailViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    taskRepository: TaskRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    val sectorId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_SECTOR_ID))

    val state = combine(
        garden.observeSector(sectorId), garden.observeCrops(sectorId), taskRepository.observeAll()
    ) { info, crops, tasks ->
        SectorDetailState(info, crops, tasks.map { it.task }, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SectorDetailState())

    fun deleteSector() {
        val sector = state.value.info?.sector ?: return
        launchSafe {
            garden.deleteSector(sector, auth.requireUser())
            send(UiEvent.SuccessAndBack("Sector \"${sector.name}\" eliminado"))
        }
    }

    fun deleteCrop(crop: Crop) = launchSafe {
        garden.deleteCrop(crop, auth.requireUser())
        send(UiEvent.ShowMessage("Cultivo \"${crop.name}\" eliminado"))
    }
}

@Composable
fun SectorDetailScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, h -> SectorDetailViewModel(c.gardenRepository, c.authRepository, c.taskRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDeleteSector by rememberSaveable { mutableStateOf(false) }
    // Se guarda solo el id (Long) porque rememberSaveable no puede guardar un Crop en el Bundle.
    var cropToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    CollectEvents(vm.events, snackbar, onBack)

    Scaffold(
        topBar = {
            HuertifyTopBar(state.info?.sector?.name ?: "Sector", onBack) {
                IconButton(onClick = { onNavigate(Routes.sectorForm(vm.sectorId)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar sector")
                }
                IconButton(onClick = { confirmDeleteSector = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar sector")
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Routes.cropForm(vm.sectorId)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo cultivo") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val info = state.info
        if (!state.loaded || info == null) return@Scaffold
        val sector = info.sector
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DetailSection("Información del sector", Icons.Filled.Info) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sector.code, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        StatusChip(sector.status.label, sector.status.color())
                    }
                    InfoLine(Icons.Filled.Place, "Ubicación", sector.location)
                    InfoLine(Icons.Filled.SquareFoot, "Superficie", sector.areaM2.formatArea())
                    InfoLine(Icons.Filled.Person, "Responsable", info.responsibleName ?: "Sin asignar")
                    if (sector.description.isNotBlank()) InfoLine(Icons.Filled.Description, "Descripción", sector.description)
                    InfoLine(Icons.Filled.Event, "Creado", sector.createdAt.formatTimestamp())
                    InfoLine(Icons.Filled.Update, "Última actualización", sector.updatedAt.formatTimestamp())
                }
            }
            item {
                Text(
                    "Cultivos (${state.crops.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (state.crops.isEmpty()) {
                item { Text("Este sector aún no tiene cultivos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.crops, key = { it.id }) { crop ->
                CropCard(
                    crop = crop,
                    nextWatering = if (WateringSchedule.needsWatering(crop)) WateringSchedule.nextWatering(crop, state.tasks) else null,
                    onClick = { onNavigate(Routes.cropForm(vm.sectorId, crop.id)) },
                    onDelete = { cropToDeleteId = crop.id }
                )
            }
        }
    }

    if (confirmDeleteSector) {
        ConfirmDialog(
            title = "Eliminar sector",
            text = "Se eliminarán también sus ${state.crops.size} cultivo(s). Las tareas asociadas quedarán sin sector. ¿Continuar?",
            confirmLabel = "Eliminar",
            onConfirm = vm::deleteSector,
            onDismiss = { confirmDeleteSector = false }
        )
    }
    state.crops.firstOrNull { it.id == cropToDeleteId }?.let { crop ->
        ConfirmDialog(
            title = "Eliminar cultivo",
            text = "¿Eliminar \"${crop.name}\" de este sector?",
            confirmLabel = "Eliminar",
            onConfirm = { vm.deleteCrop(crop) },
            onDismiss = { cropToDeleteId = null }
        )
    }
}

@Composable
private fun CropCard(crop: Crop, nextWatering: LocalDate?, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (crop.photoUri != null) {
                AsyncImage(
                    model = crop.photoUri, contentDescription = "Foto referencial de ${crop.name}", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Icon(Icons.Filled.Grass, contentDescription = null, tint = crop.status.color(), modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    crop.name + if (crop.variety.isNotBlank()) " (${crop.variety})" else "",
                    style = MaterialTheme.typography.titleMedium
                )
                StatusChip(crop.status.label, crop.status.color())
                Text(
                    buildString {
                        append("Siembra ${crop.planting.formatted()}")
                        crop.transplant?.let { append(" · Trasplante ${it.formatted()}") }
                        append(" · Cosecha ${crop.harvest.formatted()}")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(wateringText(crop, nextWatering), style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar cultivo")
            }
        }
    }
}

private fun wateringText(crop: Crop, next: LocalDate?): String {
    val base = "Riego cada ${crop.wateringFrequencyDays} día(s)"
    if (next == null) return base
    val days = ChronoUnit.DAYS.between(LocalDate.now(), next)
    val whenText = when {
        days < 0 -> "atrasado desde el ${next.formatted()}"
        days == 0L -> "hoy"
        days == 1L -> "mañana"
        else -> "el ${next.formatted()}"
    }
    return "$base · Próximo: $whenText"
}
