package com.duoc.huertify_app.ui.screens.tasks

import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.domain.model.Role
import java.time.LocalDate

typealias TaskDetailState = TaskDetailUiState

/**
 * Datos editables por el usuario en el detalle (entrega, validación o rechazo).
 */
data class TaskActionForm(
    val comment: String = "",
    val evidenceUri: String? = null,
    val commentError: String? = null,
    val showSubmitConfirm: Boolean = false,
    val showValidateDialog: Boolean = false,
    val validateComment: String = "",
    val showRejectDialog: Boolean = false,
    val rejectReason: String = "",
    val rejectError: String? = null,
    val rescheduleDate: LocalDate? = null,
    val rescheduleError: String? = null,
    val working: Boolean = false
)

/**
 * Estado UI principal para el detalle de una tarea.
 */
data class TaskDetailUiState(
    val item: TaskWithNames? = null,
    val user: User? = null,
    val form: TaskActionForm = TaskActionForm(),
    val loaded: Boolean = false
) {
    val isAdmin get() = user?.role == Role.ADMIN
    val canView get() = item != null && user != null && (isAdmin || item.task.assigneeId == user.id)
}
