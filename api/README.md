# managify-api

Spring Boot REST API for Managify.

## Stack

Java 17 · Spring Boot 3.4.1 · Maven wrapper · MySQL 8 · Flyway · Keycloak JWT (resource server) · springdoc OpenAPI · Lombok · MapStruct · Testcontainers

## Run locally

```powershell
docker compose -f docker-compose.local.yml up -d   # MySQL :3306, phpMyAdmin :8081, Redis :6379
.\mvnw.cmd spring-boot:run                          # dev profile, port 8080
```

In Git Bash, use `./mvnw` instead of `.\mvnw.cmd` (e.g. `./mvnw spring-boot:run`).

**Hot restart (dev only):** the `dev` Maven profile adds `spring-boot-devtools`, which restarts the app
when compiled classes change (save in the IDE, or run `mvnw compile` in another terminal).
It is not on the classpath with `-Pstaging` / `-Pprod`, and is never included in a packaged jar or the Docker image.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Ping: http://localhost:8080/public/ping

Every other path needs a Keycloak bearer token. Set `KEYCLOAK_URL` and `KEYCLOAK_REALM`
(defaults: `http://localhost:8180`, `managify`). The issuer is only contacted on the first
authenticated request, so the app starts without Keycloak.

## Profiles

| Profile   | How                                            | Config                              |
|-----------|------------------------------------------------|-------------------------------------|
| `dev`     | default Maven profile (includes devtools)      | `application-dev.properties`        |
| `staging` | `-Pstaging` or `SPRING_PROFILES_ACTIVE=staging`| env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `BASE_URL`, `CORS_ALLOWED_ORIGINS` |
| `prod`    | `-Pprod` or `SPRING_PROFILES_ACTIVE=prod`      | same as staging                     |

## Tests

```powershell
.\mvnw.cmd test
```

Integration tests extend `support/IntegrationTestBase` and run against MySQL in Testcontainers (Docker must be running).

## Layout

```
src/main/java/com/managify
├── config/            Swagger; config/auth: security, Keycloak role mapping, Roles
├── controller/        REST controllers (return ApiResult<T>)
├── service/           Service interfaces; service/impl: implementations
├── repository/        Spring Data JPA repositories
├── model/             JPA entities (extend FullyAuditableEntity)
├── dto/request|response
├── enums/
├── client/            Calls to other services
├── exception/         ErrorCode, AppException, GlobalExceptionHandler
└── utils/             Helpers; utils/mapper: MapStruct mappers
src/main/resources/db/migration   Flyway migrations (V<n>__<what>.sql)
```

## Project management module (`com.managify.projects`)

Projects (name, start/end date, status) with a **tracker** of tasks and their tasklets, and **workers** assigned to either.

- Tasks and tasklets share the `tracker_items` table: no parent = task, parent = tasklet. Only two levels.
- A task with dated tasklets spans the earliest tasklet start to the latest tasklet end (`datesDerived: true`).
- Every tracker write returns the updated tree.

| Action | Endpoint |
|---|---|
| Tracker tree (cached) | `GET /api/v1/projects/{id}/tracker` |
| Add task / tasklet | `POST /api/v1/projects/{pid}/tasks`, `POST .../tasks/{taskId}/tasklets` |
| Edit / delete | `PATCH`, `DELETE .../items/{id}` (deleting a task deletes its tasklets) |
| Reorder | `POST .../items/{id}/move-up`, `/move-down` |
| Task → tasklet, or move a tasklet | `POST .../items/{id}/make-tasklet` `{ "parentTaskId": 1 }` (409 if the task still has tasklets) |
| Tasklet → task | `POST .../items/{id}/make-task` |
| Assign / unassign | `PUT` / `DELETE .../items/{id}/assignee` `{ "workerId": 1 }` |
| Projects, workers | `/api/v1/projects`, `/api/v1/workers`, `GET /api/v1/workers/{id}/items` (cached) |

**Caching (Redis):** `projectTracker`, `project`, `worker` and `workerItems` hold response DTOs under
`managify::<cache>::<id>`. Writes evict the affected entries after the transaction commits
(`ProjectCacheEvictor`); TTLs are in `app.cache.ttl.*`. If Redis is down, the API serves straight from MySQL.

```powershell
docker exec -it managify-redis redis-cli -a managify keys "managify::*"
```

## Conventions

- Every response is an `ApiResult<T>`; paged lists use `PagedResponse<T>` (page size `app.page-size`).
- Throw `AppException` through its factories (`notFound`, `badRequest`, `conflict` ...); add codes to `ErrorCode`.
- Schema changes go in a new Flyway migration; never edit an applied one. Hibernate only validates (`ddl-auto=validate`).
- Secrets come from environment variables, never properties files.
