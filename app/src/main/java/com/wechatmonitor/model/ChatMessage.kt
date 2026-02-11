package com.wechatmonitor.model

import java.util.UUID

/**
 * 聊天消息数据模型
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val groupName: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isImportant: Boolean = false,
    val importanceScore: Float = 0f,
    val analysisMethod: AnalysisMethod = AnalysisMethod.NONE,
    val hasNotified: Boolean = false
)

/**
 * 分析方法枚举
 */
enum class AnalysisMethod {
    NONE,       // 未分析
    KEYWORD,    // 关键词分析
    GLM,        // GLM AI分析
    BOTH        // 两者结合
}

/**
 * 分析模式设置（用户选择使用哪种分析方式）
 */
enum class AnalysisMode {
    KEYWORD_ONLY,   // 仅关键词
    GLM_ONLY,       // 仅GLM
    BOTH            // 两者都使用
}

/**
 * 分析结果
 */
data class AnalysisResult(
    val isImportant: Boolean,
    val score: Float,
    val method: String,
    val matchedKeywords: List<String> = emptyList(),
    val reason: String = ""
)
