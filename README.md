# Hotel Launcher Admin Panel

Unified web admin panel and API for the Hotel Launcher Rebuild.

## Start

```powershell
npm install
npm start
```

The panel will run on:

- `http://localhost:3000`

## Environment

Create `E:\hotel mode ui launcher\central-admin-panel\.env.local` with:

- `DATABASE_URL`
- `IMAGEKIT_PUBLIC_KEY`
- `IMAGEKIT_PRIVATE_KEY`
- optional `PORT`

The app now runs as a single `3000`-port experience and stores state in Postgres when `DATABASE_URL` is present. It falls back to the old local JSON store only if the database is missing.

Image uploads go to ImageKit when keys are present and fall back to local `/uploads` only if ImageKit is unavailable.

## What it does

- Creates first-boot activation codes for TVs
- Lets reception bind activation codes to room numbers and guest names
- Serves one launcher content payload to bound TVs
- Uploads startup logo and background slideshow images
- Controls visible app packages and visible input/source titles
- Controls breakfast/lunch/dinner menu cards and popup call info

## Database bootstrap

If you want to create the database schema yourself, use:

- [creator.sql](E:\hotel mode ui launcher\central-admin-panel\creator.sql)

## Important launcher setting

The launcher reads the API base URL from the Android manifest metadata key:

- `launcher_api_base_url`

Current default:

- `http://10.0.2.2:3000`

For real TVs on the hotel network, change that manifest value to the actual server IP or DNS name before building the production APK.
