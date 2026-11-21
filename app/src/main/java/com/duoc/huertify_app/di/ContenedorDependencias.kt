package com.duoc.huertify_app.di

import android.content.Context
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.SeedData
import com.duoc.huertify_app.data.local.HuertifyDatabase
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.data.remote.RetrofitClient
import com.duoc.huertify_app.data.repository.ActivityRepository
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.MaintenanceRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.data.repository.WeatherRepository

/** Inyección de dependencias manual: una única instancia por aplicación. */
class AppContainer(context: Context) {
    private val database = HuertifyDatabase.create(context)
    private val prefs = context.getSharedPreferences("huertify_session", Context.MODE_PRIVATE)
    private val huertifyApi = RetrofitClient.huertifyApi

    val activityRepository = ActivityRepository(database.activityDao())
    val authRepository = AuthRepository(database.userDao(), prefs, activityRepository, huertifyApi)
    val gardenRepository = GardenRepository(database.sectorDao(), database.cropDao(), activityRepository, huertifyApi)
    val taskRepository = TaskRepository(database.taskDao(), database.userDao(), activityRepository, huertifyApi)
    val reportRepository = ReportRepository(database.reportDao(), activityRepository)
    val maintenanceRepository = MaintenanceRepository(database.maintenanceDao(), activityRepository)
    val imageStorage = ImageStorage(context.applicationContext)

    // Ubicación del huerto escolar (Santiago, Chile) para el pronóstico del clima.
    val weatherRepository = WeatherRepository(RetrofitClient.weatherApi, latitude = -33.45, longitude = -70.66)

    suspend fun initialize() {
        SeedData.seedIfEmpty(database)
        val users = huertifyApi.getUsers()
        users.forEach { remote ->
            val local = database.userDao().findById(remote.id)
            database.userDao().upsert(
                local?.copy(
                    name = remote.name,
                    email = remote.email,
                    role = remote.role,
                    workshop = remote.workshop
                ) ?: User(
                    id = remote.id,
                    name = remote.name,
                    email = remote.email,
                    passwordHash = "",
                    role = remote.role,
                    workshop = remote.workshop
                )
            )
        }
        val sectors = huertifyApi.getSectors()
        val crops = huertifyApi.getCrops()
        val tasks = huertifyApi.getTasks()

        database.sectorDao().upsertAll(sectors)
        database.cropDao().upsertAll(crops)
        database.taskDao().upsertAll(tasks)
        if (tasks.isEmpty()) database.taskDao().deleteAll()
        else database.taskDao().deleteMissing(tasks.map { it.id })
        if (crops.isEmpty()) database.cropDao().deleteAll()
        else database.cropDao().deleteMissing(crops.map { it.id })
        if (sectors.isEmpty()) database.sectorDao().deleteAll()
        else database.sectorDao().deleteMissing(sectors.map { it.id })
        authRepository.restoreSession()
    }
}
