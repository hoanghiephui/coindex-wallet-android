package io.horizontalsystems.bankwallet.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.horizontalsystems.bankwallet.worker.SyncWorker.Companion.eveningWork
import io.horizontalsystems.bankwallet.worker.SyncWorker.Companion.morningWork
import java.util.concurrent.TimeUnit

object Sync {
    // This method is initializes sync, the process that keeps the app's data current.
    // It is called from the app module's Application.onCreate() and should be only done once.
    fun initialize(
        context: Context,
        isNotificationPrice: Boolean,
        isNotificationNews: Boolean
    ) {
        initializePrice(context, isNotificationPrice)
        initializeNews(context, isNotificationNews)
    }

    fun initializePrice(
        context: Context,
        isNotificationPrice: Boolean,
    ) {
        if (isNotificationPrice) {
            WorkManager.getInstance(context).apply {
                // Run sync on app startup and ensure only one sync worker runs at any time
                enqueueUniquePeriodicWork(
                    MORNING_SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    morningWork,
                )
            }
            WorkManager.getInstance(context).apply {
                // Run sync on app startup and ensure only one sync worker runs at any time
                enqueueUniquePeriodicWork(
                    EVENING_SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    eveningWork,
                )
            }
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(MORNING_SYNC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(EVENING_SYNC_WORK_NAME)
        }
    }

    fun initializeNews(
        context: Context,
        isNotificationNews: Boolean
    ) {
        if (isNotificationNews) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "news-notifications",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                createPeriodicWork(),
            )
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("news-notifications")
        }
    }
}

// This name should not be changed otherwise the app may have concurrent sync requests running
internal const val MORNING_SYNC_WORK_NAME = "MorningSyncWork"
internal const val EVENING_SYNC_WORK_NAME = "EveningSyncWork"

private fun createPeriodicWork(): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<DelegatingWorker>(2, TimeUnit.HOURS)
        .setConstraints(SyncConstraints)
        .setInputData(NewsNotificationWorker::class.delegatedData())
        .setInitialDelay(5, TimeUnit.MINUTES)
        .build()
