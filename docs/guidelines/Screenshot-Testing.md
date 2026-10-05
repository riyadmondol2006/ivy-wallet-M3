# Screenshot testing (Paparazzi)

Screenshot tests live next to unit tests (`src/test`) and extend `PaparazziScreenshotTest` from
`shared/ui/testing`. Every test renders in both `PaparazziTheme.Light` and `PaparazziTheme.Dark`.

## Commands

```bash
# Compare the current UI against the committed snapshots; diffs land in
# <module>/build/reports/paparazzi/
./gradlew verifyPaparazziDebug --no-configuration-cache

# Re-record the snapshots after an intentional UI change (one module or all of them)
./gradlew :feature:accounts:recordPaparazziDebug --no-configuration-cache
./gradlew recordPaparazziDebug --no-configuration-cache
```

CI runs `testDebugUnitTest`, which executes the snapshot tests without pixel verification: a
composable that throws fails CI, a visual change does not. Run `verifyPaparazziDebug` locally
before you commit UI work and record once per change, not per hunk.

## Conventions

- Keep snapshots deterministic: pass fixed dates (`today = LocalDate.of(...)`) and fixed data
  into the composable; never read the clock or `LocalTimeProvider` inside a snapshotted
  component without a parameter the test can control.
- Design-system components get a test in `shared/ui/core/src/test/java/com/ivy/ui/component/`
  (see `IvyComponentsPaparazziTest`), so a token change is reviewed in one place.
- Snapshot files are named `<Test>_<method>[Theme].png`; delete stale images when you rename a
  test.

## Known environment issue

`SearchPaparazziTest` can fail with `NoSuchMethodError: Thread.setPosixNicenessInternal` on some
JDK 21 builds: layoutlib's `HandlerThread` delegate calls a JDK-internal method that newer JDK
patch releases removed. It is not a product bug; use the JDK pinned by CI (Temurin 21) locally.
