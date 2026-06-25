package com.signalgate.multipoint

import android.app.Application
import androidx.work.Configuration
import com.signalgate.multipoint.di.KoinWorkerFactory
import com.signalgate.multipoint.di.appModule
import com.signalgate.multipoint.di.initializeDatabase
import com.signalgate.multipoint.security.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * MainApplication initializes Koin for dependency injection and provides
 * a custom WorkManager configuration to enable DI in background workers.
 */
class MainApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Security baseline
        SecurityUtils.enableStrictMode()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        // Step 1.1: Idempotent source seeding (MANUAL + Contacts Allow List)
        // Runs after Koin is ready so repositories/DAOs are available
        applicationScope.launch {
            try {
                initializeDatabase(this@MainApplication)
            } catch (e: Exception) {
                // Log error but don't crash app startup
                android.util.Log.e("MainApplication", "Database initialization failed", e)
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
