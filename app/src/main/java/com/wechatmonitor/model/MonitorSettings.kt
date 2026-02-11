package com.wechatmonitor.model

import kotlinx.serialization.Serializable

/**
 * 监控设置
 */
@Serializable
data class MonitorSettings(
    // 监听的群组列表
    val monitoredGroups: Set<String> = emptySet(),

    // 关键词规则列表
    val keywordRules: List<KeywordRule> = emptyList(),

    // 发送者过滤
    val senderFilters: SenderFilters = SenderFilters(),

    // 分析模式
    val analysisMode: AnalysisMode = AnalysisMode.KEYWORD_ONLY,

    // 重要性阈值（0-1）
    val importanceThreshold: Float = 0.5f,

    // GLM API配置
    val glmApiKey: String = "",
    val glmApiUrl: String = "https://open.bigmodel.cn/api/paas/v4/chat/completions",

    // 每日摘要时间（HH:mm格式）
    val dailySummaryTime: String = "20:00",

    // 是否启用每日摘要
    val dailySummaryEnabled: Boolean = true,

    // 是否启用声音提醒
    val soundEnabled: Boolean = true,

    // 是否启用振动
    val vibrationEnabled: Boolean = true,

    // 消息保留天数
    val messageRetentionDays: Int = 7
)

/**
 * 关键词规则配置
 */
@Serializable
data class KeywordRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val keyword: String,
    val weight: Float = 0.5f,
    val isRegex: Boolean = false,
    val caseSensitive: Boolean = false,
    val enabled: Boolean = true
)

/**
 * 发送者过滤配置
 */
@Serializable
data class SenderFilters(
    val whitelist: Set<String> = emptySet(),  // 白名单：只关注这些人
    val blacklist: Set<String> = emptySet(),  // 黑名单：忽略这些人
    val mode: FilterMode = FilterMode.NONE    // 过滤模式
)

/**
 * 过滤模式
 */
@Serializable
enum class FilterMode {
    NONE,       // 不过滤，处理所有消息
    WHITELIST,  // 只处理白名单中的发送者
    BLACKLIST   // 处理除黑名单外的所有发送者
}
