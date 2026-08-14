package com.signalgate.pulse

import android.app.Application
import androidx.work.Configuration
import com.signalgate.pulse.BuildConfig
import com.signalgate.pulse.di.KoinWorkerFactory
import com.signalgate.pulse.di.appModule
import com.signalgate.pulse.di.initializeDatabase
import com.signalgate.pulse.di.rehydrateBloomFiltersInBackground
import com.signalgate.pulse.security.SecurityUtils
import com.signalgate.pulse.ui.notifications.NotificationChannelManager
import com.signalgate.pulse.workers.CommunitySyncWorker
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

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
 *
 * [applicationScope] below is NOT a reintroduction of that forbidden pattern —
 * it's used for exactly one thing, bloom filter rehydration (see AppModule's
 * rehydrateBloomFiltersInBackground()), which has no such binding dependency:
 * getCallDecision() is fully correct while it's still running, just not yet
 * fast. Do not add anything else to applicationScope without checking whether
 * it has the same binding-free property seedRequiredSources() does not.
 */
class MainApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        SecurityUtils.enableStrictMode()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // DebugTree is intentionally debug-only (avoids verbose/info spam in release
            // logcat), but that meant WARN/ERROR-level events — like a Keystore
            // invalidation forcing a database reset — went nowhere in release builds.
            // This minimal tree keeps release logs quiet except for events that actually
            // matter, so they're still visible in logcat / bug reports.
            Timber.plant(ReleaseTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        runBlocking {
            try {
                initializeDatabase(this@MainApplication)
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "FATAL: Database initialization failed — BlocklistRepository and " +
                    "ContactsViewModel will not function correctly. Stack trace follows."
                )
            }
        }

        // Deliberately outside the runBlocking block above — see class doc and
        // AppModule.rehydrateBloomFiltersInBackground() for why this one is safe
        // to run unawaited while seedRequiredSources() above is not.
        rehydrateBloomFiltersInBackground(applicationScope)

        // Phase 4.8: register all notification channels before any notification fires.
        // createAllChannels() is a no-op on API < 26 and safe to call repeatedly.
        NotificationChannelManager.createAllChannels(this)

        CommunitySyncWorker.schedule(this)

        // Signals MainActivity's splash screen (installSplashScreen()'s
        // keepOnScreenCondition) that startup work is done. By the time this line
        // runs, MainApplication.onCreate() — including the runBlocking DB init
        // above — has already fully completed, and Android guarantees Application
        // .onCreate() finishes before any Activity is created. So in practice this
        // flips to true before MainActivity even exists; it exists as a safety net
        // in case a future change adds real post-Activity-launch async work, not
        // because anything today needs the extra wait.
        AppReadiness.isReady.value = true
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}

/**
 * Signals whether MainApplication's startup work has completed. Read by
 * MainActivity's installSplashScreen().setKeepOnScreenCondition() so the splash
 * window stays up for the app's full cold-start duration rather than being
 * dismissed the instant the first Compose frame is ready to draw.
 */
object AppReadiness {
    val isReady: MutableStateFlow<Boolean> = MutableStateFlow(false)
}

/**
 * Timber tree for release builds: forwards only WARN and above to android.util.Log,
 * so it stays visible via logcat/bug reports without the verbose/info spam a full
 * DebugTree would add in production.
 */
private class ReleaseTree : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= android.util.Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        android.util.Log.println(priority, tag ?: "SignalGate", message)
        if (t != null) {
            android.util.Log.println(priority, tag ?: "SignalGate", android.util.Log.getStackTraceString(t))
        }
    }
}
