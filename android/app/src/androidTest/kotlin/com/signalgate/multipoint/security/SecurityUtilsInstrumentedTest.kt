package com.signalgate.multipoint.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SecurityUtilsInstrumentedTest — instrumented tests for SecurityUtils.getDatabasePassphrase().
 *
 * This runs on a device/emulator (not the JVM) because getOrCreateKeystoreKey() requires a
 * real Android Keystore. See SecurityUtilsTest.kt for the JVM-only unit tests.
 *
 * Scope: this exercises the PUBLIC CONTRACT of getDatabasePassphrase() — deterministic
 * 32-byte output, stability across calls, silent regeneration when the persisted envelope
 * is simply MISSING (first install — expected, safe), and throwing KeystoreInvalidatedException
 * when the persisted envelope is PRESENT but fails to decrypt (corruption or Keystore key
 * invalidation — a real event callers must handle explicitly, not something to paper over).
 * It also covers resetDatabasePassphrase(), the explicit recovery call callers make after
 * catching that exception. It deliberately does not reflect into encrypt(), decrypt(), or
 * getOrCreateKeystoreKey(), and does not assert on Keystore alias existence or Cipher/
 * transformation details — those are implementation, not contract, and pinning tests to
 * them would break on any internal refactor that preserves the same external behavior.
 *
 * The one piece of reflection here is reading the private PREFS_NAME/PREF_CIPHERTEXT/PREF_IV
 * constants — not to invoke logic, but so the tests can locate the same SharedPreferences
 * entries SecurityUtils itself reads/writes, to simulate corruption/loss without duplicating
 * those literal strings (and risking silent drift if they ever change).
 *
 * Run with: ./gradlew connectedPulseDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SecurityUtilsInstrumentedTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var ciphertextKey: String
    private lateinit var ivKey: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val clazz = SecurityUtils::class.java
        val prefsName = clazz.getDeclaredField("PREFS_NAME")
            .also { it.isAccessible = true }.get(SecurityUtils) as String
        ciphertextKey = clazz.getDeclaredField("PREF_CIPHERTEXT")
            .also { it.isAccessible = true }.get(SecurityUtils) as String
        ivKey = clazz.getDeclaredField("PREF_IV")
            .also { it.isAccessible = true }.get(SecurityUtils) as String

        prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        // Every test starts from a clean slate — no leftover envelope from a prior test.
        // The underlying Keystore key/alias is intentionally left alone: reusing the same
        // hardware-backed key across tests (and across app runs) is exactly the real-world
        // behavior this class relies on.
        prefs.edit().clear().apply()
    }

    @Test
    fun firstCall_generatesPassphrase() {
        val passphrase = SecurityUtils.getDatabasePassphrase(context)

        assertNotNull(passphrase)
        assertTrue("passphrase must not be empty", passphrase.isNotEmpty())
        assertEquals("passphrase must be 32 bytes (256 bits)", 32, passphrase.size)
    }

    @Test
    fun secondCall_returnsSamePassphrase() {
        val first = SecurityUtils.getDatabasePassphrase(context)
        val second = SecurityUtils.getDatabasePassphrase(context)

        assertArrayEquals(
            "a second call must decrypt back to the same passphrase, not mint a new one",
            first,
            second
        )
    }

    @Test
    fun corruptedCiphertext_throwsKeystoreInvalidatedException() {
        SecurityUtils.getDatabasePassphrase(context)
        corruptStoredValue(ciphertextKey)

        // A tampered ciphertext must fail GCM auth and surface as a real, caller-visible
        // event — not be silently swallowed and regenerated inside getDatabasePassphrase()
        // itself. SecureDatabase.getDatabase() is the only place that's allowed to react
        // to this by deleting the DB and calling resetDatabasePassphrase() explicitly.
        assertThrows(KeystoreInvalidatedException::class.java) {
            SecurityUtils.getDatabasePassphrase(context)
        }
    }

    @Test
    fun corruptedIv_throwsKeystoreInvalidatedException() {
        SecurityUtils.getDatabasePassphrase(context)
        corruptStoredValue(ivKey)

        assertThrows(KeystoreInvalidatedException::class.java) {
            SecurityUtils.getDatabasePassphrase(context)
        }
    }

    @Test
    fun afterKeystoreInvalidatedException_resetDatabasePassphraseRecovers() {
        val original = SecurityUtils.getDatabasePassphrase(context)
        corruptStoredValue(ciphertextKey)

        assertThrows(KeystoreInvalidatedException::class.java) {
            SecurityUtils.getDatabasePassphrase(context)
        }

        // This is the real recovery path — the caller (SecureDatabase.getDatabase())
        // catches the exception above and calls this explicitly. It must never be an
        // automatic fallback inside getDatabasePassphrase() itself.
        val recovered = SecurityUtils.resetDatabasePassphrase(context)
        assertEquals("recovered passphrase must be 32 bytes", 32, recovered.size)
        assertFalse(
            "recovery must mint a genuinely new passphrase, not recover the old one",
            original.contentEquals(recovered)
        )

        val stable = SecurityUtils.getDatabasePassphrase(context)
        assertArrayEquals(
            "once reset, the new passphrase must decrypt normally on the next call",
            recovered,
            stable
        )
    }

    @Test
    fun missingCiphertext_regenerates() {
        SecurityUtils.getDatabasePassphrase(context)
        removeStoredValue(ciphertextKey)

        val passphrase = SecurityUtils.getDatabasePassphrase(context)
        assertEquals(32, passphrase.size)
        assertNotNull(prefs.getString(ciphertextKey, null))
        assertNotNull(prefs.getString(ivKey, null))
    }

    @Test
    fun missingIv_regenerates() {
        SecurityUtils.getDatabasePassphrase(context)
        removeStoredValue(ivKey)

        val passphrase = SecurityUtils.getDatabasePassphrase(context)
        assertEquals(32, passphrase.size)
        assertNotNull(prefs.getString(ciphertextKey, null))
        assertNotNull(prefs.getString(ivKey, null))
    }

    @Test
    fun bothMissing_generatesFreshPassphrase() {
        val original = SecurityUtils.getDatabasePassphrase(context)
        removeStoredValue(ciphertextKey)
        removeStoredValue(ivKey)

        val fresh = SecurityUtils.getDatabasePassphrase(context)
        assertEquals(32, fresh.size)
        // Not a strict security requirement (a fresh 256-bit random draw colliding with the
        // original is a non-event), but it's a cheap sanity check that generation actually ran.
        assertFalse(original.contentEquals(fresh))
    }

    @Test
    fun multipleInvalidationCycles_recoverEachTime() {
        var previous = SecurityUtils.getDatabasePassphrase(context)

        repeat(5) { cycle ->
            corruptStoredValue(ciphertextKey)

            assertThrows("cycle $cycle: corrupted envelope must throw", KeystoreInvalidatedException::class.java) {
                SecurityUtils.getDatabasePassphrase(context)
            }

            val recovered = SecurityUtils.resetDatabasePassphrase(context)
            assertFalse(
                "cycle $cycle: recovery must produce a new passphrase",
                previous.contentEquals(recovered)
            )

            val stable = SecurityUtils.getDatabasePassphrase(context)
            assertArrayEquals(
                "cycle $cycle: passphrase must remain stable immediately after recovery",
                recovered,
                stable
            )

            previous = recovered
        }
    }

    @Test
    fun storedValuesAreBase64Encoded() {
        SecurityUtils.getDatabasePassphrase(context)

        val storedCiphertext = prefs.getString(ciphertextKey, null)
        val storedIv = prefs.getString(ivKey, null)
        assertNotNull("ciphertext must be persisted", storedCiphertext)
        assertNotNull("IV must be persisted", storedIv)

        // Base64.decode throws IllegalArgumentException on malformed input — reaching the
        // assertions below is itself proof the stored strings are valid Base64.
        val decodedCiphertext = Base64.decode(storedCiphertext, Base64.NO_WRAP)
        val decodedIv = Base64.decode(storedIv, Base64.NO_WRAP)

        assertTrue("decoded ciphertext must not be empty", decodedCiphertext.isNotEmpty())
        assertTrue("decoded IV must not be empty", decodedIv.isNotEmpty())
    }

    @Test
    fun generatedPassphraseContainsEntropy() {
        val passphrase = SecurityUtils.getDatabasePassphrase(context)

        // Sanity checks only — not a statistical randomness test. Just rules out the obvious
        // failure modes of a broken SecureRandom call (e.g. an all-zero or unfilled buffer).
        assertTrue(
            "passphrase must not be all zero bytes",
            passphrase.any { it != 0.toByte() }
        )
        assertTrue(
            "passphrase must not be a single repeated byte value",
            passphrase.toSet().size > 1
        )
    }

    private fun corruptStoredValue(key: String) {
        val current = prefs.getString(key, null)
            ?: error("expected $key to already be present before corrupting it")
        val bytes = Base64.decode(current, Base64.NO_WRAP)
        // Flip one byte so this is a real GCM auth-tag failure, not just malformed Base64.
        bytes[0] = (bytes[0].toInt() xor 0xFF).toByte()
        prefs.edit().putString(key, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
    }

    private fun removeStoredValue(key: String) {
        prefs.edit().remove(key).apply()
    }
}
