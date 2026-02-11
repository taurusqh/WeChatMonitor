package com.wechatmonitor.repository

import android.content.Context
import com.wechatmonitor.model.AnalysisMode
import com.wechatmonitor.model.FilterMode
import com.wechatmonitor.model.KeywordRule
import com.wechatmonitor.model.MonitorSettings
import com.wechatmonitor.model.SenderFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * 设置仓库
 * 使用 DataStore 存储用户设置
 */
class SettingsRepository(context: Context) {

    private val preferences = context.getSharedPreferences(
        "wechat_monitor_settings",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // 设置缓存
    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow = _settingsFlow.asStateFlow()

    companion object {
        private const val KEY_MONITORED_GROUPS = "monitored_groups"
        private const val KEY_KEYWORD_RULES = "keyword_rules"
        private const val KEY_SENDER_FILTERS = "sender_filters"
        private const val KEY_ANALYSIS_MODE = "analysis_mode"
        private const val KEY_IMPORTANCE_THRESHOLD = "importance_threshold"
        private const val KEY_GLM_API_KEY = "glm_api_key"
        private const val KEY_GLM_API_URL = "glm_api_url"
        private const val KEY_DAILY_SUMMARY_TIME = "daily_summary_time"
        private const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_MESSAGE_RETENTION_DAYS = "message_retention_days"

        private const val DEFAULT_ANALYSIS_MODE = "KEYWORD_ONLY"
        private const val DEFAULT_IMPORTANCE_THRESHOLD = 0.5f
        private const val DEFAULT_DAILY_SUMMARY_TIME = "20:00"
        private const val DEFAULT_MESSAGE_RETENTION_DAYS = 7
    }

    /**
     * 获取设置
     */
    suspend fun getSettings(): MonitorSettings {
        return _settingsFlow.value
    }

    /**
     * 更新设置
     */
    suspend fun updateSettings(settings: MonitorSettings) {
        saveSettings(settings)
        _settingsFlow.value = settings
    }

    /**
     * 添加监听群组
     */
    suspend fun addMonitoredGroup(groupName: String) {
        val current = _settingsFlow.value
        val updated = current.copy(
            monitoredGroups = current.monitoredGroups + groupName
        )
        updateSettings(updated)
    }

    /**
     * 移除监听群组
     */
    suspend fun removeMonitoredGroup(groupName: String) {
        val current = _settingsFlow.value
        val updated = current.copy(
            monitoredGroups = current.monitoredGroups - groupName
        )
        updateSettings(updated)
    }

    /**
     * 添加关键词规则
     */
    suspend fun addKeywordRule(rule: KeywordRule) {
        val current = _settingsFlow.value
        val updated = current.copy(
            keywordRules = current.keywordRules + rule
        )
        updateSettings(updated)
    }

    /**
     * 移除关键词规则
     */
    suspend fun removeKeywordRule(ruleId: String) {
        val current = _settingsFlow.value
        val updated = current.copy(
            keywordRules = current.keywordRules.filterNot { it.id == ruleId }
        )
        updateSettings(updated)
    }

    /**
     * 更新关键词规则
     */
    suspend fun updateKeywordRule(rule: KeywordRule) {
        val current = _settingsFlow.value
        val updated = current.copy(
            keywordRules = current.keywordRules.map {
                if (it.id == rule.id) rule else it
            }
        )
        updateSettings(updated)
    }

    /**
     * 从SharedPreferences加载设置
     */
    private fun loadSettings(): MonitorSettings {
        return MonitorSettings(
            monitoredGroups = loadStringSet(KEY_MONITORED_GROUPS)
                ?.map { it } ?: emptySet(),
            keywordRules = loadKeywordRules(),
            senderFilters = loadSenderFilters(),
            analysisMode = loadAnalysisMode(),
            importanceThreshold = loadFloat(KEY_IMPORTANCE_THRESHOLD, DEFAULT_IMPORTANCE_THRESHOLD),
            glmApiKey = loadString(KEY_GLM_API_KEY, ""),
            glmApiUrl = loadString(KEY_GLM_API_URL, "https://open.bigmodel.cn/api/paas/v4/chat/completions"),
            dailySummaryTime = loadString(KEY_DAILY_SUMMARY_TIME, DEFAULT_DAILY_SUMMARY_TIME),
            dailySummaryEnabled = loadBoolean(KEY_DAILY_SUMMARY_ENABLED, true),
            soundEnabled = loadBoolean(KEY_SOUND_ENABLED, true),
            vibrationEnabled = loadBoolean(KEY_VIBRATION_ENABLED, true),
            messageRetentionDays = loadInt(KEY_MESSAGE_RETENTION_DAYS, DEFAULT_MESSAGE_RETENTION_DAYS)
        )
    }

    /**
     * 保存设置到SharedPreferences
     */
    private fun saveSettings(settings: MonitorSettings) {
        preferences.edit().apply {
            putStringSet(KEY_MONITORED_GROUPS, settings.monitoredGroups)
            putString(KEY_KEYWORD_RULES, json.encodeToString(settings.keywordRules))
            putString(KEY_SENDER_FILTERS, json.encodeToString(settings.senderFilters))
            putString(KEY_ANALYSIS_MODE, settings.analysisMode.name)
            putFloat(KEY_IMPORTANCE_THRESHOLD, settings.importanceThreshold)
            putString(KEY_GLM_API_KEY, settings.glmApiKey)
            putString(KEY_GLM_API_URL, settings.glmApiUrl)
            putString(KEY_DAILY_SUMMARY_TIME, settings.dailySummaryTime)
            putBoolean(KEY_DAILY_SUMMARY_ENABLED, settings.dailySummaryEnabled)
            putBoolean(KEY_SOUND_ENABLED, settings.soundEnabled)
            putBoolean(KEY_VIBRATION_ENABLED, settings.vibrationEnabled)
            putInt(KEY_MESSAGE_RETENTION_DAYS, settings.messageRetentionDays)
        }.apply()
    }

    private fun loadKeywordRules(): List<KeywordRule> {
        val jsonStr = loadString(KEY_KEYWORD_RULES, "[]")
        return try {
            json.decodeFromString<List<KeywordRule>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadSenderFilters(): SenderFilters {
        val jsonStr = loadString(KEY_SENDER_FILTERS, "{}")
        return try {
            json.decodeFromString<SenderFilters>(jsonStr)
        } catch (e: Exception) {
            SenderFilters()
        }
    }

    private fun loadAnalysisMode(): AnalysisMode {
        val modeStr = loadString(KEY_ANALYSIS_MODE, DEFAULT_ANALYSIS_MODE)
        return try {
            AnalysisMode.valueOf(modeStr)
        } catch (e: Exception) {
            AnalysisMode.KEYWORD_ONLY
        }
    }

    private fun loadString(key: String, defaultValue: String): String {
        return preferences.getString(key, defaultValue) ?: defaultValue
    }

    private fun loadStringSet(key: String): Set<String>? {
        return preferences.getStringSet(key, null)
    }

    private fun loadBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    private fun loadFloat(key: String, defaultValue: Float): Float {
        return preferences.getFloat(key, defaultValue)
    }

    private fun loadInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }
}
