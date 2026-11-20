package com.duoc.huertify_app.ui.screens.tasks

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class TaskDetailViewModel(
    private val tasks: TaskRepository,
    private val auth: AuthRepository,
    private val images: ImageStorage,
    private val savedState: SavedStateHandle
) : BaseViewModel() {
    private val taskId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_TASK_ID))
    private val form = MutableStateFlow(TaskActionForm())

    val state: StateFlow<TaskDetailUiState> = combine(tasks.observeTask(taskId), auth.currentUser, form) { item, user, f ->
        TaskDetailUiState(item, user, f, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState())

    fun onComment(v: String) = form.update { it.copy(comment = v, commentError = null) }

    fun prepareCameraUri(): Uri = images.newCameraUri().also { savedState[KEY_PENDING_PHOTO] = it.toString() }

    fun onCameraResult(success: Boolean) {
        val pending = savedState.get<String>(KEY_PENDING_PHOTO)
        if (success && pending != null) {
            form.update { it.copy(evidenceUri = pending) }
        } else {
            send(UiEvent.ShowMessage("No se tomó la fotografía"))
        }
    }

    fun onGalleryPicked(uri: Uri) = launchSafe {
        val stored = images.importFromGallery(uri)
        form.update { it.copy(evidenceUri = stored.toString()) }
    }

    fun removePhoto() = form.update { it.copy(evidenceUri = null) }

    fun start() = runAction("Tarea iniciada. ¡Manos a la obra!") { tasks.startTask(taskId, it) }

    fun requestSubmit() {
        val error = Validators.optionalText(form.value.comment, "El comentario", 300)
        if (error != null) {
            form.update { it.copy(commentError = error) }
            return
        }
        form.update { it.copy(showSubmitConfirm = true) }
    }

    fun dismissSubmit() = form.update { it.copy(showSubmitConfirm = false) }

    fun confirmSubmit() {
        val f = form.value
        form.update { it.copy(showSubmitConfirm = false) }
        runAction("Tarea marcada como realizada. Queda pendiente de validación") {
            tasks.submitTask(taskId, f.comment.trim(), f.evidenceUri, it)
            form.value = TaskActionForm()
        }
    }

    fun openValidateDialog() = form.update { it.copy(showValidateDialog = true, validateComment = "") }
    fun closeValidateDialog() = form.update { it.copy(showValidateDialog = false) }
    fun onValidateComment(v: String) = form.update { it.copy(validateComment = v) }

    fun validate() {
        val comment = form.value.validateComment.trim()
        form.update { it.copy(showValidateDialog = false) }
        runAction("Tarea validada ✔") { tasks.validateTask(taskId, it, comment) }
    }

    fun openRejectDialog() = form.update {
        it.copy(showRejectDialog = true, rejectReason = "", rejectError = null, rescheduleDate = null, rescheduleError = null)
    }
    fun closeRejectDialog() = form.update { it.copy(showRejectDialog = false) }
    fun onRejectReason(v: String) = form.update { it.copy(rejectReason = v, rejectError = null) }
    fun onReschedule(v: LocalDate?) = form.update { it.copy(rescheduleDate = v, rescheduleError = null) }

    fun reject() {
        val f = form.value
        val error = Validators.rejectionReason(f.rejectReason)
        val dateError = f.rescheduleDate?.let { Validators.scheduledDate(it, LocalDate.now(), allowPast = false) }
        if (error != null || dateError != null) {
            form.update { it.copy(rejectError = error, rescheduleError = dateError) }
            return
        }
        form.update { it.copy(showRejectDialog = false) }
        val msg = if (f.rescheduleDate != null) "Tarea rechazada y reprogramada" else "Tarea rechazada. Se notificó al estudiante"
        runAction(msg) { tasks.rejectTask(taskId, f.rejectReason.trim(), it, f.rescheduleDate) }
    }

    fun delete() {
        val task = state.value.item?.task ?: return
        launchSafe {
            tasks.deleteTask(task, auth.requireUser())
            send(UiEvent.SuccessAndBack("Tarea eliminada"))
        }
    }

    private fun runAction(success: String, block: suspend (User) -> Unit) {
        form.update { it.copy(working = true) }
        launchSafe(onFinally = { form.update { it.copy(working = false) } }) {
            block(auth.requireUser())
            send(UiEvent.ShowMessage(success))
        }
    }

    private companion object {
        const val KEY_PENDING_PHOTO = "pending_photo"
    }
}
