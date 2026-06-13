package com.signalgate.multipoint

import android.app.Application
import androidx.work.Configuration
import com.signalgate.multipoint.di.KoinWorkerFactory
import com.signalgate.multipoint.di.appModule
import com.signalgate.multipoint.security.SecurityUtils
import com.signalgate.multipoint.database.SecureDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * MainApplication initializes Koin for dependency injection and provides
 * a custom WorkManager configuration to enable DI in background workers.
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
    }

    /**
     * Provides a WorkManager configuration that uses KoinWorkerFactory.
     * This allows MultiPortSyncWorker to receive repositories via constructor injection.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}
