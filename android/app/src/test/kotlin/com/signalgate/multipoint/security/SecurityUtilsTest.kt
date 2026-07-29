package com.signalgate.multipoint.security

import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * SecurityUtilsTest — unit tests for SecurityUtils.
 *
 * Scope: JVM unit tests only (no Android runtime). Keystore-dependent paths
 * (getDatabasePassphrase, clearDatabasePassphrase, envelope round-trip) require
 * an instrumented androidTest runner and a real Android Keystore.
 *
 * PULSE-TODO: create SecurityUtilsInstrumentedTest.kt in androidTest/ with:
 *   - testGetDatabasePassphrase_returnsDeterministicBytes()
 *   - testGetDatabasePassphrase_afterKeyInvalidation_returnsNewBytes()
 *   - testClearDatabasePassphrase_removesPrefs()
 *   - testEnvelopeRoundTrip_decryptedMatchesOriginal()
 *   - testEnableStrictMode_doesNotThrow() — enableStrictMode() calls real
 *     android.os.StrictMode APIs, which throw "not mocked" on a plain JVM
 *     unit test (no Robolectric runner here). Same category as the
 *     Keystore-dependent methods above — needs a real Android runtime.
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
