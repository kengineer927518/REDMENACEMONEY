# Red Menace Money — Android app

A native Kotlin/Jetpack Compose version of the household finance tracker, built the same way
as ON TRACK: no local Android Studio build required — push to `main` and GitHub Actions
compiles a debug APK you can download and sideload.

## What's in here

- `app/src/main/java/com/gullen/redmenacemoney/data/` — Models, the amortization/paycheque/goal
  math (verified against the CIBC mortgage statement figures), and SharedPreferences+JSON
  persistence (`Store.kt`, same shape as ON TRACK's `Store.kt`/`CrewStore`)
- `app/src/main/java/com/gullen/redmenacemoney/ui/` — the six screens (Dashboard, Paycheque,
  Budget, Debts, Net Worth, Goals) plus shared components and the rail/ledger theme
- `.github/workflows/build-apk.yml` — builds the debug APK on every push to `main`

## Getting the APK

1. Push changes under this `/android` folder to `main` (the workflow only triggers on changes
   here, so unrelated web-app pushes won't waste a build).
2. Go to the repo's **Actions** tab on GitHub.
3. Open the latest "Build debug APK" run (or click "Run workflow" to trigger it manually).
4. Once it finishes (green check), scroll to **Artifacts** at the bottom of the run page and
   download `RedMenaceMoney-debug-apk`. It's a zip containing `app-debug.apk`.
5. Send that APK to the phone (AirDrop, email, USB) and install it — you'll need to allow
   installs from that source the first time, same as any sideloaded app.

## Notes

- **No data starts pre-filled.** Every balance defaults to $0, matching the sanitized public
  web version — no real numbers are baked into this repo's source. Use the Dashboard's Export
  on the web version (or hand-edit a JSON backup) to get real data into this app; a matching
  Import button on this app's Dashboard reads the same JSON shape as the web version's
  `starter-data.json`.
- **Data lives on-device only**, in SharedPreferences — nothing leaves the phone.
- Opening this in Android Studio locally: it'll prompt to generate the Gradle wrapper
  automatically on import (or run `gradle wrapper` once if you have a Gradle install). The
  Actions workflow doesn't need the wrapper since it installs Gradle 8.7 directly.
- Debug builds are signed with the default debug key, which is fine for sideloading but not
  for the Play Store. If that's ever the goal, a release signing config is a small addition.
