package com.wechatmonitor.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wechatmonitor.worker.DailySummaryWorker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * 任务调度器
 */
class Scheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val DAILY_SUMMARY_WORK_NAME = "daily_summary_work"
        const val KEY_SUMMARY_TIME = "summary_time"
    }

    /**
     * 调度每日摘要任务
     * @param time 格式: HH:mm
     */
    fun scheduleDailySummary(time: String) {
        try {
            val parts = time.split(":")
            val hour = parts[0].toIntOrNull() ?: 20
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val targetTime = LocalTime.of(hour, minute)
            val now = LocalTime.now()
            val today = LocalDate.now()

            // 计算下次执行时间
            val nextRunDateTime = if (now.isBefore(targetTime)) {
                // 今天还未到指定时间
                today.atTime(targetTime)
            } else {
                // 今天已过指定时间，明天执行
                today.plusDays(1).atTime(targetTime)
            }

            val currentDateTime = java.time.LocalDateTime.now()
            val delay = Duration.between(currentDateTime, nextRunDateTime)

            // 创建一次性任务
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()

            val inputData = Data.Builder()
                .putString(KEY_SUMMARY_TIME, time)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>()
                .setInitialDelay(delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(DAILY_SUMMARY_WORK_NAME)
                .build()

            workManager.enqueueUniqueWork(
                DAILY_SUMMARY_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 取消每日摘要任务
     */
    fun cancelDailySummary() {
        workManager.cancelUniqueWork(DAILY_SUMMARY_WORK_NAME)
    }

    /**
     * 取消所有任务
     */
    fun cancelAllWork() {
        workManager.cancelAllWork()
    }

    /**
     * 立即执行每日摘要（用于测试）
     */
    fun runDailySummaryNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setConstraints(constraints)
            .addTag(DAILY_SUMMARY_WORK_NAME)
            .build()

        workManager.enqueue(workRequest)
    }
}
