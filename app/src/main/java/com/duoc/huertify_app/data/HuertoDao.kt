package com.duoc.huertify_app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.duoc.huertify_app.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HuertoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarUsuario(usuario: Usuario) // ¡Sin la palabra suspend!

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarSector(sector: Sector)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarCultivo(cultivo: Cultivo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarTarea(tarea: Tarea)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarValidacion(validacion: Validacion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarReporte(reporte: ReporteProblema)

    @Update
    fun actualizarTarea(tarea: Tarea)

    @Update
    fun actualizarReporte(reporte: ReporteProblema)

    @Query("SELECT * FROM cultivos WHERE sectorId = :sectorId")
    fun obtenerCultivosPorSector(sectorId: Int): Flow<List<Cultivo>>

    @Query("SELECT * FROM tareas WHERE estado = 'Atrasada' OR (estado = 'Pendiente' AND fechaProgramada < :fechaActual)")
    fun obtenerTareasAtrasadas(fechaActual: String): Flow<List<Tarea>>
}