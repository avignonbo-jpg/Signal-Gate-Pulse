package com.signalgate.multipoint.di

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signalgate.multipoint.database.DatabaseInitializer
import com.signalgate.multipoint.database.SignalGateDatabase
import com.signalgate.multipoint.database.repositories.BlocklistRepository
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import com.signalgate.multipoint.database.repositories.PendingCardRepository
import com.signalgate.multipoint.database.repositories.SettingRepository
import com.signalgate.multipoint.database.repositories.SyncHistoryRepository
import com.signalgate.multipoint.data.security.BloomFilterEngine
import com.signalgate.multipoint.logic.CallRiskEvaluator
import com.signalgate.multipoint.logic.CallScreeningEngine
import com.signalgate.multipoint.logic.DataSyncEngine
import com.signalgate.multipoint.logic.ReliableSourceManager
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner

/**
 * KoinModuleTest — verifies the full Koin DI graph resolves without error.
 *
 * Fixed bugs from the original:
 *   Bug 1: SettingRepository missing from testDatabaseModule — repositoryModule's
 *     BlocklistRepository takes a SettingRepository; without it Koin threw
 *     NoBeanDefFoundException on first screen that injected BlocklistRepository.
 *   Bug 2: DatabaseInitializer.seedRequiredSources() not called before startKoin —
 *     BlocklistRepository.manualSourceId() lazy-resolves the MANUAL source row;
 *     without seeding it calls error() and crashes. Mirrors MainApplication ordering.
 *   Bug 3: Ran as a plain JVM unit test with a Mockito-mocked Context — Room's
 *     real Room.inMemoryDatabaseBuilder() open sequence needs a working SQLite
 *     implementation, which a JVM-only test with a mocked Context can't provide.
 *     That mismatch produced java.lang.IllegalMonitorStateException out of
 *     androidx.sqlite's ProcessLock during the DB's first open. Fixed by running
 *     under Robolectric, which supplies a real (shadowed) Context/Application
 *     and a working SQLite implementation — no synchronous pre-open workaround
 *     or mocked Context/Application needed.
 *   Bug 4: checkModules() tries to instantiate every definition, including
 *     workerModule's parameterized CommunitySyncWorker factory (Context +
 *     WorkerParameters). No amount of mocking WorkerParameters gets past
 *     CoroutineWorker's constructor, which calls real methods on it — so
 *     workerModule is excluded from the checkModules() call. See the comment
 *     on koinGraphResolvesWithoutError() for the full reasoning.
 *
 * Mockito is still declared in build.gradle for other tests, but this class no
 * longer needs it now that Robolectric provides a real Context/Application.
 */
@RunWith(RobolectricTestRunner::class)
class KoinModuleTest : KoinTest {

    private lateinit var testApp: Application
    private lateinit var testDatabase: SignalGateDatabase
    private lateinit var testDatabaseModule: org.koin.core.module.Module

    @Before
    fun setUp() {
        testApp = ApplicationProvider.getApplicationContext()

        val singleThreadExecutor = Executors.newSingleThreadExecutor()
        testDatabase = Room.inMemoryDatabaseBuilder(testApp, SignalGateDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(singleThreadExecutor)
            .setTransactionExecutor(singleThreadExecutor)
            .build()

        runBlocking {
            DatabaseInitializer.seedRequiredSources(
                context    = testApp,
                sourceDao  = testDatabase.sourceDao(),
                settingDao = testDatabase.settingDao()
            )
        }

        testDatabaseModule = module {
            single<SignalGateDatabase> { testDatabase }
            single { testDatabase.sourceDao() }
            single { testDatabase.unifiedEntryDao() }
            single { testDatabase.callLogDao() }
            single { testDatabase.settingDao() }
            single { testDatabase.syncHistoryDao() }
            single { testDatabase.pendingCardDao() }
            single { SettingRepository(get()) }
            single { PendingCardRepository(get()) }
        }

        startKoin {
            androidContext(testApp)
            modules(testDatabaseModule, repositoryModule, engineModule, viewModelModule, workerModule)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        testDatabase.close()
    }

    @Test
    fun koinGraphResolvesWithoutError() {
        // workerModule is deliberately excluded here. CommunitySyncWorker's
        // superclass (CoroutineWorker) calls real methods on WorkerParameters
        // during construction (getTaskExecutor().getSerialTaskExecutor()) — a
        // Mockito mock returns null for those and CoroutineWorker's own init
        // logic NPEs. CommunitySyncWorker is never resolved via getKoin().get()
        // in real app code anyway; WorkManager instantiates it through
        // KoinWorkerFactory with a real WorkerParameters at runtime. Its own
        // injected dependencies (ReliableSourceManager, DataSyncEngine) are
        // better verified with androidx.work:work-testing's TestWorkerBuilder
        // in an instrumented test, which supplies a real WorkerParameters.
        stopKoin()
        checkModules {
            androidContext(testApp)
            modules(testDatabaseModule, repositoryModule, engineModule, viewModelModule)
        }
    }

    @Test
    fun repositoriesResolve() {
        getKoin().get<DataSourceRepository>()
        getKoin().get<CallLogRepository>()
        getKoin().get<SyncHistoryRepository>()
        getKoin().get<BlocklistRepository>()
        getKoin().get<SettingRepository>()
        getKoin().get<PendingCardRepository>()
    }

    @Test
    fun enginesResolve() {
        getKoin().get<CallRiskEvaluator>()
        getKoin().get<CallScreeningEngine>()
        getKoin().get<DataSyncEngine>()
        getKoin().get<ReliableSourceManager>()
        getKoin().get<BloomFilterEngine>()
    }
}
