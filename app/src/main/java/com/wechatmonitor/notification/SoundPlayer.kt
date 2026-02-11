package com.wechatmonitor.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.wechatmonitor.R

/**
 * 声音和震动播放器
 */
class SoundPlayer(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var importantAlertSoundId: Int = 0
    private var isLoaded = false

    private val vibrator: Vibrator? = getVibrator()

    init {
        initSoundPool()
    }

    /**
     * 初始化SoundPool
     */
    private fun initSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, _, _ ->
                isLoaded = true
            }

            // 加载提示音（如果没有自定义声音，使用系统默认）
            importantAlertSoundId = soundPool?.load(context, R.raw.important_alert, 1) ?: 0
        }
    }

    /**
     * 获取震动器
     */
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * 播放重要消息提示音
     */
    fun playImportantAlert() {
        if (!isLoaded) return

        soundPool?.play(
            importantAlertSoundId,
            1f,  // 左音量
            1f,  // 右音量
            0,   // 优先级
            0,   // 循环（0 = 不循环）
            1f   // 播放速度
        )
    }

    /**
     * 播放震动
     */
    fun playVibrate() {
        val vibrator = vibrator ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 震动模式：长-短-长
            val timings = longArrayOf(0, 300, 200, 100, 200)
            val amplitudes = intArrayOf(0, 255, 0, 128, 0)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 300, 200, 100, 200), -1)
        }
    }

    /**
     * 播放提示音和震动
     */
    fun playAlert() {
        playImportantAlert()
        playVibrate()
    }

    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded = false
    }
}
