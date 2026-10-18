package com.duoc.huertify_app.domain.model

enum class Role(val label: String) {
    ADMIN("Administrador"),
    STUDENT("Colaborador")
}

enum class TaskType(val label: String) {
    RIEGO("Riego"),
    SIEMBRA("Siembra"),
    COSECHA("Cosecha"),
    DESMALEZADO("Desmalezado"),
    ABONO("Abono / Compost"),
    MANTENCION("Mantención"),
    OTRO("Otro")
}

enum class TaskStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EN_DESARROLLO("En desarrollo"),
    PENDIENTE_VALIDACION("Pendiente de validación"),
    VALIDADA("Validada"),
    RECHAZADA("Rechazada");

    /** Tareas que aún requieren trabajo del estudiante (sirven para detectar atrasos). */
    val isOpen: Boolean get() = this == PENDIENTE || this == EN_DESARROLLO || this == RECHAZADA
}

enum class Severity(val label: String) {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta")
}
