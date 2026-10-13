# FREE deployment (Rs. 0): GitHub + Aiven MySQL + Render + UptimeRobot

Goal: a live HTTPS link that 50-100 people can use, with their data stored safely in a real MySQL database.
Free-plan rules change - re-check each service's pricing page before you start.

## 1. Put the project on GitHub
Run inside the folder that contains `pom.xml` and `Dockerfile`:

    git init
    git add .
    git status          <- make sure NO .env, target/, .idea/ or node_modules/ is listed
    git commit -m "CMS first version"
    git branch -M main
    git remote add origin https://github.com/YOUR_NAME/cms.git
    git push -u origin main

Create the repository as **Private** on github.com first.

## 2. Free MySQL on Aiven (no card needed)
1. aiven.io -> sign up -> Create service -> **MySQL** -> plan **Free** -> region nearest to India -> Create.
2. Wait until the service is **Running**, then copy: Host, Port, User (`avnadmin`), Password, Database (`defaultdb`).
3. Your `DB_URL` is:

       jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&characterEncoding=UTF-8

Free plan: 1 GB storage, ~76 connections, may power itself off if completely idle (the health-check pings in step 5 keep it active).

## 3. Free web app on Render
1. render.com -> sign up with GitHub -> **New** -> **Blueprint** (uses `render.yaml`) or **Web Service** -> choose the repo.
2. Runtime **Docker**, plan **Free**, Health Check Path `/api/health`.
3. Environment variables:

| Name | Value |
|---|---|
| SPRING_PROFILES_ACTIVE | prod |
| COOKIE_SECURE | true |
| DB_URL | the URL from step 2 |
| DB_USER | avnadmin |
| DB_PASS | Aiven password |
| APP_ADMIN_EMAIL | your admin email |
| APP_ADMIN_PASSWORD | a STRONG new password (12+ chars) |

4. Deploy. First build takes several minutes. In the logs wait for `Started CmsApplication` and `Admin user created`.
5. Your site: `https://<name>.onrender.com`

## 4. Check it works
- `https://<name>.onrender.com/api/health` -> `{"status":"UP"}`
- Log in as admin, sign up a student with another browser/Incognito, create a complaint (try Hindi text), resolve it as admin.
- Logged out: `/api/admin/users` must give 401.

## 5. Keep it awake (free Render sleeps after 15 min without traffic; waking takes ~1 minute)
1. uptimerobot.com -> free account -> **Add New Monitor** -> type HTTP(s).
2. URL `https://<name>.onrender.com/api/health`, interval = the smallest the free plan allows (5 min).
3. Add your email for "site is down" alerts.
A single always-on free service uses about 744 of the 750 free monthly hours - do not run a second free web service in the same Render workspace.

## 6. Backups
Aiven keeps backups, but also take your own copy now and then (from your PC, MySQL client installed):

    mysqldump -h HOST -P PORT -u avnadmin -p --ssl-mode=REQUIRED defaultdb > backup_YYYY-MM-DD.sql

## 7. Before you share the link with 50-100 people
- Admin password is strong and not shared. Do not reuse a password from anywhere else.
- Tell testers it is a test system and not to enter sensitive personal information.
- Repository is private; no passwords exist in any file (`git grep -i password` shows only code, not real values).
- Test the whole flow once on the live URL on a phone too.

## Known limits of the free setup
- One small instance (512 MB). Fine for light use by 50-100 people; not load-tested for hundreds at once.
- If the pinger fails the first visit after idle is slow (~1 min).
- Render blocks outgoing email ports on the free plan, so email OTP / notifications need an email-API service later.
- No file storage on Render's free plan (that is why attachments were removed).
