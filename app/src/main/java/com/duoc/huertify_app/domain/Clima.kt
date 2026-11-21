package com.duoc.huertify_app.domain

import java.time.LocalDate

data class DailyForecast(
    val date: LocalDate,
    val maxTemp: Double,
    val minTemp: Double,
    val precipitationMm: Double,
    val description: String
)

data class WeatherSummary(
    val currentTemp: Double,
    val currentDescription: String,
    val days: List<DailyForecast>,
    val wateringAdvice: String
)

/** Reglas para traducir el pronóstico en información útil para el huerto. */
object WeatherRules {

    const val RAIN_THRESHOLD_MM = 2.0
    const val HOT_THRESHOLD_C = 28.0
    const val FROST_THRESHOLD_C = 2.0

    fun wateringAdvice(today: DailyForecast?): String = when {
        today == null -> "Sin pronóstico: sigue el calendario de riego de cada cultivo."
        today.precipitationMm >= RAIN_THRESHOLD_MM ->
            "Se esperan ${"%.1f".format(today.precipitationMm)} mm de lluvia: probablemente no sea necesario regar hoy."
        today.minTemp <= FROST_THRESHOLD_C ->
            "Riesgo de helada (mín. ${"%.0f".format(today.minTemp)}°C): cubre los almácigos y evita regar al atardecer."
        today.maxTemp >= HOT_THRESHOLD_C ->
            "Día caluroso (máx. ${"%.0f".format(today.maxTemp)}°C): riega temprano en la mañana o al atardecer."
        else -> "Clima templado: riega según la frecuencia de cada cultivo."
    }

    /** Códigos WMO usados por Open-Meteo. */
    fun describe(code: Int?): String = when (code) {
        0 -> "Despejado"
        1, 2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55, 56, 57 -> "Llovizna"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Lluvia"
        71, 73, 75, 77, 85, 86 -> "Nieve"
        95, 96, 99 -> "Tormenta"
        else -> "Sin datos"
    }
}
