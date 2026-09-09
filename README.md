# REDMENACEMONEY

Paula's Finance App — budget, net worth, debt payoff and goal tracker for the Gullen household.

## What's in here

- `index.html` — the app itself (all HTML/CSS/JS, no build step)
- `manifest.json` — makes it installable as a phone app (PWA)
- `sw.js` — lets it work offline once it's been loaded once
- `icon-192.png`, `icon-512.png`, `icon-512-maskable.png` — home screen icons

Data is saved right on the device: it uses Claude's storage when opened inside a Claude
artifact, and your browser's local storage automatically when hosted anywhere else (like
GitHub Pages below) — no setup needed either way. It stays on that one device/browser; there's
no server, so Ken and Paula's phones won't automatically see each other's entries.

## Put it on Paula's phone (GitHub Pages — free, no server needed)

1. On GitHub, go to this repo's **Settings → Pages**.
2. Under "Build and deployment", set **Source: Deploy from a branch**, branch **main**, folder **/ (root)**.
3. Save. GitHub will publish it at:
   `https://kengineer927518.github.io/REDMENACEMONEY/`
   (takes a minute or two after the first push)
4. On Paula's phone, open that link in Chrome (Android) or Safari (iPhone).
5. Install it:
   - **Android/Chrome:** tap the ⋮ menu → **Add to Home screen** / **Install app**
   - **iPhone/Safari:** tap the Share icon → **Add to Home Screen**

It'll behave like a normal app icon from there — full screen, no browser bar, works offline
after the first load.

## Loading Paula's real numbers

This repo's `index.html` starts completely blank (all $0) on purpose — it's public, so no real
balances live in the code. There's a separate `starter-data.json` file with the real numbers
that was **not** committed here. Send that file to Paula directly (text, email, AirDrop —
whatever's easiest) instead of putting it in the repo. The first time she opens the app:

1. Go to the **Dashboard** tab.
2. Under "Your data," tap **Import data (.json)**.
3. Pick the `starter-data.json` file.

That's a one-time thing — after that her entries save automatically on her phone. She can also
tap **Export data** any time to save a backup of wherever things stand.

## Updating it later

Any time the budget, debts, or goals logic changes, just replace `index.html` and push again —
GitHub Pages picks up the new version automatically.
