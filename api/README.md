# managify-api

Spring Boot REST API for Managify.

## Stack

Java 17 · Spring Boot 3.4.1 · Maven wrapper · MySQL 8 · Flyway · Keycloak JWT (resource server) · springdoc OpenAPI · Lombok · MapStruct · Testcontainers

## Run locally

```powershell
docker compose -f docker-compose.local.yml up -d   # MySQL on localhost:3306, phpMyAdmin on localhost:8081
.\mvnw.cmd spring-boot:run                          # dev profile, port 8080
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Ping: http://localhost:8080/public/ping

Every other path needs a Keycloak bearer token. Set `KEYCLOAK_URL` and `KEYCLOAK_REALM`
(defaults: `http://localhost:8180`, `managify`). The issuer is only contacted on the first
authenticated request, so the app starts without Keycloak.

## Profiles

| Profile   | How                                            | Config                              |
|-----------|------------------------------------------------|-------------------------------------|
| `dev`     | default Maven profile                          | `application-dev.properties`        |
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

## Conventions

- Every response is an `ApiResult<T>`; paged lists use `PagedResponse<T>` (page size `app.page-size`).
- Throw `AppException` through its factories (`notFound`, `badRequest`, `conflict` ...); add codes to `ErrorCode`.
- Schema changes go in a new Flyway migration; never edit an applied one. Hibernate only validates (`ddl-auto=validate`).
- Secrets come from environment variables, never properties files.
