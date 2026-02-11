package com.wechatmonitor.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wechatmonitor.analyzer.GLMAnalyzer
import com.wechatmonitor.database.AppDatabase
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.DailySummary
import com.wechatmonitor.network.NetworkModule
import com.wechatmonitor.notification.NotificationHelper
import com.wechatmonitor.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 每日摘要 Worker
 * 定时生成并发送每日重要消息摘要
 */
class DailySummaryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailySummaryWorker"
    }

    private val database = AppDatabase.getInstance(applicationContext)
    private val settingsRepository = SettingsRepository(applicationContext)
    private val notificationHelper = NotificationHelper(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始生成每日摘要")

            // 检查功能是否启用
            val settings = settingsRepository.settingsFlow.first()
            if (!settings.dailySummaryEnabled) {
                Log.d(TAG, "每日摘要功能未启用")
                return Result.success()
            }

            // 获取今天的重要消息
            val todayStart = getTodayStartTime()
            val todayMessages = database.messageDao()
                .getTodayImportantMessages(todayStart)
                .first()
                .map { it.toChatMessage() }

            if (todayMessages.isEmpty()) {
                Log.d(TAG, "今天没有重要消息")
                return Result.success()
            }

            // 生成摘要
            val summary = generateSummary(todayMessages, settings)

            // 发送通知
            notificationHelper.showDailySummary(summary)

            // 重新调度明天的任务
            rescheduleTomorrow(settings.dailySummaryTime)

            Log.d(TAG, "每日摘要生成完成: ${summary.totalImportantMessages}条消息")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "生成每日摘要失败", e)
            Result.failure()
        }
    }

    /**
     * 生成摘要
     */
    private suspend fun generateSummary(
        messages: List<ChatMessage>,
        settings: com.wechatmonitor.model.MonitorSettings
    ): DailySummary {
        return if (settings.glmApiKey.isNotEmpty() && settings.analysisMode != com.wechatmonitor.model.AnalysisMode.KEYWORD_ONLY) {
            // 使用GLM生成摘要
            generateSummaryWithGLM(messages, settings)
        } else {
            // 简单按群分组生成摘要
            generateSimpleSummary(messages)
        }
    }

    /**
     * 使用GLM生成摘要
     */
    private suspend fun generateSummaryWithGLM(
        messages: List<ChatMessage>,
        settings: com.wechatmonitor.model.MonitorSettings
    ): DailySummary {
        return try {
            val apiService = NetworkModule.createGLMApiService(settings.glmApiUrl)
            val analyzer = GLMAnalyzer(apiService, settings.glmApiKey)

            val result = analyzer.generateSummary(messages, LocalDate.now())
            result.getOrDefault(generateSimpleSummary(messages))
        } catch (e: Exception) {
            Log.e(TAG, "GLM生成摘要失败，使用简单方式", e)
            generateSimpleSummary(messages)
        }
    }

    /**
     * 生成简单摘要（按群分组）
     */
    private fun generateSimpleSummary(messages: List<ChatMessage>): DailySummary {
        val groupedMessages = messages.groupBy { it.groupName }

        val groupSummaries = groupedMessages.map { (groupName, msgs) ->
            com.wechatmonitor.model.GroupSummary(
                groupName = groupName,
                count = msgs.size,
                summary = generateSimpleGroupSummary(msgs)
            )
        }

        return DailySummary(
            date = LocalDate.now(),
            totalImportantMessages = messages.size,
            groupSummaries = groupSummaries
        )
    }

    /**
     * 生成单个群的简单摘要
     */
    private fun generateSimpleGroupSummary(messages: List<ChatMessage>): String {
        // 统计发送者
        val senderCount = messages.groupBy { it.senderName }.size

        // 获取前几条消息预览
        val preview = messages.take(3).joinToString("; ") {
            "${it.senderName}: ${it.content.take(20)}"
        }

        return "${senderCount}人发言。$preview"
    }

    /**
     * 重新调度明天的任务
     */
    private fun rescheduleTomorrow(time: String) {
        try {
            val scheduler = com.wechatmonitor.utils.Scheduler(applicationContext)
            scheduler.scheduleDailySummary(time)
        } catch (e: Exception) {
            Log.e(TAG, "重新调度任务失败", e)
        }
    }

    /**
     * 获取今天的开始时间
     */
    private fun getTodayStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

/**
 * MessageEntity 转 ChatMessage 扩展函数
 */
private fun com.wechatmonitor.database.entities.MessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        groupName = groupName,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        isImportant = isImportant,
        importanceScore = importanceScore,
        analysisMethod = com.wechatmonitor.model.AnalysisMethod.valueOf(analysisMethod),
        hasNotified = hasNotified
    )
}
