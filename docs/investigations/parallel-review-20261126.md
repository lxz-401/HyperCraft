# Parallel review continuation — 2026-11-26

Resumed Antigravity session `01d3e4f6-d89e-4dc2-9f73-9f50344b3a80`
(“Parallel Code Review And Fixing”) after its quota interruption. The original
review compared `upstream/main` (`79ddb292a`) with `b55240159`. Its 13 uncommitted
files were retained, reviewed, and completed. Three parallel implementation
agents handled hook lifecycle, volume plugins, and UI/resources; the coordinator
handled dependencies, R8, shared shell utilities, provisioning, and integration.
A further native investigation and independent Standards/Spec reviews followed.

## Completed corrections

- Removed the external expansion dependency that duplicated four local utility
  classes; removed annotation processors and their Guava dependencies from APK
  runtime dependencies. Pinned the already resolved LunarCalendar version.
- Removed the tracked workstation JDK path; aligned Kotlin toolchains and pinned
  Gradle daemon criteria to portable Java 25 (without a workstation path). Preserved
  the existing `:app`-relative signing path: both CI workflows intentionally use
  `../key.jks`, so the old report's root-relative recommendation was rejected.
- Kept reflective HeaderManager/provision entry points and EzHookTool subpackages
  through R8. Preserved Java/Kotlin null checks instead of assuming they have no
  side effects. The obsolete, unwired release logging rules remain unwired;
  runtime diagnostic logging policy was not changed globally.
- Replaced fake signature digests with installed APK certificate digests and
  model-only device tokens with hashes of all existing token inputs. HyperCraft
  accepts local signing keys; certificate availability is not a claim of upstream
  certificate authenticity. No trusted upstream certificate was invented.
- Fixed shell pipe deadlocks, callback ownership, missed completion notifications,
  lazy shared-shell retention, and literal property argument quoting.
- Preserved primary SystemUI target state while dispatching secondary plugin
  callbacks with the correct loader. Secondary plugin hooks reject hot reload
  before cleanup because the framework cannot replay their package lifecycle;
  a SystemUI restart is required in that case.
- Removed redundant observer/cache registrations from hot paths. Fixed context
  polling CPU spin, shared executor shutdown, context fallback, executor reuse
  after hot reload, nested/concurrent auto-start correlation, and OOBE preference
  synchronization.
- Backup restore preserves bracket-containing strings and floating values and
  closes streams on failures. Provision callbacks always finish broadcasts;
  empty back stacks and missing connectivity services are guarded.
- Volume progress handles zero/small maxima and overflow. Plugin hooks deduplicate
  by weak loader keys, use current preferences and actual density, preserve OEM
  touch listeners, restore tints/outlines on disable, and complete view cleanup
  on main before hot reload continues. Long-press work belongs to each seekbar.
- Preserved child fragment state, stable page identity, navigation clearance,
  custom-control enablement, and physical preference change notifications.
  Preview gestures cooperate with scrolling; colors and outlines handle light
  mode and small progress. Pixel/HyperOS hook colors now match preview defaults. English defaults and the original Uzbek strings are
  separate resources. Duplicate advanced preferences remain only in OtherSettings.
- Fixed the process-wide native `MADV_DONTNEED` guard to discard unprotected
  neighboring pages while retaining patched pages, preserving kernel validation
  and original error results.
- Repaired the host layout endpoint runner, skipped Linux-only bounded-reader
  tests on other hosts, and replaced root test scripts' workstation ADB path with
  an `ADB` override / PATH default. Changed XML follows `.editorconfig`.

## Independent review

Standards: no documented standard violations; one duplication-related preset-color
mismatch was found and corrected so Pixel/HyperOS hook fills match the preview.
Spec/correctness: review caught numeric type loss after JSON round trips for
whole-valued floats and small longs; tagged numeric backup values and tests
using the real Android SDK JSON implementation now preserve these types. The
independent reviewer rechecked the fix and confirmed no remaining backup defect.
No formal original product specification was available; recovered audit reports
and the user’s continuation request supplied acceptance criteria.

## Validation

Passed host checks:

- `python3 tests/os4-init-prefs/run.py`: 37/37 lifecycle cases.
- `python3 tests/hook-runtime-lifecycle/run.py`: all eight groups, including real
  SDK37 JSON round trips and three executor cleanup/recreation cycles.
- `python3 tests/shell-runtime/run.py`: large stdout/stderr capture/discard,
  callback ownership, completion-before-sync, repeat sync, shared shell reuse.
- `python3 tests/volume-progress/run.py`: zero/small maxima, overflow and rounding.
- `python3 tests/volume-hook-cleanup/run.py`: main-thread ordering, timeout,
  interruption, restore failure and rejected posts.
- `python3 tests/home-madvise-guard/run.py`: production/reference comparison and
  real Linux memory discard; the pre-fix implementation fails this regression.
- `bash tests/home-layout-native/run_java.sh`: endpoint regression.
- `bash tests/run_host_native_tests.sh`: 14 executables returned success;
  ARM64 assembly skipped on x86_64. Image-specific assertions require the
  optional original vendor/Flutter libraries and were not claimed as tested.
- Changed XML parsing, shell syntax, and `git diff --check` passed.
- Debug APK class-definition inspection: 16,516 classes, zero duplicates, one
  definition for each expansion utility, zero annotation-processor classes,
  and no annotation-processor service metadata. Guava shrank from 2,335
  classes in the old report to the single transitive interface still needed.
- `./gradlew --version` confirms daemon criteria select Java 25 while the
  host launcher remains Java 27.

Integration: debug APK, R8 release APK and Gradle `:app:hostNativeTests` all
passed with Java 25, `--no-parallel --max-workers=1` (361 tasks, 7m11s).
Earlier Java 27/parallel execution failed inside AGP annotation extraction
(`AsyncExecutionService.getService must not return null`); Java 25/serial completed
annotation extraction and lint-vital normally.

Final stable-source command (daemon criteria selected Java 25):

```sh
./gradlew :app:assembleDebug :app:assembleRelease :app:lintDebug \
    :library:common:lintDebug --continue --no-parallel --max-workers=1 --console=plain
```

Both `:app:assembleDebug` and `:app:assembleRelease` passed with the final numeric
backup and preset-color corrections. `:library:common:lintDebug` passed with
**0 errors, 5 warnings**. `:app:lintReportDebug` failed inside AGP with
**`Index 3 out of bounds for length 1`**; its XML/text reports were still generated
and contain **52 MissingTranslation errors, 234 warnings**. The overall command
therefore exited 1 (465 tasks, 5m32s). App lint is explicitly not green; no lint
baseline or detector suppression was introduced to hide the failure.
Host fixtures exercise production Java/Kotlin/native implementations; they are
not evidence of Android Binder, ART, OEM plugin, or real-device rendering behavior.

## Remaining findings and review decisions

- `SharedPrefsProvider.query/openAssetFile` still expose the legacy public read
  transport. No lower-layer scoped-app access policy exists; adding a blanket UID
  gate would break the launcher and third-party hook clients. A scoped package
  policy must be designed and tested across service binding and scope changes.
  Existing mutation and DockGlass-specific UID gates were preserved.
- The final app lint report contains **52 MissingTranslation errors and 234
  warnings** (earlier: 235); common has **0 errors and 5 warnings** (earlier: 6).
  Missing translations were
  not “fixed” by disabling the detector or copying English into other locales.
  Broader translation coverage and third-party/tooling warnings remain separate.
- Optional live geometry updates while dragging (rather than persistence on stop),
  more translations, capsule redesign, preset abstraction, unused duplicate code,
  and guessed mute-color changes were not required for the confirmed fixes.
- Old ROM-specific scripts that refer to absent `home-os4-gate-controls`,
  `os4-audit-folder-geometry-20261005`, or `os4-audit-image-discovery-20261005` inputs
  cannot be validated without their historical fixtures. The repaired host runners
  and new regressions do not rely on those absent directories.
- Device verification remains necessary for SystemUI/plugin recreation, switching
  iOS/custom/off, haptics/long press, DPI changes, theme/font size, rotation and
  preference navigation. No APK was installed and no device process restarted.
