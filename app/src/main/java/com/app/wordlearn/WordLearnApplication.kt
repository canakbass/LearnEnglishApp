package com.app.wordlearn

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.wordlearn.domain.util.CrashReporter
import com.app.wordlearn.presentation.worker.DailyReminderWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class WordLearnApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Crashlytics: debug build'de toplama kapalı, release'de açık.
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        try {
            scheduleNotificationWorkers()
        } catch (e: Exception) {
            CrashReporter.reportException(TAG, "Failed to schedule notification workers", e)
        }
    }

    private fun scheduleNotificationWorkers() {
        val workManager = WorkManager.getInstance(this)
        scheduleDailyWorker(workManager, MORNING_WORK_NAME, MORNING_HOUR, REMINDER_MINUTE)
        scheduleDailyWorker(workManager, EVENING_WORK_NAME, EVENING_HOUR, REMINDER_MINUTE)
    }

    private fun scheduleDailyWorker(
        workManager: WorkManager,
        uniqueWorkName: String,
        hour: Int,
        minute: Int
    ) {
        val now = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelay = dueDate.timeInMillis - now.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // KEEP: aynı isimle mevcut bir kayıt varsa dokunma; yeniden enqueue yok.
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    companion object {
        private const val TAG = "WordLearnApplication"
        private const val MORNING_WORK_NAME = "morning_reminder"
        private const val EVENING_WORK_NAME = "evening_reminder"
        private const val MORNING_HOUR = 8
        private const val EVENING_HOUR = 20
        private const val REMINDER_MINUTE = 0
    }
}
