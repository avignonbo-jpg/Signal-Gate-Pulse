package com.signalgate.pulse.di

import android.content.Context
import com.signalgate.pulse.database.DatabaseInitializer
import com.signalgate.pulse.database.SecureDatabase
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.daos.CallLogDao
import com.signalgate.pulse.database.daos.SettingDao
import com.signalgate.pulse.database.daos.SourceDao
import com.signalgate.pulse.database.repositories.BlocklistRepository
import com.signalgate.pulse.database.repositories.CallLogRepository
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SyncHistoryRepository
import com.signalgate.pulse.database.repositories.PendingCardRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import com.signalgate.pulse.logic.CallRiskEvaluator
import com.signalgate.pulse.logic.CallScreeningEngine
import com.signalgate.pulse.logic.DataSyncEngine
import com.signalgate.pulse.logic.ReliableSourceManager
import com.signalgate.pulse.logic.SecurityRuleRepository
import com.signalgate.pulse.logic.SourceSyncUseCase
import com.signalgate.pulse.ui.BlockedNumbersViewModel
import com.signalgate.pulse.ui.RecentCallsViewModel
import com.signalgate.pulse.ui.dashboard.DashboardViewModel
import com.signalgate.pulse.ui.digest.PendingCardViewModel
import com.signalgate.pulse.ui.onboarding.OnboardingViewModel
import com.signalgate.pulse.ui.notifications.PulseHapticsController
import com.signalgate.pulse.ui.notifications.PulseTriggerLimiter
import com.signalgate.pulse.ui.screens.SettingsViewModel
import com.signalgate.pulse.ui.screens.SourcesViewModel
import com.signalgate.pulse.ui.viewmodels.ContactsViewModel
import com.signalgate.pulse.ui.viewmodels.LogcatViewModel
import com.signalgate.pulse.workers.CommunitySyncWorker
import com.signalgate.pulse.StartupDiagnostics
import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.data.security.PrecedenceEngine
import com.signalgate.pulse.data.security.SecureCsvParser
import com.signalgate.pulse.data.security.SnapshotSanityValidator
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * AppModule — Koin DI bindings per Architecture Contract §2.
 *
 * Module split (one per layer):
 *   databaseModule  — Layer 3 Persistence: SignalGateDatabase, all DAOs
 *   repositoryModule — Layer 3 Persistence: all repositories
 *   engineModule    — Layer 2 Security/Parsing and Layer 4 Domain engines
 *   viewModelModule — Layer 6 Presentation: one viewModel{} per screen (Contract §4)
 *   workerModule    — Layer 1 Platform/Edge CoroutineWorker bindings via KoinWorkerFactory (Contract §2)
 *
 * Startup ordering (BINDING per Contract §2):
 * DatabaseInitializer.seedRequiredSources() completes synchronously in
 * MainApplication.onCreate() via runBlocking BEFORE any inbound caller can
 * resolve a Koin binding. Async seeding is explicitly forbidden.
 *
 * Step 0.1 (2026-07-02) — Phase 0 Foundation Hardening:
 * ✓ runBlocking removed from BlocklistRepository binding
 * ✓ BlocklistRepository now accepts default -1 sourceId (async load via Step 2.4)
 * ✓ DashboardViewModel updated to accept CallLogRepository (Step 2.5)
 * ✓ workerModule activated for CommunitySyncWorker (Step 3.4)
 * ✓ All CI drift-detection violations fixed
 */

/**
 * databaseModule — Provides Room database and all DAOs.
 * Scope: Single instance per app lifetime.
 *
 * DAOs exposed:
 *   - SourceDao: CRUD on data sources
 *   - UnifiedEntryDao: CRUD on unified blocklist entries
 *   - CallLogDao: CRUD on call screening history
 *   - SettingDao: CRUD on app settings
 *   - SyncHistoryDao: CRUD on sync history
 *   - PendingCardDao: CRUD on pending notification cards
 */
val databaseModule = module {
    single<SignalGateDatabase> {
        SecureDatabase.getDatabase(androidContext())
    }
    single { get<SignalGateDatabase>().sourceDao() }
    single { get<SignalGateDatabase>().unifiedEntryDao() }
    single { get<SignalGateDatabase>().callLogDao() }
    single { get<SignalGateDatabase>().settingDao() }
    single { get<SignalGateDatabase>().syncHistoryDao() }
    single { get<SignalGateDatabase>().pendingCardDao() }
    single { PendingCardRepository(get()) }  // Phase 1.4
}

/**
 * repositoryModule — Provides Layer 3 Persistence repositories.
 * Scope: Single instance per app lifetime.
 *
 * Repositories mediate access between Layer 4 Domain/Layer 5 Application logic
 * and Layer 3 Persistence. No direct DAO access crosses the UI boundary.
 * No direct DAO access from UI or engines — all goes through repositories.
 *
 * Step 0.1 Changes:
 * - BlocklistRepository: No longer uses runBlocking. Default sourceSettingEntry in Step 2.4.
 *
 * - DashboardViewModel: Now receives CallLogRepository in addition to DataSourceRepository.
 *   Enables direct access to CallLogDao for call count queries (refreshCounters).
 *
 * Bloom fast-pass wiring (this session): DataSourceRepository now also takes the
 * two BloomFilterEngine singletons from engineModule — the default (unqualified)
 * instance for exact phone numbers, and the "patternBloom" qualified instance for
 * block-pattern prefixes. Both are pre-existing engineModule singletons; nothing
 * new is declared as a dependency here that didn't already exist in the graph.
 */
val repositoryModule = module {
    single { DataSourceRepository(get(), get(), get(), get(named("patternBloom"))) }
    single { CallLogRepository(get()) }
    single { SyncHistoryRepository(get()) }

    /**
     * Step 0.1 / 2.4 (2026-07-02): BlocklistRepository binding refactored.
     * 
     * BEFORE: Used runBlocking to synchronously fetch "Manual User Rules" source ID.
     * PROBLEM: Blocked app startup; violated Architecture Contract §2.
     * 
     * INTERIM STATE (left unfixed until Phase 4.2): Accepted a hardcoded
     * sourceId = -1, with a migration path noted here that was never carried
     * out. This was never caught earlier because nothing called through
     * BlocklistRepository yet — BlockAllowListScreen was still a stub. Every
     * addBlockRule()/addAllowRule() call would have failed the UnifiedEntryEntity
     * -> SourceEntity foreign key constraint the first time it was actually used.
     *
     * FIXED (Phase 4.2, this session): BlocklistRepository now takes a
     * SettingRepository and resolves + caches the real "manual_source_id"
     * lazily on first use (see BlocklistRepository.manualSourceId()) — that
     * setting is written synchronously by DatabaseInitializer.seedRequiredSources()
     * before any Koin binding is resolved, so no runBlocking is needed here.
     *
     * Phase 0.1 (Security Control-Plane Integrity, §11.7): SecurityRuleRepository
     * is now the authoritative mutation boundary (§5.2) — it takes
     * DataSourceRepository (for authoritative writes and post-commit derived-index
     * rebuilding), UnifiedEntryDao (for removeRule(), which legitimately bypasses
     * the insertion boundary — see the class doc), and SettingRepository (for the same
     * manual_source_id lookup BlocklistRepository used to own directly).
     * BlocklistRepository is now a thin deprecated facade over it — see that
     * class's doc — so its own binding shrinks to a single dependency.
     */
    single { SecurityRuleRepository(get(), get(), get(), get()) }
    single { BlocklistRepository(get()) }
    single { SettingRepository(get()) } // Added for architecture drift fix — Roadmap Step 0.4
}

/**
 * engineModule — Provides Layer 2 Security/Parsing, Layer 4 Domain, and
 * Layer 5 Application engines.
 * Scope: Single instance per app lifetime.
 *
 * Layer responsibilities:
 * - Layer 2 Security/Parsing: secure CSV parsing and input normalization
 * - Layer 4 Domain: call-risk evaluation and screening decisions
 * - Layer 5 Application: reliable-source transport and synchronization orchestration
 *
 * These engines are stateless and safe to share as singletons.
 */
val engineModule = module {
    // Layer 2 Security/Parsing — input sanitization boundary
    single { BloomFilterEngine() }
    // Bloom fast-pass wiring (this session): a second, separate BloomFilterEngine
    // instance dedicated to block-pattern prefixes (see DataSourceRepository's
    // matchesAnyPatternPrefix()). Kept separate from the exact-number instance
    // above rather than sharing one bit array — mixing two different string
    // domains (full numbers vs. short prefixes) into one filter would inflate
    // the false-positive rate for both without any benefit.
    single(named("patternBloom")) { BloomFilterEngine() }
    single { SecureCsvParser() }
    single { SnapshotSanityValidator() }
    single {
        PrecedenceEngine(
            bloomFilter = get(),
            localAllowListCache = hashSetOf(),
            localManualBlockListCache = hashSetOf()
        )
    }

    // Layer 5 Application — transport boundary (OkHttp, TLS, timeouts owned here)
    // Security fix (audit finding): third get() resolves the SecureCsvParser
    // singleton registered above — ReliableSourceManager now streams federal CSV
    // feeds through it instead of a hand-rolled parser. See ReliableSourceManager.
    single { ReliableSourceManager(get(), get(), get(), get(), get()) }
    single { SourceSyncUseCase(get()) }

    // Layer 4 Domain — decision boundary
    // CallRiskEvaluator is a stateless object — registered so CallScreeningEngine
    // receives it via constructor injection for testability.
    single { CallRiskEvaluator }
    single { CallScreeningEngine(get(), get(), get()) } // 3rd get() = SettingRepository, for HeuristicsMode (onboarding Step 3)
    single { DataSyncEngine(get(), get()) }
}

/**
 * viewModelModule — Provides Layer 6 Presentation ViewModel instances for all screens.
 * Scope: Per-screen lifecycle (Compose Navigation handles creation/destruction).
 *
 * Step 0.1 Change:
 * - DashboardViewModel now receives CallLogRepository as 2nd parameter.
 *   Used for direct CallLogDao access in refreshCounters() method (Step 2.5).
 *
 * Architecture Contract §4: Each screen gets exactly one ViewModel.
 * ViewModels are the sole interface between Layer 7 UI Composables and
 * Layer 5 Application/Layer 4 Domain logic.
 */
val viewModelModule = module {
    viewModel { ContactsViewModel(get(), get(), get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get()) } // Phase 0.4: real source sync boundary added

    // Phase 4.2: constructor dependency changed from DataSourceRepository to
    // BlocklistRepository (now backs the real BlockAllowListScreen instead of
    // a dead, never-navigated-to class). get() resolves by the new type.
    viewModel { BlockedNumbersViewModel(get()) }
    viewModel { RecentCallsViewModel(get(), get()) }
    viewModel { LogcatViewModel() }
    viewModel { OnboardingViewModel(get()) } // Step 2.6: added SettingRepository for markOnboardingComplete
    viewModel { SettingsViewModel(get()) } // Step 2.6: new — owns shield-color persistence, resolves half of FLAG-1
    viewModel { PendingCardViewModel(get(), get()) }
    viewModel { SourcesViewModel(get(), get()) } // Phase 0.4: real source sync boundary added
}

/**
 * notificationModule — Provides the Phase 2.1 UX consequence consumers.
 * Haptics and rate limiting are downstream of ScreeningDecision; neither may
 * alter the domain decision, audit record, or persisted review-card contract.
 */
val notificationModule = module {
    single { PulseHapticsController(androidContext()) }
    single { PulseTriggerLimiter() }
}

/**
 * workerModule — Provides CoroutineWorker instances via KoinWorkerFactory.
 * Scope: Per-work lifecycle (WorkManager controls creation).
 *
 * Architecture Contract §2: Every CoroutineWorker shipping in v1 must be registered here.
 * Unregistered workers are not shippable.
 *
 * Step 0.1 (2026-07-02): workerModule activated.
 * CommunitySyncWorker is now registered here and scheduled daily via its own
 * companion function, CommunitySyncWorker.schedule(context) — called once from
 * MainApplication.onCreate(). (Fixed: this used to be duplicated in a second,
 * constraint-less scheduling path inside MainApplication itself; that has
 * been removed so there is exactly one place this job is enqueued.)
 *
 * KoinWorkerFactory resolves workers from this module when WorkManager needs them.
 * Ensures constructor injection is available for all background tasks.
 */
val workerModule = module {
    // Fixed (Phase 0.3): previously passed a 3rd argument, get(), to a constructor
    // that only takes (context, params) — CommunitySyncWorker resolves its own
    // dependencies (ReliableSourceManager, DataSyncEngine) via `by inject()`
    // internally, so KoinWorkerFactory's parametersOf(appContext, workerParameters)
    // is already everything this factory needs to forward.
    factory { (context: android.content.Context, params: androidx.work.WorkerParameters) ->
        CommunitySyncWorker(context, params)
    }
}

/**
 * appModule — Master list of all modules.
 * Passed to Koin.startKoin() in MainApplication.onCreate().
 *
 * Order matters: Database → Repositories → Engines → ViewModels → Workers.
 * Dependencies flow downward; each layer depends only on layers below it.
 */
val appModule = listOf(
    databaseModule,
    repositoryModule,
    engineModule,
    viewModelModule,
    notificationModule,
    workerModule
)

/**
 * initializeDatabase — Seeds required sources before any Koin binding is resolved.
 *
 * Called synchronously in MainApplication.onCreate() via runBlocking.
 * Contract §2 requirement: Database initialization must complete BEFORE any
 * CallScreeningService callback can resolve a Koin binding (BlocklistRepository).
 *
 * Step 0.1 (2026-07-02): No changes. Still called synchronously.
 * runBlocking was removed from BlocklistRepository binding, not from this function.
 *
 * Bloom fast-pass wiring — REVISED this session: rehydrateBloomFilters() used
 * to be called from here, inside this synchronous startup path. That's been
 * removed — see rehydrateBloomFiltersInBackground() below and
 * DataSourceRepository's class doc for why bloom rehydration is NOT binding
 * the way seedRequiredSources() above it is, and why running it here was a
 * real risk to the CallScreeningService response window at production scale.
 */
suspend fun initializeDatabase(context: Context) {
    StartupDiagnostics.mark(StartupDiagnostics.Event.SOURCE_SEED_BEGIN)
    val koin = org.koin.core.context.GlobalContext.get()
    val sourceDao = koin.get<SourceDao>()
    val settingDao = koin.get<SettingDao>()
    DatabaseInitializer.seedRequiredSources(context, sourceDao, settingDao)
    StartupDiagnostics.mark(StartupDiagnostics.Event.SOURCE_SEED_END)
}

/**
 * rehydrateBloomFiltersInBackground — kicks off DataSourceRepository's bloom
 * filter rebuild on [scope], deliberately off the startup-blocking path.
 *
 * Call this from MainApplication.onCreate() AFTER the runBlocking block that
 * calls initializeDatabase() — not inside it, and not in parallel with it,
 * since rehydration reads via entryDao.getAllEntries() and depends on the
 * DB actually being open (which SecureDatabase.getDatabase() lazily handles
 * on first DAO access, but there's no reason to race it against seeding).
 *
 * Why this is safe to run unawaited: BloomFilterEngine is a pure read-skip
 * optimization (see DataSourceRepository class doc) — getCallDecision() is
 * fully correct with unrehydrated (or partially-rehydrated) filters, it just
 * doesn't get the fast-pass speedup until this completes. Unlike
 * seedRequiredSources(), nothing here can throw and crash a caller that
 * hasn't finished yet — DataSourceRepository's bloomReady flag handles that.
 *
 * This does NOT reintroduce the async-seeding pattern the class doc above
 * calls out as forbidden — that prohibition is specifically about
 * seedRequiredSources() (the MANUAL source row other bindings structurally
 * depend on). Bloom rehydration has no such dependency.
 */
fun rehydrateBloomFiltersInBackground(scope: CoroutineScope) {
    val koin = org.koin.core.context.GlobalContext.get()
    scope.launch {
        koin.get<DataSourceRepository>().rehydrateBloomFilters()
    }
}
