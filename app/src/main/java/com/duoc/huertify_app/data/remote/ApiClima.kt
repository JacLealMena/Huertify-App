package com.duoc.huertify_app.data.remote

import com.duoc.huertify_app.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** API REST pública de Open-Meteo (sin API key): https://open-meteo.com/en/docs */
interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,precipitation,weather_code",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 3
    ): ForecastResponse
}

// ---------- DTOs (respuesta JSON) ----------

data class ForecastResponse(
    @SerializedName("current") val current: CurrentDto?,
    @SerializedName("daily") val daily: DailyDto?
)

data class CurrentDto(
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("precipitation") val precipitation: Double?,
    @SerializedName("weather_code") val weatherCode: Int?
)

data class DailyDto(
    @SerializedName("time") val dates: List<String>?,
    @SerializedName("temperature_2m_max") val maxTemperatures: List<Double?>?,
    @SerializedName("temperature_2m_min") val minTemperatures: List<Double?>?,
    @SerializedName("precipitation_sum") val precipitation: List<Double?>?,
    @SerializedName("weather_code") val weatherCodes: List<Int?>?
)

object RetrofitClient {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()
    }

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.WEATHER_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}
