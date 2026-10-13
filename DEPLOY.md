# CMS - Run, Test & Deploy

## 1. Local run (no MySQL needed)
    mvnw.cmd spring-boot:run        (Windows)   |   ./mvnw spring-boot:run   (Mac/Linux)
Open http://localhost:8080
Dev admin: `admin@cms.local` / `Admin@12345` (only in the dev profile; database is in memory and resets on restart).

## 2. Tests
    mvnw.cmd test

## 3. Changing the design (only if you edit Tailwind classes in the HTML)
The site no longer loads Tailwind from a CDN. The CSS is built once into `src/main/resources/static/tailwind.css`.
If you add new Tailwind classes to an HTML page, rebuild it (Node.js needed):

    cd frontend
    npm install
    npm run build:css

## 4. Environment variables (production)
| Variable | Meaning |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | e.g. `jdbc:mysql://HOST:3306/cms_db?useSSL=true&characterEncoding=UTF-8` |
| `DB_USER`, `DB_PASS` | MySQL user (not root!) |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | first admin, created on startup if it does not exist |
| `COOKIE_SECURE` | `true` on HTTPS (default). `false` only for http://localhost tests |
| `PORT` | optional, default 8080 |

Never put these in code or Git.

## 5. Option A - one server (VPS) or your own PC with Docker
    cp .env.example .env        # edit the values
    docker compose up -d --build
App: http://SERVER_IP:8080 . For real users put HTTPS in front (Caddy or Nginx + Let's Encrypt), then set `COOKIE_SECURE=true`
in `.env` and run `docker compose up -d` again. Without HTTPS and COOKIE_SECURE=true the login cookie is not sent by browsers.

Backup (run daily from cron):
    docker compose exec db sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" cms_db' > backup_$(date +%F).sql

## 6. Option B - Render (Docker)
1. Push this project to a GitHub repository (the `.gitignore` already keeps secrets and build files out).
2. You need a MySQL database hosted somewhere (Render itself does not host MySQL). Create one, note host, database,
   user, password. Use `useSSL=true` (or the provider's SSL option) in `DB_URL` for a remote database.
3. Render dashboard -> New -> Blueprint -> select the repo (it reads `render.yaml`), or New -> Web Service -> Docker.
4. Fill the environment variables from section 4 (`COOKIE_SECURE=true`; Render serves HTTPS).
5. Health check path: `/api/health`.
Free plans may sleep when idle - check the current plan details before relying on it for real users.

## 7. After deploying - check
- Open the site over `https://`, sign in as the admin, sign up a student, create a complaint (try Hindi text).
- `https://YOUR_SITE/api/health` shows `{"status":"UP"}`.
- `https://YOUR_SITE/api/admin/users` while logged out must show 401.
- Restart the app: data must still be there.

## Security notes
* Passwords are BCrypt-hashed. Users created before the security update (plaintext passwords) must sign up again.
* Login is limited to 5 wrong attempts per email / 30 per IP per 15 minutes (in-memory, per server).
* Change any password that was ever stored in an old `application.properties`.
