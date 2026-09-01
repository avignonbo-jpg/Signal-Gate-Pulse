# SignalGate Pulse
## Security & DevOps Build Plan

> **Status:** Active build authority  
> **Date:** 2026-08-14  
> **Revision:** 2026-08-20  
> **Branch:** `consumer-v1`  
> **Governing contract:** `Architecture-Contract.md` v3 — *Security Integrity Gate* (adopted)

## Purpose and Governance

This is the practical working extraction of §11 of the adopted `Architecture-Contract.md`. It preserves the contract’s phase numbering while making the plan easier to execute. The contract remains authoritative for invariant-level requirements; if this file and §11 visibly disagree, this file must be updated under the contract’s §13 governance rule.

## Executive Direction

SignalGate Pulse is not to be advanced by feature count alone. The release objective is a security system whose guarantees survive interaction among authoritative persistence, derived indexes, external synchronization, domain decisions, Android ingress, notifications, and release infrastructure.

| Order | Focus |
|---:|---|
| 1 | Security control-plane integrity |
| 2 | Decision integrity |
| 3 | Gray-zone foundation |
| 4 | Source reliability |
| 5 | UI and product completion |
| 6 | Mandatory CI and security gates |
| 7 | Release hardening |

Broad UI and gray-zone work must not resume until Phase 0 has exited. As of 2026-08-18, Phase 0 implementation and regression sources passed the mandatory JVM and instrumented workflows on commit `956fc88`; the generated Room schema version 3 artifact is committed and the formal closure is recorded in the signed ledger entry.

## Status at a Glance

| Phase | Area | Status |
|---|---|---|
| 0 | Security control-plane integrity | **Complete** |
| 1 | Decision engine integrity | **Complete** |
| 2 | Gray-zone product completion | **Closed** |
| 3 | Data source reliability | **Complete** |
| 4 | Architecture and product completion | **Gated on 4.0** |
| 5 | Mandatory security CI | **Partially open** |
| 6 | Release hardening | **Open** |
| 7 | Release candidate gate | **Not yet met** |


## Phase 0 — Security Control-Plane Integrity Gate

> **Objective:** Prove that security state cannot diverge between authoritative persistence, derived indexes, external source data, and edge behavior.

Phase 0 — Security Control-Plane Integrity Gate Objective: prove that security state cannot diverge between authoritative persistence, derived indexes, external source data, and edge behavior.


### 0.1 — Establish one authoritative security-rule mutation boundary



Establish one authoritative security-rule mutation boundary — **✅ COMPLETE**, CI-verified 2026-08-13 What this means in practice: every piece of code that can change what a future call-screening decision returns — manual allow, manual block, contact-derived rules, imported source rules, source snapshot replacement, rule removal, derived-index invalidation/rebuild — must go through exactly one class. No feature repository gets to create its own second UnifiedEntryDao mutation path with its own synchronization responsibilities, because that's exactly how a manual block and the Bloom filter's view of the world silently drift apart. What was actually built: SecurityRuleRepository (Layer 5, logic/ package). It wraps DataSourceRepository.insertEntry() — the class that already pairs the DB write with the Bloom-index insert — for addManualBlock(), addManualAllow(), removeRule(), getAllUserRules(). BlocklistRepository (the old direct-DAO-writer) is now a thin 4-method facade delegating to SecurityRuleRepository, kept only so existing ViewModel callers didn't need to change in the same commit. Still open within this item: manual mutation and source-snapshot replacement now share the SecurityRuleRepository boundary, but mandatory behavioral evidence remains tracked under 0.2, 0.4, and 0.8. 0.1 itself remains the manual-rule boundary gate. Verified how: Koin's KoinModuleTest.koinGraphResolvesWithoutError passed with SecurityRuleRepository confirmed registered in the resolved dependency graph; check-architecture-drift.sh reported clean; lint reported 0 errors.


### 0.2 — Make the database authoritative



Make the database authoritative — **✅ COMPLETE**, CI-verified 2026-08-18 Formalize the rule: Database = security truth. Bloom/index/cache = derived acceleration. The live DataSourceRepository documents and implements this separation: an unready Bloom filter falls through to Room, and only the authoritative DAO result determines the decision. BloomAuthoritativeDecisionTest.kt now contains cold, warm, post-mutation, replacement-proxy, rebuild, reset, and pattern-prefix comparisons against a Bloom-disabled repository. The mandatory instrumented suite passed on 2026-08-18; the required cold/warm, mutation, replacement, rebuild, reset, and authoritative-comparison vectors are covered by the passing test class. Required tests (source and mandatory execution evidence complete): cold Bloom (process just started, filter not rehydrated) warm Bloom (normal steady-state) manual mutation after warm Bloom (does a fresh addManualBlock() show up immediately?) source replacement after warm Bloom Bloom rebuild database reset followed by rebuild optimized decision equals authoritative decision, for the same underlying state, in every one of the above conditions


### 0.3 — Define source lifecycle semantics



Define source lifecycle semantics — **✅ COMPLETE**, CI-verified 2026-08-18 Explicitly define allowed operations for MANUAL, CONTACTS, FTC, FCC, and any future user-created source. The live DataSourceRepository protects MANUAL, FTC, and FCC source types from deletion; MANUAL covers both seeded Manual User Rules and Contacts Allow List semantics, while federal sources remain disableable. DataSourceRepositoryDeletionTest.kt proves protected refusal and the non-protected deletion path. The protected-source and non-protected deletion tests passed in the mandatory JVM workflow on 2026-08-18.


### 0.4 — Implement last-known-good source activation



Implement last-known-good source activation — **✅ COMPLETE**, CI-verified 2026-08-18 Minimum approved scope: SourceEntity now has nullable lastAttemptedSync and lastAcceptedSnapshot fields; Room version 2→3 migration adds them without backfill; SecurityRuleRepository records attempts outside the transaction and replaces a source’s entries plus accepted timestamp atomically. Any failure rolls back the candidate replacement and preserves the prior active set. Bloom rebuild occurs only after commit, with safe Room-read fallback if rebuild cannot complete. ReliableSourceManager now routes federal snapshots through this boundary, and SourceSyncUseCase replaces the fabricated HEALTHY status paths in SourcesViewModel and DashboardViewModel. SourceActivationTransactionTest.kt and MigrationTest’s 2→3 case provide regression sources. The migration and rollback tests passed in the mandatory instrumented workflow, and the CI-generated version-3 Room schema artifact is committed at android/app/schemas/com.signalgate.pulse.database.SignalGateDatabase/3.json. No Phase 3 snapshot hash/version/count fields or state enum were added.


### 0.5 — Treat parser/resource limits as security failures



Treat parser/resource limits as security failures — **✅ COMPLETE**, CI-verified 2026-08-18 Record, byte, field, shared-string, and parsing limits must be hard boundaries. The live SecureCsvParser now throws CsvResourceLimitExceededException when a valid-row limit is exceeded, and DataSyncEngine now propagates CSV, XLSX row, and XLSX shared-string limit failures instead of returning partial results. SecureCsvParserLimitTest.kt covers the CSV hard-failure path, and DataSyncEngineXlsxLimitTest.kt covers bounded XLSX row-limit and shared-string-limit failures. The CSV and both bounded XLSX hard-failure tests passed in the mandatory JVM workflow on 2026-08-18.


### 0.6 — Establish explicit security failure semantics



Establish explicit security failure semantics — **✅ COMPLETE**, CI-verified 2026-08-18 Add a typed decision state representing failure of the decision/security subsystem. Define the Android CallResponse policy separately from the domain decision. Required invariant: exception ≠ ALLOW, and security failure ≠ CLEAN_UNKNOWN. The live branch now has the sixth CallTier/ScreeningAction state, explicit service-side CallResponse mapping, and a focused CallScreeningEngineSecurityFailureTest.kt proving the outer engine exception path returns SECURITY_FAILURE. The engine constructs the domain-level ScreeningAction directly and does not depend on the Android SignalGateCallScreeningService type. The focused security-failure test passed in the mandatory JVM workflow on 2026-08-18. The Android policy is documented in SignalGateCallScreeningService: SECURITY_FAILURE currently rings through, as a deliberate policy distinct from the domain failure state.


### 0.7 — Move edge actions inward



Move edge actions inward — **✅ COMPLETE**, CI-verified 2026-08-13 CallActionReceiver must validate the intent, then invoke an application service/repository operation. It must not inject PendingCardDao or any other feature DAO directly. What was actually built: CallActionReceiver now depends on PendingCardRepository (which already existed — it just wasn't being used here before) and SecurityRuleRepository (the new Layer 5 boundary from 0.1) instead of the direct PendingCardDao injection it had before. Its same-file internal handleAction delegate now preserves the ingress boundary while making validation and repository routing testable without a new service layer. Verified structurally via the same CI evidence as 0.1 — Koin graph resolution, drift check; behavioral sources are now present under 0.8.


### 0.8 — Add regression tests for all Phase 0 invariants



Add regression tests for all Phase 0 invariants — **✅ COMPLETE**, CI-verified 2026-08-18 Phase 0 only exits once tests exist and pass in mandatory CI — not when the code merely compiles and looks right on read-through. The live branch contains Phase 0.2 Bloom-authority coverage, Phase 0.3 protected-source deletion coverage, Phase 0.4 migration/rollback coverage, Phase 0.5 CSV and bounded XLSX hard-failure coverage, Phase 0.6 explicit-failure coverage, focused SecurityRuleRepository mutation-boundary coverage, and four CallActionReceiver edge-action behavior tests. The complete required matrix passed in the mandatory JVM and instrumented workflows on 2026-08-18; generated schema version 3 is committed. Phase 0 exit criteria (all eight required before Phase 1 starts) 
- [x] One approved rule mutation boundary exists for manual and source-snapshot mutations; mutation-boundary behavior passed in mandatory CI 
- [x] Direct feature-level DAO mutation from Layer 1/6 is removed for the audited edge paths; source sync status writes are now confined to the accepted-snapshot boundary 
- [x] Database is documented and tested as authoritative (BloomAuthoritativeDecisionTest passed in mandatory CI) 
- [x] Bloom state is derived/rebuildable (BloomAuthoritativeDecisionTest passed in mandatory CI) 
- [x] Source activation is transactional and migration-tested (MigrationTest and SourceActivationTransactionTest passed in mandatory CI) 
- [x] Last-known-good preservation is covered and passed in mandatory CI (SourceActivationTransactionTest) 
- [x] Parser partial-result paths reject hard-limit violations and CSV/XLSX regression tests passed in mandatory CI 
- [x] Security failure is explicit and the regression test passed in mandatory CI 
- [x] Mandatory JVM and instrumented CI workflows executed the complete expanded Phase 0 behavioral matrix successfully


## Phase 1 — Decision Engine Integrity

Phase 1 — Decision Engine Integrity


### 1.1 — Six-tier deterministic decision matrix



Six-tier deterministic decision matrix — **✅ COMPLETE**, CI-verified 2026-08-19 Test independently: ALLOWLISTED, FEDERAL_BLOCK, HEURISTIC_BLOCK, HEURISTIC_FLAG, CLEAN_UNKNOWN, and SECURITY_FAILURE. CallScreeningEngineDecisionMatrixTest, CallScreeningEngineSecurityFailureTest, and the mandatory JVM workflow provide deterministic coverage for all six outcomes, with normalization and authoritative repository behavior covered by the matrix suite.


### 1.2 — Make decision consequences explicit



Make decision consequences explicit — **✅ COMPLETE**, CI-verified 2026-08-19 Prefer an immutable ScreeningDecision-equivalent result that carries the action and required consequences. The edge layer must execute the decision instead of reverse-engineering consequences from a single tier value — this is exactly the class of bug that caused the still-open gray-zone gap in 1.3 below. At minimum the decision contract must distinguish: call action, audit requirement, review-card requirement, notification policy, haptic policy, and security failure. ScreeningDecision is immutable; SignalGateCallScreeningService executes its fields, and ScreeningServiceEdgeExecutionTest passed 22/22 in mandatory JVM CI run 32202859358. The same commit passed mandatory instrumented CI run 32202859350.


### 1.3 — Fix the gray-zone contract



Fix the gray-zone contract — **✅ COMPLETE**, CI-verified 2026-08-19 HEURISTIC_FLAG now produces the persisted review state promised by the domain contract. GrayZoneReviewabilityTest verifies the complete path: decision → audit record → PendingCardEntity → repository → PendingCardViewModel → DigestScreen. The test invokes the service persistence seam, asserts the audit and card records, and verifies the rendered digest card and review count in the mandatory instrumented workflow. Phase 1 exit criteria 
- [x] All six tiers have deterministic tests (CallScreeningEngineDecisionMatrixTest, CallScreeningEngineSecurityFailureTest) 
- [x] Bloom and database decisions match, across all states tested in 0.2 (BloomAuthoritativeDecisionTest) 
- [x] Gray-zone reviewability is end-to-end verified (GrayZoneReviewabilityTest) 
- [x] Decision consequences are explicit (ScreeningDecision, ScreeningDecisionConsequencesTest, ScreeningServiceEdgeExecutionTest) 
- [x] No edge-layer code invents domain semantics (contract-driven edge execution and mandatory architecture/test gates)


## Phase 2 — Gray-Zone Product Completion

Phase 2 — Gray-Zone Product Completion — **✅ CLOSED**, CI-verified 2026-08-19 Only after Phase 1 passes. All four Phase 2 items are complete and signed in PROJECT_LEDGER.md; the mandatory Consumer, instrumented, and Compose Metrics workflows passed for the final Phase 2.3/2.4 commit.


### 2.1 — Notification and haptics



Notification and haptics — **✅ COMPLETE**, CI-verified 2026-08-19 PulseHapticsController, PulseVibration, and Koin wiring are present and execute only the explicit haptic policy after persisted consequences. The normal VIBRATE manifest permission is declared. Mandatory Consumer, instrumented, and metrics workflows passed in runs 32244130376, 32244130346, and 32244130337.


### 2.2 — Rate limiting



Rate limiting — **✅ COMPLETE**, CI-verified 2026-08-19 PulseTriggerLimiter suppresses repeated UX dispatch only. The invariant test proves that a suppressed second HEURISTIC_FLAG notification leaves both call-log audit records and both required review cards persisted. Mandatory Consumer, instrumented, and metrics workflows passed in runs 32248702045, 32248701979, and 32248701987.


### 2.3 — Notification actions



Notification actions — **✅ COMPLETE**, CI-verified 2026-08-19 The existing CallActionReceiver action is validated at the ingress seam and routes allowlisting through SecurityRuleRepository and digest dismissal through PendingCardRepository; no direct DAO or receiver-owned feature persistence exists. CallActionReceiverBehaviorTest covers invalid-action rejection and supported action routing.


### 2.4 — Privacy



Privacy — **✅ COMPLETE**, CI-verified 2026-08-19 Blocked-call and review notifications now use private visibility, contain no raw phone number in rendered content, and provide a redacted public version for lock-screen and mirrored surfaces. Operational logs in the screening and limiter paths no longer emit raw phone numbers. NotificationPrivacyTest verifies private visibility and redacted public versions.


## Phase 3 — Data Source Reliability

Phase 3 — Data Source Reliability


### 3.1 — Parser/validator separation



Parser/validator separation — **✅ COMPLETE**, CI-verified 2026-08-19 SecureCsvParser owns bounded raw extraction; SourceRecordValidator owns canonicalization and phone-field validation; downstream application code owns candidate construction and SecurityRuleRepository owns atomic activation. Consumer CI 32256017818, Instrumented CI 32256017902, and Compose Metrics CI 32256017967 passed on commit 035d570.


### 3.2 — Snapshot sanity checks



Snapshot sanity checks — **✅ COMPLETE**, CI-verified 2026-08-19 SnapshotSanityValidator now validates content type, UTF-8 encoding, maximum bytes, maximum records, maximum field length, duplicate accounting, expected count ranges, freshness, catastrophic count changes, and malformed-record ratio before snapshot activation. Consumer CI 32257006586, Instrumented CI 32257006994, and Compose Metrics CI 32257006268 passed on commit 94818f8.


### 3.3 — Authenticity



Authenticity — **✅ COMPLETE**, CI-verified 2026-08-19 The first-party FTC mirror publishes dnc-numbers.json.manifest.json containing the SHA-256 payload hash and detached P-256 ECDSA signature. Pulse verifies the manifest hash and signature before activation. Live manifest validation returned Verified OK against the embedded Pulse trust anchor. Consumer CI 32287636576, Instrumented CI 32287636596, and Compose Metrics CI 32287636574 passed for the app-side verifier. Mirror workflow 32302233508 successfully published the signed manifest.


### 3.4 — Source lifecycle



Source lifecycle — **✅ COMPLETE**, CI-verified 2026-08-19 Added explicit states ENABLED, SYNCING, HEALTHY, STALE, FAILED, REJECTED, and DISABLED. SourceEntity now tracks last attempted sync, last accepted snapshot, snapshot version, snapshot hash, accepted record count, and lifecycle failure/rejection reason. Schema version 4 and MIGRATION_3_4 are committed. SecurityRuleRepository remains the atomic mutation boundary; empty candidates and failed replacements preserve the last-known-good entry set. Consumer CI 32306315653, Instrumented CI 32306315748, and Compose Metrics CI 32306315731 passed on commit c3c5b59; generated schema 4 was recovered from Consumer CI 32307077748 and committed with the cleanup commit.


### 3.5 — The SourcesViewModel fake-sync problem



The SourcesViewModel fake-sync problem — **✅ COMPLETE** Resolved in Phase 0.4 foundation: SourcesViewModel and DashboardViewModel route sync requests through SourceSyncUseCase and ReliableSourceManager, so they no longer fabricate HEALTHY from the existing entry count. The Phase 3.4 state machine now persists accepted/rejected/failed lifecycle outcomes, while the UI observes the persisted state and safe metadata. Phase 3 exit criteria — **✅ COMPLETE**, CI-verified 2026-08-19 
- [x] A bad source cannot replace a good source — atomic replacement and failed-candidate tests preserve the last-known-good entries. 
- [x] A partial source cannot become active — snapshot sanity checks and empty-candidate rejection prevent activation. 
- [x] Sync status means accepted dataset state, not network success — SYNCING is recorded before fetch, HEALTHY only on committed activation, and failures become STALE/FAILED/REJECTED. 
- [x] Source state is observable without exposing PII — SourcesScreen renders lifecycle state, timestamps, counts, and bounded reasons without phone-number payloads.


## Phase 4 — Architecture and Product Completion

> **Gate condition:** Complete **4.0** before resuming **4.1–4.7** as the primary focus.

Phase 4 — Architecture and Product Completion 4.0 Edge Execution & Control-Plane Hardening — NEW GATE, opened 2026-08-20, from a full-scope security/architecture review (persistence, decision path, security boundary, sync/parsing, Android edge behavior, DI, onboarding, manifests, ProGuard, CI, schemas, test suite) Why this sits ahead of 4.1: Phase 0/1 proved security state cannot diverge and decisions are explicit. This review found the same invariant class violated at a layer those phases didn't reach — the Android CallScreeningService edge boundary itself, plus two real correctness gaps in the Bloom/DB and source-identity model underneath it. Per this document's own stated build order (control-plane integrity before product completion), 4.0 gates ahead of 4.1-4.7's UI/product work, the same way Phase 0 gated ahead of everything else. Do not resume 4.1-4.7 work concurrently with 4.0.2/4.0.3 below.


### 4.0.1 — CallScreeningService response guarantee and deadline architecture



CallScreeningService response guarantee and deadline architecture — OPEN, highest priority in this phase Two related defects in SignalGateCallScreeningService.onScreenCall(): Problem A — silent non-response: details.handle?.schemeSpecificPart ?: return can exit the function without ever calling respondToCall(). Android's CallScreeningService contract requires a response within 5 seconds; if none arrives, the framework unbinds and the call proceeds as if allowed. A null/malformed handle currently produces exactly the implicit-ALLOW failure mode Phase 0.6 was built to eliminate, just from a different entry point that Phase 0.6's test coverage doesn't reach. Problem B — unstructured concurrency: CoroutineScope(Dispatchers.Default).launch { ... } creates a new unmanaged scope per call with no structured cancellation, no lifecycle relationship to the service, no concurrency limit, and no deadline enforcement. Decision, response, DB persistence, notification, and haptic dispatch all currently run inside that same coroutine, so a slow persistence write can push the response itself past the platform's 5-second deadline. Required shape: Code The response must not be able to block on persistence, notification, or haptic work succeeding or failing. A null/invalid handle must produce an explicit, audited response (SECURITY_FAILURE or an explicit safe-default), never a silent return. Exit test (required, not optional): a JVM/instrumented test proving the screening response is still produced when persistence blocks or throws, and a second test proving a null/invalid handle produces an explicit audited response rather than a silent return. See 4.9.A/4.9.B/4.9.C below for the full set.


### 4.0.2 — SourceType policy enum, source-identity bug, and Sources-screen/Contacts wiring audit



SourceType policy enum, source-identity bug, and Sources-screen/Contacts wiring audit — CORRECTED 2026-08-25, partially RESOLVED, partially retracted Correction: this item originally conflated two separate things in DataSourceRepository.kt. Verified directly against live source before writing this correction, not assumed. RESOLVED, confirmed against live code: the actual deletion-protection mechanism is PROTECTED_SOURCE_TYPES = setOf("MANUAL", "FTC", "FCC") — a string-based set membership check in deleteSource(), which throws ProtectedSourceDeletionException for any protected type before the DAO cascade can run. This is real, robust, and not priority-based. The original framing of this item ("priority == 100 is used as an unstable identity discriminator for deletion protection") was wrong — that was never what protects sources from deletion. Retracted as a security finding, reclassified as a low-priority hygiene note: isManualSource(sourceId), which does use priority == 100, is a decision-labeling performance shortcut on the hot screening path (avoids an extra DAO call), not a security or deletion-protection mechanism. Its own doc comment already documents a safe fallback: an unresolved source is conservatively labeled "aggregated," and the engine still blocks — it just doesn't apply FEDERAL_BLOCK-tier treatment. A misclassification here affects a UI/audit label, not whether a call gets blocked. A formal SourceType enum would still be a reasonable hygiene improvement here someday, but this is not an open security gap and should not be tracked with 4.0.1-level urgency. Live-testing finding this connects to (owner-reported, 2026-08-20) — unaffected by this correction, still open: on-device, the FTC DNC source shows Healthy/1797 entries and Sync Now visibly works. FCC Consumer Complaints shows a sync spinner that runs, then resolves to Never/0 entries/Unknown status. Both manual-type sources show the same Never/0/Unknown symptom when Sync Now is tapped, despite being MANUAL-type sources that should never go through a network sync path at all. This is still the strongest available signal that Contacts import may not actually be wired into DataSourceRepository/the sync-status system — investigate before treating Phase 4.3 (Contacts boundary) as cosmetic. This finding was never about SourceType/priority and stands independent of the correction above. Where a deletion guard was added this cycle: DataSourceRepository.deleteSource() (not SourceDao — Room codegen has no policy awareness, so the guard can only live at the repository call-site today). This closes the primary attack surface for an accidental protected-source deletion via the intended path, but does not close it for any future code that calls SourceDao.deleteSource() directly, bypassing the repository. The SourceType enum above is what turns this from a convention into a compiler-checked fact; a DB-level CHECK constraint is optional defense-in-depth on top of it, not a substitute for it.


### 4.0.3 — Bloom/database transactional decoupling



Bloom/database transactional decoupling — OPEN SecurityRuleRepository.replaceSourceSnapshot() performs a Room transaction (delete old rows, insert new rows) and mutates the Bloom filter as part of the same insert call. The Room transaction can roll back; the Bloom filter cannot. A failed transaction can therefore leave Bloom bits referencing records that were never actually committed — the derived index is not transactionally coupled to the authoritative database it's supposed to derive from. Required: make the ordering explicit and enforced, not just documented: Code Split DataSourceRepository.insertEntries() (which currently mutates both DB and Bloom in one call) into insertEntriesAuthoritative() (DB only) and rebuildDerivedIndexes() (Bloom only, called strictly post-commit). This is the same "Bloom is derived, DB is truth" principle Phase 0.2 already established for the read path; this closes the equivalent gap on the write path.


### 4.0.4 — Disabled-source sync semantics



Disabled-source sync semantics — OPEN CommunitySyncWorker calls reliableSourceManager.syncAllFederalSources(), which appears to sync hardcoded federal sources regardless of their current isEnabled state. This is not an active decision-correctness bug today — the authoritative DAO queries already filter s.isEnabled = 1, so a disabled source's entries can't affect a decision — but it violates the source lifecycle model's own stated semantics and wastes bandwidth/battery/network quota on data that's then discarded. syncSource() should explicitly skip disabled sources unless the call is a deliberate manual refresh.


### 4.0.5 — EULA/onboarding persistence reliability



EULA/onboarding persistence reliability — supersedes 4.2 below; keep both entries, this one is the concrete finding The existing 4.2 item ("move EULA acceptance behind ViewModel/application boundaries") undersold the actual defect. Current implementation calls context.getSharedPreferences(...) directly inside OnboardingWizardScreen, and prefs.edit().apply() is asynchronous — the screen navigates away immediately after calling apply(), before persistence is confirmed. This means onboarding can advance past the EULA screen without the acceptance actually being durably written yet. Required shape: Code Store agreement_id, agreement_version, accepted_at as a coherent record, not three unrelated preference keys. Exit test: write fails -> onboarding does not advance (see 4.9.G).


### 4.0.6 — SecurityRuleRepository scope review



SecurityRuleRepository scope review — OPEN, evaluate before implementing SecurityRuleRepository now owns manual mutation, snapshot activation, sync attempt/failure state, DAO access, normalization, transaction orchestration, and Bloom rebuild coordination — it has become the project's de facto security super-object. Before adding more responsibility to it (as 4.0.2's SourceType work and 4.0.3's Bloom split both would), evaluate whether it should split into narrower boundaries, e.g. SecurityRuleMutationRepository / SnapshotActivationService / SourceLifecycleRepository. This is a design decision, not a mechanical fix — do not implement a split without confirming the resulting boundaries still preserve INV-001 (one authoritative mutation boundary); a split done carelessly could recreate the exact multi-writer problem Phase 0.1 closed. 4.0 exit criteria (all required before 4.1-4.7 resume as the primary focus) 
- [ ] CallScreeningService guarantees exactly one response per invocation, including null/invalid-handle and persistence-failure paths, with tests 
- [ ] Response is decoupled from persistence/notification/haptic completion (hard deadline preserved under slow-persistence test) 
- [ ] SourceType enum exists and priority==100 is no longer used as source identity anywhere 
- [ ] FCC source sync failure root-caused (bad URL vs. silent fetch failure) and either fixed or explicitly documented as not-yet-implemented 
- [ ] Contacts import confirmed wired to DataSourceRepository/sync-status, or confirmed broken and tracked as its own fix 
- [ ] Bloom mutation is provably post-commit-only (insertEntriesAuthoritative/rebuildDerivedIndexes split, with a rollback test)


### 4.0.7 — Pre-Release Screening Assurance Gate

The existing CallScreeningService, source lifecycle, persistence, privacy, and release behaviors form Pulse's signature capability and are release-blocking assurance work, not optional post-v1 feature scope. Before broad product expansion or v1.0 sign-off, verify the screening path under cold process startup, malformed or null Telecom handles, decision-engine exceptions, slow or failed persistence, UX failures, source unavailability/staleness, Bloom rebuild and failed-transaction conditions, concurrent calls, and process death after response but before consequence persistence. The required order is: validate ingress → decide → emit exactly one Telecom response → persist required consequences → dispatch optional UX.

The gate must include pure table-driven response-policy evidence, Android framework/instrumentation evidence, adversarial parser/source/signature/rollback coverage, privacy review of logs/notifications/diagnostics, and representative real-device Telecom cold-start and release/minified validation. New screening actions or detection intelligence are outside this gate and require a separate contract-reviewed v1.1 capability plan.

Exit criteria:
- [ ] Every screening invocation produces exactly one explicit response under null/invalid input and unexpected exceptions.
- [ ] Response timing is measured against a documented internal budget below the platform ceiling.
- [ ] Response emission is independent of persistence, notification, and haptic completion.
- [ ] Source and derived-index failure paths preserve authoritative and last-known-good state.
- [ ] No reviewed log, notification, diagnostic artifact, or audit surface leaks unnecessary call metadata.
- [ ] Required JVM/instrumented CI and representative real-device evidence are attached to the ledger.


### 4.1 — Orphans and unreachable UI



Orphans and unreachable UI Resolve PermissionSettingsScreen and TelemetryViewModel: wire them to justified owners or remove them. Update from the 2026-08-14 reachability audit: this item is bigger than previously scoped. Three fully orphaned data classes were found with zero references anywhere in the codebase outside their own file: BenchmarkResult, PermissionStatus, ThreatSource. PermissionStatus is very likely the other half of the already-known PermissionSettingsScreen problem — same abandoned feature, two orphaned pieces, not two unrelated ones. Resolve them together: either both get wired into a real permissions flow, or both get deleted in the same commit. Also from that same audit — the swipe-right drawer: MainActivity wraps the app in a Compose ModalNavigationDrawer, which responds to edge-swipe by default. NavGraph.kt declares an onOpenDrawer callback that is never actually wired to any screen — meaning there's currently no intentional way to open the drawer (no 3-dot icon calls it), but it's fully reachable via the undocumented swipe gesture regardless. The drawer itself (GlassmorphicDrawerContent.kt) still has "MULTI-PORT" as a literal visible text label, and SettingsScreens.kt has a fully functional RGB shield-color-slider section — a Multi-Port-flavor feature, not part of the Pulse consumer design. Owner's plan (confirmed 2026-08-13): keep the drawer as an intentional, low-visibility "set and forget" menu — invisible until swipe or a (to-be-wired) 3-dot tap — but redesign its contents for Pulse (drop the RGB picker, rebrand the header, remove other Multi-Port-flavor calls). That redesign itself belongs to the project owner, not this build plan — but wiring onOpenDrawer to an actual trigger, once the redesign is ready, is a real Phase 4 task.


### 4.2 — Onboarding persistence



Onboarding persistence Move EULA acceptance and other onboarding persistence behind ViewModel/application boundaries. Treat consent/version state as auditable application state, not arbitrary Compose state.


### 4.3 — Contacts boundary



Contacts boundary Move ContactsContract/ContentResolver access behind a repository/data-source boundary (ContactsRepository). ContactsViewModel currently owns this directly.


### 4.4 — UI quality



UI quality Fix ShieldStatusGlow's Color.hashCode()-for-Paint-color bug (should use .toArgb()) and other verified correctness issues. Continue Compose/design-system work only against stable application contracts — i.e., after Phase 0/1 are done, not concurrently with them.


### 4.5 — Legacy resources



Legacy resources Remove unreferenced XML/layout/view resources only after the complete target files have been read and any Android/runtime dependency has been ruled out. The historical PhoneStateReceiver incident (a deliberate no-op "landmine-defusal" file that got deleted based on grep evidence alone, without reading its own header comment explaining why it existed) is the standing reason reference-search evidence alone is never sufficient for a deletion decision in this codebase.


### 4.6 — Cold-start UX



Cold-start UX — **⚠ IN PROGRESS**. Splash infrastructure landed 2026-08-14; a real-device icon failure was found and interim-mitigated 2026-08-15, but the item itself is still open — user is actively working on it, deprioritized beneath Phase 0 Not originally scoped in this plan, but real product-quality work started out of sequence because it was small, safe, and high-value: added the AndroidX SplashScreen API (core-splashscreen) so the ~5-second wait during MainApplication.onCreate()'s intentional blocking DB init shows a branded starting window instead of a blank screen. The blocking init itself was not touched — it's a deliberate, documented safety property (a cold process can be woken directly by CallScreeningService with no Activity involved, and screening must never run against a half-seeded database). Added StartupTimingTest.kt as a lightweight instrumented regression guard. 2026-08-15 update: the initially-shipped splash icon (shield_logo.png) turned out to be badly oversized for the platform icon slot (1412×1704px hero-card asset vs. the ~192dp/288dp-safe-zone the SplashScreen API expects) and caused an 8-second blank black screen on real-device testing — the exact failure this feature was built to fix, just from a different cause. Neither CI's emulator-based crash-diagnostic step nor StartupTimingTest.kt caught it, since the failure was a silent render fallback, not a crash. Interim mitigation applied: icon removed, windowSplashScreenBackground kept alone — this stops the observed blank-screen symptom and is itself a fully standard, supported configuration, but it is not the intended final branded state. This item is open, not closed — see PROJECT_LEDGER.md, 2026-08-15 entry. Per explicit user direction the same day, this is correctly deprioritized beneath Phase 0's core security control-plane work — that's the right call under Security-First, this note exists just so a future read of this plan doesn't mistake "infrastructure exists" for "finished."


### 4.7 — Package identity cleanup



Package identity cleanup — ✅ DONE, 2026-08-14, ahead of this phase Also not originally scoped, also completed early: renamed the Kotlin source package from com.signalgate.multipoint to com.signalgate.pulse across all 79 files, plus the AGP namespace, the Room schema-export directory, and the drift-check script's scan path (this last one mattered most — a stale path there would have made the script silently scan nothing and always report clean, a dangerous false-negative). applicationId was deliberately left unchanged — that's the actual Play Store/device package identity, a separate and more consequential decision than the source package name, still pending explicit confirmation. 4.8 Performance and reliability hardening — NEW, from the 2026-08-20 review


### 4.8.1 — Pattern-matching hot path



Pattern-matching hot path — **✅ COMPLETE**, CI-verified 2026-08-31. On a Bloom positive-prefix result, `UnifiedEntryDao.findMatchingBlockPatternsWithPriority(normalized)` now filters `:normalized LIKE ue.phoneNumber || '%'` in SQLite and preserves enabled-source and priority ordering. Kotlin no longer materializes every pattern. Consumer CI run `33452532389` passed all 84 JVM tests, lint, architecture checks, and artifact uploads.


### 4.8.2 — DataSyncEngine is not actually streaming



DataSyncEngine is not actually streaming — **PARTIALLY COMPLETE / OPEN**. The CSV path now has `streamCsvFile(..., onBatch)` and emits/discards bounded batches, but the compatibility list-returning API remains and XLSX still uses a two-pass parser that returns a list. Full closure requires suspend-aware XLSX batch transport and repository-backed activation semantics.


### 4.8.3 — Chunked-but-not-batched insert



Chunked-but-not-batched insert — **PARTIALLY COMPLETE / OPEN**. The new CSV parser exposes bounded batches, but a complete repository-backed batch activation path is not yet wired. Do not close this item until each batch’s database transaction, post-commit derived-index rebuild, and whole-candidate failure semantics are proven together.


### 4.8.4 — XLSX shared-string limit needs a byte budget, not just a count



XLSX shared-string limit needs a byte budget, not just a count — **✅ COMPLETE**, CI-verified 2026-08-31. Added `maxExpandedSharedStringBytes` (64 MiB default) and `maxCellLength` (64 KiB default), counted as UTF-8 bytes during SAX accumulation, with typed hard failures and regression coverage. Consumer CI run `33453014941` passed all 84 JVM tests, lint, architecture checks, and artifact uploads. 4.9 Failure-choreography test coverage — NEW, from the 2026-08-20 review. Roughly 25 test files already exist with strong coverage of the security control-plane; this set specifically targets failure paths under load/latency/malformed-input that the existing suite doesn't yet exercise. None of these are optional relative to 4.0/4.8 above — they are how 4.0/4.8 get to be more than a read-through claim.


### 4.9.A — CallScreeningService deadline test



CallScreeningService deadline test — decision path is artificially slowed; response still happens within the platform deadline. Covers 4.0.1.


### 4.9.B — Null-handle test



Null-handle test — details.handle == null; asserts an explicit safe response is produced, is audited, and the function never silently returns. Covers 4.0.1.


### 4.9.C — Service exception test



Service exception test — an unexpected exception during screening produces SECURITY_FAILURE, never BLOCK and never a silent non-response. Extends the existing Phase 0.6 engine-level test to the actual service entry point.


### 4.9.D — Snapshot failure + Bloom contamination test



Snapshot failure + Bloom contamination test — **✅ COMPLETE**, instrumented CI-verified 2026-08-31. `SourceActivationTransactionTest` asserts that a failed candidate snapshot preserves the prior authoritative decision and that the candidate-only number remains non-blocking. Pulse Instrumented Tests run `33451970789` passed successfully. Covers 4.0.3.


### 4.9.E — Disabled-source sync test



Disabled-source sync test — a disabled source is skipped by the sync worker's automatic path (not a manual refresh); decision output is unaffected either way. Covers 4.0.4.


### 4.9.F — Bounded-batch streaming test



Bounded-batch streaming test — **✅ COMPLETE for the CSV path**, Consumer CI-verified 2026-08-31. `DataSyncEngineXlsxLimitTest.csvParser_emitsBoundedBatches` proves three CSV records are delivered as `[2, 1]` batches, without requiring 2M rows. Full 4.8.2 closure remains open until XLSX receives equivalent batch transport. Covers the CSV portion of 4.8.2.


### 4.9.G — EULA persistence-failure test



EULA persistence-failure test — persistence write fails; onboarding does not advance past the EULA screen. Covers 4.0.5.


## Phase 5 — Mandatory Security CI

Phase 5 — Mandatory Security CI


### 5.1 — Test gating



Test gating Remove continue-on-error: true from required tests in pulse-ci.yml. A required failure must fail CI. Reconciled 2026-08-17: no active continue-on-error key exists in either pulse-ci.yml or pulse-instrumented-tests.yml; both the JVM unit-test and instrumented-test steps are hard-failing. Branch-protection enforcement is a separate repository-policy decision and is not implied by workflow step behavior.


### 5.2 — Instrumented security tests



Instrumented security tests Mandatory CI must cover: Android Keystore, SQLCipher open/close, Keystore invalidation, database deletion/reset, Room migration, fresh-install schema, screening service behavior, five/six-tier decision path, security failure behavior.


### 5.3 — Static/architecture checks



Static/architecture checks Run architecture drift checks as required gates — done, 2026-08-13. Expand the script to enforce the contract's edge-to-DAO and UI-persistence restrictions more completely (the contract's §9 lists 10 target rules; the script currently enforces rules 1–7, not 8–10 — those remaining rules describe target enforcement for violations that are already fixed in source, so the gap is about catching a regression, not an active hole today).


### 5.3.1 — Structural weakness in the drift script



Structural weakness in the drift script — NEW, from the 2026-08-20 review. The script is grep-based and mostly detects import ... statements rather than actual call-site relationships, so it can't enforce the strongest version of "one authoritative mutation boundary" — a future contributor could introduce database.unifiedEntryDao().insertEntries(...) through a path the current rules don't cover, and the script would report clean. Tighten it to scan specifically for the mutation method names (insertEntry, insertEntries, deleteEntry, deleteEntriesBySourceId, updateEntry) and flag any occurrence outside an explicit file allowlist (currently: SecurityRuleRepository, DataSourceRepository). This is closer to the actual invariant than the current import-based check.


### 5.3.2 — Ledger-enforcement CI gate



Ledger-enforcement CI gate — NEW. To let an automated agent (e.g. Manus) work against this repo without silent drift, add a CI check that fails the build when a commit touches production .kt files but PROJECT_LEDGER.md's Session Log entry count doesn't increase in the same PR. Crude, but it turns "forgot to log the session" from a norm someone can skip under time pressure into a hard CI failure — directly answers "how do I stop drift/fibbing/silent-failure-to-log."


### 5.4 — Dependency and secret scanning



Dependency and secret scanning Add vulnerability scanning with explicit severity policy and exception ownership. Add secret scanning for repository and build artifacts. Neither exists yet. Concretely: add Dependabot or OSV-Scanner as a scheduled + PR-triggered workflow with a defined severity threshold that fails CI (not just reports), plus a documented exception-ownership process for any accepted risk rather than a silent allowlist.


### 5.5 — GitHub Actions hardening



GitHub Actions hardening Use least-privilege permissions. Pin third-party actions to immutable commit SHAs where practical. Do not permit workflow conveniences to weaken release security.


### 5.5.1 — Concrete gap



Concrete gap, 2026-08-20 review: pulse-ci.yml, crash-diagnostic.yml, and generate-room-schema.yml still reference actions/checkout@v4, actions/setup-java@v4, and reactivecircus/android-emulator-runner@v2 by tag, not commit SHA. metrics.yml already declares permissions: contents: read at the workflow level — the same minimal-permissions block is missing from the other three workflows and should be added everywhere a workflow doesn't need write access.


### 5.6 — Known gap found 2026-08-14



Known gap found 2026-08-14 — missing script scripts/verify-launch-and-capture.sh is referenced by .github/workflows/crash-diagnostic.yml but does not actually exist in the current consumer-v1 checkout, despite existing in an earlier archive of this project. That workflow is very likely broken right now, independent of anything else in this plan. Needs its own investigation — either restore the script or fix the workflow.


## Phase 6 — Release Hardening

Phase 6 — Release Hardening


### 6.1 — R8



R8 — ⚠ PACKAGE PATH CONFIRMED CORRECT as a side effect of the 2026-08-14/15 rename merge; substance still open Replace broad -keep class com.signalgate.pulse.** { ; }-style rules with narrow, justified rules. Remove stale legacy rules. Validate the minified release build on device/emulator. Confirmed 2026-08-15: the rename merge's post-merge cleanup caught and fixed every stray com.signalgate.multipoint reference in proguard-rules.pro — it would have silently failed to protect any pulse-package class from R8 stripping in a release build, and debug CI never runs ProGuard, so this specific breakage wouldn't have surfaced as a build failure on its own. proguard-rules.pro now correctly reads com.signalgate.pulse.* throughout, with a guard comment warning against reverting it. That fix was mechanical (package path only) and does not address this item's actual substance, which remains fully open: the blanket -keep class com.signalgate.pulse.** { ; } still defeats most of R8's value for the app's own package, and both stale class-name keeps are still present unchanged — com.signalgate.pulse.CallScreeningService (the real class is SignalGateCallScreeningService) and com.signalgate.pulse.ui.SettingsFragment (no Compose-era equivalent exists). Confirmed again, 2026-08-20 review: both stale rules are still present verbatim, plus an unrelated extends androidx.fragment.app.Fragment reference with no Compose-era equivalent. The blanket -keep class com.signalgate.pulse.* { *; } makes essentially all narrower rules redundant right now — it's safe-ish but defeats most of R8's purpose. Clean this as one focused pass: remove the two stale class-name keeps, remove the dead Fragment reference, and narrow the blanket keep to the specific entry points that actually need protection (manifest-declared components, Room entities/DAOs if reflection-accessed, Parcelable implementations).


### 6.5 — Manifest permission audit



Manifest permission audit — NEW, from the 2026-08-20 review Pulse's manifest currently requests: READ_PHONE_STATE, READ_PHONE_NUMBERS, ANSWER_PHONE_CALLS, READ_CALL_LOG, WRITE_CALL_LOG, READ_CONTACTS, POST_NOTIFICATIONS. Android's current CallScreeningService guidance is that apps filtering calls should rely on the service's own supplied call details rather than declaring READ_PHONE_STATE; some of the above may be justified by other product functionality (call-history/contacts), but none should be carried forward merely because an earlier architecture used them. Audit each permission against: exact class using it -> exact feature requiring it -> runtime necessity -> Play policy justification. Do not resolve this from documentation alone; cross-reference against the still-open "Confirm READ_PHONE_STATE necessity on a real device" item (Open Items, this document's companion ledger) — that item and this audit are the same underlying question and should close together, not separately.


### 6.2 — Signing



Signing CI release signing must fail closed when credentials are absent. Release keys must remain outside source control and developer logs.


### 6.3 — SBOM/provenance



SBOM/provenance Generate an SBOM, artifact checksums, and provenance tied to source commit/workflow. Preserve these with the release candidate.


### 6.4 — Release validation



Release validation Run: release build, R8 validation, instrumented tests, launch verification, manifest/exported-component review, backup exclusion review, privacy/logging review.


## Phase 7 — Release Candidate Gate

> A release candidate may be promoted only when every gate in this section is satisfied.

Phase 7 — Release Candidate Gate A release candidate may be promoted only when all of the following are true: 
- [ ] Security invariants INV-001 through INV-010 have evidence 
- [x] Security rule mutation is singular (manual path — tested for DI/compile correctness only, not yet behaviorally tested, see 0.8) 
- [ ] Bloom is non-authoritative and safe under cold/warm/reset states (true by design, not yet proven by test) 
- [ ] Source datasets are atomic and last-known-good 
- [ ] Partial/truncated source data is rejected 
- [ ] Screening failure is explicit and tested 
- [ ] All five (six) tiers are tested 
- [ ] Gray-zone review is end-to-end functional 
- [ ] Keystore invalidation and DB reset are instrumented-tested 
- [x] Required CI tests cannot silently fail (true for the architecture-drift gate; not yet true for unit tests — see 5.1) 
- [ ] Dependency/secret scans are green or explicitly excepted 
- [ ] R8 release build is validated 
- [ ] SBOM/checksum/provenance/signing artifacts exist 
- [ ] Manifest/exported-component/privacy reviews are complete 
- [ ] CallScreeningService guarantees exactly one response per invocation under null-handle and slow-persistence conditions, with passing tests (4.0.1 / 4.9.A-C) 
- [ ] Bloom mutation is provably post-commit-only, with a rollback/contamination test passing (4.0.3 / 4.9.D) 
- [ ] SourceType is the enforced source-identity discriminator; priority is not used for identity anywhere (4.0.2) 
- [ ] Manifest permissions are individually justified against actual runtime use (6.5)


## 

Non-goals / deliberate constraints Do not reintroduce Apache POI solely to simplify parsing. The native ZipInputStream + SAX approach correctly avoids POI's MethodHandle/D8-dexing incompatibility below API 26. Do not use TLS pinning as a substitute for source-artifact authenticity (see 3.3). Do not make Bloom filters authoritative. Do not let notification/rate-limiting code decide whether a call is blocked. Do not let UI or Platform/Edge code become an alternate persistence path. Do not make a failed security operation indistinguishable from a legitimate allow decision. Do not revert MainApplication's blocking DB init in the name of startup UX — cover the wait (done, 4.6), don't remove the guarantee it provides.


## 

Final engineering principle The release bar is not "the app builds." The release bar is "the security properties remain true when the system is under failure, stale data, cold start, concurrent mutation, malformed input, dependency change, and release optimization." This document is a working extraction of the adopted Architecture-Contract.md §11, kept current as a separate reference file. If the two ever visibly disagree, the contract's §11 is the source of truth for the invariant-level requirements — update this file to match, under the same governance rule §13 of the contract for reconciling any other documentation/reality mismatch.


## Non-Goals and Deliberate Constraints

Do not reintroduce Apache POI solely to simplify parsing; the native `ZipInputStream` plus SAX approach avoids POI’s MethodHandle/D8-dexing incompatibility below API 26. TLS pinning is not a substitute for source-artifact authenticity. Bloom filters must remain derived and non-authoritative. Notification and rate-limiting code must not decide whether a call is blocked. UI and platform/edge code must not become alternate persistence paths. A failed security operation must never be indistinguishable from a legitimate allow decision. Do not remove `MainApplication`’s blocking database initialization merely to improve startup UX; cover the wait without removing its safety guarantee.

## Final Engineering Principle

> The release bar is not “the app builds.” The release bar is “the security properties remain true when the system is under failure, stale data, cold start, concurrent mutation, malformed input, dependency change, and release optimization.”
