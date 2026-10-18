package com.duoc.huertify_app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.duoc.huertify_app.HuertifyApp
import com.duoc.huertify_app.data.repository.BusinessException
import com.duoc.huertify_app.di.AppContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Eventos de una sola vez que el ViewModel envía a la UI. */
sealed interface UiEvent {
    data class ShowMessage(val text: String, val isError: Boolean = false) : UiEvent
    /** Muestra una confirmación (Toast) y vuelve a la pantalla anterior. */
    data class SuccessAndBack(val text: String) : UiEvent
}

abstract class BaseViewModel : ViewModel() {
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    protected fun send(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    protected fun showError(text: String) = send(UiEvent.ShowMessage(text, isError = true))

    /** Ejecuta una operación y traduce cualquier excepción a un mensaje de error en pantalla. */
    protected fun launchSafe(onFinally: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: BusinessException) {
                showError(e.message ?: "Operación no permitida")
            } catch (e: Exception) {
                showError("Ocurrió un error inesperado: ${e.message ?: e::class.simpleName}")
            } finally {
                onFinally()
            }
        }
    }
}

/** Crea un ViewModel con acceso al contenedor de dependencias y a los argumentos de navegación. */
@Composable
inline fun <reified VM : ViewModel> huertifyViewModel(
    crossinline create: (AppContainer, SavedStateHandle) -> VM
): VM {
    val app = LocalContext.current.applicationContext as HuertifyApp
    return viewModel(factory = viewModelFactory {
        initializer { create(app.container, createSavedStateHandle()) }
    })
}

/** Consume los eventos del ViewModel: errores/avisos en Snackbar, éxitos con Toast + volver. */
@Composable
fun CollectEvents(
    events: Flow<UiEvent>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    val prefix = if (event.isError) "⚠ " else "✓ "
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(prefix + event.text, duration = SnackbarDuration.Short)
                }
                is UiEvent.SuccessAndBack -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
        }
    }
}
