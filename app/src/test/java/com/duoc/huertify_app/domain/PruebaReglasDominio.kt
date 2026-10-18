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
