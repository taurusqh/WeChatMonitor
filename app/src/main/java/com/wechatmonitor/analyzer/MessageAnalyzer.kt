package com.wechatmonitor.analyzer

import com.wechatmonitor.model.AnalysisMode
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.MonitorSettings

/**
 * 综合消息分析器
 * 根据设置协调关键词分析和GLM分析
 */
class MessageAnalyzer(
    private val keywordAnalyzer: KeywordAnalyzer,
    private val glmAnalyzer: GLMAnalyzer?
) {

    /**
     * 分析消息
     */
    suspend fun analyze(
        message: ChatMessage,
        settings: MonitorSettings
    ): ChatMessage {
        val analysisResult = when (settings.analysisMode) {
            AnalysisMode.KEYWORD_ONLY -> {
                keywordAnalyzer.analyze(message)
            }
            AnalysisMode.GLM_ONLY -> {
                if (glmAnalyzer != null && settings.glmApiKey.isNotEmpty()) {
                    glmAnalyzer.analyze(message)
                } else {
                    // GLM不可用时回退到关键词分析
                    keywordAnalyzer.analyze(message)
                }
            }
            AnalysisMode.BOTH -> {
                // 两种方式都使用，取较高分数
                val keywordResult = keywordAnalyzer.analyze(message)

                var glmScore = 0f
                var glmReason = ""

                if (glmAnalyzer != null && settings.glmApiKey.isNotEmpty()) {
                    val glmResult = glmAnalyzer.analyze(message)
                    glmScore = glmResult.score
                    glmReason = glmResult.reason
                }

                // 取两者中较高的分数
                val finalScore = maxOf(keywordResult.score, glmScore)
                val finalReason = if (keywordResult.score >= glmScore) {
                    "关键词: ${keywordResult.reason}"
                } else {
                    "GLM: $glmReason"
                }

                keywordResult.copy(
                    score = finalScore.coerceAtMost(1f),
                    reason = finalReason
                )
            }
        }

        return message.copy(
            isImportant = analysisResult.isImportant &&
                    analysisResult.score >= settings.importanceThreshold,
            importanceScore = analysisResult.score.coerceAtMost(1f),
            analysisMethod = when (settings.analysisMode) {
                AnalysisMode.KEYWORD_ONLY -> com.wechatmonitor.model.AnalysisMethod.KEYWORD
                AnalysisMode.GLM_ONLY -> com.wechatmonitor.model.AnalysisMethod.GLM
                AnalysisMode.BOTH -> com.wechatmonitor.model.AnalysisMethod.BOTH
            }
        )
    }

    /**
     * 更新分析器配置
     */
    fun updateConfig(settings: MonitorSettings) {
        // 分析器会根据传入的settings进行分析
        // 这里可以做一些预热或缓存清理工作
    }
}
