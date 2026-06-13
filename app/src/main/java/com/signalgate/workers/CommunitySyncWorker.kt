package com.signalgate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CommunitySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO: Anonymized blocked numbers -> GitHub repo (use Retrofit/GitHub API)
        // Rate limited, opt-in
        return Result.success()
    }
}
