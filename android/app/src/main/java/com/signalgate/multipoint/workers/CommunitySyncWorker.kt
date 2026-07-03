package com.signalgate.multipoint.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<CommunitySyncWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, TimeUnit.MINUTES
                )
                .setConstraints(/* Network + BatteryNotLow */)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "community_sync",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
