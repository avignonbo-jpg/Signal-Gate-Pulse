package com.signalgate.pulse.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.signalgate.pulse.StartupDiagnostics
import com.signalgate.pulse.security.DatabaseResetEvent
import com.signalgate.pulse.security.KeystoreInvalidatedException
import com.signalgate.pulse.security.SecurityUtils
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import timber.log.Timber
import java.util.concurrent.Executors

object SecureDatabase {
    private const val DB_NAME = "secure_signal.db"

    init {
        // Required for net.zetetic:sqlcipher-android (unlike the legacy
        // net.sqlcipher.database library, this artifact does NOT auto-load its
        // native library via SQLiteDatabase.loadLibs()). Without this call,
        // SQLiteConnection.nativeOpen() has no native implementation bound and
        // every DB open throws UnsatisfiedLinkError, crash-looping the app.
        System.loadLibrary("sqlcipher")
    }

    fun getDatabase(context: Context): SignalGateDatabase {
        StartupDiagnostics.mark(StartupDiagnostics.Event.SQLCIPHER_INIT_BEGIN)
        // Updated per Architecture Contract Step 0.1:
        // Replaced hardcoded passphrase with Android Keystore derivation.
        val passphrase = try {
            SecurityUtils.getDatabasePassphrase(context)
        } catch (e: KeystoreInvalidatedException) {
            // The existing DB is unreadable — the wrapped passphrase can no longer be
            // decrypted (Keystore key invalidated, or the wrapped-passphrase blob is
            // corrupt). Log unconditionally (this must be visible in release builds,
            // not just debug — see MainApplication's release Timber tree), delete the
            // now-orphaned file so it doesn't linger as a dead, inaccessible artifact,
            // generate a fresh passphrase, and tell the UI layer so the user isn't left
            // wondering why their blocklist/history is suddenly empty.
            Timber.e(e, "Database passphrase could not be decrypted — resetting local database")
            context.getDatabasePath(DB_NAME).let { dbFile ->
                dbFile.delete()
                context.getDatabasePath("$DB_NAME-wal").delete()
                context.getDatabasePath("$DB_NAME-shm").delete()
            }
            DatabaseResetEvent.signal()
            SecurityUtils.resetDatabasePassphrase(context)
        }

        // Known sqlcipher-android quirk (zetetic/sqlcipher-android issue #4):
        // loading the native lib in an init{} block / Application.onCreate() is
        // sometimes NOT sufficient — the SupportOpenHelperFactory needs the
        // load call to happen in close proximity to its own construction.
        // System.loadLibrary is idempotent, so this second call is a safe,
        // cheap no-op on runs where the init{} block already loaded it, and a
        // real fix on the runs where it didn't.
        System.loadLibrary("sqlcipher")

        val factory = SupportOpenHelperFactory(passphrase)
        StartupDiagnostics.mark(StartupDiagnostics.Event.SQLCIPHER_FACTORY_READY)

        // Previously a single Executors.newSingleThreadExecutor() instance was
        // passed to BOTH setQueryExecutor() and setTransactionExecutor(). That
        // is a documented Room anti-pattern for two reasons:
        //
        //   1. Deadlock risk: if a transaction running on that thread needs to
        //      dispatch a query, and the query executor IS that same one
        //      thread, the query has nowhere to run — it queues behind the
        //      transaction that's waiting on it.
        //   2. Total serialization: every read AND write in the entire app —
        //      dashboard counters, source syncs, Bloom rehydration's paged
        //      reads, manual rule writes, and every getCallDecision() lookup
        //      from a live incoming call — funneled through one thread. A
        //      call arriving while that thread was occupied by unrelated work
        //      would have its decision query queued behind it, silently
        //      eating into (or blowing past) processScreeningCall()'s 3.5s
        //      withTimeout with no error until the timeout itself fired.
        //
        // Query executor: a small fixed pool so concurrent reads (e.g. a
        // dashboard counter query and a getCallDecision() lookup) don't
        // serialize behind each other. Transaction executor: kept as its own
        // single thread — Room transactions are already serialized by SQLite
        // itself, but this thread is now independent of the query pool above,
        // which is what actually removes the deadlock risk.
        val queryExecutor = Executors.newFixedThreadPool(4)
        val transactionExecutor = Executors.newSingleThreadExecutor()
        StartupDiagnostics.mark(StartupDiagnostics.Event.ROOM_OPEN_MIGRATION_BEGIN)

        return Room.databaseBuilder(context, SignalGateDatabase::class.java, DB_NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    StartupDiagnostics.mark(StartupDiagnostics.Event.ROOM_OPEN_MIGRATION_END)
                }
            })
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .setQueryExecutor(queryExecutor)
            .setTransactionExecutor(transactionExecutor)
            .build()
    }
}
