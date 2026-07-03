package com.signalgate.multipoint.di

import android.content.Context
import com.signalgate.multipoint.database.DatabaseInitializer
import com.signalgate.multipoint.database.SecureDatabase
import com.signalgate.multipoint.database.SignalGateDatabase
import com.signalgate.multipoint.database.daos.CallLogDao
import com.signalgate.multipoint.database.daos.SettingDao
import com.signalgate.multipoint.database.daos.SourceDao
import com.signalgate.multipoint.database.repositories.BlocklistRepository
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import com.signalgate.multipoint.database.repositories.SyncHistoryRepository
import com.signalgate.multipoint.database.repositories.PendingCardRepository
import com.signalgate.multipoint.logic.CallRiskEvaluator
import com.signalgate.multipoint.logic.CallScreeningEngine
import com.signalgate.multipoint.logic.DataSyncEngine
import com.signalgate.multipoint.logic.ReliableSourceManager
import com.signalgate.multipoint.ui.BlockedNumbersViewModel
import com.signalgate.multipoint.ui.RecentCallsViewModel
import com.signalgate.multipoint.ui.dashboard.DashboardViewModel
import com.signalgate.multipoint.ui.digest.PendingCardViewModel
import com.signalgate.multipoint.ui.onboarding.OnboardingViewModel
import com.signalgate.multipoint.ui.viewmodels.ContactsViewModel
import com.signalgate.multipoint.ui.viewmodels.LogcatViewModel
import com.signalgate.multipoint.ui.viewmodels.TelemetryViewModel
import com.signalgate.multipoint.workers.CommunitySyncWorker
import com.signalgate.multipoint.data.security.BloomFilterEngine
import com.signalgate.multipoint.data.security.PrecedenceEngine
import com.signalgate.multipoint.data.security.SecureCsvParser
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * AppModule — Koin DI bindings per Architecture Contract §2.
 *
 * Module split (one per layer):
 *   databaseModule  — SignalGateDatabase, all DAOs
 *   repositoryModule — all four repositories
 *   engineModule    — L2 transport, L4 sanitization, L6 decision logic
 *   viewModelModule — one viewModel{} per screen (Contract §4)
 *   workerModule    — CoroutineWorker bindings via KoinWorkerFactory (Contract §2)
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
    single { BlocklistRepository(get(), -1) }
}

/**
 * repositoryModule — Provides repository layer (L4 Transport boundary).
 * Scope: Single instance per app lifetime.
 *
 * Repositories mediate all access between domain logic (L5+) and persistence (L2).
 * No direct DAO access from UI or engines — all goes through repositories.
 *
 * Step 0.1 Changes:
 * - BlocklistRepository: No longer uses runBlocking. Default sourceSettingEntry in Step 2.4.
 *
 * - DashboardViewModel: Now receives CallLogRepository in addition to DataSourceRepository.
 *   Enables direct access to CallLogDao for call count queries (refreshCounters).
 */
val repositoryModule = module {
    single { DataSourceRepository(get(), get()) }
    single { CallLogRepository(get()) }
    single { SyncHistoryRepository(get()) }

    /**
     * Step 0.1 / 2.4 (2026-07-02): BlocklistRepository binding refactored.
     * 
     * BEFORE: Used runBlocking to synchronously fetch "Manual User Rules" source ID.
     * PROBLEM: Blocked app startup; violated Architecture Contract §2.
     * 
     * AFTER: Accepts default sourceId = -1.
     * Manual source ID is now fetched asynchronously via SettingEntry in Step 2.4.
     * 
     * Migration path:
     * 1. Step 2.4: Implement async SettingEntry read for manual_source_id
     * 2. Step 2.4: Update BlocklistRepository to use SettingEntry lookup
     * 3. Step 2.4: Remove default -1 once SettingEntry is stable
     */
    single { BlocklistRepository(get(), -1) }
}

/**
 * engineModule — Provides business logic engines and transport layer (L2, L4, L6).
 * Scope: Single instance per app lifetime.
 *
 * Layer responsibilities:
 * - L2 (Data Link): Transport, OkHttp, TLS, timeouts
 * - L4 (Network): Input sanitization, CSV parsing
 * - L6 (Presentation Logic): Call risk evaluation, screening decisions
 *
 * These engines are stateless and safe to share as singletons.
 */
val engineModule = module {
    // L4 — input sanitization boundary
    single { BloomFilterEngine() }
    single { SecureCsvParser(get()) }
    single {
        PrecedenceEngine(
            bloomFilter = get(),
            localAllowListCache = hashSetOf(),
            localManualBlockListCache = hashSetOf()
        )
    }

    // L2 — transport boundary (OkHttp, TLS, timeouts owned here)
    single { ReliableSourceManager(get(), get()) }

    // L6 — decision boundary
    // CallRiskEvaluator is a stateless object — registered so CallScreeningEngine
    // receives it via constructor injection for testability.
    single { CallRiskEvaluator }
    single { CallScreeningEngine(get(), get()) }
    single { DataSyncEngine(get(), get()) }
}

/**
 * viewModelModule — Provides ViewModel instances for all screens (L6 Presentation).
 * Scope: Per-screen lifecycle (Compose Navigation handles creation/destruction).
 *
 * Step 0.1 Change:
 * - DashboardViewModel now receives CallLogRepository as 2nd parameter.
 *   Used for direct CallLogDao access in refreshCounters() method (Step 2.5).
 *
 * Architecture Contract §4: Each screen gets exactly one ViewModel.
 * ViewModels are the sole interface between Composables (L7) and business logic (L5-L6).
 */
val viewModelModule = module {
    viewModel { ContactsViewModel(get(), get(), get()) }
    viewModel { TelemetryViewModel(get()) }
    viewModel { DashboardViewModel(get(), get()) } // Step 0.1: Added CallLogRepository parameter
    viewModel { BlockedNumbersViewModel(get()) }
    viewModel { RecentCallsViewModel(get(), get()) }
    viewModel { LogcatViewModel() }
    viewModel { OnboardingViewModel() }
    viewModel { PendingCardViewModel(get(), get()) }
}

/**
 * workerModule — Provides CoroutineWorker instances via KoinWorkerFactory.
 * Scope: Per-work lifecycle (WorkManager controls creation).
 *
 * Architecture Contract §2: Every CoroutineWorker shipping in v1 must be registered here.
 * Unregistered workers are not shippable.
 *
 * Step 0.1 (2026-07-02): workerModule activated.
 * CommunitySyncWorker is now registered and scheduled daily via MainApplication.scheduleCommunitySync().
 *
 * KoinWorkerFactory resolves workers from this module when WorkManager needs them.
 * Ensures constructor injection is available for all background tasks.
 */
val workerModule = module {
    factory { (context: android.content.Context, params: androidx.work.WorkerParameters) ->
        CommunitySyncWorker(context, params, get())
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
 */
suspend fun initializeDatabase(context: Context) {
    val koin = org.koin.core.context.GlobalContext.get()
    val sourceDao = koin.get<SourceDao>()
    val settingDao = koin.get<SettingDao>()
    DatabaseInitializer.seedRequiredSources(context, sourceDao, settingDao)
}
