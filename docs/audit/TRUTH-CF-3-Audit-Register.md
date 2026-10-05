# TRUTH-CF-3 — Audit Register

> **Snapshot:** this document describes the code at commit `a3bae8a083f1f5e86728b98876644b4a76ae99bf` only. It is not updated as the code changes; verify against live source before relying on it.

**READ-ONLY.** Observations from the supplied zip only; no fixes, recommendations, or decisions. Nothing was executed (no Gradle, no network), so "test exists" never means "test passes".
Companions: `TRUTH-CF-1-Inventory.md`, `TRUTH-CF-2-Behavior.md`. Commit SHA: a3bae8a083f1f5e86728b98876644b4a76ae99bf (zip archive comment). Refreshed 2026-10-03 from baseline `c5e34da5b3ff3af280e5b44040d7432895b4c9db`; see TRUTH-CF-1 §0a for the 4 changed files. Tags: `VERIFIED-CODE`, `VERIFIED-CI`, `CLAIMED-IN-DOC`, `NAME-ONLY` (test class exists by name; assertions not read), `NO TEST FOUND`, `UNKNOWN`.

## A. Invariants stated in `Architecture-Contract.md` (lines 134-143)
| ID | Contract statement (abridged, quoted from doc) | Enforcing code seen | Test files named for it | Tag |
|---|---|---|---|---|
| INV-001 | "The encrypted Room/SQLCipher database is the authoritative source of active security policy; Bloom filters, indexes, caches… are non-authoritative accelerators." Status in doc: SATISFIED | `DataSourceRepository.getCallDecision` queries DAO after a Bloom pre-check (`:255-330`); `bloomReady=false` → DB path (`:262`) | `androidTest/.../BloomAuthoritativeDecisionTest.kt`, `BloomPostCommitOrderingTest.kt` (cite INV-001) | VERIFIED-CODE + NAME-ONLY (instrumented only; CI-run status UNKNOWN) |
| INV-002 | "Last-Known-Good Security Dataset… `replaceSourceSnapshot()` is transactional (`withTransaction {}`), rejects empty candidates…" | `logic/SecurityRuleRepository.kt:193` (`replaceSourceSnapshot`), `:254` (batched) — body not re-read | `androidTest/.../SourceActivationTransactionTest.kt` (cites INV-002) | CLAIMED-IN-DOC + NAME-ONLY |
| INV-003 | "Explicit Security Failure… SECURITY_FAILURE is a structurally distinct CallTier/ScreeningAction, enforced by ScreeningDecision's own init invariant" | `logic/ScreeningAction.kt:28-32`; `logic/CallScreeningEngine.kt:88-92`; `SignalGateCallScreeningService.kt:236` (response policy rings through) | `test/.../CallScreeningEngineSecurityFailureTest.kt` (cites INV-003), `ScreeningServiceCallResponseMappingTest.kt` | VERIFIED-CODE + NAME-ONLY |
| INV-004 | "External Input Is Untrusted. Unchanged from v3; not re-audited" | validators exist: `SecureCsvParser`, `SnapshotSanityValidator`, `SourceRecordValidator`, `ArtifactAuthenticityVerifier` (FTC path only, see TRUTH-CF-2 §E) | `SecureCsvParserLimitTest`, `SnapshotSanityValidatorTest`, `SourceRecordValidatorTest`, `ArtifactAuthenticityVerifierTest` | NAME-ONLY |
| INV-005 | "Deterministic Security Decisions. Unchanged from v3; not re-audited" | priority/id ordering in `DatabaseDAOs.kt:171,190` | `CallScreeningEngineDecisionMatrixTest` | NAME-ONLY |
| INV-006 | "Decision Side Effects Follow the Decision Contract… `ScreeningDecision.forTier()`" | `logic/ScreeningDecision.kt`; consequences executed in service (`:176-205`) | `ScreeningDecisionConsequencesTest` | NAME-ONLY |
| INV-007 | "No Raw PII in Operational Logs" | `SanitizationEngine` applied in `CallScreeningEngine.kt:73-76`; `StartupDiagnostics` fixed names (per doc) | `NotificationPrivacyTest` | NAME-ONLY |
| INV-008 | "Protected Source Lifecycle… PROTECTED_SOURCE_TYPES = {MANUAL, FTC, FCC}" | `DataSourceRepository.kt:92,147-149`; enum `DatabaseEntities.kt:15-18` | `test/.../DataSourceRepositoryDeletionTest.kt` (cites INV-008) | VERIFIED-CODE + NAME-ONLY |
| INV-009 | "Edge-to-Application Boundary… satisfied for CallActionReceiver" | `CallActionReceiver.kt` (exported=false in manifest) | `CallActionReceiverBehaviorTest` | NAME-ONLY |
| INV-010 | "Release Gates Are Mandatory… check-architecture-drift.sh is a required CI gate; unit tests' continue-on-error status not re-checked" | `scripts/check-architecture-drift.sh` invoked in `pulse-ci.yml:31-32` | none (CI) | VERIFIED-CI for the script call; "required" status UNKNOWN (branch protection not visible) |

## B. Test inventory (counted by `@Test` occurrences)
Totals: **117** JVM unit-test methods in 29 files; **46** instrumented in 10 files; 163 combined. The zip contains `build_log_full.txt`; its first lines are a GitHub connection error, so no build or test results are available from it.


### JVM unit tests
| File | @Test |
|---|---|
| `kotlin/com/signalgate/pulse/CallActionReceiverBehaviorTest.kt` | 4 |
| `kotlin/com/signalgate/pulse/NotificationPrivacyTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/ScreeningServiceCallResponseMappingTest.kt` | 1 |
| `kotlin/com/signalgate/pulse/ScreeningServiceDeadlineTest.kt` | 6 |
| `kotlin/com/signalgate/pulse/ScreeningServiceEdgeExecutionTest.kt` | 22 |
| `kotlin/com/signalgate/pulse/ScreeningServiceTimingBudgetTest.kt` | 1 |
| `kotlin/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifierTest.kt` | 5 |
| `kotlin/com/signalgate/pulse/data/security/SecureCsvParserLimitTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/data/security/SnapshotSanityValidatorTest.kt` | 5 |
| `kotlin/com/signalgate/pulse/data/security/SourceRecordValidatorTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryDeletionTest.kt` | 6 |
| `kotlin/com/signalgate/pulse/di/KoinModuleTest.kt` | 4 |
| `kotlin/com/signalgate/pulse/logic/CallScreeningEngineDecisionMatrixTest.kt` | 7 |
| `kotlin/com/signalgate/pulse/logic/CallScreeningEngineSecurityFailureTest.kt` | 1 |
| `kotlin/com/signalgate/pulse/logic/DataSyncEngineXlsxLimitTest.kt` | 6 |
| `kotlin/com/signalgate/pulse/logic/ReliableSourceManagerDisabledSourceTest.kt` | 3 |
| `kotlin/com/signalgate/pulse/logic/ReliableSourceManagerEnsureFederalRowsTest.kt` | 5 |
| `kotlin/com/signalgate/pulse/logic/ReliableSourceManagerPolicyTest.kt` | 1 |
| `kotlin/com/signalgate/pulse/logic/ReliableSourceManagerSyncStateTest.kt` | 5 |
| `kotlin/com/signalgate/pulse/logic/ScreeningDecisionConsequencesTest.kt` | 7 |
| `kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryContactImportBoundaryTest.kt` | 1 |
| `kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryMutationBoundaryTest.kt` | 3 |
| `kotlin/com/signalgate/pulse/logic/SourceSyncUseCaseTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/security/SecurityUtilsTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/ui/navigation/NavGraphRoutePolicyTest.kt` | 3 |
| `kotlin/com/signalgate/pulse/ui/notifications/PulseTriggerLimiterTest.kt` | 4 |
| `kotlin/com/signalgate/pulse/ui/onboarding/OnboardingViewModelEulaTest.kt` | 3 |
| `kotlin/com/signalgate/pulse/ui/screens/SourcesViewModelDeletionTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/utils/PhoneNumberUtilsTest.kt` | 2 |

### Instrumented tests
| File | @Test |
|---|---|
| `kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt` | 3 |
| `kotlin/com/signalgate/pulse/GrayZoneReviewabilityTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/StartupTimingTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/database/MigrationTest.kt` | 4 |
| `kotlin/com/signalgate/pulse/database/SourceDeletionCascadeTest.kt` | 3 |
| `kotlin/com/signalgate/pulse/database/repositories/BloomAuthoritativeDecisionTest.kt` | 7 |
| `kotlin/com/signalgate/pulse/database/repositories/BloomPostCommitOrderingTest.kt` | 2 |
| `kotlin/com/signalgate/pulse/database/repositories/DecisionMatrixRepositoryTest.kt` | 6 |
| `kotlin/com/signalgate/pulse/logic/SourceActivationTransactionTest.kt` | 6 |
| `kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt` | 11 |

### B1. Behavior → test coverage (by test file name only; assertions not read)
| Behavior | Test file(s) found by name | Tag |
|---|---|---|
| Allow / block decision matrix | `CallScreeningEngineDecisionMatrixTest`, `androidTest/.../DecisionMatrixRepositoryTest` | NAME-ONLY |
| Precedence between sources | covered only through the decision-matrix tests above; no test names `PrecedenceEngine` | NO TEST FOUND by name for PrecedenceEngine |
| Screening deadline (3,500 ms) | `ScreeningServiceDeadlineTest`, `ScreeningServiceTimingBudgetTest`, `androidTest/.../StartupTimingTest` | NAME-ONLY |
| Pre-clock dependency resolution timeout | no test name indicates it | NO TEST FOUND by name |
| Security failure / ring-through mapping | `ScreeningServiceCallResponseMappingTest`, `CallScreeningEngineSecurityFailureTest`, `ScreeningServiceEdgeExecutionTest` | NAME-ONLY |
| DB unavailable / init failure at startup | no test names `initializeDatabase`/`AppReadiness` failure | NO TEST FOUND by name |
| Source sync failure / malformed source | `SnapshotSanityValidatorTest`, `SourceRecordValidatorTest`, `SecureCsvParserLimitTest`, `DataSyncEngineXlsxLimitTest`, `ReliableSourceManagerPolicyTest` | NAME-ONLY |
| FTC authenticity | `ArtifactAuthenticityVerifierTest` | NAME-ONLY |
| FCC authenticity | none (no mechanism present) | NO TEST FOUND |
| Concurrent sync / cancellation | `ReliableSourceManagerSyncStateTest`, `SourceSyncUseCaseTest` | NAME-ONLY |
| Federal row seeding / disabled rows | `ReliableSourceManagerEnsureFederalRowsTest`, `ReliableSourceManagerDisabledSourceTest` | NAME-ONLY |
| Protected-source delete / UI error | `DataSourceRepositoryDeletionTest`, `SourcesViewModelDeletionTest`, `androidTest/.../SourceDeletionCascadeTest` | NAME-ONLY |
| Protected-source disable/toggle | none by name (repository toggle has no guard in code) | NO TEST FOUND |
| Contacts switch visible / Manual switch hidden (UI) | none by name | NO TEST FOUND |
| Duplicate-number handling | none by name | NO TEST FOUND by name |
| Migrations | `androidTest/.../MigrationTest` | NAME-ONLY |
| Navigation route policy | `NavGraphRoutePolicyTest`, `androidTest/.../DrawerNavigationBackStackTest` | NAME-ONLY |
| Koin graph | `KoinModuleTest` | NAME-ONLY |
| SQLCipher / Keystore | `SecurityUtilsTest`, `androidTest/.../SecurityUtilsInstrumentedTest` | NAME-ONLY |
| Process killed mid-screening (audit write lost?) | no unit/instrumented test; CI diagnostic step in `scripts/verify-launch-and-capture.sh` invoked by `crash-diagnostic.yml` (both jobs), non-blocking; ledger says not yet run | VERIFIED-CI (wiring only); result UNKNOWN |
| R8 / release-build behavior | no test; release assemble only in `pulse-ci.yml` `build-release` (push to `consumer-v1`) | VERIFIED-CI (build only) |

## C. Dead or unreferenced code (zero references by name across `src/`; scripted)
Method: for every `fun` in `src/main`, count whole-word occurrences of its name across all Kotlin under `src/`; report those with exactly one occurrence (the definition). **Limitation:** a name shared by several classes is never flagged, and reflection/Compose-preview use is invisible. Coverage: 18 flagged.
| File | Symbol |
|---|---|
| `database/daos/DatabaseDAOs.kt` | `updateEntry`, `findUnifiedAllowEntry`, `findUnifiedBlockEntry`, `findEntriesByPhoneNumber`, `getAllBlockPatterns`, `deleteCard`, `deleteAllDismissed` |
| `ui/RecentCallsViewModel.kt` | `blockNumber`, `whitelistNumber` |
| `ui/components/` | `ShieldStatusGlow`, `SourceIcon`, `NeonButton` |
| `ui/theme/Effects.kt` | `glassPanelSmall` |
| `ui/notifications/PulseTriggerLimiter.kt` | `clearAll` |
| `ui/viewmodels/ContactsViewModel.kt` | `blockContact` |
| `data/security/PrecedenceEngine.kt` | `evaluateIncomingCall` (class itself is only instantiated in `AppModule.kt:179`) |
| `logic/ReliableSourceManager.kt` | `getSourceInfo` |
| `logic/DataSyncEngine.kt` | `parseCsvFile` |
Not flagged by the scan but verified separately by grep: `DashboardViewModel.toggleSourceEnabled/syncSource/syncAllSources` have no callers on a Dashboard instance (names collide with `SourcesViewModel`).
**TODO/FIXME/@Deprecated scan:** not run (UNKNOWN).

## D. Discrepancies (doc/comment vs code; stated factually)
| # | Document or comment says | Code / tooling shows | Evidence |
|---|---|---|---|
| D1 | `Source-Of-Truth.md`: 108 Kotlin files (83 main / 17 unit / 8 instrumented) | 124 (85 / 29 / 10) | `Source-Of-Truth.md` summary table vs `find` |
| D2 | `pulse-instrumented-tests.yml:13`: `pulse-ci.yml`'s unit-test step "still has continue-on-error" | no `continue-on-error` in any workflow file | `grep -n continue-on-error .github/workflows/*.yml` |
| D3 | `Architecture-Contract.md` 10.2: TelemetryViewModel "Registered in Koin, zero screen call sites… re-confirmed by direct grep" | no `TelemetryViewModel` class or active production binding exists; not in `AppModule` view-model list. Remaining `.kt` hits are stale comments (`ui/RecentCallsViewModel.kt:30`, `ui/screens/SettingsViewModel.kt:27`); the other hits are in `.md` files | `grep -rn TelemetryViewModel` |
| D4 | `DataSourceRepository.kt:450,458` (KDoc and exception message) name "MANUAL, CONTACTS, and federal (FTC/FCC)" sources | persisted source types are MANUAL, FTC, FCC; no CONTACTS type; Contacts is type MANUAL, pathOrUrl `contacts` | `DatabaseEntities.kt:15-18`; `DatabaseInitializer.kt:34-37` |
| D5 | Two enums named `SourceType` | `database.entities.SourceType {MANUAL,FTC,FCC}` and `data.models.SourceType {REMOTE_URL,LOCAL_CSV}` | `DatabaseEntities.kt:15`, `data/models/ThreatSource.kt:4` |
| D6 | `NotificationChannelManager`: `sync_status` description "Background sync progress for federal blocklist sources"; header says reserved/nothing posts | description text still user-visible on channel | `NotificationChannelManager.kt` |
| D7 | `PrecedenceEngine` described as Tier-1/2/4 precedence engine (KDoc) | instantiated with empty sets; no other reference found | `AppModule.kt:179-183`; grep |
| D8 | `LEFT JOIN sources` comment says entries with missing source "still appear with NULL priority" | `WHERE s.isEnabled = 1` excludes NULL-source rows | `DatabaseDAOs.kt:~160-171` |
| D9 | `applicationId` `com.signalgate.multipoint[.pulse]` | namespace/package `com.signalgate.pulse` | `app/build.gradle` |
| D10 | `Architecture-Contract.md` INV-010 / 10.x list reference a 14-day/250 policy as open | no such rule in production code | TRUTH-CF-2 §D |
| D11 | `ProtectedSourceDeletionException` KDoc says MANUAL/CONTACTS "may never be deleted (only their…" | repository deletes guard only by type; behavior of toggling those rows is unrestricted at repository level | `DataSourceRepository.kt:147-156` |
| D12 | `README.md` project-structure block lists `java/com/signalgate/multipoint/` and `CallScreeningService.kt` | source dir is `java/com/signalgate/pulse/`; the class file is `SignalGateCallScreeningService.kt` (applicationId is `com.signalgate.multipoint[.pulse]`, package is `com.signalgate.pulse`) | `README.md:53-54,71`; `ls android/app/src/main/java/com/signalgate/` |
| D13 | Ledger tail: a follow-up note about the dropped "Add Source" rationale is followed by a bare `55` line | no label or context for the value (was `5` at baseline) | `PROJECT_LEDGER.md` lines ~1311-1314 |

## E. Decisions already written in repo docs (quoted with location) and UNRESOLVED
**Written (CLAIMED-IN-DOC):** Contract header says v4 content adopted 2026-08-25 as canonical (`PROJECT_LEDGER.md` header). `PROTECTED_SOURCE_TYPES` rationale at `DataSourceRepository.kt:~80-92`. SECURITY_FAILURE "rings through today… a deliberate, visible policy choice" (`SignalGateCallScreeningService.kt:~229-234`). Startup `runBlocking` described as intentional (`app/build.gradle` comment, `MainApplication.kt` class doc).
**Open in `Architecture-Contract.md` §10:** 10.2 (TelemetryViewModel), 10.3, 10.4, 10.6, 10.12 ("carried from v3, not re-checked"), 10.13 (AppModule OSI-layer comments), 10.14 (Contacts enablement path, "audit open"), 10.15 (14-day/250-entry retention, "audit open").
**UNRESOLVED (no repo doc decides):** whether a source-less or disabled federal row should show a defined state beyond "Not synced yet"; what "fail-closed" means when SECURITY_FAILURE rings through; whether the pre-clock dependency-resolution segment should be bounded; who owns Contacts enable/disable (the wizard skip does not toggle the row); whether the Manual User Rules row may be disabled at repository level.

## F. Open-item register BP-01..BP-16
The repo defines **no** `BP-xx` identifiers (`grep -rn 'BP-[0-9]'` finds none in code, contract, or ledger apart from conversation-derived text). Therefore each ID's definition is UNKNOWN as repo-defined; the table records only current evidence for the item as phrased in the working conversation.
| ID | Item | Evidence in this zip |
|---|---|---|
| BP-01 | Contacts enablement path | `ContactsViewModel.skipContactImport()` sets `_saveError=null; _isSaved=true` only (`:151-154`); Sources screen shows a switch for the Contacts row (`SourcesScreen.kt:146`) |
| BP-02 | 14-day/250 retention | not found in `src/main` |
| BP-03 | Community/GitHub blocklist, `CommunitySyncWorker` name | worker syncs FTC/FCC only; FTC data comes from a GitHub-hosted mirror (`ReliableSourceManager.kt:103-104`); no "community" source in `SOURCES` |
| BP-04 | initializeDatabase comment rationale | KDoc exists above `suspend fun initializeDatabase` (`AppModule.kt:~280-300`); the lower lines read discuss seeding and Bloom rehydration, not call-screening Koin resolution; upper KDoc lines not read |
| BP-05 | conflict-path test with two manager instances | `ReliableSourceManagerEnsureFederalRowsTest` exists; two-instance case: NAME-ONLY unknown |
| BP-06 | UI test for Contacts switch | NO TEST FOUND |
| BP-07 | Dead Dashboard sync/toggle | confirmed no callers (§C) |
| BP-08 | Release build on PRs | `build-release` runs only on push to `consumer-v1` (`pulse-ci.yml:115`) |
| BP-09 | One definition of "federal" | UI: `type == "FTC" \|\| "FCC"` (`SourcesScreen.kt:101`); use case: type and name vs `SOURCES` (`ReliableSourceManager.kt:238`) |
| BP-10 | Sync results only logged | `summaryLine` is referenced only in `SourceSyncUseCase.kt:30` (definition), `DashboardViewModel.kt:208` and `SourcesViewModel.kt:100`, both inside `Timber...i(...)` calls; no UI reference found |
| BP-11 | Duplicate bulk-sync code in VMs | `DashboardViewModel.syncAllSources:203` and `SourcesViewModel` both present |
| BP-12 | "Why Add Source was removed" note | `grep -n 'Add Source\|inert'` in `SourcesViewModel.kt` returns nothing |
| BP-13 | `ensureFederalRows()` inside startup `runBlocking` | `AppModule.kt:~306-307` within `initializeDatabase`, called at `MainApplication.kt:111` |
| BP-14 | SyncBootReceiver exported=true | **Verified (code):** manifest `exported="true"` with comment "required for BOOT_COMPLETED on Android 12+" (`AndroidManifest.xml:121-124`); receiver accepts only BOOT_COMPLETED and MY_PACKAGE_REPLACED and calls `CommunitySyncWorker.schedule(context)` (`SyncBootReceiver.kt:27-33`); KDoc says KEEP policy prevents duplicate periodic work (CLAIMED-IN-CODE). **Still uncertain:** whether the manifest comment is correct as a platform matter. My understanding, not verified here, is that system broadcasts reach manifest receivers regardless of `exported`, which would make `true` unnecessary; check Android docs or test with `exported="false"` before changing. **Not runtime-verified:** boot/update delivery and WorkManager execution. |
| BP-15 | Doc drift / sprawl | 11 markdown files at root (§G); D1-D13 above |
| BP-16 | `app/build/` not ignored | `.gitignore:11` contains `/build` (anchored to the repo root); no `build` pattern covers `android/app/build/` |

## G. Docs inventory (line counts from `wc -l`; "claims" = first heading only; deeper claims not extracted)
| File | Lines | First heading |
|---|---|---|
| `Architecture-Contract.md` | 189 | (none at line start; INV list at 134-143) |
| `PROJECT_LEDGER.md` | 1328 | header: "single authoritative log of project state"; lists a document map |
| `Source-Of-Truth.md` | 205 | "Source-of-Truth Branch Audit" (stale counts, D1) |
| `README.md` | 239 | "Signal-Gate Pulse — Consumer v1 Overview" (rewritten since baseline; self-described as not an authoritative spec) |
| `MANUS-HANDOFF.md` | 97 | "Manus Handoff — Pulse Pre-Release Security Assurance" |
| `CLAUDE-DEVSECOPS-BUILD-PLAN.md` | 230 | "Claude DevSecOps Build Plan" |
| `SECURITY-DEVOPS-BUILD-PLAN.md` | 582 | "SignalGate Pulse" |
| `SIGNALGATE-PULSE-NEXT-ARCHITECTURAL-BUILD-PLAN.md` | 431 | (none) |
| `SignalGate-Pulse-Manus-CI-Guardrails.md` | 346 | (none) |
| `SignalGate-Pulse-Release-Roadmap.md` | 131 | "Production-Readiness & Forward Roadmap" |
| `error_corrections.md` | 20 | "Gemini Error Corrections & Metrics Log" |
| `tools/metrics-analysis/README.md` | 270 | "Jetpack Compose Metrics Analysis Guide" |
Which doc supersedes which: the ledger header says it keeps a document map (`PROJECT_LEDGER.md`, lines 5+); not extracted here.

## H. Could not verify
Resolved dependencies for androidTest, ksp, other variants and CI (pulseDebug runtime and unit-test classpaths are now covered, see TRUTH-CF-1 §2); any test or build result (no tests were executed; the dependency run only resolved); branch protection (zip-only: not visible; an external reviewer reported it disabled, see the Branch protection line in TRUTH-CF-1; not independently checked); contents of 8 of the 11 root docs beyond headings; `artifacts/crash-diagnostic-log-trace-2026-08-17.md` (present, not read). `build_log_full.txt` begins with the text "error connecting to api.github.com / check your internet connection" (first lines read), so it does not appear to hold a build log; TODO/FIXME scan; runtime behavior (including actual boot/update delivery and WorkManager execution for `SyncBootReceiver`); setting defaults.
