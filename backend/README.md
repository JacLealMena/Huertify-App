# Huertify backend

Minimal Spring Boot REST API for the Android app. Requires Java 21; the included
Maven Wrapper downloads and runs Maven, so Maven does not need to be installed.

## Database setup

By default, the backend uses a local H2 database at `backend/data/huertify` and
creates or updates its tables when it starts. No separate database service is
needed, and data persists between runs.

To use XAMPP MySQL/MariaDB instead, start MySQL, create a database named
`huertify`, then set the connection variables before starting the backend:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/huertify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = ""
cd backend
.\mvnw.cmd spring-boot:run
```

For a password-protected MySQL user, set `DB_PASSWORD` to its password.

On macOS/Linux, use `./mvnw spring-boot:run` instead of `.\mvnw.cmd spring-boot:run`.
The first run needs internet to download Maven and dependencies. The API listens
on `http://localhost:8080`. Run tests with `.\mvnw.cmd test` on Windows or
`./mvnw test` on macOS/Linux; tests use an in-memory H2 database.

On its first run, the backend seeds four demo accounts, sectors, crops, and tasks:

| Email | Password | Role |
|---|---|---|
| docente@huertify.cl | admin123 | ADMIN |
| ana@huertify.cl | huerto123 | STUDENT |
| benjamin@huertify.cl | huerto123 | STUDENT |
| sofia@huertify.cl | huerto123 | STUDENT |

Endpoints: `POST /api/auth/login`, `GET /api/users`; `GET/POST /api/sectors`,
`PUT/DELETE /api/sectors/{id}`; `GET/POST /api/crops` (optional `sectorId`),
`PUT/DELETE /api/crops/{id}`; and `GET/POST /api/tasks` (optional `assigneeId`),
`PUT/DELETE /api/tasks/{id}`. Login accepts `{"email":"...","password":"..."}`;
dates are numeric epoch-day values and timestamps are epoch milliseconds, as in
the Android models. Deletes return 204 and unknown item IDs return 404.

Development CORS is permissive. This is a local academic prototype; its CRUD
endpoints do not have production authentication or authorization.
