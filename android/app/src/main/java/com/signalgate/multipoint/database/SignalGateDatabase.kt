package com.signalgate.multipoint.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.signalgate.multipoint.database.daos.CallLogDao
import com.signalgate.multipoint.database.daos.PendingCardDao
import com.signalgate.multipoint.database.daos.SettingDao
import com.signalgate.multipoint.database.daos.SourceDao
import com.signalgate.multipoint.database.daos.SyncHistoryDao
import com.signalgate.multipoint.database.daos.UnifiedEntryDao
import com.signalgate.multipoint.database.entities.CallLogEntry
import com.signalgate.multipoint.database.entities.PendingCardEntity
import com.signalgate.multipoint.database.entities.SettingEntry
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.entities.SyncHistoryEntry
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity

/**
 * SignalGateDatabase is the Room database abstract class for SignalGate Pulse.
 *
 * Construction is exclusively managed by SecureDatabase via Koin injection.
 * Do NOT instantiate this class directly. Static construction methods have been
 * intentionally omitted — any bypass of SecureDatabase would create an unencrypted
 * database at a different filename and silently lose all user data.
 *
 * Sole permitted construction path:
 *   SecureDatabase.getDatabase(context) injected via AppModule -> databaseModule
 *
 * Schema version 2: PendingCardEntity added (Step 1.6)
 */
@Database(
    entities = [
        SourceEntity::class,
        UnifiedEntryEntity::class,
        CallLogEntry::class,
        SettingEntry::class,
        SyncHistoryEntry::class,
        PendingCardEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SignalGateDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun unifiedEntryDao(): UnifiedEntryDao
    abstract fun callLogDao(): CallLogDao
    abstract fun settingDao(): SettingDao
    abstract fun syncHistoryDao(): SyncHistoryDao
    abstract fun pendingCardDao(): PendingCardDao
}

/**
 * Migration 1 -> 2: Adds the pending_cards table for the post-call digest card system.
 * Applied automatically by SecureDatabase when an existing install upgrades.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                phoneNumber TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                decision TEXT NOT NULL,
                confidence INTEGER,
                decisionSource TEXT,
                dismissed INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_pending_cards_dismissed ON pending_cards (dismissed)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_pending_cards_timestamp ON pending_cards (timestamp)"
        )
    }
}
