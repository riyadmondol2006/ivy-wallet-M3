# Contributing to Ivy Wallet M3

This is a personal fork of the archived [Ivy-Apps/ivy-wallet](https://github.com/Ivy-Apps/ivy-wallet). Contributions, bug reports, and ideas are welcome via [Issues](https://github.com/riyadmondol2006/ivy-wallet-M3/issues) and [Pull Requests](https://github.com/riyadmondol2006/ivy-wallet-M3/pulls).

## Getting Started

**1. Fork and clone**

```bash
git clone https://github.com/riyadmondol2006/ivy-wallet-M3.git
cd ivy-wallet-M3
```

**2. Open in Android Studio** (Meerkat or later recommended)

**3. Build requirements**
- Java 17
- Android SDK API 35
- minSdk 28

**4. Run a debug build**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=~/Library/Android/sdk \
  ./gradlew :app:installDebug --no-configuration-cache
```

## Development Tips

- Keep changes focused — one concern per PR
- Build and test on a real device before opening a PR
- Run detekt before submitting: `./gradlew detekt`
- The M3 migration is ongoing — prefer `MaterialTheme.*` tokens over the legacy `UI.*` system in any new or edited composables
- New animation code should import from `IvyMotion` (in `shared/ui/core`) for consistent motion

## Architecture

The app is a multi-module Gradle project. Key modules:

| Path | Purpose |
|------|---------|
| `app/` | Application entry point, `RootActivity`, DI wiring |
| `feature/*` | One module per screen/feature |
| `shared/ui/core` | Design system: `IvyMaterial3Theme`, `IvyMotion`, M3 Expressive layer |
| `shared/ui/navigation` | `Navigation`, `NavigationRoot`, shared-element transition infra |
| `shared/domain` | Feature flags (`IvyFeatures`), use-cases |
| `shared/data/core` | Room DB, DAOs, migrations |
| `temp/legacy-code` | Legacy composables being migrated to M3 (avoid adding new code here) |
| `temp/old-design` | Old design system (do not use in new code) |
