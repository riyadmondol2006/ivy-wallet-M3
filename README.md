[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/riyadmondol2006/ivy-wallet-M3)](https://github.com/riyadmondol2006/ivy-wallet-M3/releases)
[![Fork](https://img.shields.io/github/forks/riyadmondol2006/ivy-wallet-M3?logo=github&style=social)](https://github.com/riyadmondol2006/ivy-wallet-M3/fork)

<p align="center">
  <img src="branding/ivy-wallet-m3-logo.svg" alt="Ivy Wallet M3 logo" width="128" height="128"/>
</p>

# Ivy Wallet M3 — Personal Fork

This is a personal fork of the now-archived [Ivy-Apps/ivy-wallet](https://github.com/Ivy-Apps/ivy-wallet), actively developed and expanded over time. The original project was discontinued by its maintainers in November 2024. This fork picks up where it left off and builds on it with a full Material 3 redesign, new features, and modern Android motion.

> This fork is a work in progress. New features, fixes, and improvements are added continuously.

---

## What's New (vs upstream)

### Material 3 Redesign
- App-wide **Material 3** theming — `MaterialTheme.colorScheme`, `MaterialTheme.typography` and `MaterialTheme.shapes` are the source of truth; the legacy `UI.colors`/`UI.typo`/`UI.shapes` tokens are bridged onto the M3 theme so screens that still use them render with the same colours, type scale and corner radii
- New **M3 Expressive** design layer: `IvyExpressiveType` (expressive type scale), `IvyExpressiveShapes` (morphable shape system), `IvyThemeController` (dynamic theme switching at runtime), `AppearanceCard` (in-app appearance picker)
- Dual design-system bridge keeps the remaining legacy screens visually coherent while migration continues
- `IvyMaterial3Theme` wraps the full app in a single M3 `MaterialTheme` with dynamic colour support

### Credit Cards Feature
- Accounts now support an optional **credit limit** (`creditLimit: Double?`), turning any account into a credit card account
- Room DB migrated to **v132** (`Migration130to131_AccountCreditLimit`, `Migration131to132_DualCurrencyCards`) — adds the credit limit and dual-currency card columns with safe defaults
- **Credit Cards section** on the Accounts tab — shows each card's amount to pay and limit left per currency, plus the total owed in the main currency at the bank rate for dual-currency cards
- **Pay** flow — records a transfer from a paying account to the card (partial and cross-currency payments supported); **Reset** clears a card's balance with a confirmed adjustment
- **Home summary card** — when credit cards exist, the Home tab lists every card with its per-currency debt and converted total, and an "All cards" footer when there are several
- **Billing cycle** — optional statement day and payment due day per card; cards show "Payment due in N days" / "Overdue since …", and a reminder fires when a payment is due within three days
- **Include in balance** per card — card debt can count against the total balance and card spending appears in the income/expense statistics (default on)
- Feature flag (`creditCardsEnabled`) enabled by default; turning it off keeps card accounts in the normal list

### Reminders, undo and history
- **Planned-payment reminders** — a daily notification (09:00) for planned payments due today or overdue, plus an immediate check when a rule is saved; tapping it opens Planned payments. Separate switch in Settings; the Android 13+ notification permission is requested when needed
- **Undo delete** — deleting a transaction, skipping a planned payment, deleting a planned rule or a budget shows a Material 3 snackbar with **Undo** instead of a confirmation dialog
- **Balance over time** — the Balance screen draws a line chart of the total balance (1M / 3M / 6M / 1Y / All), tap a point for its date and value

### Design system
- One Material 3 **component kit** in `shared/ui/core` (`IvyTopBar`, `IvyScreenTitle`, `IvyBottomActionBar`, `IvyListCard`, `IvySearchField`, `IvyConfirmDialog`, `IvyCloseButton`, `BackButton`, `IvyLineChart`) used by both new and legacy screens
- Legacy `UI.typo` / `UI.shapes` tokens are **bridged** onto the M3 type and shape scales, legacy modals use the M3 sheet shape, scrim, drag handle and spring motion, and the legacy buttons follow the dynamic colour scheme
- One date/time picker (Compose Material 3) everywhere; `Theme.AUTO` is the default and status-bar contrast follows the resolved scheme
- Themed home-screen widget (Glance `GlanceTheme`, Material You on Android 12+)

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
- **Predictive back gestures** — real finger-following predictive back: `NavigationRoot` drives a `SeekableTransitionState` via Compose `PredictiveBackHandler`, so the back gesture *scrubs* the screen change. Gated to plain non-legacy screen pops so legacy modals/sheets keep their existing back handling
- **Animated tab switch** — Home ↔ Accounts cross-slide instead of snapping
- **Transaction list** — `animateItem` on all transaction rows; Upcoming/Overdue sections expand/collapse with `AnimatedVisibility` + chevron rotation
- **Credit card surfaces** — `animateContentSize` on the credit section; number and progress bar tween when marked paid; Home summary card animates in/out with `expandVertically + fadeIn`
- **Add-transaction flow** — the FAB → Edit Transaction container transform was removed in favour of a clean shared-axis slide; the add-options bottom sheet animates fully closed before navigating (no overlapping transitions)

### Home "More" Menu Redesign
- The old circular-reveal overlay (a `Canvas` circle growing out of the chevron, legacy `CircleButtonFilled` buttons + the deprecated `BufferBattery`) was **fully replaced** with a Material 3 slide-up panel
- Top bar (Close · "More" · Sync), an M3 search field, a tonal **quick-access tile grid**, a prominent **Cloud Sync** card, a savings-goal card with an M3 `LinearProgressIndicator`, and the open-source fork card
- The open-menu chevron moved into the header as a real `IconButton`, so it no longer overlaps the manual-sync icon

### Cloud Sync (Upstash Redis)
- Bring-your-own **Upstash Redis** backup + sync — paste your REST URL + token (HTTPS) or a `rediss://` endpoint (TCP); the full `BackupDataUseCase` JSON is stored in your own database
- **Manual** or **Auto** modes; auto-sync debounces data changes and retries failures via WorkManager
- Cross-device pull prompt on app open (only when another device wrote the latest cloud state), test-then-add connection gating, and an onboarding "Restore from Cloud" path
- Easy access from **two** places: the Home header sync icon and the "Sync now" card in the More menu

### Repo & Build
- App **renamed to "Ivy Wallet M3"** (launcher label across all build types)
- **New adaptive launcher icon** — a white veined ivy leaf on a green gradient squircle, authored as Android vector drawables (foreground / background / monochrome themed-icon layers) with an SVG master in [`branding/`](branding/ivy-wallet-m3-logo.svg)
- Version name and code live in `gradle/libs.versions.toml` and are bumped by the release workflow
- Release/debug APKs signed with **all four signature schemes** (v1 JAR + v2/v3/v4 APK Signature Scheme)
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
- AGP 9.1.1, minSdk 28, compileSdk/targetSdk 37, JVM target 17
- [Detekt](https://github.com/detekt/detekt) 1.23.8 (linter)
- [Fastlane](https://fastlane.tools/) (build automation)

### Logging
- [Timber](https://github.com/JakeWharton/timber) (no crash reporting or analytics; nothing leaves the device except your own cloud sync)

---

## CI / Auto-Release (GitHub Actions)

Workflows run on every push to `main` and on pull requests. All jobs use **Temurin Java 21** for the Gradle and screenshot-test tooling (the app's JVM bytecode target remains 17), and each CI workflow cancels superseded runs on the same branch/PR (`concurrency` + `cancel-in-progress`) for faster feedback and fewer wasted minutes.

### Release flow — automatic on every push

Every push to `main` is released by the single `release.yml` workflow. There is **no manual step, no cron and no bot PR**. Each run:

- patch-bumps `version-name` from the latest `v*` tag (the first release defaults to `1.0.0`) and auto-increments `version-code` in `gradle/libs.versions.toml`;
- runs detekt and the unit tests, then decodes your signing keystore and builds the signed `assembleRelease` APK (v1/v2/v3/v4 schemes);
- only after the build succeeded, commits `Release v<version> (<code>) [skip ci]` and tags `v<version>` on `main`;
- publishes a GitHub Release titled `Ivy Wallet M3 v<version>` with an **auto-generated changelog** (commits since the previous tag) and the signed APK attached.

**Actions → Release → Run workflow** is still available when you want an explicit version such as `2.1.0` (a minor or major bump) or a pre-release.

To install: open the new Release and download **app-release.apk** onto your device (API 28+). Releases are serialized (`concurrency: release`) so two never run at once; a push that lands during a release is queued and released next. The bump commit carries `[skip ci]` and is pushed with the Actions token, so it triggers neither CI nor another release — the signed release build is the validation.

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
| `release.yml` | Push to main (auto patch bump), or manual `Run workflow` for an explicit version | Builds the **signed APK first**, then bumps version, commits + tags, and publishes a GitHub Release with auto-changelog. |

The old upstream community workflows (issue/stale bots, PR-description check, screenshot/emulator/compose-stability checks, wrapper-upgrade) and their `ci-actions/` helper modules have been removed. The legacy `fastlane/` directory is unused and can be ignored.

---

## Cloud Backup & Sync (optional)

Ivy Wallet M3 can back up and sync your data to **your own** [Upstash Redis](https://upstash.com)
database — no third-party server holds your data, and it's free on Upstash's tier. The whole
backup is the same JSON the local export produces, stored under the `ivy_wallet_backup` key.

**Set it up:**
1. Create a free account at [upstash.com](https://console.upstash.com) and make a new Redis database.
2. Open the database and copy either the **REST URL + token** (HTTPS) or the `rediss://` endpoint +
   password (TCP).
3. In the app, go to **Settings → Cloud Sync** (or the **Sync now** card in the Home "More" menu, or
   the **Restore from Cloud** option during onboarding).
4. Pick the connection type, paste your credentials, tap **Test connection**, then **Add database**.
5. Choose a sync mode:
   - **Manual** — data is pushed only when you tap the sync icon (Home header) or the Sync card.
   - **Auto** — every change is pushed automatically (debounced; failed pushes retry via WorkManager).

**Multi-device:** on app open, if your cloud backup was last written by *another* device, the app
offers to pull those changes. The device that wrote the latest state is never prompted to pull its
own data. Tokens are stored locally in DataStore; nothing is sent anywhere except your own database.

---

## Building

**Requirements:** Java 21, Android 17 SDK (API 37.0), SDK Build Tools 37.0.0, and Android Studio Panda 3 Patch 1 or later. The Gradle wrapper supplies Gradle 9.3.1 with Android Gradle Plugin 9.1.1. Minimum supported Android remains API 28 (Android 9).

```bash
# Debug build
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=~/Library/Android/sdk \
  ./gradlew :app:assembleDebug --no-configuration-cache

# Release-quality build (minified, debug-signed)
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=~/Library/Android/sdk \
  ./gradlew :app:assembleDemo --no-configuration-cache

# Install on connected device
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=~/Library/Android/sdk \
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
