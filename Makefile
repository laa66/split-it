.PHONY: up down build test db-reset prod logs

# Dev stack (compose auto-merges docker-compose.override.yml).
up:
	docker compose up -d --build

down:
	docker compose down

build:
	docker compose build

# Backend unit tests.
test:
	cd backend && ./gradlew test

# Drop the postgres volume and restart so init.sql is reloaded.
db-reset:
	docker compose rm -sf db
	docker volume rm split-it_pgdata || true
	docker compose up -d db

# Prod stack — base compose only, no dev override.
prod:
	docker compose -f docker-compose.yml up -d --build

logs:
	docker compose logs -f
