package com.duoc.huertify_app.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.huertify_app.model.Tarea

// 1. Pantalla conectada al ViewModel (Stateful)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PantallaPrincipal(viewModel: HuertoViewModel) {
    val tareasAtrasadas by viewModel.tareasAtrasadas.collectAsState()
    PantallaPrincipalContent(tareasAtrasadas = tareasAtrasadas)
}

// 2. Contenido puramente visual (Stateless - Fácil de previsualizar)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipalContent(tareasAtrasadas: List<Tarea>) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Huerto Escolar - Tareas Atrasadas") }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            if (tareasAtrasadas.isEmpty()) {
                Text("No hay tareas atrasadas en el huerto.")
            } else {
                LazyColumn {
                    items(tareasAtrasadas) { tarea ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Tarea: ${tarea.tipo}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Descripción: ${tarea.descripcion}")
                                Text(text = "Fecha: ${tarea.fechaProgramada}")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Vista previa gráfica del celular (Preview)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPantallaPrincipal() {
    val tareasEjemplo = listOf(
        Tarea(
            id = 1,
            tipo = "Riego urgente",
            descripcion = "Regar almácigos de lechuga",
            estudianteId = 1,
            sectorId = 1,
            fechaProgramada = "2026-10-01",
            estado = "Atrasada"
        ),
        Tarea(
            id = 2,
            tipo = "Desmaleza",
            descripcion = "Limpiar sector bancales",
            estudianteId = 2,
            sectorId = 1,
            fechaProgramada = "2026-10-05",
            estado = "Atrasada"
        )
    )
    PantallaPrincipalContent(tareasAtrasadas = tareasEjemplo)
}