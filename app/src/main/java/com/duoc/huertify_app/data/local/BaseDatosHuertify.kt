package com.duoc.huertify_app.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [User::class, Sector::class, Crop::class, Task::class, ProblemReport::class, ActivityLog::class],
    version = 1,
    exportSchema = false
)
abstract class HuertifyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sectorDao(): SectorDao
    abstract fun cropDao(): CropDao
    abstract fun taskDao(): TaskDao
    abstract fun reportDao(): ReportDao
    abstract fun activityDao(): ActivityDao

    companion object {
        fun create(context: Context): HuertifyDatabase =
            Room.databaseBuilder(context.applicationContext, HuertifyDatabase::class.java, "huertify.db")
                .setDriver(AndroidSQLiteDriver())
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
