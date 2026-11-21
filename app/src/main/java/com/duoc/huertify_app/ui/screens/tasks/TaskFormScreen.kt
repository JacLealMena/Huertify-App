package com.duoc.huertify_app.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.domain.model.TaskType
import com.duoc.huertify_app.ui.common.CollectEvents
import com.duoc.huertify_app.ui.common.DateField
import com.duoc.huertify_app.ui.common.DropdownField
import com.duoc.huertify_app.ui.common.HuertifyTopBar
import com.duoc.huertify_app.ui.common.LoadingButton
import com.duoc.huertify_app.ui.common.ValidatedTextField
import com.duoc.huertify_app.ui.common.huertifyViewModel

@Composable
fun TaskFormScreen(onBack: () -> Unit) {
    val vm = huertifyViewModel { c, h -> TaskFormViewModel(c.taskRepository, c.authRepository, c.gardenRepository, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEvents(vm.events, snackbar, onBack)
    val noSector: Sector? = null

    Scaffold(
        topBar = { HuertifyTopBar(if (state.isEdit) "Editar tarea" else "Nueva tarea", onBack) },
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
            ValidatedTextField(state.title, vm::onTitle, "Título *", state.titleError, leadingIcon = Icons.Filled.Title)
            DropdownField(
                label = "Tipo de tarea *",
                options = TaskType.entries,
                selected = state.type,
                optionLabel = { it.label },
                onSelected = vm::onType,
                error = state.typeError,
                leadingIcon = Icons.Filled.Category
            )
            DropdownField(
                label = "Responsable *",
                options = state.students,
                selected = state.students.firstOrNull { it.id == state.assigneeId },
                optionLabel = { it.name },
                onSelected = vm::onAssignee,
                error = state.assigneeError,
                leadingIcon = Icons.Filled.Person
            )
            DropdownField(
                label = "Sector",
                options = listOf(noSector) + state.sectors,
                selected = state.sectors.firstOrNull { it.id == state.sectorId },
                optionLabel = { it?.name ?: "Sin sector específico" },
                onSelected = vm::onSector,
                error = null,
                leadingIcon = Icons.Filled.Yard
            )
            DateField("Fecha programada *", state.date, vm::onDate, state.dateError)
            ValidatedTextField(
                state.description, vm::onDescription, "Descripción *", state.descriptionError,
                leadingIcon = Icons.Filled.Description, singleLine = false, minLines = 2
            )
            ValidatedTextField(
                state.instructions, vm::onInstructions, "Instrucciones para el estudiante", state.instructionsError,
                leadingIcon = Icons.Filled.Info, singleLine = false, minLines = 3
            )
            LoadingButton(
                if (state.isEdit) "Guardar cambios" else "Crear y asignar",
                state.saving, vm::save, Modifier.padding(top = 12.dp),
                if (state.isEdit) Icons.Filled.Save else Icons.AutoMirrored.Filled.Assignment
            )
        }
    }
}
