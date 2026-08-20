# JapLearn Backend - Render Docker Deployment

## Before deploying

1. Rotate the MongoDB database password because an earlier credential was committed in `application.properties`.
2. Revoke the earlier Brevo SMTP key and create a new SMTP key.
3. Push the sanitized backend files, including `Dockerfile`, `.dockerignore`, and `render.yaml`.
4. Never commit a real `.env` file.

## Create the Render service

### Blueprint option

In Render, choose **New > Blueprint**, connect the repository, and select the repository containing `render.yaml`.

### Manual web-service option

1. Choose **New > Web Service**.
2. Connect the Git repository.
3. Select **Docker** as the runtime.
4. If this repository contains multiple projects, set **Root Directory** to:
   `JapLearn-BackEnd_Test/Backend`
5. Dockerfile path relative to that root: `./Dockerfile`.
6. Health check path: `/api/health`.

No Docker command override is required. The Dockerfile starts the application.

## Render environment variables

Add these under **Service > Environment**. Secret values must be entered only in Render.

| Key | Value |
| --- | --- |
| `MONGODB_URI` | The newly rotated MongoDB Atlas Java connection string, including the `japlearn` database and query options. |
| `MONGODB_DATABASE` | `japlearn` |
| `BREVO_SMTP_LOGIN` | The SMTP login displayed in the JapLearn Official Brevo account. |
| `BREVO_SMTP_PASSWORD` | A newly generated Brevo SMTP key. Do not reuse the exposed key. |
| `SMTP_HOST` | `smtp-relay.brevo.com` |
| `SMTP_PORT` | `587` |
| `MAIL_FROM_ADDRESS` | `japlearnofficial@gmail.com` |
| `APP_BACKEND_URL` | The complete Render backend URL, such as `https://japlearn-backend.onrender.com`, without a trailing slash. |
| `APP_FRONTEND_URL` | The public student web/app URL that opens `/ResetPassword`, without a trailing slash. |
| `SPRING_WEB_LOG_LEVEL` | `INFO` |
| `TOMCAT_LOG_LEVEL` | `INFO` |

Do not manually add `PORT`. Render provides it automatically, and Spring reads it through `${PORT:8080}`.

## MongoDB Atlas network access

Render must be allowed to connect to MongoDB Atlas. Configure Atlas Network Access using the outbound addresses shown for the Render service. For temporary testing, Atlas can allow access from anywhere, but restrict access before production whenever practical.

## First deployment check

After Render reports a successful deploy, open:

`https://YOUR-SERVICE.onrender.com/api/health`

The expected response is:

```json
{"status":"ok","service":"japlearn-backend"}
```

Then update the React Native app and teacher/admin website API URLs to the new HTTPS backend URL.
