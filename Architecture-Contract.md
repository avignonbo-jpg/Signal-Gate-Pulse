SignalGate Pulse Architecture Contract (v4 — Final Target Architecture)
This document defines the binding target architecture and security-governance contract for SignalGate Pulse. It is the binding contract for all future changes: any new code, refactor, feature, or bug fix must preserve these rules or explicitly revise this contract first, per §13's single-lineage rule inherited from v3.
Revision note (this session, superseding v3 2026-08-13): v3 stated Phase 0.2–0.6 as open. This session verified, by reading full source files (not headers) and by a confirmed 25/25-passing instrumented CI run, that 0.2, 0.3, 0.4, 0.5, and 0.6 are now implemented and — for 0.2 specifically — CI-green with direct evidence (TEST-emulator-5554...xml, tests="25" failures="0" errors="0", including BloomAuthoritativeDecisionTest's 7 cases). The others (0.3-0.6) have strong source-level evidence (described per-item below) and matching test files now exist in the branch per the independent Source-of-Truth Branch Audit, but this session did not independently re-run each of those CI jobs — that distinction is marked explicitly per item rather than blurred, per §13's own governance rule. Phases 1-7 are carried forward from v3 unchanged except where this session found direct evidence of completion (noted inline); they were not re-audited file-by-file this session.
This document also resolves one internal documentation conflict found this session: AppModule.kt's own doc comments used literal OSI terms (L2 "Data Link" for OkHttp/TLS, L4 "Network" for sanitization, L6 "Presentation Logic" for decision logic) that contradict both real OSI semantics and this contract's Layer 1-7 numbering. §3 below is the single canonical layer mapping; AppModule.kt's comments should be corrected to reference it by name (Layer 1-7) rather than inventing a second, conflicting OSI scheme — tracked as Known Violation §10.13.
Scope
SignalGate Pulse is a single-activity Compose application with navigation handled entirely in Compose, not through multiple activities or XML-driven navigation flows. MainActivity is the single host for the Compose UI tree; NavGraph (SignalGateNavGraph) is the app's navigation entry point, confirmed this session to own 8 declared routes with Dashboard as start destination. The architecture must remain centered on this structure.
This contract covers UI, dependency injection, persistence, domain logic, security boundaries, and ownership of classes across OSI-style layers.
No .xml layout files may exist under res/layout/ unless actively inflated by Kotlin code and explicitly listed here as a grandfathered exception. Current grandfathered exceptions: none. (Enforced by scripts/check-architecture-drift.sh, Rule 7.)
No new user-facing feature or UI work may be started while Phase 0 (§11) has open gate items. As of this session: 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, and 0.7 are implemented at the source level (0.2 additionally CI-confirmed this session). 0.8 (regression-test completeness/mandatory-gate status across all of Phase 0) remains the one item this session did not fully close — see §11.
Required target architecture
Single activity: MainActivity is the only UI activity host.
Compose navigation: NavGraph owns route wiring and start-destination selection.
Koin for DI: Koin is the sole dependency injection framework for app wiring. Confirmed this session: appModule = databaseModule, repositoryModule, engineModule, viewModelModule, notificationModule, workerModule, resolved in that order from MainApplication.onCreate().
Room for persistence: SignalGateDatabase is the Room database, with access routed through SecureDatabase.
One ViewModel per screen: every screen must have a dedicated ViewModel, and screen state must not be shared by ad hoc cross-screen ViewModel reuse. (Exception under active resolution: TelemetryViewModel and RecentCallsViewModel both transform CallLogRepository data for overlapping purposes with no screen currently wired to TelemetryViewModel — see §10.2.)
No direct DAO access from UI or Platform/Edge: UI layer and Layer 1 code must depend on ViewModels or application-boundary classes only. ViewModels depend on repositories, not DAOs.
No direct database construction outside the secure path: SecureDatabase is the sole authorized construction path for the encrypted Room database.
One authoritative path for every decision-affecting mutation: see INV-001 and §5.2. Confirmed this session: DataSourceRepository.insertEntry()/insertEntries() is the single write chokepoint pairing the DAO write with Bloom-filter maintenance and sanitization; SecurityRuleRepository is the single application-layer entry point above it for manual mutation, snapshot replacement, and sync-attempt/failure recording.
Layer ownership (canonical OSI-style mapping — supersedes any other layer numbering in source comments, see Known Violation §10.13)
The numbering direction intentionally mirrors OSI's own logic: Layer 1 is the layer closest to the raw, untrusted "wire" (Android's own callback/broadcast/intent surface — the analog of physical/data-link ingress); Layer 7 is the layer closest to the user (Compose UI — the analog of the application layer). This is a deliberate architectural analogy for this app, not a literal reuse of OSI's seven named protocol layers, and no class comment should claim otherwise (per §10.13).
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
Use-case style orchestration, flow coordination, cross-repository rules, sync execution, the authoritative security-rule mutation boundary (SecurityRuleRepository)
UI rendering, Android widget composition, direct persistence details
Layer 4 - Domain
Policy engines and decision logic - call-risk and precedence evaluation, the explicit SECURITY_FAILURE decision state, the immutable ScreeningDecision consequence contract
Android framework classes, UI state, Room annotations, navigation logic
Layer 3 - Persistence
Repositories, DAO interfaces, Room entities, database schema, seeding primitives
Compose UI, screen logic, direct platform permissions, parsing policies, decision-affecting business rules
Layer 2 - Security/Parsing
Sanitization, secure CSV/XLSX parsing, secure database entry points, integrity-sensitive helpers, Keystore-invalidation handling, artifact authenticity verification
UI rendering, route decisions, screen state, business presentation
Layer 1 - Platform/Edge
Activity host, BroadcastReceivers, Service entry points, WorkManager workers, Application bootstrap, notification-channel registration, startup diagnostics
Screen composition, business rules, repository policy, direct DAO access, direct feature orchestration
Cross-Cutting
Plain data carriers and pure/stateless utility functions, importable from any layer
Business logic, Android framework dependencies beyond basic types, side effects, persistence, security decisions
Class ownership map
[+] = confirmed to exist in source this session but previously unlisted or unverified. [RESOLVED] = a tracked violation fixed and (at minimum) source-verified this session; CI status noted per item.
Layer 1 - Platform/Edge
MainActivity
MainApplication
SignalGateCallScreeningService — confirmed this session to own: decision dispatch via toCallResponse(), typed SECURITY_FAILURE fail-safe handling via handleSecurityFailure(), decision-consequence persistence via executeDecisionConsequences(), and best-effort UX dispatch via dispatchDecisionUx(), in that order — persistence always completes before UX dispatch is attempted, and a UX dispatch failure is caught separately so it can never retroactively become a SECURITY_FAILURE. Addition, 2026-08-25: onScreenCall()'s entry point also now handles a null/malformed Call.Details.handle explicitly via handleSecurityFailure() before this pipeline even begins, instead of a bare return that bypassed all of the above — see §10.9.
CallActionReceiver — [RESOLVED, Phase 0.1/0.7] depends on SecurityRuleRepository/PendingCardRepository only; no direct DAO injection.
PhoneStateReceiver
CommunitySyncWorker
SyncBootReceiver — BOOT_COMPLETED/MY_PACKAGE_REPLACED receiver that re-schedules CommunitySyncWorker
KoinWorkerFactory
AppModule
NotificationChannelManager — single registration point for all notification channels
StartupDiagnostics [+, new this session] — object emitting fixed-name, PII-free startup checkpoint markers (process start through first frame, Koin start, Keystore/SQLCipher/Room migration stages, Bloom rehydration, screening-service readiness). Confirmed this session to carry only event names and elapsed milliseconds, never phone numbers, database contents, keys, or exception payloads — satisfies INV-007 by construction. Backs StartupTimingTest. Not present in the Source-of-Truth Branch Audit's 83-file count; the branch snapshot audited had 83 main files, this session's snapshot has 84 — recorded here as a discrepancy per §13 rather than silently reconciled.
Layer 2 - Security/Parsing
Root of trust: SecureDatabase and SecurityUtils own the Android Keystore-wrapped SQLCipher passphrase. KeystoreInvalidatedException and DatabaseResetEvent are part of this same root-of-trust cluster.
SecureDatabase — root of trust
SecurityUtils — root of trust
KeystoreInvalidatedException — root-of-trust recovery path
DatabaseResetEvent — root-of-trust recovery path
SanitizationEngine
SecureCsvParser
DatabaseInitializer
ArtifactAuthenticityVerifier — verifies downloaded source artifacts against cryptographic authenticity metadata (manifest/digest). Backs INV-002/3.3.
SourceAuthenticityTrustAnchor — trusted authenticity configuration consumed by ArtifactAuthenticityVerifier.
SnapshotSanityValidator — structural/content sanity checks on imported snapshots before activation.
SourceRecordValidator — per-record validation for imported source records.
BoundedXlsxParser (still not a separately extracted class — the XLSX-parsing half of DataSyncEngine continues to live inside DataSyncEngine.kt as XlsxParseException/RowLimitExceededException/SharedStringsLimitExceededException handling. Confirmed this session: limit violations throw rather than return a partial/truncated result — DataSyncEngine.kt's own doc states "the exception propagates so callers cannot activate a partial result," satisfying Phase 0.5/INV-002's hard-failure requirement even without the extraction. Extraction into a separate class remains a structural nicety, not a blocking gap.)
Layer 3 - Persistence
Database: SignalGateDatabase
DAOs: SourceDao, UnifiedEntryDao, CallLogDao, SettingDao, SyncHistoryDao, PendingCardDao
Entities: SourceEntity, UnifiedEntryEntity, CallLogEntry, SettingEntry, SyncHistoryEntry, PendingCardEntity
Repositories: CallLogRepository, DataSourceRepository, PendingCardRepository, SettingRepository, SyncHistoryRepository
BlocklistRepository — thin facade over SecurityRuleRepository, retained only for existing ViewModel callers (BlockedNumbersViewModel, ContactsViewModel, PendingCardViewModel).
Layer 4 - Domain
BloomFilterEngine — derived, disposable, rebuildable, non-authoritative index (INV-001). Confirmed this session (source + 25/25-passing BloomAuthoritativeDecisionTest on emulator): a cold, warm, post-mutation, post-source-replacement, rebuilt, or reset Bloom state always yields an optimized decision identical to the authoritative DB decision, in every tested condition. bloomReady gates trust in either filter; the entire clear-to-rebuild window is treated as untrusted, not just its start.
PrecedenceEngine
CallRiskEvaluator
CallScreeningEngine
CallInfo (CallTier enum + data class) — six-tier: ALLOWLISTED / FEDERAL_BLOCK / HEURISTIC_BLOCK / HEURISTIC_FLAG / CLEAN_UNKNOWN / SECURITY_FAILURE. [RESOLVED, Phase 0.6] SECURITY_FAILURE is a structurally distinct tier, not a variant of CLEAN_UNKNOWN.
ScreeningAction [+] — domain-only action enum (ALLOW/BLOCK/SCREEN/SECURITY_FAILURE) with no android.telecom dependency. The Android CallResponse policy is derived from it exclusively inside SignalGateCallScreeningService.toCallResponse() — the one function in the app permitted to make that translation, satisfying §0.6's "define the Android CallResponse policy separately from the domain decision."
ScreeningDecision, NotificationPolicy, HapticPolicy [+] — the immutable decision-and-consequence contract envisioned by former Phase 1.5. Confirmed this session: ScreeningDecision carries tier, callAction, auditRequired, reviewCardRequired, notificationPolicy, hapticPolicy, and securityFailure, with an init block that hard-requires securityFailure == (tier == SECURITY_FAILURE) == (callAction == SECURITY_FAILURE) — the SECURITY_FAILURE-not-ALLOW and SECURITY_FAILURE-not-CLEAN_UNKNOWN invariants are now enforced by the type itself, not just by convention at each call site.
SourceLifecycleState, SnapshotMetadata [+] — ENABLED/SYNCING/HEALTHY/STALE/FAILED/REJECTED/DISABLED, matching the states former Phase 3.4 specified. SnapshotMetadata (version, hash, acceptedRecordCount) is committed only alongside an accepted snapshot.
Layer 5 - Application
ReliableSourceManager — confirmed this session to hold a fixed, hardcoded federal source list (FTC Do Not Call Registry via REST API, FCC Consumer Complaints via CSV with a documented primary/fallback URL pair) rather than reading arbitrary user-added sources. Per SourcesViewModel's own doc comment, the free-text "Add Source" (CSV/URL/XLSX) UI flow was removed because ReliableSourceManager never consumed anything it produced — Pulse's actual source model is fixed: MANUAL (contacts + post-call decisions), FTC, and FCC. This narrows the practical scope of INV-008/Phase 0.3/0.8 considerably: there is no general user-defined source type to protect against in the current product, only the three fixed types.
DataSyncEngine — parsing/validation orchestration; hard-fails (throws) rather than returning partial results on any bounded-limit violation (row count, shared-strings size) — confirmed this session.
SecurityRuleRepository — [RESOLVED, Phase 0.1, extended Phase 0.4 this session] the single authoritative entry point for every decision-affecting mutation. Confirmed surface this session: addManualBlock(), addManualAllow(), removeRule(), getAllUserRules() (Phase 0.1), plus replaceSourceSnapshot(), beginSourceSync(), recordSourceFailure() (Phase 0.4/INV-002, new this session). replaceSourceSnapshot() records the sync attempt before the transaction, requires a non-empty candidate set, performs delete-then-insert entirely inside database.withTransaction {}, and only rebuilds the Bloom filter (a non-blocking, best-effort step) after a successful commit — a failed candidate can never touch the previously accepted entries.
SourceSyncUseCase [+] — thin wrapper exposing syncSource(), syncSources(), syncAllFederalSources() over ReliableSourceManager. Confirmed wired into SourcesViewModel and DashboardViewModel this session — this closes the former open violation where SourcesViewModel fabricated HEALTHY status without a real sync; SourcesViewModel's own doc comment now states "this method never fabricates HEALTHY."
Layer 6 - Presentation
DashboardViewModel — now takes SourceSyncUseCase (real sync boundary, confirmed this session).
BlockedNumbersViewModel
RecentCallsViewModel
PendingCardViewModel
OnboardingViewModel
SourcesViewModel — [RESOLVED this session] real sync path confirmed (see SourceSyncUseCase above); the former "Add Source" custom flow was deliberately removed rather than fixed, since the product's source model is fixed (see ReliableSourceManager above).
SettingsViewModel
ContactsViewModel — directly invokes ContactsContract/ContentResolver/Cursor; still open as a Phase 4 hardening item, not re-verified this session.
LogcatViewModel
TelemetryViewModel — [STILL OPEN, confirmed this session] registered in Koin (viewModelModule) but not instantiated by any screen — grepped this session across ui/, zero call sites outside its own class body. See §10.2.
Layer 7 - UI
MainActivity hosts
NavGraph — confirmed this session: 8 routes (dashboard, sources, call_log, block_list, settings, logcat, onboarding, digest), digest additionally reachable via the signalgate://digest deep link, matching the intent-filter declared on MainActivity.
Screen
Screens and composables: ConsumerDashboardScreen, CallLogScreen, BlockAllowListScreen, SettingsScreen, LogcatViewerScreen, OnboardingWizardScreen, DigestScreen, SourcesScreen, and all reusable Compose components.
PermissionSettingsScreen — [STILL OPEN, confirmed this session] a real, substantial screen (Step 1.10 — surfaces runtime permissions, ROLE_CALL_SCREENING, and battery-optimization status) that has no route in NavGraph.kt/Screen.kt — grepped this session, zero references outside its own file. See §10.1.
Layer 1 notification consequence consumers (new this session, Cross-Layer 1/6 boundary)
PulseHapticsController, PulseTriggerLimiter, PulseVibration — downstream consumers of ScreeningDecision.notificationPolicy/hapticPolicy only. Confirmed this session: SignalGateCallScreeningService.dispatchDecisionUx() calls these only after executeDecisionConsequences() (audit + review-card persistence) has already completed, and any exception from UX dispatch is caught separately and logged rather than allowed to alter the already-persisted decision — satisfies INV-006 ("rate limiting may suppress dispatch, never audit or review state").
Cross-Cutting
Plain data carriers and stateless utility functions may be imported by any layer without violating layer-boundary rules, provided they contain no business logic, no Android framework dependencies beyond basic types, and no side effects.
Current cross-cutting classes: BenchmarkResult, CallLogItem, PermissionStatus, ThreatSource, DateUtils, PhoneNumberUtils, Color, Theme, SignalGateTheme, Effects.
(Enforced by scripts/check-architecture-drift.sh, Rule 6 — blocks Room, DAO, and Context imports in data/models/ and utils/. Still-open gap, carried from v3, not re-verified this session: Rule 6 does not scan ui/theme/.)
ViewModel and ownership rules
Each screen must have exactly one primary ViewModel. ViewModels depend on repositories or application services, never DAOs directly.
UI code must not persist security state directly. Direct SharedPreferences, Room, DAO, ContentResolver, or security-root access from Compose screens is prohibited unless explicitly grandfathered here.
Platform/Edge components are ingress points, not policy owners. A receiver, service, worker, or activity may validate the shape of an incoming event and translate it into an application command, but it must not perform feature-level DAO mutation or independently reimplement security policy.
A screen that needs new state extends its owning ViewModel or introduces a new screen/ViewModel pair. Cross-screen ViewModel reuse is prohibited unless the shared object is explicitly a domain/application service rather than a screen state owner. (TelemetryViewModel vs. RecentCallsViewModel's overlapping CallLogRepository-transformation responsibility must be resolved under this rule — see §10.2.)
Security boundary and trust model
A security boundary is the point beyond which a layer may no longer trust or manipulate raw input, secrets, or privileged state without passing through the stricter boundary defined below.
Layer 1 - Platform/Edge: Accepts Android callbacks, broadcasts, service calls, worker triggers, intents, and deep links. All external values are untrusted. Layer 1 validates the minimum shape required to safely hand the event to an application service. Layer 1 must not directly mutate security policy through DAOs.
Layer 2 - Security/Parsing: The first trusted normalization zone. Owns canonicalization, bounded parsing, secure database entry points, Keystore handling, integrity-sensitive validation, and artifact authenticity verification. External source material cannot become active policy until it passes this boundary — confirmed this session for the parsing half (hard-fail on limit violation) and the persistence-activation half (transactional replace-or-reject in SecurityRuleRepository, Layer 5).
Layer 3 - Persistence: Stores validated state and provides transactional persistence primitives. Does not decide whether data is trustworthy or whether a write is decision-affecting. The encrypted database is the authoritative store for active security policy (INV-001).
Layer 4 - Domain: Owns authoritative security decisions: precedence, risk, call tier, and decision semantics. Domain output is explicit and typed — confirmed this session via ScreeningDecision's self-enforcing invariant. A security failure is not equivalent to ALLOW, CLEAN_UNKNOWN, or any other trusted result — [RESOLVED, Phase 0.6, confirmed this session].
Layer 5 - Application: Owns cross-repository workflows and security-state mutation orchestration — the approved boundary for manual rule mutations, source snapshot activation (both confirmed implemented this session), digest/review workflows, and notification throttling. Must not bypass domain policy.
Layer 6 - Presentation: Transforms trusted domain/application results into screen state and user events. Must not reinterpret security decisions, bypass application services, or accept raw external input as final truth.
Layer 7 - UI: Renders state and collects user interaction. UI is never the authoritative security decision-maker and must not directly persist security policy.
Security invariants
INV-001 — Authoritative Security State. The encrypted Room/SQLCipher database is the authoritative source of active security policy; Bloom filters, indexes, caches, derived snapshots, and in-memory state are non-authoritative accelerators. Status: [SATISFIED, confirmed this session]. Manual mutation (Phase 0.1) and source-snapshot mutation (Phase 0.4) both route through the single SecurityRuleRepository/DataSourceRepository chokepoint. Derived-index rebuild/invalidate semantics are tested end-to-end (Phase 0.2, 25/25 passing on emulator this session, cold/warm/post-mutation/post-replacement/rebuild/reset all covered).
INV-002 — Last-Known-Good Security Dataset. Status: [SATISFIED, confirmed this session]. SecurityRuleRepository.replaceSourceSnapshot() is transactional (withTransaction {}), rejects empty candidates before touching state, records sync-attempted separately from sync-accepted (recordSyncAttempt/recordSnapshotAccepted/recordSyncFailure), and DataSyncEngine hard-fails (throws) rather than returning a partial parse result on any bounded-limit violation.
INV-003 — Explicit Security Failure. Status: [SATISFIED, confirmed this session]. SECURITY_FAILURE is a structurally distinct CallTier/ScreeningAction, enforced by ScreeningDecision's own init invariant; the Android CallResponse policy for it is an explicit, documented branch in toCallResponse(), not a fallthrough from ALLOW.
INV-004 — External Input Is Untrusted. Unchanged from v3; not re-audited file-by-file this session.
INV-005 — Deterministic Security Decisions. Unchanged from v3; not re-audited this session.
INV-006 — Decision Side Effects Follow the Decision Contract. Status: [SATISFIED for decision/audit/review-card/notification/haptic consequences, confirmed this session]. ScreeningDecision.forTier() is the single source of auditRequired/reviewCardRequired/notificationPolicy/hapticPolicy per tier; executeDecisionConsequences() persists before dispatchDecisionUx() runs, and a UX-dispatch failure is caught independently so it cannot rewrite an already-persisted outcome.
INV-007 — No Raw PII in Operational Logs. Status: [SATISFIED, confirmed this session for the paths reviewed]. StartupDiagnostics carries only fixed event names and elapsed milliseconds. SignalGateCallScreeningService's notifications use VISIBILITY_PRIVATE with an explicit buildRedactedPublicVersion() containing no phone number, confidence, or action — matches NotificationPrivacyTest.
INV-008 — Protected Source Lifecycle. Status: [SATISFIED, confirmed this session]. DataSourceRepository.PROTECTED_SOURCE_TYPES = {MANUAL, FTC, FCC} — verified to match ReliableSourceManager's literal sourceType strings exactly, so the guard cannot silently miss a real source type. deleteSource() throws ProtectedSourceDeletionException before the DAO cascade can run for any protected type; only entries or (for federal sources) enablement may be changed.
INV-009 — Edge-to-Application Boundary. Status: satisfied for CallActionReceiver (Phase 0.1/0.7); unchanged from v3, not re-audited further this session.
INV-010 — Release Gates Are Mandatory. Status: unchanged from v3 (check-architecture-drift.sh is a required CI gate; unit tests' continue-on-error status not re-checked this session).
Change control and build integrity
Unchanged from v3. Any change must be checked against this contract before implementation. Single-lineage rule (§13) still applies: this is the only Architecture Contract lineage for this project; any competing document must reconcile against actual current source and CI state before either is treated as authoritative — this document's own revision note is itself an application of that rule.
Architecture enforcement
Unchanged from v3; scripts/check-architecture-drift.sh content not re-read this session. The follow-up noted in v3 (rules 8-10 are target enforcement, not yet grep rules in the script) is carried forward as still open.
Known violations and required resolution
10.1 PermissionSettingsScreen unreachable — [STILL OPEN, re-confirmed this session by direct grep]. Resolve by wiring it into NavGraph.kt/Screen.kt with its intended entry point, or delete it. Phase 4.
10.2 TelemetryViewModel orphaned — [STILL OPEN, re-confirmed this session by direct grep]. Registered in Koin, zero screen call sites. Overlaps functionally with RecentCallsViewModel (both transform CallLogRepository data). Resolve by wiring it to a justified distinct owner, merging its responsibility into RecentCallsViewModel, or removing the class and its Koin binding. Phase 4.
10.3 Build comment drift — carried from v3, not re-checked this session. Phase 4.
10.4 ShieldStatusGlow color conversion — carried from v3, not re-checked this session. Phase 4.
10.5 Gray-zone persistence contract — [RESOLVED, v3, CI-verified 2026-08-19]. Unchanged.
10.6 Architecture script coverage gap — carried from v3, not re-checked this session. Phase 5.
10.7 Security-rule mutation has multiple paths — [RESOLVED, v3]. Unchanged.
10.8 Source synchronization atomicity — [RESOLVED, confirmed this session]. Formerly open in v3; SecurityRuleRepository.replaceSourceSnapshot() now provides the atomic, last-known-good-preserving path described in INV-002 above.
10.9 Screening exception path is an implicit allow — [RESOLVED, confirmed this session]. Formerly open in v3 (Phase 0.6); SignalGateCallScreeningService now has an explicit, documented toCallResponse()/handleSecurityFailure() pair — see §4 Layer 1 and INV-003. Addition, 2026-08-25, not a correction to the above — this resolution was accurate for what it claimed (the try/catch machinery's internal ordering), but did not cover a separate, adjacent code path: onScreenCall() previously exited via a bare details.handle?.schemeSpecificPart ?: return on a null/malformed Call.Details.handle, entirely outside the try/catch this section describes, silently skipping respondToCall() entirely (Android's own ~5s no-response timeout then proceeds as if allowed). Confirmed live before fixing, not assumed stale. Fixed: the null case now calls handleSecurityFailure(details, phoneNumber = "UNKNOWN_MALFORMED_HANDLE") — an explicit, audited SECURITY_FAILURE response. Also added an explicit withTimeout(3_500) around the engine.screenCall() decision call itself, since nothing previously enforced a deadline shorter than Android's own platform timeout. Not yet covered by an automated test — see PROJECT_LEDGER.md, 2026-08-25 entry, and SECURITY-DEVOPS-BUILD-PLAN.md 4.9.A/B/C for the still-open exit tests.
10.10 Platform edge directly accesses persistence — [RESOLVED, v3]. Unchanged.
10.11 Source deletion semantics — [RESOLVED, confirmed this session]. Formerly open in v3 (Phase 0.3); DataSourceRepository.PROTECTED_SOURCE_TYPES + ProtectedSourceDeletionException now define and enforce this — see INV-008 above.
10.12 Release hardening gaps — carried from v3, not re-checked this session. Phase 5/6.
10.13 AppModule OSI-layer comment conflict — [NEW, this session]. AppModule.kt's doc comments describe engineModule bindings using literal-but-incorrect OSI terms (L2 "Data Link" for the OkHttp/TLS transport client, L4 "Network" for sanitization, L6 "Presentation Logic" for decision engines) that conflict with both real OSI semantics and this contract's own Layer 1-7 numbering (§3). This is documentation-only — no code/binding is wrong, only the comment's layer labels. Resolve by updating AppModule.kt's comments to reference Layer 1-7 by name per §3. Phase 4 (documentation hygiene), low risk.
Security-First Build Plan
Phase 0 — Security Control-Plane Integrity Gate
Objective: prove that security state cannot diverge between authoritative persistence, derived indexes, external source data, and edge behavior.
0.1 — [COMPLETE, CI-verified 2026-08-13] Single approved application/security-rule mutation boundary for manual allow, manual block, contact rules, imported rules, source replacement, and rule removal.
0.2 — [COMPLETE, CI-verified this session — 25/25 instrumented tests passing, including all 7 BloomAuthoritativeDecisionTest cases]. Database explicitly authoritative, Bloom filters explicitly derived, with safe invalidation/rebuild behavior confirmed across cold/warm/post-mutation/post-replacement/rebuild/reset states.
0.3 — [COMPLETE, source-confirmed this session, not independently CI-reconfirmed]. Protected source lifecycle semantics defined and enforced (PROTECTED_SOURCE_TYPES, ProtectedSourceDeletionException); matching SourceDeletionCascadeTest (androidTest) and DataSourceRepositoryDeletionTest (test) exist in the branch per this session's audit.
0.4 — [COMPLETE, source-confirmed this session, not independently CI-reconfirmed]. Transactional source replacement with last-known-good retention implemented (SecurityRuleRepository.replaceSourceSnapshot()); matching SourceActivationTransactionTest (androidTest) exists.
0.5 — [COMPLETE, source-confirmed this session, not independently CI-reconfirmed]. Resource-limit violations are hard failures in DataSyncEngine (parser throws rather than returns partial data); matching DataSyncEngineXlsxLimitTest (test) exists.
0.6 — [COMPLETE, source-confirmed this session; the emulator CI run confirming §0.2 this session also compiled and ran the full instrumented suite green, which structurally required this code to compile and its dependents to resolve — but this session did not isolate and re-verify the specific CallScreeningEngineSecurityFailureTest result]. Explicit SECURITY_FAILURE decision semantics and documented Android CallResponse behavior — see §4 Layer 1, ScreeningAction, ScreeningDecision.
0.7 — [COMPLETE, CI-verified 2026-08-13] CallActionReceiver routed through an application boundary; no direct DAO access from Layer 1.
0.8 — [PARTIALLY COMPLETE — status upgraded this session, not fully closed]. v3 stated "no test coverage exists yet." This session's audit found 8 androidTest files and 17 test files in the branch, collectively naming and covering essentially every Phase 0 item (Bloom authority, decision matrix, source deletion cascade, source activation transactions, security-failure decisions, gray-zone reviewability, notification privacy, Keystore/SQLCipher). This session directly confirmed only the instrumented suite as a whole is green (25/25). Phase 0.8 closes only once every one of those tests is confirmed running as a mandatory (non-advisory) CI gate — the instrumented workflow is already mandatory per v3 §9; the JVM test workflow's advisory/mandatory status was not re-checked this session (see INV-010).
Phase 1 — Decision Engine Integrity
1.1-1.3 — source-level evidence this session strongly suggests these are done (six-tier CallTier including SECURITY_FAILURE; CallScreeningEngineDecisionMatrixTest exists; Bloom cold/warm/post-mutation/rebuild/reset all tested per §0.2) but this session did not re-derive the full decision-matrix test coverage independently — treat as likely complete, confirm before closing.
1.4 — [RESOLVED, v3, CI-verified 2026-08-19]. Unchanged.
1.5 — [COMPLETE, confirmed this session]. ScreeningDecision is exactly the immutable decision-and-consequence result this item called for.
Remaining Phase 1 items not re-audited this session: carried forward from v3 as previously stated.
Phases 2-7 — carried forward from v3 without re-audit this session, except:
2.1-2.3 — source-level evidence this session (PulseHapticsController/PulseTriggerLimiter wired downstream of ScreeningDecision only, per §4/INV-006) suggests these are substantially done; not independently re-verified against v3's full item text.
3.1, 3.4, 3.5, 3.6 — source-level evidence this session (SourceLifecycleState's seven states, attempt-vs-accepted distinction, last-known-good preservation on failure) suggests these are done; not independently re-verified against v3's full item text.
All other Phase 2-7 items: unchanged from v3, not re-audited this session.
Definition of Done for security-sensitive changes
Unchanged from v3.
Governance rule (from v3 Lineage B, adopted, unchanged)
If implementation reality and documentation disagree, do not silently choose one. Record the discrepancy, inspect the actual code/configuration in full, update the contract if the architecture changed intentionally, and update the ledger so the decision becomes auditable. This v4 revision is itself an application of that rule: v3 documented Phase 0.2-0.6 as open; this session found, by reading full files rather than headers, that they are implemented, and recorded exactly which claims were independently CI-confirmed versus source-confirmed-only rather than blurring the two.
Single-lineage rule: this is the only Architecture Contract lineage for this project going forward. Any future proposal, session, or tool that produces a competing contract document must reconcile against this one — specifically, against actual current source and CI state, not just against the text of the prior document — before either is treated as authoritative.
This is the adopted v4 contract, committed under the canonical filename Architecture-Contract.md. PROJECT_LEDGER.md should record this adoption and the specific §10.8/10.9/10.11/10.13 status changes and the new §10.13 finding.
