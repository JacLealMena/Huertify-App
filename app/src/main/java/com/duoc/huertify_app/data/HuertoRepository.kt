package com.duoc.huertify_app.data

import com.duoc.huertify_app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HuertoRepository(private val huertoDao: HuertoDao) {

    // --- Consultas (Lectura) ---
    fun obtenerCultivosPorSector(sectorId: Int): Flow<List<Cultivo>> {
        return huertoDao.obtenerCultivosPorSector(sectorId)
    }

    fun obtenerTareasAtrasadas(fechaActual: String): Flow<List<Tarea>> {
        return huertoDao.obtenerTareasAtrasadas(fechaActual)
    }

    // --- Operaciones en Hilo Secundario (Escritura) ---
    suspend fun insertarUsuario(usuario: Usuario) = withContext(Dispatchers.IO) { huertoDao.insertarUsuario(usuario) }

    suspend fun insertarSector(sector: Sector) = withContext(Dispatchers.IO) { huertoDao.insertarSector(sector) }

    suspend fun insertarCultivo(cultivo: Cultivo) = withContext(Dispatchers.IO) { huertoDao.insertarCultivo(cultivo) }

    suspend fun insertarTarea(tarea: Tarea) = withContext(Dispatchers.IO) { huertoDao.insertarTarea(tarea) }

    suspend fun insertarValidacion(validacion: Validacion) = withContext(Dispatchers.IO) { huertoDao.insertarValidacion(validacion) }

    suspend fun insertarReporte(reporte: ReporteProblema) = withContext(Dispatchers.IO) { huertoDao.insertarReporte(reporte) }

    suspend fun actualizarTarea(tarea: Tarea) = withContext(Dispatchers.IO) { huertoDao.actualizarTarea(tarea) }

    suspend fun actualizarReporte(reporte: ReporteProblema) = withContext(Dispatchers.IO) { huertoDao.actualizarReporte(reporte) }
}