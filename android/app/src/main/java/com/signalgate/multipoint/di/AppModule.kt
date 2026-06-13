package com.signalgate.multipoint.di

import com.signalgate.multipoint.database.SecureDatabase
import com.signalgate.multipoint.database.SignalGateDatabase
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import com.signalgate.multipoint.database.repositories.SyncHistoryRepository
import com.signalgate.multipoint.logic.CallScreeningEngine
import com.signalgate.multipoint.logic.DataSyncEngine
import com.signalgate.multipoint.ui.BlockedNumbersViewModel
import com.signalgate.multipoint.ui.RecentCallsViewModel
import com.signalgate.multipoint.ui.dashboard.DashboardViewModel
import com.signalgate.multipoint.ui.overlay.CallOverlayViewModel
import com.signalgate.multipoint.ui.viewmodels.TelemetryViewModel
import com.signalgate.multipoint.ui.viewmodels.LogcatViewModel
import com.signalgate.multipoint.data.security.BloomFilterEngine
import com.signalgate.multipoint.data.security.PrecedenceEngine
import com.signalgate.multipoint.data.security.SecureCsvParser
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    // Database is built via SecureDatabase to ensure SQLCipher encryption (Layer 2)
    single<SignalGateDatabase> {
        SecureDatabase.getDatabase(androidContext())
    }
    single { get<SignalGateDatabase>().sourceDao() }
    single { get<SignalGateDatabase>().unifiedEntryDao() }
    single { get<SignalGateDatabase>().callLogDao() }
    single { get<SignalGateDatabase>().settingDao() }
    single { get<SignalGateDatabase>().syncHistoryDao() }
}

val repositoryModule = module {
    single { DataSourceRepository(get(), get()) }
    single { CallLogRepository(get()) }
    single { SyncHistoryRepository(get()) }
}

val logicModule = module {
    // Security layer
    single { BloomFilterEngine() }
    single { SecureCsvParser(get()) }
    single {
        PrecedenceEngine(
            bloomFilter = get(),
            localAllowListCache = hashSetOf(),
            localManualBlockListCache = hashSetOf()
        )
    }
    // CallScreeningEngine now uses DataSourceRepository (no direct DB access)
    single { CallScreeningEngine(get()) }
    // Sync engine now uses DataSourceRepository + SyncHistoryRepository
    single { DataSyncEngine(get(), get()) }

    // WorkManager workers (factory required for KoinWorkerFactory)
    factory { (context: android.content.Context, params: androidx.work.WorkerParameters) ->
        com.signalgate.multipoint.service.workers.MultiPortSyncWorker(
            context, params, get(), get(), get()
        )
    }
}

val viewModelModule = module {
    viewModel { TelemetryViewModel(get()) }
    viewModel { CallOverlayViewModel() }
    viewModel { DashboardViewModel(get()) }
    viewModel { BlockedNumbersViewModel(get()) }
    viewModel { RecentCallsViewModel(get(), get()) }
    viewModel { LogcatViewModel() }
}

val appModule = listOf(databaseModule, repositoryModule, logicModule, viewModelModule)
