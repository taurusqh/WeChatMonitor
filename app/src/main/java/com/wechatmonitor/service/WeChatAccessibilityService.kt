package com.wechatmonitor.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.wechatmonitor.database.AppDatabase
import com.wechatmonitor.model.AnalysisMethod
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.MonitorSettings
import com.wechatmonitor.notification.NotificationHelper
import com.wechatmonitor.notification.SoundPlayer
import com.wechatmonitor.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 微信无障碍服务
 * 监听微信群消息并分析重要性
 */
class WeChatAccessibilityService : AccessibilityService() {

    companion object {
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        private const val DELAY_BETWEEN_MESSAGES = 500L // 消息处理间隔(ms)

        // 当前群名缓存，避免重复解析
        private var currentGroupName: String? = null

        // 已处理消息ID缓存，避免重复处理
        private val processedMessageIds = mutableSetOf<String>()
        private const val MAX_CACHE_SIZE = 1000
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var database: AppDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var messageProcessor: MessageProcessor

    private var lastProcessTime = 0L
    private var isServiceEnabled = false

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        soundPlayer = SoundPlayer(applicationContext)
        messageProcessor = MessageProcessor(
            database = database,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            soundPlayer = soundPlayer
        )

        // 监听设置变化
        serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                isServiceEnabled = settings.monitoredGroups.isNotEmpty()
                currentGroupName = null // 清除群名缓存
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // 检查服务是否启用
        if (!isServiceEnabled) return

        // 检查是否是微信事件
        if (event.packageName?.toString() != WECHAT_PACKAGE) return

        // 限流：避免处理过快
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < DELAY_BETWEEN_MESSAGES) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotificationEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChangedEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowChangedEvent(event)
            }
        }
    }

    /**
     * 处理通知事件
     */
    private fun handleNotificationEvent(event: AccessibilityEvent) {
        val text = event.text?.joinToString("") ?: return
        val groupName = extractGroupNameFromNotification(text)

        if (groupName != null && shouldMonitorGroup(groupName)) {
            // 通知事件内容可能不完整，标记需要稍后处理
            serviceScope.launch {
                delay(2000) // 等待消息在聊天界面完全加载
                processCurrentChatMessage(groupName)
            }
        }
    }

    /**
     * 处理文本变化事件
     */
    private fun handleTextChangedEvent(event: AccessibilityEvent) {
        val text = event.text?.joinToString("") ?: return

        // 检查是否是新消息
        if (isNewMessage(text)) {
            val groupName = getCurrentGroupName()
            if (groupName != null && shouldMonitorGroup(groupName)) {
                parseAndProcessMessage(text, groupName)
            }
        }
    }

    /**
     * 处理窗口变化事件
     */
    private fun handleWindowChangedEvent(event: AccessibilityEvent) {
        // 清除当前群名缓存
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentGroupName = null
        }
    }

    /**
     * 解析并处理消息
     */
    private fun parseAndProcessMessage(text: String, groupName: String) {
        serviceScope.launch {
            val message = parseMessage(text, groupName) ?: return@launch

            // 生成唯一ID检查是否已处理
            val messageId = generateMessageId(message)
            if (messageId in processedMessageIds) {
                return@launch
            }

            // 清理过期的缓存
            if (processedMessageIds.size > MAX_CACHE_SIZE) {
                processedMessageIds.clear()
            }
            processedMessageIds.add(messageId)

            // 处理消息
            messageProcessor.processMessage(message)
            lastProcessTime = System.currentTimeMillis()
        }
    }

    /**
     * 从通知中提取群名
     */
    private fun extractGroupNameFromNotification(text: String): String? {
        // 微信群通知格式通常是: "群名: 发送者: 消息内容"
        val colonIndex = text.indexOf(':')
        if (colonIndex > 0) {
            return text.substring(0, colonIndex).trim()
        }
        return null
    }

    /**
     * 判断是否是新消息
     */
    private fun isNewMessage(text: String): Boolean {
        // 简单判断：包含冒号分隔的格式
        return text.contains(":") && text.length > 5
    }

    /**
     * 解析消息内容
     */
    private fun parseMessage(text: String, groupName: String): ChatMessage? {
        // 微信消息格式: "发送者: 消息内容"
        val colonIndex = text.indexOf(':')
        if (colonIndex <= 0) return null

        val senderName = text.substring(0, colonIndex).trim()
        val content = text.substring(colonIndex + 1).trim()

        if (content.isEmpty()) return null

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            groupName = groupName,
            senderName = senderName,
            content = content,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 生成消息ID用于去重
     */
    private fun generateMessageId(message: ChatMessage): String {
        return "${message.groupName}_${message.senderName}_${message.content}_${message.timestamp / 1000}"
    }

    /**
     * 获取当前群名
     */
    private fun getCurrentGroupName(): String? {
        // 如果有缓存，直接返回
        if (currentGroupName != null) {
            return currentGroupName
        }

        // 从无障碍节点树中获取群名
        val rootNode = rootInActiveWindow ?: return null
        val groupName = findGroupNameInNodeTree(rootNode)
        if (groupName != null) {
            currentGroupName = groupName
        }
        return groupName
    }

    /**
     * 在节点树中查找群名
     */
    private fun findGroupNameInNodeTree(node: AccessibilityNodeInfo): String? {
        // 群名通常在顶部的TextView中
        if (node.className?.toString()?.contains("TextView") == true) {
            val text = node.text?.toString()
            if (text != null && text.length > 2 && text.length < 50) {
                // 检查是否是已知的群名
                serviceScope.launch {
                    val settings = settingsRepository.getSettings()
                    if (text in settings.monitoredGroups) {
                        currentGroupName = text
                    }
                }
            }
        }

        // 递归查找子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findGroupNameInNodeTree(child)
            if (result != null) return result
        }

        return null
    }

    /**
     * 判断是否需要监听该群
     */
    private fun shouldMonitorGroup(groupName: String): Boolean {
        return serviceScope.launch {
            val settings = settingsRepository.getSettings()
            groupName in settings.monitoredGroups
        }.let { true } // 简化处理，实际应该用Flow监听
    }

    /**
     * 处理当前聊天消息（从通知延迟后调用）
     */
    private fun processCurrentChatMessage(groupName: String) {
        val rootNode = rootInActiveWindow ?: return

        // 查找最新的消息节点
        val messages = findMessageNodes(rootNode)
        messages.lastOrNull()?.let { lastMessage ->
            val text = lastMessage.text?.toString() ?: return@let
            if (isNewMessage(text)) {
                parseAndProcessMessage(text, groupName)
            }
        }
    }

    /**
     * 查找所有消息节点
     */
    private fun findMessageNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()

        if (node.className?.toString()?.contains("TextView") == true) {
            val text = node.text?.toString()
            if (text != null && text.contains(":")) {
                result.add(node)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            result.addAll(findMessageNodes(child))
        }

        return result
    }

    override fun onInterrupt() {
        // 服务被中断
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.release()
        processedMessageIds.clear()
        currentGroupName = null
    }
}
