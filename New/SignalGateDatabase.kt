package com.signalgate.multipoint.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.signalgate.multipoint.database.daos.CallLogDao
import com.signalgate.multipoint.database.daos.SettingDao
import com.signalgate.multipoint.database.daos.SourceDao
import com.signalgate.multipoint.database.daos.SyncHistoryDao
import com.signalgate.multipoint.database.daos.UnifiedEntryDao
import com.signalgate.multipoint.database.entities.CallLogEntry
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
 */
@Database(
    entities = [
        SourceEntity::class,
        UnifiedEntryEntity::class,
        CallLogEntry::class,
        SettingEntry::class,
        SyncHistoryEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SignalGateDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun unifiedEntryDao(): UnifiedEntryDao
    abstract fun callLogDao(): CallLogDao
    abstract fun settingDao(): SettingDao
    abstract fun syncHistoryDao(): SyncHistoryDao
}

/**
 * Migration placeholder for future schema updates.
 * Add migrations here as the schema evolves and bump the @Database version above.
 *
 * Example usage when needed:
 *   SecureDatabase.getDatabase(context) is built with .addMigrations(MIGRATION_1_2)
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Example: db.execSQL("ALTER TABLE sources ADD COLUMN new_column TEXT DEFAULT NULL")
    }
}
