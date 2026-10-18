package com.duoc.huertify_app.di

import android.content.Context
import com.duoc.huertify_app.data.ImageStorage
import com.duoc.huertify_app.data.SeedData
import com.duoc.huertify_app.data.local.HuertifyDatabase
import com.duoc.huertify_app.data.remote.RetrofitClient
import com.duoc.huertify_app.data.repository.ActivityRepository
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.data.repository.WeatherRepository

/** Inyección de dependencias manual: una única instancia por aplicación. */
class AppContainer(context: Context) {
    private val database = HuertifyDatabase.create(context)
    private val prefs = context.getSharedPreferences("huertify_session", Context.MODE_PRIVATE)

    val activityRepository = ActivityRepository(database.activityDao())
    val authRepository = AuthRepository(database.userDao(), prefs, activityRepository)
    val gardenRepository = GardenRepository(database.sectorDao(), database.cropDao(), activityRepository)
    val taskRepository = TaskRepository(database.taskDao(), activityRepository)
    val reportRepository = ReportRepository(database.reportDao(), activityRepository)
    val imageStorage = ImageStorage(context.applicationContext)

    // Ubicación del huerto escolar (Santiago, Chile) para el pronóstico del clima.
    val weatherRepository = WeatherRepository(RetrofitClient.weatherApi, latitude = -33.45, longitude = -70.66)

    suspend fun initialize() {
        SeedData.seedIfEmpty(database)
        authRepository.restoreSession()
    }
}
