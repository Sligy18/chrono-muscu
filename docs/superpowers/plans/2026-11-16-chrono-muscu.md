# Chrono Muscu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A private Android rest timer (6 presets, set counter, end sound/vibration) built and signed on GitHub Actions.

**Architecture:** Single-module Compose app. A foreground `TimerService` owns the countdown (end time on `elapsedRealtime`) and exposes a `StateFlow`; the UI observes it. Settings live in DataStore. End sounds are synthesized PCM, no audio files.

**Tech Stack:** AGP 8.13.2, Gradle 8.14.3 (installed by CI, no wrapper jar), Kotlin 2.2.21 + Compose compiler plugin, Compose BOM 2025.09.01 (Material 3), activity-compose 1.11.0, core-ktx 1.17.0, lifecycle-runtime-compose 2.9.4, datastore-preferences 1.1.7, JUnit 4.13.2.

**Spec:** `docs/superpowers/specs/2026-11-16-chrono-muscu-design.md`

## Global Constraints
- applicationId / namespace `fr.gillesray.chronomuscu`; minSdk 26, compileSdk 36, targetSdk 36; JVM target 17.
- App name « Chrono Muscu »; UI strings in French.
- Default presets (seconds): 25, 60, 90, 120, 180, 240.
- Sounds: `BIP`, `BOIS`, `AUCUN`. Defaults: sound BIP, vibrate on, duck on.
- No internet permission, no analytics, no ads, no third-party audio or icon.
- Release APK signed from secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; unsigned debug build if secrets absent.

## Review Focus
- Screen off during a 4'00" rest: sound must still fire on time -> service holds a partial wake lock for the countdown (Task 3).
- Pressing another preset while running: restart with the new duration, no double sound (Task 3, `TimerService.start` cancels the previous job).
- Set counter below zero: clamps at 0 (`Series.adjust` test, Task 1).
- Notification permission refused (Android 13+): timer and sound still work, only the notification is hidden (Task 4).
- Preset edited to 0:00: rejected, minimum 5 s (`Presets.clamp` test, Task 1).

---

### Task 1: Project skeleton + pure logic

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**`, `app/src/main/java/fr/gillesray/chronomuscu/Logic.kt`, `app/src/test/java/fr/gillesray/chronomuscu/LogicTest.kt`

**Produces:**
- `fun formatClock(ms: Long): String` -> remaining time, rounded up to the second: `"1:30"`, `"0:05"`, `"0:00"` for <=0.
- `fun formatPreset(seconds: Int): String` -> `25"`, `1'00"`, `1'30"`.
- `fun remainingMs(endAt: Long, now: Long): Long` -> `max(0, endAt - now)`.
- `object Series { fun adjust(current: Int, delta: Int): Int }` clamps at 0.
- `object Presets { val DEFAULTS = listOf(25,60,90,120,180,240); fun clamp(s: Int): Int }` clamps to 5..5999.
- `enum class EndSound { BIP, BOIS, AUCUN }`

- [ ] Tests: `formatClock(90_000)=="1:30"`, `formatClock(4_001)=="0:05"`, `formatClock(-3)=="0:00"`; `formatPreset(25)=="25\""`, `formatPreset(90)=="1'30\""`; `remainingMs(1000,1500)==0`; `Series.adjust(3,-5)==0`, `Series.adjust(3,5)==8`; `Presets.clamp(0)==5`.
- [ ] Implement, verified by CI `gradle test`.

### Task 2: Settings store
**Files:** `SettingsRepository.kt`
**Produces:** `data class Settings(presets: List<Int>, sound: EndSound, vibrate: Boolean, duck: Boolean, series: Int)`; `class SettingsRepository(context)` with `val settings: Flow<Settings>`, `suspend fun setPreset(index: Int, seconds: Int)`, `setSound`, `setVibrate`, `setDuck`, `setSeries`. DataStore name `"settings"`.

### Task 3: Timer service + end alert
**Files:** `TimerService.kt`, `EndAlert.kt`
**Produces:** `TimerService.state: StateFlow<TimerState>` (`data class TimerState(running: Boolean, endAt: Long, totalMs: Long)`), `TimerService.start(context, seconds)`, `TimerService.stop(context)`. Foreground type `specialUse`, channel `"timer"`, notification updated every second with `formatClock`. `EndAlert.play(context, sound, vibrate, duck)`: synthesizes PCM (BIP: 3 x 880 Hz sine 150 ms; BOIS: 3 short decaying 1 kHz/1.6 kHz clicks), plays with `AudioTrack` (`USAGE_MEDIA`, `CONTENT_TYPE_SONIFICATION`), requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` if duck, vibrates 3 pulses if vibrate.

### Task 4: UI
**Files:** `MainActivity.kt`, `ui/Theme.kt`, `ui/TimerScreen.kt`, `ui/SettingsScreen.kt`
- Timer screen: big `formatClock`, circular progress, Stop button, 6 preset buttons (2x3, 3x2 in landscape), set counter with -5 -1 +1 +5. Settings icon opens settings screen (simple in-activity state, back handler returns).
- Settings: 6 preset rows opening a minutes/seconds stepper dialog (seconds step 5), sound radio (Bip/Bois/Aucun) with test-play, vibrate switch, duck switch.
- Requests `POST_NOTIFICATIONS` on first preset tap (API 33+); refusal does not block the timer.
- Edge-to-edge, dynamic color on API 31+.

### Task 5: CI build + release
**Files:** `.github/workflows/build.yml`, `.gitignore`, `README.md`
- On push to main: setup-java 17, `gradle/actions/setup-gradle` with `gradle-version: 8.14.3`, `gradle test assembleRelease`, publish `app-release.apk` as GitHub Release `v1.0.<run_number>` via `gh release create`.
- Keystore: PKCS12 generated locally with openssl, base64 into secrets; local copy in `signing/` (gitignored).
