package com.duoc.huertify_app.ui.screens.tasks

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
<<<<<<< HEAD:app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/TaskDetailScreen.kt
=======
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
>>>>>>> 80f66d5 (backend con xampp y phpmyadmin):app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/PantallaDetalleTarea.kt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.duoc.huertify_app.domain.TaskStateMachine
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.ConfirmDialog
import com.duoc.huertify_app.ui.common.DateField
import com.duoc.huertify_app.ui.common.DetailSection
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.InfoBanner
import com.duoc.huertify_app.ui.common.InfoLine
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.PhotoPicker
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatEpochDay
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes
<<<<<<< HEAD:app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/TaskDetailScreen.kt
=======
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

private val VALID_GREEN = Color(0xFF2E7D32)

/** Datos editables por el usuario en el detalle (entrega, validación o rechazo). */
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

    /** Valida el comentario (opcional) y pide confirmación antes de marcar la tarea como realizada. */
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
>>>>>>> 80f66d5 (backend con xampp y phpmyadmin):app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/PantallaDetalleTarea.kt

@Composable
fun TaskDetailScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = huertifyViewModel { c, h -> TaskDetailViewModel(c.taskRepository, c.authRepository, c.imageStorage, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
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
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TaskHeader(item)

            // 1. Proceso: en qué etapa está la tarea y qué viene después.
            DetailSection("Proceso", Icons.Filled.AccountTree, subtitle = "Etapa actual de la tarea") {
                StatusStepper(task.status)
                InfoBanner(Icons.Filled.Lightbulb, nextStepHint(task.status, state.isAdmin), MaterialTheme.colorScheme.primary)
            }

            // 2. Detalle: datos de la tarea.
            DetailSection("Detalle", Icons.Filled.Info) {
                InfoLine(
                    Icons.Filled.Event, "Fecha programada", task.scheduledDate.formatEpochDay(),
                    if (task.isOverdue()) MaterialTheme.colorScheme.error else Color.Unspecified
                )
                InfoLine(Icons.Filled.Yard, "Sector", item.sectorName ?: "Sin sector específico")
                InfoLine(Icons.Filled.Grass, "Cultivo", item.cropName ?: "Sin cultivo específico")
                InfoLine(Icons.Filled.Person, "Responsable", item.assigneeName)
                task.groupName?.let { InfoLine(Icons.Filled.Groups, "Grupo / taller", it) }
                InfoLine(Icons.Filled.School, "Creada por", "${item.creatorName ?: "Docente"} · ${task.createdAt.formatTimestamp()}")
            }

            // 3. Descripción e instrucciones, separadas.
            DetailSection("Descripción", Icons.Filled.Description) {
                Text(task.description, style = MaterialTheme.typography.bodyLarge)
            }
            if (task.instructions.isNotBlank()) {
<<<<<<< HEAD:app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/TaskDetailScreen.kt
                SectionTitle("Instrucciones")
                Text(task.instructions)
            }

            if (task.status == TaskStatus.RECHAZADA && task.rejectionReason != null) {
                Banner(Icons.Filled.Close, "Motivo del rechazo: ${task.rejectionReason}", MaterialTheme.colorScheme.error)
            }

            if (task.studentComment != null || task.evidenceUri != null) {
                SectionTitle("Entrega del estudiante")
                task.studentComment?.let { Text("“$it”") }
                task.evidenceUri?.let {uri ->
                    var visible by remember(uri) { mutableStateOf(false) }
                    LaunchedEffect(uri) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 3000)
                        ) + slideInVertically(
                            animationSpec = tween(durationMillis = 3000),
                            initialOffsetY = { fullHeight -> fullHeight / 4 }
                        )
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Evidencia entregada",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
=======
                DetailSection("Instrucciones", Icons.AutoMirrored.Filled.FormatListBulleted, subtitle = "Sigue estos pasos en orden") {
                    instructionSteps(task.instructions).forEachIndexed { index, step -> InstructionStep(index + 1, step) }
>>>>>>> 80f66d5 (backend con xampp y phpmyadmin):app/src/main/java/com/duoc/huertify_app/ui/screens/tasks/PantallaDetalleTarea.kt
                }
            }

            // 4. Entrega y revisión (cuando existen).
            if (task.completedAt != null || task.studentComment != null || task.evidenceUri != null) {
                DetailSection("Entrega del estudiante", Icons.AutoMirrored.Filled.Send) {
                    task.completedAt?.let { InfoLine(Icons.Filled.EventAvailable, "Marcada como realizada", it.formatTimestamp()) }
                    InfoLine(Icons.AutoMirrored.Filled.Comment, "Comentario", task.studentComment?.let { "“$it”" } ?: "Sin comentario")
                    task.evidenceUri?.let {
                        AsyncImage(
                            model = it, contentDescription = "Evidencia entregada", contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))
                        )
                    } ?: InfoLine(Icons.Filled.PhotoCamera, "Fotografía", "Sin fotografía adjunta")
                }
            }
            ReviewSection(item)

            // 5. Acciones disponibles según el rol y el estado.
            DetailSection(if (state.isAdmin) "Revisión" else "¿Qué debes hacer?", Icons.Filled.TouchApp) {
                if (state.isAdmin) {
                    AdminActions(task.status, state.form.working, vm::openValidateDialog, vm::openRejectDialog)
                } else {
                    StudentActions(
                        status = task.status,
                        form = state.form,
                        onStart = vm::start,
                        onComment = vm::onComment,
                        onSubmit = vm::requestSubmit,
                        onRequestCameraUri = vm::prepareCameraUri,
                        onCameraResult = vm::onCameraResult,
                        onGalleryPicked = vm::onGalleryPicked,
                        onRemovePhoto = vm::removePhoto
                    )
                }
            }
        }
    }

    TaskDialogs(state, vm)
    if (confirmDelete) {
        ConfirmDialog("Eliminar tarea", "Esta acción no se puede deshacer. ¿Eliminar la tarea?", "Eliminar", vm::delete) {
            confirmDelete = false
        }
    }
}

@Composable
private fun TaskHeader(item: TaskWithNames) {
    val task = item.task
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(task.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.type.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }
            StatusChip(task.status.label, task.status.color())
            if (task.isOverdue()) {
                InfoBanner(
                    Icons.Filled.Warning,
                    "Tarea atrasada: estaba programada para el ${task.scheduledDate.formatEpochDay()}",
                    MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ReviewSection(item: TaskWithNames) {
    val task = item.task
    when {
        task.status == TaskStatus.RECHAZADA && task.rejectionReason != null ->
            DetailSection("Revisión del docente", Icons.AutoMirrored.Filled.FactCheck) {
                InfoBanner(Icons.Filled.Close, "Rechazada: ${task.rejectionReason}", MaterialTheme.colorScheme.error)
                ReviewerLine(item)
                InfoLine(Icons.Filled.Event, "Nueva fecha programada", task.scheduledDate.formatEpochDay())
            }
        task.status == TaskStatus.VALIDADA ->
            DetailSection("Revisión del docente", Icons.AutoMirrored.Filled.FactCheck) {
                InfoBanner(Icons.Filled.CheckCircle, "Trabajo validado", VALID_GREEN)
                task.reviewComment?.let { InfoLine(Icons.AutoMirrored.Filled.Comment, "Comentario del docente", "“$it”") }
                ReviewerLine(item)
            }
    }
}

@Composable
private fun ReviewerLine(item: TaskWithNames) {
    val task = item.task
    if (task.reviewedAt != null) {
        InfoLine(Icons.Filled.School, "Revisado por", "${item.reviewerName ?: "Docente"} · ${task.reviewedAt.formatTimestamp()}")
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
                colors = ButtonDefaults.buttonColors(containerColor = VALID_GREEN)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Text(" Validar")
            }
        }
        TaskStatus.VALIDADA -> InfoBanner(Icons.Filled.CheckCircle, "Tarea validada. No requiere más acciones", VALID_GREEN)
        else -> InfoBanner(Icons.Filled.HourglassTop, "Esperando que el estudiante realice y entregue la tarea", MaterialTheme.colorScheme.primary)
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
    onGalleryPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit
) {
    when (status) {
        TaskStatus.PENDIENTE -> {
            Text("Cuando comiences a trabajar, presiona el botón.", style = MaterialTheme.typography.bodyMedium)
            LoadingButton("Iniciar tarea", form.working, onStart, icon = Icons.Filled.PlayArrow)
        }
        TaskStatus.RECHAZADA -> {
            Text("Revisa el motivo del rechazo y vuelve a realizar la tarea.", style = MaterialTheme.typography.bodyMedium)
            LoadingButton("Corregir y rehacer", form.working, onStart, icon = Icons.Filled.Replay)
        }
        TaskStatus.EN_DESARROLLO -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Al terminar, puedes dejar un comentario y una foto (ambos opcionales).", style = MaterialTheme.typography.bodyMedium)
            ValidatedTextField(
                value = form.comment,
                onValueChange = onComment,
                label = "Comentario (opcional)",
                error = form.commentError,
                leadingIcon = Icons.AutoMirrored.Filled.Comment,
                singleLine = false,
                minLines = 2,
                supportingText = "${form.comment.length}/300"
            )
            Text("Fotografía del avance (opcional)", style = MaterialTheme.typography.labelLarge)
            Text(
                "Fotografía solo plantas, herramientas o materiales. No incluyas personas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PhotoPicker(form.evidenceUri, null, onRequestCameraUri, onCameraResult, onGalleryPicked)
            if (form.evidenceUri != null) {
                TextButton(onClick = onRemovePhoto) { Text("Quitar fotografía") }
            }
            LoadingButton("Marcar como realizada", form.working, onSubmit, Modifier.padding(top = 4.dp), Icons.Filled.Check)
        }
        TaskStatus.PENDIENTE_VALIDACION ->
            InfoBanner(Icons.Filled.HourglassTop, "Entregada. Esperando la revisión del docente", MaterialTheme.colorScheme.primary)
        TaskStatus.VALIDADA -> InfoBanner(Icons.Filled.CheckCircle, "¡Tarea validada por el docente!", VALID_GREEN)
    }
}

@Composable
private fun TaskDialogs(state: TaskDetailState, vm: TaskDetailViewModel) {
    val f = state.form
    if (f.showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = vm::dismissSubmit,
            icon = { Icon(Icons.Filled.Check, contentDescription = null) },
            title = { Text("¿Marcar la tarea como realizada?") },
            text = {
                Text(
                    "La tarea quedará pendiente de validación hasta que el docente la revise." +
                        (if (f.evidenceUri == null) "\n\nNo adjuntaste fotografía." else "") +
                        (if (f.comment.isBlank()) "\nNo dejaste comentario." else "")
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmSubmit) { Text("Sí, marcar como realizada") } },
            dismissButton = { TextButton(onClick = vm::dismissSubmit) { Text("Cancelar") } }
        )
    }
    if (f.showValidateDialog) {
        AlertDialog(
            onDismissRequest = vm::closeValidateDialog,
            title = { Text("Validar tarea") },
            text = {
                ValidatedTextField(
                    value = f.validateComment,
                    onValueChange = vm::onValidateComment,
                    label = "Comentario para el estudiante (opcional)",
                    error = null,
                    singleLine = false,
                    minLines = 2
                )
            },
            confirmButton = { TextButton(onClick = vm::validate) { Text("Validar") } },
            dismissButton = { TextButton(onClick = vm::closeValidateDialog) { Text("Cancelar") } }
        )
    }
    if (f.showRejectDialog) {
        AlertDialog(
            onDismissRequest = vm::closeRejectDialog,
            title = { Text("Rechazar tarea") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ValidatedTextField(
                        value = f.rejectReason,
                        onValueChange = vm::onRejectReason,
                        label = "Motivo del rechazo *",
                        error = f.rejectError,
                        singleLine = false,
                        minLines = 3,
                        supportingText = "El estudiante verá este comentario"
                    )
                    DateField("Reprogramar para (opcional)", f.rescheduleDate, { vm.onReschedule(it) }, f.rescheduleError)
                    if (f.rescheduleDate != null) {
                        TextButton(onClick = { vm.onReschedule(null) }) { Text("Mantener la fecha actual") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::reject) { Text("Rechazar") } },
            dismissButton = { TextButton(onClick = vm::closeRejectDialog) { Text("Cancelar") } }
        )
    }
}

/** Línea de proceso con pasos numerados: Pendiente → En desarrollo → Pendiente de validación → Validada/Rechazada. */
@Composable
private fun StatusStepper(current: TaskStatus) {
    val final = if (current == TaskStatus.RECHAZADA) TaskStatus.RECHAZADA else TaskStatus.VALIDADA
    val steps = listOf(TaskStatus.PENDIENTE, TaskStatus.EN_DESARROLLO, TaskStatus.PENDIENTE_VALIDACION, final)
    val reached = if (current == TaskStatus.RECHAZADA) steps.lastIndex else steps.indexOf(current)
    Row(Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val done = index < reached
            val active = index == reached
            val color = if (done || active) step.color() else MaterialTheme.colorScheme.outlineVariant
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Conector a la izquierda (no se dibuja en el primer paso).
                    Spacer(
                        Modifier.weight(1f).height(3.dp)
                            .background(if (index == 0) Color.Transparent else if (index <= reached) steps[index - 1].color() else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (done || active) color else Color.Transparent)
                            .border(2.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            done -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            active && step == TaskStatus.RECHAZADA ->
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            else -> Text(
                                "${index + 1}",
                                color = if (active) Color.White else color,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(
                        Modifier.weight(1f).height(3.dp)
                            .background(if (index == steps.lastIndex) Color.Transparent else if (index < reached) step.color() else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done || active) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

private fun nextStepHint(status: TaskStatus, isAdmin: Boolean): String = when (status) {
    TaskStatus.PENDIENTE ->
        if (isAdmin) "El estudiante aún no comienza la tarea." else "Siguiente paso: lee las instrucciones y presiona \"Iniciar tarea\"."
    TaskStatus.EN_DESARROLLO ->
        if (isAdmin) "El estudiante está realizando la tarea." else "Siguiente paso: al terminar, marca la tarea como realizada."
    TaskStatus.PENDIENTE_VALIDACION ->
        if (isAdmin) "Siguiente paso: revisa la entrega y valídala o recházala." else "El docente revisará tu trabajo."
    TaskStatus.VALIDADA -> "Proceso terminado: el trabajo fue validado."
    TaskStatus.RECHAZADA ->
        if (isAdmin) "El estudiante debe rehacer la tarea y volver a entregarla." else "Siguiente paso: lee el motivo, corrige y vuelve a entregar."
}

/** Divide las instrucciones en pasos: una línea por paso o, si es un solo párrafo, una oración por paso. */
internal fun instructionSteps(text: String): List<String> {
    val lines = text.lines().map { it.trim().trimStart('-', '•', '*').trim() }.filter { it.isNotEmpty() }
    if (lines.size > 1) return lines
    return text.split(Regex("(?<=\\.)\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
    }
}
