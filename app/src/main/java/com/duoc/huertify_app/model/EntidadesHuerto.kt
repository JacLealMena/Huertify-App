package com.duoc.huertify_app.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val rol: String
)

@Entity(tableName = "sectores")
data class Sector(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val ubicacion: String,
    val estado: String
)

@Entity(
    tableName = "cultivos",
    foreignKeys = [
        ForeignKey(entity = Sector::class, parentColumns = ["id"], childColumns = ["sectorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["sectorId"])]
)
data class Cultivo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val sectorId: Int,
    val fechaSiembra: String,
    val fechaCosechaEstimada: String,
    val estadoDesarrollo: String
)

@Entity(
    tableName = "tareas",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["estudianteId"]),
        ForeignKey(entity = Sector::class, parentColumns = ["id"], childColumns = ["sectorId"])
    ],
    indices = [Index(value = ["estudianteId"]), Index(value = ["sectorId"])]
)
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipo: String,
    val descripcion: String,
    val estudianteId: Int,
    val sectorId: Int,
    val fechaProgramada: String,
    val estado: String
)

@Entity(
    tableName = "evidencias",
    foreignKeys = [
        ForeignKey(entity = Tarea::class, parentColumns = ["id"], childColumns = ["tareaId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["tareaId"])]
)
data class Evidencia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tareaId: Int,
    val urlFotografiaFicticia: String,
    val fechaSubida: String
)

@Entity(
    tableName = "validaciones",
    foreignKeys = [
        ForeignKey(entity = Tarea::class, parentColumns = ["id"], childColumns = ["tareaId"]),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["administradorId"])
    ],
    indices = [Index(value = ["tareaId"]), Index(value = ["administradorId"])]
)
data class Validacion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tareaId: Int,
    val administradorId: Int,
    val resultado: String,
    val fechaValidacion: String
)

@Entity(
    tableName = "reportes_problema",
    foreignKeys = [
        ForeignKey(entity = Sector::class, parentColumns = ["id"], childColumns = ["sectorId"])
    ],
    indices = [Index(value = ["sectorId"])]
)
data class ReporteProblema(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sectorId: Int,
    val descripcion: String,
    val estado: String
)