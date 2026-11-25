# Women's Football Community

A full-stack community platform for supporters of women's football. Fans can create communities for clubs and teams, join those communities, publish posts, and discuss posts through comments.

This is an incremental project implementation. The current version is a working web application and Community Service; notifications, Kafka, the MCP server, and AI moderation are planned components, not implemented features yet.

## What is implemented

- Account registration, login, current-user lookup, and logout.
- Passwords stored as BCrypt hashes, not plain text.
- Server-side authenticated sessions with CSRF protection for write requests.
- Browse and create club/team communities.
- Join and leave communities.
- Publish posts and add comments as a community member.
- Author identity is taken from the signed-in account, not from a name supplied in a post or comment request.
- PostgreSQL support for Docker Compose and persistent local H2 storage for development.
- OpenAPI documentation and backend integration tests.

## Technologies

| Technology | How it is used |
|---|---|
| Java 17 | Backend programming language. |
| Spring Boot 3.4.5 | Runs the Community Service and exposes its REST API. |
| Spring Web | Implements HTTP endpoints for accounts, communities, posts, and comments. |
| Spring Security | Handles authentication, server sessions, access rules, and CSRF protection. |
| Spring Data JPA / Hibernate | Maps Java entities to database tables and implements persistence. |
| BCrypt | Hashes passwords before they are stored. |
| H2 | Persistent file-based database for local development; no separate database server is needed. |
| PostgreSQL 17 | Database used by the Docker Compose deployment. |
| React 19 | Builds the interactive web interface. |
| TypeScript | Adds typed API models and checks the frontend at build time. |
| Vite | Runs the frontend development server and builds production assets. |
| Nginx | Serves the built frontend in Docker and proxies API requests to the backend. |
| Maven Wrapper | Downloads and runs the project-pinned Maven version with `./mvnw`. |
| Springdoc OpenAPI | Generates the REST API specification and Swagger UI. |
| Docker Compose | Starts the frontend, backend, and PostgreSQL together. |

Kafka, the separate Notification Service/database, Spring AI MCP, and an LLM-powered moderation agent are part of the proposal's next stages. They are not currently present in the running application.

## How the application works

The browser runs the React application. It sends HTTP requests to the Spring Boot Community Service. When running locally, Vite forwards `/api` requests from port `5173` to the backend on port `8080`. In Docker, Nginx serves the frontend on port `3000` and proxies API requests to the backend container.

The Community Service is the source of truth for accounts, communities, memberships, posts, and comments. Spring Data JPA persists these records in the configured database. The main relationships are:

- A user can join many communities.
- A community can have many members and posts.
- A post belongs to a community and has an authenticated author.
- A comment belongs to a post and has an authenticated author.

### Registration and sign-in

1. The browser requests a CSRF token from `GET /api/auth/csrf`.
2. Registration or login is sent to the API with the token in the `X-CSRF-TOKEN` header.
3. On registration, the service normalizes the email, validates the submitted values, hashes the password with BCrypt, and stores the account.
4. On successful registration or login, Spring Security creates an authenticated server-side session. The browser keeps the session cookie.
5. The frontend calls `GET /api/auth/me` to restore the current account after a page refresh.

Every write request includes the CSRF token. Spring Security protects the session cookie and requires authentication for writes. When a user joins a community, the membership record links that user's account ID to the community. Posts and comments similarly use the account from the session as their author, so changing a request body cannot impersonate another member.

### Current REST endpoints

| Method | Endpoint | Access |
|---|---|---|
| `GET` | `/api/auth/csrf` | Public; initializes a session and returns a CSRF token. |
| `POST` | `/api/auth/register` | Public; creates an account and signs it in. |
| `POST` | `/api/auth/login` | Public; signs in to an existing account. |
| `GET` | `/api/auth/me` | Signed-in user. |
| `POST` | `/api/auth/logout` | Signed-in user; requires CSRF token. |
| `GET` | `/api/communities` | Public; lists communities and member counts. |
| `GET` | `/api/communities/{id}` | Public; retrieves a community. |
| `POST` | `/api/communities` | Signed-in user; creates a community. |
| `GET` | `/api/communities/memberships/mine` | Signed-in user; lists their community IDs. |
| `POST` | `/api/communities/{id}/memberships` | Signed-in user; joins a community. |
| `DELETE` | `/api/communities/{id}/memberships` | Signed-in user; leaves a community. |
| `GET` | `/api/communities/{id}/posts` | Public; lists posts in a community. |
| `POST` | `/api/communities/{id}/posts` | Signed-in community member; creates a post. |
| `GET` | `/api/posts/{id}/comments` | Public; lists comments on a post. |
| `POST` | `/api/posts/{id}/comments` | Signed-in community member; creates a comment. |

Swagger UI is available at `/swagger-ui.html` when the backend is running.

## Run locally

Requirements: Java 17+ and Node.js 20+ with npm. The application defaults to a persistent H2 database at `./data/wfc`; PostgreSQL or Docker is not required for local development.

1. In a terminal, start the backend from the repository root:

   ```sh
   ./mvnw spring-boot:run
   ```

   Alternatively, run `CommunityApplication` from IntelliJ IDEA. Run only one backend instance at a time: the local H2 database file is locked while the application is running.

2. In another terminal, start the frontend:

   ```sh
   cd frontend
   npm install
   npm run dev
   ```

3. Open <http://localhost:5173> and register an account.

The API is available at <http://localhost:8080>; API documentation is at <http://localhost:8080/swagger-ui.html>. Keep both development servers running while using the app.

Run backend tests with:

```sh
./mvnw test
```

Check and build the frontend with:

```sh
cd frontend
npm run build
```

To use PostgreSQL locally, start a PostgreSQL database and set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` before starting Spring Boot.

## Run with Docker Compose

Install Docker Engine and the Docker Compose plugin, then run from the project root:

```sh
docker compose up --build
```

Open <http://localhost:3000>. Compose starts the React frontend, Spring Boot Community Service, and PostgreSQL. PostgreSQL data is kept in the `community-data` volume. Stop the containers with `Ctrl+C` or `docker compose down`.

## Project layout

```text
src/main/java/com/wfc/community/
  auth/        Registration, login, sessions, and security configuration
  community/  Community and membership entities, API, and persistence
  post/        Post and comment entities, API, and persistence
src/main/resources/  Spring configuration for local H2 and environment-based database access
src/test/             Backend API integration tests using an in-memory H2 database
frontend/src/          React UI, typed API client, and styles
compose.yaml           Local multi-container deployment
Dockerfile             Backend container build
frontend/Dockerfile    Frontend build and Nginx container
```

## Planned next stages

1. Publish versioned domain events from the Community Service and add Kafka.
2. Build an independent Notification Service with its own PostgreSQL database and REST API.
3. Add an MCP server with tools to read recent posts/comments and create pending moderation proposals.
4. Integrate an AI moderator that proposes actions for human review; it must not automatically delete content.
5. Improve production readiness with database migrations, deployment secrets, rate limits, and additional operational checks.
