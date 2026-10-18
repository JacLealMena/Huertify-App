package com.duoc.huertify_app.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.HuertifyDatabase
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.ActivityRepository
import com.duoc.huertify_app.data.repository.BusinessException
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    private lateinit var db: HuertifyDatabase
    private lateinit var garden: GardenRepository
    private lateinit var tasks: TaskRepository
    private lateinit var admin: User
    private lateinit var ana: User
    private lateinit var benjamin: User

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, HuertifyDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        SeedData.seedIfEmpty(db)
        val activity = ActivityRepository(db.activityDao())
        garden = GardenRepository(db.sectorDao(), db.cropDao(), activity)
        tasks = TaskRepository(db.taskDao(), activity)
        admin = db.userDao().findByEmail("docente@huertify.cl")!!
        ana = db.userDao().findByEmail("ana@huertify.cl")!!
        benjamin = db.userDao().findByEmail("benjamin@huertify.cl")!!
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newTaskFor(user: User): Long {
        tasks.saveTask(
            Task(
                title = "Prueba", description = "Descripción de prueba", instructions = "",
                type = TaskType.RIEGO, sectorId = null, assigneeId = user.id,
                scheduledDate = LocalDate.now().toEpochDay()
            ),
            admin
        )
        return tasks.observeForUser(user.id).first().maxOf { it.task.id }
    }

    private suspend inline fun assertRejected(crossinline block: suspend () -> Unit) {
        try {
            block()
            fail("Se esperaba BusinessException")
        } catch (_: BusinessException) {
        }
    }

    @Test
    fun seedCreatesFourAccounts() = runBlocking {
        assertEquals(4, db.userDao().count())
        assertEquals(Role.ADMIN, admin.role)
        assertEquals(3, db.userDao().observeByRole(Role.STUDENT).first().size)
    }

    @Test
    fun fullTaskLifecycleWithRejectionAndRedo() = runBlocking {
        val id = newTaskFor(ana)
        tasks.startTask(id, ana)
        assertEquals(TaskStatus.EN_DESARROLLO, tasks.findTask(id)!!.status)

        tasks.submitTask(id, "Regué todo el sector", "file://foto.jpg", ana)
        assertEquals(TaskStatus.PENDIENTE_VALIDACION, tasks.findTask(id)!!.status)

        tasks.rejectTask(id, "Falta regar la última hilera", admin)
        val rejected = tasks.findTask(id)!!
        assertEquals(TaskStatus.RECHAZADA, rejected.status)
        assertEquals("Falta regar la última hilera", rejected.rejectionReason)

        tasks.startTask(id, ana)
        tasks.submitTask(id, "Ahora sí, todo regado", "file://foto2.jpg", ana)
        tasks.validateTask(id, admin)
        val validated = tasks.findTask(id)!!
        assertEquals(TaskStatus.VALIDADA, validated.status)
        assertNull(validated.rejectionReason)
    }

    @Test
    fun studentCannotTouchOthersTasksNorSkipStates() = runBlocking {
        val id = newTaskFor(ana)
        assertRejected { tasks.startTask(id, benjamin) }                       // tarea ajena
        assertRejected { tasks.submitTask(id, "comentario", "uri", ana) }       // salta "En desarrollo"
        tasks.startTask(id, ana)
        tasks.submitTask(id, "comentario largo", "uri", ana)
        assertRejected { tasks.validateTask(id, ana) }                          // estudiante no valida
        assertEquals(TaskStatus.PENDIENTE_VALIDACION, tasks.findTask(id)!!.status)
    }

    @Test
    fun onlyAdminCreatesTasksAndOnlyPendingTasksAreEditable() = runBlocking {
        assertRejected {
            tasks.saveTask(
                Task(title = "x", description = "x", instructions = "", type = TaskType.OTRO,
                    sectorId = null, assigneeId = ana.id, scheduledDate = 0),
                ana
            )
        }
        val id = newTaskFor(ana)
        tasks.startTask(id, ana)
        assertRejected { tasks.saveTask(tasks.findTask(id)!!.copy(title = "Editada"), admin) }
    }

    @Test
    fun sectorNamesAreUniqueAndDeletingSectorCascadesCrops() = runBlocking {
        garden.saveSector(Sector(name = "Aromáticas", location = "Patio", areaM2 = 3.0, description = ""), admin)
        assertRejected {
            garden.saveSector(Sector(name = "aromáticas", location = "Otro", areaM2 = 1.0, description = ""), admin)
        }
        val sector = garden.observeSectors().first().first { it.name == "Aromáticas" }
        garden.saveCrop(
            Crop(sectorId = sector.id, name = "Albahaca", variety = "", plantingDate = 0, harvestDate = 30,
                wateringFrequencyDays = 2, notes = ""),
            admin
        )
        assertEquals(1, garden.observeCrops(sector.id).first().size)

        garden.deleteSector(sector, admin)
        assertTrue(garden.observeCrops(sector.id).first().isEmpty())
    }

    @Test
    fun actionsAreRecordedInHistory() = runBlocking {
        val id = newTaskFor(ana)
        tasks.startTask(id, ana)
        val history = db.activityDao().observeForUser(ana.id).first().map { it.activity.message }
        assertTrue(history.any { it.startsWith("Inició la tarea") })
        assertTrue(history.any { it.startsWith("Creó y asignó") })
    }
}
