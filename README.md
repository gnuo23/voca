# Voca

Phase 0 project skeleton with:

- Spring Boot backend
- PostgreSQL database
- Spring Security basic auth
- Swagger/OpenAPI
- Next.js frontend
- Docker Compose
- JWT auth with user profile
- Vocabulary deck CRUD with ownership checks

## Run With Docker Compose

```bash
docker compose up -d --build
```

Services:

- Frontend: http://localhost:3000
- Backend health: http://localhost:8080/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5432

Default credentials:

- Database: `voca` / `voca`, database `voca`

Auth endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `PUT /api/users/me`

Deck endpoints:

- `POST /api/decks`
- `GET /api/decks`
- `GET /api/decks/{deckId}`
- `PUT /api/decks/{deckId}`
- `DELETE /api/decks/{deckId}`

Frontend routes:

- `/decks`
- `/decks/new`
- `/decks/{deckId}`

## Run Locally

Start PostgreSQL first:

```bash
docker compose up -d db
```

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## Verification

```bash
cd backend && mvn test
cd frontend && npm run build
```
