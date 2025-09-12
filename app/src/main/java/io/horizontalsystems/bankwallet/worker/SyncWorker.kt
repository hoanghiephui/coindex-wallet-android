package io.horizontalsystems.bankwallet.worker

import android.Manifest.permission
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.android.billing.network.AppDispatcher
import com.android.billing.network.Dispatcher
import com.wallet.blockchain.bitcoin.BuildConfig
import com.wallet.blockchain.bitcoin.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.getColorCompat
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.repository.CoinBaseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.Duration
import java.util.concurrent.TimeUnit

@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
    private val repository: CoinBaseRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        try {
            val result =
                repository.getPriceCoin(base = "USD", filter = "listed", resolution = "latest")
            val data = result.data ?: return@withContext Result.retry()

            val priceData = listOf("BTC", "ETH", "BCH").mapNotNull { code ->
                data.find { it.base == code }?.let { coin ->
                    val priceChange = (coin.prices?.latestPrice?.percentChange?.day ?: 0.0) * 100
                    code to App.numberFormatter.format(priceChange, 0, 2, suffix = "%")
                }
            }.toMap()

            val btcChange = priceData["BTC"]?.removeSuffix("%")?.toDoubleOrNull() ?: 0.0
            val content = appContext.getString(
                if (btcChange > 0) R.string.Notification_PriceUp1 else R.string.Notification_PriceDown1,
                *priceData.flatMap { listOf(it.key, it.value) }.toTypedArray()
            )

            showNotification(appContext, content)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }


    private fun showNotification(context: Context, message: String) {
        if (checkSelfPermission(context, permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
            return
        }
        createNotificationChannelIfNeeded(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, getChannelId())
            .setSmallIcon(R.drawable.ic_logo_notification)
            .setContentTitle(context.getString(R.string.Notification_Title1))
            .setContentText(message)
            .setColor(context.getColorCompat(R.color.issyk_blue))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (checkSelfPermission(context, permission.POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannelIfNeeded(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(getChannelId()) == null) {
            val channel = NotificationChannel(
                getChannelId(),
                "Blockchain",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications whenever work starts"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                lightColor = Color.GRAY
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getChannelId(): String = BuildConfig.APPLICATION_ID

    companion object {
        private const val NOTIFICATION_ID = 2022

        private val morningDelay = calculateInitialDelayForTime(7)
        private val eveningDelay = calculateInitialDelayForTime(19)

        val morningWork = createPeriodicWork(morningDelay)
        val eveningWork = createPeriodicWork(eveningDelay)

        private fun createPeriodicWork(initialDelay: Long): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<DelegatingWorker>(24, TimeUnit.HOURS)
                .setConstraints(SyncConstraints)
                .setInputData(SyncWorker::class.delegatedData())
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .build()

        private fun calculateInitialDelayForTime(hour: Int): Long {
            val now = DateTime.now()
            val targetTime = now.withTimeAtStartOfDay().plusHours(hour)
            return Duration(
                now,
                if (now.hourOfDay < hour) targetTime else targetTime.plusDays(1)
            ).standardMinutes
        }
    }
}
