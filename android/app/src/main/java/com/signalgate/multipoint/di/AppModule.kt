package com.signalgate.multipoint.di

import android.content.Context
import com.signalgate.multipoint.database.DatabaseInitializer
import com.signalgate.multipoint.database.SecureDatabase
import com.signalgate.multipoint.database.SignalGateDatabase
import com.signalgate.multipoint.database.daos.SourceDao
import com.signalgate.multipoint.database.repositories.BlocklistRepository
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import com.signalgate.multipoint.database.repositories.SyncHistoryRepository
import com.signalgate.multipoint.logic.CallScreeningEngine
import com.signalgate.multipoint.logic.DataSyncEngine
import com.signalgate.multipoint.ui.BlockedNumbersViewModel
import com.signalgate.multipoint.ui.RecentCallsViewModel
import com.signalgate.multipoint.ui.dashboard.DashboardViewModel
import com.signalgate.multipoint.ui.digest.PendingCardViewModel
import com.signalgate.multipoint.ui.onboarding.OnboardingViewModel
import com.signalgate.multipoint.ui.overlay.CallOverlayViewModel
import com.signalgate.multipoint.ui.viewmodels.ContactsViewModel
import com.signalgate.multipoint.ui.viewmodels.LogcatViewModel
import com.signalgate.multipoint.ui.viewmodels.TelemetryViewModel
import com.signalgate.multipoint.data.security.BloomFilterEngine
import com.signalgate.multipoint.data.security.PrecedenceEngine
import com.signalgate.multipoint.data.security.SecureCsvParser
import kotlinx.coroutines.runBlocking
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
    single { get<SignalGateDatabase>().pendingCardDao() }
}

val repositoryModule = module {
    single { DataSourceRepository(get(), get()) }
    single { CallLogRepository(get()) }
    single { SyncHistoryRepository(get()) }

    // BlocklistRepository requires the MANUAL sourceId seeded by DatabaseInitializer.
    // runBlocking is intentional here: Koin's single{} lambda is not a coroutine scope,
    // and this lookup is a single indexed DB read that only runs once at startup after
    // seedRequiredSources() has already completed. Replace with SettingEntry cache
    // lookup in Step 2.6 when the SettingEntry key store is wired up.
    single {
        val sourceDao = get<SourceDao>()
        val manualSourceId = runBlocking {
            sourceDao.getSourceByName("Manual User Rules")?.id
                ?: error("Manual source row not found — ensure DatabaseInitializer.seedRequiredSources() ran before Koin resolves repositoryModule")
        }
        BlocklistRepository(get(), manualSourceId)
    }
}

val logicModule = module {
    single { BloomFilterEngine() }
    single { SecureCsvParser(get()) }
    single {
        PrecedenceEngine(
            bloomFilter = get(),
            localAllowListCache = hashSetOf(),
            localManualBlockListCache = hashSetOf()
        )
    }
    single { CallScreeningEngine(get()) }
    single { DataSyncEngine(get(), get()) }

    // MultiPortSyncWorker intentionally omitted — not present in consumer-v1 branch
    /*
    factory { (context: android.content.Context, params: androidx.work.WorkerParameters) ->
        com.signalgate.multipoint.service.workers.MultiPortSyncWorker(
            context, params, get(), get(), get()
        )
    }
    */
}

val viewModelModule = module {
    viewModel { ContactsViewModel(get(), get(), get()) }
    viewModel { TelemetryViewModel(get()) }
    viewModel { CallOverlayViewModel() }
    viewModel { DashboardViewModel(get()) }
    viewModel { BlockedNumbersViewModel(get()) }
    viewModel { RecentCallsViewModel(get(), get()) }
    viewModel { LogcatViewModel() }
    viewModel { OnboardingViewModel() }
    viewModel { PendingCardViewModel(get(), get()) }
}

val appModule = listOf(databaseModule, repositoryModule, logicModule, viewModelModule)

// Call this from MainApplication.onCreate() after Koin starts and before any
// repository module is resolved. seedRequiredSources() is idempotent — safe to
// call on every launch.
suspend fun initializeDatabase(context: Context) {
    val koin = org.koin.core.context.GlobalContext.get()
    val sourceDao = koin.get<SourceDao>()
    val settingDao = koin.get<com.signalgate.multipoint.database.daos.SettingDao>()
    DatabaseInitializer.seedRequiredSources(context, sourceDao, settingDao)
}
