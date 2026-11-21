package com.duoc.huertify_app.data

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.HuertifyDatabase
import com.duoc.huertify_app.data.local.Maintenance
import com.duoc.huertify_app.data.local.ProblemReport
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.repository.hashPassword
import com.duoc.huertify_app.domain.model.CropStatus
import com.duoc.huertify_app.domain.model.ProblemType
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.SectorStatus
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskType
import java.time.LocalDate
import java.time.ZoneId

/** Cuentas ficticias y datos de ejemplo para el MVP (se cargan solo si la BD está vacía). */
object SeedData {

    const val WORKSHOP_5 = "Taller 5° Básico"
    const val WORKSHOP_7 = "Taller 7° Básico"

    data class DemoAccount(
        val name: String,
        val email: String,
        val password: String,
        val role: Role,
        val workshop: String? = null
    )

    val accounts = listOf(
        DemoAccount("Prof. Carla Muñoz", "docente@huertify.cl", "admin123", Role.ADMIN),
        DemoAccount("Ana Torres", "ana@huertify.cl", "huerto123", Role.STUDENT, WORKSHOP_5),
        DemoAccount("Benjamín Rojas", "benjamin@huertify.cl", "huerto123", Role.STUDENT, WORKSHOP_5),
        DemoAccount("Sofía Pérez", "sofia@huertify.cl", "huerto123", Role.STUDENT, WORKSHOP_7)
    )

    suspend fun seedIfEmpty(db: HuertifyDatabase) {
        val userDao = db.userDao()
        if (userDao.count() > 0) return

        val ids = accounts.map {
            userDao.insert(
                User(name = it.name, email = it.email, passwordHash = hashPassword(it.password), role = it.role, workshop = it.workshop)
            )
        }
        val (docente, ana, benjamin, sofia) = ids

        val today = LocalDate.now()
        val sectorDao = db.sectorDao()
        val bancal = sectorDao.insert(
            Sector(code = "S-01", name = "Bancal Norte", location = "Patio central, junto al invernadero", areaM2 = 12.0,
                description = "Bancal elevado para hortalizas de hoja", status = SectorStatus.EN_USO, responsibleId = docente)
        )
        val invernadero = sectorDao.insert(
            Sector(code = "S-02", name = "Invernadero", location = "Costado del gimnasio", areaM2 = 20.5,
                description = "Almácigos y cultivos de temporada", status = SectorStatus.EN_USO, responsibleId = ana)
        )
        val compostera = sectorDao.insert(
            Sector(code = "S-03", name = "Compostera", location = "Fondo del patio", areaM2 = 4.0,
                description = "Zona de compostaje y lombricultura", status = SectorStatus.EN_MANTENIMIENTO, responsibleId = sofia)
        )
        sectorDao.insert(
            Sector(code = "S-04", name = "Bancal Sur", location = "Junto a la sala de 1° básico", areaM2 = 8.0,
                description = "Bancal libre para la próxima siembra", status = SectorStatus.DISPONIBLE)
        )

        val cropDao = db.cropDao()
        val lechuga = cropDao.insert(
            Crop(sectorId = bancal, name = "Lechuga", variety = "Escarola", description = "Hortaliza de hoja de ciclo corto",
                plantingDate = today.minusDays(20).toEpochDay(), transplantDate = today.minusDays(12).toEpochDay(),
                harvestDate = today.plusDays(5).toEpochDay(), wateringFrequencyDays = 2,
                status = CropStatus.EN_CRECIMIENTO, notes = "Regar temprano")
        )
        val acelga = cropDao.insert(
            Crop(sectorId = bancal, name = "Acelga", variety = "Penca blanca", description = "Hoja verde de cosecha escalonada",
                plantingDate = today.minusDays(30).toEpochDay(), harvestDate = today.plusDays(30).toEpochDay(),
                wateringFrequencyDays = 3, status = CropStatus.EN_CRECIMIENTO, notes = "")
        )
        val tomate = cropDao.insert(
            Crop(sectorId = invernadero, name = "Tomate", variety = "Cherry", description = "Fruto pequeño, requiere tutorado",
                plantingDate = today.minusDays(10).toEpochDay(), harvestDate = today.plusDays(80).toEpochDay(),
                wateringFrequencyDays = 1, status = CropStatus.SEMBRADO, notes = "Requiere tutor")
        )
        cropDao.insert(
            Crop(sectorId = invernadero, name = "Albahaca", variety = "Genovesa", description = "Aromática para el almácigo",
                plantingDate = today.plusDays(7).toEpochDay(), harvestDate = today.plusDays(60).toEpochDay(),
                wateringFrequencyDays = 2, status = CropStatus.PLANIFICADO, notes = "")
        )

        val taskDao = db.taskDao()
        // Riego ya validado hace 3 días: sirve de base para calcular el próximo riego de la lechuga.
        val threeDaysAgo = today.minusDays(3).atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        taskDao.insert(
            Task(title = "Regar lechugas", description = "Riego de las lechugas del bancal norte",
                instructions = "Regar a ras de suelo.\nNo mojar las hojas.\nTomar una foto del suelo húmedo.",
                type = TaskType.RIEGO, sectorId = bancal, cropId = lechuga, assigneeId = ana, creatorId = docente,
                scheduledDate = today.minusDays(3).toEpochDay(), status = TaskStatus.VALIDADA,
                studentComment = "Regué todas las lechugas", completedAt = threeDaysAgo,
                reviewComment = "¡Buen trabajo!", reviewerId = docente, reviewedAt = threeDaysAgo)
        )
        val yesterday = today.minusDays(1).atTime(9, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        taskDao.insert(
            Task(title = "Regar acelgas", description = "Riego de las acelgas del bancal norte",
                instructions = "Regar a ras de suelo.\nRevisar que el agua no se acumule.",
                type = TaskType.RIEGO, sectorId = bancal, cropId = acelga, assigneeId = benjamin, creatorId = docente,
                scheduledDate = today.minusDays(1).toEpochDay(), status = TaskStatus.VALIDADA,
                completedAt = yesterday, reviewerId = docente, reviewedAt = yesterday)
        )
        taskDao.insert(
            Task(title = "Regar tomates", description = "Riego diario de los tomates del invernadero",
                instructions = "Regar en la base de cada planta.\nRevisar los tutores.",
                type = TaskType.RIEGO, sectorId = invernadero, cropId = tomate, assigneeId = sofia, creatorId = docente,
                scheduledDate = today.minusDays(1).toEpochDay(), status = TaskStatus.VALIDADA,
                studentComment = "Regados y tutores firmes", completedAt = yesterday, reviewerId = docente, reviewedAt = yesterday)
        )
        taskDao.insert(
            Task(title = "Regar el bancal norte", description = "Riego completo de lechugas y acelgas",
                instructions = "Usar regadera.\nEvitar mojar las hojas.\nRegistrar con foto el suelo húmedo.",
                type = TaskType.RIEGO, sectorId = bancal, assigneeId = ana, creatorId = docente,
                scheduledDate = today.toEpochDay())
        )
        taskDao.insert(
            Task(title = "Desmalezar invernadero", description = "Retirar malezas entre las hileras de tomate",
                instructions = "Usar guantes.\nSacar la maleza con raíz.\nDepositar las malezas en la compostera.",
                type = TaskType.DESMALEZADO, sectorId = invernadero, cropId = tomate, assigneeId = benjamin,
                creatorId = docente, groupName = WORKSHOP_5, scheduledDate = today.minusDays(2).toEpochDay())
        )
        taskDao.insert(
            Task(title = "Voltear compost", description = "Airear la pila de compost",
                instructions = "Voltear con pala.\nMedir la humedad apretando un puñado.",
                type = TaskType.MANTENCION, sectorId = compostera, assigneeId = sofia, creatorId = docente,
                scheduledDate = today.plusDays(3).toEpochDay(), status = TaskStatus.EN_DESARROLLO)
        )
        taskDao.insert(
            Task(title = "Sembrar rabanitos", description = "Siembra directa en el bancal norte",
                instructions = "Hacer surcos de 1 cm.\nDejar 5 cm entre semillas.\nCubrir y regar suavemente.",
                type = TaskType.SIEMBRA, sectorId = bancal, assigneeId = ana, creatorId = docente,
                scheduledDate = today.plusDays(5).toEpochDay())
        )

        db.maintenanceDao().insert(
            Maintenance(sectorId = invernadero, title = "Revisar plástico del invernadero",
                notes = "Buscar roturas y reparar con cinta", scheduledDate = today.plusDays(4).toEpochDay())
        )
        db.maintenanceDao().insert(
            Maintenance(sectorId = compostera, title = "Cambiar malla de la compostera",
                scheduledDate = today.minusDays(1).toEpochDay())
        )

        db.reportDao().insert(
            ProblemReport(reporterId = benjamin, sectorId = invernadero, cropId = tomate, type = ProblemType.PLAGA,
                description = "Pulgones simulados en las hojas bajas del tomate", photoUri = null)
        )
    }
}
