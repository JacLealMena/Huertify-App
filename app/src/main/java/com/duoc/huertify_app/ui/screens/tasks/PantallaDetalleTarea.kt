package com.duoc.huertify_app.ui.screens.tasks

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.TaskStateMachine
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.validation.Validators
import com.duoc.huertify_app.ui.common.BaseViewModel
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.ConfirmDialog
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.PhotoPicker
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.UiEvent
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatEpochDay
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Datos editables por el usuario en el detalle (entrega o rechazo). */
data class TaskActionForm(
    val comment: String = "",
    val evidenceUri: String? = null,
    val commentError: String? = null,
    val evidenceError: String? = null,
    val showRejectDialog: Boolean = false,
    val rejectReason: String = "",
    val rejectError: String? = null,
    val working: Boolean = false
)

data class TaskDetailState(
    val item: TaskWithNames? = null,
    val user: User? = null,
    val form: TaskActionForm = TaskActionForm(),
    val loaded: Boolean = false
) {
    val isAdmin get() = user?.role == Role.ADMIN
    val canView get() = item != null && user != null && (isAdmin || item.task.assigneeId == user.id)
}

class TaskDetailViewModel(
    private val tasks: TaskRepository,
    private val auth: AuthRepository,
    private val images: ImageStorage,
    private val savedState: SavedStateHandle
) : BaseViewModel() {
    private val taskId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_TASK_ID))
    private val form = MutableStateFlow(TaskActionForm())

    val state = combine(tasks.observeTask(taskId), auth.currentUser, form) { item, user, f ->
        TaskDetailState(item, user, f, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailState())

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

@Composable
fun TaskDetailScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, h -> TaskDetailViewModel(c.taskRepository, c.authRepository, c.imageStorage, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    CollectEvents(vm.events, snackbar, onBack)

    val item = state.item
    Scaffold(
        topBar = {
            HuertifyTopBar("Detalle de tarea", onBack) {
                if (state.isAdmin && item != null) {
                    if (TaskStateMachine.isEditable(item.task.status)) {
                        IconButton(onClick = { onNavigate(Routes.taskForm(item.task.id)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar tarea")
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar tarea")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (!state.loaded || item == null) return@Scaffold
        if (!state.canView) {
            EmptyState(Icons.Filled.Block, "Acceso restringido: esta tarea no está asignada a ti", Modifier.padding(padding))
            return@Scaffold
        }
        val task = item.task
        val f = state.form
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                StatusChip(task.status.label, task.status.color())
            }
            StatusTimeline(task.status)
            if (task.isOverdue()) {
                Banner(Icons.Filled.Warning, "Tarea atrasada: estaba programada para el ${task.scheduledDate.formatEpochDay()}", MaterialTheme.colorScheme.error)
            }
            InfoRow("Tipo", task.type.label)
            InfoRow("Responsable", item.assigneeName)
            InfoRow("Sector", item.sectorName ?: "Sin sector específico")
            InfoRow("Fecha programada", task.scheduledDate.formatEpochDay())
            InfoRow("Última actualización", task.updatedAt.formatTimestamp())

            SectionTitle("Descripción")
            Text(task.description)
            if (task.instructions.isNotBlank()) {
                SectionTitle("Instrucciones")
                Text(task.instructions)
            }

            if (task.status == TaskStatus.RECHAZADA && task.rejectionReason != null) {
                Banner(Icons.Filled.Close, "Motivo del rechazo: ${task.rejectionReason}", MaterialTheme.colorScheme.error)
            }

            if (task.studentComment != null || task.evidenceUri != null) {
                SectionTitle("Entrega del estudiante")
                task.studentComment?.let { Text("“$it”") }
                task.evidenceUri?.let {
                    AsyncImage(
                        model = it, contentDescription = "Evidencia entregada", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            if (state.isAdmin) {
                AdminActions(task.status, f.working, vm::validate, vm::openRejectDialog)
            } else {
                StudentActions(
                    status = task.status,
                    form = f,
                    onStart = vm::start,
                    onComment = vm::onComment,
                    onSubmit = vm::submit,
                    onRequestCameraUri = vm::prepareCameraUri,
                    onCameraResult = vm::onCameraResult,
                    onGalleryPicked = vm::onGalleryPicked
                )
            }
        }
    }

    if (state.form.showRejectDialog) {
        AlertDialog(
            onDismissRequest = vm::closeRejectDialog,
            title = { Text("Rechazar tarea") },
            text = {
                ValidatedTextField(
                    value = state.form.rejectReason,
                    onValueChange = vm::onRejectReason,
                    label = "Motivo del rechazo *",
                    error = state.form.rejectError,
                    singleLine = false,
                    minLines = 3,
                    supportingText = "El estudiante verá este motivo"
                )
            },
            confirmButton = { TextButton(onClick = vm::reject) { Text("Rechazar") } },
            dismissButton = { TextButton(onClick = vm::closeRejectDialog) { Text("Cancelar") } }
        )
    }
    if (confirmDelete) {
        ConfirmDialog("Eliminar tarea", "Esta acción no se puede deshacer. ¿Eliminar la tarea?", "Eliminar", vm::delete) {
            confirmDelete = false
        }
    }
}

@Composable
private fun AdminActions(status: TaskStatus, working: Boolean, onValidate: () -> Unit, onReject: () -> Unit) {
    when (status) {
        TaskStatus.PENDIENTE_VALIDACION -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onReject, enabled = !working, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Text(" Rechazar")
            }
            Button(
                onClick = onValidate, enabled = !working, modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Text(" Validar")
            }
        }
        TaskStatus.VALIDADA -> Banner(Icons.Filled.CheckCircle, "Tarea validada", Color(0xFF2E7D32))
        else -> Banner(Icons.Filled.HourglassTop, "Esperando que el estudiante complete y entregue la tarea", MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StudentActions(
    status: TaskStatus,
    form: TaskActionForm,
    onStart: () -> Unit,
    onComment: (String) -> Unit,
    onSubmit: () -> Unit,
    onRequestCameraUri: () -> Uri,
    onCameraResult: (Boolean) -> Unit,
    onGalleryPicked: (Uri) -> Unit
) {
    when (status) {
        TaskStatus.PENDIENTE -> LoadingButton("Iniciar tarea", form.working, onStart, icon = Icons.Filled.PlayArrow)
        TaskStatus.RECHAZADA -> LoadingButton("Corregir y rehacer", form.working, onStart, icon = Icons.Filled.Replay)
        TaskStatus.EN_DESARROLLO -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Entregar tarea")
            ValidatedTextField(
                value = form.comment,
                onValueChange = onComment,
                label = "Comentario sobre el trabajo realizado *",
                error = form.commentError,
                leadingIcon = Icons.AutoMirrored.Filled.Comment,
                singleLine = false,
                minLines = 3,
                supportingText = "${form.comment.length}/300"
            )
            Text("Evidencia fotográfica *", style = MaterialTheme.typography.labelLarge)
            PhotoPicker(form.evidenceUri, form.evidenceError, onRequestCameraUri, onCameraResult, onGalleryPicked)
            LoadingButton("Entregar para validación", form.working, onSubmit, Modifier.padding(top = 8.dp), Icons.AutoMirrored.Filled.Send)
        }
        TaskStatus.PENDIENTE_VALIDACION ->
            Banner(Icons.Filled.HourglassTop, "Entregada. Esperando la revisión del docente", MaterialTheme.colorScheme.primary)
        TaskStatus.VALIDADA -> Banner(Icons.Filled.CheckCircle, "¡Tarea validada por el docente!", Color(0xFF2E7D32))
    }
}

/** Muestra el avance en el ciclo Pendiente → En desarrollo → Pendiente de validación → Validada/Rechazada. */
@Composable
private fun StatusTimeline(current: TaskStatus) {
    val steps = listOf(TaskStatus.PENDIENTE, TaskStatus.EN_DESARROLLO, TaskStatus.PENDIENTE_VALIDACION)
    val final = if (current == TaskStatus.RECHAZADA) TaskStatus.RECHAZADA else TaskStatus.VALIDADA
    val all = steps + final
    val reached = when (current) {
        TaskStatus.RECHAZADA -> all.size - 1
        else -> all.indexOf(current)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        all.forEachIndexed { index, step ->
            val active = index <= reached
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (active) step.color() else MaterialTheme.colorScheme.surfaceVariant)
                )
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) step.color() else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text("$label: ", fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}

@Composable
private fun Banner(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.padding(4.dp))
            Text(text, color = color)
        }
    }
}
