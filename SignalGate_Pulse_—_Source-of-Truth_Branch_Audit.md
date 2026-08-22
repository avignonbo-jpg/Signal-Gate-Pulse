# SignalGate Pulse — Source-of-Truth Branch Audit

**Audit scope.** This document records what exists in the attached branch only; it does not propose fixes or assess desired behavior. Paths are relative to the repository root `Signal-Gate-Pulse-consumer-v1`. The inventory includes production, JVM unit-test, and instrumented-test Kotlin files.

## Inventory summary

| Area | Count / value |
|---|---:|
| Production Kotlin files (`android/app/src/main`) | 83 |
| JVM unit-test Kotlin files (`android/app/src/test`) | 17 |
| Instrumented-test Kotlin files (`android/app/src/androidTest`) | 8 |
| Total Kotlin files | 108 |
| Android module | `android/app` |
| Namespace | `com.signalgate.pulse` |
| Default application ID | `com.signalgate.multipoint` |
| `pulse` flavor application ID | `com.signalgate.multipoint.pulse` |


## 1. Kotlin files

The following table lists every `.kt` file found under `android/app/src`, its declared package, detected class/interface/object declarations, and its present purpose in one sentence. Files containing only top-level functions or composables are explicitly identified as having no class declaration.

| Source set | File | Package | Class / object / interface declarations | Purpose |
|---|---|---|---|---|
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/GrayZoneReviewabilityTest.kt` | `com.signalgate.pulse` | class GrayZoneReviewabilityTest | Verifies that gray-zone screening outcomes remain reviewable through the pending-card flow. |
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/StartupTimingTest.kt` | `com.signalgate.pulse` | class StartupTimingTest | Verifies application startup sequencing and timing for the activity/database initialization path. |
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/MigrationTest.kt` | `com.signalgate.pulse.database` | class MigrationTest | Verifies Room schema migration behavior for the declared database migration versions. |
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/SourceDeletionCascadeTest.kt` | `com.signalgate.pulse.database` | class SourceDeletionCascadeTest | Verifies cascading deletion of source-owned records when a source entity is removed. |
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/BloomAuthoritativeDecisionTest.kt` | `com.signalgate.pulse.database.repositories` | class BloomAuthoritativeDecisionTest | Verifies that Bloom-filter results never override the authoritative database decision. |
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/database/repositories/DecisionMatrixRepositoryTest.kt` | `com.signalgate.pulse.database.repositories` | class DecisionMatrixRepositoryTest | Verifies repository-level allow/block decision precedence across the decision matrix. |
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/logic/SourceActivationTransactionTest.kt` | `com.signalgate.pulse.logic` | class SourceActivationTransactionTest | Verifies transactional source snapshot activation and rollback behavior. |
| `androidTest` | `android/app/src/androidTest/kotlin/com/signalgate/pulse/security/SecurityUtilsInstrumentedTest.kt` | `com.signalgate.pulse.security` | class SecurityUtilsInstrumentedTest | Verifies Android Keystore-backed passphrase encryption, decryption, and recovery on a device or emulator. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/CallActionReceiver.kt` | `com.signalgate.pulse` | class CallActionReceiver | Handles the notification Not Spam action by adding the number to the manual allow list, dismissing related pending cards, and cancelling the notification. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/CallInfo.kt` | `com.signalgate.pulse` | class CallTier, class CallInfo | Defines the call classification enum and Parcelable call-result model passed between the screening engine and service. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/MainActivity.kt` | `com.signalgate.pulse` | class MainActivity | Hosts the single-activity Jetpack Compose UI, handles the splash/deep-link lifecycle, and presents the database-reset state to the user. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/MainApplication.kt` | `com.signalgate.pulse` | class MainApplication, object AppReadiness, class ReleaseTree | Initializes the application process by enabling debug policy, starting dependency injection, initializing the encrypted database, rehydrating security indexes, registering notification channels, and scheduling background synchronization. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/PhoneStateReceiver.kt` | `com.signalgate.pulse` | class PhoneStateReceiver | Receives phone-state broadcasts and intentionally performs no call-processing work. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/SignalGateCallScreeningService.kt` | `com.signalgate.pulse` | class SignalGateCallScreeningService | Implements Android call screening by converting incoming calls into screening decisions, applying Telecom responses, recording outcomes, and issuing blocked-call notifications. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/models/BenchmarkResult.kt` | `com.signalgate.pulse.data.models` | class BenchmarkResult | Models device benchmark measurements such as storage, memory, I/O, and throttling capability. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/models/CallLogItem.kt` | `com.signalgate.pulse.data.models` | class CallType, class CallLogItem | Defines the UI model and call-type enum used to render a screened-call log entry. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/models/PermissionStatus.kt` | `com.signalgate.pulse.data.models` | class PermissionStatus | Models one runtime permission together with its manifest name, display description, and current grant state. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/models/ThreatSource.kt` | `com.signalgate.pulse.data.models` | class SourceStatus, class SourceType, class ThreatSource | Defines source status/type enums and the UI model for a configured threat-data source. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifier.kt` | `com.signalgate.pulse.data.security` | object ArtifactAuthenticityVerifier, class Manifest, interface Result, object Verified, class Failed, interface ResultOrManifest, class ManifestValue, class Failure | Verifies downloaded source artifacts against their expected cryptographic authenticity metadata. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/BloomFilterEngine.kt` | `com.signalgate.pulse.data.security` | class BloomFilterEngine | Provides an in-memory Bloom filter used to quickly identify numbers that may require authoritative database lookup. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/PrecedenceEngine.kt` | `com.signalgate.pulse.data.security` | class PrecedenceEngine | Evaluates allow/block precedence using cached rules and Bloom-filter hints before invoking authoritative verification. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/SanitizationEngine.kt` | `com.signalgate.pulse.data.security` | object SanitizationEngine | Centralizes sanitization and length limiting for phone numbers and other imported or persisted text fields. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/SecureCsvParser.kt` | `com.signalgate.pulse.data.security` | class SecureCsvParser, class CsvResourceLimitExceededException | Streams CSV source data under parser limits while sanitizing records and forwarding valid phone numbers for indexing. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/SnapshotSanityValidator.kt` | `com.signalgate.pulse.data.security` | class SnapshotSanityValidator, class Limits, class Candidate, interface Result, object Accepted, class Rejected | Checks imported source snapshots for structural and content-level sanity before activation. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/SourceAuthenticityTrustAnchor.kt` | `com.signalgate.pulse.data.security` | object SourceAuthenticityTrustAnchor | Defines the trusted authenticity configuration used when validating remote source artifacts. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/data/security/SourceRecordValidator.kt` | `com.signalgate.pulse.data.security` | object SourceRecordValidator | Validates individual imported source records before they enter the application data layer. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/DatabaseInitializer.kt` | `com.signalgate.pulse.database` | object DatabaseInitializer | Performs idempotent first-run database seeding for required manual and contacts-allow-list sources and their settings. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/SecureDatabase.kt` | `com.signalgate.pulse.database` | object SecureDatabase | Constructs the SQLCipher-backed Room database and handles recovery when the Android Keystore-protected passphrase is invalidated. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/SignalGateDatabase.kt` | `com.signalgate.pulse.database` | class SignalGateDatabase | Declares the Room database schema, its entities and DAOs, and the explicit schema migrations currently supported. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/daos/DatabaseDAOs.kt` | `com.signalgate.pulse.database.daos` | interface SourceDao, interface UnifiedEntryDao, interface CallLogDao, interface SettingDao, interface SyncHistoryDao, interface PendingCardDao | Declares Room DAO interfaces for source, rule, call-log, setting, sync-history, and pending-card persistence operations. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/entities/DatabaseEntities.kt` | `com.signalgate.pulse.database.entities` | class SourceEntity, class UnifiedEntryEntity, class CallLogEntry, class SettingEntry, class SyncHistoryEntry, class PendingCardEntity | Declares the Room entities representing sources, phone rules, call logs, settings, sync history, and pending review cards. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/repositories/BlocklistRepository.kt` | `com.signalgate.pulse.database.repositories` | class BlocklistRepository | Provides repository operations for the user-managed manual block and allow rules. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/repositories/CallLogRepository.kt` | `com.signalgate.pulse.database.repositories` | class CallLogRepository | Wraps call-log DAO operations and sanitizes call records at the repository write boundary. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/repositories/DataSourceRepository.kt` | `com.signalgate.pulse.database.repositories` | class DataSourceRepository, class CallDecision, class ProtectedSourceDeletionException | Provides authoritative source and rule access, conflict resolution, Bloom-filter fast paths, and sanitized rule insertion. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/repositories/PendingCardRepository.kt` | `com.signalgate.pulse.database.repositories` | class PendingCardRepository | Wraps pending-card queue persistence with validation, dismissal, and review operations. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/repositories/SettingKeys.kt` | `com.signalgate.pulse.database.repositories` | object SettingKeys, class HeuristicsMode | Centralizes setting-key constants and defines the available on-device heuristics protection modes and thresholds. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/repositories/SettingRepository.kt` | `com.signalgate.pulse.database.repositories` | class SettingRepository | Provides read and upsert operations for persisted application settings. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/database/repositories/SyncHistoryRepository.kt` | `com.signalgate.pulse.database.repositories` | class SyncHistoryRepository | Provides persistence operations for per-source synchronization history. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt` | `com.signalgate.pulse.di` | No class; top-level declarations only | Defines the Koin dependency graph for databases, repositories, engines, view models, workers, and startup helpers. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/di/KoinWorkerFactory.kt` | `com.signalgate.pulse.di` | class KoinWorkerFactory | Creates WorkManager workers by resolving their dependencies from Koin. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/CallRiskEvaluator.kt` | `com.signalgate.pulse.logic` | object CallRiskEvaluator, class RiskEvaluation | Computes an advisory risk score and classification for calls in the gray zone using verification and source-match signals. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/CallScreeningEngine.kt` | `com.signalgate.pulse.logic` | class CallScreeningEngine | Translates authoritative source decisions and risk evaluation into the app’s call-screening result model and user-facing details. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/DataSyncEngine.kt` | `com.signalgate.pulse.logic` | class DataSyncEngine, class ParserLimits, class SharedStringsHandler, class XlsxParseException, class RowLimitExceededException, class SharedStringsLimitExceededException | Parses bounded CSV and XLSX streams into validated phone-number records without loading unbounded input into memory. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/ReliableSourceManager.kt` | `com.signalgate.pulse.logic` | class ReliableSourceManager, class FetchStrategy, class FederalSource, class SyncResult, class FetchedSnapshot, class CountingInputStream, class RawBody | Fetches and synchronizes federal threat-data sources using authenticated snapshots, streaming parsers, fallback strategies, and sync-history recording. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/ScreeningAction.kt` | `com.signalgate.pulse.logic` | class ScreeningAction | Defines the possible call-screening actions: allow, block, screen, and security failure. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/ScreeningDecision.kt` | `com.signalgate.pulse.logic` | class NotificationPolicy, class HapticPolicy, class ScreeningDecision | Defines notification and haptic policies and packages the consequences associated with a screening decision. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/SecurityRuleRepository.kt` | `com.signalgate.pulse.logic` | class SecurityRuleRepository, interface SnapshotActivationResult, object Accepted, class Failed | Validates and transactionally activates security-sensitive source rules and snapshots. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/SourceLifecycleState.kt` | `com.signalgate.pulse.logic` | class SourceLifecycleState, class SnapshotMetadata | Defines source lifecycle states and metadata describing the current state of a synchronized source snapshot. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/logic/SourceSyncUseCase.kt` | `com.signalgate.pulse.logic` | class SourceSyncUseCase | Exposes the application use case for synchronizing one configured source through ReliableSourceManager. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/security/DatabaseResetEvent.kt` | `com.signalgate.pulse.security` | object DatabaseResetEvent | Publishes and acknowledges a process-local StateFlow signal when encrypted database recovery resets local data. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/security/KeystoreInvalidatedException.kt` | `com.signalgate.pulse.security` | class KeystoreInvalidatedException | Represents failure to decrypt the stored database passphrase because the Keystore key or wrapped data is invalid. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/security/SecurityUtils.kt` | `com.signalgate.pulse.security` | object SecurityUtils | Manages the Keystore-protected SQLCipher passphrase and provides AES-GCM encryption/decryption utilities plus strict-mode setup. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/BlockedNumbersViewModel.kt` | `com.signalgate.pulse.ui` | class BlockedNumbersViewModel, class Filter | Manages search, filtering, insertion, and deletion state for the manual block/allow-list screen. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/RecentCallsViewModel.kt` | `com.signalgate.pulse.ui` | class RecentCallsViewModel | Loads recent call-log data and exposes actions for blocking or allowlisting numbers from recent calls. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/components/AdvancedGlassCard.kt` | `com.signalgate.pulse.ui.components` | No class; top-level declarations only | Renders a reusable glassmorphic card with layered gradients and edge-light styling. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/components/GlassCard.kt` | `com.signalgate.pulse.ui.components` | No class; top-level declarations only | Renders a reusable glowing/shadowed glassmorphic container. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/components/GlassmorphicCard.kt` | `com.signalgate.pulse.ui.components` | No class; top-level declarations only | Renders a reusable translucent bordered card container. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/components/GlassmorphicDrawerContent.kt` | `com.signalgate.pulse.ui.components` | No class; top-level declarations only | Renders the branded navigation drawer and its available screen entries. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/components/NeonButton.kt` | `com.signalgate.pulse.ui.components` | class NeonButtonStyle | Defines reusable neon-styled button variants for primary, success, warning, and danger actions. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/components/ShieldStatusGlow.kt` | `com.signalgate.pulse.ui.components` | No class; top-level declarations only | Renders an animated shield status indicator with a pulsing glow and status label. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/components/SourceIcon.kt` | `com.signalgate.pulse.ui.components` | No class; top-level declarations only | Renders a compact icon badge distinguishing source types in the UI. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/dashboard/DashboardViewModel.kt` | `com.signalgate.pulse.ui.dashboard` | class DashboardViewModel | Owns dashboard state for source health, shield status, daily counters, onboarding completion, and synchronization actions. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/digest/DigestScreen.kt` | `com.signalgate.pulse.ui.digest` | No class; top-level declarations only | Renders the blocked-call review queue as an interactive pending-card digest screen. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/digest/PendingCardViewModel.kt` | `com.signalgate.pulse.ui.digest` | class PendingCardViewModel | Loads undismissed pending cards and performs dismiss, dismiss-all, and not-spam actions. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt` | `com.signalgate.pulse.ui.navigation` | No class; top-level declarations only | Builds the Compose NavHost that maps application routes and the digest deep link to their screens. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/navigation/Screen.kt` | `com.signalgate.pulse.ui.navigation` | class Screen, object Dashboard, object Sources, object CallLog, object BlockAllowList, object Settings, object Logcat, object Onboarding, object Digest | Declares the sealed set of navigation destinations with their routes, titles, and icons. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/notifications/NotificationChannelManager.kt` | `com.signalgate.pulse.ui.notifications` | object NotificationChannelManager | Registers the application’s notification channels for blocked-call review, synchronization status, and security alerts. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseHapticsController.kt` | `com.signalgate.pulse.ui.notifications` | class PulseHapticsController | Controls haptic feedback patterns used by Pulse interactions. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseTriggerLimiter.kt` | `com.signalgate.pulse.ui.notifications` | class PulseTriggerLimiter | Limits repeated Pulse notification or haptic triggers within the configured time window. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseVibration.kt` | `com.signalgate.pulse.ui.notifications` | object PulseVibration | Provides low-level vibration helpers for Pulse feedback. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingViewModel.kt` | `com.signalgate.pulse.ui.onboarding` | class PermissionItem, class OnboardingViewModel | Drives onboarding permission state, screening-role checks, heuristics selection, and completion persistence. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/onboarding/OnboardingWizardScreen.kt` | `com.signalgate.pulse.ui.onboarding` | class CompletionAction | Renders the multi-step onboarding flow covering agreement, permissions, contacts, sources, and risk threshold selection. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/BlockAllowListScreen.kt` | `com.signalgate.pulse.ui.screens` | No class; top-level declarations only | Renders the user’s searchable manual block and allow rules with add and delete actions. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/CallLogScreen.kt` | `com.signalgate.pulse.ui.screens` | No class; top-level declarations only | Renders the recent screened-call telemetry list. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt` | `com.signalgate.pulse.ui.screens` | No class; top-level declarations only | Renders the primary dashboard with shield status, daily counters, and entry points to application areas. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/LogcatViewerScreen.kt` | `com.signalgate.pulse.ui.screens` | No class; top-level declarations only | Renders the debug-only in-app Logcat viewer. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/PermissionSettingsScreen.kt` | `com.signalgate.pulse.ui.screens` | No class; top-level declarations only | Renders permission, call-screening-role, and battery-optimization status with re-request actions. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsScreens.kt` | `com.signalgate.pulse.ui.screens` | No class; top-level declarations only | Renders application settings, including shield-color controls and navigation to the Logcat viewer. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/SettingsViewModel.kt` | `com.signalgate.pulse.ui.screens` | class SettingsViewModel | Persists and exposes the configurable shield color used by the settings UI. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesScreen.kt` | `com.signalgate.pulse.ui.screens` | No class; top-level declarations only | Renders configured data sources with health state and controls for synchronization, enablement, and deletion. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/screens/SourcesViewModel.kt` | `com.signalgate.pulse.ui.screens` | class SourcesViewModel | Manages source enable/disable, manual/all synchronization, and deletion state for SourcesScreen. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/theme/Color.kt` | `com.signalgate.pulse.ui.theme` | No class; top-level declarations only | Defines the application’s deep-space and neon color palette. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/theme/Effects.kt` | `com.signalgate.pulse.ui.theme` | object SignalGateGlow | Defines reusable glass-panel modifiers and shared glow-color constants. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/theme/SignalGateTheme.kt` | `com.signalgate.pulse.ui.theme` | No class; top-level declarations only | Defines the Material typography and forced-dark SignalGate Compose theme. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/ContactsViewModel.kt` | `com.signalgate.pulse.ui.viewmodels` | class ContactItem, class ContactsViewModel | Loads device contacts, tracks selected contacts, and saves selected contacts to the contacts allow-list source. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/LogcatViewModel.kt` | `com.signalgate.pulse.ui.viewmodels` | class LogcatViewModel | Captures and exposes the most recent Logcat lines for the debug viewer. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/TelemetryViewModel.kt` | `com.signalgate.pulse.ui.viewmodels` | class TelemetryViewModel | Maps persisted call-log entities into UI-facing telemetry call items. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/utils/DateUtils.kt` | `com.signalgate.pulse.utils` | No class; top-level declarations only | Provides timestamp formatting helpers for relative and absolute human-readable dates. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/utils/PhoneNumberUtils.kt` | `com.signalgate.pulse.utils` | object PhoneNumberUtils | Provides phone-number normalization and display-formatting helpers. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt` | `com.signalgate.pulse.workers` | class CommunitySyncWorker | Runs constrained periodic synchronization of federal community sources with retry/backoff behavior. |
| `main` | `android/app/src/main/java/com/signalgate/pulse/workers/SyncBootReceiver.kt` | `com.signalgate.pulse.workers` | class SyncBootReceiver | Reschedules community synchronization after device boot or application replacement. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/CallActionReceiverBehaviorTest.kt` | `com.signalgate.pulse` | class CallActionReceiverBehaviorTest | Verifies that the notification Not Spam action validates its inputs, allowlists the number, dismisses its pending cards, and cancels the notification. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/NotificationPrivacyTest.kt` | `com.signalgate.pulse` | class NotificationPrivacyTest | Verifies that blocked-call notifications obey the configured privacy policy and do not expose disallowed call details. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceEdgeExecutionTest.kt` | `com.signalgate.pulse` | class ScreeningServiceEdgeExecutionTest | Verifies edge execution paths of the Android call-screening service, including decision application and failure handling. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/data/security/ArtifactAuthenticityVerifierTest.kt` | `com.signalgate.pulse.data.security` | class ArtifactAuthenticityVerifierTest | Verifies acceptance and rejection of source artifacts according to authenticity manifests and digests. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SecureCsvParserLimitTest.kt` | `com.signalgate.pulse.data.security` | class SecureCsvParserLimitTest, class GeneratedCsvInputStream | Verifies CSV parser row/input limits and bounded processing behavior for oversized generated input. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SnapshotSanityValidatorTest.kt` | `com.signalgate.pulse.data.security` | class SnapshotSanityValidatorTest | Verifies that source snapshots pass required sanity checks and reject malformed or unsafe content. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/data/security/SourceRecordValidatorTest.kt` | `com.signalgate.pulse.data.security` | class SourceRecordValidatorTest | Verifies validation rules for individual imported source records. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/database/repositories/DataSourceRepositoryDeletionTest.kt` | `com.signalgate.pulse.database.repositories` | class DataSourceRepositoryDeletionTest | Verifies repository behavior when a data source and its associated entries are deleted. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/di/KoinModuleTest.kt` | `com.signalgate.pulse.di` | class KoinModuleTest | Verifies that the configured Koin dependency-injection graph resolves under the JVM test environment. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineDecisionMatrixTest.kt` | `com.signalgate.pulse.logic` | class CallScreeningEngineDecisionMatrixTest | Verifies the call-screening decision matrix across source precedence, confidence, and call conditions. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/logic/CallScreeningEngineSecurityFailureTest.kt` | `com.signalgate.pulse.logic` | class CallScreeningEngineSecurityFailureTest | Verifies that call-screening security failures produce the designated safe failure decision and consequences. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/logic/DataSyncEngineXlsxLimitTest.kt` | `com.signalgate.pulse.logic` | class DataSyncEngineXlsxLimitTest | Verifies bounded XLSX parsing and rejection of inputs that exceed parser limits. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/logic/ScreeningDecisionConsequencesTest.kt` | `com.signalgate.pulse.logic` | class ScreeningDecisionConsequencesTest | Verifies notification and haptic consequences associated with each screening decision. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryMutationBoundaryTest.kt` | `com.signalgate.pulse.logic` | class SecurityRuleRepositoryMutationBoundaryTest | Verifies that security-sensitive rule mutations pass through the repository mutation boundary and enforce validation. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/security/SecurityUtilsTest.kt` | `com.signalgate.pulse.security` | class SecurityUtilsTest | Verifies JVM-testable SecurityUtils encryption, decryption, and passphrase behaviors. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/ui/notifications/PulseTriggerLimiterTest.kt` | `com.signalgate.pulse.ui.notifications` | class PulseTriggerLimiterTest | Verifies that repeated Pulse triggers are throttled according to the limiter window. |
| `test` | `android/app/src/test/kotlin/com/signalgate/pulse/utils/PhoneNumberUtilsTest.kt` | `com.signalgate.pulse.utils` | class PhoneNumberUtilsTest | Verifies phone-number normalization and display-formatting behavior. |

## 2. Dependency and version list files

No `libs.versions.toml`, Maven `pom.xml`, or package-manager manifest was found. The dependency declarations are centralized in the Gradle files below. Versionless Compose modules inherit versions from the pinned Compose BOM.

| File | Versioned build/dependency entries present |
|---|---|
| `android/build.gradle` | Android Gradle Plugin `8.7.0`; Kotlin Gradle Plugin `1.9.24`; KSP `1.9.24-1.0.20`; compile SDK `35`; target SDK `35`; min SDK `29`; build tools `35.0.0`; JVM target `17`. |
| `android/app/build.gradle` | Compose compiler extension `1.5.14`; Koin `3.5.6`; AndroidX Core KTX `1.13.1`; AppCompat `1.7.0`; Material `1.12.0`; Core SplashScreen `1.0.1`; OkHttp `4.12.0`; Timber `5.0.1`; Room `2.6.1`; SQLCipher Android `4.5.5`; AndroidX SQLite `2.4.0`; Compose BOM `2024.09.00`; Activity Compose `1.9.1`; Lifecycle ViewModel Compose `2.8.4`; Coroutines Android `1.8.1`; WorkManager `2.9.1`; Navigation components `2.7.7`; JUnit `4.13.2`; Mockito Core `5.11.0`; Mockito Kotlin `5.3.1`; Koin test artifacts `3.5.6`; AndroidX Test Core `1.5.0`; Robolectric `4.13`; AndroidX Test JUnit `1.1.5`; Espresso `3.5.1`; Test Runner `1.5.2`; Test Rules `1.5.0`. It also includes local `android/app/libs/*.jar` through `fileTree`, with no per-JAR version in the Gradle file. |
| `android/gradle/wrapper/gradle-wrapper.properties` | Gradle distribution `8.9` via `gradle-8.9-all.zip`. |
| `android/gradle.properties` | No library versions; sets JVM arguments, AndroidX/Jetifier flags, and disables the Gradle daemon. |

### Configuration and plugin declarations

| File | Present declarations |
|---|---|
| `android/app/build.gradle` | Applies `com.android.application`, `kotlin-android`, `kotlin-parcelize`, and `com.google.devtools.ksp`; defines `pulse` flavor, debug/release build types, Room KSP schema output, Compose metrics/reporting gates, and lint settings. |
| `android/settings.gradle` | Includes the `:app` module and names the root project `SignalGate`; no version declarations. |

## 3. GitHub Actions workflows

| File | Trigger expression | Existing workflow action |
|---|---|---|
| `.github/workflows/crash-diagnostic.yml` | `workflow_dispatch` only. | Checks out `consumer-v1`, builds the Pulse debug APK, boots an API 33 emulator, launches the package/activity through `scripts/verify-launch-and-capture.sh`, and uploads full Logcat. |
| `.github/workflows/generate-room-schema.yml` | `workflow_dispatch` only. | Checks out the current branch, prints database-version/entity diagnostics, runs Room KSP generation, and uploads `android/app/schemas`. |
| `.github/workflows/metrics.yml` | `workflow_dispatch`; `push` to `consumer-v1`; `pull_request` targeting `consumer-v1`. | Runs Compose metrics generation and analysis for the Pulse variant and uploads the analysis output. |
| `.github/workflows/pulse-ci.yml` | `push` to `consumer-v1`; `pull_request` targeting `consumer-v1`. | Runs architecture-drift check, Pulse debug build, JVM unit tests, lint, Compose-metrics-gate assertion, and uploads build/test/lint artifacts. |
| `.github/workflows/pulse-instrumented-tests.yml` | `push` to `consumer-v1`; `pull_request` targeting `consumer-v1`. | Boots an API 33 emulator, runs `connectedPulseDebugAndroidTest` as a mandatory gate, and uploads instrumented-test results. |

## 4. Unresolved IDs and unresolved references

### Android resource IDs

A source scan found **no `@+id/...` resource declarations, no `@id/...` references, and no `R.id....` references** under `android/app/src/main`. The UI is Compose-based, and the branch contains no XML navigation graph under `android/app/src`. Therefore there are no unresolved Android resource IDs in the scanned source.

### Static unresolved-symbol findings

No unresolved Kotlin class or symbol references were identified by the branch’s existing static inventory. The following present-but-unwired items are recorded as existing structure rather than proposed fixes: `PermissionSettingsScreen` has no `Screen` route or `NavHost` destination, and `TelemetryViewModel` is declared in production code but is not consumed by a screen in the scanned Kotlin sources.

### Manifest/resource references present

The manifest references `@xml/backup_rules`, `@xml/data_extraction_rules`, `@drawable/shield_logo`, `@string/app_name`, `@style/Theme.SignalGate`, `@xml/network_security_config`, and `@style/Theme.App.Starting`; these are resource references, not ID references.


## 5. Navigation graph

Navigation is implemented in `android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt` with `SignalGateNavGraph`, a single Compose `NavHost` whose start destination is `Screen.Dashboard.route`. `Screen.kt` defines eight destinations.

| Route | Screen declaration | Composable destination | Reachability recorded in source |
|---|---|---|---|
| `dashboard` | `Screen.Dashboard` | `ConsumerDashboardScreen` | Start destination; dashboard links to settings and onboarding. |
| `sources` | `Screen.Sources` | `SourcesScreen` | Navigation-drawer entry. |
| `call_log` | `Screen.CallLog` | `CallLogScreen` | Navigation-drawer entry. |
| `block_list` | `Screen.BlockAllowList` | `BlockAllowListScreen` | Navigation-drawer entry. |
| `settings` | `Screen.Settings` | `SettingsScreen` | Dashboard/drawer entry; links to Logcat. |
| `logcat` | `Screen.Logcat` | `LogcatViewerScreen` | Settings entry; content is guarded for debug builds. |
| `onboarding` | `Screen.Onboarding` | `OnboardingWizardScreen` | Dashboard entry. |
| `digest` | `Screen.Digest` | `DigestScreen` | Navigation-drawer entry and deep link `signalgate://digest`. |

### Navigation entry points and manifest relationship

`MainActivity` is the launcher activity and also declares a `VIEW` intent filter for scheme `signalgate` and host `digest`. `NavGraph.kt` registers the matching URI pattern `signalgate://digest` on the `digest` destination. The drawer component lists Dashboard, Sources, Call Log, Block/Allow Lists, Settings, and Blocked Calls; Logcat and Onboarding are reached from in-screen actions, while Digest is also deep-link reachable. `PermissionSettingsScreen` is not represented in `Screen.kt` or `NavGraph.kt`.


## References

[1]: `android/app/build.gradle` — module configuration and dependency declarations.
[2]: `android/build.gradle` — root build plugins, SDK levels, and JVM target.
[3]: `android/gradle/wrapper/gradle-wrapper.properties` — Gradle wrapper distribution.
[4]: `.github/workflows/` — workflow definitions and trigger expressions.
[5]: `android/app/src/main/AndroidManifest.xml` — application components and manifest resource references.
[6]: `android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt` and `Screen.kt` — Compose navigation graph and routes.

