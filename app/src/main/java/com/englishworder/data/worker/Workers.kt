package com.englishworder.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.englishworder.data.repository.WordRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FetchWordsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: WordRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            repository.fetchPendingWords()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

@HiltWorker
class ReviewReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: WordRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dueCount = repository.getDueReviews(1000).size
        if (dueCount == 0) return Result.success()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "复习提醒",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("EnglishWorder")
            .setContentText("今日有 $dueCount 个单词待复习")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "review_reminder"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "review_reminder"
        const val FETCH_WORK_NAME = "fetch_words"
    }
}
