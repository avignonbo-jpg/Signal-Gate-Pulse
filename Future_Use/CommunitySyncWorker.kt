package com.signalgate.multipoint.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.signalgate.multipoint.logic.ReliableSourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * CommunitySyncWorker runs periodic syncs of federal blocklist sources
 * (FTC Do Not Call, FCC) via WorkManager.
 *
 * Scheduled by MainApplication on a periodic cadence (recommended: daily).
 * Runs on Dispatchers.IO — safe for network and database operations.
 *
 * Routing: ReliableSourceManager -> DataSourceRepository -> UnifiedEntryDao
 * No direct database access from this class.
 *
 * Output data keys:
 *   "sources_synced"   Int  — number of sources attempted
 *   "entries_added"    Int  — total entries written across all sources
 *   "sources_failed"   Int  — number of sources that returned an error
 *
 * On partial failure (some sources succeed, some fail): returns Result.success()
 * with failed count in output so the caller can inspect without retrying a full run.
 * On total failure (all sources fail): returns Result.retry().
 *
 * PULSE-TODO (2026-06): Register in AppModule workerModule once AppModule
 * is not in the active 11-file set. Wire via KoinWorkerFactory.
 *
 * Future_Use file promoted to production — Step 3.4 / DataSyncEngine integration.
 */
class CommunitySyncWorker(
    context: Context,
    params: WorkerParameters,
    private val reliableSourceManager: ReliableSourceManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CommunitySyncWorker"
        const val WORK_NAME = "community_source_sync"
        const val KEY_SOURCES_SYNCED = "sources_synced"
        const val KEY_ENTRIES_ADDED = "entries_added"
        const val KEY_SOURCES_FAILED = "sources_failed"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("Starting community source sync")

        try {
            val results = reliableSourceManager.syncAllFederalSources(applicationContext)

            val totalAdded = results.sumOf { it.entriesAdded }
            val failedCount = results.count { !it.success }
            val successCount = results.count { it.success }

            results.forEach { result ->
                if (result.success) {
                    Timber.tag(TAG).i(
                        "Source synced: ${result.sourceName} — ${result.entriesAdded} entries"
                    )
                } else {
                    Timber.tag(TAG).w(
                        "Source failed: ${result.sourceName} — ${result.errorMessage}"
                    )
                }
            }

            val outputData = workDataOf(
                KEY_SOURCES_SYNCED to results.size,
                KEY_ENTRIES_ADDED to totalAdded,
                KEY_SOURCES_FAILED to failedCount
            )

            return@withContext if (successCount == 0) {
                // All sources failed — schedule a retry
                Timber.tag(TAG).e("All sources failed — scheduling retry")
                Result.retry()
            } else {
                // At least one source succeeded — report success with stats
                Timber.tag(TAG).i(
                    "Sync complete — $totalAdded entries from $successCount sources " +
                    "($failedCount failed)"
                )
                Result.success(outputData)
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "CommunitySyncWorker fatal error")
            Result.retry()
        }
    }
}
