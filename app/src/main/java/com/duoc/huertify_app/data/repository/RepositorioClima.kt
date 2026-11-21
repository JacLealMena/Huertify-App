package com.duoc.huertify_app.data.repository

import com.duoc.huertify_app.data.remote.ForecastResponse
import com.duoc.huertify_app.data.remote.WeatherApi
import com.duoc.huertify_app.domain.DailyForecast
import com.duoc.huertify_app.domain.WeatherRules
import com.duoc.huertify_app.domain.WeatherSummary
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

/** Consume la API REST del clima mediante Retrofit y la convierte a modelos de dominio. */
class WeatherRepository(
    private val api: WeatherApi,
    private val latitude: Double,
    private val longitude: Double
) {
    suspend fun getForecast(): Result<WeatherSummary> = try {
        Result.success(api.getForecast(latitude, longitude).toSummary())
    } catch (e: IOException) {
        Result.failure(BusinessException("Sin conexión a Internet: no se pudo obtener el clima"))
    } catch (e: HttpException) {
        Result.failure(BusinessException("El servicio de clima respondió con error ${e.code()}"))
    } catch (e: Exception) {
        Result.failure(BusinessException("No se pudo interpretar la respuesta del clima"))
    }
}

/** Convierte el DTO de la API al modelo de dominio. */
fun ForecastResponse.toSummary(): WeatherSummary {
    val d = daily
    val days = d?.dates.orEmpty().mapIndexed { i, date ->
        DailyForecast(
            date = LocalDate.parse(date),
            maxTemp = d?.maxTemperatures?.getOrNull(i) ?: 0.0,
            minTemp = d?.minTemperatures?.getOrNull(i) ?: 0.0,
            precipitationMm = d?.precipitation?.getOrNull(i) ?: 0.0,
            description = WeatherRules.describe(d?.weatherCodes?.getOrNull(i))
        )
    }
    return WeatherSummary(
        currentTemp = current?.temperature ?: days.firstOrNull()?.maxTemp ?: 0.0,
        currentDescription = WeatherRules.describe(current?.weatherCode),
        days = days,
        wateringAdvice = WeatherRules.wateringAdvice(days.firstOrNull())
    )
}
