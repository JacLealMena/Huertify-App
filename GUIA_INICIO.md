# Guía para iniciar Huertify

Esta guía explica cómo levantar el backend Java y la app Android sin Docker. El backend usa una base local por defecto; XAMPP/MySQL es opcional.

## Requisitos

- JDK 21 (no hace falta instalar Maven: el proyecto incluye Maven Wrapper).
- XAMPP con MySQL/MariaDB (opcional, si prefieres usarlo en vez de la base local).
- Android Studio y Android SDK API 37.
- Un emulador Android o un teléfono conectado.

Comprueba que Java 21 esté disponible:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -version
```

En macOS/Linux puedes comprobarlo con `java -version`. Si `JAVA_HOME` está definido, debe apuntar al JDK 21.

## 1. Preparar la base de datos

No necesitas instalar ni iniciar MySQL para probar la app. Por defecto, Spring Boot guarda los datos en `backend/data/huertify` y crea las tablas automáticamente.

Para usar XAMPP/MySQL en vez de la base local:

1. Inicia **MySQL** en XAMPP (Apache solo hace falta para phpMyAdmin).
2. Crea una base de datos llamada `huertify` en `http://localhost/phpmyadmin`.
3. En la terminal desde la que iniciarás el backend, configura:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/huertify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "tu_contraseña"
```

Si `root` no tiene contraseña, deja `DB_PASSWORD` vacío como en el ejemplo.

## 2. Iniciar el backend

Desde la carpeta raíz del proyecto, inicia el backend. La primera vez hace falta internet para que el wrapper descargue Maven y las dependencias.

```powershell
# Windows
cd backend
.\mvnw.cmd spring-boot:run
```

```bash
# macOS / Linux
cd backend
./mvnw spring-boot:run
```

Espera a que la consola indique que Tomcat inició en el puerto `8080`. Puedes comprobar que responde abriendo `http://localhost:8080/api/users`. **Deja esta terminal abierta** mientras usas la app.

La primera vez, el backend carga datos de ejemplo. Puedes iniciar sesión con cualquiera de estas cuentas:

| Rol | Correo | Contraseña |
|---|---|---|
| Docente | `docente@huertify.cl` | `admin123` |
| Estudiante | `ana@huertify.cl` | `huerto123` |
| Estudiante | `benjamin@huertify.cl` | `huerto123` |
| Estudiante | `sofia@huertify.cl` | `huerto123` |

## 3. Iniciar la app Android

1. Abre en Android Studio la **carpeta raíz** del repositorio, donde está `settings.gradle.kts` (no abras solo `app/`).
2. Espera que termine la sincronización de Gradle. Si Android Studio lo solicita, instala Android SDK API 37.
3. Enciende un emulador desde **Device Manager**.
4. Confirma que el backend siga activo y presiona **Run** con la configuración `app`.

En el emulador, la app usa `http://10.0.2.2:8080/` para conectarse al backend que corre en el computador. Al iniciar, carga desde el backend las cuentas, sectores, cultivos y tareas.

## Usar un teléfono físico

Conecta el teléfono y el computador a la misma red Wi-Fi. Cambia `HUERTIFY_BASE_URL` en [app/build.gradle.kts](./app/build.gradle.kts) a la dirección IPv4 local del computador, por ejemplo `http://192.168.1.25:8080/`. En Windows puedes encontrar la dirección con `ipconfig`; en macOS/Linux, consulta la configuración de red o ejecuta `ip addr`. Permite el acceso al puerto `8080` en el firewall del computador si lo solicita.

## Problemas comunes

| Mensaje o problema | Qué revisar |
|---|---|
| Maven Wrapper no puede iniciar | Comprueba que Java 21 esté instalado y que haya conexión a internet la primera vez. |
| La app no se conecta al backend | Que la terminal donde iniciaste el backend siga abierta. En emulador usa `10.0.2.2`, no `localhost`. |
| `Communications link failure` al iniciar | Si estás usando MySQL, inicia MySQL en XAMPP. También puedes quitar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` para usar la base local predeterminada. |
| `Access denied for user` | Revisa `DB_USERNAME` y `DB_PASSWORD` en la terminal del backend. |
| `Unknown database 'huertify'` | Si elegiste MySQL, crea la base `huertify` desde phpMyAdmin antes de iniciar Spring Boot. |
| El puerto `8080` ya está ocupado | Detén el otro proceso o configura el mismo puerto nuevo en el backend y en `HUERTIFY_BASE_URL`. |
| Android Studio no encuentra el SDK | Abre la raíz del proyecto en Android Studio e instala Android SDK API 37 desde SDK Manager. |

## Detener el proyecto

- En la terminal del backend, presiona `Ctrl+C`.
- Si usaste XAMPP, cuando termines puedes detener MySQL y Apache desde XAMPP Control Panel.
- Para detener el emulador, ciérralo desde Device Manager.

## Pruebas

Desde la carpeta `backend/`:

```powershell
# Windows
.\mvnw.cmd test
```

```bash
# macOS / Linux
./mvnw test
```

Las pruebas del backend usan una base H2 temporal; no necesitan iniciar XAMPP.
