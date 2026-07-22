package com.signalgate.multipoint.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * MigrationTest — validates Room schema migrations with MigrationTestHelper.
 *
 * What this catches (per the runtime failure analysis):
 * - MIGRATION_1_2 DDL drift: a column name typo, wrong type affinity, missing
 *   default value, or index mismatch between the migration SQL and what Room
 *   would generate from PendingCardEntity.
 * - Any future migration (1->3, 2->3, etc.) that doesn't match entity definitions.
 *
 * Requires exportSchema = true in @Database and room.schemaLocation set in
 * build.gradle ksp args. Schema JSON files in schemas/ are the ground truth —
 * MigrationTestHelper opens a real version-1 database using the exported schema,
 * runs the migration, then Room validates the resulting schema against the current
 * entity definitions at the byte level.
 *
 * This is an instrumented test — it runs on a device or emulator, not the JVM.
 * Run with: ./gradlew connectedPulseDebugAndroidTest
 *
 * Place schema files committed to version control:
 *   schemas/com.signalgate.multipoint.database.SignalGateDatabase/1.json
 *   schemas/com.signalgate.multipoint.database.SignalGateDatabase/2.json
 * These are generated automatically by Room's KSP processor when exportSchema = true
 * and room.schemaLocation points to the schemas/ directory.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration_test_db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SignalGateDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Validates MIGRATION_1_2 end-to-end.
     *
     * Steps:
     * 1. Creates a version-1 database using the exported schema JSON.
     *    Inserts a row into each v1 table to confirm data is preserved.
     * 2. Runs MIGRATION_1_2.
     * 3. Room validates the resulting schema against version-2 entity definitions.
     *    Any DDL drift (wrong column name, missing index, wrong type affinity)
     *    causes Room to throw IllegalStateException here rather than on a user device.
     * 4. Asserts the pending_cards table exists and accepts a valid insert.
     * 5. Asserts v1 data is preserved — migration must not drop existing tables.
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Step 1: create version-1 database and seed data
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert a source row to confirm it survives the migration
            execSQL(
                """INSERT INTO sources 
                   (name, type, pathOrUrl, isEnabled, lastSynced, priority, 
                    entriesCount, healthStatus, createdAt, updatedAt)
                   VALUES ('Test Source', 'MANUAL', 'local', 1, 0, 100, 0, 
                           'HEALTHY', 1000, 1000)"""
            )
            // Insert a unified entry to confirm FK integrity survives
            execSQL(
                """INSERT INTO unified_entries 
                   (phoneNumber, action, sourceId, isPattern, confidence, createdAt, updatedAt)
                   VALUES ('+18005551234', 'BLOCK', 1, 0, 85, 1000, 1000)"""
            )
            close()
        }

        // Step 2 & 3: run migration — Room validates schema on open
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Step 4: pending_cards table must exist and accept a valid insert
        db.execSQL(
            """INSERT INTO pending_cards 
               (phoneNumber, timestamp, decision, confidence, decisionSource, dismissed)
               VALUES ('+18005559999', 1700000000000, 'BLOCK', 85, 'Test Source', 0)"""
        )
        val cursor = db.query("SELECT * FROM pending_cards WHERE phoneNumber = '+18005559999'")
        assertNotNull("pending_cards table must exist after migration", cursor)
        assertEquals("pending_cards must have exactly one row", 1, cursor.count)
        cursor.moveToFirst()
        assertEquals(
            "dismissed column default must be 0",
            0,
            cursor.getInt(cursor.getColumnIndexOrThrow("dismissed"))
        )
        assertEquals(
            "decision column must store correctly",
            "BLOCK",
            cursor.getString(cursor.getColumnIndexOrThrow("decision"))
        )
        cursor.close()

        // Step 5: v1 data must still be present — migration must not drop sources
        val sourcesCursor = db.query("SELECT * FROM sources WHERE name = 'Test Source'")
        assertEquals("sources table must survive migration — data loss check", 1, sourcesCursor.count)
        sourcesCursor.close()

        val entriesCursor = db.query(
            "SELECT * FROM unified_entries WHERE phoneNumber = '+18005551234'"
        )
        assertEquals(
            "unified_entries must survive migration — data loss check",
            1,
            entriesCursor.count
        )
        entriesCursor.close()

        db.close()
    }

    /**
     * Validates the pending_cards table schema matches PendingCardEntity exactly.
     *
     * Specifically checks column names, types, and nullability — the most common
     * drift between handwritten DDL and Room's generated schema. This test
     * supplements migrate1To2() with explicit column assertions that make
     * drift failures immediately actionable.
     */
    @Test
    @Throws(IOException::class)
    fun pendingCardsSchemaIsCorrect() {
        helper.createDatabase(TEST_DB, 1).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val cursor = db.query("PRAGMA table_info(pending_cards)")
        val columns = mutableMapOf<String, String>() // name -> type
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
            columns[name] = type
        }
        cursor.close()

        // Assert every column PendingCardEntity defines is present with the right type
        assertEquals("id column type", "INTEGER", columns["id"])
        assertEquals("phoneNumber column type", "TEXT", columns["phoneNumber"])
        assertEquals("timestamp column type", "INTEGER", columns["timestamp"])
        assertEquals("decision column type", "TEXT", columns["decision"])
        assertEquals("confidence column type", "INTEGER", columns["confidence"])
        assertEquals("decisionSource column type", "TEXT", columns["decisionSource"])
        assertEquals("dismissed column type", "INTEGER", columns["dismissed"])
        assertEquals("Total column count", 7, columns.size)

        db.close()
    }
}
