Run `python3 tests/hook-runtime-lifecycle/run.py` with JDK 21+ and a Kotlin compiler
(`KOTLIN_HOME`; the local Android Studio compiler is the default fallback).
The Java regressions also compile the real Android SDK37 `org/json` sources from
`ANDROID_HOME/sources/android-37.0/org/json` (or the `HYPERCRAFT_JSON_SOURCE`
override). Android compatibility annotations are the only adapters around JSON.

The runner compiles the actual ContextUtils, ThreadPoolManager, BackupUtils,
PropUtils, VolumeDefaultStream, AllowAutoStart, HostExecutor and MonetThemeOverlay
sources. Fixtures supply Android services, hook callback registration and preferences. It checks delayed context polling without CPU spin or shared-pool
shutdown, context fallback/cancellation, backup data types and stream closure,
POSIX literal quoting, live preference reads, nested/concurrent auto-start scopes,
executor cancellation and recreation after cleanup, and optional Monet class and
method overload handling.

Backup coverage serializes and parses real JSON through both Activity and Context
overloads. It verifies integral/fractional floats, signed zero, float extremes,
small/large long values, ordinary preference types, legacy bare JSON numbers,
malformed-input stream closure and exclusion of runtime gate/identity keys. A
regression without numeric tagging fails because SDK37 parses `1.0f` as Integer.
These are host regressions, not Android Binder, ART hook or device lifecycle
validation. Secondary-package dispatch and restoration of the primary
process target are covered separately by `tests/os4-init-prefs/run.py` using the
real EzHookTool target-ready dispatcher.
