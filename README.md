# App Defend Backend

Spring Boot backend for an application security platform with:

- JWT-based authentication with refresh/logout flow backed by Redis
- RBAC APIs for users, roles, permissions, and views
- OEM integration APIs for GitLab, GitHub Actions, Jenkins, and Atlassian Bamboo
- Spring JDBC persistence with automatic schema/data bootstrap in PostgreSQL
- Local Docker Compose for PostgreSQL and Redis

## Run locally

1. Start infrastructure:

```bash
docker compose up -d
```

2. Run the application:

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

## Default credentials

- Username: `admin@appdefend.local`
- Password: `Admin@123`

## Key APIs

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/users`
- `POST /api/v1/users`
- `GET /api/v1/rbac/roles`
- `POST /api/v1/rbac/roles`
- `POST /api/v1/rbac/roles/{roleId}/permissions/{permissionId}`
- `GET /api/v1/rbac/permissions`
- `POST /api/v1/rbac/permissions`
- `GET /api/v1/rbac/views`
- `POST /api/v1/rbac/views`
- `GET /api/v1/integrations`
- `POST /api/v1/integrations`
- `POST /api/v1/integrations/{id}/test`

## OpenAPI / Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

To test secured APIs:

1. Call `POST /api/v1/auth/login`
2. Copy the `accessToken`
3. Open Swagger UI
4. Click `Authorize`
5. Paste `Bearer <accessToken>`
6. Test the protected endpoints directly from the UI

## Notes

- Database objects are initialized from `schema.sql` and `data.sql`.
- Redis stores refresh tokens and revoked JWT identifiers.
- OEM credentials are stored in the database for local development. In production, move secrets to a vault or KMS-backed mechanism.
