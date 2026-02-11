package com.wechatmonitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.wechatmonitor.database.AppDatabase

/**
 * Application 类
 */
class WeChatMonitorApplication : Application() {

    companion object {
        lateinit var instance: WeChatMonitorApplication
            private set

        const val CHANNEL_ID = "wechat_monitor_channel"
        const val CHANNEL_NAME = "重要消息提醒"
        const val CHANNEL_DESCRIPTION = "微信群重要消息通知"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化数据库
        AppDatabase.getInstance(this)

        // 创建通知渠道
        createNotificationChannel()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
