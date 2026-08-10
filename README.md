# Recruitment CRM — CSE327 Software Engineering Project

**Members**

| Name | Student ID |
|---|---|
| Mohammad Nabil Ferdous | 2423275042 |
| Kazi Muhammad Abdullah | 2321556042 |

## About

A recruitment CRM built in Java to demonstrate six GoF design patterns,
implemented by hand in the application layer. It runs as a full web app
with a REST API, user login, SQLite database, and an HTML/CSS/JS frontend
— all served from one process.

## Design patterns

All pattern code lives under `src/main/java/com/recruitcrm/patterns/`:

| Pattern | Package | What it does here |
|---|---|---|
| Factory Method | `patterns.factory` | Creates Candidate / Recruiter / Company accounts via one concrete factory per type |
| Singleton | `patterns.singleton` | `DataStore` — one shared data access point backed by SQLite |
| Strategy | `patterns.strategy` | Interchangeable candidate evaluation algorithms (Technical / HR / Behavioral) |
| Observer | `patterns.observer` | Notifies interested parties (email, audit log) when an application's status changes |
| Decorator | `patterns.decorator` | Adds Featured / Urgent badges (and a visibility score) on top of a plain job posting |
| Facade | `patterns.facade` | `RecruitmentFacade` — one simple entry point coordinating Singleton, Strategy, and Observer |

## Database

Data is stored in **SQLite** at `data/crm.db` (override with `DATABASE_PATH`).
Tables: `accounts`, `jobs`, `applications`, `sessions`. No separate database
server to install — SQLite is embedded via the JDBC driver.

On first run, two demo accounts are seeded:

| Email | Password | Role |
|---|---|---|
| recruiter@demo.com | demo123 | Recruiter — post jobs, manage pipeline |
| candidate@demo.com | demo123 | Candidate — apply to roles |

## Authentication

| Route | Purpose |
|---|---|
| POST `/api/auth/register` | Create account (type, name, email, password, extra) |
| POST `/api/auth/login` | Log in — sets an HttpOnly session cookie |
| POST `/api/auth/logout` | End session |
| GET `/api/auth/me` | Current user (or `{ authenticated: false }`) |

Protected routes:

- **Recruiter only:** post jobs, feature/urgent toggles, advance pipeline
- **Logged in:** submit applications

## Running locally

**Quick start (recommended):**
```bash
./start.sh
```
Then open **http://localhost:8080**.

**Manual (Maven):**
```bash
mvn package -DskipTests
java -jar target/recruitment-crm.jar
```

**Console demo (original pattern walkthrough):**
```bash
mvn package -DskipTests
java -cp target/recruitment-crm.jar com.recruitcrm.Main
```

**Docker:**
```bash
docker build -t recruitment-crm .
docker run -p 8080:8080 -v crm-data:/app/data recruitment-crm
```

## Deploy online (Render.com)

1. Push this repo to GitHub.
2. Go to [render.com](https://render.com) → **New** → **Blueprint** → connect the repo.
3. Render reads `render.yaml` automatically (Docker build + persistent disk at `/app/data`).
4. Deploy — you'll get a public `https://your-app.onrender.com` URL.

Alternatively: **New → Web Service → Docker**, point at this repo's `Dockerfile`,
and add a disk mounted at `/app/data` so SQLite data survives restarts.

Railway.app works the same way with the included `Dockerfile`.

## API endpoints

| Method | Route | Auth | Purpose |
|---|---|---|---|
| GET | `/api/jobs` | — | List all jobs |
| POST | `/api/jobs` | Recruiter | Create a job |
| POST | `/api/jobs/{id}/feature` | Recruiter | Toggle featured |
| POST | `/api/jobs/{id}/urgent` | Recruiter | Toggle urgent |
| GET | `/api/applications` | — | List applications |
| POST | `/api/applications` | Logged in | Submit application |
| POST | `/api/applications/{id}/status` | Recruiter | Update status + evaluation |
