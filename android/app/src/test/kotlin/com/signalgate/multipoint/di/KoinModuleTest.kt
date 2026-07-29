package com.signalgate.multipoint.di

import android.app.Application
import android.content.Context
import androidx.room.Room
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.mockito.Mockito.mock

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
 *
 * Uses Mockito (already declared in build.gradle). MockK is not declared.
 */
class KoinModuleTest : KoinTest {

    private lateinit var testDatabase: SignalGateDatabase
    private lateinit var testDatabaseModule: org.koin.core.module.Module

    @Before
    fun setUp() {
        val singleThreadExecutor = Executors.newSingleThreadExecutor()
        testDatabase = Room.inMemoryDatabaseBuilder(
            mock(Context::class.java),
            SignalGateDatabase::class.java
        )
            .allowMainThreadQueries()
            .setQueryExecutor(singleThreadExecutor)
            .setTransactionExecutor(singleThreadExecutor)
            .build()

        // Force the DB to fully open synchronously, on this thread, with no
        // coroutine involved. ProcessLock's lock/unlock only guards the very
        // first open (see FrameworkSQLiteOpenHelper — once open, get*Database()
        // does no I/O and takes no lock). Doing that first open here, plainly,
        // means seedRequiredSources()'s suspend DAO calls below hit an
        // already-open DB and never touch the lock at all.
        testDatabase.openHelper.writableDatabase

        runBlocking {
            DatabaseInitializer.seedRequiredSources(
                context   = mock(Context::class.java),
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
            androidContext(mock(Application::class.java))
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
        checkModules {
            androidContext(mock(Application::class.java))
            modules(testDatabaseModule, repositoryModule, engineModule, viewModelModule, workerModule)
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
