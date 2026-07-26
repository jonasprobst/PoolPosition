# Pool Position

A tiny Android app that watches web pages and sends a high-priority notification
when one changes — built to catch a swim-class registration the moment it opens.

It periodically fetches each watched page, strips it to visible text, and fires
an alert when a trigger condition is met. Tapping the notification opens the page
in your browser.

## Features

- A list of **watches**, each with a label, URL, check interval (min 15 min),
  trigger mode, and an on/off toggle. Add / edit / delete in the app.
- Three **trigger modes** (all transition-based — the first check only records a
  baseline and never alerts):
  - `changed` — the page's visible text changed from the baseline.
  - `appears` — a keyword went from absent to present.
  - `disappears` — a keyword went from present to absent.
- A **WorkManager** job runs every 15 minutes and checks each enabled watch whose
  interval has elapsed. Low battery impact; no foreground service.
- **High-priority, vibrating notification** on trigger; tap to open the URL.
- Pre-seeded example: a **Test page** watch pointing at this repo's
  [GitHub Pages test page](https://jonasprobst.github.io/PoolPosition/), mode
  `disappears`, keyword *"Anmeldung noch nicht möglich"* — so a fresh install is
  testable immediately. Swap its URL to <https://anmeldung.bernschwimmt.ch/> for
  the real Bern registration watch (same mode/keyword: the phrase shows on every
  course while registration is closed and disappears when it opens).
- **Check now** action (overflow menu) to force an immediate check of all
  watches without waiting for the interval — handy for testing.

## Design decisions

Kept deliberately small, because the only build path is CI (see below) and every
extra dependency is a chance to break it.

- **Kotlin + Jetpack Compose** (Material 3) for the UI.
- **Minimal dependencies**: Compose, WorkManager, `androidx.core`, and JUnit
  (test-only). No OkHttp, Jsoup, Room, DataStore, ViewModel, or material-icons.
  - HTTP: `java.net.HttpURLConnection`.
  - HTML → text: standard-library regex stripping.
  - Storage: a single JSON file via the platform's built-in `org.json` (no
    database).
- `minSdk 26`, `compileSdk/targetSdk 35`, package `ch.poolposition.app`.

## Architecture

```
core/
  HttpFetcher     plain GET, gzip + charset aware, never throws
  HtmlText        HTML -> visible text (standard library only)
  TriggerEngine   pure, side-effect-free trigger evaluation (unit-tested)
model/
  Watch, TriggerMode
data/
  WatchStore      load/save the watch list as JSON; seeds the example watch
work/
  CheckWorker     for each due, enabled watch: fetch -> strip -> evaluate -> alert
  CheckScheduler  the 15-min PeriodicWorkRequest, plus checkNow()
notify/
  Notifier        notification channel + high-priority alert
ui/
  AppScreen, WatchEditor, Theme   Compose UI
PoolPositionApp   creates the channel, seeds the store, schedules on startup
```

The trigger logic and HTML stripping have no Android or network imports, so they
run as fast JVM unit tests in CI.

## Building (CI is the source of truth)

There is no local Android SDK requirement — every push builds a **debug APK** on
GitHub Actions and uploads it as an artifact.

1. Push to any branch (or use the **Actions** tab → *Build APK* → *Run workflow*).
2. Open the run under the [**Actions**](../../actions) tab.
3. Download the **`poolposition-debug-apk`** artifact and unzip to get
   `app-debug.apk`.

The committed Gradle wrapper pins the toolchain (Gradle 8.11.1, AGP 8.7.3,
Kotlin 2.0.21). CI runs on `ubuntu-latest` with Temurin JDK 17 and runs the unit
tests before assembling.

To build locally (if you do have the Android SDK):

```bash
./gradlew testDebugUnitTest assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Installing

Sideload `app-debug.apk` (enable "install unknown apps" for your file manager or
browser). On first launch:

1. Grant the **notifications** permission (Android 13+).
2. Open **⋮ → Battery settings** and set battery usage to **Unrestricted** so
   WorkManager fires on time (Android otherwise defers background work in Doze).

## Testing it

Waiting 15 minutes per check is slow, so:

- **Check now** (⋮ menu) forces an immediate check of all enabled watches. Use a
  watch pointed at a page you can change, tap *Check now* once to set the
  baseline, change the page, then *Check now* again — the notification should
  fire.
- A **test page** you control makes this repeatable. This repo can publish one
  via GitHub Pages (see the `gh-pages` branch / `GET-STARTED-TESTING.md` there):
  point a `changed` watch at your Pages URL, edit the page, and re-check.

Note: real-world timing depends on the OS. The 15-minute interval is a floor,
not a guarantee — Android batches background work, so scheduled fires can drift,
especially in Doze. "Unrestricted" battery helps most.

## Permissions

- `INTERNET` — fetch watched pages.
- `POST_NOTIFICATIONS` — deliver alerts (requested at runtime on Android 13+).
