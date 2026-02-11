package com.wechatmonitor.model

import java.time.LocalDate

/**
 * 每日摘要
 */
data class DailySummary(
    val date: LocalDate,
    val totalImportantMessages: Int,
    val groupSummaries: List<GroupSummary>
) {
    fun formatText(): String {
        return buildString {
            appendLine("📅 ${date} 重要消息摘要")
            appendLine("共 ${totalImportantMessages} 条重要消息\n")

            groupSummaries.forEach { summary ->
                appendLine("🏷️ ${summary.groupName}")
                appendLine("   ${summary.count} 条消息")
                summary.summary.takeIf { it.isNotEmpty() }?.let {
                    appendLine("   摘要: $it")
                }
                appendLine()
            }
        }
    }
}

/**
 * 群组摘要
 */
data class GroupSummary(
    val groupName: String,
    val count: Int,
    val summary: String
)
