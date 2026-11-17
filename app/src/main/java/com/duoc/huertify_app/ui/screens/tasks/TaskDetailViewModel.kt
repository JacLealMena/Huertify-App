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
            form.update { it.copy(evidenceUri = pending, evidenceError = null) }
        } else {
            send(UiEvent.ShowMessage("No se tomó la fotografía"))
        }
    }

    fun onGalleryPicked(uri: Uri) = launchSafe {
        val stored = images.importFromGallery(uri)
        form.update { it.copy(evidenceUri = stored.toString(), evidenceError = null) }
    }

    fun start() = runAction("Tarea iniciada. ¡Manos a la obra!") { tasks.startTask(taskId, it) }

    fun submit() {
        val f = form.value
        val commentError = Validators.requiredText(f.comment, "El comentario", min = 10, max = 300)
        val evidenceError = if (f.evidenceUri == null) "Adjunta una fotografía como evidencia" else null
        form.update { it.copy(commentError = commentError, evidenceError = evidenceError) }
        if (commentError != null || evidenceError != null) {
            showError("Completa el comentario y la evidencia para entregar")
            return
        }
        runAction("Tarea entregada para validación") {
            tasks.submitTask(taskId, f.comment.trim(), f.evidenceUri!!, it)
            form.value = TaskActionForm()
        }
    }

    fun validate() = runAction("Tarea validada ✔") { tasks.validateTask(taskId, it) }

    fun openRejectDialog() = form.update { it.copy(showRejectDialog = true, rejectReason = "", rejectError = null) }
    fun closeRejectDialog() = form.update { it.copy(showRejectDialog = false) }
    fun onRejectReason(v: String) = form.update { it.copy(rejectReason = v, rejectError = null) }

    fun reject() {
        val reason = form.value.rejectReason
        val error = Validators.rejectionReason(reason)
        if (error != null) {
            form.update { it.copy(rejectError = error) }
            return
        }
        form.update { it.copy(showRejectDialog = false) }
        runAction("Tarea rechazada. Se notificó al estudiante") { tasks.rejectTask(taskId, reason.trim(), it) }
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
