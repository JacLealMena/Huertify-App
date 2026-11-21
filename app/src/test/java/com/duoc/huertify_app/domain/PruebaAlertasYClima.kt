package com.duoc.huertify_app.domain

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.Maintenance
import com.duoc.huertify_app.data.local.MaintenanceWithSector
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.domain.model.CropStatus
import com.duoc.huertify_app.domain.model.ProblemType
import com.duoc.huertify_app.domain.model.ReportStatus
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PruebaAlertasYClima {

    private val today = LocalDate.of(2026, 11, 20)

    private fun epochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // --- WeatherRules ---
    @Test
    fun weatherRulesAdviceCoversAllConditions() {
        assertTrue(WeatherRules.wateringAdvice(null).contains("Sin pronóstico"))

        val lluvia = DailyForecast(today, 20.0, 10.0, 5.0, "Lluvia")
        assertTrue(WeatherRules.wateringAdvice(lluvia).contains("no sea necesario regar"))

        val helada = DailyForecast(today, 14.0, 1.0, 0.0, "Despejado")
        assertTrue(WeatherRules.wateringAdvice(helada).contains("Riesgo de helada"))

        val calor = DailyForecast(today, 32.0, 15.0, 0.0, "Despejado")
        assertTrue(WeatherRules.wateringAdvice(calor).contains("Día caluroso"))

        val templado = DailyForecast(today, 22.0, 11.0, 0.0, "Nublado")
        assertTrue(WeatherRules.wateringAdvice(templado).contains("Clima templado"))
    }

    @Test
    fun weatherRulesDescribeWmoCodes() {
        assertEquals("Despejado", WeatherRules.describe(0))
        assertEquals("Parcialmente nublado", WeatherRules.describe(2))
        assertEquals("Nublado", WeatherRules.describe(3))
        assertEquals("Niebla", WeatherRules.describe(45))
        assertEquals("Llovizna", WeatherRules.describe(51))
        assertEquals("Lluvia", WeatherRules.describe(63))
        assertEquals("Nieve", WeatherRules.describe(71))
        assertEquals("Tormenta", WeatherRules.describe(95))
        assertEquals("Sin datos", WeatherRules.describe(null))
        assertEquals("Sin datos", WeatherRules.describe(999))
    }

    // --- WateringSchedule ---
    @Test
    fun wateringScheduleFindsLastValidatedWateringTask() {
        val crop = Crop(
            id = 10, sectorId = 5, name = "Tomate", variety = "",
            plantingDate = today.minusDays(30).toEpochDay(),
            transplantDate = null,
            harvestDate = today.plusDays(30).toEpochDay(),
            wateringFrequencyDays = 3, status = CropStatus.SEMBRADO, notes = ""
        )
        val nonWatering = Task(
            id = 1, title = "Limpiar sector", description = "", instructions = "Barrer pasillos",
            type = TaskType.LIMPIEZA, sectorId = 5, cropId = 10, assigneeId = 2, creatorId = 1,
            scheduledDate = today.minusDays(1).toEpochDay(), status = TaskStatus.VALIDADA,
            completedAt = epochMillis(today.minusDays(1))
        )
        val validWatering = Task(
            id = 2, title = "Regar", description = "", instructions = "Regar 2 litros",
            type = TaskType.RIEGO, sectorId = 5, cropId = 10, assigneeId = 2, creatorId = 1,
            scheduledDate = today.minusDays(2).toEpochDay(), status = TaskStatus.VALIDADA,
            completedAt = epochMillis(today.minusDays(2))
        )

        assertNull(WateringSchedule.lastWatering(crop, listOf(nonWatering)))
        assertEquals(today.minusDays(2), WateringSchedule.lastWatering(crop, listOf(nonWatering, validWatering)))
        assertEquals(today.plusDays(1), WateringSchedule.nextWatering(crop, listOf(validWatering)))
    }

    // --- AlertBuilder ---
    @Test
    fun alertBuilderGeneratesAdminAndStudentAlerts() {
        val overdueTask = TaskWithNames(
            task = Task(
                id = 1, title = "Regar Sector A", description = "", instructions = "Regar temprano",
                type = TaskType.RIEGO, sectorId = 1, cropId = null, assigneeId = 2, creatorId = 1,
                scheduledDate = today.minusDays(2).toEpochDay(), status = TaskStatus.PENDIENTE
            ),
            assigneeName = "Ana", sectorName = "Sector A", cropName = null, creatorName = "Profe"
        )
        val rejectedTask = TaskWithNames(
            task = Task(
                id = 2, title = "Desmalezar", description = "", instructions = "Quitar maleza",
                type = TaskType.DESMALEZADO, sectorId = 1, cropId = null, assigneeId = 2, creatorId = 1,
                scheduledDate = today.plusDays(3).toEpochDay(), status = TaskStatus.RECHAZADA,
                rejectionReason = "Faltó limpiar borde"
            ),
            assigneeName = "Ana", sectorName = "Sector A", cropName = null, creatorName = "Profe"
        )
        val validationTask = TaskWithNames(
            task = Task(
                id = 3, title = "Cosechar", description = "", instructions = "Cortar frutos maduros",
                type = TaskType.COSECHA, sectorId = 1, cropId = null, assigneeId = 2, creatorId = 1,
                scheduledDate = today.plusDays(2).toEpochDay(), status = TaskStatus.PENDIENTE_VALIDACION
            ),
            assigneeName = "Ana", sectorName = "Sector A", cropName = null, creatorName = "Profe"
        )
        val todayTask = TaskWithNames(
            task = Task(
                id = 4, title = "Observar cultivo", description = "", instructions = "Revisar hojas",
                type = TaskType.OBSERVACION, sectorId = 1, cropId = null, assigneeId = 2, creatorId = 1,
                scheduledDate = today.toEpochDay(), status = TaskStatus.EN_DESARROLLO
            ),
            assigneeName = "Ana", sectorName = "Sector A", cropName = null, creatorName = "Profe"
        )
        val pendingTask = TaskWithNames(
            task = Task(
                id = 5, title = "Revisar suelo", description = "", instructions = "Medir humedad",
                type = TaskType.REVISION_SUELO, sectorId = 1, cropId = null, assigneeId = 2, creatorId = 1,
                scheduledDate = today.plusDays(5).toEpochDay(), status = TaskStatus.PENDIENTE
            ),
            assigneeName = "Ana", sectorName = "Sector A", cropName = null, creatorName = "Profe"
        )

        val openReport = ReportWithNames(
            report = ProblemReport(
                id = 1, reporterId = 2, sectorId = 1, cropId = null,
                type = ProblemType.PLAGA, description = "Pulgones", photoUri = null,
                status = ReportStatus.INFORMADO
            ),
            reporterName = "Ana", sectorName = "Sector A", cropName = null
        )

        val overdueMaintenance = MaintenanceWithSector(
            maintenance = Maintenance(
                id = 1, sectorId = 1, title = "Revisar manguera",
                scheduledDate = today.minusDays(1).toEpochDay(), done = false
            ),
            sectorName = "Sector A"
        )
        val upcomingMaintenance = MaintenanceWithSector(
            maintenance = Maintenance(
                id = 2, sectorId = 1, title = "Cambiar filtro",
                scheduledDate = today.plusDays(1).toEpochDay(), done = false
            ),
            sectorName = "Sector A"
        )

        val allTaskItems = listOf(overdueTask, rejectedTask, validationTask, todayTask, pendingTask)

        // Rol ADMIN
        val adminAlerts = AlertBuilder.build(
            role = Role.ADMIN,
            tasks = allTaskItems,
            reports = listOf(openReport),
            maintenances = listOf(overdueMaintenance, upcomingMaintenance),
            today = today
        )
        assertTrue(adminAlerts.any { it.title == "Tarea atrasada" && it.message.contains("Ana") })
        assertTrue(adminAlerts.any { it.title == "Pendiente de validación" })
        assertTrue(adminAlerts.any { it.title.startsWith("Problema por revisar") })
        assertTrue(adminAlerts.any { it.title == "Mantenimiento atrasado" })
        assertTrue(adminAlerts.any { it.title == "Mantenimiento programado" })

        // Rol STUDENT
        val studentAlerts = AlertBuilder.build(
            role = Role.STUDENT,
            tasks = allTaskItems,
            reports = listOf(openReport),
            maintenances = listOf(overdueMaintenance),
            today = today
        )
        assertTrue(studentAlerts.any { it.title == "Tarea rechazada" })
        assertTrue(studentAlerts.any { it.title == "Tarea para hoy" })
        assertTrue(studentAlerts.any { it.title == "Tarea pendiente" })
    }
}