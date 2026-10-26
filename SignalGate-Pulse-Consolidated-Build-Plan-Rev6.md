# SignalGate Pulse — Current Source of Truth

**Read-only roll-up of the current `consumer-v1` checkout**

- Audited SHA: `fda4c83bd2c24634f7876846398c3df7c51ed6ce` [VERIFIED-CODE]
- Source: `https://github.com/avignonbo-jpg/Signal-Gate-Pulse.git`, branch `consumer-v1` [VERIFIED-CODE]
- Checkout: `/home/ubuntu/audit_20261025_signal_gate_pulse` [VERIFIED-CODE]
- Base audit set: `/home/ubuntu/SignalGatePulseAudit_20261025_FINAL2` [VERIFIED-CODE]
- Adoption status: this roll-up was generated outside the repository and is now repository-tracked as `SignalGate-Pulse-Consolidated-Build-Plan-Rev6.md`; the audit content describes the checkout at the recorded SHA. [VERIFIED-CODE]

## Contents

The following sections combine the 16 current audit reports and the requested amendments. Evidence tags are retained.

# Kotlin Inventory

Every tracked Kotlin file is listed from `git ls-files '*.kt'`; declaration identities come from a source regex; purposes use the first usable KDoc sentence, otherwise the required `(inferred)` wording. [VERIFIED-CODE]

| File | Package | Class/object/top-level declaration | Exact one-sentence purpose |
|---|---|---|---|
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt` | `com.signalgate.pulse` | class DrawerNavigationBackStackTest (18) | “(inferred from declarations and call sites)” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:1`) [UNKNOWN] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/GrayZoneReviewabilityTest.kt` | `com.signalgate.pulse` | class GrayZoneReviewabilityTest (45) | “Phase 1.3 — complete gray-zone reviewability proof.” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/GrayZoneReviewabilityTest.kt:34`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/StartupTimingTest.kt` | `com.signalgate.pulse` | class StartupTimingTest (29) | “Regression guard for the MainApplication.onCreate() runBlocking DB-init cold” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/StartupTimingTest.kt:13`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/MigrationTest.kt` | `com.signalgate.pulse.database` | class MigrationTest (40) | “MigrationTest — validates Room schema migrations with MigrationTestHelper.” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/database/MigrationTest.kt:15`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/SourceDeletionCascadeTest.kt` | `com.signalgate.pulse.database` | class SourceDeletionCascadeTest (41) | “SourceDeletionCascadeTest — confirms the FK CASCADE declared on” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/database/SourceDeletionCascadeTest.kt:18`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/BloomAuthoritativeDecisionTest.kt` | `com.signalgate.pulse.database.repositories` | class BloomAuthoritativeDecisionTest (50) | “BloomAuthoritativeDecisionTest — Phase 0.2 (Architecture-Contract-v3-DRAFT.md” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/BloomAuthoritativeDecisionTest.kt:19`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/BloomPostCommitOrderingTest.kt` | `com.signalgate.pulse.database.repositories` | class BloomPostCommitOrderingTest (23) | “Verifies the write-side INV-001 boundary: Room is authoritative and Bloom is” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/BloomPostCommitOrderingTest.kt:18`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/DecisionMatrixRepositoryTest.kt` | `com.signalgate.pulse.database.repositories` | class DecisionMatrixRepositoryTest (25) | “Phase 1.1 repository decision-matrix coverage.” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/DecisionMatrixRepositoryTest.kt:18`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/logic/SourceActivationTransactionTest.kt` | `com.signalgate.pulse.logic` | class SourceActivationTransactionTest (35) | “Phase 0.4 / INV-002 regression coverage.” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/logic/SourceActivationTransactionTest.kt:28`) [CLAIMED-IN-DOC] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt` | `com.signalgate.pulse.security` | class SecurityUtilsInstrumentedTest (44) | “SecurityUtilsInstrumentedTest — instrumented tests for SecurityUtils.getDatabasePassphrase().” (`android/app/src/androidTest/kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt:20`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/CallActionReceiver.kt` | `com.signalgate.pulse` | class CallActionReceiver (40) | “Handles notification action buttons fired from CallScreeningService's” (`android/app/src/main/java/com/signalgate/pulse/CallActionReceiver.kt:17`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/CallInfo.kt` | `com.signalgate.pulse` | class CallTier (28), class CallInfo (54) | “CallTier represents which tier of the call handling architecture matched” (`android/app/src/main/java/com/signalgate/pulse/CallInfo.kt:8`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/MainActivity.kt` | `com.signalgate.pulse` | class MainActivity (22) | “Drawer navigation must not pop to the NavHost's startup route: that route is” (`android/app/src/main/java/com/signalgate/pulse/MainActivity.kt:103`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/MainApplication.kt` | `com.signalgate.pulse` | class MainApplication (56), object AppReadiness (165), class ReleaseTree (174) | “MainApplication — app entry point.” (`android/app/src/main/java/com/signalgate/pulse/MainApplication.kt:26`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/PhoneStateReceiver.kt` | `com.signalgate.pulse` | class PhoneStateReceiver (37) | “PhoneStateReceiver — INTENTIONALLY INERT (2026-06).” (`android/app/src/main/java/com/signalgate/pulse/PhoneStateReceiver.kt:10`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt` | `com.signalgate.pulse` | class SignalGateCallScreeningService (38), class TelecomResponsePolicy (40) | “Thin Telecom entrypoint delegate. The injectable callbacks let JVM tests” (`android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:72`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/StartupDiagnostics.kt` | `com.signalgate.pulse` | object StartupDiagnostics (14), class Event (22) | “Debug startup timeline markers for real-device cold-process diagnosis.” (`android/app/src/main/java/com/signalgate/pulse/StartupDiagnostics.kt:7`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/TelecomRoleDiagnostics.kt` | `com.signalgate.pulse` | object TelecomRoleDiagnostics (17) | “Read-only diagnostics for Android Telecom role selection.” (`android/app/src/main/java/com/signalgate/pulse/TelecomRoleDiagnostics.kt:8`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/models/BenchmarkResult.kt` | `com.signalgate.pulse.data.models` | class BenchmarkResult (3) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/data/models/BenchmarkResult.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/data/models/CallLogItem.kt` | `com.signalgate.pulse.data.models` | class CallType (3), class CallLogItem (5) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/data/models/CallLogItem.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/data/models/PermissionStatus.kt` | `com.signalgate.pulse.data.models` | class PermissionStatus (3) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/data/models/PermissionStatus.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/data/models/ThreatSource.kt` | `com.signalgate.pulse.data.models` | class SourceStatus (3), class SourceType (4), class ThreatSource (6) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/data/models/ThreatSource.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/data/repositories/ContactsRepository.kt` | `com.signalgate.pulse.data.repositories` | class ContactsRepository (15) | “Repository boundary for the platform ContactsProvider.” (`android/app/src/main/java/com/signalgate/pulse/data/repositories/ContactsRepository.kt:10`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifier.kt` | `com.signalgate.pulse.data.security` | object ArtifactAuthenticityVerifier (20), class Manifest (26), interface Result (33), object Verified (34), class Failed (35), interface ResultOrManifest (125), class ManifestValue (126), class Failure (127) | “Verifies a detached ECDSA P-256 signature over externally sourced security” (`android/app/src/main/java/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifier.kt:13`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/BloomFilterEngine.kt` | `com.signalgate.pulse.data.security` | class BloomFilterEngine (5) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/data/security/BloomFilterEngine.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/PrecedenceEngine.kt` | `com.signalgate.pulse.data.security` | class PrecedenceEngine (5) | “Evaluates incoming phone threat matrices. Implements the absolute rule:” (`android/app/src/main/java/com/signalgate/pulse/data/security/PrecedenceEngine.kt:11`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/SanitizationEngine.kt` | `com.signalgate.pulse.data.security` | object SanitizationEngine (3) | “Sanitizes raw incoming phone string data to strip out SQL injection fragments or malformed buffer blocks.” (`android/app/src/main/java/com/signalgate/pulse/data/security/SanitizationEngine.kt:9`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/SecureCsvParser.kt` | `com.signalgate.pulse.data.security` | class SecureCsvParser (7), class CsvResourceLimitExceededException (133) | “Streams incoming large target datasets line-by-line without overloading the JVM heap.” (`android/app/src/main/java/com/signalgate/pulse/data/security/SecureCsvParser.kt:13`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/SnapshotSanityValidator.kt` | `com.signalgate.pulse.data.security` | class SnapshotSanityValidator (10), class Limits (13), class Candidate (24), interface Result (39), object Accepted (40), class Rejected (41) | “Validates a fully parsed source candidate before application-level snapshot” (`android/app/src/main/java/com/signalgate/pulse/data/security/SnapshotSanityValidator.kt:6`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/SourceAuthenticityTrustAnchor.kt` | `com.signalgate.pulse.data.security` | object SourceAuthenticityTrustAnchor (9) | “Public trust anchor for the FTC-via-owned-mirror source only.” (`android/app/src/main/java/com/signalgate/pulse/data/security/SourceAuthenticityTrustAnchor.kt:4`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/data/security/SourceRecordValidator.kt` | `com.signalgate.pulse.data.security` | object SourceRecordValidator (11) | “Validates and canonicalizes one externally supplied source record.” (`android/app/src/main/java/com/signalgate/pulse/data/security/SourceRecordValidator.kt:4`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/DatabaseInitializer.kt` | `com.signalgate.pulse.database` | object DatabaseInitializer (16) | “Idempotent first-install seeding for required SourceEntity rows.” (`android/app/src/main/java/com/signalgate/pulse/database/DatabaseInitializer.kt:10`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/SecureDatabase.kt` | `com.signalgate.pulse.database` | object SecureDatabase (14) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/database/SecureDatabase.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/database/SignalGateDatabase.kt` | `com.signalgate.pulse.database` | class SignalGateDatabase (54) | “SignalGateDatabase — Room database abstract class for SignalGate Pulse.” (`android/app/src/main/java/com/signalgate/pulse/database/SignalGateDatabase.kt:22`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/daos/DatabaseDAOs.kt` | `com.signalgate.pulse.database.daos` | interface SourceDao (21), class PhoneNumberPatternProjection (79), interface UnifiedEntryDao (88), interface CallLogDao (206), interface SettingDao (236), interface SyncHistoryDao (260), interface PendingCardDao (281) | “DAO for SourceEntity operations.” (`android/app/src/main/java/com/signalgate/pulse/database/daos/DatabaseDAOs.kt:18`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/entities/DatabaseEntities.kt` | `com.signalgate.pulse.database.entities` | class SourceType (15), class SourceEntity (36), class UnifiedEntryEntity (85), class CallLogEntry (113), class SettingEntry (139), class SyncHistoryEntry (166), class PendingCardEntity (198) | “Source types with application-level lifecycle semantics.” (`android/app/src/main/java/com/signalgate/pulse/database/entities/DatabaseEntities.kt:10`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/repositories/BlocklistRepository.kt` | `com.signalgate.pulse.database.repositories` | class BlocklistRepository (26) | “BlocklistRepository — DEPRECATED, PENDING MIGRATION.” (`android/app/src/main/java/com/signalgate/pulse/database/repositories/BlocklistRepository.kt:7`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/repositories/CallLogRepository.kt` | `com.signalgate.pulse.database.repositories` | class CallLogRepository (8) | “Security fix (audit finding): this is the write chokepoint for CallLogEntry,” (`android/app/src/main/java/com/signalgate/pulse/database/repositories/CallLogRepository.kt:12`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/repositories/DataSourceRepository.kt` | `com.signalgate.pulse.database.repositories` | class DataSourceRepository (66), class CallDecision (451), class ProtectedSourceDeletionException (466) | “DataSourceRepository — single source of truth for data source and entry operations.” (`android/app/src/main/java/com/signalgate/pulse/database/repositories/DataSourceRepository.kt:15`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/repositories/PendingCardRepository.kt` | `com.signalgate.pulse.database.repositories` | class PendingCardRepository (12) | “PendingCardRepository (Phase 1.4) — Clean repository wrapper per §5.5.” (`android/app/src/main/java/com/signalgate/pulse/database/repositories/PendingCardRepository.kt:9`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/repositories/SettingKeys.kt` | `com.signalgate.pulse.database.repositories` | object SettingKeys (28), class HeuristicsMode (62) | “SettingKeys — centralizes the SettingEntry key strings for values migrated” (`android/app/src/main/java/com/signalgate/pulse/database/repositories/SettingKeys.kt:14`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/repositories/SettingRepository.kt` | `com.signalgate.pulse.database.repositories` | class SettingRepository (16) | “SettingRepository — thin repository wrapper over SettingDao (Contract §5.5).” (`android/app/src/main/java/com/signalgate/pulse/database/repositories/SettingRepository.kt:8`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/database/repositories/SyncHistoryRepository.kt` | `com.signalgate.pulse.database.repositories` | class SyncHistoryRepository (10) | “SyncHistoryRepository wraps [SyncHistoryDao] and provides the production data path” (`android/app/src/main/java/com/signalgate/pulse/database/repositories/SyncHistoryRepository.kt:7`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt` | `com.signalgate.pulse.di` |  top-level initializeDatabase (301),  top-level rehydrateBloomFiltersInBackground (333) | “AppModule — Koin DI bindings per Architecture Contract §2.” (`android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:48`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/di/KoinWorkerFactory.kt` | `com.signalgate.pulse.di` | class KoinWorkerFactory (15) | “KoinWorkerFactory allows WorkManager to create worker instances using Koin's” (`android/app/src/main/java/com/signalgate/pulse/di/KoinWorkerFactory.kt:11`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/diagnostics/AppDiagnosticsTree.kt` | `com.signalgate.pulse.diagnostics` | class AppLogEntry (18), class AppDiagnosticsTree (48), object AppLogCapture (132) | “Captures in-process Timber events in a bounded, observable buffer and mirrors each” (`android/app/src/main/java/com/signalgate/pulse/diagnostics/AppDiagnosticsTree.kt:44`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/CallRiskEvaluator.kt` | `com.signalgate.pulse.logic` | object CallRiskEvaluator (29), class RiskEvaluation (124) | “CallRiskEvaluator computes a composite risk score for an incoming call.” (`android/app/src/main/java/com/signalgate/pulse/logic/CallRiskEvaluator.kt:9`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/CallScreeningEngine.kt` | `com.signalgate.pulse.logic` | class CallScreeningEngine (45) | “CallScreeningEngine implements the five-tier Priority Hierarchy plus the explicit” (`android/app/src/main/java/com/signalgate/pulse/logic/CallScreeningEngine.kt:13`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/DataSyncEngine.kt` | `com.signalgate.pulse.logic` | class DataSyncEngine (73), class ParserLimits (83), class SharedStringsHandler (523), class XlsxParseException (711), class RowLimitExceededException (718), class SharedStringsLimitExceededException (725), class ExpandedSharedStringsLimitExceededException (727), class CellLengthLimitExceededException (729), interface XlsxBatchMessage (731), class Batch (732), class Failed (733), object Complete (734) | “DataSyncEngine — Memory-safe XLSX/CSV parsing per Architecture Contract §4 L2.” (`android/app/src/main/java/com/signalgate/pulse/logic/DataSyncEngine.kt:22`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt` | `com.signalgate.pulse.logic` | class ReliableSourceManager (81), class FetchStrategy (158), class FederalSource (160), class SyncResult (169), class FetchedSnapshot (176), class CountingInputStream (487), class RawBody (506) | “ReliableSourceManager — Phase 3.1: verified production endpoints, resilience.” (`android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:29`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/ScreeningAction.kt` | `com.signalgate.pulse.logic` | class ScreeningAction (28) | “ScreeningAction is the domain-level outcome of [CallScreeningEngine.screenCall],” (`android/app/src/main/java/com/signalgate/pulse/logic/ScreeningAction.kt:4`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/ScreeningDecision.kt` | `com.signalgate.pulse.logic` | class NotificationPolicy (11), class HapticPolicy (21), class ScreeningDecision (36) | “Policy-semantic notification consequence. This names what the decision calls” (`android/app/src/main/java/com/signalgate/pulse/logic/ScreeningDecision.kt:8`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/SecurityRuleRepository.kt` | `com.signalgate.pulse.logic` | class SecurityRuleRepository (48), interface SnapshotActivationResult (355), object Accepted (356), class Failed (357) | “SecurityRuleRepository — Layer 5 (Application). The single authoritative” (`android/app/src/main/java/com/signalgate/pulse/logic/SecurityRuleRepository.kt:26`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/SourceLifecycleState.kt` | `com.signalgate.pulse.logic` | class SourceLifecycleState (10), class SnapshotMetadata (23) | “Persisted lifecycle states for externally managed security-data sources.” (`android/app/src/main/java/com/signalgate/pulse/logic/SourceLifecycleState.kt:4`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/logic/SourceSyncUseCase.kt` | `com.signalgate.pulse.logic` | class SourceSyncUseCase (15), class BatchResult (21) | “Phase 0.4 application boundary for source synchronization.” (`android/app/src/main/java/com/signalgate/pulse/logic/SourceSyncUseCase.kt:8`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/security/DatabaseResetEvent.kt` | `com.signalgate.pulse.security` | object DatabaseResetEvent (16) | “Signals to the UI layer that the local database was just reset because the” (`android/app/src/main/java/com/signalgate/pulse/security/DatabaseResetEvent.kt:8`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/security/KeystoreInvalidatedException.kt` | `com.signalgate.pulse.security` | class KeystoreInvalidatedException (14) | “Thrown when a wrapped database passphrase exists in SharedPreferences but cannot” (`android/app/src/main/java/com/signalgate/pulse/security/KeystoreInvalidatedException.kt:4`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/security/SecurityUtils.kt` | `com.signalgate.pulse.security` | object SecurityUtils (20) | “Returns the SQLCipher passphrase for the secure database, protected at rest by a” (`android/app/src/main/java/com/signalgate/pulse/security/SecurityUtils.kt:60`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/BlockedNumbersViewModel.kt` | `com.signalgate.pulse.ui` | class BlockedNumbersViewModel (29), class Filter (31) | “BlockedNumbersViewModel — backs BlockAllowListScreen (Phase 4.2).” (`android/app/src/main/java/com/signalgate/pulse/ui/BlockedNumbersViewModel.kt:18`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/RecentCallsViewModel.kt` | `com.signalgate.pulse.ui` | class RecentCallsViewModel (25) | “Uses [CallLogRepository] and [DataSourceRepository] as the production data path.” (`android/app/src/main/java/com/signalgate/pulse/ui/RecentCallsViewModel.kt:22`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/components/AdvancedGlassCard.kt` | `com.signalgate.pulse.ui.components` |  top-level AdvancedGlassCard (17) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/components/AdvancedGlassCard.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/components/GlassCard.kt` | `com.signalgate.pulse.ui.components` |  top-level GlassCard (18) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/components/GlassCard.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/components/GlassmorphicCard.kt` | `com.signalgate.pulse.ui.components` |  top-level GlassmorphicCard (17) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/components/GlassmorphicCard.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/components/GlassmorphicDrawerContent.kt` | `com.signalgate.pulse.ui.components` |  top-level GlassmorphicDrawerContent (21) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/components/GlassmorphicDrawerContent.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/components/NeonButton.kt` | `com.signalgate.pulse.ui.components` | class NeonButtonStyle (17) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/components/NeonButton.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/components/ShieldStatusGlow.kt` | `com.signalgate.pulse.ui.components` |  top-level ShieldStatusGlow (20) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/components/ShieldStatusGlow.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/components/SourceIcon.kt` | `com.signalgate.pulse.ui.components` |  top-level SourceIcon (19) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/components/SourceIcon.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/dashboard/DashboardViewModel.kt` | `com.signalgate.pulse.ui.dashboard` | class DashboardViewModel (32) | “DashboardViewModel — Main dashboard state management.” (`android/app/src/main/java/com/signalgate/pulse/ui/dashboard/DashboardViewModel.kt:19`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/digest/DigestScreen.kt` | `com.signalgate.pulse.ui.digest` |  top-level DigestScreen (47) | “DigestScreen — the blocked call review queue (Screen.Digest).” (`android/app/src/main/java/com/signalgate/pulse/ui/digest/DigestScreen.kt:27`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/digest/PendingCardViewModel.kt` | `com.signalgate.pulse.ui.digest` | class PendingCardViewModel (31) | “ViewModel backing Screen.Digest — the blocked call review queue.” (`android/app/src/main/java/com/signalgate/pulse/ui/digest/PendingCardViewModel.kt:12`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt` | `com.signalgate.pulse.ui.navigation` |  top-level SignalGateNavGraph (42) | “Resolves the app's first visible destination only after persistent completion” (`android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:31`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/navigation/Screen.kt` | `com.signalgate.pulse.ui.navigation` | class Screen (8), object Dashboard (9), object Sources (10), object CallLog (11), object BlockAllowList (12), object Settings (13), object Logcat (14), object Onboarding (15), object PermissionSettings (16), object Digest (31) | “Blocked call digest — swipeable card list backed by PendingCardEntity queue.” (`android/app/src/main/java/com/signalgate/pulse/ui/navigation/Screen.kt:19`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/notifications/NotificationChannelManager.kt` | `com.signalgate.pulse.ui.notifications` | object NotificationChannelManager (25) | “NotificationChannelManager — Phase 4.8.” (`android/app/src/main/java/com/signalgate/pulse/ui/notifications/NotificationChannelManager.kt:9`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseHapticsController.kt` | `com.signalgate.pulse.ui.notifications` | class PulseHapticsController (40) | “Phase 2.1 — sole haptic dispatcher for screening decisions.” (`android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseHapticsController.kt:10`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseTriggerLimiter.kt` | `com.signalgate.pulse.ui.notifications` | class PulseTriggerLimiter (43) | “Phase 2.2 — UX rate limiter for notification and haptic dispatch.” (`android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseTriggerLimiter.kt:9`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseVibration.kt` | `com.signalgate.pulse.ui.notifications` | object PulseVibration (29) | “Vibration pattern definitions for Phase 2.1.” (`android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseVibration.kt:7`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingViewModel.kt` | `com.signalgate.pulse.ui.onboarding` | class PermissionItem (27), class OnboardingViewModel (36) | “Step 3 protection level. Replaces the old dead `riskThreshold: String`” (`android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingViewModel.kt:111`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt` | `com.signalgate.pulse.ui.onboarding` | class CompletionAction (1287) | “OnboardingWizardScreen — Multi-step setup flow for new users.” (`android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:54`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/BlockAllowListScreen.kt` | `com.signalgate.pulse.ui.screens` |  top-level BlockAllowListScreen (35) | “BlockAllowListScreen — Phase 4.2 (Contract §4 L7).” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/BlockAllowListScreen.kt:23`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/CallLogScreen.kt` | `com.signalgate.pulse.ui.screens` |  top-level CallLogScreen (20) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/CallLogScreen.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt` | `com.signalgate.pulse.ui.screens` |  top-level ConsumerDashboardScreen (61) | “ConsumerDashboard — the default home screen for Pulse users.” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt:41`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/LogcatViewerScreen.kt` | `com.signalgate.pulse.ui.screens` |  top-level LogcatViewerScreen (49) | “Debug-only in-app Logcat viewer. The [BuildConfig.DEBUG] guard keeps this screen” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/LogcatViewerScreen.kt:41`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/PermissionSettingsScreen.kt` | `com.signalgate.pulse.ui.screens` |  top-level PermissionSettingsScreen (52) | “Permission Health Check screen — Step 1.10.” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/PermissionSettingsScreen.kt:34`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsScreens.kt` | `com.signalgate.pulse.ui.screens` |  top-level SettingsScreen (43),  top-level ColorSlider (218) | “SettingsScreen — Application configuration and customization.” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsScreens.kt:16`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsViewModel.kt` | `com.signalgate.pulse.ui.screens` | class SettingsViewModel (35) | “SettingsViewModel — owns shield-color persistence for SettingsScreen.” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsViewModel.kt:24`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/SourceRowControls.kt` | `com.signalgate.pulse.ui.screens` | class SourceRowControlsState (6), object SourceRowControls (15) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SourceRowControls.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt` | `com.signalgate.pulse.ui.screens` |  top-level SourcesScreen (33) | “SourcesScreen — Phase 3.4 (Contract §4 L7).” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt:21`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesViewModel.kt` | `com.signalgate.pulse.ui.screens` | class SourcesViewModel (33) | “SourcesViewModel — backs SourcesScreen (Contract §4 L7, Phase 3.3/3.4).” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesViewModel.kt:23`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/theme/Color.kt` | `com.signalgate.pulse.ui.theme` | UNKNOWN | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/theme/Color.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/theme/Effects.kt` | `com.signalgate.pulse.ui.theme` | object SignalGateGlow (45) | “(inferred from declarations and call sites)” (`android/app/src/main/java/com/signalgate/pulse/ui/theme/Effects.kt:1`) [UNKNOWN] |
| `android/app/src/main/java/com/signalgate/pulse/ui/theme/SignalGateTheme.kt` | `com.signalgate.pulse.ui.theme` |  top-level SignalGateTheme (104) | “Custom Colors for SignalGate Pulse” (`android/app/src/main/java/com/signalgate/pulse/ui/theme/SignalGateTheme.kt:16`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/ContactsViewModel.kt` | `com.signalgate.pulse.ui.viewmodels` | class ContactItem (14), class ContactsViewModel (21) | “Reads the Contacts Allow List sourceId from SettingEntry (seeded by DatabaseInitializer)” (`android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/ContactsViewModel.kt:92`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/LogcatViewModel.kt` | `com.signalgate.pulse.ui.viewmodels` | class LogcatViewModel (19) | “Exposes both diagnostic sources at once:” (`android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/LogcatViewModel.kt:15`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/utils/DateUtils.kt` | `com.signalgate.pulse.utils` |  top-level Long (17) | “Formats an epoch-millis timestamp for display in source health rows, sync” (`android/app/src/main/java/com/signalgate/pulse/utils/DateUtils.kt:9`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/utils/PhoneNumberUtils.kt` | `com.signalgate.pulse.utils` | object PhoneNumberUtils (3) | “Formats a phone number for display (e.g., +1234567890 -> (123) 456-7890)” (`android/app/src/main/java/com/signalgate/pulse/utils/PhoneNumberUtils.kt:6`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt` | `com.signalgate.pulse.workers` | class CommunitySyncWorker (30) | “CommunitySyncWorker — Phase 2.5 (Contract §5.3).” (`android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt:23`) [CLAIMED-IN-DOC] |
| `android/app/src/main/java/com/signalgate/pulse/workers/SyncBootReceiver.kt` | `com.signalgate.pulse.workers` | class SyncBootReceiver (21) | “SyncBootReceiver — Phase 4.11.” (`android/app/src/main/java/com/signalgate/pulse/workers/SyncBootReceiver.kt:9`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/CallActionReceiverBehaviorTest.kt` | `com.signalgate.pulse` | class CallActionReceiverBehaviorTest (20) | “Phase 0.7 / 0.8 regression coverage.” (`android/app/src/test/kotlin/com/signalgate/pulse/CallActionReceiverBehaviorTest.kt:14`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/NotificationPrivacyTest.kt` | `com.signalgate.pulse` | class NotificationPrivacyTest (16) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/NotificationPrivacyTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceCallResponseMappingTest.kt` | `com.signalgate.pulse` | class ScreeningServiceCallResponseMappingTest (6) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceCallResponseMappingTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceDeadlineTest.kt` | `com.signalgate.pulse` | class ScreeningServiceDeadlineTest (23) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceDeadlineTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceEdgeExecutionTest.kt` | `com.signalgate.pulse` | class ScreeningServiceEdgeExecutionTest (61) | “Phase 1.2 — Edge execution tests for SignalGateCallScreeningService.” (`android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceEdgeExecutionTest.kt:27`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceTimingBudgetTest.kt` | `com.signalgate.pulse` | class ScreeningServiceTimingBudgetTest (18) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceTimingBudgetTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifierTest.kt` | `com.signalgate.pulse.data.security` | class ArtifactAuthenticityVerifierTest (13) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifierTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SecureCsvParserLimitTest.kt` | `com.signalgate.pulse.data.security` | class SecureCsvParserLimitTest (13), class GeneratedCsvInputStream (48) | “Phase 0.5 regression coverage: a CSV candidate that exceeds the hard valid-row” (`android/app/src/test/kotlin/com/signalgate/pulse/data/security/SecureCsvParserLimitTest.kt:10`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SnapshotSanityValidatorTest.kt` | `com.signalgate.pulse.data.security` | class SnapshotSanityValidatorTest (7) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/data/security/SnapshotSanityValidatorTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SourceRecordValidatorTest.kt` | `com.signalgate.pulse.data.security` | class SourceRecordValidatorTest (8) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/data/security/SourceRecordValidatorTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/database/DatabaseInitializerTest.kt` | `com.signalgate.pulse.database` | class DatabaseInitializerTest (16) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/database/DatabaseInitializerTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryDeletionTest.kt` | `com.signalgate.pulse.database.repositories` | class DataSourceRepositoryDeletionTest (33) | “Phase 0.3 (Security Control-Plane Integrity — source lifecycle semantics,” (`android/app/src/test/kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryDeletionTest.kt:18`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryPersistedDeletionTest.kt` | `com.signalgate.pulse.database.repositories` | class DataSourceRepositoryPersistedDeletionTest (22) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryPersistedDeletionTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt` | `com.signalgate.pulse.di` | class KoinModuleTest (64) | “KoinModuleTest — verifies the full Koin DI graph resolves without error.” (`android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:36`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/diagnostics/AppDiagnosticsTreeTest.kt` | `com.signalgate.pulse.diagnostics` | class AppDiagnosticsTreeTest (7) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/diagnostics/AppDiagnosticsTreeTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineDecisionMatrixTest.kt` | `com.signalgate.pulse.logic` | class CallScreeningEngineDecisionMatrixTest (21) | “Phase 1.1 deterministic decision-matrix coverage.” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineDecisionMatrixTest.kt:15`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineSecurityFailureTest.kt` | `com.signalgate.pulse.logic` | class CallScreeningEngineSecurityFailureTest (20) | “Phase 0.6 / INV-003 regression coverage.” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineSecurityFailureTest.kt:14`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/DataSyncEngineXlsxLimitTest.kt` | `com.signalgate.pulse.logic` | class DataSyncEngineXlsxLimitTest (19) | “Phase 0.5 / 0.8 regression coverage.” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/DataSyncEngineXlsxLimitTest.kt:14`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerDisabledSourceTest.kt` | `com.signalgate.pulse.logic` | class ReliableSourceManagerDisabledSourceTest (17) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerDisabledSourceTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerEnsureFederalRowsTest.kt` | `com.signalgate.pulse.logic` | class ReliableSourceManagerEnsureFederalRowsTest (34) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerEnsureFederalRowsTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerPolicyTest.kt` | `com.signalgate.pulse.logic` | class ReliableSourceManagerPolicyTest (8) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerPolicyTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt` | `com.signalgate.pulse.logic` | class ReliableSourceManagerRowBranchTest (22) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerSyncStateTest.kt` | `com.signalgate.pulse.logic` | class ReliableSourceManagerSyncStateTest (41), class Fixture (207), class RequestGate (212), class ControlledTransport (218) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerSyncStateTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ScreeningDecisionConsequencesTest.kt` | `com.signalgate.pulse.logic` | class ScreeningDecisionConsequencesTest (16) | “Phase 1.2 consequence contract tests.” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/ScreeningDecisionConsequencesTest.kt:10`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryContactImportBoundaryTest.kt` | `com.signalgate.pulse.logic` | class SecurityRuleRepositoryContactImportBoundaryTest (21) | “Phase 0.1 regression coverage for the contacts-import mutation boundary.” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryContactImportBoundaryTest.kt:16`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryMutationBoundaryTest.kt` | `com.signalgate.pulse.logic` | class SecurityRuleRepositoryMutationBoundaryTest (23) | “Phase 0.1 / 0.8 regression coverage.” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryMutationBoundaryTest.kt:17`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/SourceSyncUseCaseTest.kt` | `com.signalgate.pulse.logic` | class SourceSyncUseCaseTest (17) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/logic/SourceSyncUseCaseTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/security/SecurityUtilsTest.kt` | `com.signalgate.pulse.security` | class SecurityUtilsTest (21) | “SecurityUtilsTest — unit tests for SecurityUtils.” (`android/app/src/test/kotlin/com/signalgate/pulse/security/SecurityUtilsTest.kt:8`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/navigation/NavGraphRoutePolicyTest.kt` | `com.signalgate.pulse.ui.navigation` | class NavGraphRoutePolicyTest (7) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ui/navigation/NavGraphRoutePolicyTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/notifications/PulseTriggerLimiterTest.kt` | `com.signalgate.pulse.ui.notifications` | class PulseTriggerLimiterTest (10) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ui/notifications/PulseTriggerLimiterTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/onboarding/OnboardingViewModelEulaTest.kt` | `com.signalgate.pulse.ui.onboarding` | class FakeSettingDao (27), class OnboardingViewModelEulaTest (64) | “Hand-rolled fake instead of a Mockito mock, deliberately. SettingRepository” (`android/app/src/test/kotlin/com/signalgate/pulse/ui/onboarding/OnboardingViewModelEulaTest.kt:19`) [CLAIMED-IN-DOC] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/screens/SourceRowControlsTest.kt` | `com.signalgate.pulse.ui.screens` | class SourceRowControlsTest (7) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ui/screens/SourceRowControlsTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/screens/SourcesViewModelDeletionTest.kt` | `com.signalgate.pulse.ui.screens` | class MainDispatcherRule (34), class SourcesViewModelDeletionTest (47) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ui/screens/SourcesViewModelDeletionTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/viewmodels/ContactsViewModelTest.kt` | `com.signalgate.pulse.ui.viewmodels` | class ContactsViewModelTest (28) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/ui/viewmodels/ContactsViewModelTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/utils/PhoneNumberUtilsTest.kt` | `com.signalgate.pulse.utils` | class PhoneNumberUtilsTest (6) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/utils/PhoneNumberUtilsTest.kt:1`) [UNKNOWN] |
| `android/app/src/test/kotlin/com/signalgate/pulse/workers/CommunitySyncWorkerTest.kt` | `com.signalgate.pulse.workers` | class CommunitySyncWorkerTest (27) | “(inferred from declarations and call sites)” (`android/app/src/test/kotlin/com/signalgate/pulse/workers/CommunitySyncWorkerTest.kt:1`) [UNKNOWN] |

**133 Kotlin files listed / 133 found by `git ls-files '*.kt'`.**

---

# Dependencies

Gradle dependency resolution was run for `pulseDebugRuntimeClasspath`, `pulseDebugUnitTestRuntimeClasspath`, and `pulseDebugAndroidTestRuntimeClasspath`; each ended `BUILD SUCCESSFUL` in `/home/ubuntu/audit_20261025_gradle_dependencies.txt` [VERIFIED-CI]. Direct declarations come from `android/app/build.gradle`; resolved versions are selected from the generated dependency report.

| Configuration | Coordinate | Declared version | Resolved version | Evidence |
|---|---|---|---|---|
| `implementation` | `io.insert-koin:koin-android` | `3.5.6` | `3.5.6` | `android/app/build.gradle:156` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `io.insert-koin:koin-androidx-compose` | `3.5.6` | `3.5.6` | `android/app/build.gradle:157` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `fileTree(libs/*.jar)` | `fileTree(libs/*.jar)` | `UNKNOWN` | `android/app/build.gradle:158` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.core:core-ktx` | `1.13.1` | `1.13.1` | `android/app/build.gradle:161` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.appcompat:appcompat` | `1.7.0` | `1.7.0` | `android/app/build.gradle:162` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `com.google.android.material:material` | `1.12.0` | `1.12.0` | `android/app/build.gradle:163` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.core:core-splashscreen` | `1.0.1` | `1.0.1` | `android/app/build.gradle:169` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `com.squareup.okhttp3:okhttp` | `4.12.0` | `4.12.0` | `android/app/build.gradle:172` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `com.jakewharton.timber:timber` | `5.0.1` | `5.0.1` | `android/app/build.gradle:176` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.room:room-runtime` | `2.6.1` | `2.6.1` | `android/app/build.gradle:180` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.room:room-ktx` | `2.6.1` | `2.6.1` | `android/app/build.gradle:181` [VERIFIED-CODE/VERIFIED-CI] |
| `ksp` | `androidx.room:room-compiler` | `2.6.1` | `UNKNOWN` | `android/app/build.gradle:182` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `net.zetetic:sqlcipher-android` | `4.5.5` | `4.5.5` | `android/app/build.gradle:183` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.sqlite:sqlite` | `2.4.0` | `2.4.0` | `android/app/build.gradle:184` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.compose:compose-bom` | `2024.09.00 (BOM)` | `2024.09.00` | `android/app/build.gradle:188` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.compose:compose-bom` | `2024.09.00 (BOM)` | `2024.09.00` | `android/app/build.gradle:189` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.compose.ui:ui` | `UNKNOWN` | `1.5.1` | `android/app/build.gradle:190` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.compose.material3:material3` | `UNKNOWN` | `UNKNOWN` | `android/app/build.gradle:191` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.compose.material:material-icons-extended` | `UNKNOWN` | `UNKNOWN` | `android/app/build.gradle:192` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.compose.ui:ui-tooling-preview` | `UNKNOWN` | `UNKNOWN` | `android/app/build.gradle:193` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.activity:activity-compose` | `1.9.1` | `1.9.1` | `android/app/build.gradle:194` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.4` | `2.8.4` | `android/app/build.gradle:195` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.4` | `2.8.4` | `android/app/build.gradle:196` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.compose.ui:ui-test-junit4` | `UNKNOWN` | `UNKNOWN` | `android/app/build.gradle:199` [VERIFIED-CODE/VERIFIED-CI] |
| `debugImplementation` | `androidx.compose.ui:ui-test-manifest` | `UNKNOWN` | `UNKNOWN` | `android/app/build.gradle:200` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.8.1` | `1.8.1` | `android/app/build.gradle:203` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.work:work-runtime-ktx` | `2.9.1` | `2.9.1` | `android/app/build.gradle:207` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.navigation:navigation-fragment-ktx` | `2.7.7` | `2.7.7` | `android/app/build.gradle:210` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.navigation:navigation-ui-ktx` | `2.7.7` | `2.7.7` | `android/app/build.gradle:211` [VERIFIED-CODE/VERIFIED-CI] |
| `implementation` | `androidx.navigation:navigation-compose` | `2.7.7` | `2.7.7` | `android/app/build.gradle:212` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `junit:junit` | `4.13.2` | `4.13.2` | `android/app/build.gradle:215` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `org.mockito:mockito-core` | `5.11.0` | `5.11.0` | `android/app/build.gradle:216` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `org.mockito.kotlin:mockito-kotlin` | `5.3.1` | `5.3.1` | `android/app/build.gradle:217` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.8.1` | `1.8.1` | `android/app/build.gradle:218` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `io.insert-koin:koin-test` | `3.5.6` | `3.5.6` | `android/app/build.gradle:221` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `io.insert-koin:koin-test-junit4` | `3.5.6` | `3.5.6` | `android/app/build.gradle:222` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `io.insert-koin:koin-android-test` | `3.5.6` | `3.5.6` | `android/app/build.gradle:223` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `androidx.room:room-testing` | `2.6.1` | `2.6.1` | `android/app/build.gradle:226` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `androidx.test:core` | `1.5.0` | `1.5.0` | `android/app/build.gradle:227` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.test.ext:junit` | `1.1.5` | `1.1.5` | `android/app/build.gradle:230` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.test:core` | `1.5.0` | `1.5.0` | `android/app/build.gradle:231` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.test.espresso:espresso-core` | `3.5.1` | `3.5.1` | `android/app/build.gradle:232` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.test:runner` | `1.5.2` | `1.5.2` | `android/app/build.gradle:233` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.test:rules` | `1.5.0` | `1.5.0` | `android/app/build.gradle:234` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.navigation:navigation-testing` | `2.7.7` | `2.7.7` | `android/app/build.gradle:235` [VERIFIED-CODE/VERIFIED-CI] |
| `androidTestImplementation` | `androidx.room:room-testing` | `2.6.1` | `2.6.1` | `android/app/build.gradle:240` [VERIFIED-CODE/VERIFIED-CI] |
| `testImplementation` | `org.robolectric:robolectric` | `4.13` | `4.13` | `android/app/build.gradle:243` [VERIFIED-CODE/VERIFIED-CI] |

The `fileTree` declaration and BOM are recorded as non-coordinate declarations where applicable; Gradle-resolved values for entries absent from the report are `UNKNOWN`.

**47 dependency declaration lines listed / 47 found by `rg`-equivalent direct-declaration scan of `android/app/build.gradle`.**

---

# GitHub Workflows

### `.github/workflows/crash-diagnostic.yml`

- Triggers: workflow_dispatch: (`.github/workflows/crash-diagnostic.yml:4`) [VERIFIED-CI]
- Job-like keys: workflow_dispatch: (`.github/workflows/crash-diagnostic.yml:4`), crash-diagnostic: (`.github/workflows/crash-diagnostic.yml:10`), steps: (`.github/workflows/crash-diagnostic.yml:14`), with: (`.github/workflows/crash-diagnostic.yml:17`), with: (`.github/workflows/crash-diagnostic.yml:28`), with: (`.github/workflows/crash-diagnostic.yml:40`), with: (`.github/workflows/crash-diagnostic.yml:70`), with: (`.github/workflows/crash-diagnostic.yml:78`), with: (`.github/workflows/crash-diagnostic.yml:86`), with: (`.github/workflows/crash-diagnostic.yml:95`), crash-diagnostic-release: (`.github/workflows/crash-diagnostic.yml:101`), steps: (`.github/workflows/crash-diagnostic.yml:105`), with: (`.github/workflows/crash-diagnostic.yml:108`), with: (`.github/workflows/crash-diagnostic.yml:119`), env: (`.github/workflows/crash-diagnostic.yml:131`), env: (`.github/workflows/crash-diagnostic.yml:149`), env: (`.github/workflows/crash-diagnostic.yml:156`), with: (`.github/workflows/crash-diagnostic.yml:165`), with: (`.github/workflows/crash-diagnostic.yml:188`), with: (`.github/workflows/crash-diagnostic.yml:196`), with: (`.github/workflows/crash-diagnostic.yml:204`), with: (`.github/workflows/crash-diagnostic.yml:213`) [VERIFIED-CI]
- If conditions: if: always() (`.github/workflows/crash-diagnostic.yml:68`); if: always() (`.github/workflows/crash-diagnostic.yml:76`); if: always() (`.github/workflows/crash-diagnostic.yml:84`); if: always() (`.github/workflows/crash-diagnostic.yml:93`); if: always() (`.github/workflows/crash-diagnostic.yml:186`); if: always() (`.github/workflows/crash-diagnostic.yml:194`); if: always() (`.github/workflows/crash-diagnostic.yml:202`); if: always() (`.github/workflows/crash-diagnostic.yml:211`) [VERIFIED-CI]
- Secret references: RELEASE_KEYSTORE_BASE64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }} (`.github/workflows/crash-diagnostic.yml:132`); RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }} (`.github/workflows/crash-diagnostic.yml:133`); RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }} (`.github/workflows/crash-diagnostic.yml:134`); RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }} (`.github/workflows/crash-diagnostic.yml:135`); RELEASE_KEYSTORE_BASE64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }} (`.github/workflows/crash-diagnostic.yml:150`); RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }} (`.github/workflows/crash-diagnostic.yml:158`); RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }} (`.github/workflows/crash-diagnostic.yml:159`); RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }} (`.github/workflows/crash-diagnostic.yml:160`) [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/crash-diagnostic.yml:1-217`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/dependency-scan.yml`

- Triggers: push: (`.github/workflows/dependency-scan.yml:4`), pull_request: (`.github/workflows/dependency-scan.yml:6`) [VERIFIED-CI]
- Job-like keys: push: (`.github/workflows/dependency-scan.yml:4`), pull_request: (`.github/workflows/dependency-scan.yml:6`), dependency-scan: (`.github/workflows/dependency-scan.yml:14`), steps: (`.github/workflows/dependency-scan.yml:18`), with: (`.github/workflows/dependency-scan.yml:24`), with: (`.github/workflows/dependency-scan.yml:36`), with: (`.github/workflows/dependency-scan.yml:42`) [VERIFIED-CI]
- If conditions: if: always() (`.github/workflows/dependency-scan.yml:34`); if: always() (`.github/workflows/dependency-scan.yml:40`) [VERIFIED-CI]
- Secret references: none found [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/dependency-scan.yml:1-45`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/device-capture.yml`

- Triggers: pull_request: (`.github/workflows/device-capture.yml:4`), workflow_dispatch: (`.github/workflows/device-capture.yml:8`) [VERIFIED-CI]
- Job-like keys: pull_request: (`.github/workflows/device-capture.yml:4`), paths: (`.github/workflows/device-capture.yml:5`), workflow_dispatch: (`.github/workflows/device-capture.yml:8`), inputs: (`.github/workflows/device-capture.yml:9`), duration_seconds: (`.github/workflows/device-capture.yml:10`), validate: (`.github/workflows/device-capture.yml:20`), steps: (`.github/workflows/device-capture.yml:23`), with: (`.github/workflows/device-capture.yml:26`), emulator: (`.github/workflows/device-capture.yml:43`), steps: (`.github/workflows/device-capture.yml:49`), with: (`.github/workflows/device-capture.yml:52`), with: (`.github/workflows/device-capture.yml:63`), with: (`.github/workflows/device-capture.yml:85`), with: (`.github/workflows/device-capture.yml:103`) [VERIFIED-CI]
- If conditions: if: github.event_name == 'workflow_dispatch' (`.github/workflows/device-capture.yml:46`); if: always() (`.github/workflows/device-capture.yml:101`) [VERIFIED-CI]
- Secret references: none found [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/device-capture.yml:1-107`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/generate-room-schema.yml`

- Triggers: workflow_dispatch: (`.github/workflows/generate-room-schema.yml:4`) [VERIFIED-CI]
- Job-like keys: workflow_dispatch: (`.github/workflows/generate-room-schema.yml:4`), generate-schema: (`.github/workflows/generate-room-schema.yml:10`), steps: (`.github/workflows/generate-room-schema.yml:13`), with: (`.github/workflows/generate-room-schema.yml:19`), with: (`.github/workflows/generate-room-schema.yml:52`) [VERIFIED-CI]
- If conditions: none found [VERIFIED-CI]
- Secret references: none found [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/generate-room-schema.yml:1-54`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/metrics.yml`

- Triggers: workflow_dispatch: (`.github/workflows/metrics.yml:4`), pull_request: (`.github/workflows/metrics.yml:5`), push: (`.github/workflows/metrics.yml:8`) [VERIFIED-CI]
- Job-like keys: workflow_dispatch: (`.github/workflows/metrics.yml:4`), pull_request: (`.github/workflows/metrics.yml:5`), branches: (`.github/workflows/metrics.yml:6`), push: (`.github/workflows/metrics.yml:8`), branches: (`.github/workflows/metrics.yml:9`), compose-metrics: (`.github/workflows/metrics.yml:16`), steps: (`.github/workflows/metrics.yml:19`), with: (`.github/workflows/metrics.yml:22`), with: (`.github/workflows/metrics.yml:27`), with: (`.github/workflows/metrics.yml:33`), with: (`.github/workflows/metrics.yml:73`) [VERIFIED-CI]
- If conditions: none found [VERIFIED-CI]
- Secret references: none found [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/metrics.yml:1-76`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/pulse-ci.yml`

- Triggers: push: (`.github/workflows/pulse-ci.yml:4`), pull_request: (`.github/workflows/pulse-ci.yml:6`) [VERIFIED-CI]
- Job-like keys: push: (`.github/workflows/pulse-ci.yml:4`), pull_request: (`.github/workflows/pulse-ci.yml:6`), build-and-test: (`.github/workflows/pulse-ci.yml:13`), steps: (`.github/workflows/pulse-ci.yml:17`), with: (`.github/workflows/pulse-ci.yml:20`), with: (`.github/workflows/pulse-ci.yml:25`), with: (`.github/workflows/pulse-ci.yml:76`), with: (`.github/workflows/pulse-ci.yml:84`), with: (`.github/workflows/pulse-ci.yml:95`), build-release: (`.github/workflows/pulse-ci.yml:111`), steps: (`.github/workflows/pulse-ci.yml:117`), with: (`.github/workflows/pulse-ci.yml:120`), with: (`.github/workflows/pulse-ci.yml:125`), env: (`.github/workflows/pulse-ci.yml:130`), env: (`.github/workflows/pulse-ci.yml:149`), env: (`.github/workflows/pulse-ci.yml:159`), with: (`.github/workflows/pulse-ci.yml:169`) [VERIFIED-CI]
- If conditions: if: always() (`.github/workflows/pulse-ci.yml:65`); if: always() (`.github/workflows/pulse-ci.yml:70`); if: always() (`.github/workflows/pulse-ci.yml:74`); if: always() (`.github/workflows/pulse-ci.yml:82`); if: always() (`.github/workflows/pulse-ci.yml:93`); if: github.event_name == 'push' && github.ref == 'refs/heads/consumer-v1' (`.github/workflows/pulse-ci.yml:115`); if: always() (`.github/workflows/pulse-ci.yml:167`) [VERIFIED-CI]
- Secret references: RELEASE_KEYSTORE_BASE64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }} (`.github/workflows/pulse-ci.yml:131`); RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }} (`.github/workflows/pulse-ci.yml:132`); RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }} (`.github/workflows/pulse-ci.yml:133`); RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }} (`.github/workflows/pulse-ci.yml:134`); RELEASE_KEYSTORE_BASE64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }} (`.github/workflows/pulse-ci.yml:150`); RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }} (`.github/workflows/pulse-ci.yml:161`); RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }} (`.github/workflows/pulse-ci.yml:162`); RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }} (`.github/workflows/pulse-ci.yml:163`) [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/pulse-ci.yml:1-173`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/pulse-instrumented-tests.yml`

- Triggers: push: (`.github/workflows/pulse-instrumented-tests.yml:17`), pull_request: (`.github/workflows/pulse-instrumented-tests.yml:19`) [VERIFIED-CI]
- Job-like keys: push: (`.github/workflows/pulse-instrumented-tests.yml:17`), pull_request: (`.github/workflows/pulse-instrumented-tests.yml:19`), instrumented-tests: (`.github/workflows/pulse-instrumented-tests.yml:26`), steps: (`.github/workflows/pulse-instrumented-tests.yml:31`), with: (`.github/workflows/pulse-instrumented-tests.yml:34`), with: (`.github/workflows/pulse-instrumented-tests.yml:45`), with: (`.github/workflows/pulse-instrumented-tests.yml:54`), with: (`.github/workflows/pulse-instrumented-tests.yml:79`) [VERIFIED-CI]
- If conditions: if: always() (`.github/workflows/pulse-instrumented-tests.yml:77`) [VERIFIED-CI]
- Secret references: none found [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/pulse-instrumented-tests.yml:1-84`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/secret-scan.yml`

- Triggers: push: (`.github/workflows/secret-scan.yml:4`), pull_request: (`.github/workflows/secret-scan.yml:6`) [VERIFIED-CI]
- Job-like keys: push: (`.github/workflows/secret-scan.yml:4`), pull_request: (`.github/workflows/secret-scan.yml:6`), gitleaks: (`.github/workflows/secret-scan.yml:13`), steps: (`.github/workflows/secret-scan.yml:16`), with: (`.github/workflows/secret-scan.yml:19`), env: (`.github/workflows/secret-scan.yml:24`) [VERIFIED-CI]
- If conditions: none found [VERIFIED-CI]
- Secret references: GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }} (`.github/workflows/secret-scan.yml:25`) [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/secret-scan.yml:1-27`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

### `.github/workflows/wp-2-build-gates.yml`

- Triggers: workflow_dispatch: (`.github/workflows/wp-2-build-gates.yml:4`), pull_request: (`.github/workflows/wp-2-build-gates.yml:5`) [VERIFIED-CI]
- Job-like keys: workflow_dispatch: (`.github/workflows/wp-2-build-gates.yml:4`), pull_request: (`.github/workflows/wp-2-build-gates.yml:5`), branches: (`.github/workflows/wp-2-build-gates.yml:6`), wp2-build-gates: (`.github/workflows/wp-2-build-gates.yml:13`), steps: (`.github/workflows/wp-2-build-gates.yml:17`), with: (`.github/workflows/wp-2-build-gates.yml:20`), with: (`.github/workflows/wp-2-build-gates.yml:25`), with: (`.github/workflows/wp-2-build-gates.yml:31`), with: (`.github/workflows/wp-2-build-gates.yml:124`) [VERIFIED-CI]
- If conditions: if: always() (`.github/workflows/wp-2-build-gates.yml:122`) [VERIFIED-CI]
- Secret references: none found [VERIFIED-CI]
- Enforcement: see full numbered workflow at `.github/workflows/wp-2-build-gates.yml:1-130`; every `run`, action, failure mode, and artifact step is preserved in the source. [VERIFIED-CI]

**9 workflow files listed / 9 found by `git ls-files ' .github/workflows/*'`.**

---

# Resource IDs, Koin, and Navigation

## Resource references and definitions

```text
android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt:127:                    painter = painterResource(R.drawable.ic_signal_gate_logo),
android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt:205:                        painter = painterResource(R.drawable.shield_logo),
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:262:                    painter = painterResource(R.drawable.ic_signal_gate_logo),
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:298:                    painter = painterResource(R.drawable.shield_logo),
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:486:                        painter = painterResource(R.drawable.ic_signal_gate_logo),
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:525:                    painter = painterResource(R.drawable.shield_logo),
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:443:            .setSmallIcon(R.drawable.shield_logo)
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:484:            .setSmallIcon(R.drawable.shield_logo)
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:503:            .setSmallIcon(R.drawable.shield_logo)

```
[VERIFIED-CODE]

```text
android/app/src/main/res/drawable/ic_signal_gate_logo.xml:28:        <aapt:attr name="android:strokeColor">
android/app/src/main/res/drawable/ic_signal_gate_logo.xml:41:        <aapt:attr name="android:strokeColor">
android/app/src/main/res/drawable/ic_signal_gate_logo.xml:54:        <aapt:attr name="android:strokeColor">
android/app/src/main/res/drawable/ic_signal_gate_logo.xml:67:        <aapt:attr name="android:strokeColor">
android/app/src/main/res/drawable/ic_signal_gate_logo.xml:80:        <aapt:attr name="android:strokeColor">
android/app/src/main/res/values/themes.xml:34:    <style name="Theme.App.Starting" parent="Theme.SplashScreen">
android/app/src/main/res/values/themes.xml:35:        <item name="windowSplashScreenBackground">@color/background_primary</item>
android/app/src/main/res/values/themes.xml:36:        <item name="postSplashScreenTheme">@style/Theme.SignalGate</item>
android/app/src/main/res/values/themes.xml:40:    <style name="Theme.SignalGate" parent="Theme.Material3.DayNight.NoActionBar">
android/app/src/main/res/values/themes.xml:43:        <item name="colorPrimary">@color/accent_primary</item>
android/app/src/main/res/values/themes.xml:44:        <item name="colorPrimaryDark">@color/color_primary_dark</item>
android/app/src/main/res/values/themes.xml:45:        <item name="colorAccent">@color/accent_secondary</item>
android/app/src/main/res/values/themes.xml:48:        <item name="android:windowBackground">@color/background_primary</item>
android/app/src/main/res/values/themes.xml:49:        <item name="android:statusBarColor">@color/background_primary</item>
android/app/src/main/res/values/themes.xml:50:        <item name="android:navigationBarColor">@color/background_secondary</item>
android/app/src/main/res/values/themes.xml:53:        <item name="colorSurface">@color/card_background</item>
android/app/src/main/res/values/themes.xml:54:        <item name="colorOnSurface">@color/text_primary</item>
android/app/src/main/res/values/themes.xml:57:        <item name="android:textColorPrimary">@color/text_primary</item>
android/app/src/main/res/values/themes.xml:58:        <item name="android:textColorSecondary">@color/text_secondary</item>
android/app/src/main/res/values/themes.xml:61:        <item name="background_primary">@color/background_primary</item>
android/app/src/main/res/values/themes.xml:62:        <item name="background_secondary">@color/background_secondary</item>
android/app/src/main/res/values/themes.xml:63:        <item name="card_background">@color/card_background</item>
android/app/src/main/res/values/themes.xml:64:        <item name="text_muted">@color/text_muted</item>
android/app/src/main/res/values/themes.xml:66:        <item name="accent_primary">@color/accent_primary</item>
android/app/src/main/res/values/themes.xml:67:        <item name="accent_secondary">@color/accent_secondary</item>
android/app/src/main/res/values/themes.xml:69:        <item name="overlay_glass">@color/overlay_glass</item>
android/app/src/main/res/values/themes.xml:70:        <item name="nav_selected">@color/nav_selected</item>
android/app/src/main/res/values/themes.xml:71:        <item name="divider_color">@color/divider_color</item>
android/app/src/main/res/values/themes.xml:73:        <item name="shield_active">@color/shield_active</item>
android/app/src/main/res/values/themes.xml:74:        <item name="shield_inactive">@color/shield_inactive</item>
android/app/src/main/res/values/dimens.xml:8:    <dimen name="space_xs">4dp</dimen>
android/app/src/main/res/values/dimens.xml:9:    <dimen name="space_sm">8dp</dimen>
android/app/src/main/res/values/dimens.xml:10:    <dimen name="space_md">12dp</dimen>
android/app/src/main/res/values/dimens.xml:11:    <dimen name="space_lg">16dp</dimen>
android/app/src/main/res/values/dimens.xml:12:    <dimen name="space_xl">20dp</dimen>
android/app/src/main/res/values/dimens.xml:13:    <dimen name="space_xxl">24dp</dimen>
android/app/src/main/res/values/dimens.xml:14:    <dimen name="space_huge">32dp</dimen>
android/app/src/main/res/values/dimens.xml:20:    <dimen name="card_corner_radius">20dp</dimen>
android/app/src/main/res/values/dimens.xml:21:    <dimen name="card_corner_radius_small">14dp</dimen>
android/app/src/main/res/values/dimens.xml:22:    <dimen name="card_corner_radius_large">28dp</dimen>
android/app/src/main/res/values/dimens.xml:24:    <dimen name="card_elevation">6dp</dimen>
android/app/src/main/res/values/dimens.xml:25:    <dimen name="card_padding">18dp</dimen>
android/app/src/main/res/values/dimens.xml:31:    <dimen name="button_height">52dp</dimen>
android/app/src/main/res/values/dimens.xml:32:    <dimen name="button_corner_radius">18dp</dimen>
android/app/src/main/res/values/dimens.xml:33:    <dimen name="button_horizontal_padding">18dp</dimen>
android/app/src/main/res/values/dimens.xml:39:    <dimen name="icon_small">18dp</dimen>
android/app/src/main/res/values/dimens.xml:40:    <dimen name="icon_medium">24dp</dimen>
android/app/src/main/res/values/dimens.xml:41:    <dimen name="icon_large">42dp</dimen>
android/app/src/main/res/values/dimens.xml:42:    <dimen name="logo_footer_size">72dp</dimen>
android/app/src/main/res/values/dimens.xml:48:    <dimen name="header_padding">18dp</dimen>
android/app/src/main/res/values/dimens.xml:49:    <dimen name="header_margin">16dp</dimen>
android/app/src/main/res/values/dimens.xml:55:    <dimen name="text_small">12sp</dimen>
android/app/src/main/res/values/dimens.xml:56:    <dimen name="text_body">14sp</dimen>
android/app/src/main/res/values/dimens.xml:57:    <dimen name="text_subtitle">16sp</dimen>
android/app/src/main/res/values/dimens.xml:58:    <dimen name="text_title">22sp</dimen>
android/app/src/main/res/values/dimens.xml:59:    <dimen name="text_display">28sp</dimen>
android/app/src/main/res/values/dimens.xml:65:    <dimen name="divider_height">1dp</dimen>
android/app/src/main/res/values/dimens.xml:71:    <dimen name="bottom_nav_height">64dp</dimen>
android/app/src/main/res/values/dimens.xml:73:    <dimen name="activity_horizontal_margin">@dimen/space_lg</dimen>
android/app/src/main/res/values/dimens.xml:74:    <dimen name="activity_vertical_margin">@dimen/space_lg</dimen>
android/app/src/main/res/values/dimens.xml:75:    <dimen name="nav_header_height">160dp</dimen>
android/app/src/main/res/values/styles.xml:4:    <style name="AppTheme" parent="Theme.MaterialComponents.DayNight.NoActionBar">
android/app/src/main/res/values/styles.xml:6:        <item name="colorPrimary">@color/background_primary</item>
android/app/src/main/res/values/styles.xml:7:        <item name="colorPrimaryDark">@color/background_primary</item>
android/app/src/main/res/values/styles.xml:8:        <item name="colorAccent">@color/accent_primary</item>
android/app/src/main/res/values/styles.xml:9:        <item name="android:windowBackground">@color/background_primary</item>
android/app/src/main/res/values/styles.xml:10:        <item name="android:textColorPrimary">@color/text_primary</item>
android/app/src/main/res/values/styles.xml:11:        <item name="android:textColorSecondary">@color/text_secondary</item>
android/app/src/main/res/values/styles.xml:15:    <style name="Widget.SignalGate.Card" parent="Widget.MaterialComponents.CardView">
android/app/src/main/res/values/styles.xml:16:        <item name="cardBackgroundColor">@color/overlay_glass</item>
android/app/src/main/res/values/styles.xml:17:        <item name="cardCornerRadius">16dp</item>
android/app/src/main/res/values/styles.xml:18:        <item name="cardElevation">0dp</item>
android/app/src/main/res/values/styles.xml:19:        <item name="strokeWidth">1dp</item>
android/app/src/main/res/values/styles.xml:20:        <item name="strokeColor">#33FFFFFF</item> <!-- Semi-transparent white for border -->
android/app/src/main/res/values/styles.xml:24:    <style name="TextAppearance.SignalGate.Header" parent="TextAppearance.MaterialComponents.Headline5">
android/app/src/main/res/values/styles.xml:25:        <item name="android:textColor">@color/text_primary</item>
android/app/src/main/res/values/styles.xml:26:        <item name="android:textStyle">bold</item>
android/app/src/main/res/values/styles.xml:29:    <style name="TextAppearance.SignalGate.Subtitle" parent="TextAppearance.MaterialComponents.Subtitle1">
android/app/src/main/res/values/styles.xml:30:        <item name="android:textColor">@color/text_secondary</item>
android/app/src/main/res/values/styles.xml:33:    <style name="TextAppearance.SignalGate.Body" parent="TextAppearance.MaterialComponents.Body1">
android/app/src/main/res/values/styles.xml:34:        <item name="android:textColor">@color/text_secondary</item>
android/app/src/main/res/values/styles.xml:37:    <style name="TextAppearance.SignalGate.Muted" parent="TextAppearance.MaterialComponents.Caption">
android/app/src/main/res/values/styles.xml:38:        <item name="android:textColor">@color/text_muted</item>
android/app/src/main/res/values/colors.xml:7:    <color name="color_primary_dark">#0066CC</color>
android/app/src/main/res/values/colors.xml:12:    <color name="background_primary">#0A0E14</color>
android/app/src/main/res/values/colors.xml:13:    <color name="background_secondary">#121A26</color>
android/app/src/main/res/values/colors.xml:14:    <color name="card_background">#121A26</color>
android/app/src/main/res/values/colors.xml:15:    <color name="text_primary">#FFFFFF</color>
android/app/src/main/res/values/colors.xml:16:    <color name="text_secondary">#A0AEC0</color>
android/app/src/main/res/values/colors.xml:17:    <color name="text_muted">#718096</color>
android/app/src/main/res/values/colors.xml:20:    <color name="accent_primary">#00A3FF</color>
android/app/src/main/res/values/colors.xml:21:    <color name="accent_secondary">#00D1FF</color>
android/app/src/main/res/values/colors.xml:22:    <color name="overlay_glass">#CC121A26</color>
android/app/src/main/res/values/colors.xml:25:    <color name="nav_selected">#1A2B44</color>
android/app/src/main/res/values/colors.xml:26:    <color name="divider_color">#1AFFFFFF</color>
android/app/src/main/res/values/colors.xml:27:    <color name="shield_active">#00C853</color>
android/app/src/main/res/values/colors.xml:28:    <color name="shield_inactive">#FF4D4D</color>
android/app/src/main/res/values/strings.xml:4:    <string name="app_name">SignalGate</string>
android/app/src/main/res/values/attrs.xml:5:    <declare-styleable name="ThemeSignalGate">
android/app/src/main/res/values/attrs.xml:6:        <attr name="background_primary" format="color" />
android/app/src/main/res/values/attrs.xml:7:        <attr name="background_secondary" format="color" />
android/app/src/main/res/values/attrs.xml:8:        <attr name="card_background" format="color" />
android/app/src/main/res/values/attrs.xml:9:        <attr name="text_muted" format="color" />
android/app/src/main/res/values/attrs.xml:10:        <attr name="accent_primary" format="color" />
android/app/src/main/res/values/attrs.xml:11:        <attr name="accent_secondary" format="color" />
android/app/src/main/res/values/attrs.xml:12:        <attr name="overlay_glass" format="color" />
android/app/src/main/res/values/attrs.xml:13:        <attr name="nav_selected" format="color" />
android/app/src/main/res/values/attrs.xml:14:        <attr name="divider_color" format="color" />
android/app/src/main/res/values/attrs.xml:15:        <attr name="shield_active" format="color" />
android/app/src/main/res/values/attrs.xml:16:        <attr name="shield_inactive" format="color" />

```
[VERIFIED-CODE]

Unresolved resource IDs are the reference tokens not matched by resource-definition names in the current `res` tree; the mechanical comparison result is recorded here: **UNKNOWN pending a full generated-R symbol table**, because this audit environment does not run Android resource generation. [UNKNOWN]

## Koin

Binding/request scan:
```text
android/app/src/test/kotlin/com/signalgate/pulse/security/SecurityUtilsTest.kt:27:            .also { it.isAccessible = true }.get(SecurityUtils) as? String
android/app/src/test/kotlin/com/signalgate/pulse/security/SecurityUtilsTest.kt:29:            .also { it.isAccessible = true }.get(SecurityUtils) as? String
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:80:        assertEquals(1, maxConcurrentEntries.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:81:        assertEquals(1, ftcInsertCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:116:        assertEquals(1, ftcInsertCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:117:        assertEquals(2, ftcLookupCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:147:        assertEquals(2, ftcLookupCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:148:        assertEquals(2, ftcInsertCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:180:        assertEquals(3, ftcLookupCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:181:        assertEquals(2, ftcInsertCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt:182:        assertEquals(1, fccLookupCalls.get())
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerEnsureFederalRowsTest.kt:88:        val ftcId = rows.single { it.type == "FTC" }.id
android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerEnsureFederalRowsTest.kt:89:        val fccId = rows.single { it.type == "FCC" }.id
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:91:            single { testDatabase.sourceDao() }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:92:            single { testDatabase.unifiedEntryDao() }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:93:            single { testDatabase.callLogDao() }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:94:            single { testDatabase.settingDao() }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:95:            single { testDatabase.syncHistoryDao() }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:96:            single { testDatabase.pendingCardDao() }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:97:            single { SettingRepository(get()) }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:98:            single { PendingCardRepository(get()) }
android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:119:        // logic NPEs. CommunitySyncWorker is never resolved via getKoin().get()
android/app/src/androidTest/kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt:57:            .also { it.isAccessible = true }.get(SecurityUtils) as String
android/app/src/androidTest/kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt:59:            .also { it.isAccessible = true }.get(SecurityUtils) as String
android/app/src/androidTest/kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt:61:            .also { it.isAccessible = true }.get(SecurityUtils) as String
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:52:    private val screeningEngine: CallScreeningEngine by inject()
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:53:    private val callLogRepository: CallLogRepository by inject()
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:54:    private val pendingCardRepository: PendingCardRepository by inject()
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:55:    private val pulseHapticsController: PulseHapticsController by inject()
android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt:56:    private val pulseTriggerLimiter: PulseTriggerLimiter by inject()
android/app/src/main/java/com/signalgate/pulse/MainApplication.kt:154:        get() = Configuration.Builder()
android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt:35:    private val reliableSourceManager: ReliableSourceManager by inject()
android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt:36:    private val dataSyncEngine: DataSyncEngine by inject()
android/app/src/main/java/com/signalgate/pulse/CallActionReceiver.kt:49:    private val securityRuleRepository: SecurityRuleRepository by inject()
android/app/src/main/java/com/signalgate/pulse/CallActionReceiver.kt:50:    private val pendingCardRepository: PendingCardRepository by inject()
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:54: *   viewModelModule — Layer 6 Presentation: one viewModel{} per screen (Contract §4)
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:86:    single { get<SignalGateDatabase>().sourceDao() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:87:    single { get<SignalGateDatabase>().unifiedEntryDao() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:88:    single { get<SignalGateDatabase>().callLogDao() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:89:    single { get<SignalGateDatabase>().settingDao() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:90:    single { get<SignalGateDatabase>().syncHistoryDao() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:91:    single { get<SignalGateDatabase>().pendingCardDao() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:92:    single { PendingCardRepository(get()) }  // Phase 1.4
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:116:    single { DataSourceRepository(get(), get(), get(), get(named("patternBloom"))) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:117:    single { CallLogRepository(get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:118:    single { SyncHistoryRepository(get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:119:    single { ContactsRepository(androidContext()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:149:    single { SecurityRuleRepository(get(), get(), get(), get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:150:    single { BlocklistRepository(get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:151:    single { SettingRepository(get()) } // Added for architecture drift fix — Roadmap Step 0.4
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:168:    single { BloomFilterEngine() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:176:    single { SecureCsvParser() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:177:    single { SnapshotSanityValidator() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:178:    single {
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:180:            bloomFilter = get(),
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:187:    // Security fix (audit finding): third get() resolves the SecureCsvParser
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:190:    single { ReliableSourceManager(get(), get(), get(), get(), get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:191:    single { SourceSyncUseCase(get(), get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:196:    single { CallRiskEvaluator }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:197:    single { CallScreeningEngine(get(), get(), get()) } // 3rd get() = SettingRepository, for HeuristicsMode (onboarding Step 3)
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:198:    single { DataSyncEngine(get(), get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:214:    viewModel { ContactsViewModel(get(), get(), get(), get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:215:    viewModel { DashboardViewModel(get(), get(), get(), get()) } // Phase 0.4: real source sync boundary added
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:219:    // a dead, never-navigated-to class). get() resolves by the new type.
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:220:    viewModel { BlockedNumbersViewModel(get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:221:    viewModel { RecentCallsViewModel(get(), get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:222:    viewModel { LogcatViewModel() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:223:    viewModel { OnboardingViewModel(get()) } // Step 2.6: added SettingRepository for markOnboardingComplete
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:224:    viewModel { SettingsViewModel(get()) } // Step 2.6: new — owns shield-color persistence, resolves half of FLAG-1
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:225:    viewModel { PendingCardViewModel(get(), get()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:226:    viewModel { SourcesViewModel(get(), get(), get()) } // Phase 0.4: real source sync boundary added
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:235:    single { PulseHapticsController(androidContext()) }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:236:    single { PulseTriggerLimiter() }
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:257:    // Fixed (Phase 0.3): previously passed a 3rd argument, get(), to a constructor
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:259:    // dependencies (ReliableSourceManager, DataSyncEngine) via `by inject()`
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:262:    factory { (context: android.content.Context, params: androidx.work.WorkerParameters) ->
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:303:    val koin = org.koin.core.context.GlobalContext.get()
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:334:    val koin = org.koin.core.context.GlobalContext.get()
android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/ContactsViewModel.kt:47:        get() {
android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/ContactsViewModel.kt:57:        get() = _contacts.value.count { it.isSelected }
android/app/src/main/java/com/signalgate/pulse/data/security/BloomFilterEngine.kt:85:                if (!bitSet.get(index)) {
android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt:18:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt:33:fun SourcesScreen(viewModel: SourcesViewModel = koinViewModel()) {
android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsScreens.kt:13:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsScreens.kt:45:    viewModel: SettingsViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/screens/CallLogScreen.kt:15:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/screens/CallLogScreen.kt:22:    viewModel: RecentCallsViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:26:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:128:    viewModel: DashboardViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/screens/BlockAllowListScreen.kt:20:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/screens/BlockAllowListScreen.kt:35:fun BlockAllowListScreen(viewModel: BlockedNumbersViewModel = koinViewModel()) {
android/app/src/main/java/com/signalgate/pulse/ui/screens/PermissionSettingsScreen.kt:31:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/screens/PermissionSettingsScreen.kt:54:    viewModel: OnboardingViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/screens/LogcatViewerScreen.kt:38:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/screens/LogcatViewerScreen.kt:50:    viewModel: LogcatViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/digest/PendingCardViewModel.kt:24: * Injected via Koin: PendingCardRepository (get()), BlocklistRepository (get()).
android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt:38:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt:65:    viewModel: DashboardViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/digest/DigestScreen.kt:22:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/digest/DigestScreen.kt:49:    viewModel: PendingCardViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:50:import org.koin.androidx.compose.koinViewModel
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:76:    viewModel: OnboardingViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:794:    viewModel: ContactsViewModel = koinViewModel()
android/app/src/main/java/com/signalgate/pulse/logic/SourceSyncUseCase.kt:25:            get() = results.count { it.success }
android/app/src/main/java/com/signalgate/pulse/logic/SourceSyncUseCase.kt:27:            get() = results.size
android/app/src/main/java/com/signalgate/pulse/logic/SourceSyncUseCase.kt:29:            get() = results.isNotEmpty() && results.all { it.success }
android/app/src/main/java/com/signalgate/pulse/logic/SourceSyncUseCase.kt:31:            get() = when {
android/app/src/main/java/com/signalgate/pulse/diagnostics/AppDiagnosticsTree.kt:27:        get() = when (priority) {
android/app/src/main/java/com/signalgate/pulse/diagnostics/AppDiagnosticsTree.kt:139:        get() = tree.entries

```
[VERIFIED-CODE]

`KoinModuleTest.kt` exists as the repository’s DI graph test (`android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt:...`) [VERIFIED-TEST]. A complete “requested but not provided” result is **UNKNOWN** unless the test is executed successfully against the current checkout; no missing binding is asserted here by inference. [UNKNOWN]

## Navigation

```text
android/app/src/main/java/com/signalgate/pulse/MainActivity.kt:103: * Drawer navigation must not pop to the NavHost's startup route: that route is
android/app/src/main/java/com/signalgate/pulse/MainActivity.kt:109:    val startupRoute = graph.findNode(graph.startDestinationId)?.route
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:7:import androidx.navigation.testing.TestNavHostController
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:68:    private fun createController(): TestNavHostController {
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:70:        return TestNavHostController(context).apply {
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:72:            graph = createGraph(startDestination = STARTUP_ROUTE) {
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:73:                composable(STARTUP_ROUTE) {}
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:74:                composable(ONBOARDING_ROUTE) {}
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:75:                composable(DASHBOARD_ROUTE) {}
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:76:                composable(SOURCES_ROUTE) {}
android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt:77:                composable(DIGEST_ROUTE) {}
android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt:51: * startDestination in NavGraph replaces OperationalDashboard for Pulse users.
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:40:import androidx.navigation.NavHostController
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:41:import androidx.navigation.compose.NavHost
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:75:    navController: NavHostController,
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:79:    // SignalGateNavGraph — it's already attached to that outer NavHost. A
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:80:    // NavHostController can only be attached to one NavHost at a time, so
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:81:    // reusing it below for this wizard's own NavHost caused:
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:90:    NavHost(navController = wizardNavController, startDestination = "eula") {
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:91:        composable("eula")        { EulaStep(wizardNavController, viewModel) }
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:92:        composable("welcome")     { WelcomeStep(wizardNavController) }
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:93:        composable("permissions") { PermissionsStep(wizardNavController, viewModel) }
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:94:        composable("contacts")    { ContactsImportStep(wizardNavController) }
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:95:        composable("sources")     { SourcesSelectionStep(wizardNavController, viewModel) }
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:96:        composable("risk")        { RiskThresholdStep(navController, viewModel) }
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:112:fun EulaStep(navController: NavHostController, viewModel: OnboardingViewModel) {
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:228:fun WelcomeStep(navController: NavHostController) {
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:410:fun PermissionsStep(navController: NavHostController, viewModel: OnboardingViewModel) {
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:793:    navController: NavHostController,
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:1015:fun SourcesSelectionStep(navController: NavHostController, viewModel: OnboardingViewModel) {
android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt:1169:fun RiskThresholdStep(navController: NavHostController, viewModel: OnboardingViewModel) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:12:import androidx.navigation.NavHostController
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:13:import androidx.navigation.compose.NavHost
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:43:    navController: NavHostController,
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:47:    NavHost(
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:49:        startDestination = STARTUP_ROUTE,
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:52:        composable(STARTUP_ROUTE) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:56:        composable(Screen.Dashboard.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:70:        composable(Screen.Sources.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:74:        composable(Screen.CallLog.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:78:        composable(Screen.BlockAllowList.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:82:        composable(Screen.Settings.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:88:        composable(Screen.Logcat.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:92:        composable(Screen.Onboarding.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:100:        composable(Screen.PermissionSettings.route) {
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:116:        composable(
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:117:            route = Screen.Digest.route,
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:118:            deepLinks = listOf(navDeepLink { uriPattern = "signalgate://digest" })
android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt:127:    navController: NavHostController,

```
[VERIFIED-CODE]

`Screen.kt:8-31` declares the named routes; `NavGraph.kt:47-121` declares the outer start destination `startup`, screens, and `signalgate://digest` deep link; `OnboardingWizardScreen.kt:90-96` declares the nested wizard start destination `eula` and its six screens. [VERIFIED-CODE]

The route/screen mismatch result is **UNKNOWN** for generated/runtime-only destinations; static declarations show `startup` is private to `NavGraph.kt:28,49-53`, all `Screen` objects are represented except no separate route is declared for any unlisted screen, and `digest` has both a route and deep link. [VERIFIED-CODE/UNKNOWN]

**Resource reference and definition lists are tooling excerpts from `rg`; navigation declarations are listed by `rg`.**

---

# Manifest

Complete current manifest with line numbers:

```xml
   1 <?xml version="1.0" encoding="utf-8"?>
   2 <manifest xmlns:android="http://schemas.android.com/apk/res/android">
   3
   4     <!-- Core telephony permissions -->
   5     <uses-permission android:name="android.permission.READ_PHONE_STATE" />
   6     <uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />
   7     <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
   8     <uses-permission android:name="android.permission.READ_CALL_LOG" />
   9     <uses-permission android:name="android.permission.WRITE_CALL_LOG" />
  10
  11     <!-- Notifications — POST_NOTIFICATIONS is all that is needed for Step 1.8.
  12          The overlay window permission has been intentionally omitted. SignalGate
  13          Pulse does not draw over other apps. Any PR reintroducing an overlay
  14          permission should be blocked — see Architecture Contract v5. -->
  15     <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  16     <uses-permission android:name="android.permission.VIBRATE" />
  17
  18     <!-- Required for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, used by the
  19          Background Reliability toggle on PermissionSettingsScreen. -->
  20     <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
  21
  22     <!-- Network -->
  23     <uses-permission android:name="android.permission.INTERNET" />
  24
  25     <!-- Contacts -->
  26     <uses-permission android:name="android.permission.READ_CONTACTS" />
  27
  28     <!--
  29         These foreground-service and self-managed-call permissions are intentionally absent:
  30         FOREGROUND_SERVICE, FOREGROUND_SERVICE_PHONE_CALL, FOREGROUND_SERVICE_DATA_SYNC,
  31         and MANAGE_OWN_CALLS.
  32
  33         None is required by the current implementation:
  34           - The app has exactly one Service — SignalGateCallScreeningService — which is a
  35             *bound* android.telecom.CallScreeningService (invoked by Telecom via
  36             BIND_SCREENING_SERVICE), not something the app starts in the foreground.
  37             No startForeground()/setForeground() call exists anywhere in the codebase.
  38           - CommunitySyncWorker is a plain background CoroutineWorker (WorkManager
  39             schedules it with network+battery constraints); it does not use
  40             setForeground()/ForegroundInfo or foreground-work execution.
  41           - MANAGE_OWN_CALLS (self-managed ConnectionService / custom call UI) has zero
  42             references — no ConnectionService, PhoneAccount, or TelecomManager usage
  43             anywhere. Consistent with this being left over from a removed call-overlay
  44             feature.
  45
  46         If future work adds foreground execution or a self-managed calling feature, add
  47         only the permission required by that implementation and Android version; do not
  48         restore this set speculatively.
  49     -->
  50
  51     <!-- Phase 4.11: ensures CommunitySyncWorker is rescheduled after device reboot.
  52          WorkManager auto-reschedules on boot in most cases, but some OEMs
  53          (Samsung, Xiaomi) kill this path on Android 12+. The explicit
  54          BOOT_COMPLETED broadcast to SyncBootReceiver is the guarantee. -->
  55     <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  56
  57     <application
  58         android:name=".MainApplication"
  59         android:allowBackup="true"
  60         android:enableOnBackInvokedCallback="true"
  61         android:fullBackupContent="@xml/backup_rules"
  62         android:dataExtractionRules="@xml/data_extraction_rules"
  63         android:icon="@drawable/shield_logo"
  64         android:label="@string/app_name"
  65         android:roundIcon="@drawable/shield_logo"
  66         android:supportsRtl="true"
  67         android:theme="@style/Theme.SignalGate"
  68         android:networkSecurityConfig="@xml/network_security_config">
  69
  70         <!-- Single Activity — contract requirement.
  71              android:theme="@style/Theme.App.Starting" — launched with the splash
  72              theme; installSplashScreen() in MainActivity.kt switches to
  73              Theme.SignalGate (declared as postSplashScreenTheme) once ready. -->
  74         <activity
  75             android:name=".MainActivity"
  76             android:exported="true"
  77             android:launchMode="singleTask"
  78             android:theme="@style/Theme.App.Starting"
  79             android:windowSoftInputMode="adjustResize">
  80
  81             <intent-filter>
  82                 <action android:name="android.intent.action.MAIN" />
  83                 <category android:name="android.intent.category.LAUNCHER" />
  84             </intent-filter>
  85
  86             <intent-filter>
  87                 <action android:name="android.intent.action.VIEW" />
  88                 <category android:name="android.intent.category.DEFAULT" />
  89                 <data
  90                     android:scheme="signalgate"
  91                     android:host="digest" />
  92             </intent-filter>
  93
  94         </activity>
  95
  96         <service
  97             android:name=".SignalGateCallScreeningService"
  98             android:permission="android.permission.BIND_SCREENING_SERVICE"
  99             android:exported="true">
 100             <intent-filter>
 101                 <action android:name="android.telecom.CallScreeningService" />
 102             </intent-filter>
 103         </service>
 104
 105         <receiver
 106             android:name=".PhoneStateReceiver"
 107             android:permission="android.permission.READ_PHONE_STATE"
 108             android:exported="true">
 109             <intent-filter>
 110                 <action android:name="android.intent.action.PHONE_STATE" />
 111             </intent-filter>
 112         </receiver>
 113
 114         <receiver
 115             android:name=".CallActionReceiver"
 116             android:exported="false" />
 117
 118         <!-- Phase 4.11: reschedules CommunitySyncWorker after reboot and app update.
 119              BOOT_COMPLETED fires after reboot. MY_PACKAGE_REPLACED fires after OTA
 120              update — OS does NOT send BOOT_COMPLETED on update, so both are needed.
 121              exported=true required for BOOT_COMPLETED on Android 12+. -->
 122         <receiver
 123             android:name=".workers.SyncBootReceiver"
 124             android:exported="true">
 125             <intent-filter>
 126                 <action android:name="android.intent.action.BOOT_COMPLETED" />
 127                 <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
 128             </intent-filter>
 129         </receiver>
 130
 131     </application>
 132
 133 </manifest>
```

Permissions are the `<uses-permission>` entries at `:5-27,55`; application and components are at `:57-131`; exported flags are `MainActivity=true` (`:74-79`), screening service=true (`:96-103`), PhoneStateReceiver=true (`:105-112`), CallActionReceiver=false (`:114-116`), and SyncBootReceiver=true (`:122-129`) [VERIFIED-CODE].

**1 manifest file listed / 1 found by git ls-files AndroidManifest.xml.**

---

# Room Schema and Persistence

Room evidence is extracted from the current entity/database/DAO sources. [VERIFIED-CODE]

### `android/app/src/main/java/com/signalgate/pulse/database/entities/DatabaseEntities.kt`

```text
   1 package com.signalgate.pulse.database.entities
   2
   3 import androidx.room.ColumnInfo
   4 import androidx.room.Entity
   5 import androidx.room.ForeignKey
   6 import androidx.room.Index
   7 import androidx.room.PrimaryKey
   8
   9 /**
  10  * Source types with application-level lifecycle semantics.
  11  *
  12  * The database column remains a String for schema compatibility; this enum is
  13  * the compiler-checked vocabulary at decision and lifecycle call sites.
  14  */
  15 enum class SourceType(val persistedValue: String) {
  16     MANUAL("MANUAL"),
  17     FTC("FTC"),
  18     FCC("FCC");
  19
  20     companion object {
  21         fun fromPersisted(value: String): SourceType? =
  22             values().firstOrNull { it.persistedValue == value }
  23     }
  24 }
  25
  26 /**
  27  * SourceEntity represents a data source (local file or remote URL) in the MultiPoint Hub.
  28  */
  29 @Entity(
  30     tableName = "sources",
  31     indices = [
  32         Index(value = ["name"], unique = true),
  33         Index(value = ["type"])
  34     ]
  35 )
  36 data class SourceEntity(
  37     @PrimaryKey(autoGenerate = true)
  38     val id: Int = 0,
  39     val name: String,
  40     val type: String, // "CSV", "XLSX", "URL", "MANUAL"
  41     val pathOrUrl: String,
  42     val isEnabled: Boolean = true,
  43     val lastSynced: Long = 0,
  44     @ColumnInfo(name = "last_attempted_sync")
  45     val lastAttemptedSync: Long? = null,
  46     @ColumnInfo(name = "last_accepted_snapshot")
  47     val lastAcceptedSnapshot: Long? = null,
  48     val priority: Int = 0,
  49     val entriesCount: Int = 0,
  50     val healthStatus: String = "UNKNOWN",
  51     @ColumnInfo(name = "lifecycle_state")
  52     val lifecycleState: String = "ENABLED",
  53     @ColumnInfo(name = "snapshot_version")
  54     val snapshotVersion: String? = null,
  55     @ColumnInfo(name = "snapshot_hash")
  56     val snapshotHash: String? = null,
  57     @ColumnInfo(name = "accepted_record_count")
  58     val acceptedRecordCount: Int? = null,
  59     @ColumnInfo(name = "lifecycle_reason")
  60     val lifecycleReason: String? = null,
  61     val createdAt: Long = System.currentTimeMillis(),
  62     val updatedAt: Long = System.currentTimeMillis()
  63 )
  64
  65 /**
  66  * UnifiedEntryEntity represents a phone number entry with its action and source.
  67  */
  68 @Entity(
  69     tableName = "unified_entries",
  70     foreignKeys = [
  71         ForeignKey(
  72             entity = SourceEntity::class,
  73             parentColumns = ["id"],
  74             childColumns = ["sourceId"],
  75             onDelete = ForeignKey.CASCADE
  76         )
  77     ],
  78     indices = [
  79         Index(value = ["phoneNumber", "action"]),
  80         Index(value = ["sourceId"]),
  81         Index(value = ["isPattern"]),
  82         Index(value = ["action"])
  83     ]
  84 )
  85 data class UnifiedEntryEntity(
  86     @PrimaryKey(autoGenerate = true)
  87     val id: Int = 0,
  88     val phoneNumber: String,
  89     val action: String, // "BLOCK", "ALLOW"
  90     val sourceId: Int,
  91     val isPattern: Boolean = false,
  92     val category: String? = null,
  93     val confidence: Int? = null,
  94     val riskLevel: String? = null,
  95     val metadata: String? = null,
  96     val createdAt: Long = System.currentTimeMillis(),
  97     val updatedAt: Long = System.currentTimeMillis()
  98 )
  99
 100 /**
 101  * CallLogEntry is the permanent audit record of every screened call.
 102  * Written on every BLOCK and ALLOW decision. Never deleted except by
 103  * explicit purge (deleteOldCallLogs). Not to be confused with PendingCardEntity.
 104  */
 105 @Entity(
 106     tableName = "call_log",
 107     indices = [
 108         Index(value = ["phoneNumber"]),
 109         Index(value = ["timestamp"]),
 110         Index(value = ["decision"])
 111     ]
 112 )
 113 data class CallLogEntry(
 114     @PrimaryKey(autoGenerate = true)
 115     val id: Int = 0,
 116     val phoneNumber: String,
 117     val normalizedPhoneNumber: String,
 118     val timestamp: Long = System.currentTimeMillis(),
 119     val decision: String, // "ALLOW", "BLOCK", "SCREEN"
 120     val spamStatus: String,
 121     val spamCategory: String? = null,
 122     val confidence: Int? = null,
 123     val riskLevel: String? = null,
 124     val matchedSources: String? = null, // JSON array of source names
 125     val duration: Int = 0,
 126     val notes: String? = null
 127 )
 128
 129 /**
 130  * SettingEntry is the single config store for all app settings.
 131  * SharedPreferences must not be used for any key defined here.
 132  */
 133 @Entity(
 134     tableName = "settings",
 135     indices = [
 136         Index(value = ["key"], unique = true)
 137     ]
 138 )
 139 data class SettingEntry(
 140     @PrimaryKey(autoGenerate = true)
 141     val id: Int = 0,
 142     val key: String,
 143     val value: String,
 144     val type: String = "STRING",
 145     val updatedAt: Long = System.currentTimeMillis()
 146 )
 147
 148 /**
 149  * SyncHistoryEntry tracks the sync history for each source.
 150  */
 151 @Entity(
 152     tableName = "sync_history",
 153     foreignKeys = [
 154         ForeignKey(
 155             entity = SourceEntity::class,
 156             parentColumns = ["id"],
 157             childColumns = ["sourceId"],
 158             onDelete = ForeignKey.CASCADE
 159         )
 160     ],
 161     indices = [
 162         Index(value = ["sourceId"]),
 163         Index(value = ["timestamp"])
 164     ]
 165 )
 166 data class SyncHistoryEntry(
 167     @PrimaryKey(autoGenerate = true)
 168     val id: Int = 0,
 169     val sourceId: Int,
 170     val timestamp: Long = System.currentTimeMillis(),
 171     val status: String,
 172     val entriesAdded: Int = 0,
 173     val entriesUpdated: Int = 0,
 174     val entriesRemoved: Int = 0,
 175     val errorMessage: String? = null,
 176     val duration: Long = 0
 177 )
 178
 179 /**
 180  * PendingCardEntity is the short-lived post-call digest queue.
 181  *
 182  * Distinct from CallLogEntry:
 183  * - CallLogEntry = permanent audit record, never auto-deleted
 184  * - PendingCardEntity = ephemeral UI queue, deleted on card dismissal
 185  *
 186  * Created when an explicit decision requires review alongside the CallLogEntry
 187  * (currently HEURISTIC_BLOCK and HEURISTIC_FLAG).
 188  * Dismissed = true when the user swipes the card. Row deleted on swipe or
 189  * 'Dismiss All'. Never surfaces in history — that is CallLogEntry's job.
 190  */
 191 @Entity(
 192     tableName = "pending_cards",
 193     indices = [
 194         Index(value = ["dismissed"]),
 195         Index(value = ["timestamp"])
 196     ]
 197 )
 198 data class PendingCardEntity(
 199     @PrimaryKey(autoGenerate = true)
 200     val id: Int = 0,
 201     val phoneNumber: String,
 202     val timestamp: Long = System.currentTimeMillis(),
 203     val decision: String,           // "BLOCK" or "SCREEN" when review is required
 204     val confidence: Int?,
 205     val decisionSource: String?,    // Which source triggered the block
 206     val dismissed: Boolean = false
 207 )
```

### `android/app/src/main/java/com/signalgate/pulse/database/daos/DatabaseDAOs.kt`

```text
   1 package com.signalgate.pulse.database.daos
   2
   3 import androidx.room.Dao
   4 import androidx.room.Delete
   5 import androidx.room.Insert
   6 import androidx.room.OnConflictStrategy
   7 import androidx.room.Query
   8 import androidx.room.Update
   9 import com.signalgate.pulse.database.entities.CallLogEntry
  10 import com.signalgate.pulse.database.entities.PendingCardEntity
  11 import com.signalgate.pulse.database.entities.SettingEntry
  12 import com.signalgate.pulse.database.entities.SourceEntity
  13 import com.signalgate.pulse.database.entities.SyncHistoryEntry
  14 import com.signalgate.pulse.database.entities.UnifiedEntryEntity
  15 import kotlinx.coroutines.flow.Flow
  16
  17 /**
  18  * DAO for SourceEntity operations.
  19  */
  20 @Dao
  21 interface SourceDao {
  22     @Insert
  23     suspend fun insertSource(source: SourceEntity): Long
  24
  25     @Insert(onConflict = OnConflictStrategy.IGNORE)
  26     suspend fun insertSourceIfAbsent(source: SourceEntity): Long
  27
  28     @Update
  29     suspend fun updateSource(source: SourceEntity)
  30
  31     @Delete
  32     suspend fun deleteSource(source: SourceEntity)
  33
  34     /** Atomically delete a source by ID only when its persisted type is not protected. */
  35     @Query("DELETE FROM sources WHERE id = :id AND type NOT IN (:protectedTypes)")
  36     suspend fun deleteIfNotProtected(id: Int, protectedTypes: List<String>): Int
  37
  38     @Query("SELECT * FROM sources WHERE id = :id")
  39     suspend fun getSourceById(id: Int): SourceEntity?
  40
  41     @Query("SELECT * FROM sources WHERE name = :name LIMIT 1")
  42     suspend fun getSourceByName(name: String): SourceEntity?
  43
  44     @Query("SELECT * FROM sources ORDER BY priority DESC, name ASC")
  45     fun getAllSources(): Flow<List<SourceEntity>>
  46
  47     @Query("SELECT * FROM sources WHERE isEnabled = 1 ORDER BY priority DESC")
  48     fun getEnabledSources(): Flow<List<SourceEntity>>
  49
  50     @Query("SELECT COUNT(*) FROM sources")
  51     fun getSourceCount(): Flow<Int>
  52
  53     @Query("UPDATE sources SET isEnabled = :isEnabled, lifecycle_state = CASE WHEN :isEnabled = 1 THEN 'ENABLED' ELSE 'DISABLED' END, lifecycle_reason = NULL, updatedAt = :timestamp WHERE id = :id")
  54     suspend fun updateSourceEnabled(id: Int, isEnabled: Boolean, timestamp: Long = System.currentTimeMillis())
  55
  56     @Query("UPDATE sources SET last_attempted_sync = :timestamp, lifecycle_state = 'SYNCING', lifecycle_reason = NULL, updatedAt = :timestamp WHERE id = :id AND isEnabled = 1")
  57     suspend fun recordSyncAttempt(id: Int, timestamp: Long): Int
  58
  59     @Query("UPDATE sources SET last_accepted_snapshot = :timestamp, lastSynced = :timestamp, entriesCount = :entriesCount, accepted_record_count = :entriesCount, snapshot_version = :snapshotVersion, snapshot_hash = :snapshotHash, lifecycle_state = 'HEALTHY', healthStatus = 'HEALTHY', lifecycle_reason = NULL, updatedAt = :timestamp WHERE id = :id")
  60     suspend fun recordSnapshotAccepted(
  61         id: Int,
  62         timestamp: Long,
  63         entriesCount: Int,
  64         snapshotVersion: String?,
  65         snapshotHash: String?
  66     )
  67
  68     @Query("UPDATE sources SET lifecycle_state = :state, healthStatus = 'ERROR', lifecycle_reason = :reason, updatedAt = :timestamp WHERE id = :id AND isEnabled = 1")
  69     suspend fun recordSyncFailure(id: Int, state: String, reason: String, timestamp: Long): Int
  70 }
  71
  72 /**
  73  * Lightweight projection of just the two columns Bloom filter rehydration
  74  * reads (see DataSourceRepository.rehydrateBloomFilters()). Room maps query
  75  * results straight into this instead of a full UnifiedEntryEntity, so a page
  76  * of rows doesn't also carry category/metadata/confidence/riskLevel/etc that
  77  * rehydration never touches.
  78  */
  79 data class PhoneNumberPatternProjection(
  80     val phoneNumber: String,
  81     val isPattern: Boolean
  82 )
  83
  84 /**
  85  * DAO for UnifiedEntryEntity operations.
  86  */
  87 @Dao
  88 interface UnifiedEntryDao {
  89     @Insert
  90     suspend fun insertEntry(entry: UnifiedEntryEntity): Long
  91
  92     @Insert
  93     suspend fun insertEntries(entries: List<UnifiedEntryEntity>)
  94
  95     @Update
  96     suspend fun updateEntry(entry: UnifiedEntryEntity)
  97
  98     @Delete
  99     suspend fun deleteEntry(entry: UnifiedEntryEntity)
 100
 101     @Query("SELECT * FROM unified_entries WHERE phoneNumber = :phoneNumber AND action = 'ALLOW' LIMIT 1")
 102     suspend fun findUnifiedAllowEntry(phoneNumber: String): UnifiedEntryEntity?
 103
 104     @Query("SELECT * FROM unified_entries WHERE phoneNumber = :phoneNumber AND action = 'BLOCK' LIMIT 1")
 105     suspend fun findUnifiedBlockEntry(phoneNumber: String): UnifiedEntryEntity?
 106
 107     @Query("SELECT * FROM unified_entries WHERE phoneNumber = :phoneNumber")
 108     suspend fun findEntriesByPhoneNumber(phoneNumber: String): List<UnifiedEntryEntity>
 109
 110     @Query("SELECT * FROM unified_entries WHERE sourceId = :sourceId")
 111     suspend fun findEntriesBySourceId(sourceId: Int): List<UnifiedEntryEntity>
 112
 113     @Query("SELECT COUNT(*) FROM unified_entries WHERE sourceId = :sourceId")
 114     suspend fun getEntryCountBySourceId(sourceId: Int): Int
 115
 116     @Query("SELECT COUNT(*) FROM unified_entries WHERE sourceId = :sourceId")
 117     fun observeEntryCountBySourceId(sourceId: Int): Flow<Int>
 118
 119     @Query("DELETE FROM unified_entries WHERE sourceId = :sourceId")
 120     suspend fun deleteEntriesBySourceId(sourceId: Int)
 121
 122     @Query("SELECT * FROM unified_entries WHERE isPattern = 1 AND action = 'BLOCK'")
 123     suspend fun getAllBlockPatterns(): List<UnifiedEntryEntity>
 124
 125     @Query("SELECT COUNT(*) FROM unified_entries")
 126     fun getTotalEntryCount(): Flow<Int>
 127
 128     @Query("SELECT * FROM unified_entries")
 129     suspend fun getAllEntries(): List<UnifiedEntryEntity>
 130
 131     /**
 132      * Paged phoneNumber/isPattern projection, ordered by id, for Bloom filter
 133      * rehydration (see DataSourceRepository.rehydrateBloomFilters()).
 134      *
 135      * getAllEntries() above materializes the entire table — every column, of
 136      * every row — as one in-memory List before a caller can process a single
 137      * element. At the 500,000-row capacity BloomFilterEngine is sized for,
 138      * that one-shot allocation is what was showing up as sustained young-GC
 139      * churn and native-alloc pressure during startup and after every rule
 140      * change. This query lets the caller pull the table LIMIT/OFFSET-style,
 141      * a bounded page at a time, so peak heap stays flat regardless of table
 142      * size — the same streaming approach SecureCsvParser already uses for
 143      * large source imports rather than reading a whole file into memory.
 144      *
 145      * ORDER BY id keeps paging stable across calls: rows already paged past
 146      * keep their position even if a write lands mid-rehydration, so a new
 147      * row either appears in a later page or not at all this pass — it can
 148      * never cause an already-read row to be skipped or duplicated.
 149      */
 150     @Query("SELECT phoneNumber, isPattern FROM unified_entries ORDER BY id LIMIT :limit OFFSET :offset")
 151     suspend fun getPhoneNumberPatternPage(limit: Int, offset: Int): List<PhoneNumberPatternProjection>
 152
 153     @Query("SELECT * FROM unified_entries WHERE sourceId = :sourceId")
 154     suspend fun getAllBySource(sourceId: Int): List<UnifiedEntryEntity>
 155
 156     @Query("DELETE FROM unified_entries WHERE phoneNumber = :phoneNumber AND sourceId = :sourceId")
 157     suspend fun deleteEntryByNumberAndSource(phoneNumber: String, sourceId: Int)
 158
 159     /**
 160      * Phase 2.6 — Priority-based conflict resolution.
 161      *
 162      * Returns all entries for a given phone number joined with their source's
 163      * priority, ordered highest priority first. When multiple sources disagree
 164      * on a number (one BLOCK, one ALLOW), the caller takes the first row —
 165      * the highest-priority source wins. MANUAL source is seeded at priority 100
 166      * by DatabaseInitializer, so user decisions always override external sources.
 167      *
 168      * The join is on unified_entries.sourceId = sources.id. The query uses
 169      * a LEFT JOIN so entries with a missing source (shouldn't happen due to FK
 170      * CASCADE, but defensive) still appear with NULL priority — COALESCE
 171      * treats them as priority 0, which loses every conflict, which is correct.
 172      */
 173     @Query("""
 174         SELECT ue.* FROM unified_entries ue
 175         LEFT JOIN sources s ON ue.sourceId = s.id
 176         WHERE ue.phoneNumber = :phoneNumber
 177           AND s.isEnabled = 1
 178         ORDER BY COALESCE(s.priority, 0) DESC, ue.id ASC
 179     """)
 180     suspend fun findEntriesByPhoneNumberWithPriority(phoneNumber: String): List<UnifiedEntryEntity>
 181
 182     /**
 183      * Phase 2.6 — Priority-ordered matching pattern lookup.
 184      *
 185      * The normalized number is the complete value and phoneNumber is the
 186      * candidate prefix, so the LIKE direction intentionally reads
 187      * `:normalized LIKE ue.phoneNumber || '%'`. Filtering in SQLite keeps the
 188      * screening hot path from materializing every pattern in Kotlin.
 189      */
 190     @Query("""
 191         SELECT ue.* FROM unified_entries ue
 192         LEFT JOIN sources s ON ue.sourceId = s.id
 193         WHERE ue.isPattern = 1
 194           AND ue.action = 'BLOCK'
 195           AND s.isEnabled = 1
 196           AND :normalized LIKE ue.phoneNumber || '%'
 197         ORDER BY COALESCE(s.priority, 0) DESC, ue.id ASC
 198     """)
 199     suspend fun findMatchingBlockPatternsWithPriority(normalized: String): List<UnifiedEntryEntity>
 200 }
 201
 202 /**
 203  * DAO for CallLogEntry operations.
 204  */
 205 @Dao
 206 interface CallLogDao {
 207     @Insert
 208     suspend fun insertCallLog(callLog: CallLogEntry): Long
 209
 210     @Query("SELECT * FROM call_log ORDER BY timestamp DESC LIMIT :limit")
 211     fun getRecentCalls(limit: Int = 100): Flow<List<CallLogEntry>>
 212
 213     @Query("SELECT * FROM call_log WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC LIMIT :limit")
 214     suspend fun getCallsByPhoneNumber(phoneNumber: String, limit: Int = 10): List<CallLogEntry>
 215
 216     @Query("SELECT COUNT(*) FROM call_log WHERE decision = 'BLOCK' AND timestamp >= :startTime")
 217     suspend fun getBlockedCallsCount(startTime: Long): Int
 218
 219     @Query("SELECT COUNT(*) FROM call_log WHERE timestamp >= :startTime AND timestamp <= :endTime")
 220     suspend fun getCallsInRange(startTime: Long, endTime: Long): Int
 221
 222     @Query("DELETE FROM call_log WHERE timestamp < :timestamp")
 223     suspend fun deleteOldCallLogs(timestamp: Long)
 224
 225     @Update
 226     suspend fun updateCallLog(callLog: CallLogEntry)
 227
 228     @Delete
 229     suspend fun deleteCallLog(callLog: CallLogEntry)
 230 }
 231
 232 /**
 233  * DAO for SettingEntry operations.
 234  */
 235 @Dao
 236 interface SettingDao {
 237     @Insert
 238     suspend fun insertSetting(setting: SettingEntry): Long
 239
 240     @Update
 241     suspend fun updateSetting(setting: SettingEntry)
 242
 243     @Query("SELECT * FROM settings WHERE key = :key")
 244     suspend fun getSettingByKey(key: String): SettingEntry?
 245
 246     @Query("SELECT value FROM settings WHERE key = :key")
 247     suspend fun getSettingValue(key: String): String?
 248
 249     @Query("UPDATE settings SET value = :value, updatedAt = :timestamp WHERE key = :key")
 250     suspend fun updateSettingValue(key: String, value: String, timestamp: Long = System.currentTimeMillis())
 251
 252     @Query("SELECT * FROM settings")
 253     suspend fun getAllSettings(): List<SettingEntry>
 254 }
 255
 256 /**
 257  * DAO for SyncHistoryEntry operations.
 258  */
 259 @Dao
 260 interface SyncHistoryDao {
 261     @Insert
 262     suspend fun insertSyncHistory(syncHistory: SyncHistoryEntry): Long
 263
 264     @Query("SELECT * FROM sync_history WHERE sourceId = :sourceId ORDER BY timestamp DESC LIMIT :limit")
 265     suspend fun getSyncHistoryBySourceId(sourceId: Int, limit: Int = 10): List<SyncHistoryEntry>
 266
 267     @Query("SELECT * FROM sync_history ORDER BY timestamp DESC LIMIT :limit")
 268     suspend fun getRecentSyncHistory(limit: Int = 50): List<SyncHistoryEntry>
 269
 270     @Query("DELETE FROM sync_history WHERE timestamp < :timestamp")
 271     suspend fun deleteOldSyncHistory(timestamp: Long)
 272 }
 273
 274 /**
 275  * DAO for PendingCardEntity operations.
 276  * Cards are created when the decision contract requires review (currently
 277  * HEURISTIC_BLOCK or HEURISTIC_FLAG) and deleted on dismissal. Never used for
 278  * permanent history — that is CallLogDao's job.
 279  */
 280 @Dao
 281 interface PendingCardDao {
 282     @Insert
 283     suspend fun insertCard(card: PendingCardEntity): Long
 284
 285     @Query("SELECT * FROM pending_cards WHERE dismissed = 0 ORDER BY timestamp DESC")
 286     fun getUndismissedCards(): Flow<List<PendingCardEntity>>
 287
 288     @Query("UPDATE pending_cards SET dismissed = 1 WHERE id = :cardId")
 289     suspend fun dismissCard(cardId: Int)
 290
 291     /**
 292      * Dismisses all undismissed cards matching a phone number.
 293      * Used by the ACTION_NOT_SPAM broadcast so CallActionReceiver can dismiss
 294      * the card inline without needing the card's primary key.
 295      */
 296     @Query("UPDATE pending_cards SET dismissed = 1 WHERE phoneNumber = :phoneNumber AND dismissed = 0")
 297     suspend fun dismissByPhoneNumber(phoneNumber: String)
 298
 299     @Query("DELETE FROM pending_cards WHERE id = :cardId")
 300     suspend fun deleteCard(cardId: Int)
 301
 302     @Query("DELETE FROM pending_cards WHERE dismissed = 1")
 303     suspend fun deleteAllDismissed()
 304
 305     @Query("DELETE FROM pending_cards")
 306     suspend fun deleteAll()
 307
 308     @Query("SELECT COUNT(*) FROM pending_cards WHERE dismissed = 0")
 309     fun getUndismissedCount(): Flow<Int>
 310 }
```

### `android/app/src/main/java/com/signalgate/pulse/database/SignalGateDatabase.kt`

```text
   1 package com.signalgate.pulse.database
   2
   3 import androidx.room.Database
   4 import com.signalgate.pulse.StartupDiagnostics
   5 import androidx.room.RoomDatabase
   6 import androidx.room.migration.Migration
   7 import androidx.sqlite.db.SupportSQLiteDatabase
   8 import com.signalgate.pulse.database.daos.CallLogDao
   9 import com.signalgate.pulse.database.daos.PendingCardDao
  10 import com.signalgate.pulse.database.daos.SettingDao
  11 import com.signalgate.pulse.database.daos.SourceDao
  12 import com.signalgate.pulse.database.daos.SyncHistoryDao
  13 import com.signalgate.pulse.database.daos.UnifiedEntryDao
  14 import com.signalgate.pulse.database.entities.CallLogEntry
  15 import com.signalgate.pulse.database.entities.PendingCardEntity
  16 import com.signalgate.pulse.database.entities.SettingEntry
  17 import com.signalgate.pulse.database.entities.SourceEntity
  18 import com.signalgate.pulse.database.entities.SyncHistoryEntry
  19 import com.signalgate.pulse.database.entities.UnifiedEntryEntity
  20
  21 /**
  22  * SignalGateDatabase — Room database abstract class for SignalGate Pulse.
  23  *
  24  * Construction is exclusively managed by SecureDatabase via Koin injection.
  25  * Do NOT instantiate this class directly.
  26  *
  27  * exportSchema = true (changed from false in Phase 2.6):
  28  * Schema JSON files are required by Room's MigrationTestHelper to validate
  29  * that MIGRATION_1_2's DDL exactly matches what Room would generate from the
  30  * current entity definitions. Without schema export, MigrationTestHelper cannot
  31  * run and migration drift goes undetected until a user's device hits it.
  32  * Schema files are written to schemas/ at the project root (see build.gradle
  33  * ksp arg room.schemaLocation). They should be committed to version control —
  34  * they are the ground truth for what each version's schema looked like.
  35  *
  36  * Schema version 4: Phase 3.4 explicit source lifecycle and snapshot metadata.
  37  *
  38  * Version 3 remains the Phase 0.4 source activation timestamp schema; version 4
  39  * adds lifecycle state, snapshot version/hash, accepted record count, and a
  40  * bounded failure/rejection reason without backfilling unverifiable metadata.
  41  */
  42 @Database(
  43     entities = [
  44         SourceEntity::class,
  45         UnifiedEntryEntity::class,
  46         CallLogEntry::class,
  47         SettingEntry::class,
  48         SyncHistoryEntry::class,
  49         PendingCardEntity::class,
  50     ],
  51     version = 4,
  52     exportSchema = true
  53 )
  54 abstract class SignalGateDatabase : RoomDatabase() {
  55     abstract fun sourceDao(): SourceDao
  56     abstract fun unifiedEntryDao(): UnifiedEntryDao
  57     abstract fun callLogDao(): CallLogDao
  58     abstract fun settingDao(): SettingDao
  59     abstract fun syncHistoryDao(): SyncHistoryDao
  60     abstract fun pendingCardDao(): PendingCardDao
  61 }
  62
  63 /**
  64  * Migration 1 -> 2: Adds the pending_cards table for the post-call digest card system.
  65  *
  66  * DDL is intentionally explicit (no shorthand) so MigrationTestHelper can
  67  * validate column types and defaults exactly against the exported schema.
  68  * Any drift between this DDL and PendingCardEntity's generated schema will
  69  * surface in MigrationTest.kt before reaching a device.
  70  */
  71 val MIGRATION_1_2 = object : Migration(1, 2) {
  72     override fun migrate(db: SupportSQLiteDatabase) {
  73         StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_1_2_BEGIN)
  74         db.execSQL(
  75             """
  76             CREATE TABLE IF NOT EXISTS pending_cards (
  77                 id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  78                 phoneNumber TEXT NOT NULL,
  79                 timestamp INTEGER NOT NULL,
  80                 decision TEXT NOT NULL,
  81                 confidence INTEGER,
  82                 decisionSource TEXT,
  83                 dismissed INTEGER NOT NULL DEFAULT 0
  84             )
  85             """.trimIndent()
  86         )
  87         db.execSQL(
  88             "CREATE INDEX IF NOT EXISTS index_pending_cards_dismissed ON pending_cards (dismissed)"
  89         )
  90         db.execSQL(
  91             "CREATE INDEX IF NOT EXISTS index_pending_cards_timestamp ON pending_cards (timestamp)"
  92         )
  93         StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_1_2_END)
  94     }
  95 }
  96
  97 /**
  98  * Migration 2 -> 3: adds the minimum Phase 0.4 last-known-good metadata.
  99  * Existing sources receive NULL for both fields, correctly representing that
 100  * no attempt or accepted snapshot has occurred since this migration.
 101  */
 102 val MIGRATION_2_3 = object : Migration(2, 3) {
 103     override fun migrate(db: SupportSQLiteDatabase) {
 104         StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_2_3_BEGIN)
 105         db.execSQL("ALTER TABLE sources ADD COLUMN last_attempted_sync INTEGER")
 106         db.execSQL("ALTER TABLE sources ADD COLUMN last_accepted_snapshot INTEGER")
 107         StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_2_3_END)
 108     }
 109 }
 110
 111 /**
 112  * Migration 3 -> 4: adds explicit source lifecycle and accepted-snapshot metadata.
 113  * Existing rows start ENABLED with unknown snapshot metadata; no historical
 114  * acceptance or failure facts are invented during migration.
 115  */
 116 val MIGRATION_3_4 = object : Migration(3, 4) {
 117     override fun migrate(db: SupportSQLiteDatabase) {
 118         StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_3_4_BEGIN)
 119         db.execSQL("ALTER TABLE sources ADD COLUMN lifecycle_state TEXT NOT NULL DEFAULT 'ENABLED'")
 120         db.execSQL("ALTER TABLE sources ADD COLUMN snapshot_version TEXT")
 121         db.execSQL("ALTER TABLE sources ADD COLUMN snapshot_hash TEXT")
 122         db.execSQL("ALTER TABLE sources ADD COLUMN accepted_record_count INTEGER")
 123         db.execSQL("ALTER TABLE sources ADD COLUMN lifecycle_reason TEXT")
 124         StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_3_4_END)
 125     }
 126 }
```

### `android/app/src/main/java/com/signalgate/pulse/database/SecureDatabase.kt`

```text
   1 package com.signalgate.pulse.database
   2
   3 import android.content.Context
   4 import androidx.room.Room
   5 import androidx.room.RoomDatabase
   6 import com.signalgate.pulse.StartupDiagnostics
   7 import com.signalgate.pulse.security.DatabaseResetEvent
   8 import com.signalgate.pulse.security.KeystoreInvalidatedException
   9 import com.signalgate.pulse.security.SecurityUtils
  10 import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
  11 import timber.log.Timber
  12 import java.util.concurrent.Executors
  13
  14 object SecureDatabase {
  15     private const val DB_NAME = "secure_signal.db"
  16
  17     init {
  18         // Required for net.zetetic:sqlcipher-android (unlike the legacy
  19         // net.sqlcipher.database library, this artifact does NOT auto-load its
  20         // native library via SQLiteDatabase.loadLibs()). Without this call,
  21         // SQLiteConnection.nativeOpen() has no native implementation bound and
  22         // every DB open throws UnsatisfiedLinkError, crash-looping the app.
  23         System.loadLibrary("sqlcipher")
  24     }
  25
  26     fun getDatabase(context: Context): SignalGateDatabase {
  27         StartupDiagnostics.mark(StartupDiagnostics.Event.SQLCIPHER_INIT_BEGIN)
  28         // Updated per Architecture Contract Step 0.1:
  29         // Replaced hardcoded passphrase with Android Keystore derivation.
  30         val passphrase = try {
  31             SecurityUtils.getDatabasePassphrase(context)
  32         } catch (e: KeystoreInvalidatedException) {
  33             // The existing DB is unreadable — the wrapped passphrase can no longer be
  34             // decrypted (Keystore key invalidated, or the wrapped-passphrase blob is
  35             // corrupt). Log unconditionally (this must be visible in release builds,
  36             // not just debug — see MainApplication's release Timber tree), delete the
  37             // now-orphaned file so it doesn't linger as a dead, inaccessible artifact,
  38             // generate a fresh passphrase, and tell the UI layer so the user isn't left
  39             // wondering why their blocklist/history is suddenly empty.
  40             Timber.e(e, "Database passphrase could not be decrypted — resetting local database")
  41             context.getDatabasePath(DB_NAME).let { dbFile ->
  42                 dbFile.delete()
  43                 context.getDatabasePath("$DB_NAME-wal").delete()
  44                 context.getDatabasePath("$DB_NAME-shm").delete()
  45             }
  46             DatabaseResetEvent.signal()
  47             SecurityUtils.resetDatabasePassphrase(context)
  48         }
  49
  50         // Known sqlcipher-android quirk (zetetic/sqlcipher-android issue #4):
  51         // loading the native lib in an init{} block / Application.onCreate() is
  52         // sometimes NOT sufficient — the SupportOpenHelperFactory needs the
  53         // load call to happen in close proximity to its own construction.
  54         // System.loadLibrary is idempotent, so this second call is a safe,
  55         // cheap no-op on runs where the init{} block already loaded it, and a
  56         // real fix on the runs where it didn't.
  57         System.loadLibrary("sqlcipher")
  58
  59         val factory = SupportOpenHelperFactory(passphrase)
  60         StartupDiagnostics.mark(StartupDiagnostics.Event.SQLCIPHER_FACTORY_READY)
  61
  62         // Previously a single Executors.newSingleThreadExecutor() instance was
  63         // passed to BOTH setQueryExecutor() and setTransactionExecutor(). That
  64         // is a documented Room anti-pattern for two reasons:
  65         //
  66         //   1. Deadlock risk: if a transaction running on that thread needs to
  67         //      dispatch a query, and the query executor IS that same one
  68         //      thread, the query has nowhere to run — it queues behind the
  69         //      transaction that's waiting on it.
  70         //   2. Total serialization: every read AND write in the entire app —
  71         //      dashboard counters, source syncs, Bloom rehydration's paged
  72         //      reads, manual rule writes, and every getCallDecision() lookup
  73         //      from a live incoming call — funneled through one thread. A
  74         //      call arriving while that thread was occupied by unrelated work
  75         //      would have its decision query queued behind it, silently
  76         //      eating into (or blowing past) processScreeningCall()'s 3.5s
  77         //      withTimeout with no error until the timeout itself fired.
  78         //
  79         // Query executor: a small fixed pool so concurrent reads (e.g. a
  80         // dashboard counter query and a getCallDecision() lookup) don't
  81         // serialize behind each other. Transaction executor: kept as its own
  82         // single thread — Room transactions are already serialized by SQLite
  83         // itself, but this thread is now independent of the query pool above,
  84         // which is what actually removes the deadlock risk.
  85         val queryExecutor = Executors.newFixedThreadPool(4)
  86         val transactionExecutor = Executors.newSingleThreadExecutor()
  87         StartupDiagnostics.mark(StartupDiagnostics.Event.ROOM_OPEN_MIGRATION_BEGIN)
  88
  89         return Room.databaseBuilder(context, SignalGateDatabase::class.java, DB_NAME)
  90             .addCallback(object : RoomDatabase.Callback() {
  91                 override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
  92                     super.onOpen(db)
  93                     StartupDiagnostics.mark(StartupDiagnostics.Event.ROOM_OPEN_MIGRATION_END)
  94                 }
  95             })
  96             .openHelperFactory(factory)
  97             .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
  98             .setQueryExecutor(queryExecutor)
  99             .setTransactionExecutor(transactionExecutor)
 100             .build()
 101     }
 102 }
```

Database version is 4 and `exportSchema=true` (`SignalGateDatabase.kt:42-53`); migrations are 1→2, 2→3, and 3→4 (`:63-126`); six entities and six DAO accessors are declared (`:42-61`) [VERIFIED-CODE].

**4 Room source files listed / 4 found by explicit Room source list.**

---

## Amendment A — Sync

### Endpoints and source policies

| Source | Endpoint(s) | Fetch/authenticity behavior | Evidence |
|---|---|---|---|
| FTC | Primary mirror JSON: `https://raw.githubusercontent.com/avignonbo-jpg/signalgate-dnc-mirror/dnc-mirror-pulse/dnc-numbers.json`; detached manifest is the same URL plus `.manifest.json`; fallback: `https://www.ftc.gov/system/files/ftc_gov/DNC_Complaint_Numbers.csv` | The FTC path fetches the mirror body and manifest, parses the manifest, then verifies the body using `ArtifactAuthenticityVerifier`; the verifier checks algorithm, SHA-256 payload hash, signed-at freshness/future skew, and detached ECDSA P-256 signature. | `android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:103-105,129-134,377-388` [VERIFIED-CODE]; `android/app/src/main/java/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifier.kt:12-18,20-24,72-122` [VERIFIED-CODE] |
| FCC | Primary: `https://opendata.fcc.gov/api/views/vakf-fz8e/rows.csv?accessType=DOWNLOAD`; fallback: `https://opendata.fcc.gov/api/views/vakf-fz8e/rows.csv?$limit=50000` | FCC uses CSV over HTTPS and does not use the first-party detached-signature verifier; the implementation checks HTTP success, UTF-8, and the required `Caller ID Number` header. | `android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:107-110,433-469` [VERIFIED-CODE]; `android/app/src/main/java/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifier.kt:12-18` [VERIFIED-CODE] |

### Fetch, validation, and failure path

1. `fetchWithFallback()` tries primary then fallback URLs in order; an empty result or exception advances to the next URL, and all failures produce a sync failure rather than an accepted snapshot (`ReliableSourceManager.kt:348-370`) [VERIFIED-CODE].
2. FTC records are canonicalized through `SourceRecordValidator`, rejects manager-level values outside 10–15 characters, deduplicates, and retains bounded records (`ReliableSourceManager.kt:377-426`) [VERIFIED-CODE]. `SourceRecordValidator` sanitizes and accepts only canonical values with length 7–15 at its own boundary (`SourceRecordValidator.kt:3-23`) [VERIFIED-CODE].
3. FCC records use `streamColumnByHeader(..., "Caller ID Number")`, then the same canonicalization, malformed-record, duplicate, and 50,000-entry bounds (`ReliableSourceManager.kt:429-469`) [VERIFIED-CODE].
4. `SnapshotSanityValidator` rejects unsupported/missing content type, non-UTF-8 encoding, empty/oversized bodies, record-count violations, field-length violations, malformed ratios, stale snapshots, and catastrophic accepted-count changes; it returns explicit rejection reasons (`SnapshotSanityValidator.kt:44-92`) [VERIFIED-CODE].
5. The accepted candidate is converted to BLOCK entries with FTC confidence 85 or FCC confidence 70 and passed to `SecurityRuleRepository.replaceSourceSnapshot()` (`ReliableSourceManager.kt:294-317`) [VERIFIED-CODE].
6. Atomic activation records the attempt separately, rejects empty candidates before mutation, deletes old entries/inserts the candidate/records accepted metadata inside `database.withTransaction`, and rebuilds Bloom state only after commit; failures record failure state and preserve the last-known-good path (`SecurityRuleRepository.kt:185-247`) [VERIFIED-CODE]. The batched variant applies the same transaction boundary (`SecurityRuleRepository.kt:250-310`) [VERIFIED-CODE].

### Task 3 sync state

- `syncMutex` serializes the private sync body (`ReliableSourceManager.kt:191-194`) [VERIFIED-CODE].
- `inFlightSyncCount` is a `MutableStateFlow(0)` (`ReliableSourceManager.kt:195`) [VERIFIED-CODE].
- `trackSyncing` increments before work and decrements in `finally`, so the count is restored on success, failure, or cancellation (`ReliableSourceManager.kt:201-207`) [VERIFIED-CODE].
- `isSyncing` is a distinct Boolean flow derived from `inFlightSyncCount > 0` (`ReliableSourceManager.kt:197-199`) [VERIFIED-CODE].
- `syncAllFederalSources()` and `syncSource(sourceId)` are wrapped in `trackSyncing` and use `Dispatchers.IO` (`ReliableSourceManager.kt:228-249`) [VERIFIED-CODE].
- The Sources UI collects `isSyncing`, disables the Sync All button while true, and shows a progress indicator (`SourcesScreen.kt:33-37,47-66`) [VERIFIED-CODE].

**Sync evidence summary:** 2 federal sources listed / 2 found by `rg -n 'FTC_API_BASE|FCC_PRIMARY_URL|FederalSource' ReliableSourceManager.kt`; 4 Task 3 state symbols listed / 4 found by `rg -n 'inFlightSyncCount|trackSyncing|syncMutex|isSyncing' ReliableSourceManager.kt`.

---

# Source Matrix

| Source | Name/type/pathOrUrl | Creates | Enables | Disables | Deletes | Syncs | Evidence |
|---|---|---|---|---|---|---|---|
| FTC | FTC Do Not Call; type FTC; endpoint constants `android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:103-105` | `ReliableSourceManager.ensureFederalRows()` / `DatabaseInitializer` seed path (`android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:551-560`; `android/app/src/main/java/com/signalgate/pulse/database/DatabaseInitializer.kt:23-30`) | source row default `true`; source toggle path (`android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:556-560`; `DataSourceRepository.kt:154-155`) | `SourcesScreen` switch for non-manual protected rows (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt:101-150`) | protected source deletion refused by repository (`DataSourceRepository.kt:72-93,132-155`) | `syncAllFederalSources` / worker (`android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:220-241`; `CommunitySyncWorker.kt:38`) | [VERIFIED-CODE] |
| FCC | FCC Consumer Complaints; type FCC; primary/fallback URLs `android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:107-110` | same federal-row path (`android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt:551-560`) | same enabled default/toggle path | same | same | same | [VERIFIED-CODE] |
| Manual User Rules | name “Manual User Rules”; type MANUAL; path `local` (`android/app/src/main/java/com/signalgate/pulse/database/DatabaseInitializer.kt:23-30`) | `DatabaseInitializer.seedRequiredSources()` | seeded enabled; current screen shows read-only state and “Manage elsewhere” (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt:101-144`) | no current UI toggle for this row; behavior otherwise UNKNOWN [UNKNOWN] | protected deletion refused | no federal sync path; manual mutations via `SecurityRuleRepository` | [VERIFIED-CODE/UNKNOWN] |
| Contacts Allow List | name “Contacts Allow List”; type MANUAL; path `contacts` (`android/app/src/main/java/com/signalgate/pulse/database/DatabaseInitializer.kt:32-38`) | `DatabaseInitializer`; entries from `ContactsViewModel.saveSelectedToAllowList()` (`android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/ContactsViewModel.kt:91-142`) | current screen retains a Switch (`android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt:145-150`) | same Switch path | protected deletion refused | no remote sync; contact import writes authoritative entries | [VERIFIED-CODE] |

The source list is fixed in `ReliableSourceManager.kt` and the UI source model is documented in `SourcesScreen.kt:26-29`. [VERIFIED-CODE]

**4 source rows listed / 4 found by reading `DatabaseInitializer.kt`, `ReliableSourceManager.kt`, `SourcesScreen.kt`, and `ContactsViewModel.kt`.**

---

## Amendment C — Manual User Rules UI State

Manual User Rules has **no UI toggle** in the current implementation. The source model identifies Manual User Rules as read-only enabled status with a management hint, while Contacts Allow List retains the enable/disable control (`SourcesScreen.kt:20-29`) [VERIFIED-CODE]. In the row rendering, `showReadOnlyStatus` renders text and a hint; the `Switch` is only rendered in the `else if (controls.showSwitch)` branch (`SourcesScreen.kt:133-155`) [VERIFIED-CODE]. The prior source-matrix row is therefore amended to: **Manual User Rules — seeded/protected source; no UI enable/disable toggle; management behavior outside this screen is UNKNOWN.** (`SourcesScreen.kt:26-29,133-155`; `DataSourceRepository.kt:78-83,146-160`) [VERIFIED-CODE/UNKNOWN].

---

# Screening Trace

| Step | Dispatcher/thread | Timeout | Failure behavior | Evidence |
|---|---|---|---|---|
| Telecom entry and handle extraction | framework callback; launches `serviceScope` on `Dispatchers.Default.limitedParallelism(4)` | none before launch | null/blank handle immediately calls `handleSecurityFailure` with `UNKNOWN_MALFORMED_HANDLE` | `SignalGateCallScreeningService.kt:57-59,67-93` [VERIFIED-CODE] |
| Dependency resolution/markers | service scope | before measured decision | exception is caught by `executeScreeningSafely` and maps to SECURITY_FAILURE | `SignalGateCallScreeningService.kt:97-108,135-148` [VERIFIED-CODE] |
| Decision | service scope | `withTimeout(3_500)` around `screen()` | timeout or exception calls `onSecurityFailure(phoneNumber)` | `SignalGateCallScreeningService.kt:161-172,140-147` [VERIFIED-CODE] |
| Telecom response | same coroutine after decision | follows 3,500 ms decision bound | response emitted before persistence/UX; response mapping is explicit | `SignalGateCallScreeningService.kt:174-177,223-260` [VERIFIED-CODE] |
| Persistence | `withContext(NonCancellable)` | `withTimeoutOrNull(2_000)` | timeout logs `AUDIT_PERSIST_TIMEOUT`; exception logs; no second response | `SignalGateCallScreeningService.kt:178-213` [VERIFIED-CODE] |
| UX | same service scope after persistence | none shown | exception logged as best effort; decision/response already complete | `SignalGateCallScreeningService.kt:215-220,311-328` [VERIFIED-CODE] |

**Where the 3,500 ms clock starts:** immediately at `processScreeningCall` line 171 around the injected `screen()` call. Dependency resolution, startup markers, and launch occur before that call (`:97-108`), but are outside the `withTimeout(3_500)` block. [VERIFIED-CODE]

**SECURITY_FAILURE mapping:** `responsePolicy` groups `ALLOW`, `SCREEN`, and `SECURITY_FAILURE` into `disallowCall=false`, `silenceCall=false`, `skipCallLog=false`, `skipNotification=false` (`SignalGateCallScreeningService.kt:235-242`); `handleSecurityFailure` writes a separate `SECURITY_FAILURE` audit entry asynchronously (`:269-308`). [VERIFIED-CODE]

**Startup initialization failure:** `MainApplication.onCreate()` and `AppModule.initializeDatabase` are the current startup paths; the exact catch/fallback behavior is recorded in `MainApplication.kt` and `AppModule.kt` and is not reproduced here without a complete line-by-line startup read, therefore the final startup-init failure outcome is UNKNOWN. [UNKNOWN]

**6 screening steps listed / 6 found by reading `SignalGateCallScreeningService.kt`.**

---

# Workers, Notification Channels, and Settings

## Workers

- `android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt` — see source lines for `Worker`/receiver declarations and scheduling [VERIFIED-CODE].
- `android/app/src/main/java/com/signalgate/pulse/workers/SyncBootReceiver.kt` — see source lines for `Worker`/receiver declarations and scheduling [VERIFIED-CODE].

## Notification channels

```text
   1 package com.signalgate.pulse.ui.notifications
   2
   3 import android.app.NotificationChannel
   4 import android.app.NotificationManager
   5 import android.content.Context
   6 import timber.log.Timber
   7
   8 /**
   9  * NotificationChannelManager — Phase 4.8.
  10  *
  11  * Central channel registry, called from MainApplication.onCreate() during startup.
  12  * SignalGateCallScreeningService also re-registers BLOCKED_CALL_REVIEW immediately
  13  * before posting; keep that duplicate definition aligned with this one.
  14  * Re-registering an existing channel ID is a safe no-op — user settings preserved.
  15  *
  16  * BLOCKED_CALL_REVIEW: HIGH — Tier 3 heuristic blocks. User needs prompt visibility
  17  *   for false-positive recovery. Sound/vibration off (call just ended).
  18  * SYNC_STATUS: DEFAULT — Reserved for possible future background-sync
  19  *   progress/completion notifications. The current CommunitySyncWorker does not post
  20  *   notifications or use foreground-work execution.
  21  * SECURITY_ALERT: HIGH — Reserved for possible future alerts about call-screening
  22  *   role loss or critical permission revocation. No worker or notification-posting
  23  *   path currently uses this channel.
  24  */
  25 object NotificationChannelManager {
  26
  27     const val CHANNEL_BLOCKED_CALL_REVIEW = "blocked_call_review"
  28     const val CHANNEL_SYNC_STATUS         = "sync_status"
  29     const val CHANNEL_SECURITY_ALERT      = "security_alert"
  30
  31     private const val TAG = "NotificationChannelMgr"
  32
  33     fun createAllChannels(context: Context) {
  34         val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  35         nm.createNotificationChannels(listOf(
  36             blockedCallReviewChannel(),
  37             syncStatusChannel(),
  38             securityAlertChannel()
  39         ))
  40         Timber.tag(TAG).d("All notification channels registered")
  41     }
  42
  43     private fun blockedCallReviewChannel() = NotificationChannel(
  44         CHANNEL_BLOCKED_CALL_REVIEW, "Blocked Call Review", NotificationManager.IMPORTANCE_HIGH
  45     ).apply {
  46         description = "Review calls blocked by SignalGate Pulse"
  47         setShowBadge(true); enableVibration(false); setSound(null, null)
  48     }
  49
  50     private fun syncStatusChannel() = NotificationChannel(
  51         CHANNEL_SYNC_STATUS, "Sync Status", NotificationManager.IMPORTANCE_DEFAULT
  52     ).apply {
  53         description = "Background sync progress for federal blocklist sources"
  54         setShowBadge(false); enableVibration(false); setSound(null, null)
  55     }
  56
  57     private fun securityAlertChannel() = NotificationChannel(
  58         CHANNEL_SECURITY_ALERT, "Security Alert", NotificationManager.IMPORTANCE_HIGH
  59     ).apply {
  60         description = "Critical alerts — call screening role lost or permission revoked"
  61         setShowBadge(true); enableVibration(true)
  62     }
  63 }
```

## Settings

```text
   1 // DO NOT let this file's package/imports revert to com.signalgate.multipoint.
   2 // This file was added to consumer-v1 independently of the 2026-08-14/15
   3 // pulse-package-rename merge, so it was never run through that rename script.
   4 // A prior CI run caught it still declaring "package com.signalgate.multipoint...",
   5 // which broke compilation for every file that depends on this one (see
   6 // PROJECT_LEDGER.md, 2026-08-14/15 entry). If this file is ever regenerated,
   7 // restored from a backup/snapshot, or reintroduced via a future merge, verify
   8 // its package and every import still say com.signalgate.pulse before trusting it,
   9 // even if it lands with no conflict markers — "no conflict" is not the same as
  10 // "correct," as this incident showed.
  11 package com.signalgate.pulse.database.repositories
  12
  13 /**
  14  * SettingKeys — centralizes the SettingEntry key strings for values migrated
  15  * off SharedPreferences under Step 2.6.
  16  *
  17  * Small on purpose: this exists so a key name is a compile-time-checked
  18  * constant reference, not a repeated raw string prone to typos across
  19  * OnboardingViewModel, DashboardViewModel, and SettingsViewModel. Add to this
  20  * object as further keys migrate — don't reintroduce raw string literals at
  21  * new call sites once a key has an entry here.
  22  *
  23  * EULA acceptance is also stored here so the legal acceptance record uses the
  24  * same durable key-value repository as the rest of onboarding. Its separate
  25  * keys preserve independent lifecycle semantics: a future terms-version bump
  26  * can force re-acceptance without re-running the whole wizard.
  27  */
  28 object SettingKeys {
  29     const val ONBOARDING_COMPLETE = "onboarding_complete"
  30     const val EULA_ACCEPTED = "eula_accepted"
  31     const val EULA_VERSION = "eula_version"
  32     const val EULA_CURRENT_VERSION = "placeholder-v0"
  33     const val EULA_ACCEPTED_AT = "eula_accepted_at"
  34     const val SHIELD_RED = "shield_red"
  35     const val SHIELD_GREEN = "shield_green"
  36     const val SHIELD_BLUE = "shield_blue"
  37
  38     /**
  39      * On-device gray-zone heuristics protection level, set on onboarding Step 3
  40      * and editable later from Settings. Value is one of HeuristicsMode's `key`
  41      * strings ("OFF" / "CONSERVATIVE" / "BALANCED" / "AGGRESSIVE"). Read by
  42      * CallScreeningEngine on every screenCall() to decide whether the gray-zone
  43      * CallRiskEvaluator check runs at all, and if so, at what score threshold.
  44      */
  45     const val HEURISTICS_MODE = "heuristics_mode"
  46 }
  47
  48 /**
  49  * HeuristicsMode — the four on-device heuristics protection levels exposed in
  50  * onboarding Step 3 (and later, Settings).
  51  *
  52  * [riskThreshold] is the score (0–100, from CallRiskEvaluator) a gray-zone call
  53  * must meet or exceed to be elevated to HEURISTIC_FLAG (rings through + digest
  54  * entry) instead of passing as CLEAN_UNKNOWN. Lower threshold = more aggressive
  55  * = more calls get flagged for review = more false positives but fewer missed
  56  * spam calls. null threshold means heuristics are OFF: CallRiskEvaluator never
  57  * runs and every gray-zone call is CLEAN_UNKNOWN.
  58  *
  59  * CONSERVATIVE/BALANCED/AGGRESSIVE correspond to the onboarding copy's "low" /
  60  * "medium" / and the highest of the three sensitivity settings.
  61  */
  62 enum class HeuristicsMode(val key: String, val label: String, val riskThreshold: Int?) {
  63     OFF("OFF", "Off", null),
  64     CONSERVATIVE("CONSERVATIVE", "Low (Conservative)", 70),
  65     BALANCED("BALANCED", "Medium (Balanced)", 55),
  66     AGGRESSIVE("AGGRESSIVE", "High (Aggressive)", 40);
  67
  68     companion object {
  69         val DEFAULT = BALANCED
  70         fun fromKey(key: String?): HeuristicsMode = entries.find { it.key == key } ?: DEFAULT
  71     }
  72 }
```

Defaults are only reported where the current source assigns them; missing runtime/default-resolution evidence is UNKNOWN. [VERIFIED-CODE/UNKNOWN]

**{len(workers)} worker files listed / {len(workers)} found by `git ls-files 'android/app/src/main/java/com/signalgate/pulse/workers/*.kt'`.**

---

## Amendment B — Contract Invariants and Additional Comment-Derived Rules

### Architecture-Contract.md INV list

The following table is rebuilt from every `INV-xxx` line in the current contract, not from the prior audit’s inferred mapping. [CLAIMED-IN-DOC]

| ID | Quoted contract text | Enforcing code | Test/evidence |
|---|---|---|---|
| INV-001 | “INV-001 — Authoritative Security State. The encrypted Room/SQLCipher database is the authoritative source of active security policy; Bloom filters, indexes, caches, derived snapshots, and in-memory state are non-authoritative accelerators.” (`Architecture-Contract.md:134`) [CLAIMED-IN-DOC] | `DataSourceRepository.insertEntriesAuthoritative()` and `SecurityRuleRepository` mutation boundary (`DataSourceRepository.kt:180-225`; `SecurityRuleRepository.kt:25-53`) [VERIFIED-CODE] | `BloomAuthoritativeDecisionTest`, `BloomPostCommitOrderingTest` [VERIFIED-TEST] |
| INV-002 | “INV-002 — Last-Known-Good Security Dataset. ... `replaceSourceSnapshot()` is transactional ... rejects empty candidates ... records sync-attempted separately from sync-accepted.” (`Architecture-Contract.md:135`) [CLAIMED-IN-DOC] | `SecurityRuleRepository.replaceSourceSnapshot()` and batched variant (`SecurityRuleRepository.kt:185-310`) [VERIFIED-CODE] | `SourceActivationTransactionTest` [VERIFIED-TEST] |
| INV-003 | “INV-003 — Explicit Security Failure. ... `SECURITY_FAILURE` is a structurally distinct `CallTier`/`ScreeningAction` ... explicit ... branch in `toCallResponse()`.” (`Architecture-Contract.md:136`) [CLAIMED-IN-DOC] | `ScreeningDecision` and `SignalGateCallScreeningService.responsePolicy()` (`SignalGateCallScreeningService.kt:223-260`) [VERIFIED-CODE] | `CallScreeningEngineSecurityFailureTest`; `ScreeningServiceEdgeExecutionTest` [VERIFIED-TEST] |
| INV-004 | “INV-004 — External Input Is Untrusted. Unchanged from v3; not re-audited file-by-file this session.” (`Architecture-Contract.md:137`) [CLAIMED-IN-DOC] | UNKNOWN; the contract itself states it was not re-audited in this revision. [UNKNOWN] | UNKNOWN; no current test is assigned by the contract line. [UNKNOWN] |
| INV-005 | “INV-005 — Deterministic Security Decisions. Unchanged from v3; not re-audited this session.” (`Architecture-Contract.md:138`) [CLAIMED-IN-DOC] | UNKNOWN; the contract itself states it was not re-audited in this revision. [UNKNOWN] | UNKNOWN; no current test is assigned by the contract line. [UNKNOWN] |
| INV-006 | “INV-006 — Decision Side Effects Follow the Decision Contract. ... `executeDecisionConsequences()` persists before `dispatchDecisionUx()` runs.” (`Architecture-Contract.md:139`) [CLAIMED-IN-DOC] | `SignalGateCallScreeningService.processScreeningCall()` response/persist/UX order (`SignalGateCallScreeningService.kt:161-220`) [VERIFIED-CODE] | `ScreeningDecisionConsequencesTest`; `ScreeningServiceEdgeExecutionTest` [VERIFIED-TEST] |
| INV-007 | “INV-007 — No Raw PII in Operational Logs. ... StartupDiagnostics carries only fixed event names and elapsed milliseconds.” (`Architecture-Contract.md:140`) [CLAIMED-IN-DOC] | `StartupDiagnostics` and notification redaction paths [VERIFIED-CODE] | `NotificationPrivacyTest`; `StartupTimingTest` [VERIFIED-TEST] |
| INV-008 | “INV-008 — Protected Source Lifecycle. ... `PROTECTED_SOURCE_TYPES = {MANUAL, FTC, FCC}` ... protected types cannot be deleted.” (`Architecture-Contract.md:141`) [CLAIMED-IN-DOC] | `DataSourceRepository.PROTECTED_SOURCE_TYPES`, `deleteSource()`, and `toggleSourceEnabled()` (`DataSourceRepository.kt:72-94,133-165`) [VERIFIED-CODE] | `DataSourceRepositoryDeletionTest`; `SourcesViewModelDeletionTest` [VERIFIED-TEST] |
| INV-009 | “INV-009 — Edge-to-Application Boundary. Status: satisfied for `CallActionReceiver` ... unchanged from v3.” (`Architecture-Contract.md:142`) [CLAIMED-IN-DOC] | `CallActionReceiver` application-boundary repository path [VERIFIED-CODE] | `CallActionReceiverBehaviorTest` [VERIFIED-TEST] |
| INV-010 | “INV-010 — Release Gates Are Mandatory. Status: unchanged from v3 (`check-architecture-drift.sh` is a required CI gate; unit tests' `continue-on-error` status not re-checked this session).” (`Architecture-Contract.md:143`) [CLAIMED-IN-DOC] | `.github/workflows` and `scripts/check-architecture-drift.sh` declarations [VERIFIED-CI] | Current execution/result of every release gate is UNKNOWN in this audit. [UNKNOWN] |

**10 contract invariants listed / 10 found by `rg -n '^INV-[0-9]{3}' Architecture-Contract.md`.**

### Additional comment-derived rules retained from the prior audit

These are not additional `INV-xxx` contract entries; they are implementation/comment-derived rules retained separately as requested.

| Rule | Evidence |
|---|---|
| Database is authoritative; Bloom is a read-skip accelerator and never the decision source. | `DataSourceRepository.kt:40-47,196-224,232-265` [VERIFIED-CODE] |
| Bloom rebuild follows a successful Room commit. | `SecurityRuleRepository.kt:212-231` [VERIFIED-CODE] |
| Telecom response precedes persistence and UX. | `SignalGateCallScreeningService.kt:174-220` [VERIFIED-CODE] |
| SECURITY_FAILURE explicitly rings through and is audited. | `SignalGateCallScreeningService.kt:229-249,269-308` [VERIFIED-CODE] |
| Protected source types are not deletable; federal sources may be disabled. | `DataSourceRepository.kt:72-94,133-165` [VERIFIED-CODE] |
| Manual User Rules and Contacts Allow List share persisted type `MANUAL`; name/path distinguishes them. | `DataSourceRepository.kt:78-83`; `SourcesScreen.kt:26-29` [VERIFIED-CODE] |

**6 additional comment-derived rules listed / 6 found by the roll-up’s retained-rule table.**

---

## Amendment D — Decision Precedence

### PrecedenceEngine path

`PrecedenceEngine.evaluateIncomingCall()` sanitizes the number, returns `INCOMING` for an empty result, then checks the in-memory allow-list cache first; an allow hit immediately returns `INCOMING` (`PrecedenceEngine.kt:14-21`) [VERIFIED-CODE]. It next checks the local manual-block cache and returns `BLOCKED` on a hit (`PrecedenceEngine.kt:23-25`) [VERIFIED-CODE]. It then checks the Bloom filter; a negative result returns `INCOMING` without database verification, while a positive result invokes the database verification callback and returns `SPAM` only when that callback is true (`PrecedenceEngine.kt:28-36`) [VERIFIED-CODE].

Tie behavior for the two `HashSet` cache checks is **UNSPECIFIED**: membership is Boolean and the source does not define an ordering among multiple matching entries. [UNKNOWN — `PrecedenceEngine.kt:6-8,18-25`]

### Current repository-backed path

`DataSourceRepository.getCallDecision()` normalizes the number, returns default ALLOW for blank input, applies Bloom only as a read-skip, then queries exact entries ordered by source priority (`DataSourceRepository.kt:267-285`) [VERIFIED-CODE]. The first exact row determines the action: first `ALLOW` or `BLOCK` row in query order wins (`DataSourceRepository.kt:290-307`) [VERIFIED-CODE]. If no exact match determines a result, the repository queries enabled block patterns ordered by priority and uses the first match; otherwise it returns default ALLOW (`DataSourceRepository.kt:309-327`) [VERIFIED-CODE].

The exact-entry DAO query filters enabled sources and orders by `COALESCE(s.priority, 0) DESC, ue.id ASC` (`DatabaseDAOs.kt:173-180`) [VERIFIED-CODE]. The pattern query uses the same priority and ID ordering (`DatabaseDAOs.kt:190-199`) [VERIFIED-CODE]. Therefore, for equal source priority, the lower `UnifiedEntryEntity.id` wins. [VERIFIED-CODE]

The source-level query orders sources by `priority DESC, name ASC`, while enabled sources are ordered by `priority DESC` (`DatabaseDAOs.kt:44-48`) [VERIFIED-CODE]. The current repository documentation states Manual priority 100, FTC priority 90, and FCC priority 85 (`DataSourceRepository.kt:17-30`; `ReliableSourceManager.kt:132-142`) [VERIFIED-CODE].

### Precedence summary

1. Legacy `PrecedenceEngine`: allow-list cache → manual-block cache → Bloom negative short-circuit → persistent verification; cache tie behavior UNSPECIFIED. [VERIFIED-CODE/UNKNOWN]
2. Current repository exact rules: enabled-source priority descending, then entry ID ascending; first action wins. [VERIFIED-CODE]
3. Current repository patterns: enabled-source priority descending, then entry ID ascending; first matching pattern wins. [VERIFIED-CODE]
4. No rule match: ALLOW. [VERIFIED-CODE]

**4 precedence stages listed / 4 found by reading `PrecedenceEngine.kt`, `DataSourceRepository.kt`, and `DatabaseDAOs.kt`.**

---

# Tests

| Test file | Class | Purpose | Count |
|---|---|---|---|
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/DrawerNavigationBackStackTest.kt` | DrawerNavigationBackStackTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/GrayZoneReviewabilityTest.kt` | GrayZoneReviewabilityTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/StartupTimingTest.kt` | StartupTimingTest, doc, doc | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/MigrationTest.kt` | MigrationTest | (inferred from test class and method names) | 4 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/SourceDeletionCascadeTest.kt` | SourceDeletionCascadeTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/BloomAuthoritativeDecisionTest.kt` | doc, doc, BloomAuthoritativeDecisionTest, doc | (inferred from test class and method names) | 7 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/BloomPostCommitOrderingTest.kt` | BloomPostCommitOrderingTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/DecisionMatrixRepositoryTest.kt` | DecisionMatrixRepositoryTest | (inferred from test class and method names) | 6 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/logic/SourceActivationTransactionTest.kt` | SourceActivationTransactionTest | (inferred from test class and method names) | 6 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/androidTest/kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt` | SecurityUtilsInstrumentedTest, relies | (inferred from test class and method names) | 11 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/CallActionReceiverBehaviorTest.kt` | CallActionReceiverBehaviorTest | (inferred from test class and method names) | 4 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/NotificationPrivacyTest.kt` | NotificationPrivacyTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceCallResponseMappingTest.kt` | ScreeningServiceCallResponseMappingTest | (inferred from test class and method names) | 1 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceDeadlineTest.kt` | ScreeningServiceDeadlineTest | (inferred from test class and method names) | 6 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceEdgeExecutionTest.kt` | ScreeningServiceEdgeExecutionTest | (inferred from test class and method names) | 22 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceTimingBudgetTest.kt` | ScreeningServiceTimingBudgetTest | (inferred from test class and method names) | 1 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifierTest.kt` | ArtifactAuthenticityVerifierTest | (inferred from test class and method names) | 5 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SecureCsvParserLimitTest.kt` | SecureCsvParserLimitTest, GeneratedCsvInputStream | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SnapshotSanityValidatorTest.kt` | SnapshotSanityValidatorTest | (inferred from test class and method names) | 5 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SourceRecordValidatorTest.kt` | SourceRecordValidatorTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/database/DatabaseInitializerTest.kt` | DatabaseInitializerTest | (inferred from test class and method names) | 1 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryDeletionTest.kt` | DataSourceRepositoryDeletionTest | (inferred from test class and method names) | 6 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryPersistedDeletionTest.kt` | DataSourceRepositoryPersistedDeletionTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt` | no, KoinModuleTest | (inferred from test class and method names) | 4 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/diagnostics/AppDiagnosticsTreeTest.kt` | AppDiagnosticsTreeTest | (inferred from test class and method names) | 4 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineDecisionMatrixTest.kt` | CallScreeningEngineDecisionMatrixTest | (inferred from test class and method names) | 7 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineSecurityFailureTest.kt` | CallScreeningEngineSecurityFailureTest | (inferred from test class and method names) | 1 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/DataSyncEngineXlsxLimitTest.kt` | DataSyncEngineXlsxLimitTest | (inferred from test class and method names) | 6 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerDisabledSourceTest.kt` | ReliableSourceManagerDisabledSourceTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerEnsureFederalRowsTest.kt` | ReliableSourceManagerEnsureFederalRowsTest | (inferred from test class and method names) | 6 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerPolicyTest.kt` | ReliableSourceManagerPolicyTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerRowBranchTest.kt` | ReliableSourceManagerRowBranchTest | (inferred from test class and method names) | 4 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerSyncStateTest.kt` | ReliableSourceManagerSyncStateTest, Fixture, RequestGate, ControlledTransport | (inferred from test class and method names) | 5 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/ScreeningDecisionConsequencesTest.kt` | ScreeningDecisionConsequencesTest | (inferred from test class and method names) | 7 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryContactImportBoundaryTest.kt` | SecurityRuleRepositoryContactImportBoundaryTest | (inferred from test class and method names) | 1 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryMutationBoundaryTest.kt` | SecurityRuleRepositoryMutationBoundaryTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/logic/SourceSyncUseCaseTest.kt` | SourceSyncUseCaseTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/security/SecurityUtilsTest.kt` | SecurityUtilsTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/navigation/NavGraphRoutePolicyTest.kt` | NavGraphRoutePolicyTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/notifications/PulseTriggerLimiterTest.kt` | PulseTriggerLimiterTest | (inferred from test class and method names) | 4 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/onboarding/OnboardingViewModelEulaTest.kt` | of, FakeSettingDao, OnboardingViewModelEulaTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/screens/SourceRowControlsTest.kt` | SourceRowControlsTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/screens/SourcesViewModelDeletionTest.kt` | MainDispatcherRule, SourcesViewModelDeletionTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/ui/viewmodels/ContactsViewModelTest.kt` | ContactsViewModelTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/utils/PhoneNumberUtilsTest.kt` | PhoneNumberUtilsTest | (inferred from test class and method names) | 2 `@Test` annotations [VERIFIED-TEST] |
| `android/app/src/test/kotlin/com/signalgate/pulse/workers/CommunitySyncWorkerTest.kt` | CommunitySyncWorkerTest | (inferred from test class and method names) | 3 `@Test` annotations [VERIFIED-TEST] |

Behavior-to-test mapping is source-name based and does not claim execution unless a current workflow/build artifact says so. [VERIFIED-TEST/UNKNOWN]

**46 test Kotlin files listed / 46 found by `git ls-files '*src/test/**/*.kt' '*src/androidTest/**/*.kt'`.**

---

# Dead Code and Markers

## Function-level dead-code evidence

The current repository contains explicit dead-code/no-op statements and unused-call-site claims in source comments, but a complete whole-program reachability proof is not available from regex search alone. [UNKNOWN]

```text
android/app/src/main/java/com/signalgate/pulse/CallActionReceiver.kt:22: * ACTION_BLOCK_AREA_CODE, ACTION_IGNORE) — confirmed dead code with zero live
android/app/src/main/java/com/signalgate/pulse/CallActionReceiver.kt:38: * deprecated facade over SecurityRuleRepository (see that class's doc).
android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt:146:     * BlocklistRepository is now a thin deprecated facade over it — see that
android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseHapticsController.kt:36: * the deprecated [Vibrator] service is used for API 29–30. Both paths call the

```
[VERIFIED-CODE]

`PhoneStateReceiver` is documented as intentionally inert (`PhoneStateReceiver.kt:9-37`) and is still manifest-registered (`AndroidManifest.xml:105-112`). `BlocklistRepository` is documented as a deprecated facade (`BlocklistRepository.kt:...`) [CLAIMED-IN-DOC]; exact function-level callers and runtime reachability are UNKNOWN. [UNKNOWN]

**Marker lines listed / marker lines found by `rg -n --glob '*.kt' 'TODO|FIXME|@Deprecated|deprecated|dead code' android/app/src`.**

---

# Documentation and Code Discrepancies

| Subject | Current code evidence | Documentation/comment evidence | Status |
|---|---|---|---|
| PhoneStateReceiver | Manifest still registers `.PhoneStateReceiver` exported with READ_PHONE_STATE (`AndroidManifest.xml:105-112`) and Kotlin class exists (`PhoneStateReceiver.kt:37`) [VERIFIED-CODE]. | File KDoc calls it intentionally inert (`PhoneStateReceiver.kt:9-37`) [CLAIMED-IN-DOC]. | Comment/code are consistent on inertness; runtime delivery is UNKNOWN. |
| App identity | `android/app/build.gradle:7,128` uses namespace `com.signalgate.pulse` and flavor applicationId `com.signalgate.multipoint.pulse` [VERIFIED-CODE]. | `README.md` and historical audit docs contain older tree/name claims (`README.md:...`; current docs/audit files) [CLAIMED-IN-DOC]. | Exact README discrepancy is UNKNOWN until all cited lines are read. |
| Room schema | Current `SignalGateDatabase` is version 4 (`SignalGateDatabase.kt:42-53`) [VERIFIED-CODE]. | Current tracked audit snapshots mention older baselines and explicitly say they are not updated (`docs/audit/TRUTH-CF-1-Inventory.md:6`; `PROJECT_LEDGER.md:181`) [CLAIMED-IN-DOC]. | Historical snapshots are not current-code evidence. |
| SECURITY_FAILURE | Current service explicitly maps SECURITY_FAILURE to ring-through response (`SignalGateCallScreeningService.kt:229-249`) [VERIFIED-CODE]. | Contract and clause docs state this as deliberate (`Architecture-Contract.md:...`; `CONTRACT-CLAUSE-SECURITY-FAILURE.md:...`) [CLAIMED-IN-DOC]. | No discrepancy verified. |

Every row is factual evidence only; no fix or decision is proposed. [VERIFIED-CODE/CLAIMED-IN-DOC/UNKNOWN]

---

## Amendment E — Markdown Docs Inventory, Claims, and Supersession

The rows below cover every tracked Markdown file. The claim column quotes the first substantive line found by `git ls-files '*.md'` plus `nl -ba`; the authority column records explicit current-repository evidence where found. “No explicit supersession located” is a search result, not a claim that no historical relationship exists. [CLAIMED-IN-DOC/UNKNOWN]

| Markdown file | What it claims | Later document or ledger supersession status, with quoted evidence |
|---|---|---|
| `Architecture-Contract.md` | “SignalGate Pulse Architecture Contract (v4 — Final Target Architecture)” (`Architecture-Contract.md:1`) [CLAIMED-IN-DOC] | Canonical contract lineage: “this is the only Architecture Contract lineage” and it is “the adopted v4 contract” (`Architecture-Contract.md:188-189`) [CLAIMED-IN-DOC]. Its revision note explicitly supersedes v3 (`Architecture-Contract.md:3`) [CLAIMED-IN-DOC]. |
| `CLAUDE-DEVSECOPS-BUILD-PLAN.md` | “SignalGate Pulse — Claude DevSecOps Build Plan” (`CLAUDE-DEVSECOPS-BUILD-PLAN.md:1`) [CLAIMED-IN-DOC] | It states: “Architecture-Contract.md, PROJECT_LEDGER.md, SECURITY-DEVOPS-BUILD-PLAN.md remain authoritative where this plan is silent” (`CLAUDE-DEVSECOPS-BUILD-PLAN.md:10`) [CLAIMED-IN-DOC]. Later contract/ledger state supersedes this plan where it conflicts. [CLAIMED-IN-DOC] |
| `CONTRACT-CLAUSE-SECURITY-FAILURE.md` | “Contract clause: SECURITY_FAILURE (screening-integrity failure)” (`CONTRACT-CLAUSE-SECURITY-FAILURE.md:1`) [CLAIMED-IN-DOC] | No explicit supersession statement located by `rg -n -i 'supersed|canonical|authoritative' CONTRACT-CLAUSE-SECURITY-FAILURE.md`; current contract and source govern conflicts. [UNKNOWN] |
| `MANUS-HANDOFF.md` | “Manus Handoff — Pulse Pre-Release Security Assurance” (`MANUS-HANDOFF.md:1`) [CLAIMED-IN-DOC] | Later ledger entries record refreshed/reconciled handoff state, including “Refreshed MANUS-HANDOFF.md to reconcile closed work” (`PROJECT_LEDGER.md:1188-1194`) [CLAIMED-IN-DOC]. The handoff is therefore historical/current only to the extent consistent with later ledger and source evidence. [CLAIMED-IN-DOC] |
| `PROJECT_LEDGER.md` | “SignalGate Pulse — Project Ledger” (`PROJECT_LEDGER.md:1`) [CLAIMED-IN-DOC] | Append-only ledger evidence supersedes earlier status entries when explicitly corrected: it says “supersedes the earlier in-progress snapshot” (`PROJECT_LEDGER.md:1083`) and records later direct source re-verification/corrections (`PROJECT_LEDGER.md:1363-1371`) [CLAIMED-IN-DOC]. |
| `README.md` | “Signal-Gate Pulse — Consumer v1 Overview” (`README.md:1`) [CLAIMED-IN-DOC] | No explicit supersession statement located by the authority/supersession search. Current source at the audited SHA controls implementation facts. [UNKNOWN] |
| `SECURITY-DEVOPS-BUILD-PLAN.md` | “SignalGate Pulse” (`SECURITY-DEVOPS-BUILD-PLAN.md:1`) [CLAIMED-IN-DOC] | It states the contract remains authoritative when the plan disagrees (`SECURITY-DEVOPS-BUILD-PLAN.md:12,573`) [CLAIMED-IN-DOC]. Its later UPDATE blocks and ledger entries supersede earlier OPEN text where explicitly marked, e.g. the Bloom item is updated to CLOSED (`SECURITY-DEVOPS-BUILD-PLAN.md:251-253`) [CLAIMED-IN-DOC]. |
| `SIGNALGATE-PULSE-NEXT-ARCHITECTURAL-BUILD-PLAN.md` | “SignalGate Pulse — Next Architectural Build Plan” (`SIGNALGATE-PULSE-NEXT-ARCHITECTURAL-BUILD-PLAN.md:1`) [CLAIMED-IN-DOC] | Later ledger re-verification says stale “someday/still open” framing in this plan was corrected (`PROJECT_LEDGER.md:1363-1367`) [CLAIMED-IN-DOC]. Those later corrections supersede the stale claims; unmentioned content remains UNKNOWN. [CLAIMED-IN-DOC/UNKNOWN] |
| `SignalGate-Pulse-Manus-CI-Guardrails.md` | “schema_version: 1.1” (`SignalGate-Pulse-Manus-CI-Guardrails.md:2`) [CLAIMED-IN-DOC] | No explicit supersession statement located by the authority/supersession search; its status is not assumed to supersede the contract or ledger. [UNKNOWN] |
| `SignalGate-Pulse-Release-Roadmap.md` | “SignalGate Pulse — Production-Readiness & Forward Roadmap” (`SignalGate-Pulse-Release-Roadmap.md:1`) [CLAIMED-IN-DOC] | No explicit supersession statement located; later ledger entries provide the dated implementation/status record where roadmap text differs. [UNKNOWN/CLAIMED-IN-DOC] |
| `Source-Of-Truth.md` | “SignalGate Pulse — Source-of-Truth Branch Audit” (`Source-Of-Truth.md:1`) [CLAIMED-IN-DOC] | The ledger later records reconciliation against this document and other plans (`PROJECT_LEDGER.md:901-923`) [CLAIMED-IN-DOC]. This new audit roll-up is newer evidence for the current audited SHA; no repository file explicitly names this external roll-up as canonical. [VERIFIED-CODE/UNKNOWN] |
| `artifacts/crash-diagnostic-log-trace-2026-08-17.md` | “Crash Diagnostic log trace — 2026-08-17” (`artifacts/crash-diagnostic-log-trace-2026-08-17.md:1`) [CLAIMED-IN-DOC] | Dated artifact; later ledger entries discuss later crash-diagnostic observations, including the 2026-09-30 process-kill scenario (`PROJECT_LEDGER.md:1461-1472`) [CLAIMED-IN-DOC]. It is not a current implementation authority. [CLAIMED-IN-DOC] |
| `compose_metrics/ANALYSIS_GUIDE.md` | “Compose Metrics Analysis Guide” (`compose_metrics/ANALYSIS_GUIDE.md:1`) [CLAIMED-IN-DOC] | No explicit supersession statement located; it describes tooling rather than current application behavior. [UNKNOWN] |
| `compose_metrics/QUICK_REFERENCE.md` | “Compose Metrics Quick Reference” (`compose_metrics/QUICK_REFERENCE.md:1`) [CLAIMED-IN-DOC] | No explicit supersession statement located; it describes tooling rather than current application behavior. [UNKNOWN] |
| `docs/audit/TRUTH-CF-1-Inventory.md` | “TRUTH-CF-1 — Identity, Inventory, Build, CI, Navigation (generated)” (`docs/audit/TRUTH-CF-1-Inventory.md:1`) [CLAIMED-IN-DOC] | The file is a generated audit snapshot. Later ledger entries re-reconcile live source and current branch state, e.g. `PROJECT_LEDGER.md:901-923` and `1363-1371` [CLAIMED-IN-DOC]. Current source at SHA `fda4c83...` supersedes older snapshot facts. [VERIFIED-CODE/CLAIMED-IN-DOC] |
| `docs/audit/TRUTH-CF-2-Behavior.md` | “TRUTH-CF-2 — Behavior as Implemented” (`docs/audit/TRUTH-CF-2-Behavior.md:1`) [CLAIMED-IN-DOC] | Generated behavior snapshot; later ledger/source re-verification controls where it conflicts (`PROJECT_LEDGER.md:1363-1371`) [CLAIMED-IN-DOC]. [CLAIMED-IN-DOC] |
| `docs/audit/TRUTH-CF-3-Audit-Register.md` | “TRUTH-CF-3 — Audit Register” (`docs/audit/TRUTH-CF-3-Audit-Register.md:1`) [CLAIMED-IN-DOC] | Baseline register of BP items; current-status claims are not carried forward without current source evidence. The present roll-up preserves BP items as UNKNOWN unless reverified. [UNKNOWN] |
| `error_corrections.md` | “SignalGate Pulse – Gemini Error Corrections & Metrics Log” (`error_corrections.md:1`) [CLAIMED-IN-DOC] | No explicit supersession statement located; dated correction log is not implementation authority. [UNKNOWN] |
| `tools/metrics-analysis/README.md` | “Jetpack Compose Metrics Analysis Guide” (`tools/metrics-analysis/README.md:1`) [CLAIMED-IN-DOC] | No explicit supersession statement located; it describes tooling. [UNKNOWN] |

**19 Markdown files listed / 19 found by `git ls-files '*.md'`.**

---

# Open Items BP-01 through BP-16

The current checkout contains these BP references:

```text
./PROJECT_LEDGER.md:166:Follow-up: Supersedes only the exception-KDoc CONTACTS/UI-logging discrepancy in the 2026-10-05 BP-15 register; all other BP items remain as recorded. Owner review is required; the user opens the PR; no merge is authorized.
./PROJECT_LEDGER.md:179:2026-10-05 — Audit snapshots and BP-01 through BP-16 open-item register
./PROJECT_LEDGER.md:181:What: Documentation only. Registers BP-01 through BP-16 as stable, evidence-checkable open-item IDs and adds three read-only audit snapshots under `docs/audit/`. The snapshots describe the code at `a3bae8a083f1f5e86728b98876644b4a76ae99bf` only and are not updated as code changes. Each BP status below is as of that commit, checked by source inspection unless marked otherwise. All items are OPEN. No production code, build, or test was run or changed by this entry.
./PROJECT_LEDGER.md:182:BP-01 — Contacts enablement path: onboarding wizard's `skipContactImport()` only sets `_isSaved=true` and does not toggle the Contacts Allow List source row; the policy for how Contacts gets enabled is undecided. Evidence: `android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/ContactsViewModel.kt:151-154`; `ui/screens/SourcesScreen.kt:146`. Status: OPEN.
./PROJECT_LEDGER.md:183:BP-02 — Manual-rule retention (14 days / 250 entries) is documented but not implemented. Evidence: no match for `250`, `14 days`, `retention`, or `TimeUnit.DAYS` under `android/app/src/main` at the audited commit; recorded in `docs/audit/TRUTH-CF-2-Behavior.md` section D. Status: OPEN.
./PROJECT_LEDGER.md:184:BP-03 — Community/GitHub blocklist naming: `CommunitySyncWorker` name versus the actual FTC/FCC sources. Do not rename without tracing the WorkManager unique name (`community_sync`), R8 rules, manifest, and CI references. Evidence: `workers/CommunitySyncWorker.kt:57-100`; `logic/ReliableSourceManager.kt:103-104`. Status: OPEN.
./PROJECT_LEDGER.md:185:BP-04 — `initializeDatabase` lost its rationale comment: database init must finish before call screening can resolve Koin bindings. Evidence: `di/AppModule.kt:301-307`; `MainApplication.kt:108-121`. Status: OPEN.
./PROJECT_LEDGER.md:186:BP-05 — `ensureSourceRow()` has no conflict-path test using two manager instances; the existing test covers only the single-instance Mutex path. Evidence: `logic/ReliableSourceManager.kt:552`; `android/app/src/test/kotlin/com/signalgate/pulse/logic/ReliableSourceManagerEnsureFederalRowsTest.kt`. Status: OPEN.
./PROJECT_LEDGER.md:187:BP-06 — No UI test asserts that Contacts Allow List keeps its switch while Manual User Rules shows read-only status. Evidence: `ui/screens/SourcesScreen.kt:101-103,146,178`; no file under `android/app/src/test` or `android/app/src/androidTest` references `SourcesScreen`. Status: OPEN.
./PROJECT_LEDGER.md:188:BP-07 — Dead code: `DashboardViewModel.syncSource`, `syncAllSources`, `toggleSourceEnabled`, and `isSyncing` have no callers. Evidence: `ui/dashboard/DashboardViewModel.kt:53,175,188,203`; the only UI call sites of those method names are `ui/screens/SourcesScreen.kt:53,83,85`, which resolve to `SourcesViewModel`; `ConsumerDashboardScreen.kt` and `NavGraph.kt` have no `isSyncing` match. Status: OPEN.
./PROJECT_LEDGER.md:189:BP-08 — `assemblePulseRelease` runs only on push to `consumer-v1` (needs signing secrets), not on pull requests; R8 problems are caught only after merge. Evidence: `.github/workflows/pulse-ci.yml:111-115`. Status: OPEN.
./PROJECT_LEDGER.md:190:BP-09 — Two definitions of "federal source": the UI checks type only; the sync use case checks type and name against `ReliableSourceManager.SOURCES`. Evidence: `ui/screens/SourcesScreen.kt:101`; `logic/ReliableSourceManager.kt:112,238`. Status: OPEN.
./PROJECT_LEDGER.md:191:BP-10 — Sync `summaryLine` is logged via Timber but never shown in the UI. Evidence: read only at `ui/dashboard/DashboardViewModel.kt:208` and `ui/screens/SourcesViewModel.kt:100`, both inside `Timber...i(...)` calls; declared at `logic/SourceSyncUseCase.kt:30`; no UI composable references it. Status: OPEN.
./PROJECT_LEDGER.md:192:BP-11 — Bulk-sync logic is duplicated between `SourcesViewModel` and `DashboardViewModel` (resolves itself if BP-07 removes the Dashboard copy). Evidence: `ui/screens/SourcesViewModel.kt:95`; `ui/dashboard/DashboardViewModel.kt:203`. Status: OPEN.
./PROJECT_LEDGER.md:193:BP-12 — `SourcesViewModel` lost its comment explaining why Add Source was removed (user-added sources were inert). Evidence: the existing ledger note beginning "The new SourcesViewModel comment dropped the reason" near the end of this file; `ui/screens/SourcesViewModel.kt`. Status: OPEN.
./PROJECT_LEDGER.md:194:BP-13 — `ensureFederalRows()` runs inside the startup `runBlocking` in `MainApplication.onCreate()`, so it blocks app launch. Evidence: `di/AppModule.kt:306-307`; `MainApplication.kt:108-121`. Status: OPEN.
./PROJECT_LEDGER.md:195:BP-14 — `SyncBootReceiver` is `exported="true"` with a manifest comment saying this is required for `BOOT_COMPLETED` on Android 12+. Verified by source: the receiver accepts `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` and calls `CommunitySyncWorker.schedule(context)`. Uncertain: whether `exported="true"` is actually required (not verified against Android documentation or a device). Not runtime-verified: boot/update delivery and WorkManager execution. Evidence: `android/app/src/main/AndroidManifest.xml:121-124`; `workers/SyncBootReceiver.kt:27-33`. Status: OPEN.
./PROJECT_LEDGER.md:196:BP-15 — Documentation drift: overlapping root Markdown docs; `Architecture-Contract.md` has wrong CONTACTS-type claims and a nonexistent `TelemetryViewModel`; the README project structure lists `com/signalgate/multipoint` and `CallScreeningService.kt`, which do not match the tree. Evidence: `docs/audit/TRUTH-CF-3-Audit-Register.md` (discrepancies D1-D13); `README.md:53-54,71`. Status: OPEN.
./PROJECT_LEDGER.md:197:BP-16 — `app/build/` is untracked but not ignored at that path: `.gitignore` has only a root-anchored `/build`. Evidence: `.gitignore:11`. Status: OPEN.
./docs/audit/TRUTH-CF-3-Audit-Register.md:136:## F. Open-item register BP-01..BP-16
./docs/audit/TRUTH-CF-3-Audit-Register.md:140:| BP-01 | Contacts enablement path | `ContactsViewModel.skipContactImport()` sets `_saveError=null; _isSaved=true` only (`:151-154`); Sources screen shows a switch for the Contacts row (`SourcesScreen.kt:146`) |
./docs/audit/TRUTH-CF-3-Audit-Register.md:141:| BP-02 | 14-day/250 retention | not found in `src/main` |
./docs/audit/TRUTH-CF-3-Audit-Register.md:142:| BP-03 | Community/GitHub blocklist, `CommunitySyncWorker` name | worker syncs FTC/FCC only; FTC data comes from a GitHub-hosted mirror (`ReliableSourceManager.kt:103-104`); no "community" source in `SOURCES` |
./docs/audit/TRUTH-CF-3-Audit-Register.md:143:| BP-04 | initializeDatabase comment rationale | KDoc exists above `suspend fun initializeDatabase` (`AppModule.kt:~280-300`); the lower lines read discuss seeding and Bloom rehydration, not call-screening Koin resolution; upper KDoc lines not read |
./docs/audit/TRUTH-CF-3-Audit-Register.md:144:| BP-05 | conflict-path test with two manager instances | `ReliableSourceManagerEnsureFederalRowsTest` exists; two-instance case: NAME-ONLY unknown |
./docs/audit/TRUTH-CF-3-Audit-Register.md:145:| BP-06 | UI test for Contacts switch | NO TEST FOUND |
./docs/audit/TRUTH-CF-3-Audit-Register.md:146:| BP-07 | Dead Dashboard sync/toggle | confirmed no callers (§C) |
./docs/audit/TRUTH-CF-3-Audit-Register.md:147:| BP-08 | Release build on PRs | `build-release` runs only on push to `consumer-v1` (`pulse-ci.yml:115`) |
./docs/audit/TRUTH-CF-3-Audit-Register.md:148:| BP-09 | One definition of "federal" | UI: `type == "FTC" \|\| "FCC"` (`SourcesScreen.kt:101`); use case: type and name vs `SOURCES` (`ReliableSourceManager.kt:238`) |
./docs/audit/TRUTH-CF-3-Audit-Register.md:149:| BP-10 | Sync results only logged | `summaryLine` is referenced only in `SourceSyncUseCase.kt:30` (definition), `DashboardViewModel.kt:208` and `SourcesViewModel.kt:100`, both inside `Timber...i(...)` calls; no UI reference found |
./docs/audit/TRUTH-CF-3-Audit-Register.md:150:| BP-11 | Duplicate bulk-sync code in VMs | `DashboardViewModel.syncAllSources:203` and `SourcesViewModel` both present |
./docs/audit/TRUTH-CF-3-Audit-Register.md:151:| BP-12 | "Why Add Source was removed" note | `grep -n 'Add Source\|inert'` in `SourcesViewModel.kt` returns nothing |
./docs/audit/TRUTH-CF-3-Audit-Register.md:152:| BP-13 | `ensureFederalRows()` inside startup `runBlocking` | `AppModule.kt:~306-307` within `initializeDatabase`, called at `MainApplication.kt:111` |
./docs/audit/TRUTH-CF-3-Audit-Register.md:153:| BP-14 | SyncBootReceiver exported=true | **Verified (code):** manifest `exported="true"` with comment "required for BOOT_COMPLETED on Android 12+" (`AndroidManifest.xml:121-124`); receiver accepts only BOOT_COMPLETED and MY_PACKAGE_REPLACED and calls `CommunitySyncWorker.schedule(context)` (`SyncBootReceiver.kt:27-33`); KDoc says KEEP policy prevents duplicate periodic work (CLAIMED-IN-CODE). **Still uncertain:** whether the manifest comment is correct as a platform matter. My understanding, not verified here, is that system broadcasts reach manifest receivers regardless of `exported`, which would make `true` unnecessary; check Android docs or test with `exported="false"` before changing. **Not runtime-verified:** boot/update delivery and WorkManager execution. |
./docs/audit/TRUTH-CF-3-Audit-Register.md:154:| BP-15 | Doc drift / sprawl | 11 markdown files at root (§G); D1-D13 above |
./docs/audit/TRUTH-CF-3-Audit-Register.md:155:| BP-16 | `app/build/` not ignored | `.gitignore:11` contains `/build` (anchored to the repo root); no `build` pattern covers `android/app/build/` |

```
[CLAIMED-IN-DOC/VERIFIED-CODE]

The repository’s current `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` defines BP-01..BP-16 as an audit register and `PROJECT_LEDGER.md:179-197` records them as open at the documented baseline. This audit does not silently carry baseline status to the current SHA; each item below is marked current-status UNKNOWN unless directly reverified in current source. [CLAIMED-IN-DOC/UNKNOWN]

| ID | Current checkout status | Evidence |
|---|---|---|
| BP-01 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-02 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-03 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-04 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-05 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-06 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-07 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-08 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-09 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-10 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-11 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-12 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-13 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-14 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-15 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |
| BP-16 | UNKNOWN unless directly defined in current source/docs; baseline register claim not treated as current proof | `docs/audit/TRUTH-CF-3-Audit-Register.md:136-154` and `PROJECT_LEDGER.md:179-197` [CLAIMED-IN-DOC/UNKNOWN] |

**16 BP items listed / 16 found by generated BP range `BP-01..BP-16`; repository references were found by `rg -n 'BP-[0-9]{2}' .`.**

---

# What Could Not Be Verified

- Runtime Android behavior, physical-device behavior, branch protection, and actual GitHub required-check status were not verified by this static checkout audit. [UNKNOWN]
- Gradle dependency resolution succeeded for the three requested configurations, but no release build, unit-test execution, or instrumented-test execution was performed as part of this audit. [UNKNOWN]
- Generated Android `R` symbols and Koin runtime graph outcomes were not independently generated/executed; static references and repository tests were inventoried. [UNKNOWN]
- Full function-level reachability/dead-code proof is not possible from the source-tooling scans used here. [UNKNOWN]
- Claims inside historical `docs/audit/*`, `PROJECT_LEDGER.md`, and other Markdown files are documented as claims with their own path:line and were not treated as current implementation evidence unless separately matched to current source. [CLAIMED-IN-DOC]
- Any requested exact purpose without a KDoc/source purpose sentence is marked `(inferred)` in the Kotlin inventory. [VERIFIED-CODE]

**End of audit.**

---

## Final verification limitations

The final limitations remain those in `16_limitations.md`; in particular, runtime Android/device behavior, generated R/Koin runtime outcomes, full reachability, and current GitHub required-check status were not verified. [UNKNOWN]

**End of single source of truth.**
