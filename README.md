# REDMENACEMONEY

Paula's Finance App — budget, net worth, debt payoff and goal tracker for the Gullen household.

This repo has two versions of the same app:

- **Web app** (this folder) — a PWA hosted via GitHub Pages, installable to a phone home screen.
  See the instructions below.
- **Android app** (`/android`) — a native Kotlin/Jetpack Compose version, cloud-built into a
  real APK via GitHub Actions (no Android Studio needed locally). See `android/README.md`.

Both versions use the same data shape, so a backup exported from one can be imported into
the other.

## Web app: what's in this folder

- `index.html` — the app itself (all HTML/CSS/JS, no build step)
- `manifest.json` — makes it installable as a phone app (PWA)
- `sw.js` — lets it work offline once it's been loaded once
- `icon-192.png`, `icon-512.png`, `icon-512-maskable.png` — home screen icons

Data is saved right on the device: it uses Claude's storage when opened inside a Claude
artifact, and your browser's local storage automatically when hosted anywhere else (like
GitHub Pages below) — no setup needed either way. It stays on that one device/browser; there's
no server, so different phones won't automatically see each other's entries.

## Put the web app on Paula's phone (GitHub Pages — free, no server needed)

1. On GitHub, go to this repo's **Settings → Pages**.
2. Under "Build and deployment," set **Source: Deploy from a branch**, branch **main**, folder **/ (root)**.
3. Save. GitHub will publish it at:
   `https://kengineer927518.github.io/REDMENACEMONEY/`
4. On Paula's phone, open that link in Chrome (Android) or Safari (iPhone).
5. Install it: Android/Chrome → ⋮ menu → "Add to Home screen"/"Install app". iPhone/Safari →
   Share icon → "Add to Home Screen".

## Loading real numbers into either version

This repo's source stays blank on purpose (all $0) since it's public — no real balances live
in the code. There's a separate `starter-data.json` file with the real numbers that was **not**
committed here; send that file to Paula directly (text, email, AirDrop) instead. On either
version: Dashboard tab → **Import data** → pick that file. Same file works for both the web
app and the Android app.

## Updating it later

Replace `index.html` and push — GitHub Pages picks up the new version automatically. For the
Android app, push changes under `/android` and GitHub Actions rebuilds the APK — see
`android/README.md`.
