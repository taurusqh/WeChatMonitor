package com.wechatmonitor.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wechatmonitor.database.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 消息数据访问对象
 */
@Dao
interface MessageDao {

    /**
     * 获取所有消息（按时间倒序）
     */
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    /**
     * 获取重要消息
     */
    @Query("SELECT * FROM messages WHERE isImportant = 1 ORDER BY timestamp DESC")
    fun getImportantMessages(): Flow<List<MessageEntity>>

    /**
     * 获取今天的重要消息
     */
    @Query("SELECT * FROM messages WHERE isImportant = 1 AND timestamp >= :startTime ORDER BY timestamp DESC")
    fun getTodayImportantMessages(startTime: Long): Flow<List<MessageEntity>>

    /**
     * 根据群组获取消息
     */
    @Query("SELECT * FROM messages WHERE groupName = :groupName ORDER BY timestamp DESC")
    fun getMessagesByGroup(groupName: String): Flow<List<MessageEntity>>

    /**
     * 根据群组获取重要消息
     */
    @Query("SELECT * FROM messages WHERE groupName = :groupName AND isImportant = 1 ORDER BY timestamp DESC")
    fun getImportantMessagesByGroup(groupName: String): Flow<List<MessageEntity>>

    /**
     * 插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    /**
     * 批量插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    /**
     * 删除指定时间之前的消息
     */
    @Query("DELETE FROM messages WHERE timestamp < :olderThan")
    suspend fun deleteOldMessages(olderThan: Long)

    /**
     * 删除指定消息
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    /**
     * 清空所有消息
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    /**
     * 获取消息数量
     */
    @Query("SELECT COUNT(*) FROM messages")
    fun getMessageCount(): Flow<Int>

    /**
     * 获取重要消息数量
     */
    @Query("SELECT COUNT(*) FROM messages WHERE isImportant = 1")
    fun getImportantMessageCount(): Flow<Int>
}
