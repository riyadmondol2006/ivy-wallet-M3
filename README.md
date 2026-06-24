[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/riyadmondol2006/ivy-wallet-M3)](https://github.com/riyadmondol2006/ivy-wallet-M3/releases)
[![Fork](https://img.shields.io/github/forks/riyadmondol2006/ivy-wallet-M3?logo=github&style=social)](https://github.com/riyadmondol2006/ivy-wallet-M3/fork)

# Ivy Wallet M3 — Personal Fork

This is a personal fork of the now-archived [Ivy-Apps/ivy-wallet](https://github.com/Ivy-Apps/ivy-wallet), actively developed and expanded over time. The original project was discontinued by its maintainers in November 2024. This fork picks up where it left off and builds on it with a full Material 3 redesign, new features, and modern Android motion.

> This fork is a work in progress. New features, fixes, and improvements are added continuously.

---

## What's New (vs upstream)

### Material 3 Redesign
- Full app-wide conversion to **Material 3** components and tokens — `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes` replace the legacy `UI.colors`/`UI.typo`/`UI.shapes` design system everywhere
- New **M3 Expressive** design layer: `IvyExpressiveType` (expressive type scale), `IvyExpressiveShapes` (morphable shape system), `IvyThemeController` (dynamic theme switching at runtime), `AppearanceCard` (in-app appearance picker)
- Dual design-system bridge keeps the remaining legacy screens visually coherent while migration continues
- `IvyMaterial3Theme` wraps the full app in a single M3 `MaterialTheme` with dynamic colour support

### Credit Cards Feature
- Accounts now support an optional **credit limit** (`creditLimit: Double?`), turning any account into a credit card account
- Room DB migrated to **v131** (`Migration130to131_AccountCreditLimit`) — adds `creditLimit` column with a safe default
- **Credit Cards section** on the Accounts tab — shows each card's current owed amount, limit left, and a progress bar
- **Mark as Paid** flow — clears the balance of a credit card with a single tap
- **Home summary card** — when credit cards exist, the Home tab shows total balance and total credit exposure separately
- Feature flag (`creditCardsEnabled`) enabled by default

### Settings Redesign
- Full **Material 3** layout — cards, dividers, and spacing follow M3 conventions
- **Removed** the "Rate us on Google Play" button
- **Removed** the entire "Product" section (Telegram, Help Center, Releases, Report Bug, Request Feature, Contact Support, Contributors, Attributions, Terms & Privacy)
- **Share** now shares this GitHub repo (`https://github.com/riyadmondol2006/ivy-wallet-M3`) instead of the Play Store link
- **Open source** button opens this repo directly
- Credit Cards feature flag moved to the Features section

### Motion System
- **`IvyMotion`** — central motion spec object (`shared/ui/core`) with named spring constants, shared-axis enter/exit transitions, section expand/collapse specs, and `animateItem` / `animateContentSize` helpers. Single import for consistent physics-based motion across all modules
- **Screen transitions** — shared-axis X: forward navigation slides in from the right, back slides from the left, driven by `Navigation.isBack`
- **Predictive back gestures** — `android:enableOnBackInvokedCallback="true"` + `OnBackPressedDispatcher.addCallback` (replaces the deprecated `onBackPressed()` override). Works correctly with legacy modals and sheets
- **Animated tab switch** — Home ↔ Accounts cross-slide instead of snapping
- **Transaction list** — `animateItem` on all transaction rows; Upcoming/Overdue sections expand/collapse with `AnimatedVisibility` + chevron rotation
- **Credit card surfaces** — `animateContentSize` on the credit section; number and progress bar tween when marked paid; Home summary card animates in/out with `expandVertically + fadeIn`
- FAB → Edit Transaction **container transform** (shared element) preserved throughout

### Repo & Build
- Version bumped to **2026.06.24** (code 207)
- Application ID: `com.ivym3.wallet`
- Git remote updated to `https://github.com/riyadmondol2006/ivy-wallet-M3`
- Cleaned up generated/junk files from the repo root

---

## Tech Stack

### Core
- 100% [Kotlin](https://kotlinlang.org/) 2.0.20
- 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) 1.9.0
- [Material 3](https://m3.material.io/) 1.4.0
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) 1.9.0
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)
- [Hilt](https://dagger.dev/hilt/) 2.52 (DI)
- [ArrowKt](https://arrow-kt.io/) 1.2.4 (functional programming)

### Local Persistence
- [Room DB](https://developer.android.com/training/data-storage/room) 2.7.1 (SQLite ORM, current schema v131)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (key-value storage & feature flags)

### Networking
- [Ktor client](https://ktor.io/docs/getting-started-ktor-client.html) 2.3.13
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) 1.7.3

### Build
- [Gradle KTS](https://docs.gradle.org/current/userguide/kotlin_dsl.html) + version catalogs
- AGP 8.6.0, minSdk 28, compileSdk/targetSdk 35, JVM target 17
- [Detekt](https://github.com/detekt/detekt) 1.23.8 (linter)
- [Fastlane](https://fastlane.tools/) (build automation)

### Monitoring
- [Firebase Crashlytics](https://firebase.google.com/products/crashlytics)
- [Timber](https://github.com/JakeWharton/timber)

---

## CI / Auto-Release (GitHub Actions)

Workflows run on every push to `main` and on pull requests. All jobs use **Temurin Java 17** (matching the build's JVM target), and each CI workflow cancels superseded runs on the same branch/PR (`concurrency` + `cancel-in-progress`) for faster feedback and fewer wasted minutes.

### Release flow — one click

Releases are fully on-demand via a single `release.yml` workflow. There is **no cron and no bot PR**.

1. Go to **Actions → Release → Run workflow**.
2. Set **version** to a semantic version like `2.1.0`, or leave it as `auto` to patch-bump the latest tag (the first release defaults to `1.0.0`). Optionally tick **prerelease**.
3. Click **Run**. That one run:
   - bumps `version-name` (semantic) and auto-increments `version-code` in `gradle/libs.versions.toml`;
   - commits `Release v<version> (<code>) [skip ci]` and tags `v<version>` on `main`;
   - decodes your signing keystore and builds the signed `assembleRelease` APK;
   - publishes a GitHub Release titled `Ivy Wallet M3 v<version>` with an **auto-generated changelog** (commits since the previous tag) and the signed APK attached.

To install: open the new Release and download **app-release.apk** onto your device (API 28+). Releases are serialized (`concurrency: release`) so two never run at once. The bump commit carries `[skip ci]` so it doesn't re-run the push CI jobs — the signed release build is the validation.

### Required GitHub Secrets

Go to **Settings → Secrets and variables → Actions** in your fork and add these four secrets:

| Secret | How to get it |
|--------|--------------|
| `SIGNING_KEYSTORE` | Base64-encoded `.jks` file: `base64 -i sign.jks \| pbcopy` (macOS) |
| `SIGNING_STORE_PASSWORD` | The keystore password |
| `SIGNING_KEY_ALIAS` | The key alias inside the keystore |
| `SIGNING_KEY_PASSWORD` | The key password |

Once the secrets are in place, the **Release** workflow decodes your keystore, builds `assembleRelease`, and attaches the signed APK to a new GitHub Release tagged `v<version>`.

### Workflows at a glance

Just two workflows — lean and focused:

| Workflow | Triggers | What it does |
|----------|----------|-------------|
| `ci.yml` | PR, push to main | Parallel jobs: **detekt**, **unit tests**, **Android lint** (release), and **build** (demo APK artifact). Cancels superseded runs. |
| `release.yml` | Manual (`Run workflow`) | Builds the **signed APK first**, then bumps version, commits + tags, and publishes a GitHub Release with auto-changelog. |

The old upstream community workflows (issue/stale bots, PR-description check, screenshot/emulator/compose-stability checks, wrapper-upgrade) and their `ci-actions/` helper modules have been removed. The legacy `fastlane/` directory is unused and can be ignored.

---

## Building

**Requirements:** Java 17, Android SDK (API 35), Android Studio Meerkat or later.

```bash
# Debug build
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=~/Library/Android/sdk \
  ./gradlew :app:assembleDebug --no-configuration-cache

# Release-quality build (minified, debug-signed)
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=~/Library/Android/sdk \
  ./gradlew :app:assembleDemo --no-configuration-cache

# Install on connected device
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=~/Library/Android/sdk \
  ./gradlew :app:installDebug --no-configuration-cache
```

For a production-signed release, place your `sign.jks` in the project root and provide `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, and `SIGNING_KEY_PASSWORD` as environment variables, then run `:app:assembleRelease`.

> **Build speed:** the Gradle build cache and parallel execution are enabled by default (`org.gradle.caching`, `org.gradle.parallel` in `gradle.properties`), so warm builds are substantially faster than a clean build. `--no-configuration-cache` is still required because the root `module.graph.assertion` plugin is not configuration-cache compatible — the cache + parallel flags deliver the speedup without it.

---

## Roadmap

This fork will continue to grow. Planned areas include:

- Completing the M3 migration on all remaining legacy screens
- Expanding the credit cards feature (statements, payment reminders)
- UI polish and accessibility improvements
- Further motion refinements as M3 Expressive APIs stabilise in stable releases

---

## License

[GPL-3.0](LICENSE) — forked from [Ivy-Apps/ivy-wallet](https://github.com/Ivy-Apps/ivy-wallet) in accordance with the original license. Original project copyright Ivy Apps Ltd.
