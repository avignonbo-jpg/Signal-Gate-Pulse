# SignalGate Pulse Architecture Contract (v2 — Reconciled)

This document defines the final target architecture for SignalGate Pulse. It is the binding contract for all future changes: any new code, refactor, feature, or bug fix must preserve these rules or explicitly revise this contract first.

**Reconciliation note (this revision, updated 2026-08-13):** §4's Layer 2 now calls out `SecureDatabase`/`SecurityUtils` (the Keystore-wrapped passphrase) as an explicit "root of trust" sub-item rather than one bullet of equal visual weight among six — a deliberate, small borrow from Track 1's "Physical" framing, adopted without re-flipping this document's overall numbering (see discussion below for why a full renumbering isn't worth it).

Two independent Architecture Contract lineages existed for this project simultaneously — one from a Perplexity-originated session (this document's direct ancestor, enforced in CI by `check-architecture-drift.sh`), and one from a separate session lineage (`SignalGate-Pulse-Architecture-Contract.md`, prose-only, no CI enforcement, opposite layer-numbering direction: Layer 7 = Application/Layer 1 = Physical, versus this document's Layer 7 = UI/Layer 1 = Platform-Edge). **This document's numbering is adopted as canonical going forward**, because it is the one an actual machine check enforces — a contract nobody's CI reads is a suggestion, not a contract. All class-ownership content from both lineages is merged below against the verified real source tree (`Signal-Gate-Pulse-consumer-v1`, 77 Kotlin files, cross-checked against `SignalGate-Pulse-Source-of-Truth-Audit.md`). The other lineage's Contract/Roadmap/Audit set should be treated as superseded by this document for architecture-ownership purposes; its phased release-roadmap content is retained separately (§10) since this lineage never had an equivalent.

## 1. Scope

SignalGate Pulse is a single-activity Compose application with navigation handled entirely in Compose, not through multiple activities or XML-driven navigation flows. The app already has MainActivity as the single host for the Compose UI tree and NavGraph as the app's navigation entry point. The architecture must remain centered on this structure.

This contract covers UI, dependency injection, persistence, domain logic, security boundaries, and ownership of classes across OSI-style layers.

No `.xml` layout files may exist under `res/layout/` unless actively inflated by Kotlin code (via `setContentView`, `findViewById`, or ViewBinding) and explicitly listed here as a grandfathered exception. Any layout XML with zero references in `src/main/java` for more than one release cycle must be deleted, not retained "in case it's needed." Current grandfathered exceptions: none. (Enforced automatically by `scripts/check-architecture-drift.sh`, Rule 7.)

## 2. Required target architecture

- Single activity: MainActivity is the only UI activity host.
- Compose navigation: NavGraph owns route wiring and start-destination selection.
- Koin for DI: Koin is the sole dependency injection framework for app wiring.
- Room for persistence: SignalGateDatabase is the Room database, with access routed through SecureDatabase.
- One ViewModel per screen: every screen must have a dedicated ViewModel, and screen state must not be shared by ad hoc cross-screen ViewModel reuse.
- No direct DAO access from UI: UI layer code must depend on ViewModels only, while ViewModels depend on repositories, not DAOs.
- No direct database construction outside the secure path: SecureDatabase is the sole authorized construction path for the encrypted Room database.

## 3. Layer ownership

| Layer | Owns | Does not own |
|---|---|---|
| Layer 7 - UI | Activities, Compose screens, navigation destinations, screen-only state rendering, user interaction handling | DAOs, direct database calls, sync logic, security enforcement, business policy |
| Layer 6 - Presentation | ViewModels, screen state assembly, UI event translation, permission-step orchestration | Room entities, DAO calls, transport/parsing internals, database construction |
| Layer 5 - Application | Use-case style orchestration, flow coordination, cross-repository rules, sync execution | UI rendering, Android widget composition, direct persistence details |
| Layer 4 - Domain | Policy engines and decision logic such as call-risk and precedence evaluation | Android framework classes, UI state, Room annotations, navigation logic |
| Layer 3 - Persistence | Repositories, DAO interfaces, Room entities, database schema, seeding primitives | Compose UI, screen logic, direct platform permissions, parsing policies |
| Layer 2 - Security/Parsing | Sanitization, secure CSV parsing, secure database entry points, integrity-sensitive helpers, Keystore-invalidation handling | UI rendering, route decisions, screen state, business presentation |
| Layer 1 - Platform/Edge | Activity host, BroadcastReceivers, Service entry points, WorkManager workers, Application bootstrap, notification-channel registration | Screen composition, business rules, repository policy, direct feature orchestration |
| Cross-Cutting | Plain data carriers and pure/stateless utility functions, importable from any layer | Business logic, Android framework dependencies beyond basic types, side effects, persistence, security decisions |

## 4. Class ownership map

**Additions in this revision are marked `[+]`** — confirmed to exist in the real source but absent from the prior version of this map.

### Layer 1 - Platform/Edge
- MainActivity
- MainApplication
- SignalGateCallScreeningService
- CallActionReceiver
- PhoneStateReceiver
- CommunitySyncWorker
- SyncBootReceiver `[+]` — BOOT_COMPLETED/MY_PACKAGE_REPLACED receiver that re-schedules CommunitySyncWorker; same ingress-point category as the other receivers, was simply missing from the prior map
- KoinWorkerFactory
- AppModule
- NotificationChannelManager `[+]` — single registration point for all notification channels; this is app-bootstrap, not UI rendering (it was previously unassigned in this lineage)

### Layer 2 - Security/Parsing

**Root of trust (not just another bullet in this layer):** `SecureDatabase` and `SecurityUtils` own the Android Keystore-wrapped SQLCipher passphrase — the one piece of state every other class in this layer, and every layer above it, transitively depends on. This is the closest thing this app has to Track 1's "Physical" concept (bytes at rest, hardware-backed secrets), and it's called out separately here so it doesn't read as visually equal to ordinary CSV sanitization below it. `KeystoreInvalidatedException` (thrown when the wrapped passphrase can no longer be decrypted) and `DatabaseResetEvent` (the recovery-path signal telling Layer 7 the DB was just reset) are part of this same root-of-trust cluster, kept in Layer 2 rather than treated as cross-cutting because they carry a security-relevant fact, not an arbitrary UI flag.

- SecureDatabase — *root of trust*
- SecurityUtils — *root of trust*
- KeystoreInvalidatedException `[+]` — *root-of-trust recovery path*
- DatabaseResetEvent `[+]` — *root-of-trust recovery path*
- SanitizationEngine
- SecureCsvParser
- DatabaseInitializer

### Layer 3 - Persistence
- Database: SignalGateDatabase
- DAOs: SourceDao, UnifiedEntryDao, CallLogDao, SettingDao, SyncHistoryDao, PendingCardDao `[+]`
- Entities: SourceEntity, UnifiedEntryEntity, CallLogEntry, SettingEntry, SyncHistoryEntry, PendingCardEntity `[+]`
- Repositories: BlocklistRepository, CallLogRepository, DataSourceRepository, PendingCardRepository, SettingRepository, SyncHistoryRepository

*(Note: `PendingCardRepository` was already listed in the prior version of this map, but its DAO and Entity were not — an internally inconsistent gap, now closed.)*

### Layer 4 - Domain
- BloomFilterEngine
- PrecedenceEngine
- CallRiskEvaluator
- CallScreeningEngine
- CallInfo (CallTier enum + data class) — five-tier classification: ALLOWLISTED / FEDERAL_BLOCK / HEURISTIC_BLOCK / HEURISTIC_FLAG (gray-zone) / CLEAN_UNKNOWN

### Layer 5 - Application
- ReliableSourceManager
- DataSyncEngine

### Layer 6 - Presentation
- DashboardViewModel
- BlockedNumbersViewModel
- RecentCallsViewModel
- PendingCardViewModel
- OnboardingViewModel
- SourcesViewModel
- SettingsViewModel
- ContactsViewModel
- LogcatViewModel
- TelemetryViewModel

### Layer 7 - UI
- MainActivity hosts
- NavGraph
- Screen
- Screens and composables: ConsumerDashboardScreen, CallLogScreen, BlockAllowListScreen, SettingsScreen, LogcatViewerScreen, OnboardingWizardScreen, DigestScreen, SourcesScreen, PermissionSettingsScreen, and all reusable Compose components

*(Note: the prior map listed both "ConsumerDashboardScreen" and "DashboardScreen" as if distinct — the real source has only `ConsumerDashboardScreen`; the duplicate name is removed here.)*

### Cross-Cutting (no layer ownership)
Plain data carriers and stateless utility functions may be imported by any layer without violating layer-boundary rules, **provided they contain no business logic, no Android framework dependencies beyond basic types, and no side effects.** If a "utility" class ever gains a dependency on a repository, DAO, or Context, it must be reclassified into the appropriate layer immediately — this is a drift signal, not a convenience.

Current cross-cutting classes: `BenchmarkResult`, `CallLogItem`, `PermissionStatus`, `ThreatSource`, `DateUtils`, `PhoneNumberUtils`, `Color`, `Theme`, `SignalGateTheme`, `Effects`.

(Enforced automatically by `scripts/check-architecture-drift.sh`, Rule 6 — blocks Room, DAO, and Context imports in `data/models/` and `utils/`. **Gap identified this revision:** Rule 6 does not currently scan `ui/theme/`, even though `Color`/`Theme`/`SignalGateTheme`/`Effects` are claimed as cross-cutting above. See §8 change log.)

## 5. ViewModel rules

Each screen must be backed by exactly one primary ViewModel. A screen may depend on helpers or shared repositories, but the screen state owner must be singular and explicit. ViewModels must speak only to repositories or application services, never to DAOs directly, because repository indirection is the stability boundary for persistence. If a screen requires new state, the change must be made by extending that screen's ViewModel or by introducing a new screen with its own ViewModel.

Current examples already established in the codebase include SourcesViewModel for SourcesScreen, BlockedNumbersViewModel for BlockAllowListScreen, RecentCallsViewModel for CallLogScreen, PendingCardViewModel for DigestScreen, and SettingsViewModel for SettingsScreen.

## 6. Security boundary meaning

"Security boundary" means the point beyond which a layer is no longer allowed to trust or manipulate raw unvalidated input, secrets, or privileged state without passing through a stricter layer.

### Layer 1 - Platform/Edge
Accepts external callbacks from Android and must treat all inputs as untrusted until passed inward. Services, receivers, and workers are ingress points only, not policy owners.

### Layer 2 - Security/Parsing
This is the first trusted normalization zone. Input is sanitized, parsed, and reduced to safe internal forms before it can reach domain or persistence.

### Layer 3 - Persistence
This layer may store and retrieve state, but it must not decide whether data is safe or semantically correct. It persists already-validated objects and schema-defined entities.

### Layer 4 - Domain
Business rules become authoritative here. Domain engines may decide allow/block, precedence, confidence, or risk, but they must not own UI state or Android lifecycle concerns. Advisory inputs (e.g. STIR/SHAKEN verification status feeding `CallRiskEvaluator`) must never become a sole allow/block trigger — they inform the tier decision, they do not bypass it.

### Layer 5 - Application
Orchestration logic can coordinate secure workflows, but it must not bypass domain policy or directly expose persistence internals to the UI.

### Layer 6 - Presentation
Presentation code may transform trusted domain output into screen state, but it must not reinterpret security decisions or directly accept raw external input as final truth.

### Layer 7 - UI
UI is the outermost surface and must assume all user input and external events are potentially hostile until validated by the inner layers.

## 7. Change control

Any future change must be checked against this contract before implementation. If a change requires violating one of these boundaries, the contract must be updated first, and the update must state what layer, ownership rule, or security boundary is changing.

The practical rule is simple: UI renders, ViewModels adapt, application code orchestrates, domain decides, persistence stores, and security enforces.

**Single-lineage rule (new this revision):** this is now the only Architecture Contract for this project. Any future proposal, session, or tool that produces a competing contract document must reconcile against this one before either is treated as authoritative — see the reconciliation note at the top of this document for why that rule exists.

## 8. Build Integrity

Every third-party import used in `src/main/java` must correspond to a declared dependency in `app/build.gradle` — no import may rely on a transitive dependency pulled in incidentally by another artifact. `minSdkVersion`, `targetSdkVersion`, and `compileSdkVersion` are architectural constraints, not build details: any change to these values requires the same review as a layer-ownership change, since they gate which platform APIs (e.g. `RoleManager.ROLE_CALL_SCREENING`, requiring API 29+) the app may rely on.

**Verified this revision:** `DataSyncEngine.kt`'s XLSX import path uses native `ZipInputStream` + SAX parsing with no Apache POI dependency — this was a deliberate design choice specifically to avoid POI's `MethodHandle`/D8-dexing incompatibility below API 26, not an unfinished migration. Any Open Item or ledger entry describing POI as "needing to be re-added" describes a state that predates this fix and should be corrected, not acted on (see `PROJECT_LEDGER.md`).

## 9. Known Violations (tracked, not yet resolved)

*(New section this revision — the prior version of this contract had no equivalent tracking mechanism; the sibling lineage's Contract did, under its own §6, and is merged in here.)*

**9.1 — `PermissionSettingsScreen` unreachable.** Listed in §4's Layer 7 map and compiles, but has no `Screen` entry and no `NavGraph.kt` `composable(...)` registration. Per §1/§2 this is dead code as shipped. Resolution: add a route + nav entry + Koin ViewModel binding, or delete the file.

**9.2 — `TelemetryViewModel` orphaned.** Registered in Koin's `viewModelModule` (§4, Layer 6) but consumed by no screen, violating §5's one-ViewModel-per-screen pillar in the "no orphan" direction. Resolution: wire into `CallLogScreen` (it already maps `CallLogRepository` → `CallLogItem`, the natural fit) or delete the class and its Koin binding.

**9.3 — `app/build.gradle` doc-comment mismatch.** The comment above the `ksp { room.schemaLocation }` block states `exportSchema = false`; `SignalGateDatabase.kt`'s actual `@Database` annotation has `exportSchema = true` (deliberate). The comment misdocuments the schema-export contract for anyone auditing migrations.

**9.4 — `ShieldStatusGlow.kt` uses `Color.hashCode()` for `Paint` color.** `.hashCode()` is not a defined ARGB packed-int conversion; must use `.toArgb()`. A Layer 7 (UI) correctness bug, not a security-boundary issue.

**9.5 — `HEURISTIC_FLAG` (gray-zone) calls never produce a `PendingCardEntity`.** `CallScreeningEngine.kt`'s own doc comment states gray-zone calls "are never silently blocked — the user always gets to review them via the digest." The actual write path in `SignalGateCallScreeningService`'s audit-recording function gates `PendingCardEntity` creation on `CallTier.HEURISTIC_BLOCK` only. Tier 4 calls get a `CallLogEntry` and nothing else — the digest review the Domain layer's own documented contract promises never happens. This is a Layer 4-vs-Layer 1 contract mismatch: the domain decision says "reviewable," the edge-layer write path disagrees. **Blocks any UI/notification work built on top of gray-zone review until fixed** — see §10.

**9.6 — `check-architecture-drift.sh` Rule 6 does not scan `ui/theme/`.** §4's Cross-Cutting list claims `Color`/`Theme`/`SignalGateTheme`/`Effects` as pure, but the enforcement script's `CROSS_CUTTING_DIRS` array only contains `data/models` and `utils`. The claim in this document is currently unverified by the one thing that's supposed to verify it.

## 10. Feature Completion & Test Coverage (net-new, not yet built)

*(This lineage previously had no phased roadmap. The sibling lineage's roadmap phases are adopted here, scoped down to what's still outstanding and cross-referenced against this document's §9 violations.)*

**10.1 — Fix 9.5 first, before any gray-zone UI work.** A prior session built a gray-zone review notification, a Compose-independent haptic controller (`PulseHapticsController`/`PulseVibration`, Layer 1 — hardware/vibrator I/O triggered from a Service context), extended `CallActionReceiver` (Layer 1) with Block/Allow/Skip actions, and a `PulseTriggerLimiter` rate limiter (Layer 5 — Application: it orchestrates and persists throttling state via `SettingRepository` but does not itself decide allow/block, so it does not belong in Layer 4). **That work is held, not merged** — it was built without a §7 change-control citation, and it assumes a `PendingCardEntity` exists for gray-zone calls, which 9.5 shows is false. Sequence: fix 9.5 → re-verify the digest surface actually shows gray-zone cards → only then reintroduce the notification/haptic layer on top of a working foundation → then the rate limiter, re-checked against §6 Layer 4's "advisory-only" boundary to confirm it only gates the *notification*, never the tier decision or the `CallLogEntry` audit write.

**10.2 — Test coverage gaps, by layer:**
- Layer 4 (Domain): `CallScreeningEngine`/`CallRiskEvaluator` need a test matrix covering all five tiers independently, including the gray-zone path — currently unverified that 9.5's fix doesn't regress tier-selection logic itself.
- Layer 3 (Persistence): migration test coverage for a fresh-install path, not just upgrade-path, per Room's `MigrationTestHelper`.
- Layer 2 (Security): an instrumented test simulating Keystore invalidation, confirming the old encrypted DB file is deleted (not merely inaccessible) — this is the one Layer 2 boundary in §6 with no current enforcing test.
- Layer 1 (Platform/Edge): `SecurityUtilsTest.kt` is currently a placeholder stub per the project ledger, not real test coverage — needs actual implementation.
- Cross-cutting: extend `check-architecture-drift.sh` Rule 6 to scan `ui/theme/` (closes 9.6) as a same-day, low-risk fix alongside any other script change.

**10.3 — Dependency/CVE scanning is not currently a CI gate.** Given SQLCipher, OkHttp, Koin, and Room are all real attack surface, a dependency-vulnerability scan blocking on high/critical CVEs should be added to CI, not left as a manual periodic check.

---

*Amendment status (2026-07-15): Amendments 0 (script/contract numbering), 1 (Layer 5 reassignment), 2 (missing classes added), 3 (Cross-Cutting category), and 4 (XML-layout loophole) have been applied to this document and approved. Amendment 5 (Build Integrity, Section 8) has also been applied and approved, with a standing scope caveat noted in that section for future reconsideration. See `Architecture-Contract-Amendments.md` for the full original proposal text and reasoning behind each.*

*Reconciliation revision applied: see §0 note at top of document, §4 `[+]` markers, §9 (new), §10 (new). Full accounting logged in `PROJECT_LEDGER.md`, Session Log.*
