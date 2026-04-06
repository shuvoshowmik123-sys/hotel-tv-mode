package com.hotelvision.launcher.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hotelvision.launcher.data.repository.LauncherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LauncherRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val roomNumber = "101"
        repository.syncRoomInfo(roomNumber)
        return Result.success()
    }
}
