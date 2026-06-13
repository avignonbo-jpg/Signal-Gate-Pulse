package com.signalgate.multipoint.security

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.signalgate.multipoint.BuildConfig

object SecurityUtils {

    private const val TAG = "SecurityUtils"

    /**
     * Enables StrictMode in debug builds only.
     * Detects disk/network access on the main thread and other violations.
     */
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

        Log.d(TAG, "StrictMode enabled")
    }

    /**
     * Requests a Play Integrity token to verify the app has not been tampered with.
     * Should be called on a background coroutine — result is logged in debug,
     * and should be verified server-side in production.
     *
     * @param context Application context
     * @param nonce A unique per-request nonce (base64, min 16 bytes recommended)
     */
    fun requestIntegrityToken(context: Context, nonce: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Play Integrity check skipped in debug build")
            return
        }

        try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { response ->
                    val token = response.token()
                    // TODO: Send token to your backend for server-side verification.
                    // Never trust the result client-side only.
                    Log.d(TAG, "Integrity token received (send to backend for verification)")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Play Integrity check failed: ${exception.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Play Integrity unavailable: ${e.message}")
        }
    }
}
