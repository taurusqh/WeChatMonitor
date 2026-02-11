package com.wechatmonitor.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityServiceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wechatmonitor.database.AppDatabase
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.repository.MessageRepository
import com.wechatmonitor.repository.SettingsRepository
import com.wechatmonitor.utils.Scheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主界面 ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val messageRepository = MessageRepository(database)
    private val settingsRepository = SettingsRepository(application)
    private val scheduler = Scheduler(application)

    // UI 状态
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // 消息列表
    val messages = messageRepository.getAllMessages()
    val importantMessages = messageRepository.getImportantMessages()
    val settings = settingsRepository.settingsFlow

    init {
        loadSettings()
        scheduleDailySummary()
    }

    /**
     * 加载设置
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    monitoredGroups = settings.monitoredGroups.toList(),
                    dailySummaryEnabled = settings.dailySummaryEnabled,
                    dailySummaryTime = settings.dailySummaryTime
                )
            }
        }
    }

    /**
     * 检查无障碍权限
     */
    fun checkAccessibilityPermission() {
        val accessibilityManager = getApplication<Application>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        val isEnabled = accessibilityManager.isEnabled
        val weChatServiceEnabled = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { it.id.contains("WeChatAccessibilityService") }

        _uiState.value = _uiState.value.copy(
            accessibilityEnabled = weChatServiceEnabled
        )
    }

    /**
     * 打开无障碍设置
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    /**
     * 添加监听群组
     */
    fun addMonitoredGroup(groupName: String) {
        viewModelScope.launch {
            settingsRepository.addMonitoredGroup(groupName)
        }
    }

    /**
     * 移除监听群组
     */
    fun removeMonitoredGroup(groupName: String) {
        viewModelScope.launch {
            settingsRepository.removeMonitoredGroup(groupName)
        }
    }

    /**
     * 更新设置
     */
    fun updateSettings(
        dailySummaryEnabled: Boolean? = null,
        dailySummaryTime: String? = null
    ) {
        viewModelScope.launch {
            val current = settingsRepository.settingsFlow.value
            val updated = current.copy(
                dailySummaryEnabled = dailySummaryEnabled ?: current.dailySummaryEnabled,
                dailySummaryTime = dailySummaryTime ?: current.dailySummaryTime
            )
            settingsRepository.updateSettings(updated)

            // 重新调度摘要任务
            if (dailySummaryEnabled == true || dailySummaryTime != null) {
                scheduleDailySummary()
            }
        }
    }

    /**
     * 调度每日摘要任务
     */
    private fun scheduleDailySummary() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.value
            if (settings.dailySummaryEnabled) {
                scheduler.scheduleDailySummary(settings.dailySummaryTime)
            } else {
                scheduler.cancelDailySummary()
            }
        }
    }

    /**
     * 清除旧消息
     */
    fun clearOldMessages() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.value
            val retentionTime = System.currentTimeMillis() -
                    (settings.messageRetentionDays * 24 * 60 * 60 * 1000L)
            messageRepository.deleteOldMessages(retentionTime)
        }
    }

    /**
     * 清除所有消息
     */
    fun clearAllMessages() {
        viewModelScope.launch {
            messageRepository.deleteAllMessages()
        }
    }

    companion object {
        fun create(context: Context): MainViewModel {
            // 这里应该通过依赖注入创建，简化处理
            return MainViewModel(context.applicationContext as Application)
        }
    }
}

/**
 * 主界面UI状态
 */
data class MainUiState(
    val accessibilityEnabled: Boolean = false,
    val monitoredGroups: List<String> = emptyList(),
    val dailySummaryEnabled: Boolean = true,
    val dailySummaryTime: String = "20:00",
    val isLoading: Boolean = false,
    val error: String? = null
)
