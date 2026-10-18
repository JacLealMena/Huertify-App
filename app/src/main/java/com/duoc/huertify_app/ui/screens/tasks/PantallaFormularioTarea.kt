package com.duoc.huertify_app.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.TaskStateMachine
import com.duoc.huertify_app.domain.model.TaskType
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DateField
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TaskFormState(
    val id: Long = 0,
    val original: Task? = null,
    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val type: TaskType? = null,
    val sectorId: Long? = null,
    val assigneeId: Long? = null,
    val date: LocalDate? = null,
    val students: List<User> = emptyList(),
    val sectors: List<Sector> = emptyList(),
    val titleError: String? = null,
    val descriptionError: String? = null,
    val instructionsError: String? = null,
    val typeError: String? = null,
    val assigneeError: String? = null,
    val dateError: String? = null,
    val saving: Boolean = false
) {
    val isEdit get() = id != 0L
    val hasErrors get() = listOf(titleError, descriptionError, instructionsError, typeError, assigneeError, dateError).any { it != null }
}

class TaskFormViewModel(
    private val tasks: TaskRepository,
    private val auth: AuthRepository,
    garden: GardenRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(TaskFormState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(auth.observeStudents(), garden.observeSectors()) { s, sec -> s to sec }.collect { (students, sectors) ->
                _state.update { it.copy(students = students, sectors = sectors) }
            }
        }
        val id = savedState.get<Long>(Routes.ARG_TASK_ID) ?: 0L
        if (id > 0) viewModelScope.launch {
            tasks.findTask(id)?.let { t ->
                if (!TaskStateMachine.isEditable(t.status)) {
                    showError("Esta tarea ya fue iniciada; solo se pueden editar tareas pendientes")
                }
                _state.update {
                    it.copy(
                        id = t.id, original = t, title = t.title, description = t.description,
                        instructions = t.instructions, type = t.type, sectorId = t.sectorId,
                        assigneeId = t.assigneeId, date = t.scheduled
                    )
                }
            }
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v, titleError = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v, descriptionError = null) }
    fun onInstructions(v: String) = _state.update { it.copy(instructions = v, instructionsError = null) }
    fun onType(v: TaskType) = _state.update { it.copy(type = v, typeError = null) }
    fun onSector(v: Sector?) = _state.update { it.copy(sectorId = v?.id) }
    fun onAssignee(v: User) = _state.update { it.copy(assigneeId = v.id, assigneeError = null) }
    fun onDate(v: LocalDate) = _state.update { it.copy(date = v, dateError = null) }

    fun save() {
        val s = _state.value
        // Al editar se permite conservar una fecha ya pasada si no se modificó.
        val keepsOriginalDate = s.original != null && s.date == s.original.scheduled
        val validated = s.copy(
            titleError = Validators.requiredText(s.title, "El título", min = 3, max = 60),
            descriptionError = Validators.requiredText(s.description, "La descripción", min = 10, max = 300),
            instructionsError = Validators.optionalText(s.instructions, "Las instrucciones", 500),
            typeError = Validators.required(s.type, "Selecciona el tipo de tarea"),
            assigneeError = Validators.required(s.assigneeId, "Selecciona un estudiante responsable"),
            dateError = Validators.scheduledDate(s.date, LocalDate.now(), allowPast = keepsOriginalDate)
        )
        _state.value = validated
        if (validated.hasErrors) {
            showError("Corrige los campos marcados antes de guardar")
            return
        }
        _state.update { it.copy(saving = true) }
        launchSafe(onFinally = { _state.update { it.copy(saving = false) } }) {
            val base = s.original ?: Task(
                title = "", description = "", instructions = "", type = TaskType.OTRO,
                sectorId = null, assigneeId = 0, scheduledDate = 0
            )
            tasks.saveTask(
                base.copy(
                    title = s.title.trim(),
                    description = s.description.trim(),
                    instructions = s.instructions.trim(),
                    type = s.type!!,
                    sectorId = s.sectorId,
                    assigneeId = s.assigneeId!!,
                    scheduledDate = s.date!!.toEpochDay()
                ),
                auth.requireUser()
            )
            send(UiEvent.SuccessAndBack(if (s.isEdit) "Tarea actualizada" else "Tarea creada y asignada"))
        }
    }
}

@Composable
fun TaskFormScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, h -> TaskFormViewModel(c.taskRepository, c.authRepository, c.gardenRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar, onBack)
    val noSector: Sector? = null

    Scaffold(
        topBar = { HuertifyTopBar(if (state.isEdit) "Editar tarea" else "Nueva tarea", onBack) },
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
            ValidatedTextField(state.title, vm::onTitle, "Título *", state.titleError, leadingIcon = Icons.Filled.Title)
            DropdownField(
                label = "Tipo de tarea *",
                options = TaskType.entries,
                selected = state.type,
                optionLabel = { it.label },
                onSelected = vm::onType,
                error = state.typeError,
                leadingIcon = Icons.Filled.Category
            )
            DropdownField(
                label = "Responsable *",
                options = state.students,
                selected = state.students.firstOrNull { it.id == state.assigneeId },
                optionLabel = { it.name },
                onSelected = vm::onAssignee,
                error = state.assigneeError,
                leadingIcon = Icons.Filled.Person
            )
            DropdownField(
                label = "Sector",
                options = listOf(noSector) + state.sectors,
                selected = state.sectors.firstOrNull { it.id == state.sectorId },
                optionLabel = { it?.name ?: "Sin sector específico" },
                onSelected = vm::onSector,
                error = null,
                leadingIcon = Icons.Filled.Yard
            )
            DateField("Fecha programada *", state.date, vm::onDate, state.dateError)
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción *", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 2
            )
            ValidatedTextField(
                state.instructions, vm::onInstructions, "Instrucciones para el estudiante", state.instructionsError,
                leadingIcon = Icons.Filled.Info, singleLine = false, minLines = 3
            )
            LoadingButton(
                if (state.isEdit) "Guardar cambios" else "Crear y asignar",
                state.saving, vm::save, Modifier.padding(top = 12.dp),
                if (state.isEdit) Icons.Filled.Save else Icons.AutoMirrored.Filled.Assignment
            )
        }
    }
}
