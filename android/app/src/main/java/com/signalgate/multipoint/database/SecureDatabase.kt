package com.signalgate.multipoint.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object SecureDatabase {
    fun getDatabase(context: Context): SignalGateDatabase {
        // PULSE-TODO (2026-06): Replace hardcoded passphrase with Android Keystore derivation.
        // See Architecture Contract Step 1.12 — Keystore-Key-Design.md must be written first.
        // Key must be hardware-backed with setUserAuthenticationRequired(false) so
        // CallScreeningService can access the DB when the phone is locked.
        val passphrase = "your-secure-passphrase".toByteArray()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(context, SignalGateDatabase::class.java, "secure_signal.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
