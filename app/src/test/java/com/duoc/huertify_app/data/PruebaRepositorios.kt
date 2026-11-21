package com.duoc.huertify_app.data

import android.content.SharedPreferences
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
import com.duoc.huertify_app.data.remote.UserResponse
import com.duoc.huertify_app.data.repository.ActivityRepository
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.BusinessException
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.MaintenanceRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.data.repository.hashPassword
import com.duoc.huertify_app.domain.model.CropStatus
import com.duoc.huertify_app.domain.model.ProblemType
import com.duoc.huertify_app.domain.model.ReportStatus
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.SectorStatus
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class PruebaRepositorios {

    private val admin = User(1L, "Docente", "admin@huertify.cl", "hash", Role.ADMIN, null)
    private val studentA = User(2L, "Ana", "ana@huertify.cl", "hash", Role.STUDENT, "Taller A")
    private val studentB = User(3L, "Luis", "luis@huertify.cl", "hash", Role.STUDENT, "Taller B")

    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var fakeSectorDao: FakeSectorDao
    private lateinit var fakeCropDao: FakeCropDao
    private lateinit var fakeTaskDao: FakeTaskDao
    private lateinit var fakeReportDao: FakeReportDao
    private lateinit var fakeMaintenanceDao: FakeMaintenanceDao
    private lateinit var fakeActivityDao: FakeActivityDao
    private lateinit var fakeApi: FakeHuertifyApi
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var activityRepo: ActivityRepository

    @Before
    fun setup() {
        fakeUserDao = FakeUserDao().apply {
            items.addAll(listOf(admin, studentA, studentB))
        }
        fakeSectorDao = FakeSectorDao()
        fakeCropDao = FakeCropDao()
        fakeTaskDao = FakeTaskDao()
        fakeReportDao = FakeReportDao()
        fakeMaintenanceDao = FakeMaintenanceDao()
        fakeActivityDao = FakeActivityDao()
        fakeApi = FakeHuertifyApi()
        fakePrefs = FakeSharedPreferences()
        activityRepo = ActivityRepository(fakeActivityDao)
    }

    // --- 1. hashPassword & ActivityRepository ---
    @Test
    fun hashPasswordAndActivityRepository() = runBlocking {
        val hash = hashPassword("123456")
        assertEquals(64, hash.length)
        assertEquals("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92", hash)

        activityRepo.log(admin.id, "Evento general")
        activityRepo.log(studentA.id, "Evento estudiante")

        assertEquals(2, activityRepo.observeAll().first().size)
        assertEquals(1, activityRepo.observeForUser(studentA.id).first().size)
    }

    // --- 2. AuthRepository ---
    @Test
    fun authRepositoryLoginRestoreAndLogout() = runBlocking {
        val authRepo = AuthRepository(fakeUserDao, fakePrefs, activityRepo, fakeApi)
        fakeApi.nextLoginResponse = UserResponse(1L, "Docente", "admin@huertify.cl", Role.ADMIN, null)

        val logged = authRepo.login("admin@huertify.cl", "123456")
        assertEquals("Docente", logged.name)
        assertEquals(admin.id, authRepo.requireUser().id)
        assertEquals(listOf("Taller A", "Taller B"), authRepo.observeWorkshops().first())
        assertEquals(3, authRepo.observeUsers().first().size)

        // Cerrar sesión y restaurar
        authRepo.logout()
        assertNull(authRepo.currentUser.value)

        // Simular sesión guardada en prefs y restaurarla
        fakePrefs.data["session_user_id"] = 2L
        authRepo.restoreSession()
        assertEquals("Ana", authRepo.currentUser.value?.name)
    }

    @Test
    fun authRepositoryHandles401AndIoException() = runBlocking {
        val authRepo = AuthRepository(fakeUserDao, fakePrefs, activityRepo, fakeApi)

        fakeApi.loginError = HttpException(Response.error<Any>(401, "".toResponseBody()))
        try {
            authRepo.login("bad@huertify.cl", "wrong")
            throw AssertionError("Debió lanzar BusinessException por 401")
        } catch (e: BusinessException) {
            assertEquals("Correo o contraseña incorrectos", e.message)
        }

        fakeApi.loginError = IOException("Sin conexión")
        try {
            authRepo.login("bad@huertify.cl", "wrong")
            throw AssertionError("Debió lanzar BusinessException por red")
        } catch (e: BusinessException) {
            assertTrue(e.message!!.contains("No se pudo conectar"))
        }

        // requireUser sin sesión activa lanza BusinessException
        try {
            authRepo.requireUser()
            throw AssertionError("Debió lanzar Sesion expirada")
        } catch (e: BusinessException) {
            assertEquals("Sesión expirada", e.message)
        }
    }

    // --- 3. GardenRepository ---
    @Test
    fun gardenRepositoryCrudSectorsAndCrops() = runBlocking {
        val gardenRepo = GardenRepository(fakeSectorDao, fakeCropDao, activityRepo, fakeApi)

        val newSector = Sector(0L, "S-01", "Bancal Norte", "Zona A", 15.0, "Sol directo", SectorStatus.DISPONIBLE)
        gardenRepo.saveSector(newSector, admin)

        val savedSector = gardenRepo.observeSectors().first().first()
        assertEquals("Bancal Norte", savedSector.name)
        assertNotNull(gardenRepo.findSector(savedSector.id))
        assertEquals(1, gardenRepo.observeSectorsWithInfo().first().size)
        assertNotNull(gardenRepo.observeSector(savedSector.id).first())

        // Actualizar sector cambiando estado
        gardenRepo.saveSector(savedSector.copy(status = SectorStatus.EN_USO), admin)
        assertEquals(SectorStatus.EN_USO, gardenRepo.findSector(savedSector.id)?.status)

        // Crear y actualizar cultivo
        val newCrop = Crop(0L, savedSector.id, "Tomate", "Cherry", "", 100L, null, 160L, 3, CropStatus.SEMBRADO, null, "")
        gardenRepo.saveCrop(newCrop, admin)
        val savedCrop = gardenRepo.observeCrops(savedSector.id).first().first()
        assertEquals(1, gardenRepo.observeCropCount().first())
        assertEquals(1, gardenRepo.observeAllCrops().first().size)
        assertNotNull(gardenRepo.findCrop(savedCrop.id))

        gardenRepo.saveCrop(savedCrop.copy(status = CropStatus.EN_CRECIMIENTO), admin)
        assertEquals(CropStatus.EN_CRECIMIENTO, gardenRepo.findCrop(savedCrop.id)?.status)

        // Eliminar cultivo y sector
        gardenRepo.deleteCrop(savedCrop, admin)
        assertEquals(0, gardenRepo.observeCropCount().first())

        gardenRepo.deleteSector(savedSector, admin)
        assertTrue(gardenRepo.observeSectors().first().isEmpty())
    }

    @Test
    fun gardenRepositoryValidatesPermissionsAndDuplicates() = runBlocking {
        val gardenRepo = GardenRepository(fakeSectorDao, fakeCropDao, activityRepo, fakeApi)
        val s1 = Sector(0L, "S-01", "Norte", "Zona A", 10.0, "Desc")

        // Estudiante no autorizado
        try {
            gardenRepo.saveSector(s1, studentA)
            throw AssertionError("Debió rechazar estudiante")
        } catch (_: BusinessException) {}

        gardenRepo.saveSector(s1, admin)

        // Nombre duplicado
        try {
            gardenRepo.saveSector(s1.copy(id = 0L, code = "S-02"), admin)
            throw AssertionError("Debió rechazar nombre duplicado")
        } catch (_: BusinessException) {}

        // Código duplicado
        try {
            gardenRepo.saveSector(s1.copy(id = 0L, name = "Sur"), admin)
            throw AssertionError("Debió rechazar código duplicado")
        } catch (_: BusinessException) {}
    }

    // --- 4. MaintenanceRepository ---
    @Test
    fun maintenanceRepositoryScheduleCompleteAndDelete() = runBlocking {
        val maintRepo = MaintenanceRepository(fakeMaintenanceDao, activityRepo)
        val m = Maintenance(1L, 10L, "Revisar goteo", "Revisar válvulas", 200L, false)

        maintRepo.schedule(m, admin)
        assertEquals(1, maintRepo.observeAll().first().size)

        maintRepo.setDone(m, true, admin)
        assertTrue(maintRepo.observeAll().first().first().maintenance.done)

        maintRepo.setDone(m, false, admin)
        assertFalse(maintRepo.observeAll().first().first().maintenance.done)

        maintRepo.delete(m, admin)
        assertTrue(maintRepo.observeAll().first().isEmpty())
    }

    // --- 5. TaskRepository ---
    @Test
    fun taskRepositoryWorkflowAndGroupAssignment() = runBlocking {
        val taskRepo = TaskRepository(fakeTaskDao, fakeUserDao, activityRepo, fakeApi)

        val baseTask = Task(
            id = 0L, title = "Regar lechugas", description = "Riego matinal",
            instructions = "Usar regadera fina", type = TaskType.RIEGO,
            sectorId = 1L, cropId = null, assigneeId = studentA.id,
            scheduledDate = 200L
        )

        // Crear y editar mientras está PENDIENTE
        taskRepo.saveTask(baseTask, admin)
        val created = taskRepo.observeAll().first().first().task
        assertNotNull(taskRepo.findTask(created.id))
        assertNotNull(taskRepo.observeTask(created.id).first())
        assertEquals(1, taskRepo.observeForUser(studentA.id).first().size)

        taskRepo.saveTask(created.copy(title = "Regar lechugas urgente"), admin)
        assertEquals("Regar lechugas urgente", taskRepo.findTask(created.id)?.title)

        // Iniciar -> Entregar -> Rechazar -> Reabrir -> Validar
        taskRepo.startTask(created.id, studentA)
        assertEquals(TaskStatus.EN_DESARROLLO, taskRepo.findTask(created.id)?.status)

        // No se puede editar si ya no está PENDIENTE
        try {
            taskRepo.saveTask(created.copy(status = TaskStatus.EN_DESARROLLO), admin)
            throw AssertionError("No debió permitir editar tarea en desarrollo")
        } catch (_: BusinessException) {}

        taskRepo.submitTask(created.id, "Regado completo", "uri://foto", studentA)
        assertEquals(TaskStatus.PENDIENTE_VALIDACION, taskRepo.findTask(created.id)?.status)

        taskRepo.rejectTask(created.id, "Faltó el sector sur", admin, LocalDate.of(2026, 12, 1))
        assertEquals(TaskStatus.RECHAZADA, taskRepo.findTask(created.id)?.status)

        taskRepo.startTask(created.id, studentA)
        taskRepo.submitTask(created.id, "Ahora sí", null, studentA)
        taskRepo.validateTask(created.id, admin, "Excelente")
        assertEquals(TaskStatus.VALIDADA, taskRepo.findTask(created.id)?.status)

        // Eliminar tarea
        taskRepo.deleteTask(created, admin)
        assertNull(taskRepo.findTask(created.id))

        // Asignación por grupo
        val assignedCount = taskRepo.assignToGroup(baseTask, "Taller A", admin)
        assertEquals(1, assignedCount)

        try {
            taskRepo.assignToGroup(baseTask, "Grupo Inexistente", admin)
            throw AssertionError("Debió fallar con grupo vacío")
        } catch (_: BusinessException) {}
    }

    // --- 6. ReportRepository ---
    @Test
    fun reportRepositoryCreateAndUpdateStatus() = runBlocking {
        val reportRepo = ReportRepository(fakeReportDao, activityRepo)
        val report = ProblemReport(1L, studentA.id, 1L, null, ProblemType.PLAGA, "Pulgones en hojas", null)

        reportRepo.createReport(report, studentA)
        assertEquals(1, reportRepo.observeAll().first().size)
        assertEquals(1, reportRepo.observeForReporter(studentA.id).first().size)

        reportRepo.updateStatus(1L, ReportStatus.ATENDIDO, "Aplicado jabón potásico", admin)
        val updated = fakeReportDao.findById(1L)
        assertEquals(ReportStatus.ATENDIDO, updated?.status)
        assertEquals("Aplicado jabón potásico", updated?.adminComment)
        assertNotNull(updated?.attendedAt)
    }

    // =========================================================================
    // IMPLEMENTACIONES FALSAS EN MEMORIA (FAKES) PARA JUNIT PURO
    // =========================================================================

    private class FakeUserDao : UserDao {
        val items = mutableListOf<User>()
        private val flow = MutableStateFlow<List<User>>(emptyList())
        private fun emit() { flow.value = items.toList() }

        override suspend fun insert(user: User): Long { items.add(user); emit(); return user.id }
        override suspend fun upsert(user: User) {
            items.removeAll { it.id == user.id }
            items.add(user)
            emit()
        }
        override suspend fun count(): Int = items.size
        override suspend fun findByEmail(email: String): User? = items.find { it.email.equals(email, true) }
        override suspend fun findById(id: Long): User? = items.find { it.id == id }
        override fun observeByRole(role: Role): Flow<List<User>> =
            MutableStateFlow(items.filter { it.role == role })
        override fun observeAll(): Flow<List<User>> = MutableStateFlow(items.toList())
        override suspend fun findByWorkshop(workshop: String, role: Role): List<User> =
            items.filter { it.workshop == workshop && it.role == role }
    }

    private class FakeSectorDao : SectorDao {
        val items = mutableListOf<Sector>()
        private val flow = MutableStateFlow<List<Sector>>(emptyList())
        private fun emit() { flow.value = items.toList() }

        override suspend fun insert(sector: Sector): Long { items.add(sector); emit(); return sector.id }
        override suspend fun update(sector: Sector) { upsertAll(listOf(sector)) }
        override suspend fun upsertAll(sectors: List<Sector>) {
            sectors.forEach { s -> items.removeAll { it.id == s.id }; items.add(s) }
            emit()
        }
        override suspend fun deleteMissing(ids: List<Long>) { items.removeAll { it.id !in ids }; emit() }
        override suspend fun deleteAll() { items.clear(); emit() }
        override suspend fun delete(sector: Sector) { items.removeAll { it.id == sector.id }; emit() }
        override fun observeAll(): Flow<List<Sector>> = flow
        override fun observeAllWithInfo(): Flow<List<SectorWithInfo>> =
            flow.map { list -> list.map { SectorWithInfo(it, "Docente", 0) } }
        override fun observeById(id: Long): Flow<SectorWithInfo?> =
            flow.map { list -> list.find { it.id == id }?.let { SectorWithInfo(it, "Docente", 0) } }
        override suspend fun findById(id: Long): Sector? = items.find { it.id == id }
        override suspend fun countByName(name: String, excludeId: Long): Int =
            items.count { it.name.equals(name, true) && it.id != excludeId }
        override suspend fun countByCode(code: String, excludeId: Long): Int =
            items.count { it.code.equals(code, true) && it.id != excludeId }
    }

    private class FakeCropDao : CropDao {
        val items = mutableListOf<Crop>()
        private val flow = MutableStateFlow<List<Crop>>(emptyList())
        private fun emit() { flow.value = items.toList() }

        override suspend fun insert(crop: Crop): Long { items.add(crop); emit(); return crop.id }
        override suspend fun update(crop: Crop) { upsertAll(listOf(crop)) }
        override suspend fun upsertAll(crops: List<Crop>) {
            crops.forEach { c -> items.removeAll { it.id == c.id }; items.add(c) }
            emit()
        }
        override suspend fun deleteMissing(ids: List<Long>) { items.removeAll { it.id !in ids }; emit() }
        override suspend fun deleteAll() { items.clear(); emit() }
        override suspend fun delete(crop: Crop) { items.removeAll { it.id == crop.id }; emit() }
        override fun observeBySector(sectorId: Long): Flow<List<Crop>> =
            flow.map { list -> list.filter { it.sectorId == sectorId } }
        override fun observeAllWithSector(): Flow<List<CropWithSector>> =
            flow.map { list -> list.map { CropWithSector(it, "Sector") } }
        override suspend fun findById(id: Long): Crop? = items.find { it.id == id }
        override fun observeCount(): Flow<Int> = flow.map { it.size }
    }

    private class FakeTaskDao : TaskDao {
        val items = mutableListOf<Task>()
        private val flow = MutableStateFlow<List<Task>>(emptyList())
        private fun emit() { flow.value = items.toList() }

        override suspend fun insert(task: Task): Long { items.add(task); emit(); return task.id }
        override suspend fun update(task: Task) { upsertAll(listOf(task)) }
        override suspend fun upsertAll(tasks: List<Task>) {
            tasks.forEach { t -> items.removeAll { it.id == t.id }; items.add(t) }
            emit()
        }
        override suspend fun deleteMissing(ids: List<Long>) { items.removeAll { it.id !in ids }; emit() }
        override suspend fun deleteAll() { items.clear(); emit() }
        override suspend fun delete(task: Task) { items.removeAll { it.id == task.id }; emit() }
        override suspend fun findById(id: Long): Task? = items.find { it.id == id }
        override fun observeAll(): Flow<List<TaskWithNames>> =
            flow.map { list -> list.map { TaskWithNames(it, "Ana", "Sector") } }
        override fun observeByAssignee(userId: Long): Flow<List<TaskWithNames>> =
            flow.map { list -> list.filter { it.assigneeId == userId }.map { TaskWithNames(it, "Ana", "Sector") } }
        override fun observeById(id: Long): Flow<TaskWithNames?> =
            flow.map { list -> list.find { it.id == id }?.let { TaskWithNames(it, "Ana", "Sector") } }
    }

    private class FakeReportDao : ReportDao {
        val items = mutableListOf<ProblemReport>()
        private val flow = MutableStateFlow<List<ProblemReport>>(emptyList())
        private fun emit() { flow.value = items.toList() }

        override suspend fun insert(report: ProblemReport): Long { items.add(report); emit(); return report.id }
        override suspend fun update(report: ProblemReport) {
            items.removeAll { it.id == report.id }
            items.add(report)
            emit()
        }
        override suspend fun findById(id: Long): ProblemReport? = items.find { it.id == id }
        override fun observeAll(): Flow<List<ReportWithNames>> =
            flow.map { list -> list.map { ReportWithNames(it, "Ana", "Sector") } }
        override fun observeByReporter(userId: Long): Flow<List<ReportWithNames>> =
            flow.map { list -> list.filter { it.reporterId == userId }.map { ReportWithNames(it, "Ana", "Sector") } }
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val items = mutableListOf<Maintenance>()
        private val flow = MutableStateFlow<List<Maintenance>>(emptyList())
        private fun emit() { flow.value = items.toList() }

        override suspend fun insert(maintenance: Maintenance): Long { items.add(maintenance); emit(); return maintenance.id }
        override suspend fun update(maintenance: Maintenance) {
            items.removeAll { it.id == maintenance.id }
            items.add(maintenance)
            emit()
        }
        override suspend fun delete(maintenance: Maintenance) { items.removeAll { it.id == maintenance.id }; emit() }
        override fun observeAll(): Flow<List<MaintenanceWithSector>> =
            flow.map { list -> list.map { MaintenanceWithSector(it, "Sector") } }
    }

    private class FakeActivityDao : ActivityDao {
        val items = mutableListOf<ActivityLog>()
        override suspend fun insert(activity: ActivityLog): Long { items.add(activity); return activity.id }
        override fun observeAll(): Flow<List<ActivityWithUser>> =
            MutableStateFlow(items.map { ActivityWithUser(it, "Usuario") })
        override fun observeForUser(userId: Long): Flow<List<ActivityWithUser>> =
            MutableStateFlow(items.filter { it.userId == userId }.map { ActivityWithUser(it, "Usuario") })
    }

    private class FakeHuertifyApi : HuertifyApi {
        var nextLoginResponse = UserResponse(1L, "Admin", "admin@huertify.cl", Role.ADMIN, null)
        var loginError: Throwable? = null
        private var seq = 10L

        override suspend fun login(request: LoginRequest): UserResponse {
            loginError?.let { throw it }
            return nextLoginResponse
        }
        override suspend fun getUsers(): List<UserResponse> = emptyList()
        override suspend fun getSectors(): List<Sector> = emptyList()
        override suspend fun createSector(sector: Sector): Sector = sector.copy(id = ++seq)
        override suspend fun updateSector(id: Long, sector: Sector): Sector = sector
        override suspend fun deleteSector(id: Long) {}
        override suspend fun getCrops(sectorId: Long?): List<Crop> = emptyList()
        override suspend fun createCrop(crop: Crop): Crop = crop.copy(id = ++seq)
        override suspend fun updateCrop(id: Long, crop: Crop): Crop = crop
        override suspend fun deleteCrop(id: Long) {}
        override suspend fun getTasks(assigneeId: Long?): List<Task> = emptyList()
        override suspend fun createTask(task: Task): Task = task.copy(id = ++seq)
        override suspend fun updateTask(id: Long, task: Task): Task = task
        override suspend fun deleteTask(id: Long) {}
    }

    private class FakeSharedPreferences : SharedPreferences {
        val data = mutableMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?) = apply { if (key != null) data[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) data[key] = values }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) data[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) data[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) data[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) data[key] = value }
            override fun remove(key: String?) = apply { data.remove(key) }
            override fun clear() = apply { data.clear() }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }
}