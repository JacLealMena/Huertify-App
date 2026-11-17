package com.duoc.huertify_app.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.domain.model.Severity
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.PhotoPicker
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.huertifyViewModel

@Composable
fun ReportFormScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, h -> ReportFormViewModel(c.reportRepository, c.authRepository, c.imageStorage, c.gardenRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar, onBack)
    val noSector: Sector? = null

    Scaffold(
        topBar = { HuertifyTopBar("Reportar problema", onBack) },
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
            ValidatedTextField(state.title, vm::onTitle, "Título del problema *", state.titleError, leadingIcon = Icons.Filled.Title)
            DropdownField(
                label = "Sector afectado",
                options = listOf(noSector) + state.sectors,
                selected = state.sector,
                optionLabel = { it?.name ?: "General / no aplica" },
                onSelected = vm::onSector,
                error = null,
                leadingIcon = Icons.Filled.Yard
            )
            Text("Gravedad *", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Severity.entries.forEach { sev ->
                    FilterChip(selected = state.severity == sev, onClick = { vm.onSeverity(sev) }, label = { Text(sev.label) })
                }
            }
            state.severityError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción *", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 3,
                supportingText = "Ej: plaga de pulgones en las lechugas, manguera rota…"
            )
            Text("Fotografía (opcional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            PhotoPicker(state.photoUri, null, vm::prepareCameraUri, vm::onCameraResult, vm::onGalleryPicked)
            LoadingButton("Enviar reporte", state.saving, vm::submit, Modifier.padding(top = 12.dp), Icons.AutoMirrored.Filled.Send)
        }
    }
}
