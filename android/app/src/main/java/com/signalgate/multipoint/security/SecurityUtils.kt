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
            } catch (e: Exception) {
                // Keystore key was invalidated (e.g. lock-screen removed on some OEMs) or the
                // stored blob is corrupt. Fall through and re-wrap a fresh passphrase rather
                // than crash — this is a recoverable local-cache condition, not a hard failure.
            }
        }

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
