package com.signalgate

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.signalgate.database.SignalGateDatabase
import com.signalgate.di.appModule
import com.signalgate.workers.CommunitySyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Koin Dependency Injection
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        // Schedule the Community Sync Worker for Pulse (set-and-forget)
        scheduleCommunitySync()

        Timber.i("✅ SignalGate Pulse Application initialized - Room + WorkManager ready")
    }

    private fun scheduleCommunitySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)   // WiFi only
            .setRequiresCharging(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CommunitySyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "pulse_community_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        Timber.d("CommunitySyncWorker scheduled (every 24h on WiFi + charging)")
    }
}
