# Split-it

Aplikacja do dzielenia wydatków w grupach (klon Tricount). Tworzysz grupę, zapraszasz ludzi mailem, dodajesz wydatki z różnymi sposobami podziału, a aplikacja na bieżąco liczy salda i proponuje optymalny plan rozliczeń. Do tego cotygodniowe przypomnienia mailowe dla dłużników i eksport raportu do PDF.

Stack: Java 21 + Spring Boot 3 (backend, architektura heksagonalna), Vue 3 + TypeScript + Ionic (frontend), PostgreSQL, Docker + Docker Compose + Nginx. Całość uruchamiana jedną komendą — nie musisz mieć lokalnie zainstalowanej Javy ani Node, wystarczy Docker.

---

## Spis treści

1. [Wymagania](#1-wymagania)
2. [Instalacja od zera — krok po kroku](#2-instalacja-od-zera--krok-po-kroku)
3. [Konfiguracja `.env`](#3-konfiguracja-env)
4. [Uruchomienie w trybie deweloperskim](#4-uruchomienie-w-trybie-deweloperskim)
5. [Uruchomienie w trybie produkcyjnym](#5-uruchomienie-w-trybie-produkcyjnym)
6. [Konfiguracja poczty (Gmail SMTP)](#6-konfiguracja-poczty-gmail-smtp)
7. [Komendy Makefile](#7-komendy-makefile)
8. [Zmienne środowiskowe](#8-zmienne-środowiskowe)
9. [Architektura](#9-architektura)
10. [Rozwiązywanie problemów](#10-rozwiązywanie-problemów)

---

## 1. Wymagania

Do uruchomienia całości potrzebujesz wyłącznie:

| Narzędzie | Minimalna wersja | Do czego |
|-----------|------------------|----------|
| **Docker Engine** | 24+ | uruchamianie wszystkich kontenerów |
| **Docker Compose** | v2 (wbudowany w `docker`) | orkiestracja stacku |
| **Make** | dowolna | skróty do komend (opcjonalne, ale zalecane) |
| **Git** | dowolna | sklonowanie repozytorium |

Backend, frontend i baza działają w kontenerach, więc **nie musisz instalować lokalnie Javy 21 ani Node.js**. Przydadzą się tylko, jeśli chcesz uruchamiać testy backendu poza Dockerem (`make test`) lub developować bez konteneryzacji.

### Instalacja wymagań na Ubuntu / WSL2

```bash
# Docker Engine + Compose plugin
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER     # dodaje Cię do grupy docker (wyloguj/zaloguj się po tym)

# Make i Git
sudo apt update && sudo apt install -y make git
```

Sprawdź, że Docker działa:

```bash
docker --version
docker compose version
```

> Na WSL2 upewnij się, że Docker Desktop ma włączoną integrację z Twoją dystrybucją (Settings → Resources → WSL Integration), albo że Docker Engine działa natywnie w WSL.

---

## 2. Instalacja od zera — krok po kroku

Pełna ścieżka od czystej maszyny do działającej aplikacji:

```bash
# 1. Sklonuj repozytorium
git clone <adres-repo> split-it
cd split-it

# 2. Utwórz plik konfiguracyjny .env na bazie szablonu
cp .env.example .env

# 3. Wygeneruj sekret JWT (min. 32 bajty) i wklej go do .env jako JWT_SECRET
openssl rand -base64 48

# 4. Otwórz .env i uzupełnij JWT_SECRET (i opcjonalnie maila — patrz sekcja 6)
#    nano .env   /   code .env

# 5. Uruchom cały stack w trybie deweloperskim
make up

# 6. Poczekaj aż kontenery wstaną (pierwszy build trwa kilka minut),
#    potem otwórz w przeglądarce:
#    http://localhost:5173
```

Po `make up` dostajesz komplet usług:

| Usługa | Adres | Opis |
|--------|-------|------|
| Frontend (Vite, hot-reload) | http://localhost:5173 | aplikacja, tu wchodzisz |
| Backend (Spring Boot) | http://localhost:8080/api | REST API |
| Mailhog (lokalny SMTP + UI) | http://localhost:8025 | podgląd wszystkich maili wysłanych w devie |
| PostgreSQL | localhost:5432 | baza (`splitit`/`splitit`) |

### Pierwsze kroki w aplikacji

1. Wejdź na http://localhost:5173 i **zarejestruj konto** (email + nazwa + hasło min. 8 znaków).
2. **Utwórz grupę**.
3. **Zaproś** kogoś mailem — jeśli ten email ma już konto, zostanie dodany od razu; jeśli nie, dostanie link rejestracyjny. W devie maila zobaczysz w Mailhog (http://localhost:8025).
4. **Dodaj wydatek**, wybierz kto płacił i sposób podziału (po równo / procentowo / kwotowo).
5. Zajrzyj w **salda** i **plan rozliczeń**, a w razie potrzeby **pobierz raport PDF**.

---

## 3. Konfiguracja `.env`

Plik `.env` (nieśledzony przez git) trzyma wszystkie sekrety i parametry. Tworzysz go raz, kopiując `.env.example`:

```bash
cp .env.example .env
```

Minimalnie do uruchomienia **musisz ustawić `JWT_SECRET`** — bez niego logowanie nie zadziała. Reszta ma sensowne domyślne wartości dla devu. Pełna lista zmiennych jest w [sekcji 8](#8-zmienne-środowiskowe).

```bash
# Wygenerowanie bezpiecznego sekretu (min. 32 bajty)
openssl rand -base64 48
```

Wklej wynik do `.env`:

```
JWT_SECRET=<tu-wklej-wygenerowany-ciąg>
```

> Nigdy nie commituj `.env`. W repo jest tylko `.env.example` jako szablon.

---

## 4. Uruchomienie w trybie deweloperskim

```bash
make up
```

`make up` uruchamia `docker compose up -d --build`, który automatycznie scala `docker-compose.yml` z `docker-compose.override.yml`. Tryb dev daje:

- **Frontend Vite** z hot-reload na :5173 (zmiany w `frontend/src` widoczne od razu),
- **Backend** na profilu `dev`, wystawiony na :8080,
- **Mailhog** zamiast prawdziwego SMTP — żadne maile nie wychodzą na zewnątrz, wszystkie lądują w http://localhost:8025,
- **PostgreSQL** wystawiony na :5432 (możesz się podłączyć klientem SQL).

Podgląd logów:

```bash
make logs
```

Zatrzymanie:

```bash
make down
```

---

## 5. Uruchomienie w trybie produkcyjnym

```bash
make prod
```

`make prod` uruchamia **wyłącznie** `docker-compose.yml` (bez override), czyli:

- **Nginx** serwuje zbudowany statycznie frontend i proxuje `/api` do backendu — wszystko na jednym originie, brak problemów z CORS. Domyślnie port **80** (`http://localhost`).
- **Backend** na profilu `prod` — wymaga ustawionego `JWT_SECRET` i prawdziwego SMTP (patrz sekcja 6).
- **Mailhog nie wstaje** — w prod maile idą przez skonfigurowany SMTP.

Przed `make prod` upewnij się, że w `.env` masz ustawione: `JWT_SECRET`, dane SMTP (`MAIL_*`) oraz `APP_BASE_URL` na publiczny adres aplikacji (używany w linkach z zaproszeń).

---

## 6. Konfiguracja poczty (Gmail SMTP)

W devie nie musisz nic robić — maile przechwytuje Mailhog. Do produkcji podłącz prawdziwy SMTP. Przykład dla Gmaila:

1. Włącz **weryfikację dwuetapową (2FA)** na koncie Google.
2. Wejdź w Google Account → Security → **App passwords** i wygeneruj 16-znakowe hasło aplikacji.
3. Uzupełnij `.env`:

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=twoj.adres@gmail.com
MAIL_PASS=<16-znakowe-hasło-aplikacji>
MAIL_FROM=twoj.adres@gmail.com
```

> Gmail przepisuje lub odrzuca pole „From" inne niż uwierzytelniony adres — trzymaj `MAIL_FROM` równy `MAIL_USER`.

Inny dostawca (Mailgun, SendGrid, firmowy SMTP) działa tak samo — podmieniasz `MAIL_HOST/PORT/USER/PASS`. Konfiguracja zakłada port **587 ze STARTTLS**.

---

## 7. Komendy Makefile

| Komenda | Działanie |
|---------|-----------|
| `make up` | uruchamia stack deweloperski (dev override, hot-reload, Mailhog) |
| `make down` | zatrzymuje i usuwa kontenery |
| `make build` | przebudowuje obrazy Docker bez uruchamiania |
| `make prod` | uruchamia stack produkcyjny (tylko base compose, Nginx na :80) |
| `make test` | uruchamia testy jednostkowe i integracyjne backendu (`./gradlew test`) |
| `make db-reset` | usuwa wolumen bazy i odtwarza ją z `db/init.sql` |
| `make logs` | śledzi logi wszystkich kontenerów |

> `make test` wymaga Dockera (testy integracyjne podnoszą PostgreSQL przez Testcontainers).

---

## 8. Zmienne środowiskowe

Wszystkie ustawiane w `.env` (szablon: `.env.example`).

| Zmienna | Domyślna | Opis |
|---------|----------|------|
| `HTTP_PORT` | `80` | port Nginx w trybie prod |
| `POSTGRES_DB` | `splitit` | nazwa bazy |
| `POSTGRES_USER` | `splitit` | użytkownik bazy |
| `POSTGRES_PASSWORD` | `change-me` | hasło bazy (zmień na prod) |
| `SPRING_PROFILES_ACTIVE` | `prod` | profil Springa (`dev` ustawiany automatycznie przez override) |
| `APP_BASE_URL` | `http://localhost` | bazowy URL w linkach z zaproszeń |
| `JWT_SECRET` | *(brak — wymagane)* | sekret do podpisu JWT, min. 32 bajty |
| `JWT_EXPIRATION_HOURS` | `24` | ważność tokenu logowania |
| `INVITATION_EXPIRATION_DAYS` | `7` | ważność linku z zaproszenia |
| `REMINDER_CRON` | `0 0 9 * * MON` | harmonogram przypomnień (format Spring: `sek min godz dzień mies dzień-tyg`) |
| `MAIL_HOST` | `smtp.gmail.com` | host SMTP (w devie nadpisany na `mailhog`) |
| `MAIL_PORT` | `587` | port SMTP (w devie `1025`) |
| `MAIL_USER` | — | login SMTP |
| `MAIL_PASS` | — | hasło SMTP / hasło aplikacji |
| `MAIL_FROM` | `no-reply@split-it.local` | adres nadawcy (trzymaj równy `MAIL_USER` dla Gmaila) |

---

## 9. Architektura

Backend trzyma się **architektury heksagonalnej**: warstwa `domain/` zawiera czyste modele i interfejsy portów (zero adnotacji JPA/Spring), a `infrastructure/` to adaptery — kontrolery REST, repozytoria JPA, wysyłka maili, generowanie PDF. Dzięki temu logika biznesowa jest niezależna od frameworka i w pełni testowalna w izolacji.

Kwoty zawsze jako `NUMERIC(12,2)` (nigdy float). Salda liczone są na bieżąco z wydatków i rozliczeń — nigdy nie są zapisywane. Wydatki są niezmienne po utworzeniu; rozliczenia to osobne wiersze. Schemat bazy żyje w `db/init.sql` (bez Flyway) i ładuje się przy pierwszym starcie PostgreSQL; `make db-reset` odtwarza go od zera.

Nginx serwuje statyczny build frontendu i proxuje `/api/**` do backendu — frontend i API działają na jednym originie, więc nie ma problemów z CORS.

---

## 10. Rozwiązywanie problemów

**`make up` kończy się błędem „JWT_SECRET variable is not set"**
Nie utworzyłeś `.env` albo nie ustawiłeś w nim `JWT_SECRET`. Wykonaj `cp .env.example .env` i wklej wynik `openssl rand -base64 48`.

**Zmieniłem coś w `db/init.sql`, ale baza tego nie widzi**
`init.sql` ładuje się tylko przy pierwszym starcie (na pustym wolumenie). Po zmianie schematu zrób `make db-reset`.

**Port 80 / 5173 / 8080 zajęty**
Zatrzymaj kolidującą usługę albo zmień mapowanie portów (`HTTP_PORT` w `.env` dla prod; porty dev w `docker-compose.override.yml`).

**Maile nie docierają w devie**
To normalne — w devie nie wychodzą na zewnątrz. Sprawdź je w Mailhog: http://localhost:8025.

**Chcę zacząć zupełnie od zera**
`make down`, potem `docker volume rm split-it_pgdata`, potem `make up`.
