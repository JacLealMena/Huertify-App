package com.duoc.huertify_app.domain

import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.validation.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidatorsTest {
    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun harvestBeforePlantingIsRejected() {
        val planting = today
        assertNotNull(Validators.harvestDate(today.minusDays(1), planting))
        assertNotNull(Validators.harvestDate(today, planting))
        assertNull(Validators.harvestDate(today.plusDays(30), planting))
        assertNotNull(Validators.harvestDate(null, planting))
    }

    @Test
    fun wateringFrequencyMustBePositiveInteger() {
        assertNotNull(Validators.wateringFrequency(""))
        assertNotNull(Validators.wateringFrequency("0"))
        assertNotNull(Validators.wateringFrequency("-2"))
        assertNotNull(Validators.wateringFrequency("1.5"))
        assertNotNull(Validators.wateringFrequency("abc"))
        assertNull(Validators.wateringFrequency("3"))
    }

    @Test
    fun emailFormat() {
        assertNotNull(Validators.email(""))
        assertNotNull(Validators.email("docente@"))
        assertNull(Validators.email("docente@huertify.cl"))
    }

    @Test
    fun positiveDecimalAcceptsCommaSeparator() {
        assertNull(Validators.positiveDecimal("12,5", "Área"))
        assertNotNull(Validators.positiveDecimal("0", "Área"))
        assertNotNull(Validators.positiveDecimal("x", "Área"))
    }

    @Test
    fun scheduledDateCannotBeInPastForNewTasks() {
        assertNotNull(Validators.scheduledDate(today.minusDays(1), today, allowPast = false))
        assertNull(Validators.scheduledDate(today.minusDays(1), today, allowPast = true))
        assertNull(Validators.scheduledDate(today, today, allowPast = false))
        assertNotNull(Validators.scheduledDate(null, today, allowPast = false))
    }

    @Test
    fun rejectionReasonIsMandatory() {
        assertNotNull(Validators.rejectionReason(""))
        assertNotNull(Validators.rejectionReason("mal"))
        assertNull(Validators.rejectionReason("Falta la foto del sector regado"))
    }
}

class TaskStateMachineTest {
    @Test
    fun studentFollowsLifecycle() {
        assertTrue(TaskStateMachine.canTransition(TaskStatus.PENDIENTE, TaskStatus.EN_DESARROLLO, Role.STUDENT))
        assertTrue(TaskStateMachine.canTransition(TaskStatus.EN_DESARROLLO, TaskStatus.PENDIENTE_VALIDACION, Role.STUDENT))
        assertTrue(TaskStateMachine.canTransition(TaskStatus.RECHAZADA, TaskStatus.EN_DESARROLLO, Role.STUDENT))
    }

    @Test
    fun cannotSkipStates() {
        assertFalse(TaskStateMachine.canTransition(TaskStatus.PENDIENTE, TaskStatus.PENDIENTE_VALIDACION, Role.STUDENT))
        assertFalse(TaskStateMachine.canTransition(TaskStatus.PENDIENTE, TaskStatus.VALIDADA, Role.ADMIN))
        assertFalse(TaskStateMachine.canTransition(TaskStatus.EN_DESARROLLO, TaskStatus.VALIDADA, Role.ADMIN))
        assertTrue(TaskStateMachine.nextStatesFor(TaskStatus.VALIDADA, Role.ADMIN).isEmpty())
    }

    @Test
    fun onlyAdminValidatesOrRejects() {
        assertFalse(TaskStateMachine.canTransition(TaskStatus.PENDIENTE_VALIDACION, TaskStatus.VALIDADA, Role.STUDENT))
        assertEquals(
            setOf(TaskStatus.VALIDADA, TaskStatus.RECHAZADA),
            TaskStateMachine.nextStatesFor(TaskStatus.PENDIENTE_VALIDACION, Role.ADMIN).toSet()
        )
    }
}

class NewRulesTest {
    private val today = java.time.LocalDate.of(2026, 11, 11)

    private fun crop(status: com.duoc.huertify_app.domain.model.CropStatus = com.duoc.huertify_app.domain.model.CropStatus.EN_CRECIMIENTO) =
        com.duoc.huertify_app.data.local.Crop(
            id = 1, sectorId = 10, name = "Lechuga", variety = "", plantingDate = today.minusDays(20).toEpochDay(),
            transplantDate = today.minusDays(10).toEpochDay(), harvestDate = today.plusDays(5).toEpochDay(),
            wateringFrequencyDays = 2, status = status, notes = ""
        )

    @Test
    fun sectorCodeFormat() {
        assertNull(Validators.sectorCode("S-01"))
        assertNotNull(Validators.sectorCode(""))
        assertNotNull(Validators.sectorCode("S 01"))
        assertNotNull(Validators.sectorCode("ABCDEFGHIJK"))
    }

    @Test
    fun transplantIsOptionalButMustBeBetweenPlantingAndHarvest() {
        val planting = today
        val harvest = today.plusDays(60)
        assertNull(Validators.transplantDate(null, planting, harvest))
        assertNull(Validators.transplantDate(today.plusDays(10), planting, harvest))
        assertNotNull(Validators.transplantDate(today.minusDays(1), planting, harvest))
        assertNotNull(Validators.transplantDate(today.plusDays(61), planting, harvest))
    }

    @Test
    fun nextWateringUsesTransplantWhenNeverWatered() {
        // Sin riegos registrados se cuenta desde el trasplante: hace 10 días + 2 = hace 8 días.
        assertEquals(today.minusDays(8), WateringSchedule.nextWatering(crop(), lastWatering = null))
    }

    @Test
    fun nextWateringUsesLastWatering() {
        assertEquals(today.plusDays(1), WateringSchedule.nextWatering(crop(), lastWatering = today.minusDays(1)))
    }

    @Test
    fun plannedOrHarvestedCropsDoNotNeedWatering() {
        assertFalse(WateringSchedule.needsWatering(crop(com.duoc.huertify_app.domain.model.CropStatus.PLANIFICADO)))
        assertFalse(WateringSchedule.needsWatering(crop(com.duoc.huertify_app.domain.model.CropStatus.COSECHADO)))
        assertTrue(WateringSchedule.needsWatering(crop()))
    }

    @Test
    fun remindersIncludeOverdueWateringAndUpcomingHarvest() {
        val cropWithSector = com.duoc.huertify_app.data.local.CropWithSector(crop(), "Bancal Norte")
        val alerts = AlertBuilder.build(Role.ADMIN, emptyList(), emptyList(), listOf(cropWithSector), emptyList(), emptyList(), today)
        assertTrue(alerts.any { it.title == "Riego atrasado" })
        assertTrue(alerts.any { it.title == "Próxima cosecha" })
        assertTrue(alerts.all { it.category == AlertCategory.RECORDATORIO })
    }

    @Test
    fun passwordRules() {
        assertNotNull(Validators.password(""))
        assertNotNull(Validators.password("   "))
        assertNotNull(Validators.password("12345"))
        assertNull(Validators.password("123456"))
    }

    @Test
    fun requiredTextRules() {
        assertNotNull(Validators.requiredText("", "El nombre"))
        assertNotNull(Validators.requiredText("ab", "El nombre", min = 3))
        assertNotNull(Validators.requiredText("abcdef", "El nombre", max = 5))
        assertNull(Validators.requiredText("Lechuga", "El nombre", min = 3, max = 20))
    }

    @Test
    fun optionalTextRules() {
        assertNull(Validators.optionalText("", "Notas", max = 10))
        assertNull(Validators.optionalText("Sin plagas", "Notas", max = 10))
        assertNotNull(Validators.optionalText("Texto demasiado largo", "Notas", max = 10))
    }

    @Test
    fun requiredGenericObject() {
        assertEquals("Selecciona un sector", Validators.required(null, "Selecciona un sector"))
        assertNull(Validators.required(10, "Selecciona un sector"))
    }

    @Test
    fun plantingDateRules() {
        assertNotNull(Validators.plantingDate(null, today))
        assertNotNull(Validators.plantingDate(today.plusYears(1).plusDays(1), today))
        assertNull(Validators.plantingDate(today.plusYears(1), today))
        assertNull(Validators.plantingDate(today, today))
    }

    @Test
    fun edgeCasesForExistingValidators() {
        // positiveDecimal: vacío, negativo y mayor a 100_000
        assertNotNull(Validators.positiveDecimal("", "Área"))
        assertNotNull(Validators.positiveDecimal("-5", "Área"))
        assertNotNull(Validators.positiveDecimal("100001", "Área"))
        assertNull(Validators.positiveDecimal("100000", "Área"))

        // wateringFrequency: mayor a 60 y límite 60
        assertNotNull(Validators.wateringFrequency("61"))
        assertNull(Validators.wateringFrequency("60"))

        // rejectionReason: mayor a 300 caracteres
        assertNotNull(Validators.rejectionReason("a".repeat(301)))
    }
}
