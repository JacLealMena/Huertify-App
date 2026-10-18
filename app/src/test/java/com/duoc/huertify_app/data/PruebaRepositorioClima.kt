package com.duoc.huertify_app.data

import com.duoc.huertify_app.data.remote.CurrentDto
import com.duoc.huertify_app.data.remote.DailyDto
import com.duoc.huertify_app.data.remote.ForecastResponse
import com.duoc.huertify_app.data.remote.WeatherApi
import com.duoc.huertify_app.data.repository.WeatherRepository
import com.duoc.huertify_app.domain.DailyForecast
import com.duoc.huertify_app.domain.WeatherRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class WeatherRepositoryTest {

    private fun fakeApi(block: () -> ForecastResponse) = object : WeatherApi {
        override suspend fun getForecast(
            latitude: Double, longitude: Double, current: String, daily: String, timezone: String, forecastDays: Int
        ): ForecastResponse = block()
    }

    private val sample = ForecastResponse(
        current = CurrentDto(temperature = 18.4, precipitation = 0.0, weatherCode = 61),
        daily = DailyDto(
            dates = listOf("2026-10-17", "2026-10-18"),
            maxTemperatures = listOf(20.0, 25.0),
            minTemperatures = listOf(9.0, 10.0),
            precipitation = listOf(5.2, 0.0),
            weatherCodes = listOf(61, 0)
        )
    )

    @Test
    fun mapsApiResponseToDomain() = runBlocking {
        val summary = WeatherRepository(fakeApi { sample }, 0.0, 0.0).getForecast().getOrThrow()
        assertEquals(18.4, summary.currentTemp, 0.01)
        assertEquals("Lluvia", summary.currentDescription)
        assertEquals(2, summary.days.size)
        assertEquals(LocalDate.of(2026, 10, 17), summary.days[0].date)
        assertEquals("Despejado", summary.days[1].description)
        assertTrue(summary.wateringAdvice.contains("no sea necesario regar"))
    }

    @Test
    fun networkErrorBecomesFriendlyFailure() = runBlocking {
        val result = WeatherRepository(fakeApi { throw IOException("offline") }, 0.0, 0.0).getForecast()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Sin conexión"))
    }

    @Test
    fun wateringAdviceRules() {
        fun day(max: Double, min: Double, rain: Double) = DailyForecast(LocalDate.now(), max, min, rain, "")
        assertTrue(WeatherRules.wateringAdvice(day(30.0, 15.0, 0.0)).contains("caluroso"))
        assertTrue(WeatherRules.wateringAdvice(day(10.0, 0.0, 0.0)).contains("helada"))
        assertTrue(WeatherRules.wateringAdvice(day(20.0, 10.0, 0.0)).contains("templado"))
        assertTrue(WeatherRules.wateringAdvice(null).contains("Sin pronóstico"))
    }
}
