package com.signalgate.multipoint.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.signalgate.multipoint.logic.DataSyncEngine
import com.signalgate.multipoint.logic.ReliableSourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * CommunitySyncWorker — Phase 2.5 (Contract §5.3).
 * Exponential backoff, network/battery constraints, foreground service option.
 * Idempotent, retryable vs fatal errors.
 */
class CommunitySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val reliableSourceManager: ReliableSourceManager by inject()
    private val dataSyncEngine: DataSyncEngine by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.i("CommunitySyncWorker started")

        try {
            val results = reliableSourceManager.syncAllFederalSources()
            val successCount = results.count { it.success }

            if (successCount == results.size) {
                Timber.i("All sources synced successfully")
                Result.success()
            } else {
                Timber.w("Partial sync failure")
                Result.retry() // exponential backoff via WorkRequest
            }
        } catch (e: Exception) {
            Timber.e(e, "Sync worker fatal error")
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "CommunitySyncWorker"

        /** Single unique-work name for this worker — used by enqueueUniquePeriodicWork(). */
        const val WORK_NAME = "community_sync"

        private const val SYNC_INTERVAL_HOURS = 24L
        private const val BACKOFF_DELAY_MINUTES = 10L

        /**
         * Schedules CommunitySyncWorker as a daily periodic job with exponential
         * backoff on failure and network/battery constraints.
         *
         * This is the single source of truth for scheduling this worker — call it
         * once from MainApplication.onCreate() and nowhere else. (Previously,
         * MainApplication.scheduleCommunitySync() enqueued its own separate,
         * constraint-less periodic request under the same work name — two
         * competing definitions of the same job. That duplicate has been removed;
         * this is now the only place "community_sync" is enqueued.)
         *
         * KEEP means a running or already-enqueued periodic job is never
         * cancelled and re-enqueued on every app launch — one job total, no
         * duplicate syncs across restarts.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CommunitySyncWorker>(
                SYNC_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Timber.tag(TAG).i("Scheduled: daily, backoff=${BACKOFF_DELAY_MINUTES}min exponential, network+battery constraints")
        }
    }
}
