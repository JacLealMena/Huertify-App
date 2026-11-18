package com.duoc.huertify_app.data.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.duoc.huertify_app.domain.model.ActivityKind
import com.duoc.huertify_app.domain.model.CropStatus
import com.duoc.huertify_app.domain.model.ProblemType
import com.duoc.huertify_app.domain.model.ReportStatus
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.SectorStatus
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.domain.model.TaskType
import java.time.LocalDate

// Las fechas de calendario se guardan como epochDay (Long) y los instantes como epoch millis.

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: Role,
    /** Taller o grupo al que pertenece el estudiante (permite asignar tareas a un grupo). */
    val workshop: String? = null
)

@Entity(
    tableName = "sectors",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["responsibleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["code"], unique = true),
        Index(value = ["responsibleId"])
    ]
)
data class Sector(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val location: String,
    val areaM2: Double,
    val description: String,
    val status: SectorStatus = SectorStatus.DISPONIBLE,
    val responsibleId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** Sector con el nombre del responsable y la cantidad de cultivos, para el listado. */
data class SectorWithInfo(
    @Embedded val sector: Sector,
    val responsibleName: String?,
    val cropCount: Int
)

@Entity(
    tableName = "crops",
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sectorId"])]
)
data class Crop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectorId: Long,
    val name: String,
    val variety: String,
    val description: String = "",
    val plantingDate: Long,
    /** Fecha de trasplante; solo si corresponde. */
    val transplantDate: Long? = null,
    val harvestDate: Long,
    val wateringFrequencyDays: Int,
    val status: CropStatus = CropStatus.PLANIFICADO,
    /** Fotografía referencial ficticia del cultivo. */
    val photoUri: String? = null,
    val notes: String
) {
    val planting: LocalDate get() = LocalDate.ofEpochDay(plantingDate)
    val transplant: LocalDate? get() = transplantDate?.let(LocalDate::ofEpochDay)
    val harvest: LocalDate get() = LocalDate.ofEpochDay(harvestDate)
}

/** Cultivo con el nombre de su sector, para listados generales. */
data class CropWithSector(
    @Embedded val crop: Crop,
    val sectorName: String
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["assigneeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Crop::class,
            parentColumns = ["id"],
            childColumns = ["cropId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["assigneeId"]), Index(value = ["sectorId"]),
        Index(value = ["cropId"]), Index(value = ["creatorId"])
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val instructions: String,
    val type: TaskType,
    val sectorId: Long?,
    val cropId: Long? = null,
    val assigneeId: Long,
    /** Taller o grupo, cuando la tarea se asignó a un grupo completo. */
    val groupName: String? = null,
    /** Docente o tallerista que creó la tarea. */
    val creatorId: Long? = null,
    val scheduledDate: Long,
    val status: TaskStatus = TaskStatus.PENDIENTE,
    val studentComment: String? = null,
    val evidenceUri: String? = null,
    /** Fecha y hora en que el estudiante marcó la tarea como realizada. */
    val completedAt: Long? = null,
    val rejectionReason: String? = null,
    /** Comentario opcional del docente al validar. */
    val reviewComment: String? = null,
    val reviewerId: Long? = null,
    val reviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val scheduled: LocalDate get() = LocalDate.ofEpochDay(scheduledDate)

    fun isOverdue(today: LocalDate = LocalDate.now()): Boolean =
        status.isOpen && scheduled.isBefore(today)
}

/** Tarea con los nombres relacionados (responsable, sector, cultivo, creador y revisor). */
data class TaskWithNames(
    @Embedded val task: Task,
    val assigneeName: String,
    val sectorName: String?,
    val cropName: String? = null,
    val creatorName: String? = null,
    val reviewerName: String? = null
)

@Entity(
    tableName = "problem_reports",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["reporterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Crop::class,
            parentColumns = ["id"],
            childColumns = ["cropId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["reporterId"]), Index(value = ["sectorId"]), Index(value = ["cropId"])]
)
data class ProblemReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reporterId: Long,
    val sectorId: Long?,
    val cropId: Long? = null,
    val type: ProblemType,
    val description: String,
    val photoUri: String?,
    val status: ReportStatus = ReportStatus.INFORMADO,
    /** Comentario de seguimiento del docente, visible para el estudiante. */
    val adminComment: String? = null,
    /** Fecha y hora en que el reporte fue atendido o cerrado. */
    val attendedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ReportWithNames(
    @Embedded val report: ProblemReport,
    val reporterName: String,
    val sectorName: String?,
    val cropName: String? = null
)

/** Mantenimiento programado de un sector (HU-26). */
@Entity(
    tableName = "maintenances",
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sectorId"])]
)
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectorId: Long,
    val title: String,
    val notes: String = "",
    val scheduledDate: Long,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val scheduled: LocalDate get() = LocalDate.ofEpochDay(scheduledDate)
}

data class MaintenanceWithSector(
    @Embedded val maintenance: Maintenance,
    val sectorName: String
)

@Entity(
    tableName = "activity_log",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["sectorId"]), Index(value = ["cropId"])]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val message: String,
    val kind: ActivityKind = ActivityKind.GENERAL,
    val taskId: Long? = null,
    val sectorId: Long? = null,
    val cropId: Long? = null,
    /** Foto asociada al registro (evidencia o foto de un problema). */
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActivityWithUser(
    @Embedded val activity: ActivityLog,
    val userName: String
)
