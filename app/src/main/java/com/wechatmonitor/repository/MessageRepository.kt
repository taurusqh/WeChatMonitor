package com.wechatmonitor.repository

import com.wechatmonitor.database.AppDatabase
import com.wechatmonitor.database.entities.MessageEntity
import com.wechatmonitor.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 消息仓库
 */
class MessageRepository(private val database: AppDatabase) {

    /**
     * 获取所有消息
     */
    fun getAllMessages(): Flow<List<ChatMessage>> {
        return database.messageDao().getAllMessages()
            .map { entities -> entities.map { it.toChatMessage() } }
    }

    /**
     * 获取重要消息
     */
    fun getImportantMessages(): Flow<List<ChatMessage>> {
        return database.messageDao().getImportantMessages()
            .map { entities -> entities.map { it.toChatMessage() } }
    }

    /**
     * 获取今天的重要消息
     */
    fun getTodayImportantMessages(): Flow<List<ChatMessage>> {
        val startTime = getTodayStartTime()
        return database.messageDao().getTodayImportantMessages(startTime)
            .map { entities -> entities.map { it.toChatMessage() } }
    }

    /**
     * 根据群组获取消息
     */
    fun getMessagesByGroup(groupName: String): Flow<List<ChatMessage>> {
        return database.messageDao().getMessagesByGroup(groupName)
            .map { entities -> entities.map { it.toChatMessage() } }
    }

    /**
     * 根据群组获取重要消息
     */
    fun getImportantMessagesByGroup(groupName: String): Flow<List<ChatMessage>> {
        return database.messageDao().getImportantMessagesByGroup(groupName)
            .map { entities -> entities.map { it.toChatMessage() } }
    }

    /**
     * 插入消息
     */
    suspend fun insertMessage(message: ChatMessage) {
        database.messageDao().insert(message.toMessageEntity())
    }

    /**
     * 批量插入消息
     */
    suspend fun insertMessages(messages: List<ChatMessage>) {
        database.messageDao().insertAll(messages.map { it.toMessageEntity() })
    }

    /**
     * 删除旧消息
     */
    suspend fun deleteOldMessages(olderThan: Long) {
        database.messageDao().deleteOldMessages(olderThan)
    }

    /**
     * 删除指定消息
     */
    suspend fun deleteMessage(messageId: String) {
        database.messageDao().deleteMessage(messageId)
    }

    /**
     * 清空所有消息
     */
    suspend fun deleteAllMessages() {
        database.messageDao().deleteAllMessages()
    }

    /**
     * 获取消息数量
     */
    fun getMessageCount(): Flow<Int> {
        return database.messageDao().getMessageCount()
    }

    /**
     * 获取重要消息数量
     */
    fun getImportantMessageCount(): Flow<Int> {
        return database.messageDao().getImportantMessageCount()
    }

    /**
     * 获取今天的开始时间
     */
    private fun getTodayStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

/**
 * MessageEntity 转 ChatMessage
 */
private fun MessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        groupName = groupName,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        isImportant = isImportant,
        importanceScore = importanceScore,
        analysisMethod = com.wechatmonitor.model.AnalysisMethod.valueOf(analysisMethod),
        hasNotified = hasNotified
    )
}

/**
 * ChatMessage 转 MessageEntity
 */
private fun ChatMessage.toMessageEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        groupName = groupName,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        isImportant = isImportant,
        importanceScore = importanceScore,
        analysisMethod = analysisMethod.name,
        hasNotified = hasNotified
    )
}
