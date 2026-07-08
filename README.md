# Voca

Phase 0 project skeleton with:

- Spring Boot backend
- PostgreSQL database
- Spring Security basic auth
- Swagger/OpenAPI
- Next.js frontend
- Docker Compose
- Android native API client app
- JWT auth with user profile
- Vocabulary deck CRUD with ownership checks
- Vocabulary list import preview and confirm
- Vocabulary item CRUD and flashcard study progress
- AI vocabulary enrichment jobs with batch processing

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

Vocabulary import endpoints:

- `POST /api/vocab/import/preview`
- `POST /api/vocab/import/confirm`

Vocabulary item endpoints:

- `GET /api/decks/{deckId}/vocab`
- `GET /api/vocab/{vocabId}`
- `PUT /api/vocab/{vocabId}`
- `DELETE /api/vocab/{vocabId}`
- `POST /api/vocab/{vocabId}/mark`

AI enrichment endpoints:

- `POST /api/vocab/{vocabId}/enrich`
- `POST /api/decks/{deckId}/enrich`
- `GET /api/enrich/jobs/{jobId}`

Frontend routes:

- `/decks`
- `/decks/new`
- `/decks/{deckId}` with vocabulary list, edit/delete actions, import, and flashcard study mode

AI configuration:

- Default provider: `APP_AI_PROVIDER=local`
- OpenAI provider: `APP_AI_PROVIDER=openai`, `OPENAI_API_KEY=...`, optional `OPENAI_MODEL=...`
- Gemini provider: `APP_AI_PROVIDER=gemini`, `GEMINI_API_KEY=...`, optional `GEMINI_MODEL=...`
- Batch/retry: `APP_AI_BATCH_SIZE=20`, `APP_AI_MAX_RETRIES=3`

Supported vocabulary import line formats:

- `word`
- `word ; meaning`
- `word ; (pos) meaning`
- `word - meaning`
- `word: meaning`
- `word | pos | meaning`
- `multi word phrase ; (n) meaning`

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

## Android App

The Android app lives in `android/` and calls the Voca backend API directly with native Android screens.

```bash
cd android
gradle :app:assembleDebug
```

Open `android/` in Android Studio if Gradle or the Android SDK is not installed locally. The backend API URL is configured by `vocaApiUrl` in `android/gradle.properties`.
