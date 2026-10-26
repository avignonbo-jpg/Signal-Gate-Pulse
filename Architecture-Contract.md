SignalGate Pulse — Architecture Contract (Final Target Architecture)

This is the binding architecture and security-governance contract for SignalGate Pulse. This document is the single authoritative architecture reference for `consumer-v1`. Every new code change, refactor, feature, bug fix, test, workflow, or governance document must cite this contract and must not contradict it without an explicit, evidence-backed amendment recorded in `PROJECT_LEDGER.md` and in this file.

Status: Final target architecture for the current branch.
Source basis: live `consumer-v1` implementation in the repository plus the current authoritative branch audit snapshot `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`.

1. Scope and governing rule

This contract defines the final target architecture for SignalGate Pulse:
- Single Activity, Compose-only navigation
- Koin as the sole DI framework
- Room / SQLCipher-backed persistence through the secure database path
- One ViewModel per screen
- Explicit OSI-style layer ownership for source code
- Security-first boundaries and trust model for every layer

If implementation reality and this contract disagree, the code/configuration must be inspected directly and the contract must be corrected only with evidence. The project must not silently choose whichever document is more convenient.

2. Required target architecture

2.1 Single Activity
- `MainActivity` is the only Activity host for the app.
- No second app activity is part of the target architecture.
- UI composition and routing are hosted within the single Activity and Compose NavHost.

2.2 Compose navigation
- All screen navigation is Compose-based and owned by `ui/navigation/NavGraph.kt`.
- `Screen.kt` defines the canonical route objects.
- The outer NavHost is the single route broker for app screens.
- Deep links are allowed only when explicitly represented in the navigation graph.

2.3 Koin for dependency injection
- Koin is the sole dependency injection framework in the app.
- `di/AppModule.kt` owns the application wiring for database, repositories, services, engines, workers, notification components, and ViewModels.
- No ad hoc manual wiring is allowed for production code.

2.4 Room persistence and secure database path
- Persistence is Room-backed via `SignalGateDatabase`.
- The secure path is `SecureDatabase` -> `SignalGateDatabase` -> DAO/repository access.
- Direct database construction outside the secure path is prohibited.
- The database is the authoritative state for active security policy.

2.5 One ViewModel per screen
- Every distinct screen must have one primary ViewModel.
- Screen state must not be shared through ad hoc cross-screen ViewModels.
- ViewModels depend on repositories or application services, not DAOs directly.
- UI screens may not persist security-sensitive state directly.

3. Layer model (OSI-style mapping)

The numbering below follows the OSI pattern: lower numbers are closer to raw untrusted input and platform boundaries; higher numbers are closer to user experience.

Layer 1 — Platform / Edge
Owns:
- `MainActivity`
- `MainApplication`
- `SignalGateCallScreeningService`
- `CallActionReceiver`
- `PhoneStateReceiver`
- `CommunitySyncWorker`
- `SyncBootReceiver`
- `KoinWorkerFactory`
- `NotificationChannelManager`
- startup / lifecycle / process diagnostics

Does not own:
- security policy decisions
- persistence mutation semantics
- domain precedence logic
- business interpretation of raw data

Layer 2 — Security / Parsing
Owns:
- `SecureDatabase`
- `SecurityUtils`
- `DatabaseInitializer`
- `SanitizationEngine`
- `SecureCsvParser`
- `SourceRecordValidator`
- `SnapshotSanityValidator`
- `ArtifactAuthenticityVerifier`
- `SourceAuthenticityTrustAnchor`
- Keystore invalidation and database-reset handling

Does not own:
- UI rendering
- route decisions
- user-facing policy choices
- screen-state interpretation

Layer 3 — Persistence
Owns:
- `SignalGateDatabase`
- Room entities
- DAO interfaces
- repositories including `DataSourceRepository`, `PendingCardRepository`, `SettingRepository`, `SyncHistoryRepository`, `CallLogRepository`
- `BlocklistRepository` as a compatibility facade only

Does not own:
- domain decision logic
- trust evaluation
- call-flow enforcement

Layer 4 — Domain
Owns:
- `BloomFilterEngine`
- `PrecedenceEngine`
- `CallRiskEvaluator`
- `CallScreeningEngine`
- `CallInfo` and `ScreeningAction`
- `ScreeningDecision`, `NotificationPolicy`, `HapticPolicy`
- `SourceLifecycleState`, `SnapshotMetadata`

Does not own:
- Android framework callbacks
- UI rendering
- database schema or SQL behavior
- screen-only state

Layer 5 — Application
Owns:
- `ReliableSourceManager`
- `DataSyncEngine`
- `SecurityRuleRepository`
- `SourceSyncUseCase`
- authoritative mutation orchestration across repositories and dependency state

Does not own:
- raw input validation at the platform edge
- UI rendering
- low-level parsing of external data beyond orchestration

Layer 6 — Presentation
Owns:
- all ViewModels
- screen state assembly
- permission orchestration
- UI event translation into application requests

Does not own:
- security decision semantics
- direct DAO access
- direct persistence of security state

Layer 7 — UI
Owns:
- `MainActivity` host composition
- `NavGraph` and route selection
- screen composables and reusable UI components
- user interaction capture and rendering

Does not own:
- authoritative decisions
- security enforcement
- persistence writes for security-critical state

Cross-Cutting
Owns:
- plain data carriers and helper utilities such as models and pure stateless helpers

Must not:
- contain business logic
- own security decisions
- create side effects
- import Room, Context, or security-root APIs beyond trivial helper usage

4. Class ownership map

The following class-level ownership is the target model for the current branch.

Layer 1 — Platform / Edge
- `MainActivity`
- `MainApplication`
- `SignalGateCallScreeningService`
- `CallActionReceiver`
- `PhoneStateReceiver`
- `CommunitySyncWorker`
- `SyncBootReceiver`
- `KoinWorkerFactory`
- `NotificationChannelManager`
- `StartupDiagnostics`

Layer 2 — Security / Parsing
- `SecureDatabase`
- `SecurityUtils`
- `DatabaseInitializer`
- `KeystoreInvalidatedException`
- `DatabaseResetEvent`
- `SanitizationEngine`
- `SecureCsvParser`
- `SourceRecordValidator`
- `SnapshotSanityValidator`
- `ArtifactAuthenticityVerifier`
- `SourceAuthenticityTrustAnchor`

Layer 3 — Persistence
- `SignalGateDatabase`
- `SourceEntity`, `UnifiedEntryEntity`, `CallLogEntry`, `SettingEntry`, `SyncHistoryEntry`, `PendingCardEntity`
- `SourceDao`, `UnifiedEntryDao`, `CallLogDao`, `SettingDao`, `SyncHistoryDao`, `PendingCardDao`
- `CallLogRepository`, `DataSourceRepository`, `PendingCardRepository`, `SettingRepository`, `SyncHistoryRepository`
- `BlocklistRepository` as a compatibility facade only

Layer 4 — Domain
- `CallTier`, `CallInfo`
- `ScreeningAction`
- `ScreeningDecision`, `NotificationPolicy`, `HapticPolicy`
- `CallRiskEvaluator`
- `CallScreeningEngine`
- `BloomFilterEngine`
- `PrecedenceEngine`
- `SourceLifecycleState`, `SnapshotMetadata`

Layer 5 — Application
- `ReliableSourceManager`
- `DataSyncEngine`
- `SecurityRuleRepository`
- `SourceSyncUseCase`

Layer 6 — Presentation
- `DashboardViewModel`
- `BlockedNumbersViewModel`
- `RecentCallsViewModel`
- `PendingCardViewModel`
- `OnboardingViewModel`
- `SourcesViewModel`
- `SettingsViewModel`
- `ContactsViewModel`
- `LogcatViewModel`
- `PermissionSettingsScreen` is a real routed screen and its UI state is owned by `OnboardingViewModel`

Layer 7 — UI
- `MainActivity` hosts
- `NavGraph` and route selection
- `Screen`
- `ConsumerDashboardScreen`, `CallLogScreen`, `BlockAllowListScreen`, `SettingsScreen`, `LogcatViewerScreen`, `OnboardingWizardScreen`, `DigestScreen`, `SourcesScreen`, `PermissionSettingsScreen`
- reusable Compose components and theme resources

Layer 1 notification consequence consumers (Cross-Layer 1/6 boundary)
- `PulseHapticsController`, `PulseTriggerLimiter`, `PulseVibration`

Cross-Cutting
- plain data carriers and stateless utility functions may be imported by any layer without violating layer-boundary rules, provided they contain no business logic, no Android framework dependencies beyond basic types, and no side effects.
- historical comments referencing a former `TelemetryViewModel` are not active code paths and do not represent a live class or live Koin binding.

5. Security-first model and security boundaries

A security boundary is the point beyond which a layer may no longer trust or manipulate raw input, secrets, privileged state, or security-affecting data without passing through the stricter boundary defined below.

5.1 Layer 1 security boundary
- Inputs from Android callbacks, broadcasts, intents, service invocations, deep links, and worker triggers are untrusted.
- Layer 1 validates minimum shape and routes the event into the application or domain layer.
- Layer 1 must not decide security policy, mutate authoritative state, or bypass the next boundary.
- The edge is a translation layer, not an authority.

5.2 Layer 2 security boundary
- Layer 2 owns the first trusted normalization zone.
- It canonicalizes data, validates file/record limits, rejects malformed source payloads, protects the keystore-backed secret path, and verifies artifact authenticity.
- This is the layer that converts untrusted raw input into trusted data structures for persistence or domain evaluation.
- It must reject partial or malformed results rather than silently trying to recover.

5.3 Layer 3 security boundary
- Persistence stores validated state but does not decide whether data is trustworthy.
- Room/SQLCipher is the authoritative storage of active security policy and source configuration.
- Repositories may expose persistence operations but may not silently reinterpret policy semantics.
- Database state is authoritative; derived caches and Bloom indexes are derived-only artifacts.

5.4 Layer 4 security boundary
- Layer 4 is the domain authority for risk, precedence, and decision semantics.
- It owns: decision tiers, risk evaluation, precedence, explicit failure semantics, and immutable consequence contracts.
- Domain outputs must be explicit, typed, and testable.
- The domain must never rely on UI state, raw Android callbacks, or framework-only interpretation to determine a security decision.

5.5 Layer 5 security boundary
- Layer 5 owns cross-repository and cross-source mutation orchestration.
- It is the allowed place for source synchronization, snapshot activation, and authoritative mutation flows.
- `SecurityRuleRepository` is the single approved mutation boundary for security-affecting rule changes.
- This layer coordinates atomic commit behavior and last-known-good preservation.

5.6 Layer 6 security boundary
- Layer 6 converts trusted domain/application results into screen state and user actions.
- It may render state, expose flows, and prepare requests, but it may not reinterpret domain consequences.
- Presentation must not mutate authoritative security state directly or bypass the application service boundary.

5.7 Layer 7 security boundary
- Layer 7 is the display layer only.
- It may collect user input, render state, show notifications and screens, and trigger navigation.
- It may not decide whether a call is allowed, blocked, or escalated.
- It may not directly persist security-critical records.

6. Security invariants

The following invariants are binding and must hold for every change:

INV-001 — Authoritative security state
The encrypted Room database is the authoritative source of active security policy. Bloom filters, caches, derived indexes, and in-memory state are not authoritative.

INV-002 — Last-known-good preservation
When source activation fails, the previous good state remains authoritative and the failed attempt is recorded without replacing the last known good dataset.

INV-003 — Explicit security failure
A security failure is a distinct typed outcome and is never silently converted into an allow path.

INV-004 — Untrusted input is never trusted
Platform and external input must be shape-validated and normalized before any security-affecting use.

INV-005 — Deterministic decisions
The same normalized inputs and state must produce the same decision outcome.

INV-006 — Decision consequences are explicit
Every domain decision must carry the required consequences: audit, review card, notification, haptic policy, and call action semantics.

INV-007 — No raw PII in operational logs
Operational diagnostics and app logs must never expose raw phone numbers or other unnecessary user-identifying data.

INV-008 — Protected source lifecycle
Protected source types and deletion semantics are enforced and cannot be bypassed.

INV-009 — Platform edge to application boundary
The service/broadcast/worker edge must route through the application boundary rather than directly mutating persistence or security state.

INV-010 — Mandatory CI and release gates
Branch-level security and architecture verification are mandatory; no release decision may pass without the required gates.

7. Implementation rules

- No direct DAO access from UI or Layer 1 code.
- No direct database construction outside the secure path.
- No new screen may be introduced without a dedicated ViewModel.
- No new architecture or security policy may be implemented without reference to this document.
- No feature work may be started against a security-sensitive branch unless the relevant security boundary and layer ownership are checked against this contract.

8. Enforcement

This contract is enforced by repository process and CI checks.
- `check-architecture-drift.sh` remains the required architecture gate for structural drift.
- Security-sensitive changes must be traced to this document in the commit or session record.
- Future changes must reference this doc in the PR description, session notes, or project ledger update.
- If a change conflicts with this architecture, the conflict must be documented and resolved explicitly before merge.

9. Final status

This file is the authoritative final target architecture for SignalGate Pulse. It supersedes earlier phase-labeled drafts for architectural intent and governs all future implementation.

Any future feature, refactor, or bug fix must be designed to fit this architecture and must not introduce a competing ownership model.

"The target architecture is not optional. It is the security boundary model the app is designed to respect."

10. Resolution of stale known-violation entries

The following items were previously recorded as open in older contract drafts, but were later validated against live code and are now resolved:

- `PermissionSettingsScreen` was wired into `NavGraph.kt` and is reachable from the dashboard permission-health path. It is not an orphan or unreachable screen.
- `TelemetryViewModel` was intentionally removed; no live implementation or Koin binding remains. Remaining references are historical comments only.

These are no longer active violations and must not be treated as open issues for this branch.
