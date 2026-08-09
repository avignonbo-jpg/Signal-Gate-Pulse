package com.signalgate.multipoint.security

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.signalgate.multipoint.BuildConfig
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import timber.log.Timber

object SecurityUtils {

    private const val KEY_ALIAS = "SignalGateDatabaseKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PASSPHRASE_LENGTH_BYTES = 32

    private const val PREFS_NAME = "signalgate_secure_prefs"
    private const val PREF_CIPHERTEXT = "db_passphrase_ciphertext"
    private const val PREF_IV = "db_passphrase_iv"

    fun enableStrictMode() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        detectUnsafeIntentLaunch()
                    }
                }
                .penaltyLog()
                .build()
        )
    }

    /**
     * Returns the SQLCipher passphrase for the secure database, protected at rest by a
     * hardware-backed Android Keystore key.
     *
     * Per Architecture Contract §5.7, §9:
     * - Uses KeyGenParameterSpec
     * - setUserAuthenticationRequired(false) to allow background access when locked
     *   (the CallScreeningService must be able to query the DB while the device is locked).
     *
     * IMPORTANT — envelope encryption, not direct key export:
     * AndroidKeyStore-backed SecretKeys are intentionally non-exportable — the key material
     * never leaves the secure hardware (TEE/StrongBox where available). Calling
     * `secretKey.encoded` on such a key throws `UnsupportedOperationException` (or returns
     * null on some OEMs); it can never be used directly as a raw passphrase.
     *
     * Instead, this uses the standard "envelope encryption" pattern:
     * 1. A random 256-bit passphrase is generated once via SecureRandom.
     * 2. That passphrase is encrypted (AES/GCM) using the Keystore key as a KEK
     *    (key-encrypting-key) — the encryption/decryption happens inside the Cipher,
     *    the raw key bytes are never touched by app code.
     * 3. Only the ciphertext + IV are persisted (SharedPreferences); they are useless
     *    without the hardware-backed Keystore key to decrypt them.
     * 4. On every subsequent call, the ciphertext is decrypted back into the same
     *    passphrase bytes and handed to SQLCipher.
     *
     * Requires a Context because the wrapped (encrypted) passphrase must be persisted
     * somewhere — SharedPreferences here; the values stored are ciphertext, not secrets.
     *
     * @throws KeystoreInvalidatedException if a wrapped passphrase is stored but the
     * current Keystore key can't decrypt it — this means the existing database is about
     * to become unreadable and callers must handle it explicitly (see SecureDatabase),
     * not silently regenerate around it.
     */
    fun getDatabasePassphrase(context: Context): ByteArray {
        val secretKey = getOrCreateKeystoreKey()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val existingCiphertext = prefs.getString(PREF_CIPHERTEXT, null)
        val existingIv = prefs.getString(PREF_IV, null)

        if (existingCiphertext != null && existingIv != null) {
            try {
                return decrypt(
                    secretKey,
                    Base64.decode(existingCiphertext, Base64.NO_WRAP),
                    Base64.decode(existingIv, Base64.NO_WRAP)
                )
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                // Android's own explicit signal that this key can never be used again.
                // Must be caught before InvalidKeyException below — it's a subclass.
                throw KeystoreInvalidatedException(e)
            } catch (e: java.security.InvalidKeyException) {
                // Key rejected outright by the Cipher — same category as above.
                throw KeystoreInvalidatedException(e)
            } catch (e: javax.crypto.BadPaddingException) {
                // GCM auth-tag check failed — ciphertext/IV don't decrypt with this key.
                // This is the actual signature of "Keystore key no longer matches what
                // encrypted this blob" and is the case this method's callers must treat
                // as a real, destructive-database-reset-worthy event.
                throw KeystoreInvalidatedException(e)
            }
            // Deliberately NOT a blanket catch (e: Exception) here — this is a
            // destructive path (SecureDatabase.getDatabase() deletes the existing DB
            // file on KeystoreInvalidatedException). An unrelated bug — a malformed
            // Base64 string, a transient IllegalStateException, etc. — must surface as
            // itself rather than being misclassified into a database wipe.
        }

        // No passphrase stored yet — first install. Expected, safe to generate silently.
        return generateAndPersistPassphrase(secretKey, prefs)
    }

    /**
     * Explicitly generates and persists a brand-new wrapped passphrase, overwriting
     * whatever (if anything) was stored before.
     *
     * Only call this after deliberately handling a [KeystoreInvalidatedException] —
     * e.g. after logging the event and deleting the now-unreadable database file.
     * Do not call this as an automatic fallback from [getDatabasePassphrase].
     */
    fun resetDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return try {
            generateAndPersistPassphrase(getOrCreateKeystoreKey(), prefs)
        } catch (e: Exception) {
            // getOrCreateKeystoreKey() only generates a fresh key when containsAlias()
            // is false — but containsAlias() can still report true for an alias whose
            // key material is no longer usable for any operation (the same class of
            // invalidation that got us into this recovery path in the first place).
            // If reusing that "existing" key just failed too, don't leave the caller
            // with an unhandled exception: delete the alias outright so the next
            // getOrCreateKeystoreKey() call is forced down its key-generation branch,
            // then retry once with a guaranteed-fresh key.
            Timber.w(e, "Reusing existing Keystore alias failed during passphrase reset — deleting alias and retrying with a fresh key")
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
            generateAndPersistPassphrase(getOrCreateKeystoreKey(), prefs)
        }
    }

    private fun generateAndPersistPassphrase(
        secretKey: SecretKey,
        prefs: android.content.SharedPreferences
    ): ByteArray {
        val newPassphrase = ByteArray(PASSPHRASE_LENGTH_BYTES).apply { SecureRandom().nextBytes(this) }
        val (ciphertext, iv) = encrypt(secretKey, newPassphrase)
        prefs.edit()
            .putString(PREF_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        return newPassphrase
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        return if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Required for background service access when device is locked
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } else {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
    }

    private fun encrypt(key: SecretKey, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(plaintext)
        return ciphertext to cipher.iv
    }

    private fun decrypt(key: SecretKey, ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
