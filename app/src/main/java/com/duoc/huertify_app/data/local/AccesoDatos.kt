package com.duoc.huertify_app.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.duoc.huertify_app.domain.model.Role
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Long): User?

    @Query("SELECT * FROM users WHERE role = :role ORDER BY name")
    fun observeByRole(role: Role): Flow<List<User>>
}

@Dao
interface SectorDao {
    @Insert
    suspend fun insert(sector: Sector): Long

    @Update
    suspend fun update(sector: Sector)

    @Delete
    suspend fun delete(sector: Sector)

    @Query("SELECT * FROM sectors ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Sector>>

    @Query("SELECT * FROM sectors WHERE id = :id")
    fun observeById(id: Long): Flow<Sector?>

    @Query("SELECT * FROM sectors WHERE id = :id")
    suspend fun findById(id: Long): Sector?

    @Query("SELECT COUNT(*) FROM sectors WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long): Int
}

@Dao
interface CropDao {
    @Insert
    suspend fun insert(crop: Crop): Long

    @Update
    suspend fun update(crop: Crop)

    @Delete
    suspend fun delete(crop: Crop)

    @Query("SELECT * FROM crops WHERE sectorId = :sectorId ORDER BY name COLLATE NOCASE")
    fun observeBySector(sectorId: Long): Flow<List<Crop>>

    @Query("SELECT * FROM crops WHERE id = :id")
    suspend fun findById(id: Long): Crop?

    @Query("SELECT COUNT(*) FROM crops")
    fun observeCount(): Flow<Int>
}

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun findById(id: Long): Task?

    @Query(
        """
        SELECT tasks.*, users.name AS assigneeName, sectors.name AS sectorName
        FROM tasks
        INNER JOIN users ON users.id = tasks.assigneeId
        LEFT JOIN sectors ON sectors.id = tasks.sectorId
        ORDER BY tasks.scheduledDate ASC, tasks.id DESC
        """
    )
    fun observeAll(): Flow<List<TaskWithNames>>

    @Query(
        """
        SELECT tasks.*, users.name AS assigneeName, sectors.name AS sectorName
        FROM tasks
        INNER JOIN users ON users.id = tasks.assigneeId
        LEFT JOIN sectors ON sectors.id = tasks.sectorId
        WHERE tasks.assigneeId = :userId
        ORDER BY tasks.scheduledDate ASC, tasks.id DESC
        """
    )
    fun observeByAssignee(userId: Long): Flow<List<TaskWithNames>>

    @Query(
        """
        SELECT tasks.*, users.name AS assigneeName, sectors.name AS sectorName
        FROM tasks
        INNER JOIN users ON users.id = tasks.assigneeId
        LEFT JOIN sectors ON sectors.id = tasks.sectorId
        WHERE tasks.id = :id
        """
    )
    fun observeById(id: Long): Flow<TaskWithNames?>
}

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: ProblemReport): Long

    @Query("UPDATE problem_reports SET resolved = :resolved WHERE id = :id")
    suspend fun setResolved(id: Long, resolved: Boolean)

    @Query(
        """
        SELECT problem_reports.*, users.name AS reporterName, sectors.name AS sectorName
        FROM problem_reports
        INNER JOIN users ON users.id = problem_reports.reporterId
        LEFT JOIN sectors ON sectors.id = problem_reports.sectorId
        ORDER BY problem_reports.resolved ASC, problem_reports.createdAt DESC
        """
    )
    fun observeAll(): Flow<List<ReportWithNames>>
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: ActivityLog): Long

    @Query(
        """
        SELECT activity_log.*, users.name AS userName FROM activity_log
        INNER JOIN users ON users.id = activity_log.userId
        ORDER BY activity_log.timestamp DESC LIMIT 200
        """
    )
    fun observeAll(): Flow<List<ActivityWithUser>>

    @Query(
        """
        SELECT activity_log.*, users.name AS userName FROM activity_log
        INNER JOIN users ON users.id = activity_log.userId
        WHERE activity_log.userId = :userId
           OR activity_log.taskId IN (SELECT id FROM tasks WHERE assigneeId = :userId)
        ORDER BY activity_log.timestamp DESC LIMIT 200
        """
    )
    fun observeForUser(userId: Long): Flow<List<ActivityWithUser>>
}
