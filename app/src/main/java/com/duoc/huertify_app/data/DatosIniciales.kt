package com.duoc.huertify_app.data

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.HuertifyDatabase
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.hashPassword
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskType
import java.time.LocalDate

/** Cuentas ficticias y datos de ejemplo para el MVP (se cargan solo si la BD está vacía). */
object SeedData {

    data class DemoAccount(val name: String, val email: String, val password: String, val role: Role)

    val accounts = listOf(
        DemoAccount("Prof. Carla Muñoz", "docente@huertify.cl", "admin123", Role.ADMIN),
        DemoAccount("Ana Torres", "ana@huertify.cl", "huerto123", Role.STUDENT),
        DemoAccount("Benjamín Rojas", "benjamin@huertify.cl", "huerto123", Role.STUDENT),
        DemoAccount("Sofía Pérez", "sofia@huertify.cl", "huerto123", Role.STUDENT)
    )

    suspend fun seedIfEmpty(db: HuertifyDatabase) {
        val userDao = db.userDao()
        if (userDao.count() > 0) return

        val ids = accounts.map {
            userDao.insert(User(name = it.name, email = it.email, passwordHash = hashPassword(it.password), role = it.role))
        }
        val (_, ana, benjamin, sofia) = ids

        val today = LocalDate.now()
        val sectorDao = db.sectorDao()
        val bancal = sectorDao.insert(
            Sector(name = "Bancal Norte", location = "Patio central, junto al invernadero", areaM2 = 12.0,
                description = "Bancal elevado para hortalizas de hoja")
        )
        val invernadero = sectorDao.insert(
            Sector(name = "Invernadero", location = "Costado del gimnasio", areaM2 = 20.5,
                description = "Almácigos y cultivos de temporada")
        )
        sectorDao.insert(
            Sector(name = "Compostera", location = "Fondo del patio", areaM2 = 4.0,
                description = "Zona de compostaje y lombricultura")
        )

        val cropDao = db.cropDao()
        cropDao.insert(
            Crop(sectorId = bancal, name = "Lechuga", variety = "Escarola", plantingDate = today.minusDays(20).toEpochDay(),
                harvestDate = today.plusDays(40).toEpochDay(), wateringFrequencyDays = 2, notes = "Regar temprano")
        )
        cropDao.insert(
            Crop(sectorId = bancal, name = "Acelga", variety = "Penca blanca", plantingDate = today.minusDays(30).toEpochDay(),
                harvestDate = today.plusDays(30).toEpochDay(), wateringFrequencyDays = 3, notes = "")
        )
        cropDao.insert(
            Crop(sectorId = invernadero, name = "Tomate", variety = "Cherry", plantingDate = today.minusDays(10).toEpochDay(),
                harvestDate = today.plusDays(80).toEpochDay(), wateringFrequencyDays = 1, notes = "Requiere tutor")
        )

        val taskDao = db.taskDao()
        taskDao.insert(
            Task(title = "Regar el bancal norte", description = "Riego completo de lechugas y acelgas",
                instructions = "Usar regadera, evitar mojar las hojas. Registrar con foto el suelo húmedo.",
                type = TaskType.RIEGO, sectorId = bancal, assigneeId = ana, scheduledDate = today.toEpochDay())
        )
        taskDao.insert(
            Task(title = "Desmalezar invernadero", description = "Retirar malezas entre las hileras de tomate",
                instructions = "Usar guantes. Depositar malezas en la compostera.",
                type = TaskType.DESMALEZADO, sectorId = invernadero, assigneeId = benjamin,
                scheduledDate = today.minusDays(2).toEpochDay())
        )
        taskDao.insert(
            Task(title = "Voltear compost", description = "Airear la pila de compost",
                instructions = "Voltear con pala y medir humedad apretando un puñado.",
                type = TaskType.ABONO, sectorId = null, assigneeId = sofia,
                scheduledDate = today.plusDays(3).toEpochDay(), status = TaskStatus.EN_DESARROLLO)
        )
        taskDao.insert(
            Task(title = "Sembrar rabanitos", description = "Siembra directa en el bancal norte",
                instructions = "Surcos de 1 cm, separación de 5 cm entre semillas.",
                type = TaskType.SIEMBRA, sectorId = bancal, assigneeId = ana,
                scheduledDate = today.plusDays(5).toEpochDay())
        )
    }
}
