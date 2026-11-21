package com.duoc.huertify_app.domain.model

enum class Role(val label: String) {
    ADMIN("Administrador"),
    STUDENT("Colaborador")
}

/** Estados posibles de un sector del huerto (caso, sección 3.2). */
enum class SectorStatus(val label: String) {
    DISPONIBLE("Disponible"),
    EN_USO("En uso"),
    EN_MANTENIMIENTO("En mantenimiento"),
    FUERA_DE_USO("Fuera de uso")
}

/** Estados de desarrollo de un cultivo (caso, sección 3.2). */
enum class CropStatus(val label: String) {
    PLANIFICADO("Planificado"),
    SEMBRADO("Sembrado"),
    EN_CRECIMIENTO("En crecimiento"),
    LISTO_COSECHA("Listo para cosechar"),
    COSECHADO("Cosechado"),
    CON_PROBLEMAS("Con problemas"),
    FINALIZADO("Finalizado");

    /** Un cultivo activo sigue necesitando riego y puede generar recordatorios. */
    val isActive: Boolean get() = this != COSECHADO && this != FINALIZADO
}

/** Tipos de tarea definidos en el caso. */
enum class TaskType(val label: String) {
    PREPARACION("Preparación del sector"),
    SIEMBRA("Siembra"),
    TRASPLANTE("Trasplante"),
    RIEGO("Riego"),
    LIMPIEZA("Limpieza"),
    DESMALEZADO("Retiro de malezas"),
    OBSERVACION("Observación del cultivo"),
    MANTENCION("Mantenimiento"),
    REVISION_SUELO("Revisión del suelo"),
    REGISTRO_CRECIMIENTO("Registro del crecimiento"),
    COSECHA("Cosecha"),
    OTRO("Otra actividad autorizada")
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

/** Tipos de problema que se pueden reportar (caso, sección 3.2). */
enum class ProblemType(val label: String) {
    FALTA_AGUA("Falta de agua"),
    DANO_PLANTA("Daño en la planta"),
    PLAGA("Plaga simulada"),
    DETERIORO_SECTOR("Deterioro del sector"),
    MANTENIMIENTO("Problema de mantenimiento"),
    OTRO("Otro problema observado")
}

/** Seguimiento de un reporte de problema. */
enum class ReportStatus(val label: String) {
    INFORMADO("Informado"),
    EN_REVISION("En revisión"),
    ATENDIDO("Atendido"),
    CERRADO("Cerrado");

    /** Reportes que el docente todavía debe revisar o resolver. */
    val isOpen: Boolean get() = this == INFORMADO || this == EN_REVISION
}

/** Tipo de registro del historial, para mostrar un ícono y permitir filtrar. */
enum class ActivityKind {
    GENERAL, SECTOR, CULTIVO, TAREA, ENTREGA, VALIDACION, RECHAZO, PROBLEMA, MANTENIMIENTO
}
