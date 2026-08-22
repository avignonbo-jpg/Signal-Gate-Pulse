package com.signalgate.pulse.database

import androidx.room.Database
import com.signalgate.pulse.StartupDiagnostics
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.signalgate.pulse.database.daos.CallLogDao
import com.signalgate.pulse.database.daos.PendingCardDao
import com.signalgate.pulse.database.daos.SettingDao
import com.signalgate.pulse.database.daos.SourceDao
import com.signalgate.pulse.database.daos.SyncHistoryDao
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.CallLogEntry
import com.signalgate.pulse.database.entities.PendingCardEntity
import com.signalgate.pulse.database.entities.SettingEntry
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.SyncHistoryEntry
import com.signalgate.pulse.database.entities.UnifiedEntryEntity

/**
 * SignalGateDatabase — Room database abstract class for SignalGate Pulse.
 *
 * Construction is exclusively managed by SecureDatabase via Koin injection.
 * Do NOT instantiate this class directly.
 *
 * exportSchema = true (changed from false in Phase 2.6):
 * Schema JSON files are required by Room's MigrationTestHelper to validate
 * that MIGRATION_1_2's DDL exactly matches what Room would generate from the
 * current entity definitions. Without schema export, MigrationTestHelper cannot
 * run and migration drift goes undetected until a user's device hits it.
 * Schema files are written to schemas/ at the project root (see build.gradle
 * ksp arg room.schemaLocation). They should be committed to version control —
 * they are the ground truth for what each version's schema looked like.
 *
 * Schema version 4: Phase 3.4 explicit source lifecycle and snapshot metadata.
 *
 * Version 3 remains the Phase 0.4 source activation timestamp schema; version 4
 * adds lifecycle state, snapshot version/hash, accepted record count, and a
 * bounded failure/rejection reason without backfilling unverifiable metadata.
 */
@Database(
    entities = [
        SourceEntity::class,
        UnifiedEntryEntity::class,
        CallLogEntry::class,
        SettingEntry::class,
        SyncHistoryEntry::class,
        PendingCardEntity::class,
    ],
    version = 4,
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
 *
 * DDL is intentionally explicit (no shorthand) so MigrationTestHelper can
 * validate column types and defaults exactly against the exported schema.
 * Any drift between this DDL and PendingCardEntity's generated schema will
 * surface in MigrationTest.kt before reaching a device.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_1_2_BEGIN)
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
        StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_1_2_END)
    }
}

/**
 * Migration 2 -> 3: adds the minimum Phase 0.4 last-known-good metadata.
 * Existing sources receive NULL for both fields, correctly representing that
 * no attempt or accepted snapshot has occurred since this migration.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_2_3_BEGIN)
        db.execSQL("ALTER TABLE sources ADD COLUMN last_attempted_sync INTEGER")
        db.execSQL("ALTER TABLE sources ADD COLUMN last_accepted_snapshot INTEGER")
        StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_2_3_END)
    }
}

/**
 * Migration 3 -> 4: adds explicit source lifecycle and accepted-snapshot metadata.
 * Existing rows start ENABLED with unknown snapshot metadata; no historical
 * acceptance or failure facts are invented during migration.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_3_4_BEGIN)
        db.execSQL("ALTER TABLE sources ADD COLUMN lifecycle_state TEXT NOT NULL DEFAULT 'ENABLED'")
        db.execSQL("ALTER TABLE sources ADD COLUMN snapshot_version TEXT")
        db.execSQL("ALTER TABLE sources ADD COLUMN snapshot_hash TEXT")
        db.execSQL("ALTER TABLE sources ADD COLUMN accepted_record_count INTEGER")
        db.execSQL("ALTER TABLE sources ADD COLUMN lifecycle_reason TEXT")
        StartupDiagnostics.mark(StartupDiagnostics.Event.MIGRATION_3_4_END)
    }
}
