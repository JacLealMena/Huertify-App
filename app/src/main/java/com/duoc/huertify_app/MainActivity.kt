package com.duoc.huertify_app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.duoc.huertify_app.data.HuertoDatabase
import com.duoc.huertify_app.data.HuertoRepository
import com.duoc.huertify_app.model.Sector
import com.duoc.huertify_app.model.Tarea
import com.duoc.huertify_app.model.Usuario
import com.duoc.huertify_app.ui.HuertoViewModel
import com.duoc.huertify_app.ui.PantallaPrincipal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar BD y Repositorio
        val database = HuertoDatabase.getDatabase(applicationContext)
        val repository = HuertoRepository(database.huertoDao())

        // 2. Usuario ficticio de prueba
        val usuarioPrueba = Usuario(id = 1, nombre = "Docente Pruebas", rol = "Administrador")

        // 3. Instanciar ViewModel
        val viewModel = HuertoViewModel(repository, usuarioPrueba)

        // 4. Mostrar la UI
        setContent {
            PantallaPrincipal(viewModel = viewModel)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Insertamos el usuario PRIMERO para respetar la llave foránea
            repository.insertarUsuario(usuarioPrueba)

            // 2. Insertamos el sector
            repository.insertarSector(
                Sector(
                    id = 1,
                    nombre = "Sector Invernadero",
                    ubicacion = "Zona Norte",
                    estado = "En uso"
                )
            )

            // 3. Ahora sí podemos insertar la tarea sin que la BD explote
            repository.insertarTarea(
                Tarea(
                    id = 1,
                    tipo = "Riego urgente",
                    descripcion = "Regar almácigos de lechuga",
                    estudianteId = 1, // Ahora sí existe
                    sectorId = 1,     // Ahora sí existe
                    fechaProgramada = "2026-10-01",
                    estado = "Atrasada"
                )
            )
        }
    }
}