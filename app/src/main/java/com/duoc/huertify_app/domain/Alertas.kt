package com.duoc.huertify_app.domain

import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.Severity
import com.duoc.huertify_app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class AlertLevel { CRITICAL, WARNING, INFO }

data class Alert(
    val level: AlertLevel,
    val title: String,
    val message: String,
    val taskId: Long? = null
)

/** Genera los avisos que ve cada rol a partir del estado actual de tareas y reportes. */
object AlertBuilder {

    fun build(
        role: Role,
        tasks: List<TaskWithNames>,
        reports: List<ReportWithNames>,
        today: LocalDate = LocalDate.now()
    ): List<Alert> {
        val alerts = mutableListOf<Alert>()
        tasks.forEach { item ->
            val task = item.task
            val days = ChronoUnit.DAYS.between(today, task.scheduled)
            when {
                task.isOverdue(today) -> alerts += Alert(
                    AlertLevel.CRITICAL,
                    "Tarea atrasada",
                    if (role == Role.ADMIN) "\"${task.title}\" de ${item.assigneeName} venció hace ${-days} día(s)"
                    else "\"${task.title}\" venció hace ${-days} día(s)",
                    task.id
                )
                task.status == TaskStatus.RECHAZADA && role == Role.STUDENT -> alerts += Alert(
                    AlertLevel.WARNING,
                    "Tarea rechazada",
                    "\"${task.title}\": ${task.rejectionReason.orEmpty()}",
                    task.id
                )
                task.status == TaskStatus.PENDIENTE_VALIDACION && role == Role.ADMIN -> alerts += Alert(
                    AlertLevel.WARNING,
                    "Pendiente de validación",
                    "${item.assigneeName} entregó \"${task.title}\"",
                    task.id
                )
                task.status.isOpen && days in 0..1 && role == Role.STUDENT -> alerts += Alert(
                    AlertLevel.INFO,
                    if (days == 0L) "Tarea para hoy" else "Tarea para mañana",
                    "\"${task.title}\" (${task.status.label})",
                    task.id
                )
                task.status == TaskStatus.PENDIENTE && role == Role.STUDENT -> alerts += Alert(
                    AlertLevel.INFO,
                    "Tarea pendiente",
                    "\"${task.title}\" aún no ha sido iniciada",
                    task.id
                )
            }
        }
        if (role == Role.ADMIN) {
            reports.filter { !it.report.resolved }.forEach {
                alerts += Alert(
                    if (it.report.severity == Severity.ALTA) AlertLevel.CRITICAL else AlertLevel.WARNING,
                    "Problema reportado (${it.report.severity.label})",
                    "${it.report.title} — ${it.sectorName ?: "Sin sector"}"
                )
            }
        }
        return alerts.sortedBy { it.level.ordinal }
    }
}
