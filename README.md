# Touchline — Women's Football Community

A runnable first full-stack slice of the project: React + TypeScript web app, Spring Boot REST API, and PostgreSQL. Fans can create/browse communities, join or leave, publish posts, and comment. New communities are seeded automatically on the first web visit.

> **Prototype identity:** there is no registration or login yet. The app uses a display name stored in this browser, and the API trusts the submitted name. Do not expose this prototype to the public internet.

## Run everything with Docker Compose

Install Docker with the Compose plugin, then from the repository root:

```sh
docker compose up --build
```

Open <http://localhost:3000>. Stop with `Ctrl+C`; run `docker compose down` to stop containers. PostgreSQL data stays in the `community-data` volume. Use `docker compose down -v` only when you want to delete that local database.

The REST API runs at port `8080` inside Compose and is proxied through the web app. Open API documentation at <http://localhost:3000/swagger-ui.html>.

## Run from IntelliJ or without Docker

Requirements: Java 17+ and Node.js 20+ with npm. The Maven Wrapper downloads the project's pinned Maven version the first time you use it. The backend defaults to a local H2 database, so you can run it directly in IntelliJ without starting PostgreSQL.

1. Run `CommunityApplication` in IntelliJ, or start the API with `./mvnw spring-boot:run`. Local data is stored in `./data/wfc`.
2. To use PostgreSQL instead, start a PostgreSQL 17 database and set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. These environment variables override the local H2 settings.
3. In another terminal run the web app: `cd frontend && npm install && npm run dev`
4. Open <http://localhost:5173>. Vite proxies REST requests to `localhost:8080`.

Run API tests with `./mvnw test`; build/type-check the frontend with `cd frontend && npm run build`.

## IntelliJ IDEA setup

Spring Boot is supplied by the dependencies in `pom.xml`; there is no separate Spring installation. To remove unresolved Spring imports and build warnings in the IDE:

1. Open the project `pom.xml` and choose **Add as Maven Project** (or open the Maven tool window and click **Reload All Maven Projects**).
2. In **File → Project Structure → Project**, set the Project SDK and language level to **Java 17**.
3. In **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner**, set the JRE to **Project SDK (17)**. Set the Maven importer JDK to the same SDK if shown.
4. Reload Maven, then run `CommunityApplication` or `./mvnw spring-boot:run`.

If IntelliJ doesn't list Java 17, add `/usr/lib/jvm/java-17-openjdk-amd64` as a JDK. Maven will download the declared Spring Boot libraries on reload; an internet connection is needed the first time.

## Implemented REST API

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/communities` | List communities and member counts |
| `GET` | `/api/communities/{id}` | Get a community |
| `POST` | `/api/communities` | Create a community |
| `POST` | `/api/communities/{id}/memberships` | Join using a display name |
| `DELETE` | `/api/communities/{id}/memberships?displayName=...` | Leave |
| `GET` | `/api/communities/{id}/posts` | List community posts |
| `POST` | `/api/communities/{id}/posts` | Publish as a community member |
| `GET` | `/api/posts/{id}/comments` | List post comments |
| `POST` | `/api/posts/{id}/comments` | Comment as a community member |

Swagger UI documents the API. Request validation, duplicate community/member conflicts, and non-member write rejection are included.

## Next implementation stages

1. **Real accounts:** add user entities, password hashing, registration/login, and authenticated ownership checks. Replace display-name identity before deploying or adding other services.
2. **Notifications:** publish versioned events after successful writes; add a separate consumer/service and notification database. Process events idempotently and expose a paginated REST API.
3. **MCP moderation:** expose recent posts/comments and a pending-proposal tool. Keep analysis advisory and require a human moderator to approve actions.
4. **Production readiness:** use database migrations, secret management, authorization/rate limits, pagination, health checks, and automated integration tests. Then add Kafka and the independent service containers to Compose.

Keep the next feature end-to-end: API contract, persistence, UI, and tests together. Kafka and the LLM workflow are easier to integrate once real identities and the core content flow are stable.
