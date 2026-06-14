package com.signalgate.multipoint.security

import android.os.Build
import android.os.StrictMode
import com.signalgate.multipoint.BuildConfig

object SecurityUtils {

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
}
