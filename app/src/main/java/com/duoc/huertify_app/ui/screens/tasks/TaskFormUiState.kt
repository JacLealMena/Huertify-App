package com.duoc.huertify_app.ui.screens.tasks

import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.domain.model.TaskType
import java.time.LocalDate

typealias TaskFormState = TaskFormUiState

/**
 * Estado UI para el formulario de tareas.
 */
data class TaskFormUiState(
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
