<div align="center">

  # JapLearn Backend API

  **Spring Boot service behind the JapLearn student app, teacher dashboard, and admin portal.**

  [![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
  [![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?logo=mongodb&logoColor=white)](https://www.mongodb.com/atlas)

</div>

---

## Overview

This service handles accounts, classes, lessons, game content, learning records, speech assessment, live conversation with Sumi, email, and the evaluation reports used by teachers and administrators.

| Client | How it connects |
|---|---|
| Student app (Android and [japlearn.com](https://japlearn.com)) | Through the portal relay at `https://portal.japlearn.com/api/...` |
| Teacher dashboard and admin portal ([portal.japlearn.com](https://portal.japlearn.com)) | Same relay (a Vercel function that forwards `/api/*` here) |

### Related repositories

| Repository | Contents |
|---|---|
| [JapLearn2.0](https://github.com/rychfghg/JapLearn2.0) | This backend |
| [JapLearn-2.0](https://github.com/rychfghg/JapLearn-2.0) | Student application |
| [Japlearn-Website](https://github.com/rychfghg/Japlearn-Website) | Teacher dashboard and admin portal |

## Tech Stack

| Area | Technology |
|---|---|
| Runtime | Java 17 (Eclipse Temurin) |
| Framework | Spring Boot 3.2 (Web, Security, Data MongoDB, Mail) |
| Database | MongoDB Atlas |
| Email | Brevo SMTP |
| Speech assessment | Microsoft Azure Speech (pronunciation) |
| Live conversation | Google Gemini Live (WebSocket) |
| Sign-in | Email and password, Google ID token verification |
| PDF | Apache PDFBox |
| Hosting | Render (Docker) |

## Features

- **Accounts:** sign-up with input rules (no emoji in names, emails, or passwords; passwords may use any keyboard symbol), login limits, password reset, Google sign-in, students editing their own name, and full account deletion.
- **Classes and lessons:** teacher classes, enrollment, lesson databank, and lesson progress.
- **Game content:** QuackSituate, QuackSlate, Quack-a-Mole, Quackman, and QuackResponse content, editable in the admin portal.
- **Learning records:** scores, unfinished runs, per-question QuackSituate answers, Reply Coach attempts and readiness, Response Rush evidence and review, Dialogue Relay progress, Guided Phrase practice, and Talk with Sumi sessions.
- **Speech and conversation:** Azure pronunciation assessment and Gemini Live sessions for Guided Phrase Practice and Talk with Sumi, with adaptive learner level and a daily limit.
- **Evaluation:** the admin-only SMART Goals report, Google Forms survey comparison, and refactoring priorities.

## Evaluation Rules (SMART Goals report)

- Only the **first independent attempt** counts; repeat runs, repairs, and practice are excluded and counted separately.
- **Assisted answers** (hint, Romaji, or meaning shown) never count toward accuracy.
- **Wrong, timeout, and technical interruption** are recorded separately; technical failures are never scored as wrong.
- **Unfinished runs** are scored on the questions actually answered; an Ahiru Rescue game over is a finished but uncleared run.
- **Completion** = activities completed ÷ (8 activities × eligible students).
- Survey responses are matched to accounts **by email only** and are never used as performance data.

The report states its own rules, so the numbers can be checked against the records.

## Security

- `PortalAuthorizationFilter`: `/api/admin/**` is admin-only; changing another user's account is admin-only.
- `StudentOwnershipFilter`: students can only read and write their own records.
- `RateLimitingFilter`: per-client limits on sign-in, sign-up, and costly endpoints. The real client IP is trusted only when the relay's shared secret matches.
- Error responses never include messages or stack traces.

## Requirements

- JDK 17
- Maven (or the included `mvnw` wrapper)
- A MongoDB connection string

## Run Locally

Set the required environment variables, then:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell use `.\mvnw spring-boot:run`. The API listens on `http://localhost:8080`; check `GET /api/health`.

Run the tests:

```bash
./mvnw test
./mvnw -q test -Dtest=SmartGoalsServiceTest
```

## Environment Variables

Values marked **secret** must only be set in Render (or a local, untracked environment). Never commit them.

| Variable | Purpose | |
|---|---|---|
| `MONGODB_URI` | MongoDB connection string | **secret** |
| `MONGODB_DATABASE` | Database name | |
| `BREVO_SMTP_LOGIN`, `BREVO_SMTP_PASSWORD` | Email sending | **secret** |
| `SMTP_HOST`, `SMTP_PORT`, `MAIL_FROM_ADDRESS` | Email settings | |
| `APP_BACKEND_URL`, `APP_STUDENT_WEB_URL`, `APP_PORTAL_WEB_URL` | Links used in emails | |
| `JAPLEARN_PROXY_SECRET` | Shared secret with the portal relay | **secret** |
| `AZURE_SPEECH_KEY`, `AZURE_SPEECH_REGION` | Pronunciation assessment | key is **secret** |
| `GEMINI_API_KEY`, `GEMINI_LIVE_MODEL`, `SUMI_GEMINI_VOICE` | Live conversation | key is **secret** |
| `GOOGLE_CLIENT_IDS` | Comma-separated Android and web OAuth client IDs accepted for Google sign-in | |
| `ALLOWED_ORIGINS` | CORS origins | |
| `ALLOWED_EMAIL_DOMAINS`, `BLOCKED_EMAIL_DOMAINS`, `MAX_SIGNUPS_PER_HOUR` | Sign-up guard | |
| `ADMIN_ALERT_EMAIL`, `ADMIN_ALERTS_ENABLED` | Security alerts to the administrator | set in Render only |
| `STUDENT_TOKEN_REQUIRED` | Require student tokens | |

## Deploy (Render)

The service is defined in [`render.yaml`](./render.yaml) and built from the [`Dockerfile`](./Dockerfile). Push to the deployed branch, or trigger a manual deploy in Render. The health check is `/api/health`. A free-tier instance may sleep and need time to wake.

**Release order:** deploy the backend before publishing an app update that sends new fields.

### One-time content updates

Some seeders run once on startup and then record a marker in the `AppSetting` collection, so later edits made in the admin portal are never overwritten:

| Seeder | What it does |
|---|---|
| `QuackmanContentSeeder` | Six beginner Quackman words |
| `PolitenessNameSeeder` | Replaces a real surname in one Politeness question with a neutral name |
| `SituationalQuestionSeeder`, `ExpressionMatchSeeder`, `QuackslateQuestionSeeder`, `ReplyCoachChapterSeeder` | Default content when a collection is empty |

## Project Structure

```text
src/main/java/japlearn/demo/
├── Config/       # Security filters, rate limiting, and content seeders
├── Controller/   # REST endpoints for students, teachers, and administrators
├── DTO/          # Request objects
├── Entity/       # MongoDB documents
├── Repository/   # Spring Data repositories
└── Service/      # Business logic, evaluation, speech, and live conversation
src/test/java/    # Unit tests
```

## Troubleshooting

- **Requests from the app fail but `/api/health` works:** check `ALLOWED_ORIGINS` and that the portal relay's `JAPLEARN_PROXY_SECRET` matches.
- **Google sign-in returns an error:** confirm `GOOGLE_CLIENT_IDS` contains both the Android and web client IDs.
- **Account deletion says the portal session is missing:** an older build is running; redeploy.
