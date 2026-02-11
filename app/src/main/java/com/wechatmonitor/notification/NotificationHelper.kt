package com.wechatmonitor.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wechatmonitor.MainActivity
import com.wechatmonitor.R
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.DailySummary

/**
 * 通知助手
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "wechat_monitor_channel"
        const val DAILY_SUMMARY_NOTIFICATION_ID = 99999
    }

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示重要消息通知
     */
    fun showImportantMessageNotification(message: ChatMessage) {
        // 点击通知打开应用
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${message.groupName} ${context.getString(R.string.notification_important_message)}")
            .setContentText("${message.senderName}: ${message.content.take(50)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${message.senderName}: ${message.content}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setShowWhen(true)
            .build()

        notificationManager.notify(message.id.hashCode(), notification)
    }

    /**
     * 显示每日摘要通知
     */
    fun showDailySummary(summary: DailySummary) {
        // 创建分享Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "每日消息摘要 - ${summary.date}")
            putExtra(Intent.EXTRA_TEXT, summary.formatText())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val sharePendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent.createChooser(shareIntent, context.getString(R.string.notification_share)),
            PendingIntent.FLAG_IMMUTABLE
        )

        // 点击通知打开应用
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            DAILY_SUMMARY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_daily_summary))
            .setContentText("共 ${summary.totalImportantMessages} 条重要消息")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(summary.formatText())
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_share,
                context.getString(R.string.notification_share),
                sharePendingIntent
            )
            .build()

        notificationManager.notify(DAILY_SUMMARY_NOTIFICATION_ID, notification)
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
