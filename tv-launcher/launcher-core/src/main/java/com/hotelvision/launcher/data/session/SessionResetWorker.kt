package com.hotelvision.launcher.data.session

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * A background worker that automatically triggers the guest session reset.
 * Useful for scenarios where a PMS webhook or ADB broadcast isn't used, ensuring
 * that daily at a specific checkout time, the TV session data is wiped clean.
 */
class SessionResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Trigger the centralized reset handler
            CheckoutResetHandler.reset(applicationContext)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
