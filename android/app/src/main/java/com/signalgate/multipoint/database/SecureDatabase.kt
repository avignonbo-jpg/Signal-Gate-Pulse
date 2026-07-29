package com.signalgate.multipoint.database

import android.content.Context
import androidx.room.Room
import com.signalgate.multipoint.security.SecurityUtils
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.concurrent.Executors

object SecureDatabase {
    fun getDatabase(context: Context): SignalGateDatabase {
        // Updated per Architecture Contract Step 0.1:
        // Replaced hardcoded passphrase with Android Keystore derivation.
        val passphrase = SecurityUtils.getDatabasePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)
        val singleThreadExecutor = Executors.newSingleThreadExecutor()

        return Room.databaseBuilder(context, SignalGateDatabase::class.java, "secure_signal.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .setQueryExecutor(singleThreadExecutor)
            .setTransactionExecutor(singleThreadExecutor)
            .build()
    }
}
