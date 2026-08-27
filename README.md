# Cadre — Recruitment CRM

**CSE327 Software Engineering — Final Project**

| Name | Student ID |
|---|---|
| Mohammad Nabil Ferdous | 2423275042 |
| Kazi Muhammad Abdullah | 2321556042 |

---

## About

A recruitment CRM written in **plain Java with no framework**, implementing
**nine Gang of Four design patterns** by hand. It runs as a complete web
application — REST API, session authentication, SQLite database, real SMTP
email, and an HTML/CSS/JS frontend — all served from a single process.

The only external library in the entire project is the SQLite JDBC driver.
No Spring, no Laravel, no ORM, no JSON library, no mail library. Every design
pattern is application code written for this project, so none of it can be
attributed to a framework.

| | |
|---|---|
| Design patterns | 9 (6 required + Builder, Proxy, Adapter) |
| Java source files | 75 main + 9 test |
| Automated tests | 117 JUnit 5 tests, all passing |
| External libraries | 1 (SQLite JDBC driver) |
| Database tables | 4 |

**Full technical documentation:** see [`PATTERNS_EXPLAINED.md`](PATTERNS_EXPLAINED.md)
— line-by-line breakdowns of all nine patterns plus the database layer,
request lifecycle, and authentication design.

---

## Design patterns

| # | Pattern | Category | Package | Role in the system |
|---|---|---|---|---|
| 1 | Factory Method | Creational | `patterns.factory` | Creates the three account types via one factory class per type, dispatched through a registry — no conditional decides the class |
| 2 | Singleton | Creational | `patterns.singleton` | `DataStore` — one shared point of access to persistence |
| 3 | Builder | Creational | `patterns.builder` | Assembles `Job` postings with three required and five optional fields, plus a `JobDirector` holding standard recipes |
| 4 | Strategy | Behavioral | `patterns.strategy` | Interchangeable evaluation algorithms; new assessment types can be registered **at runtime** |
| 5 | Observer | Behavioral | `patterns.observer` | Email and audit-log observers react to application status changes |
| 6 | Decorator | Structural | `patterns.decorator.compensation` | Builds compensation packages — base salary wrapped by allowances and bonuses, each adding a real amount and line item |
| 7 | Facade | Structural | `patterns.facade` | `RecruitmentFacade` — two methods hiding three subsystems |
| 8 | Proxy | Structural | `patterns.proxy` | Protection proxy controlling access to candidate contact details, with lazy loading and access auditing |
| 9 | Adapter | Structural | `notification` | Adapts the raw SMTP protocol to a one-line `EmailSender` interface |

### Notes on two of them

**Decorator** was rebuilt after instructor feedback. The first version added
Featured/Urgent badges to job postings, which only *highlighted* a posting
rather than adding responsibility to it. The current version decorates
compensation: each decorator contributes to the description, the monthly
total, **and** the itemised breakdown. Because percentage-based decorators
calculate from whatever sits beneath them, **the order of composition changes
the result** — housing-then-transport gives 75,000 while transport-then-housing
gives 77,000. This is asserted by the test suite.

**Observer** is hand-written rather than using `java.util.Observer`, both
because the assignment requires it and because those classes have been
deprecated since Java 9.

---

## Running it

### Quickest — compile and run directly

```bash
javac -cp lib/sqlite-jdbc.jar -d out $(find src/main/java -name "*.java")
java -cp "out:lib/sqlite-jdbc.jar" com.recruitcrm.web.WebMain
```

Open **http://localhost:8080**. Leave the terminal window open — closing it
stops the server.

On Windows, swap the classpath separator: `"out;lib/sqlite-jdbc.jar"`.

### Alternatives

```bash
./start.sh                                          # uses Maven if present, javac otherwise
java -cp "out:lib/sqlite-jdbc.jar" com.recruitcrm.Main   # console pattern walkthrough
docker build -t cadre . && docker run -p 8080:8080 -v crm-data:/app/data cadre
```

### Demo accounts

Seeded automatically on first run.

| Email | Password | Role |
|---|---|---|
| `recruiter@demo.com` | `demo123` | Recruiter — post roles, run the pipeline |
| `candidate@demo.com` | `demo123` | Candidate — browse and apply |

---

## Running the tests

One-time setup:

```bash
curl -L -o lib/junit-platform-console-standalone.jar \
  https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar
```

Then:

```bash
./run-tests.sh
```

117 tests across nine suites — one per pattern. Notable cases:

- `DecoratorTest.orderOfDecoratorsChangesTheTotal()` — proves the chain genuinely composes
- `StrategyTest.sameScoreDifferentVerdicts()` — a score of 65 passes HR and fails Technical
- `SingletonTest.dataStoreConstructorIsPrivate()` — uses reflection to verify the constructor

---

## Email notifications

Application status changes trigger real email through a hand-written SMTP
client. Five templates cover application received, shortlisted, interview
invitation, offer, and rejection. The offer letter contains the itemised
compensation package produced by the Decorator chain.

**Without credentials** the app falls back to printing emails to the console,
so it runs anywhere with no setup.

**To send real email**, set these before starting the server — never commit
them, as this repository is public:

```bash
export MAIL_USERNAME="your.address@gmail.com"
export MAIL_PASSWORD="your-16-char-google-app-password"
export MAIL_FROM_NAME="Cadre Recruitment"
```

Gmail requires an **App Password** (Google Account → Security → 2-Step
Verification → App passwords), not your normal password.

---

## Database

SQLite at `data/crm.db`, overridable with `DATABASE_PATH`. Embedded via the
JDBC driver — no database server to install. `data/` is gitignored, so
password hashes are never pushed.

| Table | Holds |
|---|---|
| `accounts` | All three account types, distinguished by a `role` column |
| `jobs` | Postings, including numeric `base_salary` used by the Decorator |
| `applications` | Candidate–job links, status, evaluation results, offer entitlements |
| `sessions` | Session tokens with expiry |

Inspect it directly:

```bash
sqlite3 data/crm.db
.headers on
.mode column
SELECT email, role, name FROM accounts;
```

Columns added after the first release are applied on startup by an
`addColumnIfMissing` migration helper, so an existing database is upgraded in
place rather than wiped.

---

## Security

- Passwords hashed with **PBKDF2-HMAC-SHA256**, 120,000 iterations, 16-byte
  per-user salt from `SecureRandom`. Never stored or recoverable.
- Password comparison is **constant-time**, defeating timing attacks.
- Session cookies are `HttpOnly` and `SameSite=Lax`, expiring after 24 hours.
- **Every** SQL statement uses `PreparedStatement` with bound parameters —
  no query concatenates user input.
- The static file handler normalises paths and rejects anything resolving
  outside `public/`, preventing path traversal.
- Authorisation is enforced **server-side** in every handler. Hidden buttons
  are treated as convenience, never as security.

---

## API

| Method | Route | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | — | Create an account |
| POST | `/api/auth/login` | — | Log in; sets the session cookie |
| POST | `/api/auth/logout` | — | End the session |
| GET | `/api/auth/me` | — | Current user, or `{authenticated:false}` |
| GET | `/api/jobs` | — | List open roles |
| POST | `/api/jobs` | Recruiter | Create a role |
| POST | `/api/jobs/{id}/feature` | Recruiter | Toggle Featured |
| POST | `/api/jobs/{id}/urgent` | Recruiter | Toggle Urgent |
| GET | `/api/applications` | Logged in | **Filtered** — candidates get only their own rows |
| POST | `/api/applications` | Candidate | Submit an application |
| POST | `/api/applications/{id}/status` | Recruiter | Advance a stage, run an evaluation, attach an offer |
| GET | `/api/applications/metrics` | Recruiter | List assessment types |
| POST | `/api/applications/metrics` | Recruiter | Register a new assessment type at runtime |
| GET | `/api/candidates/{email}` | Logged in | Profile — **fields masked by the Proxy** unless authorised |
| POST | `/api/accounts` | — | Create an account without credentials |

---

## Project structure

```
src/main/java/com/recruitcrm/
├── Main.java              Console demo of the patterns
├── domain/                Plain data classes — no pattern logic
├── patterns/              EIGHT PATTERNS, one package each
│   ├── factory/  singleton/  builder/
│   ├── strategy/  observer/
│   └── decorator/  facade/  proxy/
├── notification/          ADAPTER — SMTP client, adapter, email templates
├── persistence/           SQLite connections, schema, migrations
└── web/                   HTTP handlers, auth, JSON. Calls the patterns.

src/test/java/com/recruitcrm/    Nine JUnit suites, one per pattern
public/                          Frontend — 3 files, no build step
```

The `web/` package is delivery plumbing that *calls* the patterns; it is not
itself one. `PasswordUtil`, `SessionManager` and `JsonWriter` are supporting
utilities, not design patterns.

---

## Deploying

`render.yaml` and a `Dockerfile` are included. On [render.com](https://render.com):
**New → Blueprint → connect this repo**. Render reads `render.yaml`
automatically and mounts a persistent disk at `/app/data` so the database
survives restarts.

Note that many free hosting tiers block outbound SMTP, so real email may work
locally but not once deployed.

---

## Known limitations

Stated openly rather than left to be discovered:

- `DataStore` imports from the `web` package — dependencies should point
  inward, so the layering here is wrong even though it works.
- Custom assessment types live in memory and are lost on restart.
- Interview details are not persisted; they survive long enough to build the
  invitation email but not a restart.
- `JobDirector` is implemented but not yet called from the UI.
- Listing applications loads each candidate inside the loop — a genuine N+1
  pattern, unobservable at this scale but real.
- The SQL is SQLite-specific (the upsert syntax in particular), so moving to
  another database would require rewriting those statements.
- The offer negotiation flow was designed but not built; it is a state machine
  and would be better served by adding the State pattern than by bolting it on.

---

## Repository

<https://github.com/NabilRafi/Recruitment-Customer-Relationship-Management-CRM->
