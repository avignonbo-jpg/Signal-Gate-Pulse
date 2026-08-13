# SignalGate Pulse — Source-of-Truth Audit

**Branch audited:** `consumer-v1`
**Module:** `android/app` (namespace `com.signalgate.multipoint`, flavor `pulse` → applicationId `com.signalgate.multipoint.pulse`)
**Scope:** Documentation only — no fixes applied or suggested.

---

## 1. Kotlin Files

### 1.1 Root package — `com.signalgate.multipoint`

| File | Class(es) | Purpose |
|---|---|---|
| `MainApplication.kt` | `MainApplication` (+ private `ReleaseTree`) | App entry point: enables StrictMode, plants Timber trees, starts Koin, synchronously seeds the database, kicks off background bloom-filter rehydration, registers notification channels, and schedules `CommunitySyncWorker`. |
| `MainActivity.kt` | `MainActivity` | The app's single Activity; hosts the Compose UI, nav drawer, and `SignalGateNavGraph`, and shows a dialog when `DatabaseResetEvent` fires. |
| `SignalGateCallScreeningService.kt` | `SignalGateCallScreeningService`, nested `CallDecision` | Android `CallScreeningService` implementation that screens each incoming call via `CallScreeningEngine`, applies the allow/block response, writes audit records, and fires the blocked-call notification. |
| `CallInfo.kt` | `CallTier`, `CallInfo` | Defines the five-tier call classification enum and the `Parcelable` carrier object passed from `CallScreeningEngine` to `SignalGateCallScreeningService`. |
| `PhoneStateReceiver.kt` | `PhoneStateReceiver` | Registered `BroadcastReceiver` for `PHONE_STATE` that is intentionally a documented no-op (retained to prevent reactivating a deleted, conflicting notification path). |
| `CallActionReceiver.kt` | `CallActionReceiver` | Handles the "Not Spam" notification action button: allowlists the number, dismisses the related pending card, and cancels the notification. |

### 1.2 `data/models`

| File | Class(es) | Purpose |
|---|---|---|
| `PermissionStatus.kt` | `PermissionStatus` | Data class describing a single runtime permission's manifest string, description, and grant state. |
| `CallLogItem.kt` | `CallType`, `CallLogItem` | UI-facing model for a single call-log row (type, timestamp, matched sources, risk confidence). |
| `ThreatSource.kt` | `SourceStatus`, `SourceType`, `ThreatSource` | UI-facing model representing a threat-data source's health and configuration. |
| `BenchmarkResult.kt` | `BenchmarkResult` | Data class holding device-benchmark output (I/O speed, memory, storage, throttle support). |

### 1.3 `data/security`

| File | Class(es) | Purpose |
|---|---|---|
| `SanitizationEngine.kt` | `SanitizationEngine` (object) | Central sanitizer for raw phone-number strings and free-text fields, stripping injection-prone characters and enforcing length caps. |
| `SecureCsvParser.kt` | `SecureCsvParser` | Streams a CSV `InputStream` line-by-line (capped at 2,000,000 rows), sanitizing and inserting each number into a Bloom filter as it goes. |
| `BloomFilterEngine.kt` | `BloomFilterEngine` | Custom Bloom filter (MurmurHash3-based) used as a fast, in-memory read-skip optimization ahead of Room queries. |
| `PrecedenceEngine.kt` | `PrecedenceEngine` | Evaluates an incoming number against in-memory allow/block caches and a Bloom filter before falling back to a DB verification callback. |

### 1.4 `database`

| File | Class(es) | Purpose |
|---|---|---|
| `SignalGateDatabase.kt` | `SignalGateDatabase` (abstract), `MIGRATION_1_2` (val) | Room `@Database` declaration (v2, `exportSchema = true`) exposing all six DAOs, plus the explicit 1→2 migration adding `pending_cards`. |
| `SecureDatabase.kt` | `SecureDatabase` (object) | Builds the SQLCipher-encrypted Room database instance, handling native-library loading and Keystore-invalidation recovery (delete + regenerate passphrase). |
| `DatabaseInitializer.kt` | `DatabaseInitializer` (object) | Idempotent first-install seeding of the required `MANUAL` and `Contacts Allow List` `SourceEntity` rows, storing their IDs in `SettingEntry`. |

### 1.5 `database/entities` — `DatabaseEntities.kt`

| Class | Purpose |
|---|---|
| `SourceEntity` | Represents one data source (CSV/XLSX/URL/MANUAL) with health, priority, and enable state. |
| `UnifiedEntryEntity` | A single phone-number rule (BLOCK/ALLOW, exact or pattern) tied to a source, with confidence/risk metadata. |
| `CallLogEntry` | Permanent audit record of every screened call. |
| `SettingEntry` | Generic key/value app-settings row (the SharedPreferences replacement). |
| `SyncHistoryEntry` | Per-source sync run record (added/updated/removed counts, error message). |
| `PendingCardEntity` | Ephemeral post-call digest queue row, deleted on dismissal. |

### 1.6 `database/daos` — `DatabaseDAOs.kt`

| Interface | Purpose |
|---|---|
| `SourceDao` | CRUD + status queries for `SourceEntity`. |
| `UnifiedEntryDao` | CRUD plus priority-joined lookup queries (`findEntriesByPhoneNumberWithPriority`, `getAllBlockPatternsWithPriority`) for `UnifiedEntryEntity`. |
| `CallLogDao` | Insert/query/purge operations for `CallLogEntry`, including range/count queries used by the dashboard. |
| `SettingDao` | Get/set/list operations for `SettingEntry`. |
| `SyncHistoryDao` | Insert/query/purge operations for `SyncHistoryEntry`. |
| `PendingCardDao` | Insert/dismiss/delete/count operations for `PendingCardEntity`. |

### 1.7 `database/repositories`

| File | Class(es) | Purpose |
|---|---|---|
| `DataSourceRepository.kt` | `DataSourceRepository` (+ nested `CallDecision`) | Single source of truth for source/entry data; owns `getCallDecision()` (priority-ordered conflict resolution with Bloom-filter fast-pass) and the sanitizing `insertEntry()` write chokepoint. |
| `CallLogRepository.kt` | `CallLogRepository` | Wraps `CallLogDao`; re-sanitizes phone numbers at the entity write boundary before insert. |
| `BlocklistRepository.kt` | `BlocklistRepository` | Thin facade over `UnifiedEntryDao` for the user's own MANUAL block/allow rules; lazily resolves and caches the manual source ID. |
| `PendingCardRepository.kt` | `PendingCardRepository` | Clean repository wrapper over `PendingCardDao` with validation and logging for the digest queue. |
| `SettingRepository.kt` | `SettingRepository` | Thin repository wrapper over `SettingDao`; provides an upsert (`setSetting`) convenience method. |
| `SyncHistoryRepository.kt` | `SyncHistoryRepository` | Wraps `SyncHistoryDao` for all sync-history reads/writes. |
| `SettingKeys.kt` | `SettingKeys` (object), `HeuristicsMode` (enum) | Centralizes `SettingEntry` key-name constants and defines the four on-device heuristics protection levels with their risk thresholds. |

### 1.8 `logic`

| File | Class(es) | Purpose |
|---|---|---|
| `CallScreeningEngine.kt` | `CallScreeningEngine` | Implements the five-tier priority hierarchy, translating a `DataSourceRepository` decision (plus gray-zone `CallRiskEvaluator` input) into a `CallInfo`. |
| `CallRiskEvaluator.kt` | `CallRiskEvaluator` (object), `RiskEvaluation` | Computes an advisory 0–100 risk score from STIR/SHAKEN verification status and source-match count for gray-zone calls. |
| `DataSyncEngine.kt` | `DataSyncEngine` (+ nested SAX handlers/exceptions) | Memory-safe CSV and native (ZipInputStream + SAX, no Apache POI) XLSX parsing for bulk phone-number import, with byte/row/shared-string caps. |
| `ReliableSourceManager.kt` | `ReliableSourceManager` (+ nested `FetchStrategy`, `FederalSource`, `SyncResult`) | Fetches and syncs the FTC (via a self-hosted GitHub mirror) and FCC federal data sources, streaming CSV through `SecureCsvParser` with per-source fallback URLs. |

### 1.9 `security`

| File | Class(es) | Purpose |
|---|---|---|
| `SecurityUtils.kt` | `SecurityUtils` (object) | Manages the SQLCipher database passphrase via Android Keystore envelope encryption (AES/GCM KEK wrapping a random 256-bit passphrase); also enables StrictMode in debug. |
| `KeystoreInvalidatedException.kt` | `KeystoreInvalidatedException` | Thrown when a stored wrapped passphrase can no longer be decrypted by the current Keystore key. |
| `DatabaseResetEvent.kt` | `DatabaseResetEvent` (object) | Single-flag `StateFlow` signal telling the UI layer the local DB was just reset after a Keystore invalidation. |

### 1.10 `workers`

| File | Class(es) | Purpose |
|---|---|---|
| `CommunitySyncWorker.kt` | `CommunitySyncWorker` | Daily `CoroutineWorker` (exponential backoff, network+battery constraints) that runs `ReliableSourceManager.syncAllFederalSources()`. |
| `SyncBootReceiver.kt` | `SyncBootReceiver` | `BroadcastReceiver` for `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` that re-schedules `CommunitySyncWorker` (OEM background-kill mitigation). |

### 1.11 `di`

| File | Class(es) | Purpose |
|---|---|---|
| `AppModule.kt` | `databaseModule`, `repositoryModule`, `engineModule`, `viewModelModule`, `workerModule`, `appModule` (vals), plus `initializeDatabase()` / `rehydrateBloomFiltersInBackground()` (functions) | Defines every Koin DI binding, layered database → repositories → engines → view models → workers, plus the two startup-sequencing helper functions. |
| `KoinWorkerFactory.kt` | `KoinWorkerFactory` | `WorkerFactory` implementation that resolves `CoroutineWorker` instances through Koin's DI graph. |

### 1.12 `utils`

| File | Class(es) | Purpose |
|---|---|---|
| `PhoneNumberUtils.kt` | `PhoneNumberUtils` (object) | Formats a phone number for display (US/international) and normalizes a number by stripping non-digit/`+` characters. |
| `DateUtils.kt` | (extension) `Long.humanReadable()` | Formats an epoch-millis timestamp as a relative ("5m ago") or absolute date string. |

### 1.13 `ui` (root)

| File | Class(es) | Purpose |
|---|---|---|
| `BlockedNumbersViewModel.kt` | `BlockedNumbersViewModel` (+ nested `Filter`) | Backs `BlockAllowListScreen`: search/filter/add/delete of the user's MANUAL block/allow rules via `BlocklistRepository`. |
| `RecentCallsViewModel.kt` | `RecentCallsViewModel` | Loads recent call-log entries and exposes block/whitelist actions via `CallLogRepository` / `DataSourceRepository`. |

### 1.14 `ui/viewmodels`

| File | Class(es) | Purpose |
|---|---|---|
| `ContactsViewModel.kt` | `ContactItem`, `ContactsViewModel` | Loads device contacts, manages selection state, and saves selected contacts to the Contacts Allow List source during onboarding. |
| `TelemetryViewModel.kt` | `TelemetryViewModel` | Maps `CallLogRepository`'s entity flow into `CallLogItem` UI models for a telemetry-style call feed. |
| `LogcatViewModel.kt` | `LogcatViewModel` | Captures the last 500 lines of `logcat` output for the in-app, debug-only Logcat viewer. |

### 1.15 `ui/dashboard`, `ui/digest`, `ui/onboarding`, `ui/screens` (ViewModels)

| File | Class(es) | Purpose |
|---|---|---|
| `dashboard/DashboardViewModel.kt` | `DashboardViewModel` | Main dashboard state: source LED states, shield (role) status, today's blocked/screened counters, onboarding-complete flag, and sync actions. |
| `digest/PendingCardViewModel.kt` | `PendingCardViewModel` | Backs the digest screen: exposes undismissed cards, and supports dismiss/dismiss-all/mark-as-not-spam. |
| `onboarding/OnboardingViewModel.kt` | `PermissionItem`, `OnboardingViewModel` | Drives the onboarding wizard: permission list/state, heuristics-mode selection, ROLE_CALL_SCREENING check, and onboarding-complete persistence. |
| `screens/SourcesViewModel.kt` | `SourcesViewModel` | Backs `SourcesScreen`: source enable/disable, manual/all sync, and deletion, against `DataSourceRepository`. |
| `screens/SettingsViewModel.kt` | `SettingsViewModel` | Owns shield-color (RGB) persistence for `SettingsScreen` via `SettingRepository`. |

### 1.16 `ui/screens` and `ui/digest`, `ui/onboarding` (Composables)

| File | Function(s) | Purpose |
|---|---|---|
| `screens/ConsumerDashboardScreen.kt` | `ConsumerDashboardScreen` | The app's main/home screen — shield status, today's counters, and navigation into settings/activity/onboarding. |
| `screens/BlockAllowListScreen.kt` | `BlockAllowListScreen` | UI for viewing/searching/adding/deleting the user's manual block and allow rules. |
| `screens/SourcesScreen.kt` | `SourcesScreen` | UI listing configured data sources with health status, sync-now, and enable/disable controls. |
| `screens/CallLogScreen.kt` | `CallLogScreen` | UI listing recent screened calls ("Telemetry Call Log"). |
| `screens/LogcatViewerScreen.kt` | `LogcatViewerScreen` | Debug-only in-app Logcat viewer (renders blank in release via `BuildConfig.DEBUG` guard). |
| `screens/PermissionSettingsScreen.kt` | `PermissionSettingsScreen` | UI for reviewing/re-requesting runtime permissions, role, and battery-optimization status. |
| `screens/SettingsScreens.kt` | `SettingsScreen`, `ColorSlider` | Application settings UI (shield-color RGB sliders, navigation to Logcat viewer). |
| `digest/DigestScreen.kt` | `DigestScreen` | UI for the blocked-call review queue (swipeable pending cards). |
| `onboarding/OnboardingWizardScreen.kt` | `OnboardingWizardScreen`, `EulaStep`, `WelcomeStep`, `PermissionsStep`, `ContactsImportStep`, `ContactRow`, `SourcesSelectionStep`, `RiskThresholdStep` | Multi-step onboarding wizard composables (EULA → welcome → permissions → contacts import → source selection → risk threshold). |

### 1.17 `ui/components`

| File | Function(s) | Purpose |
|---|---|---|
| `GlassCard.kt` | `GlassCard` | Reusable glow/shadow glassmorphic `Box` container. |
| `GlassmorphicCard.kt` | `GlassmorphicCard` | Reusable translucent bordered card container (simpler variant). |
| `AdvancedGlassCard.kt` | `AdvancedGlassCard` | Reusable glassmorphic card with layered gradient background and edge-light border. |
| `GlassmorphicDrawerContent.kt` | `GlassmorphicDrawerContent` | Renders the nav-drawer content (branding header + screen list) used by `MainActivity`. |
| `ShieldStatusGlow.kt` | `ShieldStatusGlow` | Animated pulsing neon shield icon + status text indicator. |
| `SourceIcon.kt` | `SourceIcon` | Small icon badge distinguishing `REMOTE_URL` vs `LOCAL_CSV` source types. |
| `NeonButton.kt` | `NeonButtonStyle`, `NeonButton` | Styled button component with Primary/Success/Warning/Danger color variants. |

### 1.18 `ui/navigation`

| File | Class/Function | Purpose |
|---|---|---|
| `Screen.kt` | `Screen` (sealed class) | Declares every navigation route/title/icon (Dashboard, Sources, CallLog, BlockAllowList, Settings, Logcat, Onboarding, Digest). |
| `NavGraph.kt` | `SignalGateNavGraph` | Builds the `NavHost` wiring each `Screen` route to its composable, including the `signalgate://digest` deep link. |

### 1.19 `ui/theme` and `ui/notifications`

| File | Content | Purpose |
|---|---|---|
| `Color.kt` | Color vals | Defines the "deep space / neon" palette (background, surface, neon cyan/green/red/orange, text colors). |
| `SignalGateTheme.kt` | `SignalGateTypography` (val), `SignalGateTheme` (composable) | Defines Material3 typography and the forced-dark `SignalGateTheme` wrapper. |
| `Effects.kt` | `Modifier.glassPanel()`, `Modifier.glassPanelSmall()`, `SignalGateGlow` (object) | Reusable glassmorphic modifier extensions and glow-color constants. |
| `notifications/NotificationChannelManager.kt` | `NotificationChannelManager` (object) | Single registration point for all three notification channels (blocked-call review, sync status, security alert). |

### 1.20 Unit tests (`src/test`)

| File | Purpose |
|---|---|
| `di/KoinModuleTest.kt` | Verifies the full Koin DI graph resolves, running under `RobolectricTestRunner` for a real shadowed `Context`/`Application`. |
| `security/SecurityUtilsTest.kt` | JVM-only unit tests for non-Keystore-dependent `SecurityUtils` behavior. |
| `utils/PhoneNumberUtilsTest.kt` | Unit tests for `PhoneNumberUtils` formatting/normalization. |

### 1.21 Instrumented tests (`src/androidTest`)

| File | Purpose |
|---|---|
| `database/MigrationTest.kt` | Validates the `MIGRATION_1_2` DDL against Room's generated schema using `MigrationTestHelper`. |
| `database/SourceDeletionCascadeTest.kt` | Verifies `ForeignKey.CASCADE` behavior when a `SourceEntity` is deleted. |
| `security/SecurityUtilsInstrumentedTest.kt` | Tests the real Android-Keystore-backed passphrase envelope-encryption round trip and recovery paths. |

**Total Kotlin files: 77** (68 in `main`, 3 in `test`, 3 in `androidTest`, plus one dual-declaration file `CallInfo.kt` counted once).

---

## 2. Dependencies (`android/app/build.gradle`, `android/build.gradle`)

### 2.1 Build tooling / plugin versions (root `build.gradle`)

| Item | Version |
|---|---|
| Android Gradle Plugin | `8.7.0` |
| Kotlin Gradle Plugin | `1.9.24` |
| KSP | `1.9.24-1.0.20` |
| compileSdk / targetSdk | `35` |
| minSdk | `29` |
| buildToolsVersion | `35.0.0` |
| Compose Compiler extension | `1.5.14` |
| Java/Kotlin JVM target | `17` |

### 2.2 App-module dependencies

| Library | Version | Configuration |
|---|---|---|
| `io.insert-koin:koin-android` | `3.5.6` | implementation |
| `io.insert-koin:koin-androidx-compose` | `3.5.6` | implementation |
| local `libs/*.jar` (fileTree) | — | implementation |
| `androidx.core:core-ktx` | `1.13.1` | implementation |
| `androidx.appcompat:appcompat` | `1.7.0` | implementation |
| `com.google.android.material:material` | `1.12.0` | implementation |
| `com.squareup.okhttp3:okhttp` | `4.12.0` | implementation |
| `com.jakewharton.timber:timber` | `5.0.1` | implementation |
| `androidx.room:room-runtime` | `2.6.1` | implementation |
| `androidx.room:room-ktx` | `2.6.1` | implementation |
| `androidx.room:room-compiler` | `2.6.1` | ksp |
| `net.zetetic:sqlcipher-android` | `4.5.5` | implementation |
| `androidx.sqlite:sqlite` | `2.4.0` | implementation |
| `androidx.compose:compose-bom` | `2024.09.00` | implementation (platform) |
| `androidx.compose.ui:ui` | BOM-managed | implementation |
| `androidx.compose.material3:material3` | BOM-managed | implementation |
| `androidx.compose.material:material-icons-extended` | BOM-managed | implementation |
| `androidx.compose.ui:ui-tooling-preview` | BOM-managed | implementation |
| `androidx.activity:activity-compose` | `1.9.1` | implementation |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.4` | implementation |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.8.1` | implementation |
| `androidx.work:work-runtime-ktx` | `2.9.1` | implementation |
| `androidx.navigation:navigation-fragment-ktx` | `2.7.7` | implementation |
| `androidx.navigation:navigation-ui-ktx` | `2.7.7` | implementation |
| `androidx.navigation:navigation-compose` | `2.7.7` | implementation |
| `junit:junit` | `4.13.2` | testImplementation |
| `org.mockito:mockito-core` | `5.11.0` | testImplementation |
| `org.mockito.kotlin:mockito-kotlin` | `5.3.1` | testImplementation |
| `io.insert-koin:koin-test` | `3.5.6` | testImplementation |
| `io.insert-koin:koin-test-junit4` | `3.5.6` | testImplementation |
| `io.insert-koin:koin-android-test` | `3.5.6` | testImplementation |
| `androidx.room:room-testing` | `2.6.1` | testImplementation |
| `androidx.test:core` | `1.5.0` | testImplementation |
| `org.robolectric:robolectric` | `4.13` | testImplementation |
| `androidx.test.ext:junit` | `1.1.5` | androidTestImplementation |
| `androidx.test.espresso:espresso-core` | `3.5.1` | androidTestImplementation |
| `androidx.test:runner` | `1.5.2` | androidTestImplementation |
| `androidx.test:rules` | `1.5.0` | androidTestImplementation |
| `androidx.room:room-testing` | `2.6.1` | androidTestImplementation |

### 2.3 Plugins applied (`app/build.gradle`)

`com.android.application`, `kotlin-android`, `kotlin-parcelize`, `com.google.devtools.ksp`

### 2.4 Notable build config

- Product flavor `pulse` (dimension `version`) → `applicationId "com.signalgate.multipoint.pulse"`.
- `release` build type: `minifyEnabled true`, `shrinkResources true`, signs from `RELEASE_KEYSTORE_*` env vars (fails loudly if unset).
- `debug` build type: signs with the default debug config.
- `ksp { arg("room.incremental", "true"); arg("room.schemaLocation", "$projectDir/schemas") }`.
- `lint { abortOnError true; disable 'RemoveWorkManagerInitializer' }`.
- `sourceSets.androidTest.assets.srcDirs += "$projectDir/schemas"` (for `MigrationTestHelper`).

---

## 3. GitHub Actions Workflows (`.github/workflows/`)

| File | Trigger(s) | What it does |
|---|---|---|
| `pulse-ci.yml` ("Pulse Consumer CI") | `push` and `pull_request` on branch `consumer-v1` | Builds `assemblePulseDebug`, runs `testPulseDebugUnitTest` (continue-on-error), runs `lintPulseDebug`, uploads the debug APK / test results / lint report as artifacts, and asserts the compose-metrics gate in `app/build.gradle` is still present. |
| `crash-diagnostic.yml` ("Crash Diagnostic") | `workflow_dispatch` only | Checks out `consumer-v1`, builds `assemblePulseDebug`, boots an API 33 `google_apis` x86_64 emulator, installs the APK, runs `scripts/verify-launch-and-capture.sh` to launch `com.signalgate.multipoint.pulse/com.signalgate.multipoint.MainActivity` and poll for the running process, and uploads the full logcat capture. |
| `generate-room-schema.yml` ("Generate Room Schema 2") | `workflow_dispatch` only | Checks out the current branch, prints the `SignalGateDatabase` version/`PendingCardEntity` grep for diagnostics, runs `:app:kspPulseDebugKotlin` to generate Room schema JSON, and uploads `android/app/schemas` as an artifact. |
| `metrics.yml` ("Compose Metrics CI") | `workflow_dispatch`, `push`/`pull_request` on branch `consumer-v1` | Sets up JDK + Android SDK, runs `scripts/analyze-compose-metrics.sh --pulse`, runs `tools/metrics-analysis/analyze_metrics.py` against the generated report, and uploads the analysis artifacts. |

---

## 4. Unresolved Errors / Duplicate Classes

No duplicate class or filename collisions were found in this snapshot — every `.kt` file under `main`, `test`, and `androidTest` has a unique filename, and no two files declare the same top-level class name. The specific issues flagged in the prior audit session (`SecureCsvParser` wiring gap, `ReliableSourceManager` constructor/Koin binding mismatch, duplicate `ReliableSourceManager.kt`, `crash-diagnostic.yml`'s hardcoded launch target, `KoinModuleTest` failing without Robolectric) are **not present in this snapshot** — each shows the corresponding fix already in place (see inline code comments documenting each fix, e.g. in `ReliableSourceManager.kt`, `AppModule.kt`, `verify-launch-and-capture.sh`, and `KoinModuleTest.kt`'s `@RunWith(RobolectricTestRunner::class)`).

Findings that remain in this snapshot:

- **`ui/screens/PermissionSettingsScreen.kt` is not reachable.** It is not referenced by `Screen.kt` (no route defined for it) or `ui/navigation/NavGraph.kt` (no `composable(...)` entry), and it is not registered as needing its own ViewModel binding in `AppModule.kt`. The function `PermissionSettingsScreen` exists and compiles but has no navigation path to it from any other screen in this snapshot.
- **`ui/viewmodels/TelemetryViewModel.kt` is registered in Koin (`viewModelModule`) but not consumed by any screen.** No `.kt` file under `ui/screens` or elsewhere calls `koinViewModel<TelemetryViewModel>()` or otherwise instantiates it. This matches the "orphan-screen problem" the `SettingsViewModel.kt` doc comment references as a separately-tracked, not-yet-addressed item.
- **Documentation/comment inconsistency in `app/build.gradle`:** the comment above the `ksp { arg("room.schemaLocation", ...) }` block states *"Phase 2.6: required for MigrationTestHelper — exportSchema = false in @Database"*, but `database/SignalGateDatabase.kt`'s actual `@Database` annotation has `exportSchema = true` (and its own doc comment states this was deliberately changed from `false`). The build.gradle comment's `exportSchema = false` claim does not match the current entity source.
- **`ui/components/ShieldStatusGlow.kt`** builds its native `Paint` colors via `glowColor.copy(alpha = pulseAlpha).hashCode()` and `glowColor.hashCode()` rather than an ARGB-int conversion (e.g. `.toArgb()`). This compiles without error, but `Color.hashCode()` is not defined to produce a valid ARGB packed int, so the rendered glow/stroke color is not guaranteed to match `glowColor`.

No other unresolved references, missing symbols, or structurally duplicate classes were found by static inspection of this snapshot.

---

## 5. UI Entry Point & Navigation Graph

**Application entry point:** `MainApplication` (`android:name=".MainApplication"` in `AndroidManifest.xml`) — starts Koin, seeds the database, and schedules background work in `onCreate()`.

**Activity entry point:** `MainActivity` is the app's single `Activity` (`android:launchMode="singleTask"`, `LAUNCHER` intent-filter, plus a `VIEW` intent-filter for the `signalgate://digest` deep link). It sets Compose content: `SignalGateTheme` → `ModalNavigationDrawer` (drawer content from `GlassmorphicDrawerContent`) → `Scaffold` → `SignalGateNavGraph`.

**Navigation graph** (`ui/navigation/NavGraph.kt`, `SignalGateNavGraph`) — a single `NavHost` with `startDestination = Screen.Dashboard.route`:

| Route (`Screen`) | Composable | Notes |
|---|---|---|
| `dashboard` (start destination) | `ConsumerDashboardScreen` | Navigates to Settings, Digest ("Activity"), and Onboarding. |
| `sources` | `SourcesScreen` | |
| `call_log` | `CallLogScreen` | |
| `block_list` | `BlockAllowListScreen` | |
| `settings` | `SettingsScreen` | Navigates to Logcat. |
| `logcat` | `LogcatViewerScreen` | Debug-build-only content. |
| `onboarding` | `OnboardingWizardScreen` | |
| `digest` | `DigestScreen` | Also reachable via deep link `signalgate://digest` (matches the manifest's `MainActivity` `VIEW` intent-filter). |

**Not present in the nav graph:** `Screen.kt` defines only the eight routes above; `PermissionSettingsScreen` has no corresponding `Screen` entry or `composable(...)` registration (see §4).

**Drawer navigation** (`GlassmorphicDrawerContent`) lists five of the eight screens: Dashboard, Sources, Call Log, Block/Allow List, Settings — Logcat, Onboarding, and Digest are reached only via in-screen navigation or deep link, not the drawer.
