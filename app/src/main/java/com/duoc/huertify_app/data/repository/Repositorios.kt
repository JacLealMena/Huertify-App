package com.duoc.huertify_app.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.duoc.huertify_app.data.local.ActivityDao
import com.duoc.huertify_app.data.local.ActivityLog
import com.duoc.huertify_app.data.local.ActivityWithUser
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.CropDao
import com.duoc.huertify_app.data.local.CropWithSector
import com.duoc.huertify_app.data.local.Maintenance
import com.duoc.huertify_app.data.local.MaintenanceDao
import com.duoc.huertify_app.data.local.MaintenanceWithSector
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.ReportDao
import com.duoc.huertify_app.data.local.ReportWithNames
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.SectorDao
import com.duoc.huertify_app.data.local.SectorWithInfo
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.TaskDao
import com.duoc.huertify_app.data.local.TaskWithNames
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.local.UserDao
import com.duoc.huertify_app.data.remote.HuertifyApi
import com.duoc.huertify_app.data.remote.LoginRequest
import com.duoc.huertify_app.domain.TaskStateMachine
import com.duoc.huertify_app.domain.model.ActivityKind
import com.duoc.huertify_app.domain.model.ReportStatus
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.security.MessageDigest
import java.time.LocalDate

/** Error de negocio con un mensaje apto para mostrarse al usuario. */
class BusinessException(message: String) : Exception(message)

fun hashPassword(password: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(password.toByteArray())
        .joinToString("") { "%02x".format(it) }

private fun requireAdmin(actor: User) {
    if (actor.role != Role.ADMIN) throw BusinessException("Acción permitida solo para administradores")
}

private fun now() = System.currentTimeMillis()

class AuthRepository(
    private val userDao: UserDao,
    private val prefs: SharedPreferences,
    private val activity: ActivityRepository,
    private val api: HuertifyApi
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun restoreSession() {
        val id = prefs.getLong(KEY_USER_ID, -1L)
        _currentUser.value = if (id > 0) userDao.findById(id) else null
    }

    suspend fun login(email: String, password: String): User {
        val response = try {
            api.login(LoginRequest(email.trim(), password))
        } catch (e: HttpException) {
            if (e.code() == 401) throw BusinessException("Correo o contraseña incorrectos")
            throw e
        } catch (_: IOException) {
            throw BusinessException("No se pudo conectar con el backend. Verifica que esté iniciado.")
        }
        val cached = userDao.findById(response.id)
        val user = User(
            id = response.id,
            name = response.name,
            email = response.email,
            passwordHash = cached?.passwordHash ?: "",
            role = response.role,
            workshop = response.workshop
        )
        userDao.upsert(user)
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

    /** Todos los usuarios ficticios, para elegir el responsable de un sector. */
    fun observeUsers(): Flow<List<User>> = userDao.observeAll()

    /** Talleres o grupos existentes (a partir de los estudiantes). */
    fun observeWorkshops(): Flow<List<String>> =
        observeStudents().map { list -> list.mapNotNull { it.workshop }.distinct().sorted() }

    private companion object {
        const val KEY_USER_ID = "session_user_id"
    }
}

class GardenRepository(
    private val sectorDao: SectorDao,
    private val cropDao: CropDao,
    private val activity: ActivityRepository,
    private val api: HuertifyApi
) {
    fun observeSectors(): Flow<List<Sector>> = sectorDao.observeAll()
    fun observeSectorsWithInfo(): Flow<List<SectorWithInfo>> = sectorDao.observeAllWithInfo()
    fun observeSector(id: Long): Flow<SectorWithInfo?> = sectorDao.observeById(id)
    fun observeCrops(sectorId: Long): Flow<List<Crop>> = cropDao.observeBySector(sectorId)
    fun observeAllCrops(): Flow<List<CropWithSector>> = cropDao.observeAllWithSector()
    fun observeCropCount(): Flow<Int> = cropDao.observeCount()

    suspend fun findSector(id: Long): Sector? = sectorDao.findById(id)
    suspend fun findCrop(id: Long): Crop? = cropDao.findById(id)

    suspend fun isSectorNameTaken(name: String, excludeId: Long): Boolean =
        sectorDao.countByName(name.trim(), excludeId) > 0

    suspend fun isSectorCodeTaken(code: String, excludeId: Long): Boolean =
        sectorDao.countByCode(code.trim(), excludeId) > 0

    suspend fun saveSector(sector: Sector, actor: User) {
        requireAdmin(actor)
        if (isSectorNameTaken(sector.name, sector.id)) {
            throw BusinessException("Ya existe un sector llamado \"${sector.name}\"")
        }
        if (isSectorCodeTaken(sector.code, sector.id)) {
            throw BusinessException("Ya existe un sector con el código \"${sector.code}\"")
        }
        if (sector.id == 0L) {
            val saved = api.createSector(sector.copy(createdAt = now(), updatedAt = now()))
            sectorDao.upsertAll(listOf(saved))
            activity.log(actor.id, "Creó el sector \"${saved.name}\" (${saved.status.label})", ActivityKind.SECTOR, sectorId = saved.id)
        } else {
            val previous = sectorDao.findById(sector.id) ?: throw BusinessException("El sector no existe")
            val saved = api.updateSector(
                sector.id,
                sector.copy(createdAt = previous.createdAt, updatedAt = now())
            )
            sectorDao.upsertAll(listOf(saved))
            val change = if (previous.status != saved.status) {
                ": estado ${previous.status.label} → ${saved.status.label}"
            } else ""
            activity.log(actor.id, "Editó el sector \"${saved.name}\"$change", ActivityKind.SECTOR, sectorId = saved.id)
        }
    }

    suspend fun deleteSector(sector: Sector, actor: User) {
        requireAdmin(actor)
        api.deleteSector(sector.id)
        sectorDao.delete(sector)
        activity.log(actor.id, "Eliminó el sector \"${sector.name}\"", ActivityKind.SECTOR)
    }

    suspend fun saveCrop(crop: Crop, actor: User) {
        requireAdmin(actor)
        if (crop.id == 0L) {
            val saved = api.createCrop(crop)
            cropDao.upsertAll(listOf(saved))
            activity.log(
                actor.id, "Registró el cultivo \"${saved.name}\" (${saved.status.label})", ActivityKind.CULTIVO,
                sectorId = saved.sectorId, cropId = saved.id, photoUri = saved.photoUri
            )
        } else {
            val previous = cropDao.findById(crop.id) ?: throw BusinessException("El cultivo no existe")
            val saved = api.updateCrop(crop.id, crop)
            cropDao.upsertAll(listOf(saved))
            // El cambio de estado de desarrollo queda en el historial (HU-10).
            val message = if (previous.status != saved.status) {
                "Cambió el estado de \"${saved.name}\": ${previous.status.label} → ${saved.status.label}"
            } else {
                "Editó el cultivo \"${saved.name}\""
            }
            activity.log(actor.id, message, ActivityKind.CULTIVO, sectorId = saved.sectorId, cropId = saved.id)
        }
    }

    suspend fun deleteCrop(crop: Crop, actor: User) {
        requireAdmin(actor)
        api.deleteCrop(crop.id)
        cropDao.delete(crop)
        activity.log(actor.id, "Eliminó el cultivo \"${crop.name}\"", ActivityKind.CULTIVO, sectorId = crop.sectorId)
    }
}

class MaintenanceRepository(
    private val maintenanceDao: MaintenanceDao,
    private val activity: ActivityRepository
) {
    fun observeAll(): Flow<List<MaintenanceWithSector>> = maintenanceDao.observeAll()

    suspend fun schedule(maintenance: Maintenance, actor: User) {
        requireAdmin(actor)
        maintenanceDao.insert(maintenance)
        activity.log(
            actor.id, "Programó el mantenimiento \"${maintenance.title}\" para el ${maintenance.scheduled}",
            ActivityKind.MANTENIMIENTO, sectorId = maintenance.sectorId
        )
    }

    suspend fun setDone(maintenance: Maintenance, done: Boolean, actor: User) {
        requireAdmin(actor)
        maintenanceDao.update(maintenance.copy(done = done))
        val verb = if (done) "Completó" else "Reabrió"
        activity.log(actor.id, "$verb el mantenimiento \"${maintenance.title}\"", ActivityKind.MANTENIMIENTO, sectorId = maintenance.sectorId)
    }

    suspend fun delete(maintenance: Maintenance, actor: User) {
        requireAdmin(actor)
        maintenanceDao.delete(maintenance)
        activity.log(actor.id, "Eliminó el mantenimiento \"${maintenance.title}\"", ActivityKind.MANTENIMIENTO, sectorId = maintenance.sectorId)
    }
}

class TaskRepository(
    private val taskDao: TaskDao,
    private val userDao: UserDao,
    private val activity: ActivityRepository,
    private val api: HuertifyApi
) {
    fun observeAll(): Flow<List<TaskWithNames>> = taskDao.observeAll()
    fun observeForUser(userId: Long): Flow<List<TaskWithNames>> = taskDao.observeByAssignee(userId)
    fun observeTask(id: Long): Flow<TaskWithNames?> = taskDao.observeById(id)
    suspend fun findTask(id: Long): Task? = taskDao.findById(id)

    suspend fun saveTask(task: Task, actor: User) {
        requireAdmin(actor)
        if (task.id == 0L) {
            // Se registra el docente que creó la tarea (HU-12).
            val saved = api.createTask(task.copy(creatorId = actor.id, status = TaskStatus.PENDIENTE))
            taskDao.upsertAll(listOf(saved))
            log(actor, "Creó y asignó la tarea \"${saved.title}\"", ActivityKind.TAREA, saved)
        } else {
            val current = taskDao.findById(task.id) ?: throw BusinessException("La tarea no existe")
            if (!TaskStateMachine.isEditable(current.status)) {
                throw BusinessException("Solo se pueden editar tareas en estado Pendiente")
            }
            val saved = api.updateTask(task.id, task.copy(updatedAt = now()))
            taskDao.upsertAll(listOf(saved))
            log(actor, "Editó la tarea \"${saved.title}\"", ActivityKind.TAREA, saved)
        }
    }

    /**
     * Asigna la tarea a todo un taller o grupo: cada integrante recibe su propia copia
     * (con el nombre del grupo), así cada estudiante la entrega y se valida por separado.
     * Devuelve cuántos estudiantes la recibieron.
     */
    suspend fun assignToGroup(task: Task, groupName: String, actor: User): Int {
        requireAdmin(actor)
        val members = userDao.findByWorkshop(groupName, Role.STUDENT)
        if (members.isEmpty()) throw BusinessException("El grupo \"$groupName\" no tiene estudiantes")
        members.forEach { member ->
            val saved = api.createTask(
                task.copy(id = 0, assigneeId = member.id, groupName = groupName, creatorId = actor.id, status = TaskStatus.PENDIENTE)
            )
            taskDao.upsertAll(listOf(saved))
        }
        log(actor, "Creó la tarea \"${task.title}\" y la asignó al grupo $groupName (${members.size} estudiantes)", ActivityKind.TAREA, task)
        return members.size
    }

    suspend fun deleteTask(task: Task, actor: User) {
        requireAdmin(actor)
        api.deleteTask(task.id)
        taskDao.delete(task)
        activity.log(actor.id, "Eliminó la tarea \"${task.title}\"", ActivityKind.TAREA, sectorId = task.sectorId, cropId = task.cropId)
    }

    suspend fun startTask(taskId: Long, actor: User) {
        val task = transition(taskId, TaskStatus.EN_DESARROLLO, actor)
        val updated = api.updateTask(taskId, task.copy(status = TaskStatus.EN_DESARROLLO, updatedAt = now()))
        taskDao.upsertAll(listOf(updated))
        log(actor, "Inició la tarea \"${updated.title}\"", ActivityKind.TAREA, updated)
    }

    /** El estudiante marca la tarea como realizada; foto y comentario son opcionales (HU-16, HU-17). */
    suspend fun submitTask(taskId: Long, comment: String?, evidenceUri: String?, actor: User) {
        val task = transition(taskId, TaskStatus.PENDIENTE_VALIDACION, actor)
        val updated = task.copy(
            status = TaskStatus.PENDIENTE_VALIDACION,
            studentComment = comment?.takeIf { it.isNotBlank() },
            evidenceUri = evidenceUri,
            completedAt = now(),
            updatedAt = now()
        )
        val saved = api.updateTask(taskId, updated)
        taskDao.upsertAll(listOf(saved))
        log(actor, "Marcó como realizada la tarea \"${saved.title}\" (pendiente de validación)", ActivityKind.ENTREGA, saved, evidenceUri)
    }

    suspend fun validateTask(taskId: Long, actor: User, comment: String? = null) {
        val task = transition(taskId, TaskStatus.VALIDADA, actor)
        val note = comment?.takeIf { it.isNotBlank() }
        val updated = task.copy(
            status = TaskStatus.VALIDADA, rejectionReason = null, reviewComment = note,
            reviewerId = actor.id, reviewedAt = now(), updatedAt = now()
        )
        val saved = api.updateTask(taskId, updated)
        taskDao.upsertAll(listOf(saved))
        val suffix = note?.let { ": $it" }.orEmpty()
        log(actor, "Validó la tarea \"${saved.title}\"$suffix", ActivityKind.VALIDACION, saved, saved.evidenceUri)
    }

    /** Rechaza la entrega con un motivo obligatorio y, opcionalmente, la reprograma (HU-20). */
    suspend fun rejectTask(taskId: Long, reason: String, actor: User, reschedule: LocalDate? = null) {
        val task = transition(taskId, TaskStatus.RECHAZADA, actor)
        val updated = task.copy(
            status = TaskStatus.RECHAZADA, rejectionReason = reason, reviewComment = null,
            reviewerId = actor.id, reviewedAt = now(),
            scheduledDate = reschedule?.toEpochDay() ?: task.scheduledDate,
            updatedAt = now()
        )
        val saved = api.updateTask(taskId, updated)
        taskDao.upsertAll(listOf(saved))
        val suffix = reschedule?.let { " y la reprogramó para el $it" }.orEmpty()
        log(actor, "Rechazó la tarea \"${saved.title}\"$suffix: $reason", ActivityKind.RECHAZO, saved, saved.evidenceUri)
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

    private suspend fun log(actor: User, message: String, kind: ActivityKind, task: Task, photoUri: String? = null) =
        activity.log(actor.id, message, kind, task.id.takeIf { it != 0L }, task.sectorId, task.cropId, photoUri)
}

class ReportRepository(
    private val reportDao: ReportDao,
    private val activity: ActivityRepository
) {
    fun observeAll(): Flow<List<ReportWithNames>> = reportDao.observeAll()
    fun observeForReporter(userId: Long): Flow<List<ReportWithNames>> = reportDao.observeByReporter(userId)

    /** Todo reporte nuevo queda en estado "Informado" (HU-22). */
    suspend fun createReport(report: ProblemReport, actor: User) {
        reportDao.insert(report.copy(reporterId = actor.id, status = ReportStatus.INFORMADO, createdAt = now()))
        activity.log(
            actor.id, "Reportó un problema: ${report.type.label} — ${report.description}", ActivityKind.PROBLEMA,
            sectorId = report.sectorId, cropId = report.cropId, photoUri = report.photoUri
        )
    }

    /** Seguimiento del docente: cambia el estado y deja un comentario visible al estudiante (HU-23). */
    suspend fun updateStatus(reportId: Long, status: ReportStatus, comment: String?, actor: User) {
        requireAdmin(actor)
        val report = reportDao.findById(reportId) ?: throw BusinessException("El reporte no existe")
        val closing = status == ReportStatus.ATENDIDO || status == ReportStatus.CERRADO
        reportDao.update(
            report.copy(
                status = status,
                adminComment = comment?.trim()?.takeIf { it.isNotEmpty() } ?: report.adminComment,
                attendedAt = if (closing) report.attendedAt ?: now() else null
            )
        )
        val suffix = comment?.trim()?.takeIf { it.isNotEmpty() }?.let { ": $it" }.orEmpty()
        activity.log(
            actor.id, "Cambió el reporte \"${report.type.label}\" a ${status.label}$suffix", ActivityKind.PROBLEMA,
            sectorId = report.sectorId, cropId = report.cropId
        )
    }
}

class ActivityRepository(private val activityDao: ActivityDao) {
    fun observeAll(): Flow<List<ActivityWithUser>> = activityDao.observeAll()
    fun observeForUser(userId: Long): Flow<List<ActivityWithUser>> = activityDao.observeForUser(userId)

    suspend fun log(
        userId: Long,
        message: String,
        kind: ActivityKind = ActivityKind.GENERAL,
        taskId: Long? = null,
        sectorId: Long? = null,
        cropId: Long? = null,
        photoUri: String? = null
    ) {
        activityDao.insert(
            ActivityLog(
                userId = userId, message = message, kind = kind, taskId = taskId,
                sectorId = sectorId, cropId = cropId, photoUri = photoUri
            )
        )
    }
}
