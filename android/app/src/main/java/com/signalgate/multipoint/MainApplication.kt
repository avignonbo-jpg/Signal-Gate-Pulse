package com.signalgate.multipoint

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.signalgate.multipoint.di.KoinWorkerFactory
import com.signalgate.multipoint.di.appModule
import com.signalgate.multipoint.di.initializeDatabase
import com.signalgate.multipoint.security.SecurityUtils
import com.signalgate.multipoint.workers.CommunitySyncWorker
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

/**
 * MainApplication — app entry point.
 *
 * Startup ordering constraint (Architecture Contract §2 — BINDING):
 * Android can cold-start a fresh process to deliver a CallScreeningService
 * callback before any Activity ever launches. In that path, BlocklistRepository
 * is the first Koin binding resolved, and its runBlocking lookup for the MANUAL
 * source row throws via error() if DatabaseInitializer has not yet run.
 *
 * DatabaseInitializer.seedRequiredSources() therefore runs synchronously inside
 * runBlocking BEFORE onCreate() returns. Async seeding via applicationScope.launch
 * is explicitly forbidden as a regression of the 2026-06 race-condition fix
 * (see Architecture Contract §2). The blocking time is a few indexed DB reads —
 * the correct tradeoff against a process crash on cold CallScreeningService start.
 */
class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        SecurityUtils.enableStrictMode()

        // Plant Timber logging tree before any module is resolved.
        // DebugTree in debug builds only — no logging in release per Step 1.13.
        if (android.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        // Synchronous seed — must complete before onCreate() returns.
        // Do NOT convert this to applicationScope.launch or any async pattern.
        runBlocking {
            try {
                initializeDatabase(this@MainApplication)
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainApplication",
                    "FATAL: Database initialization failed — BlocklistRepository and " +
                    "ContactsViewModel will not function correctly. Stack trace follows.",
                    e
                )
            }
        }

        scheduleCommunitySync()
    }

    /**
     * Schedules CommunitySyncWorker as a daily periodic job.
     * KEEP_EXISTING policy means a running or enqueued sync is never cancelled
     * and re-enqueued on every launch — one sync per day, no duplicates.
     *
     * WorkManager uses KoinWorkerFactory (see workManagerConfiguration below)
     * so CommunitySyncWorker receives ReliableSourceManager via constructor
     * injection rather than having to locate it itself.
     */
    private fun scheduleCommunitySync() {
        val syncRequest = PeriodicWorkRequestBuilder<CommunitySyncWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CommunitySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP_EXISTING,
            syncRequest
        )
    }

    /**
     * Provides WorkManager with KoinWorkerFactory so all CoroutineWorkers
     * declared in workerModule receive their dependencies via DI.
     * Contract §2: every Worker must be in workerModule — this is what enforces it.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}
