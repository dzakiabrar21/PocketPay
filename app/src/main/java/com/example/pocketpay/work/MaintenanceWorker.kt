package com.example.pocketpay.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.pocketpay.MainActivity
import com.example.pocketpay.R
import com.example.pocketpay.domain.notify.Milestone
import com.example.pocketpay.domain.notify.Milestones
import com.example.pocketpay.domain.repository.BankingRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MaintenanceEntryPoint {
    fun repository(): BankingRepository
}

/**
 * Daily background job: credits interest even when the app is not opened, then announces
 * milestones (deposit matured, savings goal reached) with local notifications.
 */
class MaintenanceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, MaintenanceEntryPoint::class.java)
            .repository()

        val credited = repository.accrueInterest().getOrElse { return Result.retry() }
        val notifier = PocketNotifier(applicationContext)

        if (credited > 0) {
            notifier.show(
                id = NOTIF_INTEREST,
                title = applicationContext.getString(R.string.notif_interest_title),
                text = applicationContext.resources.getQuantityString(R.plurals.notif_interest_text, credited, credited)
            )
        }

        val pockets = repository.getPockets().first()
        for (m in Milestones.detect(pockets, System.currentTimeMillis(), notifier.notifiedKeys())) {
            val (title, text) = when (m) {
                is Milestone.DepositMatured -> applicationContext.getString(R.string.notif_matured_title) to
                    applicationContext.getString(R.string.notif_matured_text, m.pocket.name)
                is Milestone.GoalReached -> applicationContext.getString(R.string.notif_goal_title) to
                    applicationContext.getString(R.string.notif_goal_text, m.pocket.name)
            }
            notifier.show(id = m.key.hashCode(), title = title, text = text)
            notifier.markNotified(m.key)
        }
        return Result.success()
    }

    companion object {
        private const val NOTIF_INTEREST = 1001
        const val UNIQUE_NAME = "pocketpay_maintenance"

        /** Runs once a day; KEEP means re-scheduling on every app start never resets the timer. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

/** Posts private notifications: the lock screen only shows a generic line, details need unlocking. */
class PocketNotifier(private val context: Context) {
    private val prefs = context.getSharedPreferences("pocketpay_notified", Context.MODE_PRIVATE)

    fun notifiedKeys(): Set<String> = prefs.getStringSet("keys", emptySet()) ?: emptySet()

    fun markNotified(key: String) {
        prefs.edit().putStringSet("keys", notifiedKeys() + key).apply()
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(id: Int, title: String, text: String) {
        if (!canNotify()) return
        ensureChannel()
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_public_text))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: nothing to show.
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "pocketpay_updates"
    }
}
