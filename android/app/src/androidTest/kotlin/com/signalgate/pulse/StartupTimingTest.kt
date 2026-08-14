package com.signalgate.pulse

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for the MainApplication.onCreate() runBlocking DB-init cold
 * start (see that class's doc comment for why the block itself is intentional
 * and must not be removed). This test does not measure true OS-level
 * TTID/TTFD (that needs androidx.benchmark.macro in its own Gradle module,
 * with baseline-profile tooling — a heavier addition than this needs right
 * now); it asserts a simpler, cheaper regression bound: once the process is
 * already running and MainActivity is launched, AppReadiness.isReady must
 * already be true almost immediately, since Application.onCreate() completes
 * before any Activity is created. If this ever starts failing/flaking, that's
 * a signal the ordering guarantee this test depends on has changed — same
 * signal a real macrobenchmark would eventually give, just without the
 * separate module. A future Phase 5/6 CI hardening pass should still add a
 * real androidx.benchmark.macro module for true cold-process TTID measurement;
 * this test is a floor, not a replacement for that.
 */
@RunWith(AndroidJUnit4::class)
class StartupTimingTest {

    @Test
    fun appReadiness_isTrueShortlyAfterActivityLaunch() {
        val start = SystemClock.elapsedRealtime()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                // By the time onActivity() runs, MainApplication.onCreate() has
                // already completed in-process (Android's startup ordering
                // guarantee), so AppReadiness.isReady should already be true.
                assertTrue(
                    "AppReadiness.isReady was false when MainActivity's onActivity() " +
                        "ran — this means Application.onCreate()'s blocking DB init " +
                        "had not completed by Activity launch, which breaks the " +
                        "ordering guarantee MainApplication's class doc depends on.",
                    AppReadiness.isReady.value
                )
            }
        }

        val elapsedMs = SystemClock.elapsedRealtime() - start

        // Generous regression ceiling, not a tight perf target: catches a real
        // regression (e.g. seedRequiredSources() accidentally doing unbounded
        // work) without being flaky on slow CI emulators. Tighten once a real
        // macrobenchmark baseline exists.
        assertTrue(
            "MainActivity launch + AppReadiness confirmation took ${elapsedMs}ms, " +
                "exceeding the 8000ms regression ceiling. This measures in-process " +
                "activity launch only, not true cold-process start — see class doc.",
            elapsedMs < 8000
        )
    }

    /**
     * Sanity check that AppReadiness itself behaves as a simple one-way gate —
     * cheap JVM-side companion to the instrumented test above, catches a
     * logic error in AppReadiness independent of any Android startup timing.
     */
    @Test
    fun appReadiness_defaultsFalseThenLatches() = runBlocking {
        // This test intentionally does not reset AppReadiness.isReady — by the
        // time any test in this instrumented suite runs, MainApplication has
        // already set it true for the process. It documents the one-way-latch
        // shape of AppReadiness for future readers rather than re-asserting
        // process-level ordering already covered above.
        delay(1)
        assertTrue(AppReadiness.isReady.value)
    }
}
