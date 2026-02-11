package com.wechatmonitor.analyzer

import com.wechatmonitor.model.AnalysisResult
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.DailySummary
import com.wechatmonitor.model.GroupSummary
import com.wechatmonitor.network.GLMAnalysisResult
import com.wechatmonitor.network.GLMMessage
import com.wechatmonitor.network.GLMRequest
import com.wechatmonitor.network.GLMResponse
import com.wechatmonitor.network.GLMApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * GLM 大模型分析器
 * 使用智谱AI GLM-4-Flash模型进行消息分析
 */
class GLMAnalyzer(
    private val apiService: GLMApiService,
    private val apiKey: String
) {

    companion object {
        private const val MODEL = "glm-4-flash"
        private const val IMPORTANCE_THRESHOLD = 0.5f

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    /**
     * 分析单条消息的重要性
     */
    suspend fun analyze(message: ChatMessage): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            val prompt = buildAnalysisPrompt(message)
            val request = GLMRequest(
                model = MODEL,
                messages = listOf(
                    GLMMessage(role = "system", content = getSystemPrompt()),
                    GLMMessage(role = "user", content = prompt)
                ),
                temperature = 0.3f,
                max_tokens = 512
            )

            val response = apiService.analyzeMessage(
                authorization = "Bearer $apiKey",
                request = request
            )

            parseAnalysisResponse(response)
        } catch (e: Exception) {
            AnalysisResult(
                isImportant = false,
                score = 0f,
                method = "GLM分析",
                reason = "分析失败: ${e.message}"
            )
        }
    }

    /**
     * 生成每日摘要
     */
    suspend fun generateSummary(
        messages: List<ChatMessage>,
        date: LocalDate = LocalDate.now()
    ): Result<DailySummary> = withContext(Dispatchers.IO) {
        try {
            if (messages.isEmpty()) {
                return@withContext Result.success(
                    DailySummary(date, 0, emptyList())
                )
            }

            val prompt = buildSummaryPrompt(messages)
            val request = GLMRequest(
                model = MODEL,
                messages = listOf(
                    GLMMessage(role = "system", content = getSummarySystemPrompt()),
                    GLMMessage(role = "user", content = prompt)
                ),
                temperature = 0.5f,
                max_tokens = 2048
            )

            val response = apiService.generateSummary(
                authorization = "Bearer $apiKey",
                request = request
            )

            val summary = parseSummaryResponse(response, messages, date)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 构建分析提示词
     */
    private fun buildAnalysisPrompt(message: ChatMessage): String {
        return """
            请分析以下微信群消息的重要性：

            群名：${message.groupName}
            发送者：${message.senderName}
            内容：${message.content}

            判断标准：
            - 工作相关、紧急事项、@消息、任务指派 重要性高
            - 会议通知、重要公告 重要性高
            - 闲聊、表情包、普通问候 重要性低
            - 广告、推广信息 重要性低

            请只返回JSON格式的分析结果，格式如下：
            {"isImportant": true/false, "score": 0.0-1.0, "reason": "简短原因"}
        """.trimIndent()
    }

    /**
     * 构建摘要提示词
     */
    private fun buildSummaryPrompt(messages: List<ChatMessage>): String {
        // 按群分组
        val groupedMessages = messages.groupBy { it.groupName }

        return buildString {
            appendLine("请为以下微信群重要消息生成每日摘要：")
            appendLine()

            groupedMessages.forEach { (groupName, msgs) ->
                appendLine("【$groupName】(${msgs.size}条)")
                msgs.forEach { msg ->
                    appendLine("  ${msg.senderName}: ${msg.content}")
                }
                appendLine()
            }

            appendLine("请按群分类总结，每个群生成简短的摘要（30字以内）。")
            appendLine("请只返回JSON格式：")
            appendLine("""{"groups": [{"groupName": "群名", "summary": "摘要内容"}, ...]}""")
        }
    }

    /**
     * 获取分析系统提示词
     */
    private fun getSystemPrompt(): String {
        return """
            你是一个专业的消息分析助手，负责判断微信群消息的重要性。
            你需要根据消息内容、发送者、群组上下文等因素，给出客观的分析。
            请严格按照JSON格式返回结果。
        """.trimIndent()
    }

    /**
     * 获取摘要系统提示词
     */
    private fun getSummarySystemPrompt(): String {
        return """
            你是一个专业的消息摘要助手，负责为微信群重要消息生成每日摘要。
            你需要简洁明了地总结每条消息的核心内容，按群分类。
            请严格按照JSON格式返回结果。
        """.trimIndent()
    }

    /**
     * 解析分析响应
     */
    private fun parseAnalysisResponse(response: GLMResponse): AnalysisResult {
        return try {
            val content = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalArgumentException("Empty response")

            // 提取JSON内容
            val jsonContent = extractJson(content)
            val result = json.decodeFromString<GLMAnalysisResult>(jsonContent)

            AnalysisResult(
                isImportant = result.isImportant,
                score = result.score.coerceIn(0f, 1f),
                method = "GLM分析",
                reason = result.reason
            )
        } catch (e: Exception) {
            AnalysisResult(
                isImportant = false,
                score = 0f,
                method = "GLM分析",
                reason = "解析失败: ${e.message}"
            )
        }
    }

    /**
     * 解析摘要响应
     */
    private fun parseSummaryResponse(
        response: GLMResponse,
        messages: List<ChatMessage>,
        date: LocalDate
    ): DailySummary {
        return try {
            val content = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalArgumentException("Empty response")

            val jsonContent = extractJson(content)
            val summaryData = json.decodeFromString<SummaryData>(jsonContent)

            val groupSummaries = summaryData.groups.map { groupSummary ->
                GroupSummary(
                    groupName = groupSummary.groupName,
                    count = messages.count { it.groupName == groupSummary.groupName },
                    summary = groupSummary.summary
                )
            }

            DailySummary(
                date = date,
                totalImportantMessages = messages.size,
                groupSummaries = groupSummaries
            )
        } catch (e: Exception) {
            // 解析失败时返回简单的分组统计
            val grouped = messages.groupBy { it.groupName }
            DailySummary(
                date = date,
                totalImportantMessages = messages.size,
                groupSummaries = grouped.map { (groupName, msgs) ->
                    GroupSummary(
                        groupName = groupName,
                        count = msgs.size,
                        summary = "摘要生成失败"
                    )
                }
            )
        }
    }

    /**
     * 从响应中提取JSON内容
     */
    private fun extractJson(content: String): String {
        // 查找JSON对象的开始和结束
        val startIndex = content.indexOf('{')
        val endIndex = content.lastIndexOf('}')

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return content.substring(startIndex, endIndex + 1)
        }

        return content
    }

    /**
     * 摘要数据结构
     */
    @kotlinx.serialization.Serializable
    private data class SummaryData(
        val groups: List<GroupSummaryData>
    )

    @kotlinx.serialization.Serializable
    private data class GroupSummaryData(
        val groupName: String,
        val summary: String
    )
}
