# TRUTH-CF-2 — Behavior as Implemented

> **Snapshot:** this document describes the code at commit `a3bae8a083f1f5e86728b98876644b4a76ae99bf` only. It is not updated as the code changes; verify against live source before relying on it.

**READ-ONLY.** Describes what the code in the supplied zip does. No fixes, recommendations, or decisions. Nothing was executed; every row is from reading source (`VERIFIED-CODE`) unless tagged otherwise. Paths are relative to `android/app/src/main/java/com/signalgate/pulse/` unless shown.
Companion files: `TRUTH-CF-1-Inventory.md`, `TRUTH-CF-3-Audit-Register.md`. Commit SHA: a3bae8a083f1f5e86728b98876644b4a76ae99bf (from the zip archive comment; no `.git` inside). Refreshed 2026-10-03 from baseline `c5e34da5b3ff3af280e5b44040d7432895b4c9db`; the only code change is logging in `SignalGateCallScreeningService.kt` (see TRUTH-CF-1 §0a), so line numbers in that file after `:194` moved.

## A. Screening trace (incoming call)
| # | Step | Where | Thread / dispatcher | Timeout | Failure behavior |
|---|---|---|---|---|---|
| 1 | Telecom calls the screening service | `SignalGateCallScreeningService.kt` (`onScreenCall` → `handleScreeningRequest`, line ~78) | Telecom binder thread | none | n/a |
| 2 | Extract number from `details.handle` | `:91` area | caller thread | none | null/blank handle → `onSecurityFailure("UNKNOWN_MALFORMED_HANDLE")` and return |
| 3 | Launch work on service scope | `:58` scope = `SupervisorJob() + Dispatchers.Default.limitedParallelism(4)` | Default, parallelism 4 | none | n/a |
| 4 | Resolve Koin dependencies (`by inject()` x5: engine, call-log repo, pending-card repo, haptics, limiter) | `:52-56` read at `:101-107` inside `executeScreeningSafely` (`:98`, defined `:135`) | Default | **none** (before the clock starts; comment at `:100` says "Resolve dependencies before the measured decision begins") | any exception → `onSecurityFailure(number)` (`:143-147`) |
| 5 | Start decision clock | `:171` `withTimeout(3_500) { screen() }` | Default | **3,500 ms** | `TimeoutCancellationException` → `onSecurityFailure(number)` (`:143`) |
| 6 | Engine: sanitize + normalize number | `logic/CallScreeningEngine.kt:73-76` | Default | inside 3,500 ms | any exception → `buildSecurityFailureInfo` (typed `SECURITY_FAILURE`) (`:~88-92`) |
| 7 | Repository decision | `database/repositories/DataSourceRepository.kt:255` `getCallDecision` | Room/IO | inside 3,500 ms | exception propagates to step 6 |
| 8 | Map decision → tier (ALLOW / BLOCK / else gray-zone) | `CallScreeningEngine.kt:78-85`, builders `:97-250` | Default | inside 3,500 ms | see step 6 |
| 9 | **Respond to Telecom** | `:176` `respond(responseFactory(decision.callAction))` | Default | n/a | n/a |
| 10 | Persist audit / review card | `:195-196` `withContext(NonCancellable) { withTimeoutOrNull(2_000) { persist(...) } }`; result now checked: null → `AUDIT_PERSIST_TIMEOUT` (error log, `:205`), else `AUDIT_PERSIST_SUCCESS` (info log, `:207`) | Default | **2,000 ms** | timeout is logged, not thrown; exception caught and logged; no second response (`:209`) |
| 11 | UX dispatch (notification, haptics) | `:216` `dispatchUx(...)` | Default | none | exception caught and logged |
| F | Security-failure path | `handleSecurityFailure` (`:269-309`) | | audit write `withTimeoutOrNull(2_000)` (`:298`), result logged as `AUDIT_PERSIST_TIMEOUT` (`:300`) or `AUDIT_PERSIST_SUCCESS` (`:302`) | response issued first via `responseFactory(ScreeningAction.SECURITY_FAILURE)` (`:276`); audit-write failure logged (`:305`) |

**Authoritative vs cached:** the encrypted DB is queried via `entryDao` after a Bloom pre-check (`DataSourceRepository.kt:~262-268`). If `bloomReady` is false, the Bloom pre-check is skipped and the DB is queried (`:262` `!bloomReady || ...`). `bloomReady` starts false (`:106`) until rehydration completes (`AppModule.rehydrateBloomFiltersInBackground`, launched without awaiting from `MainApplication.kt:~123-125`).

### A1. Response mapping (`SignalGateCallScreeningService.kt:~235-261`)
| ScreeningAction | disallowCall | silenceCall | skipCallLog | skipNotification |
|---|---|---|---|---|
| ALLOW, SCREEN, **SECURITY_FAILURE** | false | false | false | false |
| BLOCK | false | **true** | **true** | **true** |

`SECURITY_FAILURE` shares the same policy branch as ALLOW and SCREEN (`:236`) — the call rings through. A KDoc above it (`:~229-234`) calls this "a deliberate, visible policy choice." `ScreeningServiceCallResponseMappingTest.kt` exists (assertions not read: UNKNOWN). BLOCK therefore silences the call without disallowing it (`disallowCall=false`) and skips the system call log and system notification (`:~244-249`).

**Persistence logging (new at this baseline):** both shielded writes now emit `AUDIT_PERSIST_SUCCESS` on completion and `AUDIT_PERSIST_TIMEOUT` when the 2,000 ms bound cuts them off (`withTimeoutOrNull` returns null rather than throwing). Before this change a timeout was indistinguishable from success in logcat. No behavior or bound changed. `NonCancellable` shields against scope cancellation only; the code comment and the ledger both state it does not protect against the hosting process being killed (CLAIMED-IN-CODE / CLAIMED-IN-DOC). `scripts/verify-launch-and-capture.sh` now has a diagnostic for that case (kill -9 after call #3, `PROCESS_KILL_TEST_RESULT`), invoked from both `crash-diagnostic.yml` jobs; whether it has ever run is UNKNOWN (ledger says not yet).

## B. Decision precedence as implemented (`DataSourceRepository.getCallDecision`, `:255-330`)
1. Blank normalized number → `ALLOW`, source `"default"`, reason "Invalid number" (`:257-259`).
2. Bloom fast-pass: if bloom is ready and neither the exact filter nor the prefix filter might match → `ALLOW`, source `"default"` (`:~262-268`).
3. Exact matches: `findEntriesByPhoneNumberWithPriority` (`database/daos/DatabaseDAOs.kt:~166-172`): joins `sources`, **only rows with `s.isEnabled = 1`**, `ORDER BY COALESCE(s.priority,0) DESC, ue.id ASC`. The **first row with action ALLOW or BLOCK decides**:
   - ALLOW → source label `"manual_allow"` — assigned by action alone, regardless of which source owns the row.
   - BLOCK → label `"manual_block"` if `isManualSource(sourceId)`, else `"aggregated"`.
4. Pattern matches (BLOCK only): `findMatchingBlockPatternsWithPriority` (`:~183-191`), same enabled/priority/id ordering; first row → `"pattern"`.
5. No match → `ALLOW`, `"default"`.
Engine tiers: `manual_allow` → Tier 1 ALLOWLISTED; `aggregated` and `manual_block` → Tier 2 FEDERAL_BLOCK (BLOCK, silence); `pattern` with confidence ≥ 70 → Tier 3 HEURISTIC_BLOCK; pattern < 70 → Tier 4 HEURISTIC_FLAG → ScreeningAction.SCREEN (`CallScreeningEngine.kt:13-22,127-137`). Note `buildBlockInfo` receives the decision's source/confidence; threshold logic lives there.

**Priorities seeded/defined:** Manual User Rules 100, Contacts Allow List 100 (`database/DatabaseInitializer.kt:25-37`); FTC 90 and FCC 85 (`logic/ReliableSourceManager.kt:133,141`).
**Ties:** equal-priority rows are ordered by `ue.id ASC` (`DatabaseDAOs.kt:171`). Manual and Contacts share priority 100, so a number present in both is decided by lower entry id. Stored explicit tie rule other than this: NOT PRESENT.
**Engine use of `SourceEntity.priority`:** yes, through the SQL ordering above only.
**`PrecedenceEngine`:** class exists (`data/security/PrecedenceEngine.kt`) and is registered in Koin with two empty `HashSet`s (`di/AppModule.kt:179-183`). No other production or test reference to it was found by `grep -rn PrecedenceEngine`. Its `evaluateIncomingCall` has a single occurrence (definition).
**Contacts-vs-blocklist, FTC-vs-FCC:** no dedicated logic; resolved only by priority/id ordering above.
**Stale source:** no staleness check in `getCallDecision` (UNKNOWN whether elsewhere; none found in this function).
**Disabled source:** excluded by `s.isEnabled = 1` in both queries. Rows with a missing source row are excluded too (the `WHERE s.isEnabled = 1` filter removes NULL-source rows despite the LEFT JOIN comment at `DatabaseDAOs.kt:~160-164`).

## C. Startup (`MainApplication.kt`)
| Step | Evidence | Thread |
|---|---|---|
| `SecurityUtils.enableStrictMode()` | `:80` | main |
| Plant Timber tree (Debug or Release) | `:83,:90` | main |
| `startKoin { androidContext; modules(appModule) }` | `:98-101` | main |
| `runBlocking { initializeDatabase(this) }` inside try/catch | `:108-121` | **main (blocking)** |
| `initializeDatabase`: seed required sources, then `ReliableSourceManager.ensureFederalRows()` | `di/AppModule.kt:~306-307` | runBlocking; `ensureFederalRows` switches to `Dispatchers.IO` |
| Bloom rehydration launched in `applicationScope` (not awaited) | `:~123-125` | background |
| `NotificationChannelManager.createAllChannels` | `:~129` | main |
| `CommunitySyncWorker.schedule(this)` | `:~131` | main |
| `AppReadiness.isReady.value = true` | `:142` | main |

**If DB init throws:** the `catch (e: Exception)` logs "FATAL: Database initialization failed…" via `Timber.e` and execution continues (`:114-120`); `AppReadiness.isReady` is still set true at `:142`. No failed-state value exists in `AppReadiness` (`:161-162`: a single `MutableStateFlow<Boolean>`). Consumer of `AppReadiness`: `MainActivity.kt:28` splash `setKeepOnScreenCondition { !AppReadiness.isReady.value }`.
**Cold-start native load:** `useLegacyPackaging = false` set to avoid slow `loadLibrary("sqlcipher")` (`app/build.gradle`, comment cites ~4.9 s).

## D. Source matrix
| Source | name | type | pathOrUrl | priority | Created by | Syncable | Deletable (repo) | Toggle (repo) | Toggle in Sources UI |
|---|---|---|---|---|---|---|---|---|---|
| FTC | "FTC Do Not Call Registry" | `FTC` | mirror URL (`ReliableSourceManager.kt:103-104`) | 90 | `ReliableSourceManager.ensureSourceRow`/`ensureFederalRows` (`:~205-215`, `:552-561`) — not seeded by `DatabaseInitializer` | yes (`SOURCES`) | no — `ProtectedSourceDeletionException` (`DataSourceRepository.kt:147-149`) | unrestricted (`:154`) | switch shown (type FTC/FCC → `isRemoteSource`, `SourcesScreen.kt:101`) |
| FCC | "FCC Consumer Complaints" | `FCC` | `opendata.fcc.gov/.../rows.csv` | 85 | same as FTC | yes | no | unrestricted | switch shown |
| Manual User Rules | "Manual User Rules" | `MANUAL` | `local` | 100 | `DatabaseInitializer.seedRequiredSources` (`:25-28`) | no (`syncSource` rejects non-federal) | no | unrestricted in repo | read-only status; switch hidden (`SourcesScreen.kt:103` `isManualUserRules`) |
| Contacts Allow List | "Contacts Allow List" | `MANUAL` | `contacts` | 100 | `DatabaseInitializer.seedRequiredSources` (`:34-37`) | no | no | unrestricted in repo | switch shown; Remove hidden (`SourcesScreen.kt:146,178`) |

`PROTECTED_SOURCE_TYPES` = persisted values of enum `database.entities.SourceType` = {MANUAL, FTC, FCC} (`DatabaseEntities.kt:15-18`; `DataSourceRepository.kt:92`). There is **no CONTACTS type**. A second, unrelated enum `data.models.SourceType { REMOTE_URL, LOCAL_CSV }` also exists (`data/models/ThreatSource.kt:4`).
**What `CommunitySyncWorker` actually syncs:** the class `workers/CommunitySyncWorker.kt` calls `ReliableSourceManager.syncAllFederalSources()` (and `DataSyncEngine` is injected; use there not re-read) — FTC and FCC only, per `SOURCES`. No source named "community" exists in `SOURCES` or seeding.
**Manual rule entry points:** `SecurityRuleRepository.addManualBlock` (`:79`), `addManualAllow` (`:96`), `removeRule` (`:176`), `getAllUserRules` (`:180`). **Contacts:** import via `ContactsViewModel`; `skipContactImport()` only sets `_saveError=null; _isSaved=true` (`ui/viewmodels/ContactsViewModel.kt:151-154`) and does not touch the source row.
**14-day / 250-entry retention rule:** grep for `250`, `14 days`, `retention`, `TimeUnit.DAYS` across `src/main` found nothing → **NOT PRESENT in production code**.

## E. Source sync (FTC / FCC)
| Aspect | FTC | FCC |
|---|---|---|
| Endpoint | `raw.githubusercontent.com/avignonbo-jpg/signalgate-dnc-mirror/dnc-mirror-pulse/dnc-numbers.json` + `.manifest.json` (`ReliableSourceManager.kt:103-105`) | `https://opendata.fcc.gov/api/views/vakf-fz8e/rows.csv?accessType=DOWNLOAD`; fallback `...?$limit=50000` (`:107-111`) |
| Fallback | `ftc.gov/.../DNC_Complaint_Numbers.csv` (comment says liveness unverified, `:~116-129`) | the `$limit` URL |
| Format | JSON snapshot (`FetchStrategy.FTC_REST_API`) | CSV (`FetchStrategy.CSV`) |
| Authenticity | manifest fetched and `ArtifactAuthenticityVerifier.parseManifest/verify`; rejection → exception "Snapshot authenticity rejected" → lifecycle `REJECTED` (`:369-380,:582`) | **NOT PRESENT** in `fetchCsvSnapshot` (`:425-505` contains no verifier call; grep for `verify`/`manifest` hits only `:371-379`) |
| Size/limits | `MAX_ENTRIES_PER_SOURCE = 50_000` (`:94`), `MAX_NUMBER_LENGTH = 15` (`:96`), body bound from `SnapshotSanityValidator.Limits().maxBytes` (`:516,:528-536`) | same constants |
| Persistence | `SecurityRuleRepository.replaceSourceSnapshot` (transactional per Contract INV-002 — body not re-read here) | same |
| HTTP | OkHttp, connect/read timeouts `CONNECT_TIMEOUT_SEC`/`READ_TIMEOUT_SEC` (`:98-100`; values not captured) | same |
| Schedule | `CommunitySyncWorker`: `PeriodicWorkRequest` 24 h, exponential backoff 10 min, constraints network CONNECTED + battery-not-low, unique name `community_sync`, `ExistingPeriodicWorkPolicy.KEEP` (`workers/CommunitySyncWorker.kt:57-100`) | same worker |
| Disabled rows | `shouldSyncAutomatically` returns `source?.isEnabled != false` (`:146-148`): missing row → true; disabled row → false | same |

**Sync state and locking (Task 3 code present):** `syncMutex` wraps the private per-source sync body (`:245` `syncMutex.withLock`); `ensureSourceRowMutex` separately guards row creation (`:552`); `inFlightSyncCount: MutableStateFlow<Int>` updated in `trackSyncing` with decrement in `finally` (`:185-198`); `isSyncing: Flow<Boolean>` = count > 0, `distinctUntilChanged`. Public `syncAllFederalSources()` (`:220`) and `syncSource(id)` (`:234`) are wrapped in `trackSyncing`. `CommunitySyncWorker` and `SourceSyncUseCase` resolve the same Koin singleton `ReliableSourceManager` (`AppModule.kt:190-191`; worker by `by inject()`).
**Dashboard duplicates:** `DashboardViewModel` defines `toggleSourceEnabled` (`:175`), `syncSource` (`:188`), `syncAllSources` (`:203`), `isSyncing` (`:53`, delegated to the use case). `grep` in `src/main` finds no call to these three methods on a Dashboard view-model instance (the `viewModel.syncSource`/`syncAllSources` calls at `SourcesScreen.kt:53,83` use `SourcesViewModel`). `ConsumerDashboardScreen.kt:72` reads `sourceActionError`.

## F. Workers, notifications, settings
**Worker:** `CommunitySyncWorker` (above); `SyncBootReceiver` (BOOT_COMPLETED, MY_PACKAGE_REPLACED): on either action it logs and calls `CommunitySyncWorker.schedule(context)`; any other action returns early (`workers/SyncBootReceiver.kt:27-33`, VERIFIED-CODE). Its KDoc says `schedule()` uses `ExistingPeriodicWorkPolicy.KEEP`, so repeated calls do not create duplicate work (CLAIMED-IN-CODE; `schedule()` itself not re-inspected for this note). Actual boot/update delivery and WorkManager execution: UNKNOWN (no runtime evidence).
**Notification channels** (`ui/notifications/NotificationChannelManager.kt`): `blocked_call_review` (HIGH, no sound/vibration), `sync_status` (DEFAULT), `security_alert` (HIGH, vibration on). Header comment says `sync_status` and `security_alert` are reserved and nothing posts to them (CLAIMED-IN-CODE; not independently grepped for posters). The `sync_status` channel description still reads "Background sync progress for federal blocklist sources."
**Setting keys** (`database/repositories/SettingKeys.kt`): `onboarding_complete`, `eula_accepted`, `eula_version`, `eula_accepted_at`, `shield_red`, `shield_green`, `shield_blue`, `heuristics_mode`; constant `EULA_CURRENT_VERSION = "placeholder-v0"` (`:32`). Defaults for keys: NOT captured (UNKNOWN).

## G. Could not verify (this file)
HTTP timeout values; `replaceSourceSnapshot` internals; behavior of any test (nothing executed); runtime logs (none supplied); setting defaults.
