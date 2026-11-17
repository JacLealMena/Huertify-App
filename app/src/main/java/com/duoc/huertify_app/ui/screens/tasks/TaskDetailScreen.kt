package com.duoc.huertify_app.ui.screens.tasks

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.duoc.huertify_app.domain.TaskStateMachine
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.ConfirmDialog
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.PhotoPicker
import com.duoc.huertify_app.ui.common.SectionTitle
import com.duoc.huertify_app.ui.common.StatusChip
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.color
import com.duoc.huertify_app.ui.common.formatEpochDay
import com.duoc.huertify_app.ui.common.formatTimestamp
import com.duoc.huertify_app.ui.common.huertifyViewModel
import com.duoc.huertify_app.ui.navigation.Routes

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
