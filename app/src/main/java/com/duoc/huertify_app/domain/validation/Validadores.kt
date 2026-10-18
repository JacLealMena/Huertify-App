package com.duoc.huertify_app.domain.validation

import java.time.LocalDate

/** Reglas de negocio de formularios. Devuelven el mensaje de error o null si el valor es válido. */
object Validators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun email(value: String): String? = when {
        value.isBlank() -> "El correo es obligatorio"
        !EMAIL_REGEX.matches(value.trim()) -> "Formato de correo inválido (ej: nombre@huertify.cl)"
        else -> null
    }

    fun password(value: String): String? = when {
        value.isBlank() -> "La contraseña es obligatoria"
        value.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }

    fun requiredText(value: String, field: String, min: Int = 1, max: Int = 200): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Debes ingresar ${field.lowercase()}"
            text.length < min -> "$field debe tener al menos $min caracteres"
            text.length > max -> "$field no puede superar los $max caracteres"
            else -> null
        }
    }

    fun optionalText(value: String, field: String, max: Int): String? =
        if (value.trim().length > max) "$field no puede superar los $max caracteres" else null

    fun <T> required(value: T?, message: String): String? = if (value == null) message else null

    fun positiveDecimal(value: String, field: String): String? {
        val text = value.trim().replace(',', '.')
        val number = text.toDoubleOrNull()
        return when {
            text.isEmpty() -> "Debes ingresar ${field.lowercase()}"
            number == null -> "$field debe ser un número (ej: 12.5)"
            number <= 0 -> "$field debe ser mayor a 0"
            number > 100_000 -> "$field es demasiado grande"
            else -> null
        }
    }

    fun wateringFrequency(value: String): String? {
        val text = value.trim()
        val number = text.toIntOrNull()
        return when {
            text.isEmpty() -> "La frecuencia de riego es obligatoria"
            !text.all { it.isDigit() } || number == null -> "Ingresa un número entero de días"
            number <= 0 -> "La frecuencia de riego debe ser mayor a 0"
            number > 60 -> "La frecuencia no puede superar 60 días"
            else -> null
        }
    }

    fun plantingDate(date: LocalDate?, today: LocalDate): String? = when {
        date == null -> "La fecha de siembra es obligatoria"
        date.isAfter(today.plusYears(1)) -> "La fecha de siembra es demasiado lejana"
        else -> null
    }

    fun harvestDate(harvest: LocalDate?, planting: LocalDate?): String? = when {
        harvest == null -> "La fecha estimada de cosecha es obligatoria"
        planting != null && !harvest.isAfter(planting) ->
            "La cosecha debe ser posterior a la fecha de siembra"
        else -> null
    }

    fun scheduledDate(date: LocalDate?, today: LocalDate, allowPast: Boolean): String? = when {
        date == null -> "La fecha programada es obligatoria"
        !allowPast && date.isBefore(today) -> "La fecha programada no puede estar en el pasado"
        else -> null
    }

    fun rejectionReason(value: String): String? = when {
        value.isBlank() -> "Debes indicar el motivo del rechazo"
        value.trim().length < 10 -> "Explica el motivo con al menos 10 caracteres"
        value.trim().length > 300 -> "El motivo no puede superar los 300 caracteres"
        else -> null
    }
}
