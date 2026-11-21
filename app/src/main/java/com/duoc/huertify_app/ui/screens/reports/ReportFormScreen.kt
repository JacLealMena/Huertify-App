package com.duoc.huertify_app.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Yard
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
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.domain.model.ProblemType
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
    val noCrop: Crop? = null

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
            DropdownField(
                label = "Tipo de problema *",
                options = ProblemType.entries,
                selected = state.type,
                optionLabel = { it.label },
                onSelected = vm::onType,
                error = state.typeError,
                leadingIcon = Icons.Filled.Category
            )
            DropdownField(
                label = "Sector afectado",
                options = listOf(noSector) + state.sectors,
                selected = state.sector,
                optionLabel = { it?.name ?: "General / no aplica" },
                onSelected = vm::onSector,
                error = null,
                leadingIcon = Icons.Filled.Yard
            )
            if (state.sector != null && state.crops.isNotEmpty()) {
                DropdownField(
                    label = "Cultivo afectado",
                    options = listOf(noCrop) + state.crops,
                    selected = state.crop,
                    optionLabel = { it?.name ?: "Sin cultivo específico" },
                    onSelected = vm::onCrop,
                    error = null,
                    leadingIcon = Icons.Filled.Grass
                )
            }
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
