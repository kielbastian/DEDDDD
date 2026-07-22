package com.mibox.iptv.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mibox.iptv.domain.repository.EpgRepository
import com.mibox.iptv.domain.repository.PlaylistRepository
import com.mibox.iptv.domain.repository.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import java.util.concurrent.TimeUnit

/** Okresowa synchronizacja EPG w tle (tylko przy sieci). Idempotentna: upsert do Room. */
@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val epgRepository: EpgRepository,
    private val playlistRepository: PlaylistRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // sources() to nieskończony Flow z Room — pobieramy pierwszą emisję (first),
        // nie last (który nigdy by się nie zakończył).
        val sourceId = playlistRepository.sources().first().firstOrNull()?.id
            ?: return Result.success()
        val status = epgRepository.syncEpg(sourceId).lastOrNull()
        return when (status) {
            is SyncStatus.Error -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val UNIQUE = "epg_sync"

        /** Rejestracja co 6 h przy dostępnej sieci. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EpgSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }
    }
}
