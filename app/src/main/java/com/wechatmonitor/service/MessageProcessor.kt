package com.wechatmonitor.service

import android.util.Log
import com.wechatmonitor.analyzer.GLMAnalyzer
import com.wechatmonitor.analyzer.KeywordAnalyzer
import com.wechatmonitor.analyzer.MessageAnalyzer
import com.wechatmonitor.database.AppDatabase
import com.wechatmonitor.database.entities.MessageEntity
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.MonitorSettings
import com.wechatmonitor.network.NetworkModule
import com.wechatmonitor.notification.NotificationHelper
import com.wechatmonitor.notification.SoundPlayer
import com.wechatmonitor.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 消息处理器
 * 负责分析、存储和通知重要消息
 */
class MessageProcessor(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val soundPlayer: SoundPlayer
) {

    private val processorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var messageAnalyzer: MessageAnalyzer? = null
    private var currentSettings: MonitorSettings? = null

    companion object {
        private const val TAG = "MessageProcessor"
    }

    /**
     * 处理消息
     */
    fun processMessage(message: ChatMessage) {
        processorScope.launch {
            try {
                // 获取最新设置
                val settings = settingsRepository.settingsFlow.first()
                currentSettings = settings

                // 更新分析器
                updateAnalyzerIfNeeded(settings)

                // 分析消息
                val analyzedMessage = analyzeMessage(message, settings)

                // 保存到数据库
                saveMessage(analyzedMessage)

                // 如果是重要消息，发送通知
                if (analyzedMessage.isImportant && !analyzedMessage.hasNotified) {
                    showNotification(analyzedMessage, settings)

                    // 标记为已通知
                    val updatedMessage = analyzedMessage.copy(hasNotified = true)
                    saveMessage(updatedMessage)
                }

                Log.d(TAG, "消息处理完成: ${message.groupName} - ${message.senderName}")
            } catch (e: Exception) {
                Log.e(TAG, "处理消息失败", e)
            }
        }
    }

    /**
     * 分析消息
     */
    private suspend fun analyzeMessage(
        message: ChatMessage,
        settings: MonitorSettings
    ): ChatMessage {
        return withContext(Dispatchers.Default) {
            val analyzer = messageAnalyzer ?: return@withContext message
            try {
                analyzer.analyze(message, settings)
            } catch (e: Exception) {
                Log.e(TAG, "分析消息失败", e)
                message.copy(
                    isImportant = false,
                    analysisMethod = com.wechatmonitor.model.AnalysisMethod.NONE
                )
            }
        }
    }

    /**
     * 保存消息到数据库
     */
    private suspend fun saveMessage(message: ChatMessage) {
        val entity = MessageEntity(
            id = message.id,
            groupName = message.groupName,
            senderName = message.senderName,
            content = message.content,
            timestamp = message.timestamp,
            isImportant = message.isImportant,
            importanceScore = message.importanceScore,
            analysisMethod = message.analysisMethod.name,
            hasNotified = message.hasNotified
        )
        database.messageDao().insert(entity)
    }

    /**
     * 显示通知
     */
    private fun showNotification(message: ChatMessage, settings: MonitorSettings) {
        // 显示系统通知
        notificationHelper.showImportantMessageNotification(message)

        // 播放声音和震动
        if (settings.soundEnabled || settings.vibrationEnabled) {
            if (settings.soundEnabled && settings.vibrationEnabled) {
                soundPlayer.playAlert()
            } else if (settings.soundEnabled) {
                soundPlayer.playImportantAlert()
            } else if (settings.vibrationEnabled) {
                soundPlayer.playVibrate()
            }
        }
    }

    /**
     * 更新分析器配置
     */
    private fun updateAnalyzerIfNeeded(settings: MonitorSettings) {
        // 检查是否需要重新创建分析器
        if (messageAnalyzer == null || needsRecreate(settings)) {
            recreateAnalyzer(settings)
        }
    }

    /**
     * 检查是否需要重新创建分析器
     */
    private fun needsRecreate(settings: MonitorSettings): Boolean {
        // 这里可以根据实际需要判断
        // 例如：关键词规则变化、API Key变化等
        return true
    }

    /**
     * 重新创建分析器
     */
    private fun recreateAnalyzer(settings: MonitorSettings) {
        val keywordAnalyzer = KeywordAnalyzer(
            keywordRules = settings.keywordRules,
            senderFilters = settings.senderFilters
        )

        val glmAnalyzer = if (settings.glmApiKey.isNotEmpty()) {
            val apiService = NetworkModule.createGLMApiService(settings.glmApiUrl)
            GLMAnalyzer(apiService, settings.glmApiKey)
        } else {
            null
        }

        messageAnalyzer = MessageAnalyzer(
            keywordAnalyzer = keywordAnalyzer,
            glmAnalyzer = glmAnalyzer
        )
    }

    /**
     * 清理旧消息
     */
    fun cleanOldMessages() {
        processorScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val retentionTime = System.currentTimeMillis() -
                    (settings.messageRetentionDays * 24 * 60 * 60 * 1000L)
            database.messageDao().deleteOldMessages(retentionTime)
            Log.d(TAG, "已清理 ${retentionTime} 之前的消息")
        }
    }
}
