package com.duoc.huertify_app.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
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
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskFormViewModel(
    private val tasks: TaskRepository,
    private val auth: AuthRepository,
    garden: GardenRepository,
    savedState: SavedStateHandle
) : BaseViewModel() {
    private val _state = MutableStateFlow(TaskFormUiState())
    val state: StateFlow<TaskFormUiState> = _state.asStateFlow()

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
