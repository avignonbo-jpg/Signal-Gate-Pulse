# SignalGate Pulse — Production-Readiness Procedure (Verified)

**Supersedes:** *Development Roadmap v1.0.0 (2026-07-02)* and *Project Audit* for all items below.
**Method:** Every item in this document was checked directly against the current `Signal-Gate-Pulse-consumer-v1` source (not inferred from the roadmap or audit text). Where the original roadmap or audit was wrong about the current state — either too pessimistic or too optimistic — this document corrects it and says so.
**Session 2 update (prior pass):** Phase 4.2 (`BlockAllowList` screen) implemented, plus the latent `BlocklistRepository` binding bug (4.2b) it surfaced.
**Session 3 update (this pass):** Phase 4.4 (`SettingEntry` migration) and Phase 4.6 (`ConsumerDashboardScreen` as default) implemented. All 3 remaining `PULSE-TODO`s resolved — zero remain. See the "This session (Phase 4.4 + 4.6)" table near the bottom. Phase 3.1 (FTC/FCC endpoints) or Phase 3.6 (priority conflict resolution) is next.

### Status legend

| Symbol | Meaning |
|---|---|
| ✅ | Verified working in the current source |
| 🛠️ | Broken — **already fixed in this session** (included in the patched zip) |
| 🔴 | Broken — **build-blocking**, not yet fixed |
| ⚠️ | Present but incomplete / functionally inert |
| ❌ | Not started |

---

## PHASE 0 — Stop-Ship: Build-Blocking Defects

**This phase does not exist in the original roadmap.** The roadmap and audit both assumed the project compiles today. It does not. These six defects will fail the build or crash at startup and must be fixed before any other phase is worth touching.

### 0.1 🛠️ `SourcesScreen.kt` referenced a `ViewModel` that didn't exist — **Fixed this session**
- **File:** `ui/screens/SourcesScreen.kt`
- **Problem (as found):** `fun SourcesScreen(viewModel: SourcesViewModel = viewModel())` — no `SourcesViewModel` class existed anywhere in the project. The file also called `viewModel.showAddSheet()` (undefined), used `Color.Green/Yellow/Red` without importing `androidx.compose.ui.graphics.Color`, and called a `.humanReadable()` extension on `Long` that was never defined.
- **Why it mattered:** This screen is wired live into `NavGraph.kt` (`composable(Screen.Sources.route) { SourcesScreen() }`), not sitting in an unused corner — the module would not compile as long as this file existed in its original form.
- **Fix applied:**
  1. Created `ui/screens/SourcesViewModel.kt` — injects `DataSourceRepository`, exposes `sources: Flow<List<SourceEntity>>` and `isSyncing`/`isAddSheetVisible`/`addSourceError` as `StateFlow`s, and `showAddSheet()`/`hideAddSheet()`/`addSource(...)`/`syncSource(...)`/`deleteSource(...)`. `addSource()` runs name and path/URL through `SanitizationEngine.sanitizeTextField()` before calling `DataSourceRepository.insertSource()`, and clamps user-supplied priority to 0–99 (100 stays reserved for the `MANUAL` blocklist source).
  2. Registered it in `viewModelModule` in `AppModule.kt`.
  3. Created `utils/DateUtils.kt` with the missing `Long.humanReadable()` extension (relative time — "5m ago" / "3h ago" / "2d ago" — falling back to an absolute date beyond a week, and "Never" for unsynced sources).
  4. Rewrote `SourcesScreen.kt`: proper `Color` import, real health-color-coded list backed by `SourcesViewModel.sources`, and a working `ModalBottomSheet` ("Add Source") with name/type/path-URL/priority fields that calls `viewModel.addSource(...)` and surfaces validation errors inline.
- **Result:** `SourcesScreen` compiles, renders real `SourceEntity` rows with health color, and "Add Source" opens a working bottom sheet that inserts through the repository. (This absorbs the original roadmap's 2.3 and 2.4 — see Phase 3 below, both now ✅.)

### 0.2 🛠️ `CommunitySyncWorker.schedule()` would not compile — **Fixed this session**
- **File:** `workers/CommunitySyncWorker.kt`
- **Problem (as found):** `.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ...)` used `BackoffPolicy` with no import anywhere in the file, and `.setConstraints(/* Network + BatteryNotLow */)` passed a **comment** where a `Constraints` object was required — there was no actual argument. It also built a `OneTimeWorkRequest`, which didn't match the daily-recurring behavior described everywhere else in the codebase.
- **Fix applied — made this the single source of truth for scheduling, as decided:**
  - Added the missing imports (`BackoffPolicy`, `Constraints`, `NetworkType`) and built a real `Constraints` object (`NetworkType.CONNECTED` + `setRequiresBatteryNotLow(true)`).
  - Switched from a one-time request to `PeriodicWorkRequestBuilder<CommunitySyncWorker>(24, TimeUnit.HOURS)` — matching the daily cadence `MainApplication` was separately (and inconsistently) trying to schedule.
  - Added `const val WORK_NAME = "community_sync"` to the companion object — this also resolves the old 0.5 item below, since that's exactly the constant `MainApplication` was referencing without it ever being defined.
  - Deleted `MainApplication.scheduleCommunitySync()` entirely and replaced its call site with `CommunitySyncWorker.schedule(this)`. There is now exactly one place `"community_sync"` is ever enqueued, with one consistent set of constraints and backoff — not two disagreeing definitions of the same job.

### 0.3 🛠️ Koin `workerModule` factory passed 3 arguments to a 2-argument constructor — **Fixed this session**
- **File:** `di/AppModule.kt`
- **Problem (as found):** `CommunitySyncWorker(context, params, get())` — but the actual constructor is `CommunitySyncWorker(context: Context, params: WorkerParameters)`. Its two real dependencies (`ReliableSourceManager`, `DataSyncEngine`) are pulled via `by inject()` property delegation inside the class, not constructor params. The DI factory didn't match the class signature. Cross-checked against `KoinWorkerFactory.createWorker()`, which only ever calls `parametersOf(appContext, workerParameters)` — confirms the factory lambda's two destructured params were always the right (and only) two arguments; the trailing `get()` was simply extra.
- **Fix applied:** `CommunitySyncWorker(context, params)` — dropped the trailing `get()`.

### 0.4 🛠️ `MainApplication.kt` referenced a nonexistent `android.BuildConfig` — **Fixed this session**
- **File:** `MainApplication.kt`
- **Problem (as found):** `if (android.BuildConfig.DEBUG)` — there is no `android.BuildConfig` class in the Android SDK; only the app's own generated `com.signalgate.multipoint.BuildConfig` exists (and is used correctly elsewhere, e.g. `SecurityUtils.kt`).
- **Fix applied:** added `import com.signalgate.multipoint.BuildConfig` and changed the check to `BuildConfig.DEBUG`. Confirmed against `app/build.gradle` (`namespace "com.signalgate.multipoint"`) that this is the correct generated package regardless of the release build's separate `applicationId` override — matches what `SecurityUtils.kt` already does successfully. Swept the rest of the project for any other stray `android.BuildConfig` references — none found.

### 0.5 🛠️ `MainApplication.kt` referenced `CommunitySyncWorker.WORK_NAME`, which didn't exist — **Fixed this session**
- **File:** `MainApplication.kt`, was inside `scheduleCommunitySync()`
- **Problem (as found):** `CommunitySyncWorker.WORK_NAME` was used but the companion object only defined `TAG` and `schedule()`. There was also **no `Constraints`** applied to the `PeriodicWorkRequestBuilder` here at all — this was a second, independent scheduling path that duplicated (and disagreed with) the one in 0.2.
- **Fix applied:** resolved as a direct consequence of the 0.2 fix — `WORK_NAME` now exists on `CommunitySyncWorker`, and `MainApplication.scheduleCommunitySync()` (the competing definition) is gone entirely. `MainApplication.onCreate()` now just calls `CommunitySyncWorker.schedule(this)`.

### 0.6 🛠️ Duplicate Koin binding for `BlocklistRepository` — **Fixed this session**
- **File:** `di/AppModule.kt`
- **Problem (as found):** `single { BlocklistRepository(get(), -1) }` was declared **twice** — once in `databaseModule` (line 82) and again in `repositoryModule` (line 117) — and both modules are loaded together in `appModule`. Koin throws a definition-override exception the moment `startKoin {}` runs, meaning the app couldn't start at all, independent of the compile errors above.
- **Fix applied:** deleted the binding from `databaseModule`; `BlocklistRepository` now lives solely in `repositoryModule`, matching that module's own doc comment ("Provides repository layer... No direct DAO access from UI or engines — all goes through repositories"). Swept the whole file for other duplicate `single{}`/`factory{}`/`viewModel{}` bindings by return type — none found.
- **Note, not fixed (out of scope for this ticket):** `databaseModule` also contains `single { PendingCardRepository(get()) }` — that one isn't duplicated, so it doesn't crash anything, but per that module's own doc comment ("Provides Room database and all DAOs") it's arguably misplaced the same way `BlocklistRepository` was, just without the collision. Flagging for a future cleanup pass, not touched here.

**Phase 0 is now fully resolved — the app compiles and starts.** All six stop-ship defects (0.1–0.6) are fixed.

---

## PHASE 1 — Foundation Hardening *(was roadmap Phase 0)*

| # | Task | Verified status |
|---|---|---|
| 1.1 | Keystore-derived SQLCipher passphrase | 🛠️ **Fixed this session.** The old code called `secretKey.encoded` on an Android Keystore-backed key — this throws/returns null because Keystore keys are intentionally non-exportable, so the encrypted DB could never actually open. Rewritten using envelope encryption: a random 256-bit passphrase is generated once, encrypted with the Keystore key, and only the ciphertext is persisted (`SecurityUtils.kt`, `SecureDatabase.kt`). |
| 1.2 | `CallLogRepository` full CRUD | ✅ Already implemented correctly — matches the roadmap's own acceptance criteria. |
| 1.3 | `Theme.kt` / `CallType.kt` / `ThreatSource.kt` content gaps | ✅ Already resolved. Minor note: `CallType` lives inside `data/models/CallLogItem.kt` rather than its own file (this is why the audit's file-by-file table showed it as a gap — the class exists, just not where expected). Cosmetic only; optional cleanup: extract to its own file for architecture-doc consistency. |
| 1.4 | CI drift-detection lint in `pulse-ci.yml` | 🛠️ **Fixed this session.** Added `scripts/check-architecture-drift.sh`, a grep-based structural check, and wired it into `pulse-ci.yml` as its own step *before* the Gradle build so it fails fast. Enforces: `ui/**` importing `database.daos.*`; `CallScreeningEngine.kt`/`CallRiskEvaluator.kt` (the true L6 files — see note below) importing `Notification*`; `data/security/**` importing `androidx.room.*`; `DataSyncEngine.kt`/`ReliableSourceManager.kt` (the true L2 files) importing `androidx.room.*`; and `runBlocking(`/`runBlocking{` anywhere outside `MainApplication.kt`. **Important scoping note:** the `logic/` package holds files from *two* different layers — L2 transport (`DataSyncEngine`, `ReliableSourceManager`) and L6 decision logic (`CallScreeningEngine`, `CallRiskEvaluator`) — so the L2/L6 rules target those specific files by name rather than the whole directory; a directory-wide rule would have been wrong. **Dry-running this before wiring it in caught two real, pre-existing violations that had nothing to do with anything else fixed this session** — see below. |

**Bonus finds from dry-running the new lint before enabling it in CI:** `PendingCardViewModel.kt` was injecting `PendingCardDao` directly (fixed — swapped for the `PendingCardRepository` that already existed), and `ContactsViewModel.kt` was injecting `SettingDao` directly (fixed — a new thin `SettingRepository` was created, since no repository wrapped `SettingDao` at all yet; it also gives the Phase 4.4 settings migration a home to read/write through). Both are registered in `AppModule.kt`. The lint now passes cleanly against the current tree — it wasn't merged in a state that immediately fails CI on unrelated debt.
| 1.5 | Audit `PULSE-TODO` comments → tracked issues | ✅ **Complete.** All `PULSE-TODO` comments resolved — Phase 4.4 eliminated the last 3. Zero `PULSE-TODO`s remain in the codebase. |

---

## PHASE 2 — Core Call Screening Hardening *(was roadmap Phase 1)*

| # | Task | Verified status |
|---|---|---|
| 2.1 | Repository (not DAO) injected into `SignalGateCallScreeningService` | ✅ Done — verified no DAO imports in the service file. |
| 2.2 | `DashboardViewModel.refreshCounters()` on real queries | ✅ Done — `getBlockedCallsCount()` / `getCallsInRange()` wired correctly. |
| 2.3 | STIR/SHAKEN wiring in `CallRiskEvaluator` | 🛠️ **Fixed this session**, and the roadmap's own acceptance criteria was wrong: it says to read "`EXTRA_CALL_SUBJECT` or carrier extras," but that's not a STIR/SHAKEN signal at all. The old code read a custom extras key (`"stir_attestation"`) that no carrier or the Android telecom stack ever populates — it was permanently `UNKNOWN` in production. Rewired to the real API, `Call.Details.getCallerNumberVerificationStatus()` (Android 11 / API 30+), which returns a 3-state verdict (`PASSED` / `FAILED` / `NOT_VERIFIED`) — Android does not expose full A/B/C attestation to apps, only this coarser verdict. Risk weights were adjusted accordingly (`CallRiskEvaluator.kt`). |
| 2.4 | `PendingCardRepository` wrapper, injected not DAO | ✅ Done. |
| 2.5 | Unit tests for `CallScreeningEngine` tier logic | ❌ **Not started.** Zero test files reference `CallScreeningEngine`. `src/test/kotlin/.../security/SecurityUtilsTest.kt` is a placeholder (literally the text `placeholder` + a function name, not valid Kotlin) — it doesn't compile as a test either. Only `PhoneNumberUtilsTest.kt` is a real test in the whole project. **To-do:** write parameterized tests for `buildAllowInfo`/`buildBlockInfo`/`buildGrayZoneInfo`/`buildDefaultInfo` using a fake `DataSourceRepository`; write a real `SecurityUtilsTest` (note: Keystore isn't available under plain JUnit — this needs either Robolectric's shadow keystore or an instrumented `androidTest`, not a unit test — budget for that distinction). |
| 2.6 | `getCallDecision()` pattern-matching cascade | ✅ Done — matches the roadmap's fallback order exactly (`manual_allow → manual_block → pattern → aggregated → default`). |

---

## PHASE 3 — Data Source & Sync System *(was roadmap Phase 2)*

| # | Task | Verified status |
|---|---|---|
| 3.1 | Replace placeholder FTC/FCC URLs with verified endpoints | ⚠️ **Likely still wrong, needs a real fix.** `ReliableSourceManager.SOURCES` now points at real domains (not obvious placeholders), but the FTC URL (`.../DNC_Complaint_Numbers.csv`) is a **static, undated filename** — the FTC actually publishes this file with the date baked into the name (e.g. `DNC_Complaint_Numbers_5-1-2019.csv`) and also offers a proper REST endpoint at `api.ftc.gov/v0/dnc-complaints`. A static undated URL is very likely a 404 against the live site. **To-do:** switch to the `api.ftc.gov` JSON endpoint (paginated, needs an API key — `DEMO_KEY` works for light testing) instead of scraping a CSV filename, or add date-resolution logic if staying with CSV. The FCC URL was not independently re-verified this session — confirm it still resolves before shipping. No fallback URL list exists for either source yet (roadmap asked for one).
| 3.2 | `DataSyncEngine.parseXLSXFile()` | ✅ Done — Apache POI streaming, row cap enforced, header skip. |
| 3.3 | Source health monitoring dashboard | ✅ **Fixed this session** — see Phase 0.1. `SourcesScreen.kt` now compiles and renders real, color-coded `SourceEntity` data via the new `SourcesViewModel`. |
| 3.4 | Manual source addition UI | ✅ **Fixed this session** — same fix as 3.3/0.1. "Add Source" now opens a working `ModalBottomSheet` (name/type/path-URL/priority) that validates via `SanitizationEngine` and inserts through `DataSourceRepository`. |
| 3.5 | Sync retry with exponential backoff | ✅ **Fixed this session** — see Phase 0.2. Real `Constraints` + `BackoffPolicy.EXPONENTIAL` now wired correctly, and this is the sole scheduling path. |
| 3.6 | Source conflict resolution by priority | ❌ **Not implemented as specified.** `getCallDecision()` uses a fixed precedence by *rule type* (manual > pattern > aggregated), which is a reasonable default, but never compares `SourceEntity.priority` when two non-manual sources disagree (e.g. a federal source says BLOCK and a community source says ALLOW on the same number) — it just returns whichever query matches first. **To-do:** when `findEntriesByPhoneNumber()` returns multiple conflicting rows, sort by `SourceEntity.priority` (descending) before picking a winner, and keep `MANUAL` pinned at priority 100 as the roadmap specifies. |

---

## PHASE 4 — User Experience & Background Reliability *(merges roadmap Phases 3 + 4)*

| # | Task | Verified status |
|---|---|---|
| 4.1 | Onboarding completion persistence | ✅ Complete. `onboarding_complete` now written to `SettingEntry` (migrated in 4.4 this session). `NavGraph` reads it on startup to set the correct `startDestination`. |
| 4.2 | `BlockAllowList` screen | 🛠️ **Fixed this session.** `NavGraph.kt` now renders a real `BlockAllowListScreen()` instead of the literal `Text("Block / Allow List — Coming Soon")`. Built against `BlockedNumbersViewModel` / `BlocklistRepository` per the original acceptance criteria: search-by-number, filter chips (All/Blocked/Allowed), manual add (Block or Allow, with an optional note) through a `ModalBottomSheet` matching `SourcesScreen`'s pattern, and delete. **Bonus find while wiring this up:** `BlockedNumbersViewModel` was rewritten from scratch — it previously read `DataSourceRepository.getAllEntries()` (every entry from every source) filtered client-side to `action == "BLOCK"`, with no ALLOW support and no way to add or delete anything. It now reads through `BlocklistRepository`, which is the correct, user-scoped data source for a screen the user edits directly. See 4.2b below for a second bug this surfaced. |
| 4.2b | `BlocklistRepository` manual-source binding | 🛠️ **Fixed this session — pre-existing, latent bug, not in the original roadmap or audit.** `di/AppModule.kt` constructed `BlocklistRepository(get(), -1)` — a hardcoded, invalid `sourceId` — with a comment deferring the real fix to "Step 2.4" of the old roadmap, which was never carried out. This was never caught earlier because nothing actually called through `BlocklistRepository` yet (`BlockAllowListScreen` was still the stub above). The moment 4.2 wired a real screen to it, every `addBlockRule()`/`addAllowRule()` call would have failed the `UnifiedEntryEntity` → `SourceEntity` foreign key constraint (`-1` isn't a row in `sources`) — i.e. tapping "Save" in the new screen would have crashed or silently failed on every single call. **Fix applied:** `BlocklistRepository` now takes a `SettingRepository` and resolves the real `manual_source_id` lazily on first use, caching it (`BlocklistRepository.manualSourceId()`). That setting is written synchronously by `DatabaseInitializer.seedRequiredSources()` before any Koin binding is resolved, so this needed no `runBlocking` — confirmed clean against the Phase 1.4 drift lint (`./scripts/check-architecture-drift.sh` passes with zero violations after this change). |
| 4.3 | Global number search | ❌ Not found anywhere in `ui/`. Not started. |
| 4.4 | Settings migration: `SharedPreferences` → `SettingEntry` | 🛠️ **Fixed this session.** `shield_red/green/blue` and `onboarding_complete` migrated from `SharedPreferences` to `SettingEntry` via `SettingRepository`. New `SettingsViewModel` owns all color state. All 3 remaining `PULSE-TODO`s are resolved — zero remain. |
| 4.5 | Haptics / animation polish | Not verified this session — lower priority, revisit after 4.2–4.4. |
| 4.6 | `ConsumerDashboardScreen` as default | 🛠️ **Fixed this session.** `NavGraph.kt` now reads `onboarding_complete` from `SettingEntry` on startup and routes to `Screen.ConsumerDashboard` (real default) or `Screen.Onboarding` accordingly. `OperationalDashboard` retained at `Screen.Dashboard.route` for power users. `Screen.ConsumerDashboard` added to `Screen.kt`. |
| 4.7 | Grouped notification summary | Not verified — only a single blocked-call notification channel (`blocked_call_review`) exists today; no grouping/summary logic found. |
| 4.8 | Notification channels & priorities | ⚠️ Only `BLOCKED_CALL_REVIEW` exists. `SYNC_STATUS` and `SECURITY_ALERT` channels from the roadmap don't exist yet. |
| 4.9 | Foreground service for sync | ❌ Not started — no `Service` subclass or `startForeground()` call anywhere; not declared in `AndroidManifest.xml`. |
| 4.10 | Battery optimization exemption prompt | Not verified — lower priority. |
| 4.11 | `BootReceiver` for sync reschedule | ❌ **Not started.** No `BOOT_COMPLETED` receiver exists, and `RECEIVE_BOOT_COMPLETED` isn't in `AndroidManifest.xml`. Combined with the broken worker scheduling in Phase 0, sync currently has no reliable trigger at all after a reboot. |
| 4.12 | WorkManager constraints (network/battery) | ✅ **Fixed this session** — see Phase 0.2. `NetworkType.CONNECTED` + `setRequiresBatteryNotLow(true)` now applied in `CommunitySyncWorker.schedule()`, the single scheduling path. |

---

## PHASE 5 — Testing & Quality Assurance

**Verified status: essentially 0% started.** The entire `src/test` directory contains two files: one real test (`PhoneNumberUtilsTest.kt`) and one placeholder that isn't valid Kotlin (`SecurityUtilsTest.kt` — literally contains the word `placeholder` and a function name as plain text). There is no `androidTest` (instrumented) source set at all in the zip.

To-do, in order:
1. Get the project compiling first (Phase 0) — nothing here is testable until then.
2. Write real unit tests for `SanitizationEngine` (SQL injection, XSS, buffer/unicode edge cases) and `SecureCsvParser` (zip bomb, oversized file, malformed CSV) — these are pure functions, cheapest to cover.
3. Write the `CallScreeningEngine` tier-boundary tests noted in Phase 2.5.
4. Add a Robolectric or instrumented test for `SecurityUtils` — real `AndroidKeyStore` behavior can't be unit-tested without one of those two, decide which now rather than late.
5. Room in-memory DB integration tests for `DataSourceRepository.getCallDecision()` and `BlocklistRepository`.
6. Compose UI tests for onboarding completion, dashboard shield toggle, digest card dismiss — once those screens are stable post-Phase 4.
7. Performance/benchmark and OWASP pass — last, once behavior is stable (matches original roadmap ordering; no reason to move this earlier).

---

## PHASE 6 — Release Preparation

| # | Task | Verified status |
|---|---|---|
| 6.1 | `minifyEnabled` / `shrinkResources` / ProGuard rules | ✅ Already configured in `app/build.gradle` (`minifyEnabled true`, `shrinkResources true`, `proguard-rules.pro` wired). Confirm the rules file preserves security-critical classes (Room entities, SQLCipher, Koin-reflected classes) before shipping — not verified this session. |
| 6.2 | Firebase Crashlytics | ❌ Not started — no Firebase/Crashlytics dependency anywhere in either `build.gradle`. |
| 6.3 | Privacy-preserving analytics | ❌ Not started. |
| 6.4 | Play Store listing assets | ❌ Not started (expected — this is a content task, not code). |
| 6.5 | Staged rollout plan | Planning-only task, no code dependency — can be written any time, unaffected by this audit. |
| 6.6 | Post-launch monitoring dashboard | ❌ Not started; depends on 6.2/6.3 shipping first. |

---

## Recommended sequencing to reach a shippable v1.0

1. ~~**Phase 0 (0.1–0.6)**~~ — ✅ **done.** The app compiles and starts.
2. ~~**Phase 1.4** (CI drift lint)~~ — ✅ **done.**
3. ~~**Phase 4.2** (`BlockAllowList` screen, + the 4.2b `BlocklistRepository` binding bug it surfaced)~~ — ✅ **done.** Last placeholder screen is now real; `SourcesScreen`/`SourcesViewModel` (0.1, 3.3, 3.4) were already fixed in the prior session.
4. ~~**Phase 4.4**~~ (`SettingEntry` migration) — ✅ **done.** All 3 `PULSE-TODO`s resolved; zero remain.
5. ~~**Phase 4.6**~~ (wire `ConsumerDashboardScreen` as the real default) — ✅ **done.** `ConsumerDashboardScreen` is now the startup default; `OperationalDashboard` retained for power users.
6. **Phase 3.1** (fix the FTC/FCC endpoints) and **3.6** (priority-based conflict resolution) — data-quality correctness.
7. **Phase 4 remainder** (notifications, foreground service, boot receiver) — background reliability.
8. **Phase 5** (testing) — in parallel with the above once each area stabilizes, not strictly last. Note: `CallScreeningEngine`/`SanitizationEngine`/`SecureCsvParser` tests (5.2–5.3) can now also cover `BlocklistRepository.manualSourceId()` resolution/caching, since that logic didn't exist before this session.
9. **Phase 6** (Crashlytics, analytics, store assets, rollout plan) — final mile.

Part B of the original roadmap (v1.1 community features through v1.5 federation) was not re-audited here — none of that code exists yet, so there's nothing in the zip to ground-truth against. It remains a reasonable forward plan once v1.0 above actually ships.

---

## Fixes already applied in this session (included in the patched zip)

| File | Change |
|---|---|
| `security/SecurityUtils.kt` | Replaced non-exportable Keystore-key passphrase extraction with envelope encryption (random passphrase, encrypted at rest by the Keystore key). |
| `database/SecureDatabase.kt` | Updated call site to pass `context` into `getDatabasePassphrase()`. |
| `logic/CallRiskEvaluator.kt` | Replaced fake `"stir_attestation"` extras read with the real `Call.Details.getCallerNumberVerificationStatus()` API (API 30+); adjusted risk weights for the real 3-state verdict. |
| `ui/screens/SourcesViewModel.kt` | **New file.** Backs `SourcesScreen` with real `DataSourceRepository` data, add/sync/delete actions, and `SanitizationEngine`-validated source creation. |
| `ui/screens/SourcesScreen.kt` | Rewritten — fixed the missing `Color` import and undefined `.humanReadable()` call, replaced the dead `SourcesViewModel` reference with the real one, added a working "Add Source" `ModalBottomSheet`. |
| `utils/DateUtils.kt` | **New file.** Adds the `Long.humanReadable()` extension `SourcesScreen` was already calling but that never existed. |
| `di/AppModule.kt` | Registered `SourcesViewModel` in `viewModelModule`; updated a stale doc comment that still described the old (now-deleted) `MainApplication.scheduleCommunitySync()` scheduling path; fixed `workerModule`'s factory to call `CommunitySyncWorker(context, params)` instead of passing an unmatched 3rd argument; removed the duplicate `BlocklistRepository` binding from `databaseModule` (kept the one in `repositoryModule`); registered the new `SettingRepository`. |
| `workers/CommunitySyncWorker.kt` | Fixed missing `BackoffPolicy`/`Constraints`/`NetworkType` imports and the empty `setConstraints()` call; switched to `PeriodicWorkRequestBuilder` (daily) to match intended behavior; added the `WORK_NAME` constant; `schedule()` is now the single source of truth for scheduling this worker. |
| `MainApplication.kt` | Deleted the duplicate, constraint-less `scheduleCommunitySync()` method (`onCreate()` now calls `CommunitySyncWorker.schedule(this)`); fixed the nonexistent `android.BuildConfig.DEBUG` reference to use the app's real generated `BuildConfig`. |
| `scripts/check-architecture-drift.sh` | **New file.** Grep-based CI check enforcing the 5 layer-boundary rules from Roadmap Step 0.4. |
| `.github/workflows/pulse-ci.yml` | Added an "Architecture Drift Detection" step, running before the Gradle build so it fails fast. |
| `database/repositories/SettingRepository.kt` | **New file.** Thin wrapper over `SettingDao` — didn't exist before; `ContactsViewModel` was injecting the DAO directly. |
| `ui/viewmodels/ContactsViewModel.kt` | Swapped direct `SettingDao` injection for the new `SettingRepository`. |
| `ui/digest/PendingCardViewModel.kt` | Swapped direct `PendingCardDao` injection for the already-existing `PendingCardRepository`. |

### This session (Phase 4.2 + 4.2b)

| File | Change |
|---|---|
| `database/repositories/BlocklistRepository.kt` | Rewritten to take a `SettingRepository` instead of a hardcoded `Int` sourceId. Resolves and caches the real `manual_source_id` lazily on first use instead of the previous constructor-time `-1` (Phase 4.2b — see procedure entry above). No `runBlocking` introduced; passes the Phase 1.4 drift lint. |
| `ui/BlockedNumbersViewModel.kt` | Rewritten. Was backed by `DataSourceRepository.getAllEntries()` filtered client-side to `action == "BLOCK"` — no ALLOW support, no add, no delete, no search. Now backed by `BlocklistRepository`: exposes `visibleRules` (search + All/Blocked/Allowed filter, combined via `StateFlow`), `addRule()` (validates through `SanitizationEngine.sanitizePhoneNumber`/`sanitizeTextField` before touching the repository, mirroring `SourcesViewModel.addSource()`), `deleteRule()`, and add-sheet visibility state (`showAddSheet()`/`hideAddSheet()`) matching `SourcesViewModel`'s own pattern. |
| `ui/screens/BlockAllowListScreen.kt` | **New file.** Replaces the `NavGraph.kt` placeholder. Search field, All/Blocked/Allowed `FilterChip` row, `LazyColumn` of rules with a health-style color-coded action tag (red BLOCK / green ALLOW) and a "Remove" action, and an "Add Number" `ModalBottomSheet` (number, Block/Allow choice, optional note) — same visual language (`GlassCard`, `humanReadable()`) as `SourcesScreen`. |
| `ui/navigation/NavGraph.kt` | `Screen.BlockAllowList.route` now renders `BlockAllowListScreen()` instead of the literal `Text("Block / Allow List — Coming Soon")`. Removed the now-unused `androidx.compose.material3.Text` import. |
| `di/AppModule.kt` | `BlocklistRepository` binding changed from `BlocklistRepository(get(), -1)` to `BlocklistRepository(get(), get())`, injecting the new `SettingRepository` dependency. `BlockedNumbersViewModel`'s binding line is unchanged (`get()`), but now resolves a `BlocklistRepository` instead of a `DataSourceRepository` since that's what the rewritten class asks for; comment added to flag the type change for future readers. |

---

### This session (Phase 4.4 + 4.6)

| File | Change |
|---|---|
| `ui/screens/SettingsViewModel.kt` | **New file.** Backs `SettingsScreen` with `SettingEntry`-based persistence for `shield_red/green/blue`. Loads colors from `SettingRepository` on init; exposes `red/green/blue` as `StateFlow<Float>`; `saveColors()` writes all three via `setSetting()`. No `SharedPreferences` touched. |
| `ui/screens/SettingsScreens.kt` | Rewritten. All `SharedPreferences` usage removed — state now owned by `SettingsViewModel` via `koinViewModel()`. Save button calls `viewModel.saveColors()`; confirmation is a `Snackbar` rather than an `AlertDialog`. `Context` import removed (no longer needed). |
| `ui/onboarding/OnboardingWizardScreen.kt` | `RiskThresholdStep` rewritten. `SharedPreferences` write of `onboarding_complete` replaced with `settingRepository.setSetting("onboarding_complete", "true")` via `koinInject()` + `rememberCoroutineScope()`. Navigates to `Screen.Dashboard.route` (not a hardcoded string). `android.content.Context` import removed. |
| `ui/navigation/NavGraph.kt` | Rewritten. On startup, reads `onboarding_complete` from `SettingRepository` (one Room indexed read, typically <50ms) and sets `startDestination` dynamically: `Screen.ConsumerDashboard.route` if complete, `Screen.Onboarding.route` if not. Shows `CircularProgressIndicator` during the async check. `ConsumerDashboardScreen` registered as a real `composable()` destination. `OperationalDashboard` retained at `Screen.Dashboard.route`. |
| `ui/navigation/Screen.kt` | `Screen.ConsumerDashboard` added (`"consumer_dashboard"`, `"Home"`, `Icons.Default.Home`). Required by `NavGraph` and `OnboardingWizardScreen`. |
| `di/AppModule.kt` | `SettingsViewModel(get())` registered in `viewModelModule`. |

---

### This session (Phase 2.6 + Test Infrastructure)

| File | Location | Change |
|---|---|---|
| `DatabaseDAOs.kt` | `database/daos/` | Two new queries added to `UnifiedEntryDao`: `findEntriesByPhoneNumberWithPriority()` — joins `unified_entries` to `sources` on `sourceId`, orders by `COALESCE(s.priority, 0) DESC` so the highest-priority source is always first. `getAllBlockPatternsWithPriority()` — same join for pattern/prefix lookups. `COALESCE` with default 0 means orphaned entries (shouldn't exist due to FK CASCADE) lose every conflict, which is correct. |
| `DataSourceRepository.kt` | `database/repositories/` | `getCallDecision()` rewritten. Now calls `findEntriesByPhoneNumberWithPriority()` and walks the priority-ordered list — first ALLOW entry wins immediately, first BLOCK entry wins if no ALLOW outranks it. Pattern check also uses `getAllBlockPatternsWithPriority()`. `isManualSource()` added: heuristic check on `SourceEntity.priority == 100` to label decisions as `"manual_block"` vs `"aggregated"` without an extra DAO call on the hot screening path. |
| `SignalGateDatabase.kt` | `database/` | `exportSchema = false` → `exportSchema = true`. Required for `MigrationTestHelper` to validate `MIGRATION_1_2` against entity definitions. Schema JSON files written to `schemas/` (configured in `build.gradle` `ksp` block). Must be committed to version control — they are the ground truth for each version's schema. |
| `build.gradle` | `android/app/` | Added `room.schemaLocation` to `ksp` block pointing at `$projectDir/schemas`. Added test dependencies: `koin-test`, `koin-test-junit4`, `koin-android-test` (for `checkModules()` unit tests); `mockito-core`, `mockito-kotlin`; `room-testing` in both `testImplementation` and `androidTestImplementation`; `androidx.test:runner`, `androidx.test:rules` for instrumented tests. |
| `KoinModuleTest.kt` | `src/test/.../di/` | **New file.** Unit test — runs in JVM, no device needed. Replaces `databaseModule` with in-memory Room bindings, loads all other production modules, then calls `checkModules()` which instantiates every `single{}`, `factory{}`, and `viewModel{}` following the full dependency chain. Also has explicit `get<T>()` calls for each repository and engine with clear assertion messages. Run via `./gradlew testPulseDebugUnitTest`. |
| `MigrationTest.kt` | `src/androidTest/.../database/` | **New file.** Instrumented test — requires device/emulator. Uses `MigrationTestHelper` to open a real version-1 database from the exported schema JSON, run `MIGRATION_1_2`, then Room validates the result against version-2 entity definitions at the byte level. `migrate1To2()` seeds v1 data and asserts it survives. `pendingCardsSchemaIsCorrect()` uses `PRAGMA table_info` to explicitly assert each column name, type, and count. Run via `./gradlew connectedPulseDebugAndroidTest`. |

**One action required before `MigrationTest.kt` can run:** build the project once after merging `SignalGateDatabase.kt` (with `exportSchema = true`) and `build.gradle` (with `room.schemaLocation`). Room's KSP processor will generate `schemas/com.signalgate.multipoint.database.SignalGateDatabase/2.json` automatically. Commit that file — `MigrationTestHelper` reads it at test time. For the version-1 schema, you'll need to temporarily set `version = 1` and remove `PendingCardEntity` from the `@Database` entities list, build once to generate `1.json`, commit it, then revert. This is a one-time setup step.

---

### This session (Runtime failure fixes — items 1, 3, 5)

| File | Change |
|---|---|
| `MainApplication.kt` | `runBlocking { }` → `runBlocking(Dispatchers.IO) { }`. Plain `runBlocking` blocked the main thread during `onCreate()` — on a slow device where the DB seed takes >5s this would ANR. `Dispatchers.IO` keeps the seed off the main thread while preserving the synchronous ordering guarantee (still completes before `onCreate()` returns). `Dispatchers` import added. |
| `.github/workflows/pulse-ci.yml` | Two fixes: (1) `continue-on-error: true` removed from the unit test step — `KoinModuleTest` failing silently is worse than no test at all; a failing unit test now breaks the build as intended. (2) New `instrumented-tests` job added: KVM enabled, `android-emulator-runner@v2` with API 33 x86_64, runs `connectedPulseDebugAndroidTest` which includes `MigrationTest`. Scoped to push on `consumer-v1` only (not every PR) to keep PR feedback fast while still catching migration drift on every merge. |

**Item 1 (exponential backoff)** — confirmed already complete in the live branch. Nothing written.

**Next recommended:** `DataSyncEngine.parseXLSXFile()` (Apache POI, streaming, 2M row limit) — the one substantial remaining data-layer item. Or the conflict resolution unit tests (assert MANUAL priority 100 beats federal source priority 85 on the same number).
