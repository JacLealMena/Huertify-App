# 🌱 Huertify

App Android para organizar el huerto escolar entre docentes y estudiantes: qué hay plantado, qué tareas hay que hacer, quién las hace y si quedaron bien hechas.

Este README es para el equipo: explica **qué está hecho**, **cómo correrlo** y **cómo está organizado el código**, para que cualquiera pueda meterse a trabajar sin perderse.

---

## 1. Cómo correr el proyecto

1. Abre la carpeta en **Android Studio** (versión reciente, el proyecto usa AGP 9 y `compileSdk 37`).
2. Espera que Gradle sincronice (la primera vez descarga dependencias, puede tardar).
3. Presiona ▶️ **Ejecutar** (el botón *Run*) con un emulador o un teléfono conectado.

La primera vez que se abre, la app crea la base de datos y carga **datos de ejemplo** (3 sectores, 3 cultivos y 4 tareas), así que no parte vacía.

### Cuentas de prueba

En la pantalla de login aparecen estas cuentas: **si tocas una, se rellenan solas** el correo y la contraseña.

| Persona | Correo | Contraseña | Rol |
|---|---|---|---|
| Prof. Carla Muñoz | `docente@huertify.cl` | `admin123` | Administrador (docente) |
| Ana Torres | `ana@huertify.cl` | `huerto123` | Colaborador (estudiante) |
| Benjamín Rojas | `benjamin@huertify.cl` | `huerto123` | Colaborador (estudiante) |
| Sofía Pérez | `sofia@huertify.cl` | `huerto123` | Colaborador (estudiante) |

> 💡 Benjamín tiene una tarea **atrasada** y Sofía una **en desarrollo**: sirven para mostrar esos casos rápido en una presentación.

---

## 2. Qué hace la app

Hay dos roles, y la app cambia según quién entra.

### 👩‍🏫 Docente (Administrador)
- **Panel** con resumen: sectores, cultivos, tareas, entregas por validar, tareas atrasadas y reportes abiertos.
- **Sectores y cultivos**: crear, editar y eliminar (CRUD completo). Al borrar un sector se borran sus cultivos (pide confirmación).
- **Tareas**: crear, asignar a un estudiante, editar (solo si nadie la ha empezado) y **filtrar** por estado, estudiante, atrasadas o texto.
- **Revisiones**: ver lo que entregó el estudiante (comentario + foto) y **validar** o **rechazar**. Para rechazar el motivo es obligatorio.
- **Problemas del huerto**: ver los reportes y marcarlos como resueltos.
- **Historial** de todo lo que pasó en la app y **avisos** (campana 🔔).

### 🧑‍🎓 Estudiante (Colaborador)
- Ve **solo sus tareas**; no puede entrar a las secciones del docente.
- Lee las instrucciones, **inicia** la tarea y la **entrega** con un comentario y una **foto** (cámara o galería).
- Si se la rechazan, ve el motivo y puede **corregirla y volver a entregarla**.
- Puede **reportar problemas** del huerto (plaga, manguera rota, etc.) y ver su propio historial.

### 🌦️ Clima (para todos)
En el panel aparece el pronóstico de 3 días con una **recomendación de riego** (ej: "Se esperan 4.9 mm de lluvia: probablemente no sea necesario regar hoy"). Viene de internet usando **Retrofit**; más detalle en la sección 5.

---

## 3. El ciclo de vida de una tarea

Esto es lo más importante del negocio, y la app **no deja saltarse pasos**:

```
 Pendiente ──(estudiante inicia)──▶ En desarrollo ──(estudiante entrega)──▶ Pendiente de validación
                                          ▲                                        │
                                          │                          (docente revisa)
                                          │                         ┌──────┴──────┐
                                          └──(estudiante corrige)── Rechazada    Validada ✅
```

- Solo el **estudiante asignado** puede iniciar y entregar.
- Solo el **docente** puede validar o rechazar.
- Una tarea **atrasada** es la que pasó su fecha y sigue sin entregarse; se marca en rojo y genera un aviso.

Estas reglas viven en `domain/MaquinaEstadosTarea.kt`. Si el profe dice que una rechazada **no** se puede rehacer, basta con quitar una línea ahí.

---

## 4. Cómo está organizado el código (MVVM)

Usamos **MVVM**: la pantalla solo *muestra*, el ViewModel *decide* y el repositorio *guarda o trae* datos.

```
app/src/main/java/com/duoc/huertify_app/
│
├── AplicacionHuertify.kt      Arranca la app y crea las dependencias
├── ActividadPrincipal.kt      Única Activity; muestra la navegación (y fija el idioma español)
│
├── domain/            ← Reglas del negocio (Kotlin puro, sin Android)
│   ├── model/Enumeraciones.kt       Roles, estados y tipos de tarea, gravedad
│   ├── validation/Validadores.kt    TODAS las validaciones de formularios
│   ├── MaquinaEstadosTarea.kt       Qué cambios de estado están permitidos
│   ├── Alertas.kt                   Genera los avisos (atrasadas, por validar…)
│   └── Clima.kt                     Recomendación de riego según el clima
│
├── data/              ← De dónde salen los datos
│   ├── local/Entidades.kt           Room: las tablas
│   ├── local/AccesoDatos.kt         Room: las consultas (DAOs)
│   ├── local/BaseDatosHuertify.kt   Room: la base de datos
│   ├── remote/ApiClima.kt           Retrofit: la API del clima
│   ├── repository/Repositorios.kt   Repositorios: única puerta de entrada a los datos
│   ├── repository/RepositorioClima.kt
│   ├── DatosIniciales.kt            Cuentas y datos de ejemplo
│   └── AlmacenImagenes.kt           Guarda las fotos de evidencia
│
├── di/ContenedorDependencias.kt  ← Crea los repositorios una sola vez (inyección manual)
│
└── ui/                ← Lo que se ve
    ├── navigation/NavegacionHuertify.kt   Rutas y control de acceso por rol
    ├── common/                 Piezas reutilizables: ModeloVistaBase.kt, Componentes.kt, SelectorFoto.kt
    ├── screens/                Una carpeta por sección; cada archivo Pantalla*.kt trae su ViewModel
    └── theme/                  Colores.kt, Tema.kt, Tipografia.kt
```

> 📝 **Nombres:** los **archivos** están en español para ubicarlos fácil, pero las **clases y funciones** dentro del código están en inglés (`TaskRepository`, `saveSector()`…). Es la convención habitual en Android, y además así combinan con lo que ya viene en inglés desde Android, Room y Retrofit (`ViewModel`, `@Query`, `@GET`).

### Ejemplo: ¿qué pasa cuando un estudiante entrega una tarea?

1. **Pantalla** (`TaskDetailScreen`, en `PantallaDetalleTarea.kt`): el estudiante toca "Entregar". La pantalla solo llama a `vm.submit()`.
2. **ViewModel** (`TaskDetailViewModel`, en el mismo archivo): valida que haya comentario (mínimo 10 caracteres) y foto. Si falta algo, pone el error en el estado y la pantalla lo pinta en rojo debajo del campo.
3. **Repositorio** (`TaskRepository.submitTask`, en `Repositorios.kt`): revisa que la tarea sea de ese estudiante y que el cambio de estado esté permitido, guarda en Room y anota en el historial.
4. **Room** avisa que los datos cambiaron, y todas las pantallas que muestran esa tarea se actualizan solas (usamos `Flow`).

> 📌 Regla del equipo: **nada de lógica en los Composables**. Si te ves escribiendo un `if` de validación dentro de una pantalla, eso va en el ViewModel o en `Validadores.kt`.

---

## 5. Retrofit (consumo de API REST)

El curso pide Retrofit, así que lo usamos para algo útil en un huerto: el **clima**.

- **API**: [Open-Meteo](https://open-meteo.com/), gratis y **sin API key** (no hay que configurar nada).
- `data/remote/ApiClima.kt`: la interfaz con `@GET` y `@Query`, los modelos de la respuesta JSON (DTOs) y el cliente Retrofit con conversor Gson.
- `data/repository/RepositorioClima.kt`: llama a la API, convierte la respuesta a nuestros modelos y transforma los errores en mensajes entendibles ("Sin conexión a Internet…").
- La ubicación está en `di/ContenedorDependencias.kt` (ahora Santiago). Si quieren poner la del colegio, se cambia ahí.

Si no hay internet, la tarjeta muestra el error y un botón para reintentar; el resto de la app sigue funcionando normal, porque todo lo demás está guardado en el teléfono.

---

## 6. Validaciones y mensajes

Todos los formularios validan antes de guardar; si algo está mal, **no se envía**. Cada error aparece **debajo del campo**, con un ícono ❗ y un Snackbar de aviso. Si se guarda bien, sale un Toast de confirmación.

Algunos ejemplos de reglas:

| Formulario | Regla |
|---|---|
| Login | correo con formato válido, contraseña de 6+ caracteres |
| Sector | nombre único (sin importar mayúsculas), superficie mayor a 0 (acepta `12,5` o `12.5`) |
| Cultivo | **la cosecha debe ser posterior a la siembra**, riego entre 1 y 60 días |
| Tarea | responsable, tipo, descripción y fecha obligatorios; la fecha no puede estar en el pasado |
| Entrega | comentario y foto obligatorios |
| Rechazo | motivo obligatorio (mínimo 10 caracteres) |

---

## 7. Pruebas

Hay **23 pruebas automáticas**, todas pasando.

```bash
# Pruebas rápidas, no necesitan emulador (13)
./gradlew testDebugUnitTest

# Pruebas en dispositivo, necesitan un emulador abierto (10)
./gradlew connectedDebugAndroidTest
```

- **Unitarias** (`app/src/test`): validaciones, ciclo de vida de tareas y la capa de Retrofit con una API falsa.
- **En dispositivo** (`app/src/androidTest`): Room con una base de datos real en memoria (permisos por rol, ciclo completo de tareas, borrado en cascada…) y la pantalla de login.

Además, todos los flujos principales se probaron a mano en el emulador.

---

## 8. Cosas a tener en cuenta

- **Las contraseñas** se guardan con hash SHA-256. Es suficiente para un MVP, pero no es lo que se usaría en una app real.
- **El selector de fechas** sale en el idioma del teléfono (en un emulador en inglés aparece en inglés).
- **Si cambian las tablas** (`Entidades.kt`), la base de datos se borra y se recrea con los datos de ejemplo, porque está configurada así para desarrollo. Ojo si tenían datos de prueba propios.
- **Stack**: Kotlin · Jetpack Compose · Material 3 · MVVM · Room 3 · Retrofit · Navigation Compose · Coil (imágenes).

¿Dudas? Lo más rápido es abrir `ui/navigation/NavegacionHuertify.kt`, que tiene el mapa de todas las pantallas, y seguir desde ahí. 🌿
