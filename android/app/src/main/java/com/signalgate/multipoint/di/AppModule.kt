package com.signalgate.multipoint.di

import android.content.Context
import com.signalgate.multipoint.database.DatabaseInitializer
import com.signalgate.multipoint.database.SecureDatabase
import com.signalgate.multipoint.database.SignalGateDatabase
import com.signalgate.multipoint.database.daos.SettingDao
import com.signalgate.multipoint.database.daos.SourceDao
import com.signalgate.multipoint.database.repositories.BlocklistRepository
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import com.signalgate.multipoint.database.repositories.SyncHistoryRepository
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
import kotlinx.coroutines.runBlocking
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
}

val repositoryModule = module {
    single { DataSourceRepository(get(), get()) }
    single { CallLogRepository(get()) }
    single { SyncHistoryRepository(get()) }

    // runBlocking intentional — single indexed DB read, runs once at startup after
    // seedRequiredSources() has completed. See MainApplication doc for context.
    // PULSE-TODO (2026-06): replace with SettingEntry key read in Step 2.4.
    single {
        val sourceDao = get<SourceDao>()
        val manualSourceId = runBlocking {
            sourceDao.getSourceByName("Manual User Rules")?.id
                ?: error(
                    "Manual source row not found — DatabaseInitializer.seedRequiredSources() " +
                    "must complete synchronously before any Koin module is resolved"
                )
        }
        BlocklistRepository(get(), manualSourceId)
    }
}

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

val viewModelModule = module {
    viewModel { ContactsViewModel(get(), get(), get()) }
    viewModel { TelemetryViewModel(get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel { BlockedNumbersViewModel(get()) }
    viewModel { RecentCallsViewModel(get(), get()) }
    viewModel { LogcatViewModel() }
    viewModel { OnboardingViewModel() }
    viewModel { PendingCardViewModel(get(), get()) }
}

/**
 * workerModule — every CoroutineWorker shipping in v1 must have an entry here.
 * Resolved via KoinWorkerFactory. Contract §2: unregistered workers are not shippable.
 * PULSE-TODO (2026-06): activate once CommunitySyncWorker is moved from Future_Use/
 * into android/app/src/main/java/com/signalgate/multipoint/workers/.
 */
val workerModule = module {
    factory { (context: android.content.Context, params: androidx.work.WorkerParameters) ->
        CommunitySyncWorker(context, params, get())
    }
}

val appModule = listOf(
    databaseModule,
    repositoryModule,
    engineModule,
    viewModelModule,
    workerModule
)

suspend fun initializeDatabase(context: Context) {
    val koin = org.koin.core.context.GlobalContext.get()
    val sourceDao = koin.get<SourceDao>()
    val settingDao = koin.get<SettingDao>()
    DatabaseInitializer.seedRequiredSources(context, sourceDao, settingDao)
}
