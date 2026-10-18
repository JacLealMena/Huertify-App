package com.duoc.huertify_app.data.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.Severity
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
    val role: Role
)

@Entity(tableName = "sectors", indices = [Index(value = ["name"], unique = true)])
data class Sector(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,
    val areaM2: Double,
    val description: String
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
    val plantingDate: Long,
    val harvestDate: Long,
    val wateringFrequencyDays: Int,
    val notes: String
) {
    val planting: LocalDate get() = LocalDate.ofEpochDay(plantingDate)
    val harvest: LocalDate get() = LocalDate.ofEpochDay(harvestDate)
}

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
        )
    ],
    indices = [Index(value = ["assigneeId"]), Index(value = ["sectorId"])]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val instructions: String,
    val type: TaskType,
    val sectorId: Long?,
    val assigneeId: Long,
    val scheduledDate: Long,
    val status: TaskStatus = TaskStatus.PENDIENTE,
    val studentComment: String? = null,
    val evidenceUri: String? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val scheduled: LocalDate get() = LocalDate.ofEpochDay(scheduledDate)

    fun isOverdue(today: LocalDate = LocalDate.now()): Boolean =
        status.isOpen && scheduled.isBefore(today)
}

/** Tarea con los nombres del responsable y del sector, para listados. */
data class TaskWithNames(
    @Embedded val task: Task,
    val assigneeName: String,
    val sectorName: String?
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
        )
    ],
    indices = [Index(value = ["reporterId"]), Index(value = ["sectorId"])]
)
data class ProblemReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reporterId: Long,
    val sectorId: Long?,
    val title: String,
    val description: String,
    val severity: Severity,
    val photoUri: String?,
    val resolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ReportWithNames(
    @Embedded val report: ProblemReport,
    val reporterName: String,
    val sectorName: String?
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
    indices = [Index(value = ["userId"])]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val message: String,
    val taskId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActivityWithUser(
    @Embedded val activity: ActivityLog,
    val userName: String
)
