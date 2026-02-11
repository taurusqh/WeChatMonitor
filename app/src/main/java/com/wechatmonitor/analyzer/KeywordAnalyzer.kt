package com.wechatmonitor.analyzer

import com.wechatmonitor.model.AnalysisResult
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.FilterMode
import com.wechatmonitor.model.KeywordRule
import com.wechatmonitor.model.SenderFilters
import java.util.regex.Pattern

/**
 * 关键词分析器
 * 支持正则表达式匹配和发送者过滤
 */
class KeywordAnalyzer(
    private val keywordRules: List<KeywordRule>,
    private val senderFilters: SenderFilters
) {

    companion object {
        private const val IMPORTANCE_THRESHOLD = 0.5f
        private const val WHITELIST_BONUS = 0.3f
    }

    /**
     * 编译后的规则缓存
     */
    private val compiledRules = keywordRules.map { rule ->
        CompiledRule(
            rule = rule,
            pattern = compilePattern(rule)
        )
    }

    /**
     * 分析消息重要性
     */
    fun analyze(message: ChatMessage): AnalysisResult {
        // 首先检查发送者过滤
        if (!shouldProcessSender(message.senderName)) {
            return AnalysisResult(
                isImportant = false,
                score = 0f,
                method = "发送者过滤",
                reason = getFilterReason(message.senderName)
            )
        }

        var score = 0f
        val matchedRules = mutableListOf<String>()
        val matchedTexts = mutableListOf<String>()

        // 遍历所有规则进行匹配
        for (compiledRule in compiledRules) {
            val rule = compiledRule.rule
            if (!rule.enabled) continue

            val matcher = compiledRule.pattern?.matcher(message.content)
            if (matcher != null && matcher.find()) {
                score += rule.weight
                matchedRules.add(rule.keyword)
                matchedTexts.add(matcher.group())
            }
        }

        // 发送者白名单加分
        val isWhitelisted = senderFilters.mode == FilterMode.WHITELIST &&
                senderFilters.whitelist.contains(message.senderName)
        if (isWhitelisted) {
            score += WHITELIST_BONUS
        }

        // 限制分数范围
        score = score.coerceIn(0f, 1f)

        return AnalysisResult(
            isImportant = score >= IMPORTANCE_THRESHOLD,
            score = score,
            method = "关键词匹配",
            matchedKeywords = matchedRules,
            reason = buildReason(matchedRules, matchedTexts, isWhitelisted)
        )
    }

    /**
     * 判断是否应该处理该发送者的消息
     */
    private fun shouldProcessSender(sender: String): Boolean {
        return when (senderFilters.mode) {
            FilterMode.NONE -> true
            FilterMode.WHITELIST -> senderFilters.whitelist.contains(sender)
            FilterMode.BLACKLIST -> !senderFilters.blacklist.contains(sender)
        }
    }

    /**
     * 获取过滤原因
     */
    private fun getFilterReason(sender: String): String {
        return when (senderFilters.mode) {
            FilterMode.WHITELIST -> "发送者不在白名单中: $sender"
            FilterMode.BLACKLIST -> "发送者在黑名单中: $sender"
            FilterMode.NONE -> "未知原因"
        }
    }

    /**
     * 编译正则表达式
     */
    private fun compilePattern(rule: KeywordRule): Pattern? {
        return try {
            val pattern = if (rule.isRegex) {
                rule.keyword
            } else {
                Pattern.quote(rule.keyword)
            }
            val flags = if (rule.caseSensitive) 0 else Pattern.CASE_INSENSITIVE
            Pattern.compile(pattern, flags)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 构建分析原因
     */
    private fun buildReason(
        matchedRules: List<String>,
        matchedTexts: List<String>,
        isWhitelisted: Boolean
    ): String {
        return buildString {
            if (matchedRules.isNotEmpty()) {
                append("匹配: ")
                matchedRules.forEachIndexed { index, rule ->
                    if (index > 0) append(", ")
                    append(rule)
                }
            }
            if (isWhitelisted) {
                if (isNotEmpty()) append(" + ")
                append("白名单加分")
            }
        }
    }

    /**
     * 编译后的规则
     */
    private data class CompiledRule(
        val rule: KeywordRule,
        val pattern: Pattern?
    )
}
