package com.duoc.huertify_app.domain

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.CropWithSector
import com.duoc.huertify_app.data.local.MaintenanceWithSector
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.domain.model.CropStatus
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class AlertLevel { CRITICAL, WARNING, INFO }

/** Avisos sobre tareas y problemas; recordatorios sobre el calendario del huerto. */
enum class AlertCategory { AVISO, RECORDATORIO }

data class Alert(
    val level: AlertLevel,
    val title: String,
    val message: String,
    val taskId: Long? = null,
    val category: AlertCategory = AlertCategory.AVISO,
    /** Sector al que lleva el aviso (solo navegable para el administrador). */
    val sectorId: Long? = null
)

/**
 * Calcula el próximo riego de un cultivo a partir de su frecuencia (HU-09 y HU-24).
 * Se toma como último riego la tarea de riego validada más reciente del cultivo (o de su sector,
 * si la tarea no indica cultivo). Si nunca se ha regado, se cuenta desde el trasplante o la siembra.
 */
object WateringSchedule {

    /** Estados en los que el cultivo está plantado y necesita riego. */
    private val NEEDS_WATER = setOf(
        CropStatus.SEMBRADO, CropStatus.EN_CRECIMIENTO, CropStatus.LISTO_COSECHA, CropStatus.CON_PROBLEMAS
    )

    fun needsWatering(crop: Crop): Boolean = crop.status in NEEDS_WATER

    fun lastWatering(crop: Crop, tasks: List<Task>): LocalDate? =
        tasks.asSequence()
            .filter { it.type == TaskType.RIEGO && it.status == TaskStatus.VALIDADA }
            .filter { it.cropId == crop.id || (it.cropId == null && it.sectorId == crop.sectorId) }
            .map { (it.completedAt ?: it.reviewedAt ?: it.updatedAt).toLocalDate() }
            .maxOrNull()

    fun nextWatering(crop: Crop, lastWatering: LocalDate?): LocalDate {
        val base = lastWatering ?: crop.transplant ?: crop.planting
        return base.plusDays(crop.wateringFrequencyDays.toLong())
    }

    fun nextWatering(crop: Crop, tasks: List<Task>): LocalDate = nextWatering(crop, lastWatering(crop, tasks))

    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

/** Genera los avisos y recordatorios que ve cada rol a partir del estado actual del huerto. */
object AlertBuilder {

    private val SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM")
    private const val HARVEST_WINDOW_DAYS = 7L
    private const val MAINTENANCE_WINDOW_DAYS = 7L
    private const val WATERING_WINDOW_DAYS = 2L

    fun build(
        role: Role,
        tasks: List<TaskWithNames>,
        reports: List<ReportWithNames>,
        crops: List<CropWithSector> = emptyList(),
        allTasks: List<Task> = tasks.map { it.task },
        maintenances: List<MaintenanceWithSector> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): List<Alert> {
        val alerts = mutableListOf<Alert>()
        alerts += taskAlerts(role, tasks, today)
        if (role == Role.ADMIN) {
            reports.filter { it.report.status.isOpen }.forEach {
                alerts += Alert(
                    AlertLevel.WARNING,
                    "Problema por revisar (${it.report.status.label})",
                    "${it.report.type.label} — ${it.sectorName ?: "Sin sector"}: ${it.report.description}"
                )
            }
        }
        alerts += wateringReminders(crops, allTasks, today)
        if (role == Role.ADMIN) {
            alerts += harvestReminders(crops, today)
            alerts += maintenanceReminders(maintenances, today)
        }
        return alerts.sortedWith(compareBy({ it.category.ordinal }, { it.level.ordinal }))
    }

    private fun taskAlerts(role: Role, tasks: List<TaskWithNames>, today: LocalDate): List<Alert> = tasks.mapNotNull { item ->
        val task = item.task
        val days = ChronoUnit.DAYS.between(today, task.scheduled)
        when {
            task.isOverdue(today) -> Alert(
                AlertLevel.CRITICAL,
                "Tarea atrasada",
                if (role == Role.ADMIN) "\"${task.title}\" de ${item.assigneeName} venció hace ${-days} día(s)"
                else "\"${task.title}\" venció hace ${-days} día(s)",
                task.id
            )
            task.status == TaskStatus.RECHAZADA && role == Role.STUDENT -> Alert(
                AlertLevel.WARNING,
                "Tarea rechazada",
                "\"${task.title}\": ${task.rejectionReason.orEmpty()}",
                task.id
            )
            task.status == TaskStatus.PENDIENTE_VALIDACION && role == Role.ADMIN -> Alert(
                AlertLevel.WARNING,
                "Pendiente de validación",
                "${item.assigneeName} entregó \"${task.title}\"",
                task.id
            )
            task.status.isOpen && days in 0..1 && role == Role.STUDENT -> Alert(
                AlertLevel.INFO,
                if (days == 0L) "Tarea para hoy" else "Tarea para mañana",
                "\"${task.title}\" (${task.status.label})",
                task.id
            )
            task.status == TaskStatus.PENDIENTE && role == Role.STUDENT -> Alert(
                AlertLevel.INFO,
                "Tarea pendiente",
                "\"${task.title}\" aún no ha sido iniciada",
                task.id
            )
            else -> null
        }
    }

    private fun wateringReminders(crops: List<CropWithSector>, allTasks: List<Task>, today: LocalDate): List<Alert> =
        crops.filter { WateringSchedule.needsWatering(it.crop) }.mapNotNull { item ->
            val next = WateringSchedule.nextWatering(item.crop, allTasks)
            val days = ChronoUnit.DAYS.between(today, next)
            val name = "${item.crop.name} (${item.sectorName})"
            when {
                days < 0 -> Alert(
                    AlertLevel.WARNING, "Riego atrasado",
                    "$name debía regarse el ${next.format(SHORT_DATE)} (hace ${-days} día(s))",
                    category = AlertCategory.RECORDATORIO, sectorId = item.crop.sectorId
                )
                days <= WATERING_WINDOW_DAYS -> Alert(
                    AlertLevel.INFO, "Próximo riego",
                    "$name: ${relativeDay(days)} (${next.format(SHORT_DATE)}), cada ${item.crop.wateringFrequencyDays} día(s)",
                    category = AlertCategory.RECORDATORIO, sectorId = item.crop.sectorId
                )
                else -> null
            }
        }

    private fun harvestReminders(crops: List<CropWithSector>, today: LocalDate): List<Alert> =
        crops.filter { it.crop.status.isActive && it.crop.status != CropStatus.PLANIFICADO }.mapNotNull { item ->
            val days = ChronoUnit.DAYS.between(today, item.crop.harvest)
            val name = "${item.crop.name} (${item.sectorName})"
            when {
                days < 0 -> Alert(
                    AlertLevel.WARNING, "Cosecha pendiente",
                    "$name tenía cosecha estimada el ${item.crop.harvest.format(SHORT_DATE)}",
                    category = AlertCategory.RECORDATORIO, sectorId = item.crop.sectorId
                )
                days <= HARVEST_WINDOW_DAYS -> Alert(
                    AlertLevel.INFO, "Próxima cosecha",
                    "$name: ${relativeDay(days)} (${item.crop.harvest.format(SHORT_DATE)})",
                    category = AlertCategory.RECORDATORIO, sectorId = item.crop.sectorId
                )
                else -> null
            }
        }

    private fun maintenanceReminders(list: List<MaintenanceWithSector>, today: LocalDate): List<Alert> =
        list.filter { !it.maintenance.done }.mapNotNull { item ->
            val m = item.maintenance
            val days = ChronoUnit.DAYS.between(today, m.scheduled)
            when {
                days < 0 -> Alert(
                    AlertLevel.WARNING, "Mantenimiento atrasado",
                    "${m.title} en ${item.sectorName} estaba programado para el ${m.scheduled.format(SHORT_DATE)}",
                    category = AlertCategory.RECORDATORIO, sectorId = m.sectorId
                )
                days <= MAINTENANCE_WINDOW_DAYS -> Alert(
                    AlertLevel.INFO, "Mantenimiento programado",
                    "${m.title} en ${item.sectorName}: ${relativeDay(days)} (${m.scheduled.format(SHORT_DATE)})",
                    category = AlertCategory.RECORDATORIO, sectorId = m.sectorId
                )
                else -> null
            }
        }

    private fun relativeDay(days: Long): String = when (days) {
        0L -> "hoy"
        1L -> "mañana"
        else -> "en $days días"
    }
}
