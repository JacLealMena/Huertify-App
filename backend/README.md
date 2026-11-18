# Huertify backend

Minimal Spring Boot REST API for the Android app. Requires Java 21, Maven, and
XAMPP's MySQL/MariaDB service.

## Database setup

1. Start **MySQL** (and Apache if you use phpMyAdmin) in the XAMPP Control Panel.
2. Open phpMyAdmin at `http://localhost/phpmyadmin`.
3. Create a database named `huertify` with collation `utf8mb4_unicode_ci`.
4. The backend creates or updates the tables when it starts.

By default, the backend connects to `localhost:3306` as `root` with an empty
password, which matches a typical XAMPP installation. If your XAMPP credentials
or port differ, set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` before starting it.
For example, `DB_URL` can be `jdbc:mysql://localhost:3307/huertify`.

```powershell
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Run tests with `mvn test`; tests use
an in-memory H2 database and do not require XAMPP.

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
