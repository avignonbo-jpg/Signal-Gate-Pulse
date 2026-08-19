SignalGate Pulse Architecture Contract (v3 — Security Integrity Gate)
This document defines the binding target architecture and security-governance contract for SignalGate Pulse. It is the binding contract for all future changes: any new code, refactor, feature, or bug fix must preserve these rules or explicitly revise this contract first.
Revision note (2026-08-13, reconciled): This contract merges two independent v3 lineages produced the same day and reconciles both against the actual current source, per the shared governance principle both lineages independently arrived at: if implementation reality and documentation disagree, do not silently choose one — record the discrepancy, inspect the actual code, and update accordingly.
Lineage A (this session, 2026-08-12–13): added a 3-invariant Security Invariants section, a SecurityRuleRepository mutation-boundary design, and implemented + CI-verified Phase 0.1 against real code (SecurityRuleRepository created, BlocklistRepository collapsed to a facade, CallActionReceiver migrated off direct DAO access — confirmed via Koin graph resolution, lint, and check-architecture-drift.sh).
Lineage B (external governance rewrite, 2026-08-13): reviewed a consumer-v1 snapshot independently and produced a materially better-structured document — 10 invariants instead of 3, a cleaner Phase 0–7 roadmap, a full architecture-enforcement checklist, and a Definition of Done section. Its own ledger states plainly: "CI was not independently rerun during this governance rewrite" and its source snapshot predates Lineage A's Phase 0.1 work — so its §10.7 ("security-rule mutation has multiple paths") and §10.10 ("platform edge directly accesses persistence") are listed as open violations that are actually already resolved and CI-verified in the current branch.
This reconciled contract takes Lineage B's structure and invariant set (materially stronger) and corrects the specific items that Lineage B couldn't have known were already fixed. Everything else in Lineage B is adopted as-is. The reconciled v3 contract is adopted and binding as this Architecture-Contract.md.
1. Scope
SignalGate Pulse is a single-activity Compose application with navigation handled entirely in Compose, not through multiple activities or XML-driven navigation flows. The app already has MainActivity as the single host for the Compose UI tree and NavGraph as the app's navigation entry point. The architecture must remain centered on this structure.
This contract covers UI, dependency injection, persistence, domain logic, security boundaries, and ownership of classes across OSI-style layers.
No .xml layout files may exist under res/layout/ unless actively inflated by Kotlin code (via setContentView, findViewById, or ViewBinding) and explicitly listed here as a grandfathered exception. Any layout XML with zero references in src/main/java for more than one release cycle must be deleted, not retained "in case it's needed." Current grandfathered exceptions: none. (Enforced automatically by scripts/check-architecture-drift.sh, Rule 7.)
No new user-facing feature or UI work may be started while Phase 0 (§11) has open gate items. As of 2026-08-13: Phase 0.1 and 0.7 (the mutation-boundary and edge-DAO items) are closed and CI-verified. Phase 0.2–0.6 remain open.
2. Required target architecture
Single activity: MainActivity is the only UI activity host.
Compose navigation: NavGraph owns route wiring and start-destination selection.
Koin for DI: Koin is the sole dependency injection framework for app wiring.
Room for persistence: SignalGateDatabase is the Room database, with access routed through SecureDatabase.
One ViewModel per screen: every screen must have a dedicated ViewModel, and screen state must not be shared by ad hoc cross-screen ViewModel reuse.
No direct DAO access from UI or Platform/Edge: UI layer and Layer 1 code must depend on ViewModels or application-boundary classes only. ViewModels depend on repositories, not DAOs.
No direct database construction outside the secure path: SecureDatabase is the sole authorized construction path for the encrypted Room database.
One authoritative path for every decision-affecting mutation: see INV-001 and §5.2.
3. Layer ownership
Layer
Owns
Does not own
Layer 7 - UI
Activities, Compose screens, navigation destinations, screen-only state rendering, user interaction handling
DAOs, direct database calls, sync logic, security enforcement, business policy
Layer 6 - Presentation
ViewModels, screen state assembly, UI event translation, permission-step orchestration
Room entities, DAO calls, transport/parsing internals, database construction
Layer 5 - Application
Use-case style orchestration, flow coordination, cross-repository rules, sync execution, the authoritative security-rule mutation boundary (SecurityRuleRepository, implemented)
UI rendering, Android widget composition, direct persistence details
Layer 4 - Domain
Policy engines and decision logic such as call-risk and precedence evaluation, the explicit SECURITY_FAILURE decision state (planned, Phase 0.6)
Android framework classes, UI state, Room annotations, navigation logic
Layer 3 - Persistence
Repositories, DAO interfaces, Room entities, database schema, seeding primitives
Compose UI, screen logic, direct platform permissions, parsing policies, decision-affecting business rules
Layer 2 - Security/Parsing
Sanitization, secure CSV parsing, secure database entry points, integrity-sensitive helpers, Keystore-invalidation handling
UI rendering, route decisions, screen state, business presentation
Layer 1 - Platform/Edge
Activity host, BroadcastReceivers, Service entry points, WorkManager workers, Application bootstrap, notification-channel registration
Screen composition, business rules, repository policy, direct DAO access (closed as of Phase 0.1/0.7), direct feature orchestration
Cross-Cutting
Plain data carriers and pure/stateless utility functions, importable from any layer
Business logic, Android framework dependencies beyond basic types, side effects, persistence, security decisions
4. Class ownership map
[+] = confirmed to exist in source but previously unlisted. [RESOLVED] = a tracked violation that has since been fixed and CI-verified.
Layer 1 - Platform/Edge
MainActivity
MainApplication
SignalGateCallScreeningService
CallActionReceiver — [RESOLVED, Phase 0.1/0.7, CI-verified 2026-08-13] now depends on SecurityRuleRepository/PendingCardRepository only; no direct DAO injection.
PhoneStateReceiver
CommunitySyncWorker
SyncBootReceiver [+] — BOOT_COMPLETED/MY_PACKAGE_REPLACED receiver that re-schedules CommunitySyncWorker
KoinWorkerFactory
AppModule
NotificationChannelManager [+] — single registration point for all notification channels
Layer 2 - Security/Parsing
Root of trust: SecureDatabase and SecurityUtils own the Android Keystore-wrapped SQLCipher passphrase — the one piece of state every other class in this layer, and every layer above it, transitively depends on. KeystoreInvalidatedException and DatabaseResetEvent are part of this same root-of-trust cluster.
SecureDatabase — root of trust
SecurityUtils — root of trust
KeystoreInvalidatedException [+] — root-of-trust recovery path
DatabaseResetEvent [+] — root-of-trust recovery path
SanitizationEngine
SecureCsvParser
DatabaseInitializer
BoundedXlsxParser (target, not yet extracted) — the parsing half of DataSyncEngine, to be split out per Phase 0.5 so bound violations become hard rejections rather than partial successes.
Layer 3 - Persistence
Database: SignalGateDatabase
DAOs: SourceDao, UnifiedEntryDao, CallLogDao, SettingDao, SyncHistoryDao, PendingCardDao [+]
Entities: SourceEntity (extended target: snapshotVersion/snapshotHash/lastAcceptedSnapshot/lastSuccessfulSync/lastAttemptedSync, Phase 3), UnifiedEntryEntity, CallLogEntry, SettingEntry, SyncHistoryEntry, PendingCardEntity [+]
Repositories: CallLogRepository, DataSourceRepository, PendingCardRepository, SettingRepository, SyncHistoryRepository
BlocklistRepository — [DEPRECATED FACADE, Phase 0.1 complete, CI-verified 2026-08-13]. Previously wrote to UnifiedEntryDao directly (§10.7, now resolved). Now a thin 4-method pass-through to SecurityRuleRepository. Kept only so existing ViewModel callers (BlockedNumbersViewModel, ContactsViewModel, PendingCardViewModel) didn't need to change in the same commit — should not exist by the time Phase 0 fully closes.
Layer 4 - Domain
BloomFilterEngine — derived, disposable, rebuildable, non-authoritative index (INV-001). A Bloom-state loss is a performance event; a Bloom/database divergence is a security incident.
PrecedenceEngine
CallRiskEvaluator
CallScreeningEngine
CallInfo (CallTier enum + data class) — currently five-tier: ALLOWLISTED / FEDERAL_BLOCK / HEURISTIC_BLOCK / HEURISTIC_FLAG (gray-zone) / CLEAN_UNKNOWN. Target (Phase 0.6): six-tier, adding SECURITY_FAILURE per INV-003.
Layer 5 - Application
ReliableSourceManager — target scope (Phase 3): full SourceState lifecycle, transactional snapshot replacement.
DataSyncEngine — parsing/validation orchestration; "accept partial result on limit-exceeded" behavior is superseded by INV-002 (Phase 0.5, not yet implemented).
SecurityRuleRepository [IMPLEMENTED, CI-verified 2026-08-13] — the single authoritative entry point for manual security-rule mutation. Implemented surface: addManualBlock(), addManualAllow(), removeRule(), getAllUserRules(). importSourceSnapshot()/replaceSourceRules()/rebuildDerivedIndexes() are Phase 3/0.2 targets, not yet built.
SourceSyncUseCase (target, not yet built) — wraps ReliableSourceManager so SourcesViewModel can no longer report HEALTHY without an actual sync (Phase 3).
Layer 6 - Presentation
DashboardViewModel
BlockedNumbersViewModel
RecentCallsViewModel
PendingCardViewModel
OnboardingViewModel
SourcesViewModel — [VIOLATION, open] syncSource()/syncAllSources() currently update lastSynced/HEALTHY without invoking ReliableSourceManager. Resolution: Phase 3.
SettingsViewModel
ContactsViewModel — directly invokes ContactsContract/ContentResolver/Cursor; P2 hardening item (Phase 4), not P0.
LogcatViewModel
TelemetryViewModel
Layer 7 - UI
MainActivity hosts
NavGraph
Screen
Screens and composables: ConsumerDashboardScreen, CallLogScreen, BlockAllowListScreen, SettingsScreen, LogcatViewerScreen, OnboardingWizardScreen, DigestScreen, SourcesScreen, PermissionSettingsScreen, and all reusable Compose components
Cross-Cutting (no layer ownership)
Plain data carriers and stateless utility functions may be imported by any layer without violating layer-boundary rules, provided they contain no business logic, no Android framework dependencies beyond basic types, and no side effects. If a "utility" class ever gains a dependency on a repository, DAO, or Context, it must be reclassified into the appropriate layer immediately — this is a drift signal, not a convenience.
Current cross-cutting classes: BenchmarkResult, CallLogItem, PermissionStatus, ThreatSource, DateUtils, PhoneNumberUtils, Color, Theme, SignalGateTheme, Effects.
(Enforced automatically by scripts/check-architecture-drift.sh, Rule 6 — blocks Room, DAO, and Context imports in data/models/ and utils/. Still-open gap: Rule 6 does not scan ui/theme/, even though Color/Theme/SignalGateTheme/Effects are claimed cross-cutting above.)
5. ViewModel and ownership rules
Each screen must have exactly one primary ViewModel. A screen may depend on helpers or shared repositories, but screen state ownership must remain singular and explicit. ViewModels depend on repositories or application services, never DAOs directly.
UI code must not persist security state directly. Direct SharedPreferences, Room, DAO, ContentResolver, or security-root access from Compose screens is prohibited unless explicitly grandfathered here. The ViewModel/application boundary owns persistence orchestration.
Platform/Edge components are ingress points, not policy owners. A receiver, service, worker, or activity may validate the shape of an incoming event and translate it into an application command, but it must not perform feature-level DAO mutation or independently reimplement security policy.
A screen that needs new state extends its owning ViewModel or introduces a new screen/ViewModel pair. Cross-screen ViewModel reuse is prohibited unless the shared object is explicitly a domain/application service rather than a screen state owner.
6. Security boundary and trust model
A security boundary is the point beyond which a layer may no longer trust or manipulate raw input, secrets, or privileged state without passing through the stricter boundary defined below.
Layer 1 - Platform/Edge
Accepts Android callbacks, broadcasts, service calls, worker triggers, intents, and deep links. All external values are untrusted. Layer 1 validates the minimum shape required to safely hand the event to an application service. Layer 1 must not directly mutate security policy through DAOs — enforced as of Phase 0.1/0.7.
Layer 2 - Security/Parsing
The first trusted normalization zone. It owns canonicalization, bounded parsing, secure database entry points, Keystore handling, and integrity-sensitive validation. External source material cannot become active policy until it passes this boundary.
Layer 3 - Persistence
Stores validated state and provides transactional persistence primitives. Persistence does not decide whether data is trustworthy or what a call should do, and does not decide whether a given write is decision-affecting and therefore must route through SecurityRuleRepository. The encrypted database is the authoritative store for active security policy.
Layer 4 - Domain
Owns authoritative security decisions: precedence, risk, call tier, and decision semantics. Domain output must be explicit and typed. A security failure is not equivalent to ALLOW, CLEAN_UNKNOWN, or any other trusted result (target: Phase 0.6).
Layer 5 - Application
Owns cross-repository workflows and security-state mutation orchestration. This layer is the approved boundary for manual rule mutations (implemented), source snapshot activation (target), digest/review workflows, and notification throttling. It may call repositories, but it must not bypass domain policy.
Layer 6 - Presentation
Transforms trusted domain/application results into screen state and user events. It must not reinterpret security decisions, bypass application services, or accept raw external input as final truth.
Layer 7 - UI
Renders state and collects user interaction. UI is never the authoritative security decision-maker and must not directly persist security policy.
7. Security invariants
These invariants are binding architectural requirements. A feature, optimization, test, or refactor is not complete if it violates one of them.
INV-001 — Authoritative Security State
The encrypted Room/SQLCipher database is the authoritative source of active security policy. Bloom filters, indexes, caches, derived snapshots, and in-memory state are non-authoritative accelerators. A derived structure may improve performance, but it must never introduce a false negative, stale decision, or policy divergence from the authoritative store.
Every decision-affecting mutation must pass through an approved application/persistence boundary that updates, invalidates, or rebuilds all required derived indexes. No feature repository may create a second security-rule write path.
Status: partially satisfied. Manual block/allow mutation is closed (SecurityRuleRepository, Phase 0.1, CI-verified). Derived-index rebuild/invalidate semantics and source-snapshot mutation are not yet built (Phase 0.2/Phase 3).
INV-002 — Last-Known-Good Security Dataset
External or generated security-source snapshots must not modify active policy until the complete candidate dataset has passed parsing, schema, size, freshness, integrity, and sanity validation. Activation must be transactional. A failed, partial, truncated, stale, malformed, or suspicious snapshot leaves the previously accepted dataset unchanged.
The system must distinguish sync attempted from snapshot accepted. Source health must reflect the accepted active dataset, not merely a successful network request or partial parse. Status: open (Phase 0.4/0.5, Phase 3).
INV-003 — Explicit Security Failure
Failure of the screening engine, persistence path, source synchronization, or security root of trust must produce an explicit failure state. It must never silently become ALLOW, CLEAN_UNKNOWN, or another trusted result.
The Android response to a security failure is a product policy decision, but the failure itself must remain distinguishable and auditable from a legitimate allow decision. Status: open (Phase 0.6).
INV-004 — External Input Is Untrusted
Caller ID, contact-provider data, downloaded source data, broadcast extras, deep-link parameters, notification actions, and other external values are untrusted until validated and canonicalized at the appropriate boundary.
INV-005 — Deterministic Security Decisions
Given the same authoritative security state, normalized input, source configuration, and domain policy, the decision engine must produce the same decision. UI state, notification timing, haptic effects, logging, and network availability must not alter the domain decision.
INV-006 — Decision Side Effects Follow the Decision Contract
Audit records, review cards, notifications, haptics, and rate limiting are consequences of an explicit domain/application decision. Edge code must not infer security semantics from one enum field when the domain contract requires additional consequences. In particular, HEURISTIC_FLAG must remain reviewable according to the domain contract. Status: satisfied for decision, audit, and persisted review-card consequences; notification/haptic dispatch remains governed by Phase 2 product completion.
INV-007 — No Raw PII in Operational Logs
Production logging must not emit raw phone numbers, contact names, or equivalent call-identifying PII. Diagnostics should use non-reversible identifiers or controlled redaction. User-visible notifications must have an explicit lock-screen/privacy policy.
INV-008 — Protected Source Lifecycle
Sources containing user-authoritative or foundational policy must not be casually deleted. MANUAL and CONTACTS semantics must be explicitly defined before deletion is permitted. Removing a source must have an explicit, tested policy for its existing entries and derived indexes. Status: open (Phase 0.3).
INV-009 — Edge-to-Application Boundary
Platform/Edge components must not directly access DAOs for feature-level security mutations. Receivers, services, workers, and activities hand commands inward to application services/repositories. This prevents duplicated policy and makes external ingress auditable. Status: satisfied for CallActionReceiver (Phase 0.1/0.7, CI-verified 2026-08-13).
INV-010 — Release Gates Are Mandatory
A required security or correctness test may not be advisory in CI. Required test failures fail the gate. Exceptions must be explicit, time-bounded, owned, and recorded in the ledger. Status: partially satisfied. check-architecture-drift.sh is now a required, non-advisory CI gate (fixed this session — it previously existed but was invoked by no workflow). Unit tests still run with continue-on-error: true (Phase 5, open).
8. Change control and build integrity
Any change must be checked against this contract before implementation. If a change requires violating a boundary or invariant, the contract must be updated first and the change must explicitly state which rule is being revised and why.
The practical rule remains: UI renders, ViewModels adapt, application code orchestrates and enforces the mutation boundary, domain decides and declares failure explicitly, persistence stores authoritative state, and security/parsing establishes trust.
Single-lineage rule: this is the only Architecture Contract lineage for this project going forward. Any future proposal, session, or tool that produces a competing contract document must reconcile against this one — specifically, against actual current source and CI state, not just against the text of the prior document — before either is treated as authoritative.
Every third-party import used in src/main/java must correspond to a declared dependency in app/build.gradle; no import may rely on an incidental transitive dependency. minSdkVersion, targetSdkVersion, and compileSdkVersion are architectural constraints and require review when changed.
DataSyncEngine.kt uses bounded native ZipInputStream + SAX parsing rather than Apache POI. POI is not to be reintroduced unless a new decision explicitly proves it compatible with the current Android toolchain and security/resource constraints.
Release builds must use minification/resource shrinking and must fail closed when release signing credentials are absent. Broad R8 keep rules that effectively disable optimization/obfuscation are prohibited in the final release configuration. proguard-rules.pro currently violates this (-keep class com.signalgate.multipoint.** { *; } plus two stale class-name keeps) — Phase 6.
CI workflows must use least-privilege permissions, immutable action references where practical, mandatory test gates, dependency/secret scanning, and release artifact provenance appropriate to a security application.
9. Architecture enforcement
scripts/check-architecture-drift.sh is a structural guard, not a substitute for behavioral security tests. It must enforce at least:
canonical layer numbering and ownership;
no direct DAO access from UI/Presentation;
no Room construction outside SecureDatabase;
Layer 2 security imports do not leak into UI;
prohibited dependency direction;
cross-cutting purity checks, including ui/theme/ where those classes are declared cross-cutting (currently unenforced — open gap);
no unreferenced legacy layout XML unless explicitly grandfathered;
no direct DAO access from Platform/Edge feature code (satisfied for the current codebase as of Phase 0.1/0.7 — not yet a standing grep rule in the script itself, see follow-up below);
no direct SharedPreferences/persistence access from Compose UI unless explicitly grandfathered;
approved application boundary for decision-affecting security mutations, to the extent statically enforceable.
Follow-up needed: rules 8–10 above describe target enforcement; the script as currently written enforces rules 1–7 (confirmed by reading it this session) but does not yet have grep rules for 8–10. The underlying violations these rules would have caught (§10.7, §10.10) are already fixed in source, so the script passing today doesn't yet mean it would catch a regression — that gap should close in Phase 5.
Structural checks must be complemented by behavioral tests for security invariants. A green architecture-drift script alone is not a release authorization.
Newly confirmed this session: the script existed but was invoked by zero CI workflows — none of pulse-ci.yml, crash-diagnostic.yml, generate-room-schema.yml, or metrics.yml called it. Fixed: a "Check Architecture Drift" step now runs in pulse-ci.yml before the Gradle build, with no continue-on-error, and has been confirmed passing in a real CI run against the current source.
10. Known violations and required resolution
10.1 PermissionSettingsScreen unreachable
PermissionSettingsScreen exists but is not reachable from Screen.kt/NavGraph.kt. Resolve by wiring it with its intended ViewModel or delete it. Open — Phase 4.
10.2 TelemetryViewModel orphaned
TelemetryViewModel is registered in Koin but is not consumed by a screen. Wire it to its justified owner or remove the class and binding. Open — Phase 4.
10.3 Build comment drift
The app/build.gradle KSP comment describing exportSchema = false conflicts with SignalGateDatabase using exportSchema = true. Open — Phase 4.
10.4 ShieldStatusGlow color conversion
ShieldStatusGlow.kt uses Color.hashCode() for a native Paint color. Use toArgb(). Open — Phase 4.
10.5 Gray-zone persistence contract — [RESOLVED, CI-verified 2026-08-19]
HEURISTIC_FLAG now creates the review state promised by the domain contract through the explicit ScreeningDecision consequence contract. GrayZoneReviewabilityTest verifies decision → audit record → PendingCardEntity → repository → PendingCardViewModel → DigestScreen in mandatory instrumented CI. Notification/haptic product dispatch remains Phase 2 work.
10.6 Architecture script coverage gap
Rule 6 must scan ui/theme/ if those classes remain cross-cutting. Open — Phase 5.
10.7 Security-rule mutation has multiple paths — [RESOLVED, CI-verified 2026-08-13]
BlocklistRepository previously wrote UnifiedEntryDao directly while DataSourceRepository.insertEntry() also acted as a write chokepoint and updated Bloom state — multiple persistence paths with different derived-index responsibilities, violating INV-001. Resolved: SecurityRuleRepository introduced as the approved application/security-rule mutation boundary; BlocklistRepository collapsed to a facade over it. Confirmed via Koin dependency-graph resolution and architecture-drift check in CI.
10.8 Source synchronization is not yet an atomic last-known-good workflow
The source-management path must distinguish download, parse, validate, accepted snapshot, and active dataset. Open — Phase 3.
10.9 Screening exception path is an implicit allow
SignalGateCallScreeningService currently catches screening exceptions and builds an allow response, violating INV-003. Open — Phase 0.6.
10.10 Platform edge directly accesses persistence — [RESOLVED, CI-verified 2026-08-13]
CallActionReceiver previously injected PendingCardDao directly. Resolved: now routes through SecurityRuleRepository/PendingCardRepository. Confirmed via CI (Koin graph resolution, architecture-drift check).
10.11 Source deletion semantics are underspecified
SourceDao foreign-key cascade behavior means deleting a source can remove its associated entries. The product must explicitly define which sources may be deleted before exposing deletion as a general operation. Open — Phase 0.3.
10.12 Release hardening gaps
Required release work includes: removing advisory test gates, adding instrumented security tests to mandatory CI, dependency/CVE scanning, secret scanning, least-privilege workflow permissions, immutable action references where practical, SBOM/provenance generation, and narrowing stale/broad R8 keep rules. Open — Phase 5/6.
11. Security-First Build Plan
The build plan is deliberately reordered. Do not resume broad UI/gray-zone feature work until Phase 0 exits.
Phase 0 — Security Control-Plane Integrity Gate
Objective: prove that security state cannot diverge between authoritative persistence, derived indexes, external source data, and edge behavior.
0.1 — [COMPLETE, CI-verified 2026-08-13] Establish a single approved application/security-rule mutation boundary for manual allow, manual block, contact rules, imported rules, source replacement, and rule removal. (Contact/import/source-replacement routing remains open — only manual allow/block/remove/read are implemented so far.)
0.2 Make the database explicitly authoritative and Bloom filters explicitly derived. Add safe invalidation/rebuild behavior. A cold/empty Bloom state must only reduce performance, never change a decision. Open.
0.3 Define protected source lifecycle semantics. At minimum, decide whether MANUAL, CONTACTS, FTC, FCC, and any future user-defined source may be disabled, refreshed, cleared, or deleted. Open.
0.4 Implement transactional source replacement with last-known-good retention. Do not activate partial datasets. Open.
0.5 Make resource-limit violations hard failures for security-source parsing. A truncated source is not a successful source. Open.
0.6 Establish explicit SECURITY_FAILURE decision semantics and document the Android CallResponse behavior for that state. Open.
0.7 — [COMPLETE, CI-verified 2026-08-13] Route CallActionReceiver through an application boundary and remove direct DAO access from Layer 1.
0.8 Add regression tests for all Phase 0 invariants. Phase exits only when the tests pass in mandatory CI. Open — no test coverage exists yet for SecurityRuleRepository or CallActionReceiver.
Phase 1 — Decision Engine Integrity
1.1 Build a five-tier decision matrix covering ALLOWLISTED, FEDERAL_BLOCK, HEURISTIC_BLOCK, HEURISTIC_FLAG, and CLEAN_UNKNOWN.
1.2 Test manual allow versus external block, manual block versus external allow, source priority, exact versus pattern rules, normalization, empty/invalid input, and default behavior.
1.3 Test Bloom cold start, warm state, post-mutation state, post-source-replacement state, rebuild, and reset. Verify every optimized decision equals the authoritative DB decision.
1.4 Fix the gray-zone persistence contract so every HEURISTIC_FLAG result becomes reviewable exactly as the domain contract specifies.
1.5 Define an immutable decision result containing the security action and required downstream consequences rather than requiring Layer 1 to infer side effects from a tier alone.
Phase 2 — Gray-Zone Product Completion
2.1 Re-verify the Digest surface against real persisted gray-zone cards.
2.2 Reintroduce the held notification/haptic implementation only after Phase 1 passes.
2.3 Keep haptic/notification behavior independent from domain policy. A limiter may suppress a notification but must never alter the call tier or audit record.
2.4 Validate notification privacy, lock-screen behavior, notification actions, and deep-link handling.
2.5 Ensure action buttons invoke application services rather than DAOs.
Phase 3 — Data Source Reliability
3.1 Separate bounded parsing from security validation.
3.2 Validate source schema, content type, encoding, record limits, field limits, duplicate behavior, expected count ranges, freshness, and catastrophic dataset anomalies.
3.3 Add signed snapshot/hash verification for externally sourced security datasets where operationally supported. HTTPS is transport security; artifact authenticity must be separately established.
3.4 Implement source state transitions such as ENABLED, SYNCING, HEALTHY, STALE, FAILED, REJECTED, and DISABLED.
3.5 Make "sync attempted" distinct from "snapshot accepted" and expose the accepted snapshot metadata for diagnostics.
3.6 Ensure failed synchronization leaves the active last-known-good dataset untouched.
Phase 4 — UI / Onboarding / Architecture Completion
4.1 Resolve PermissionSettingsScreen and TelemetryViewModel orphan states.
4.2 Move EULA persistence out of Compose and behind its owning ViewModel/application boundary.
4.3 Move Contacts Provider access behind a repository/data-source boundary; ViewModels should not directly own ContentResolver/ContactsContract operations.
4.4 Complete the Compose design-system/UI work only against the stable application contracts.
4.5 Correct ShieldStatusGlow color conversion and other non-security UI defects.
4.6 Remove or grandfather stale XML resources only after full-file/reference review and live-device verification where applicable.
Phase 5 — Security Test and CI Gate
5.1 Make unit tests mandatory; remove continue-on-error from required tests.
5.2 Add instrumented Keystore, SQLCipher, migration, database-reset, and decision-path tests to mandatory CI.
5.3 Implement the actual SecurityUtilsTest/instrumented coverage rather than retaining a placeholder.
5.4 Add dependency/CVE scanning with explicit severity policy and exception ownership.
5.5 Add secret scanning and verify no credentials/API keys are embedded in the application artifact.
5.6 Expand architecture-drift enforcement (§9, rules 8–10) and make the script itself a required CI check — the "required CI check" half of this is done; the rule-8–10 expansion is not.
5.7 Add least-privilege GitHub Actions permissions and pin third-party actions to immutable SHAs where practical.
Phase 6 — Release Hardening and Provenance
6.1 Replace broad/stale R8 keep rules with narrowly justified rules. Prove the minified release build still starts Koin, Room, WorkManager, navigation, and the CallScreeningService correctly.
6.2 Build a signed release candidate using CI-held release credentials. Missing release credentials must fail closed.
6.3 Generate SBOM and artifact checksums; retain build provenance tied to the source commit and workflow run.
6.4 Run release instrumentation, manifest/exported-component review, and launch verification on a real or CI-managed device.
6.5 Verify backup exclusions for the encrypted database and secure preference material.
6.6 Perform a final privacy review of Logcat, notifications, crash diagnostics, and generated artifacts.
Phase 7 — Release Candidate Gate
The release candidate is not approved until all of the following are true:
[ ] INV-001 through INV-010 have automated evidence or documented platform-level justification.
[x] Manual-rule mutation path is singular and tested for compile/DI correctness (behavioral test coverage still open — Phase 0.8).
[ ] Five-tier decision matrix is green.
[ ] Bloom optimization has no security-semantic authority.
[ ] Source synchronization is atomic and last-known-good.
[ ] Partial/truncated source datasets are rejected.
[ ] Screening failure is explicit and tested.
[ ] Gray-zone review path is end-to-end tested.
[ ] Keystore invalidation/recovery is instrumented-tested.
[ ] Database migration and reset paths are tested.
[x] Mandatory CI contains the architecture-drift gate as a required (non-advisory) check.
[ ] Mandatory CI contains no other advisory security/correctness tests (unit tests still continue-on-error: true).
[ ] Dependency and secret scans are green or have approved exceptions.
[ ] Release R8 build is validated.
[ ] SBOM, checksum, signing, and provenance artifacts exist.
[ ] Manifest/exported-component and privacy reviews are complete.
12. Definition of Done for security-sensitive changes
A security-sensitive change is not complete when the code compiles. It is complete only when:
the Architecture Contract has been checked;
the owning layer and trust boundary are explicit;
the authoritative-state impact is understood;
derived indexes/caches are updated or invalidated safely;
failure semantics are explicit;
relevant unit/instrumented tests exist;
architecture drift checks pass;
required CI gates pass; and
the ledger records the decision and remaining risk.
Historical reasoning belongs in PROJECT_LEDGER.md; this contract states the current rules.
13. Governance rule (from Lineage B, adopted)
If implementation reality and documentation disagree, do not silently choose one. Record the discrepancy, inspect the actual code/configuration in full, update the contract if the architecture changed intentionally, and update the ledger so the decision becomes auditable. This rule is precisely why this document exists in its current merged form — Lineage B's own snapshot disagreed with Lineage A's shipped, CI-verified code, and this reconciliation is the record of that discrepancy.
This is the adopted v3 reconciled contract. It supersedes the former v2 wording and is committed under the canonical filename Architecture-Contract.md; no separate Architecture-Contract-v3-DRAFT.md file remains. PROJECT_LEDGER.md records this adoption. Historical amendment tracking from v2's footer (Amendments 0–5, applied 2026-07-15) is preserved in PROJECT_LEDGER.md and not repeated here.
