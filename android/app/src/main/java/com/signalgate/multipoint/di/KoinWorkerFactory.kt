package com.signalgate.multipoint.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf

/**
 * KoinWorkerFactory allows WorkManager to create worker instances using Koin's
 * dependency injection. This resolves constructors that require repositories
 * or logic engines.
 */
class KoinWorkerFactory : WorkerFactory(), KoinComponent {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return try {
            val workerClass = Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
            getKoin().getOrNull(
                clazz = workerClass.kotlin,
                qualifier = null,
                parameters = { parametersOf(appContext, workerParameters) }
            )
        } catch (e: Exception) {
            null
        }
    }
}
