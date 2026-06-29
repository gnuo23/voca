# Voca

Phase 0 project skeleton with:

- Spring Boot backend
- PostgreSQL database
- Spring Security basic auth
- Swagger/OpenAPI
- Next.js frontend
- Docker Compose

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

- Backend basic auth: `admin` / `admin`
- Database: `voca` / `voca`, database `voca`

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
