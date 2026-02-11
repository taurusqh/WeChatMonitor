package com.wechatmonitor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wechatmonitor.model.AnalysisMethod

/**
 * 消息数据库实体
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val groupName: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isImportant: Boolean,
    val importanceScore: Float,
    val analysisMethod: String,  // 存储AnalysisMethod的name
    val hasNotified: Boolean
)
