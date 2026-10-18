package com.duoc.huertify_app.domain

import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskStatus.EN_DESARROLLO
import com.duoc.huertify_app.domain.model.TaskStatus.PENDIENTE
import com.duoc.huertify_app.domain.model.TaskStatus.PENDIENTE_VALIDACION
import com.duoc.huertify_app.domain.model.TaskStatus.RECHAZADA
import com.duoc.huertify_app.domain.model.TaskStatus.VALIDADA

/**
 * Ciclo de vida de una tarea:
 * Pendiente → En desarrollo → Pendiente de validación → Validada | Rechazada.
 * Una tarea rechazada puede volver a "En desarrollo" para que el estudiante la corrija.
 */
object TaskStateMachine {

    private val transitions: Map<TaskStatus, Map<TaskStatus, Role>> = mapOf(
        PENDIENTE to mapOf(EN_DESARROLLO to Role.STUDENT),
        EN_DESARROLLO to mapOf(PENDIENTE_VALIDACION to Role.STUDENT),
        PENDIENTE_VALIDACION to mapOf(VALIDADA to Role.ADMIN, RECHAZADA to Role.ADMIN),
        RECHAZADA to mapOf(EN_DESARROLLO to Role.STUDENT),
        VALIDADA to emptyMap()
    )

    fun canTransition(from: TaskStatus, to: TaskStatus, role: Role): Boolean =
        transitions[from]?.get(to) == role

    fun nextStatesFor(from: TaskStatus, role: Role): List<TaskStatus> =
        transitions[from].orEmpty().filterValues { it == role }.keys.toList()

    /** El administrador solo puede editar la definición de una tarea antes de que se inicie. */
    fun isEditable(status: TaskStatus): Boolean = status == PENDIENTE
}
