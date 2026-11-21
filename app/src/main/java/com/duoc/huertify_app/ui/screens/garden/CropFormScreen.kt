package com.duoc.huertify_app.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DateField
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.huertifyViewModel

@Composable
fun CropFormScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, h -> CropFormViewModel(c.gardenRepository, c.authRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar, onBack)

    Scaffold(
        topBar = { HuertifyTopBar(if (state.isEdit) "Editar cultivo" else "Nuevo cultivo", onBack) },
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
            ValidatedTextField(state.name, vm::onName, "Cultivo *", state.nameError, leadingIcon = Icons.Filled.Grass)
            ValidatedTextField(state.variety, vm::onVariety, "Variedad", state.varietyError, leadingIcon = Icons.Filled.Spa)
            DateField("Fecha de siembra *", state.plantingDate, vm::onPlanting, state.plantingError)
            DateField("Fecha estimada de cosecha *", state.harvestDate, vm::onHarvest, state.harvestError)
            ValidatedTextField(
                state.watering, vm::onWatering, "Frecuencia de riego (días) *", state.wateringError,
                leadingIcon = Icons.Filled.WaterDrop,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = "Cada cuántos días se debe regar"
            )
            ValidatedTextField(
                state.notes, vm::onNotes, "Notas", state.notesError,
                leadingIcon = Icons.AutoMirrored.Filled.Notes, singleLine = false, minLines = 2
            )
            LoadingButton("Guardar", state.saving, vm::save, Modifier.padding(top = 12.dp), Icons.Filled.Save)
        }
    }
}
