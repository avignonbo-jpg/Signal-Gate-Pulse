package com.signalgate.multipoint.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object SecureDatabase {
    fun getDatabase(context: Context): SignalGateDatabase {
        val passphrase = "your-secure-passphrase".toByteArray() // Derive from Biometric/KeyStore in prod
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(context, SignalGateDatabase::class.java, "secure_signal.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration() // Handle carefully in prod
            .build()
    }
}
