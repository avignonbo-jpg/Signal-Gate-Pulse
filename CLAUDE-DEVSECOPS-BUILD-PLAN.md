# SignalGate Pulse — Claude DevSecOps Build Plan
## Compiler Warning Cleanup & Release CI

> **Status:** All tasks complete — implementation verified against commit `193462fc`
> **Authored:** Claude (chat session), 2026-09-01
> **Base commit at authoring:** `1cf6196`
> **Verified complete at:** `193462fc`
> **Branch:** `consumer-v1`
> **Referenced by:** `MANUS-HANDOFF.md` — "Current execution-plan addendum (2026-09-01)"
> **Governing authority:** `Architecture-Contract.md`, `PROJECT_LEDGER.md`, `SECURITY-DEVOPS-BUILD-PLAN.md` remain authoritative where this plan is silent.

---

## Purpose

This plan governed cleanup of all compiler warnings produced by the `62fa158` CI build log, deletion of two stale ProGuard rules, and addition of a release build job to `pulse-ci.yml`. It does not touch screening logic, database schema, security boundaries, or any behavior governed by Phase 4.0 of `SECURITY-DEVOPS-BUILD-PLAN.md`.

**Non-negotiable constraint inherited from all governing documents:** No task in this plan may weaken authoritative database semantics, screening decision integrity, explicit security-failure behavior, source activation safety, or release/CI security controls. Every change here is cosmetic, hygiene, or infrastructure — none of it touches the call-screening path.

---

## Execution Order

```
Task 0.1 → Task 1.1 → Task 1.2 → Task 1.3 → Task 1.4 → Task 1.5 → Task 1.6 → Task 2.1
```

Tasks 1.1–1.6 had no dependencies between them. Part 1 landed before Part 2 so that the first release build under R8 validated the Part 0 ProGuard fix in a minified context.

---

## Part 0 — ProGuard Stale Rules

### Task 0.1 — Delete two dead ProGuard keep rules

**Status: ✅ COMPLETE** — verified in `android/app/proguard-rules.pro` at `193462fc`

**What was removed:**
- `-keep public class com.signalgate.pulse.CallScreeningService { *; }` — class is named `SignalGateCallScreeningService`, not `CallScreeningService`; this rule never protected the real class
- `-keep class com.signalgate.pulse.ui.SettingsFragment { *; }` — no `SettingsFragment` exists; app is Compose-only

**Why it was safe:** The package-level rule `-keep class com.signalgate.pulse.** { *; }` on line 16 already covers everything legitimately needed. Both deleted rules were protecting nothing.

**allowed_files:** `android/app/proguard-rules.pro` only
**No call site or production logic was changed.**

---

## Part 1 — Kotlin Compiler Warnings

All warnings appeared in the CI build log at commit `62fa158`. None touch screening logic, database, or security boundaries.

### Task 1.1 — `ReliableSourceManager.kt:348` type mismatch

**Status: ✅ COMPLETE** — verified at `193462fc`

**Problem:** `json.optString("generated_at", null)` — passing Kotlin `null` to `JSONObject.optString(key, fallback)` causes the compiler to infer `Nothing?` because the parameter is a platform type (`String!`). On some Android API levels, `JSONObject.optString` with a null fallback returns the string `"null"` rather than Kotlin `null`, making the result assigned to `snapshotVersion: String?` potentially a non-null sentinel string.

**Fix applied:**
```kotlin
// Before:
snapshotVersion = json.optString("generated_at", null),

// After:
snapshotVersion = if (json.has("generated_at")) json.getString("generated_at") else null,
```

**allowed_files:** `android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt` only

---

### Task 1.2 — `ExperimentalCoroutinesApi` opt-in in `SignalGateCallScreeningService.kt`

**Status: ✅ COMPLETE** — verified at `193462fc`

**Problem:** `Dispatchers.Default.limitedParallelism(4)` uses `@ExperimentalCoroutinesApi` without a formal opt-in declaration.

**Fix applied:** `@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)` added as the first line of the file, before the `package` declaration.

```kotlin
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.signalgate.pulse
```

**allowed_files:** `android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt` only
**`limitedParallelism(4)` and `serviceScope` definition unchanged.**

---

### Task 1.3 — `LocalLifecycleOwner` deprecation in three screens

**Status: ✅ COMPLETE** — verified at `193462fc`

**Problem:** Three files imported `LocalLifecycleOwner` from `androidx.compose.ui.platform` (deprecated); it moved to `androidx.lifecycle.compose`.

**Affected files:**
- `ui/screens/ConsumerDashboardScreen.kt`
- `ui/onboarding/OnboardingWizardScreen.kt`
- `ui/screens/PermissionSettingsScreen.kt`

**Fix applied:** Added `androidx.lifecycle:lifecycle-runtime-compose:2.8.4` to `android/app/build.gradle` (pinned to match existing `lifecycle-viewmodel-compose:2.8.4`). Swapped import in all three files:
```kotlin
// Before:
import androidx.compose.ui.platform.LocalLifecycleOwner

// After:
import androidx.lifecycle.compose.LocalLifecycleOwner
```

No `DisposableEffect`, `lifecycleOwner`, or lifecycle observer code changed. API surface is identical.

**allowed_files:**
- `android/app/build.gradle`
- `android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt`
- `android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt`
- `android/app/src/main/java/com/signalgate/pulse/ui/screens/PermissionSettingsScreen.kt`

---

### Task 1.4 — Deprecated `Icons.Default.List` in `SourceIcon.kt` and `Screen.kt`

**Status: ✅ COMPLETE** — verified at `193462fc`

**Problem:** `Icons.Filled.List` (accessed as `Icons.Default.List`) moved to `Icons.AutoMirrored.Filled.List` for RTL layout support.

**Fix applied in both files:**
```kotlin
// Import:
import androidx.compose.material.icons.automirrored.filled.List

// Usage:
Icons.AutoMirrored.Filled.List
```

**allowed_files:**
- `android/app/src/main/java/com/signalgate/pulse/ui/components/SourceIcon.kt`
- `android/app/src/main/java/com/signalgate/pulse/ui/navigation/Screen.kt`

---

### Task 1.5 — Deprecated `outlinedButtonBorder` in `DigestScreen.kt`

**Status: ✅ COMPLETE** — verified at `193462fc`

**Problem:** `ButtonDefaults.outlinedButtonBorder` (no-arg property) deprecated in favour of `ButtonDefaults.outlinedButtonBorder(enabled: Boolean)`.

**Fix applied:**
```kotlin
// Before:
border = ButtonDefaults.outlinedButtonBorder.copy(
    brush = androidx.compose.ui.graphics.SolidColor(BorderGlass)
),

// After:
border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
    brush = androidx.compose.ui.graphics.SolidColor(BorderGlass)
),
```

The Dismiss button is always enabled; `enabled = true` is semantically correct.

**allowed_files:** `android/app/src/main/java/com/signalgate/pulse/ui/digest/DigestScreen.kt` only

---

### Task 1.6 — Suppress or rename four unused parameters

**Status: ✅ COMPLETE** — verified at `193462fc`

Four unused parameters were suppressed or renamed. No parameter was removed and no call site was changed.

| File | Parameter | Resolution |
|---|---|---|
| `DatabaseInitializer.kt:19` | `context: Context` | `@Suppress("UNUSED_PARAMETER")` — retained for future migration use |
| `NavGraph.kt:24` | `onOpenDrawer: () -> Unit` | `@Suppress("UNUSED_PARAMETER")` — retained, drawer opened from `MainActivity` |
| `PermissionSettingsScreen.kt:65` | `isGranted` (lambda param) | Renamed to `_` — re-checks permissions via `ContextCompat.checkSelfPermission()` directly |
| `SignalGateTheme.kt:105` | `darkTheme: Boolean` | `@Suppress("UNUSED_PARAMETER")` — retained; dark mode forced unconditionally pending light theme |

**allowed_files:**
- `android/app/src/main/java/com/signalgate/pulse/database/DatabaseInitializer.kt`
- `android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt`
- `android/app/src/main/java/com/signalgate/pulse/ui/screens/PermissionSettingsScreen.kt`
- `android/app/src/main/java/com/signalgate/pulse/ui/theme/SignalGateTheme.kt`

---

## Part 2 — Release Build CI

### Task 2.1 — Add release build job to `pulse-ci.yml`

**Status: ✅ COMPLETE** — verified in `.github/workflows/pulse-ci.yml` at `193462fc`

**Problem:** Every CI artifact was a debug build. A release build is required to validate ProGuard rules under R8 and to produce a store-submittable APK from CI.

**Job added:** `build-release` — runs after `build-and-test` on push to `consumer-v1` only. Decodes `RELEASE_KEYSTORE_BASE64` secret to `$RUNNER_TEMP/release.keystore`, then runs `assemblePulseRelease`. Uploads `pulse-release-apk` (14-day retention).

**Key design decisions:**
- `RELEASE_KEYSTORE_PATH` is constructed from `${{ runner.temp }}` at runtime, not a secret — the keystore file is written there by the decode step
- Five secrets required: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
- `needs: build-and-test` ensures debug build, tests, and lint all pass before release build attempts signing
- `if: github.event_name == 'push'` — does not run on pull requests, preventing signing secrets from being used on unreviewed code

**allowed_files:** `.github/workflows/pulse-ci.yml` only

---

## Ledger Status

**Gap notice:** The implementation commits for Tasks 0.1–2.1 in this plan were not accompanied by `PROJECT_LEDGER.md` entries at the time of execution. This is a ledger discipline gap. The next Manus entity handling any `consumer-v1` work should backfill a single consolidated ledger entry covering all tasks in this plan, recording:
- Which commits landed each task
- That all CI workflows passed (Consumer CI, Instrumented Tests, Compose Metrics, Dependency/CVE Scan)
- That no production screening logic, database schema, security boundary, or Phase 4.0 work was affected

This backfill should be the first commit of the next work session before any other changes are made.

---

## Remaining Work Not Covered by This Plan

This plan is narrowly scoped to compiler hygiene and CI infrastructure. The following items are explicitly out of scope here and remain open under `SECURITY-DEVOPS-BUILD-PLAN.md`:

- `LogcatViewerScreen` is not restricted to debug builds at the build-variant level (Architecture Contract §3 requirement — currently navigable from Settings in release builds)
- Phase 4.0.1 — CallScreeningService response guarantee: null-handle test and measured timing against the 3.5-second deadline budget remain open
- Phase 4.0.7 — Pre-Release Screening Assurance Gate: real-device cold-start, process death, and release/minified Telecom behavior remain owner-only release blockers
- Privacy and operational-surface review: call metadata in logs, notifications, and debug surfaces
- `BlocklistRepository` deprecated facade elimination
- `navigation-fragment-ktx` and `navigation-ui-ktx` dependencies with no Fragment-based navigation in use

These are tracked under their respective phases in `SECURITY-DEVOPS-BUILD-PLAN.md` and are not governed by this document.
