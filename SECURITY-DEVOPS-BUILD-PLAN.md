SignalGate Pulse — Security & DevOps Build Plan
Status: Active build authority
Date: 2026-08-14 (extracted and updated from the adopted Architecture-Contract.md §11)
Branch: consumer-v1
Governing contract: Architecture-Contract.md (v3 — Security Integrity Gate, adopted)
This document exists for one reason: the phase list inside the contract is correct but dense — a 430-line governance document isn't what you want open while you're actually doing the work. This is the same plan, same phase numbering, but written to be worked from directly, with status markers kept current as of each session.
Executive direction
SignalGate Pulse is not to be advanced by feature count alone. The release objective is a security system whose guarantees survive component interaction: authoritative persistence, derived indexes, external data synchronization, domain decisions, Android ingress, notifications, and release infrastructure must all preserve the same security invariants.
The build order is therefore:
Security control-plane integrity → decision integrity → gray-zone foundation → source reliability → UI/product completion → mandatory CI/security gates → release hardening.
The previous roadmap's assumption that gray-zone UI could proceed once PendingCardEntity existed is replaced by a stronger gate: the application must first prove that security state and derived decision state cannot diverge.
Do not resume broad UI/gray-zone feature work until Phase 0 exits. As of 2026-08-16, 0.1 and 0.7 are CI-verified complete; 0.2 and 0.3 have live implementations and regression-test sources but still require mandatory execution evidence; 0.4, 0.5 and 0.6 have implementation and regression-test sources, including dedicated XLSX row/shared-string hard-failure coverage for 0.5, but local execution is blocked by the sandbox's missing Android SDK; 0.8 remains open for complete mandatory coverage. Phase 0's own exit criteria (§ below) are the actual gate — not a vibe check.
Phase 0 — Security Control-Plane Integrity Gate
Objective: prove that security state cannot diverge between authoritative persistence, derived indexes, external source data, and edge behavior.
0.1 Establish one authoritative security-rule mutation boundary — ✅ COMPLETE, CI-verified 2026-08-13
What this means in practice: every piece of code that can change what a future call-screening decision returns — manual allow, manual block, contact-derived rules, imported source rules, source snapshot replacement, rule removal, derived-index invalidation/rebuild — must go through exactly one class. No feature repository gets to create its own second UnifiedEntryDao mutation path with its own synchronization responsibilities, because that's exactly how a manual block and the Bloom filter's view of the world silently drift apart.
What was actually built: SecurityRuleRepository (Layer 5, logic/ package). It wraps DataSourceRepository.insertEntry() — the class that already pairs the DB write with the Bloom-index insert — for addManualBlock(), addManualAllow(), removeRule(), getAllUserRules(). BlocklistRepository (the old direct-DAO-writer) is now a thin 4-method facade delegating to SecurityRuleRepository, kept only so existing ViewModel callers didn't need to change in the same commit.
Still open within this item: manual mutation and source-snapshot replacement now share the SecurityRuleRepository boundary, but mandatory behavioral evidence remains tracked under 0.2, 0.4, and 0.8. 0.1 itself remains the manual-rule boundary gate.
Verified how: Koin's KoinModuleTest.koinGraphResolvesWithoutError passed with SecurityRuleRepository confirmed registered in the resolved dependency graph; check-architecture-drift.sh reported clean; lint reported 0 errors.
0.2 Make the database authoritative — ◐ IMPLEMENTED; mandatory execution evidence pending
Formalize the rule: Database = security truth. Bloom/index/cache = derived acceleration.
The live DataSourceRepository documents and implements this separation: an unready Bloom filter falls through to Room, and only the authoritative DAO result determines the decision. BloomAuthoritativeDecisionTest.kt now contains cold, warm, post-mutation, replacement-proxy, rebuild, reset, and pattern-prefix comparisons against a Bloom-disabled repository. This item is not closed until the instrumented suite passes in mandatory CI.
Required tests (source present; mandatory execution still pending):
cold Bloom (process just started, filter not rehydrated)
warm Bloom (normal steady-state)
manual mutation after warm Bloom (does a fresh addManualBlock() show up immediately?)
source replacement after warm Bloom
Bloom rebuild
database reset followed by rebuild
optimized decision equals authoritative decision, for the same underlying state, in every one of the above conditions
0.3 Define source lifecycle semantics — ◐ IMPLEMENTED; mandatory execution evidence pending
Explicitly define allowed operations for MANUAL, CONTACTS, FTC, FCC, and any future user-created source. The live DataSourceRepository protects MANUAL, FTC, and FCC source types from deletion; MANUAL covers both seeded Manual User Rules and Contacts Allow List semantics, while federal sources remain disableable. DataSourceRepositoryDeletionTest.kt proves protected refusal and the non-protected deletion path. This item is not closed until the test passes in mandatory CI.
0.4 Implement last-known-good source activation — ✅ IMPLEMENTATION COMPLETE; migration and behavioral execution pending
Minimum approved scope: SourceEntity now has nullable lastAttemptedSync and lastAcceptedSnapshot fields; Room version 2→3 migration adds them without backfill; SecurityRuleRepository records attempts outside the transaction and replaces a source’s entries plus accepted timestamp atomically. Any failure rolls back the candidate replacement and preserves the prior active set. Bloom rebuild occurs only after commit, with safe Room-read fallback if rebuild cannot complete.
ReliableSourceManager now routes federal snapshots through this boundary, and SourceSyncUseCase replaces the fabricated HEALTHY status paths in SourcesViewModel and DashboardViewModel. SourceActivationTransactionTest.kt and MigrationTest’s 2→3 case provide regression sources. This item remains open until mandatory CI executes the migration and rollback tests and the generated version-3 Room schema artifact is committed. No Phase 3 snapshot hash/version/count fields or state enum were added.
0.5 Treat parser/resource limits as security failures — ✅ IMPLEMENTATION COMPLETE; regression execution pending
Record, byte, field, shared-string, and parsing limits must be hard boundaries. The live SecureCsvParser now throws CsvResourceLimitExceededException when a valid-row limit is exceeded, and DataSyncEngine now propagates CSV, XLSX row, and XLSX shared-string limit failures instead of returning partial results. SecureCsvParserLimitTest.kt covers the CSV hard-failure path, and DataSyncEngineXlsxLimitTest.kt covers bounded XLSX row-limit and shared-string-limit failures. Local execution and mandatory execution evidence remain pending; implementation coverage is now present for all explicitly tracked parser-limit paths.
0.6 Establish explicit security failure semantics — ✅ IMPLEMENTATION COMPLETE; regression execution pending
Add a typed decision state representing failure of the decision/security subsystem. Define the Android CallResponse policy separately from the domain decision.
Required invariant: exception ≠ ALLOW, and security failure ≠ CLEAN_UNKNOWN.
The live branch now has the sixth CallTier/ScreeningAction state, explicit service-side CallResponse mapping, and a focused CallScreeningEngineSecurityFailureTest.kt proving the outer engine exception path returns SECURITY_FAILURE. The engine constructs the domain-level ScreeningAction directly and does not depend on the Android SignalGateCallScreeningService type. Local Gradle execution is currently blocked because this sandbox has no Android SDK; the test remains open for mandatory CI execution before Phase 0 exits. The Android policy is documented in SignalGateCallScreeningService: SECURITY_FAILURE currently rings through, as a deliberate policy distinct from the domain failure state.
0.7 Move edge actions inward — ✅ COMPLETE, CI-verified 2026-08-13
CallActionReceiver must validate the intent, then invoke an application service/repository operation. It must not inject PendingCardDao or any other feature DAO directly.
What was actually built: CallActionReceiver now depends on PendingCardRepository (which already existed — it just wasn't being used here before) and SecurityRuleRepository (the new Layer 5 boundary from 0.1) instead of the direct PendingCardDao injection it had before. Verified via the same CI evidence as 0.1 — Koin graph resolution, drift check.
0.8 Add regression tests for all Phase 0 invariants — ☐ OPEN (partial sources now present)
Phase 0 only actually exits once tests exist and pass in mandatory CI — not when the code merely compiles and looks right on read-through. The live branch now contains Phase 0.2 Bloom-authority coverage, Phase 0.3 protected-source deletion coverage, Phase 0.4 migration/rollback coverage sources, Phase 0.5 CSV and bounded XLSX hard-failure coverage, and a Phase 0.6 explicit-failure regression source. Mandatory execution evidence, generated schema version 3, and any remaining mutation-boundary/edge-action behavioral coverage still need to be confirmed before this gate can close.
Phase 0 exit criteria (all eight required before Phase 1 starts)
[x] One approved rule mutation boundary exists for manual and source-snapshot mutations (mandatory behavioral execution pending)
[x] Direct feature-level DAO mutation from Layer 1/6 is removed for the audited edge paths; source sync status writes are now confined to the accepted-snapshot boundary
[ ] Database is documented and tested as authoritative (test source present; mandatory execution pending)
[ ] Bloom state is derived/rebuildable (implementation and test source present; mandatory execution pending)
[x] Source activation is transactional in implementation (mandatory migration/rollback execution pending)
[x] Last-known-good preservation is covered by a regression source (mandatory execution pending)
[x] Parser partial-result paths now reject hard-limit violations in implementation and have CSV/XLSX regression sources (execution evidence pending)
[x] Security failure is explicit in the domain/service implementation (regression execution pending)
[x] Mandatory CI runs the Phase 0 tests that exist (the drift-check gate itself is now mandatory CI — see Phase 5 note below — but there's nothing yet in CI specifically testing Phase 0's behavioral invariants, since 0.8's tests don't exist)
Phase 1 — Decision Engine Integrity
1.1 Five-tier decision matrix
Test independently: ALLOWLISTED, FEDERAL_BLOCK, HEURISTIC_BLOCK, HEURISTIC_FLAG, CLEAN_UNKNOWN, and SECURITY_FAILURE.
Test combinations: manual allow vs. external block; manual block vs. external allow; exact vs. pattern; source priority; normalization; malformed/empty input; advisory signals; default path; source disablement; source replacement.
1.2 Make decision consequences explicit
Prefer an immutable ScreeningDecision-equivalent result that carries the action and required consequences. The edge layer must execute the decision instead of reverse-engineering consequences from a single tier value — this is exactly the class of bug that caused the still-open gray-zone gap in 1.3 below. At minimum the decision contract must distinguish: call action, audit requirement, review-card requirement, notification policy, haptic policy, and security failure.
1.3 Fix the gray-zone contract
HEURISTIC_FLAG must produce the persisted review state promised by the domain contract. Verify the complete path: decision → audit record → PendingCardEntity → repository → DigestViewModel → DigestScreen. This is the specific, already-diagnosed bug: CallScreeningEngine's own doc comment promises gray-zone review for every HEURISTIC_FLAG call, but the actual write path only creates a PendingCardEntity for HEURISTIC_BLOCK. It's why the held notification/haptic feature has been sitting unmerged.
Phase 1 exit criteria
[ ] All five (soon six) tiers have deterministic tests
[ ] Bloom and database decisions match, across all states tested in 0.2
[ ] Gray-zone reviewability is end-to-end verified
[ ] Decision consequences are explicit
[ ] No edge-layer code invents domain semantics
Phase 2 — Gray-Zone Product Completion
Only after Phase 1 passes.
2.1 Notification and haptics
Reintroduce the held gray-zone notification/haptic implementation (PulseHapticsController, PulseVibration) only after persisted review state is proven in 1.3. It was built once already and shelved specifically because it depends on the 1.3 fix — re-check it against the current CallActionReceiver (rewired in 0.7) before merging, not the version it was originally written against.
2.2 Rate limiting
PulseTriggerLimiter may suppress repeated notifications, but it must never suppress: the domain decision, the call-log audit record, or the existence of a required review card. Rate limiting is a UX throttle, not a security control — it must not be able to accidentally become one.
2.3 Notification actions
All action buttons must route through application/repository boundaries. No receiver may directly own feature persistence — this is the same rule 0.7 already established for CallActionReceiver's existing action; any new actions added here inherit that same requirement.
2.4 Privacy
Define lock-screen and notification-mirroring behavior. Raw phone numbers should not be exposed by default on privacy-sensitive surfaces (INV-007 in the contract).
Phase 3 — Data Source Reliability
3.1 Parser/validator separation
Separate: bounded raw parsing, schema validation, canonicalization, security sanity checks, snapshot activation. Same underlying work as 0.5's BoundedXlsxParser extraction — this phase is where that separation gets fully carried through the rest of the source pipeline, not just the parser entry point.
3.2 Snapshot sanity checks
Validate: content type, encoding, schema/header, maximum bytes, maximum records, maximum field length, duplicate behavior, expected count ranges, freshness, catastrophic count changes, malformed-record ratio.
3.3 Authenticity
Where operationally supported, publish signed source snapshots/manifests. The app verifies signature/hash before activation. HTTPS remains transport security, not the sole artifact-authenticity guarantee — the current FTC-mirror trust model is GitHub-raw-HTTPS-and-nothing-else, which is fine as transport but isn't artifact authenticity.
3.4 Source lifecycle
Use explicit states: ENABLED, SYNCING, HEALTHY, STALE, FAILED, REJECTED, DISABLED. Track at least: last attempted sync, last accepted snapshot, snapshot version, snapshot hash, accepted record count, rejection/failure reason.
3.5 The SourcesViewModel fake-sync problem
Resolved in Phase 0.4 foundation: SourcesViewModel and DashboardViewModel now route sync requests through SourceSyncUseCase and ReliableSourceManager, so they no longer fabricate HEALTHY from the existing entry count. The full Phase 3.4 state machine remains intentionally out of scope; current UI reporting is still limited to logging accepted/failed outcomes.
Phase 3 exit criteria
[ ] A bad source cannot replace a good source
[ ] A partial source cannot become active
[ ] Sync status means accepted dataset state, not network success
[ ] Source state is observable without exposing PII
Phase 4 — Architecture and Product Completion
4.1 Orphans and unreachable UI
Resolve PermissionSettingsScreen and TelemetryViewModel: wire them to justified owners or remove them.
Update from the 2026-08-14 reachability audit: this item is bigger than previously scoped. Three fully orphaned data classes were found with zero references anywhere in the codebase outside their own file: BenchmarkResult, PermissionStatus, ThreatSource. PermissionStatus is very likely the other half of the already-known PermissionSettingsScreen problem — same abandoned feature, two orphaned pieces, not two unrelated ones. Resolve them together: either both get wired into a real permissions flow, or both get deleted in the same commit.
Also from that same audit — the swipe-right drawer: MainActivity wraps the app in a Compose ModalNavigationDrawer, which responds to edge-swipe by default. NavGraph.kt declares an onOpenDrawer callback that is never actually wired to any screen — meaning there's currently no intentional way to open the drawer (no 3-dot icon calls it), but it's fully reachable via the undocumented swipe gesture regardless. The drawer itself (GlassmorphicDrawerContent.kt) still has "MULTI-PORT" as a literal visible text label, and SettingsScreens.kt has a fully functional RGB shield-color-slider section — a Multi-Port-flavor feature, not part of the Pulse consumer design. Owner's plan (confirmed 2026-08-13): keep the drawer as an intentional, low-visibility "set and forget" menu — invisible until swipe or a (to-be-wired) 3-dot tap — but redesign its contents for Pulse (drop the RGB picker, rebrand the header, remove other Multi-Port-flavor calls). That redesign itself belongs to the project owner, not this build plan — but wiring onOpenDrawer to an actual trigger, once the redesign is ready, is a real Phase 4 task.
4.2 Onboarding persistence
Move EULA acceptance and other onboarding persistence behind ViewModel/application boundaries. Treat consent/version state as auditable application state, not arbitrary Compose state.
4.3 Contacts boundary
Move ContactsContract/ContentResolver access behind a repository/data-source boundary (ContactsRepository). ContactsViewModel currently owns this directly.
4.4 UI quality
Fix ShieldStatusGlow's Color.hashCode()-for-Paint-color bug (should use .toArgb()) and other verified correctness issues. Continue Compose/design-system work only against stable application contracts — i.e., after Phase 0/1 are done, not concurrently with them.
4.5 Legacy resources
Remove unreferenced XML/layout/view resources only after the complete target files have been read and any Android/runtime dependency has been ruled out. The historical PhoneStateReceiver incident (a deliberate no-op "landmine-defusal" file that got deleted based on grep evidence alone, without reading its own header comment explaining why it existed) is the standing reason reference-search evidence alone is never sufficient for a deletion decision in this codebase.
4.6 Cold-start UX — ⚠ IN PROGRESS. Splash infrastructure landed 2026-08-14; a real-device icon failure was found and interim-mitigated 2026-08-15, but the item itself is still open — user is actively working on it, deprioritized beneath Phase 0
Not originally scoped in this plan, but real product-quality work started out of sequence because it was small, safe, and high-value: added the AndroidX SplashScreen API (core-splashscreen) so the ~5-second wait during MainApplication.onCreate()'s intentional blocking DB init shows a branded starting window instead of a blank screen. The blocking init itself was not touched — it's a deliberate, documented safety property (a cold process can be woken directly by CallScreeningService with no Activity involved, and screening must never run against a half-seeded database). Added StartupTimingTest.kt as a lightweight instrumented regression guard.
2026-08-15 update: the initially-shipped splash icon (shield_logo.png) turned out to be badly oversized for the platform icon slot (1412×1704px hero-card asset vs. the ~192dp/288dp-safe-zone the SplashScreen API expects) and caused an 8-second blank black screen on real-device testing — the exact failure this feature was built to fix, just from a different cause. Neither CI's emulator-based crash-diagnostic step nor StartupTimingTest.kt caught it, since the failure was a silent render fallback, not a crash. Interim mitigation applied: icon removed, windowSplashScreenBackground kept alone — this stops the observed blank-screen symptom and is itself a fully standard, supported configuration, but it is not the intended final branded state. This item is open, not closed — see PROJECT_LEDGER.md, 2026-08-15 entry. Per explicit user direction the same day, this is correctly deprioritized beneath Phase 0's core security control-plane work — that's the right call under Security-First, this note exists just so a future read of this plan doesn't mistake "infrastructure exists" for "finished."
4.7 Package identity cleanup — ✅ DONE, 2026-08-14, ahead of this phase
Also not originally scoped, also completed early: renamed the Kotlin source package from com.signalgate.multipoint to com.signalgate.pulse across all 79 files, plus the AGP namespace, the Room schema-export directory, and the drift-check script's scan path (this last one mattered most — a stale path there would have made the script silently scan nothing and always report clean, a dangerous false-negative). applicationId was deliberately left unchanged — that's the actual Play Store/device package identity, a separate and more consequential decision than the source package name, still pending explicit confirmation.
Phase 5 — Mandatory Security CI
5.1 Test gating
Remove continue-on-error: true from required tests in pulse-ci.yml. A required failure must fail CI. Currently, unit tests still run with this flag set — the architecture-drift check does not (that gap was closed 2026-08-13, it's now a required, non-advisory step), but unit tests are still advisory.
5.2 Instrumented security tests
Mandatory CI must cover: Android Keystore, SQLCipher open/close, Keystore invalidation, database deletion/reset, Room migration, fresh-install schema, screening service behavior, five/six-tier decision path, security failure behavior.
5.3 Static/architecture checks
Run architecture drift checks as required gates — done, 2026-08-13. Expand the script to enforce the contract's edge-to-DAO and UI-persistence restrictions more completely (the contract's §9 lists 10 target rules; the script currently enforces rules 1–7, not 8–10 — those remaining rules describe target enforcement for violations that are already fixed in source, so the gap is about catching a regression, not an active hole today).
5.4 Dependency and secret scanning
Add vulnerability scanning with explicit severity policy and exception ownership. Add secret scanning for repository and build artifacts. Neither exists yet.
5.5 GitHub Actions hardening
Use least-privilege permissions. Pin third-party actions to immutable commit SHAs where practical. Do not permit workflow conveniences to weaken release security.
5.6 Known gap found 2026-08-14 — missing script
scripts/verify-launch-and-capture.sh is referenced by .github/workflows/crash-diagnostic.yml but does not actually exist in the current consumer-v1 checkout, despite existing in an earlier archive of this project. That workflow is very likely broken right now, independent of anything else in this plan. Needs its own investigation — either restore the script or fix the workflow.
Phase 6 — Release Hardening
6.1 R8 — ⚠ PACKAGE PATH CONFIRMED CORRECT as a side effect of the 2026-08-14/15 rename merge; substance still open
Replace broad -keep class com.signalgate.pulse.** { *; }-style rules with narrow, justified rules. Remove stale legacy rules. Validate the minified release build on device/emulator.
Confirmed 2026-08-15: the rename merge's post-merge cleanup caught and fixed every stray com.signalgate.multipoint reference in proguard-rules.pro — it would have silently failed to protect any pulse-package class from R8 stripping in a release build, and debug CI never runs ProGuard, so this specific breakage wouldn't have surfaced as a build failure on its own. proguard-rules.pro now correctly reads com.signalgate.pulse.** throughout, with a guard comment warning against reverting it. That fix was mechanical (package path only) and does not address this item's actual substance, which remains fully open: the blanket -keep class com.signalgate.pulse.** { *; } still defeats most of R8's value for the app's own package, and both stale class-name keeps are still present unchanged — com.signalgate.pulse.CallScreeningService (the real class is SignalGateCallScreeningService) and com.signalgate.pulse.ui.SettingsFragment (no Compose-era equivalent exists).
6.2 Signing
CI release signing must fail closed when credentials are absent. Release keys must remain outside source control and developer logs.
6.3 SBOM/provenance
Generate an SBOM, artifact checksums, and provenance tied to source commit/workflow. Preserve these with the release candidate.
6.4 Release validation
Run: release build, R8 validation, instrumented tests, launch verification, manifest/exported-component review, backup exclusion review, privacy/logging review.
Phase 7 — Release Candidate Gate
A release candidate may be promoted only when all of the following are true:
[ ] Security invariants INV-001 through INV-010 have evidence
[x] Security rule mutation is singular (manual path — tested for DI/compile correctness only, not yet behaviorally tested, see 0.8)
[ ] Bloom is non-authoritative and safe under cold/warm/reset states (true by design, not yet proven by test)
[ ] Source datasets are atomic and last-known-good
[ ] Partial/truncated source data is rejected
[ ] Screening failure is explicit and tested
[ ] All five (six) tiers are tested
[ ] Gray-zone review is end-to-end functional
[ ] Keystore invalidation and DB reset are instrumented-tested
[x] Required CI tests cannot silently fail (true for the architecture-drift gate; not yet true for unit tests — see 5.1)
[ ] Dependency/secret scans are green or explicitly excepted
[ ] R8 release build is validated
[ ] SBOM/checksum/provenance/signing artifacts exist
[ ] Manifest/exported-component/privacy reviews are complete
Non-goals / deliberate constraints
Do not reintroduce Apache POI solely to simplify parsing. The native ZipInputStream + SAX approach correctly avoids POI's MethodHandle/D8-dexing incompatibility below API 26.
Do not use TLS pinning as a substitute for source-artifact authenticity (see 3.3).
Do not make Bloom filters authoritative.
Do not let notification/rate-limiting code decide whether a call is blocked.
Do not let UI or Platform/Edge code become an alternate persistence path.
Do not make a failed security operation indistinguishable from a legitimate allow decision.
Do not revert MainApplication's blocking DB init in the name of startup UX — cover the wait (done, 4.6), don't remove the guarantee it provides.
Final engineering principle
The release bar is not "the app builds." The release bar is "the security properties remain true when the system is under failure, stale data, cold start, concurrent mutation, malformed input, dependency change, and release optimization."
This document is a working extraction of the adopted Architecture-Contract.md §11, kept current as a separate reference file. If the two ever visibly disagree, the contract's §11 is the source of truth for the invariant-level requirements — update this file to match, under the same governance rule §13 of the contract for reconciling any other documentation/reality mismatch.
