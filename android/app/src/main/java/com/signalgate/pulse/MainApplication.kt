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
import kotlinx.coroutines.withTimeoutOrNull
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
 *
 * 2026-08-15 — DIAGNOSTIC TIMING added throughout onCreate(), tagged
 * "SignalGate" (matches LogcatViewerScreen's capture filter). Added to
 * investigate a reported ~8s blank-black-screen on first-ever app launch
 * only (subsequent launches show the splash correctly) — see
 * PROJECT_LEDGER.md 2026-08-15 entry. These are Timber.i() calls, dropped
 * entirely in release builds by ReleaseTree's WARN-and-above filter — zero
 * behavior or performance impact either way. Safe to remove once the
 * first-run slowdown is diagnosed and fixed, but harmless to leave in.
 *
 * 2026-10-28 — DATABASE_INIT_BUDGET_MS deadline added around the
 * runBlocking DB-init block below, in response to §4.0.7's open
 * CallScreeningService deadline-architecture gate and the 2026-09-10
 * real-device evidence showing ~1.2s of StrictMode-flagged main-thread
 * work in this exact block. Two things were true before this change that
 * are not true after it:
 *   (1) This block had NO upper bound. A device that's slow here (cold
 *       page cache, a busy encrypted filesystem, a struggling low-end
 *       phone) could in principle let onCreate() — and therefore Service
 *       creation, and therefore the Service's own 3.5s decision budget —
 *       run past Android's 5s CallScreeningService deadline with no
 *       instrumented signal that this was the cause. (1.2s observed on
 *       one device is not a worst-case bound.)
 *   (2) On any device where setup legitimately does run long, there was
 *       no way for the rest of onCreate() — Service creation included —
 *       to proceed until it finished, however long that took.
 * DATABASE_INIT_BUDGET_MS is a conservative placeholder ceiling, not a
 * validated value — Task 2.1 (real-device Telecom cold-start measurement,
 * still open, owner-only) is what should tune or replace it. Choosing a
 * number now, rather than leaving the block unbounded while waiting on
 * that data, converts an unknown/unprotected risk into a known/bounded
 * one today; this does not substitute for that measurement.
 *
 * This change does NOT touch the ordering guarantee in the class doc
 * above (seedRequiredSources() still runs, still synchronously, still
 * before anything else in onCreate() proceeds) — it only stops that work
 * from being able to run unbounded. On timeout, onCreate() logs and moves
 * on with seeding possibly incomplete, the same already-handled state as
 * the existing catch(Exception) branch below produces on a thrown error.
 * Downstream repositories already tolerate that case (see AppModule.kt's
 * notes on BlocklistRepository's default -1 sourceId / lazy load). This
 * does NOT resolve §4.0.7 on its own — Problem A/B in
 * SECURITY-DEVOPS-BUILD-PLAN.md are in SignalGateCallScreeningService.kt,
 * a different file, and are untouched here.
 */
class MainApplication : Application(), Configuration.Provider {

    companion object {
        // Total Telecom deadline is 5000ms (android.telecom.CallScreeningService).
        // The decision engine already budgets withTimeout(3_500) in
        // SignalGateCallScreeningService — on its own that leaves only
        // 1500ms for setup + Service creation/binding overhead combined,
        // and 2000 + 3500 = 5500ms, over the 5000ms deadline on paper.
        // That arithmetic gap is being left visible here, not papered
        // over: the 3.5s decision budget is a separate, deliberate
        // product-quality tradeoff (shrinking it means less time to
        // decide, which pushes more real calls into the SECURITY_FAILURE/
        // uncertain path) and is NOT changed by this commit. Narrowing it
        // needs its own explicit, documented decision once Task 2.1's
        // real-device numbers exist, the same way this value does.
        // 2000ms is a conservative placeholder ceiling for setup alone —
        // not a validated value, not proof the two budgets fit together.
        // It exists so setup can no longer run unbounded while that real
        // measurement and that separate decision are still pending.
        private const val DATABASE_INIT_BUDGET_MS = 2_000L
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Warms sqlcipher's native lib load in parallel with Koin startup below,
        // rather than paying its cost serially inside the runBlocking DB-init
        // block further down. SecureDatabase.<clinit> calls
        // System.loadLibrary("sqlcipher") itself when the DB is first resolved;
        // System.loadLibrary is idempotent per classloader, so if this finishes
        // first, that later call is a no-op, and if it hasn't finished, the
        // runBlocking below still correctly blocks on it via the JVM's class-init
        // lock. This does not touch the seedRequiredSources() ordering guarantee
        // documented above — it only gives the expensive native-lib disk scan
        // (APK zip central-directory lookup, StrictMode-flagged at ~1.2s on cold
        // page cache) a head start elsewhere instead of removing it from the
        // critical path.
        Thread { System.loadLibrary("sqlcipher") }.start()

        StartupDiagnostics.mark(StartupDiagnostics.Event.APPLICATION_ON_CREATE_BEGIN)
        val startupBeginMs = System.currentTimeMillis()
        fun elapsed() = System.currentTimeMillis() - startupBeginMs

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
        
        TelecomRoleDiagnostics.log(this)

        Timber.tag("SignalGate").i("STARTUP: onCreate begin, elapsed=0ms")

        StartupDiagnostics.mark(StartupDiagnostics.Event.KOIN_START_BEGIN)
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        StartupDiagnostics.mark(StartupDiagnostics.Event.KOIN_STARTED)
        Timber.tag("SignalGate").i("STARTUP: Koin started, elapsed=${elapsed()}ms")

        StartupDiagnostics.mark(StartupDiagnostics.Event.DATABASE_INIT_BEGIN)
        runBlocking {
            val seeded = withTimeoutOrNull(DATABASE_INIT_BUDGET_MS) {
                try {
                    Timber.tag("SignalGate").i("STARTUP: DB init begin, elapsed=${elapsed()}ms")
                    initializeDatabase(this@MainApplication)
                    Timber.tag("SignalGate").i("STARTUP: DB init end, elapsed=${elapsed()}ms")
                    true
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "FATAL: Database initialization failed — BlocklistRepository and " +
                        "ContactsViewModel will not function correctly. Stack trace follows."
                    )
                    false
                }
            }
            if (seeded == null) {
                // Setup exceeded DATABASE_INIT_BUDGET_MS. This does NOT crash the
                // process and does NOT let onCreate() hang further — onCreate()
                // still returns on schedule, so Service.onCreate() and
                // SignalGateCallScreeningService's existing SECURITY_FAILURE
                // response path (which answers before touching the DB) stay
                // reachable within Android's 5s deadline. seedRequiredSources()
                // may not have finished — downstream repositories must tolerate
                // that the same way they already tolerate the caught-exception
                // branch above (see AppModule.kt's notes on BlocklistRepository's
                // default -1 sourceId / lazy load).
                Timber.e(
                    "SECURITY: Database initialization exceeded " +
                    "${DATABASE_INIT_BUDGET_MS}ms budget, elapsed=${elapsed()}ms — " +
                    "proceeding with onCreate() regardless; seeding may be incomplete"
                )
            }
            StartupDiagnostics.mark(StartupDiagnostics.Event.DATABASE_INIT_END)
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
        StartupDiagnostics.mark(StartupDiagnostics.Event.APP_READINESS_TRUE)
        StartupDiagnostics.mark(StartupDiagnostics.Event.APPLICATION_ON_CREATE_END)

        Timber.tag("SignalGate").i("STARTUP: AppReadiness=true, total elapsed=${elapsed()}ms")
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
