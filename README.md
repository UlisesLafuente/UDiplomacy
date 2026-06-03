# UDiplomacy

Diplomacy game implementation with Spring Boot (Java 21) backend and React + TypeScript frontend.

## Stack

- **Backend**: Java 21, Spring Boot, MongoDB (game state), PostgreSQL (user/game projections)
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS, React Router
- **Build**: Docker multi-stage (backend + nginx static serve), Maven wrapper

## Quick start

```bash
# Backend
./mvnw spring-boot:run

# Frontend (dev)
cd frontend && npm install && npm run dev
```

## Docker

```bash
docker compose up --build
```

## Default admin

- Username: `admin`
- Password: `diplomacy`

## API

All endpoints under `/api/` — see controllers in `infrastructure/web/controllers/`.

## Tests

```bash
./mvnw test
```

## Project structure

- `src/main/java/com/ulises/udiplomacy/application/` — use cases + services
- `src/main/java/com/ulises/udiplomacy/domain/` — domain model
- `src/main/java/com/ulises/udiplomacy/infrastructure/` — persistence, web, security
- `frontend/src/` — React app
