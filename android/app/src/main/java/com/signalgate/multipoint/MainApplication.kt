package com.signalgate.multipoint

import android.app.Application
import androidx.work.Configuration
import com.signalgate.multipoint.di.KoinWorkerFactory
import com.signalgate.multipoint.di.appModule
import com.signalgate.multipoint.di.initializeDatabase
import com.signalgate.multipoint.security.SecurityUtils
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * MainApplication initializes Koin for dependency injection and provides
 * a custom WorkManager configuration to enable DI in background workers.
 *
 * CRITICAL ORDERING — startup race condition fix (2026-06):
 * Android can cold-start a fresh process specifically to deliver a
 * CallScreeningService callback (no Activity ever launches). In that path,
 * BlocklistRepository's singleton is the FIRST thing Koin resolves, and its
 * runBlocking lookup for the MANUAL source row will throw via error() if
 * DatabaseInitializer.seedRequiredSources() has not run yet.
 *
 * The previous implementation seeded asynchronously via
 * applicationScope.launch(Dispatchers.Main), which meant Koin modules were
 * resolvable before the seed completed — a real race, not theoretical.
 * ContactsViewModel.saveSelectedToAllowList() hit the same race but failed
 * silently (bails with no error) since it just reads a null-able SettingEntry.
 *
 * Fix: seed synchronously here, BEFORE startKoin() modules are available to
 * be resolved by any inbound call. runBlocking is appropriate at this single
 * call site — it blocks app cold-start for a few indexed DB reads, which is
 * the correct tradeoff against a process crash on every cold CallScreeningService
 * invocation. seedRequiredSources() is idempotent — safe on every launch.
 */
class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // Security baseline
        SecurityUtils.enableStrictMode()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        // Step 1.1: Idempotent source seeding (MANUAL + Contacts Allow List).
        // MUST complete before this function returns — Koin modules become
        // resolvable to inbound calls (including a cold CallScreeningService
        // invocation) the instant onCreate() finishes. Async seeding here
        // reintroduces the crash this fix addresses.
        runBlocking {
            try {
                initializeDatabase(this@MainApplication)
            } catch (e: Exception) {
                // Seeding failure is fatal to correct operation — BlocklistRepository
                // and ContactsViewModel both depend on these rows existing. Log loudly
                // but do not silently continue into a broken state.
                android.util.Log.e(
                    "MainApplication",
                    "FATAL: Database initialization failed — BlocklistRepository and " +
                    "ContactsViewModel will not function correctly",
                    e
                )
            }
        }
    }

    /**
     * Provides a WorkManager configuration that uses KoinWorkerFactory.
     * This allows workers to receive repositories via constructor injection.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}
