package com.duoc.huertify_app.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import androidx.room3.Update
import com.duoc.huertify_app.domain.model.Role
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Upsert
    suspend fun upsert(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Long): User?

    @Query("SELECT * FROM users WHERE role = :role ORDER BY name")
    fun observeByRole(role: Role): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY role, name")
    fun observeAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = :role AND workshop = :workshop ORDER BY name")
    suspend fun findByWorkshop(workshop: String, role: Role): List<User>
}

@Dao
interface SectorDao {
    @Insert
    suspend fun insert(sector: Sector): Long

    @Update
    suspend fun update(sector: Sector)

    @Upsert
    suspend fun upsertAll(sectors: List<Sector>)

    @Query("DELETE FROM sectors WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<Long>)

    @Query("DELETE FROM sectors")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(sector: Sector)

    @Query("SELECT * FROM sectors ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Sector>>

    @Query(
        """
        SELECT sectors.*, users.name AS responsibleName,
               (SELECT COUNT(*) FROM crops WHERE crops.sectorId = sectors.id) AS cropCount
        FROM sectors
        LEFT JOIN users ON users.id = sectors.responsibleId
        ORDER BY sectors.name COLLATE NOCASE
        """
    )
    fun observeAllWithInfo(): Flow<List<SectorWithInfo>>

    @Query(
        """
        SELECT sectors.*, users.name AS responsibleName,
               (SELECT COUNT(*) FROM crops WHERE crops.sectorId = sectors.id) AS cropCount
        FROM sectors
        LEFT JOIN users ON users.id = sectors.responsibleId
        WHERE sectors.id = :id
        """
    )
    fun observeById(id: Long): Flow<SectorWithInfo?>

    @Query("SELECT * FROM sectors WHERE id = :id")
    suspend fun findById(id: Long): Sector?

    @Query("SELECT COUNT(*) FROM sectors WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long): Int

    @Query("SELECT COUNT(*) FROM sectors WHERE code = :code COLLATE NOCASE AND id != :excludeId")
    suspend fun countByCode(code: String, excludeId: Long): Int
}

@Dao
interface CropDao {
    @Insert
    suspend fun insert(crop: Crop): Long

    @Update
    suspend fun update(crop: Crop)

    @Upsert
    suspend fun upsertAll(crops: List<Crop>)

    @Query("DELETE FROM crops WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<Long>)

    @Query("DELETE FROM crops")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(crop: Crop)

    @Query("SELECT * FROM crops WHERE sectorId = :sectorId ORDER BY name COLLATE NOCASE")
    fun observeBySector(sectorId: Long): Flow<List<Crop>>

    @Query(
        """
        SELECT crops.*, sectors.name AS sectorName FROM crops
        INNER JOIN sectors ON sectors.id = crops.sectorId
        ORDER BY sectors.name COLLATE NOCASE, crops.name COLLATE NOCASE
        """
    )
    fun observeAllWithSector(): Flow<List<CropWithSector>>

    @Query("SELECT * FROM crops WHERE id = :id")
    suspend fun findById(id: Long): Crop?

    @Query("SELECT COUNT(*) FROM crops")
    fun observeCount(): Flow<Int>
}

private const val TASK_SELECT = """
    SELECT tasks.*, assignee.name AS assigneeName, sectors.name AS sectorName, crops.name AS cropName,
           creator.name AS creatorName, reviewer.name AS reviewerName
    FROM tasks
    INNER JOIN users AS assignee ON assignee.id = tasks.assigneeId
    LEFT JOIN sectors ON sectors.id = tasks.sectorId
    LEFT JOIN crops ON crops.id = tasks.cropId
    LEFT JOIN users AS creator ON creator.id = tasks.creatorId
    LEFT JOIN users AS reviewer ON reviewer.id = tasks.reviewerId
"""

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Upsert
    suspend fun upsertAll(tasks: List<Task>)

    @Query("DELETE FROM tasks WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<Long>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun findById(id: Long): Task?

    @Query("$TASK_SELECT ORDER BY tasks.scheduledDate ASC, tasks.id DESC")
    fun observeAll(): Flow<List<TaskWithNames>>

    @Query("$TASK_SELECT WHERE tasks.assigneeId = :userId ORDER BY tasks.scheduledDate ASC, tasks.id DESC")
    fun observeByAssignee(userId: Long): Flow<List<TaskWithNames>>

    @Query("$TASK_SELECT WHERE tasks.id = :id")
    fun observeById(id: Long): Flow<TaskWithNames?>
}

private const val REPORT_SELECT = """
    SELECT problem_reports.*, users.name AS reporterName, sectors.name AS sectorName, crops.name AS cropName
    FROM problem_reports
    INNER JOIN users ON users.id = problem_reports.reporterId
    LEFT JOIN sectors ON sectors.id = problem_reports.sectorId
    LEFT JOIN crops ON crops.id = problem_reports.cropId
"""

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: ProblemReport): Long

    @Update
    suspend fun update(report: ProblemReport)

    @Query("SELECT * FROM problem_reports WHERE id = :id")
    suspend fun findById(id: Long): ProblemReport?

    // Primero los abiertos (Informado, En revisión) y luego los más recientes.
    @Query(
        """
        $REPORT_SELECT
        ORDER BY CASE problem_reports.status WHEN 'INFORMADO' THEN 0 WHEN 'EN_REVISION' THEN 1 ELSE 2 END,
                 problem_reports.createdAt DESC
        """
    )
    fun observeAll(): Flow<List<ReportWithNames>>

    @Query("$REPORT_SELECT WHERE problem_reports.reporterId = :userId ORDER BY problem_reports.createdAt DESC")
    fun observeByReporter(userId: Long): Flow<List<ReportWithNames>>
}

@Dao
interface MaintenanceDao {
    @Insert
    suspend fun insert(maintenance: Maintenance): Long

    @Update
    suspend fun update(maintenance: Maintenance)

    @Delete
    suspend fun delete(maintenance: Maintenance)

    @Query(
        """
        SELECT maintenances.*, sectors.name AS sectorName FROM maintenances
        INNER JOIN sectors ON sectors.id = maintenances.sectorId
        ORDER BY maintenances.done ASC, maintenances.scheduledDate ASC
        """
    )
    fun observeAll(): Flow<List<MaintenanceWithSector>>
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: ActivityLog): Long

    @Query(
        """
        SELECT activity_log.*, users.name AS userName FROM activity_log
        INNER JOIN users ON users.id = activity_log.userId
        ORDER BY activity_log.timestamp DESC LIMIT 300
        """
    )
    fun observeAll(): Flow<List<ActivityWithUser>>

    @Query(
        """
        SELECT activity_log.*, users.name AS userName FROM activity_log
        INNER JOIN users ON users.id = activity_log.userId
        WHERE activity_log.userId = :userId
           OR activity_log.taskId IN (SELECT id FROM tasks WHERE assigneeId = :userId)
        ORDER BY activity_log.timestamp DESC LIMIT 300
        """
    )
    fun observeForUser(userId: Long): Flow<List<ActivityWithUser>>
}
