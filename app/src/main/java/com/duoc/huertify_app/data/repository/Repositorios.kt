package com.duoc.huertify_app.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.duoc.huertify_app.data.local.ActivityDao
import com.duoc.huertify_app.data.local.ActivityLog
import com.duoc.huertify_app.data.local.ActivityWithUser
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.CropDao
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.ReportDao
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.SectorDao
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.TaskDao
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.local.UserDao
import com.duoc.huertify_app.domain.TaskStateMachine
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/** Error de negocio con un mensaje apto para mostrarse al usuario. */
class BusinessException(message: String) : Exception(message)

fun hashPassword(password: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(password.toByteArray())
        .joinToString("") { "%02x".format(it) }

class AuthRepository(
    private val userDao: UserDao,
    private val prefs: SharedPreferences,
    private val activity: ActivityRepository
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun restoreSession() {
        val id = prefs.getLong(KEY_USER_ID, -1L)
        _currentUser.value = if (id > 0) userDao.findById(id) else null
    }

    suspend fun login(email: String, password: String): User {
        val user = userDao.findByEmail(email.trim())
            ?: throw BusinessException("No existe una cuenta con ese correo")
        if (user.passwordHash != hashPassword(password)) {
            throw BusinessException("Contraseña incorrecta")
        }
        prefs.edit { putLong(KEY_USER_ID, user.id) }
        _currentUser.value = user
        activity.log(user.id, "Inició sesión")
        return user
    }

    suspend fun logout() {
        _currentUser.value?.let { activity.log(it.id, "Cerró sesión") }
        prefs.edit { remove(KEY_USER_ID) }
        _currentUser.value = null
    }

    fun requireUser(): User = _currentUser.value ?: throw BusinessException("Sesión expirada")

    fun observeStudents(): Flow<List<User>> = userDao.observeByRole(Role.STUDENT)

    private companion object {
        const val KEY_USER_ID = "session_user_id"
    }
}

class GardenRepository(
    private val sectorDao: SectorDao,
    private val cropDao: CropDao,
    private val activity: ActivityRepository
) {
    fun observeSectors(): Flow<List<Sector>> = sectorDao.observeAll()
    fun observeSector(id: Long): Flow<Sector?> = sectorDao.observeById(id)
    fun observeCrops(sectorId: Long): Flow<List<Crop>> = cropDao.observeBySector(sectorId)
    fun observeCropCount(): Flow<Int> = cropDao.observeCount()

    suspend fun findSector(id: Long): Sector? = sectorDao.findById(id)
    suspend fun findCrop(id: Long): Crop? = cropDao.findById(id)

    suspend fun isSectorNameTaken(name: String, excludeId: Long): Boolean =
        sectorDao.countByName(name.trim(), excludeId) > 0

    suspend fun saveSector(sector: Sector, actor: User) {
        if (isSectorNameTaken(sector.name, sector.id)) {
            throw BusinessException("Ya existe un sector llamado \"${sector.name}\"")
        }
        if (sector.id == 0L) {
            sectorDao.insert(sector)
            activity.log(actor.id, "Creó el sector \"${sector.name}\"")
        } else {
            sectorDao.update(sector)
            activity.log(actor.id, "Editó el sector \"${sector.name}\"")
        }
    }

    suspend fun deleteSector(sector: Sector, actor: User) {
        sectorDao.delete(sector)
        activity.log(actor.id, "Eliminó el sector \"${sector.name}\"")
    }

    suspend fun saveCrop(crop: Crop, actor: User) {
        if (crop.id == 0L) {
            cropDao.insert(crop)
            activity.log(actor.id, "Registró el cultivo \"${crop.name}\"")
        } else {
            cropDao.update(crop)
            activity.log(actor.id, "Editó el cultivo \"${crop.name}\"")
        }
    }

    suspend fun deleteCrop(crop: Crop, actor: User) {
        cropDao.delete(crop)
        activity.log(actor.id, "Eliminó el cultivo \"${crop.name}\"")
    }
}

class TaskRepository(
    private val taskDao: TaskDao,
    private val activity: ActivityRepository
) {
    fun observeAll(): Flow<List<TaskWithNames>> = taskDao.observeAll()
    fun observeForUser(userId: Long): Flow<List<TaskWithNames>> = taskDao.observeByAssignee(userId)
    fun observeTask(id: Long): Flow<TaskWithNames?> = taskDao.observeById(id)
    suspend fun findTask(id: Long): Task? = taskDao.findById(id)

    suspend fun saveTask(task: Task, actor: User) {
        requireAdmin(actor)
        if (task.id == 0L) {
            val id = taskDao.insert(task)
            activity.log(actor.id, "Creó y asignó la tarea \"${task.title}\"", id)
        } else {
            val current = taskDao.findById(task.id) ?: throw BusinessException("La tarea no existe")
            if (!TaskStateMachine.isEditable(current.status)) {
                throw BusinessException("Solo se pueden editar tareas en estado Pendiente")
            }
            taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))
            activity.log(actor.id, "Editó la tarea \"${task.title}\"", task.id)
        }
    }

    suspend fun deleteTask(task: Task, actor: User) {
        requireAdmin(actor)
        taskDao.delete(task)
        activity.log(actor.id, "Eliminó la tarea \"${task.title}\"")
    }

    suspend fun startTask(taskId: Long, actor: User) {
        val task = transition(taskId, TaskStatus.EN_DESARROLLO, actor)
        taskDao.update(task.copy(status = TaskStatus.EN_DESARROLLO, updatedAt = now()))
        activity.log(actor.id, "Inició la tarea \"${task.title}\"", taskId)
    }

    suspend fun submitTask(taskId: Long, comment: String, evidenceUri: String, actor: User) {
        val task = transition(taskId, TaskStatus.PENDIENTE_VALIDACION, actor)
        taskDao.update(
            task.copy(
                status = TaskStatus.PENDIENTE_VALIDACION,
                studentComment = comment,
                evidenceUri = evidenceUri,
                updatedAt = now()
            )
        )
        activity.log(actor.id, "Entregó la tarea \"${task.title}\" para validación", taskId)
    }

    suspend fun validateTask(taskId: Long, actor: User) {
        val task = transition(taskId, TaskStatus.VALIDADA, actor)
        taskDao.update(task.copy(status = TaskStatus.VALIDADA, rejectionReason = null, updatedAt = now()))
        activity.log(actor.id, "Validó la tarea \"${task.title}\"", taskId)
    }

    suspend fun rejectTask(taskId: Long, reason: String, actor: User) {
        val task = transition(taskId, TaskStatus.RECHAZADA, actor)
        taskDao.update(task.copy(status = TaskStatus.RECHAZADA, rejectionReason = reason, updatedAt = now()))
        activity.log(actor.id, "Rechazó la tarea \"${task.title}\": $reason", taskId)
    }

    /** Verifica permisos y transición; devuelve la tarea actual. */
    private suspend fun transition(taskId: Long, to: TaskStatus, actor: User): Task {
        val task = taskDao.findById(taskId) ?: throw BusinessException("La tarea no existe")
        if (actor.role == Role.STUDENT && task.assigneeId != actor.id) {
            throw BusinessException("Esta tarea no está asignada a ti")
        }
        if (!TaskStateMachine.canTransition(task.status, to, actor.role)) {
            throw BusinessException("No se puede pasar de \"${task.status.label}\" a \"${to.label}\"")
        }
        return task
    }

    private fun requireAdmin(actor: User) {
        if (actor.role != Role.ADMIN) throw BusinessException("Acción permitida solo para administradores")
    }

    private fun now() = System.currentTimeMillis()
}

class ReportRepository(
    private val reportDao: ReportDao,
    private val activity: ActivityRepository
) {
    fun observeAll(): Flow<List<ReportWithNames>> = reportDao.observeAll()

    suspend fun createReport(report: ProblemReport, actor: User) {
        reportDao.insert(report)
        activity.log(actor.id, "Reportó un problema: \"${report.title}\"")
    }

    suspend fun setResolved(report: ProblemReport, resolved: Boolean, actor: User) {
        if (actor.role != Role.ADMIN) throw BusinessException("Acción permitida solo para administradores")
        reportDao.setResolved(report.id, resolved)
        val verb = if (resolved) "Marcó como resuelto" else "Reabrió"
        activity.log(actor.id, "$verb el reporte \"${report.title}\"")
    }
}

class ActivityRepository(private val activityDao: ActivityDao) {
    fun observeAll(): Flow<List<ActivityWithUser>> = activityDao.observeAll()
    fun observeForUser(userId: Long): Flow<List<ActivityWithUser>> = activityDao.observeForUser(userId)

    suspend fun log(userId: Long, message: String, taskId: Long? = null) {
        activityDao.insert(ActivityLog(userId = userId, message = message, taskId = taskId))
    }
}
