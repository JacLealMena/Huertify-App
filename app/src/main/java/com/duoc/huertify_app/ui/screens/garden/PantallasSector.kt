package com.duoc.huertify_app.ui.screens.garden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.ConfirmDialog
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.formatArea
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

// ---------- Listado de sectores ----------

class SectorListViewModel(garden: GardenRepository) : BaseViewModel() {
    val sectors = garden.observeSectors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun SectorListScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, _ -> SectorListViewModel(c.gardenRepository) }
    val sectors by vm.sectors.collectAsStateWithLifecycle()

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
        val list = sectors ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.Yard, "Aún no hay sectores.\nCrea el primero con el botón +", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(list, key = { _, sector -> sector.id }) { index, sector ->

                    var visible by remember(sector.id) {
                        mutableStateOf(false)
                    }

                    LaunchedEffect(sector.id) {
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = index * 100
                            )
                        )
                    ) {
                        ElevatedCard(
                            onClick = {
                                onNavigate(Routes.sectorDetail(sector.id))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Yard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.width(16.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        sector.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        "${sector.location} · ${sector.areaM2.formatArea()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Formulario de sector ----------

data class SectorFormState(
    val id: Long = 0,
    val name: String = "",
    val location: String = "",
    val area: String = "",
    val description: String = "",
    val nameError: String? = null,
    val locationError: String? = null,
    val areaError: String? = null,
    val descriptionError: String? = null,
    val saving: Boolean = false
) {
    val isEdit get() = id != 0L
}

class SectorFormViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(SectorFormState())
    val state = _state.asStateFlow()

    init {
        val id = savedState.get<Long>(Routes.ARG_SECTOR_ID) ?: 0L
        if (id > 0) viewModelScope.launch {
            garden.findSector(id)?.let { s ->
                _state.value = SectorFormState(s.id, s.name, s.location, s.areaM2.toString().replace('.', ','), s.description)
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
            ValidatedTextField(state.name, vm::onName, "Nombre del sector *", state.nameError, leadingIcon = Icons.Filled.Yard)
            ValidatedTextField(state.location, vm::onLocation, "Ubicación *", state.locationError, leadingIcon = Icons.Filled.Place)
            ValidatedTextField(
                state.area, vm::onArea, "Superficie (m²) *", state.areaError,
                leadingIcon = Icons.Filled.SquareFoot,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 3,
                supportingText = "${state.description.length}/200"
            )
            LoadingButton("Guardar", state.saving, vm::save, Modifier.padding(top = 12.dp), Icons.Filled.Save)
        }
    }
}

// ---------- Detalle de sector con sus cultivos ----------

data class SectorDetailState(val sector: Sector? = null, val crops: List<Crop> = emptyList(), val loaded: Boolean = false)

class SectorDetailViewModel(
    private val garden: GardenRepository,
    private val auth: AuthRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    val sectorId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_SECTOR_ID))

    val state = combine(garden.observeSector(sectorId), garden.observeCrops(sectorId)) { sector, crops ->
        SectorDetailState(sector, crops, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SectorDetailState())

    fun deleteSector() {
        val sector = state.value.sector ?: return
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
    val vm = huertifyViewModel { c, h -> SectorDetailViewModel(c.gardenRepository, c.authRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDeleteSector by remember { mutableStateOf(false) }
    var cropToDelete by remember { mutableStateOf<Crop?>(null) }
    CollectEvents(vm.events, snackbar, onBack)

    Scaffold(
        topBar = {
            HuertifyTopBar(state.sector?.name ?: "Sector", onBack) {
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
        val sector = state.sector
        if (!state.loaded || sector == null) return@Scaffold
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("📍 ${sector.location}")
                Text("📐 ${sector.areaM2.formatArea()}")
                if (sector.description.isNotBlank()) Text(sector.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SectionTitle("Cultivos (${state.crops.size})") }
            if (state.crops.isEmpty()) {
                item { Text("Este sector aún no tiene cultivos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.crops, key = { it.id }) { crop ->
                ElevatedCard(onClick = { onNavigate(Routes.cropForm(vm.sectorId, crop.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Grass, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                crop.name + if (crop.variety.isNotBlank()) " (${crop.variety})" else "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Siembra ${crop.planting.formatted()} · Cosecha ${crop.harvest.formatted()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text("Riego cada ${crop.wateringFrequencyDays} día(s)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { cropToDelete = crop }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar cultivo")
                        }
                    }
                }
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
    cropToDelete?.let { crop ->
        ConfirmDialog(
            title = "Eliminar cultivo",
            text = "¿Eliminar \"${crop.name}\" de este sector?",
            confirmLabel = "Eliminar",
            onConfirm = { vm.deleteCrop(crop) },
            onDismiss = { cropToDelete = null }
        )
    }
}
