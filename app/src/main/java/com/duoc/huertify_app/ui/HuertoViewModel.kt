package com.duoc.huertify_app.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.HuertoRepository
import com.duoc.huertify_app.model.ReporteProblema
import com.duoc.huertify_app.model.Tarea
import com.duoc.huertify_app.model.Usuario
import com.duoc.huertify_app.model.Validacion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HuertoViewModel(private val repository: HuertoRepository, private val usuarioActual: Usuario) : ViewModel() {

    // Regla: Solo administrador cambia estado del problema
    fun actualizarEstadoProblema(reporte: ReporteProblema, nuevoEstado: String) {
        if (usuarioActual.rol == "Administrador" || usuarioActual.rol == "Docente") {
            val reporteActualizado = reporte.copy(estado = nuevoEstado)
            viewModelScope.launch {
                repository.actualizarReporte(reporteActualizado)
            }
        } else {
            // Manejar error: Permisos insuficientes
        }
    }

    // Regla: Estudiante marca realizada, Admin valida
    @RequiresApi(Build.VERSION_CODES.O)
    fun validarTarea(tarea: Tarea, esAprobada: Boolean, comentario: String) {
        // Impedir que el estudiante apruebe sus propias actividades
        if (usuarioActual.rol == "Estudiante") return

        val nuevoEstado = if (esAprobada) "Validada" else "Rechazada"

        viewModelScope.launch {
            // Actualizar estado de la tarea (Permitir rechazar y reenviar)
            repository.actualizarTarea(tarea.copy(estado = nuevoEstado))

            // Registrar quién validó y el resultado
            repository.insertarValidacion(
                Validacion(
                    tareaId = tarea.id,
                    administradorId = usuarioActual.id,
                    resultado = nuevoEstado,
                    fechaValidacion = LocalDate.now().toString()
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val tareasAtrasadas: StateFlow<List<Tarea>> = repository
        .obtenerTareasAtrasadas(LocalDate.now().toString())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}