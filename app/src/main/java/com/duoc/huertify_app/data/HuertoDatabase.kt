package com.duoc.huertify_app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duoc.huertify_app.model.Cultivo
import com.duoc.huertify_app.model.Evidencia
import com.duoc.huertify_app.model.ReporteProblema
import com.duoc.huertify_app.model.Sector
import com.duoc.huertify_app.model.Tarea
import com.duoc.huertify_app.model.Usuario
import com.duoc.huertify_app.model.Validacion

// Asegúrate de importar tus modelos (cl.duoc.fotomotion.model.*)

@Database(
    entities = [
        Usuario::class, Sector::class, Cultivo::class,
        Tarea::class, Evidencia::class, Validacion::class, ReporteProblema::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HuertoDatabase : RoomDatabase() {

    abstract fun huertoDao(): HuertoDao

    companion object {
        @Volatile
        private var INSTANCE: HuertoDatabase? = null

        fun getDatabase(context: Context): HuertoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HuertoDatabase::class.java,
                    "huerto_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}