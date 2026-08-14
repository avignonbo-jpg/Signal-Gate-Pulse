package com.signalgate.pulse.security

import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * SecurityUtilsTest — unit tests for SecurityUtils.
 *
 * Scope: JVM unit tests only (no Android runtime). Keystore-dependent paths
 * (getDatabasePassphrase, envelope round-trip, recovery from corrupted/missing storage)
 * require a real Android Keystore and are covered instead by
 * SecurityUtilsInstrumentedTest.kt in androidTest/.
 *
 * enableStrictMode() is intentionally not covered by a dedicated test anywhere: it calls
 * real android.os.StrictMode APIs (so it can't run here without Robolectric), and an
 * instrumented test that just calls it again would only confirm "it didn't throw" — a
 * property already exercised by MainApplication.onCreate() every time the instrumented
 * test process starts.
 */
class SecurityUtilsTest {

    @Test
    fun keystoreConstants_areNonEmpty() {
        val clazz = SecurityUtils::class.java
        val keyAlias = clazz.getDeclaredField("KEY_ALIAS")
            .also { it.isAccessible = true }.get(SecurityUtils) as? String
        val provider = clazz.getDeclaredField("ANDROID_KEYSTORE")
            .also { it.isAccessible = true }.get(SecurityUtils) as? String

        assertNotNull("KEY_ALIAS must not be null", keyAlias)
        assertTrue("KEY_ALIAS must not be blank", keyAlias!!.isNotBlank())
        assertNotNull("ANDROID_KEYSTORE must not be null", provider)
        assertTrue("ANDROID_KEYSTORE must not be blank", provider!!.isNotBlank())
    }

    @Test
    fun passphraseLengthBytes_isAtLeast32() {
        val length = SecurityUtils::class.java
            .getDeclaredField("PASSPHRASE_LENGTH_BYTES")
            .also { it.isAccessible = true }
            .getInt(SecurityUtils)
        assertTrue("PASSPHRASE_LENGTH_BYTES must be >= 32 (256 bits). Found: $length", length >= 32)
    }
}
