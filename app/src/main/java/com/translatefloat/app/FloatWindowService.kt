package com.translatefloat.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var translateApi: TranslateApi
    private lateinit var obsidianHelper: ObsidianHelper

    private var floatBallView: View? = null
    private var translationPanelView: View? = null
    private var backgroundView: View? = null
    private var floatBallParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var startX = 0f
    private var startY = 0f
    private var initialX = 0
    private var initialY = 0

    private var currentOriginalText = ""
    private var currentTranslatedText = ""
    private var isPanelLoading = false

    companion object {
        private const val CHANNEL_ID = "translate_float_channel"
        private const val NOTIFICATION_ID = 1
        var isRunning = false
        
        // 存储剪贴板内容的静态变量（主界面和悬浮窗共享）
        var lastClipboardText: String = ""
        
        // 存储用户选中的文字（通过 AccessibilityService）
        var lastSelectedText: String = ""
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsManager = SettingsManager(this)
        translateApi = TranslateApi()
        obsidianHelper = ObsidianHelper(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true
        
        Handler(Looper.getMainLooper()).postDelayed({
            showFloatBall()
        }, 500)
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeFloatWindow()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "悬浮窗翻译", NotificationManager.IMPORTANCE_LOW).apply {
                description = "悬浮窗翻译服务运行中"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // 点击通知会打开应用
        val pendingIntent = PendingIntent.getActivity(
            this, 0, 
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TranslateFloat")
            .setContentText("悬浮窗已启动，点击可打开主界面")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFloatBall() {
        if (floatBallView != null) return

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(56), dpToPx(56))
            setBackgroundColor(0xFF6366F1.toInt())
        }

        container.addView(TextView(this).apply {
            text = "译"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })

        floatBallParams = WindowManager.LayoutParams(
            dpToPx(56), dpToPx(56), getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dpToPx(72)
            y = resources.displayMetrics.heightPixels / 2 - dpToPx(28)
        }

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX; startY = event.rawY
                    initialX = floatBallParams?.x ?: 0; initialY = floatBallParams?.y ?: 0
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - startX).toInt()
                    val deltaY = (event.rawY - startY).toInt()
                    if (Math.abs(deltaX) > 15 || Math.abs(deltaY) > 15) {
                        floatBallParams?.x = initialX + deltaX
                        floatBallParams?.y = initialY + deltaY
                        floatBallView?.let { windowManager.updateViewLayout(it, floatBallParams) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(event.rawX - startX) < 15 && Math.abs(event.rawY - startY) < 15) {
                        onFloatBallClick()
                    }
                    true
                }
                else -> true
            }
        }

        floatBallView = container
        try {
            windowManager.addView(floatBallView, floatBallParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun onFloatBallClick() {
        // 优先使用用户选中的文字（通过 AccessibilityService）
        var textToTranslate: String? = null
        
        // 方法1: 使用用户选中的文字
        if (lastSelectedText.isNotBlank()) {
            textToTranslate = lastSelectedText
            lastSelectedText = "" // 使用后清除
        }
        
        // 方法2: 尝试读取剪贴板
        if (textToTranslate.isNullOrBlank()) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                textToTranslate = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            } catch (e: Exception) {
                // 忽略
            }
        }
        
        // 方法3: 使用主界面保存的最后剪贴板内容
        if (textToTranslate.isNullOrBlank() && lastClipboardText.isNotBlank()) {
            textToTranslate = lastClipboardText
        }
        
        if (textToTranslate.isNullOrBlank()) {
            showToast("请先复制或选中要翻译的文字")
            return
        }
        
        currentOriginalText = textToTranslate
        isPanelLoading = true
        showTranslationPanel()
        
        // 执行翻译
        Thread {
            try {
                val translated = translateApi.translate(textToTranslate, settingsManager.targetLang)
                currentTranslatedText = translated
                settingsManager.addToHistory(textToTranslate, translated)
                Handler(Looper.getMainLooper()).post {
                    isPanelLoading = false
                    updateTranslationPanel()
                    showToast("翻译完成")
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    currentTranslatedText = "翻译失败: ${e.message}"
                    isPanelLoading = false
                    updateTranslationPanel()
                }
            }
        }.start()
    }

    private fun showTranslationPanel() {
        if (translationPanelView != null) return

        // 创建半透明背景，点击可关闭
        val bgParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT
        )
        
        backgroundView = View(this).apply {
            setOnClickListener {
                hideTranslationPanel()
            }
        }
        
        try {
            windowManager.addView(backgroundView, bgParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // 标题
        val titleBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        titleBar.addView(TextView(this).apply {
            text = "翻译结果"
            textSize = 16f
            setTextColor(0xFF111827.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        titleBar.addView(TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(0xFF9CA3AF.toInt())
            setOnClickListener { hideTranslationPanel() }
        })
        panel.addView(titleBar)

        // 原文
        panel.addView(TextView(this).apply {
            text = "原文"
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dpToPx(12), 0, dpToPx(4))
        })
        panel.addView(TextView(this).apply {
            text = currentOriginalText
            textSize = 14f
            setTextColor(0xFF374151.toInt())
            maxLines = 3
        })

        // 译文
        panel.addView(TextView(this).apply {
            text = "译文"
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dpToPx(12), 0, dpToPx(4))
        })
        
        val transTextView = TextView(this).apply {
            text = if (isPanelLoading) "翻译中..." else currentTranslatedText
            textSize = 14f
            setTextColor(0xFF000000.toInt())
            maxLines = 5
        }
        panel.addView(transTextView)

        // 保存按钮
        panel.addView(Button(this).apply {
            text = "保存到 Obsidian"
            setBackgroundColor(0xFF10B981.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44)).apply { topMargin = dpToPx(16) }
            setOnClickListener {
                try {
                    obsidianHelper.saveToObsidian(currentOriginalText, currentTranslatedText)
                    showToast("已保存到 Obsidian")
                    hideTranslationPanel()
                } catch (e: Exception) {
                    showToast("保存失败: ${e.message}")
                }
            }
        })

        panelParams = WindowManager.LayoutParams(
            dpToPx(320), WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(), 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        translationPanelView = panel
        try {
            windowManager.addView(translationPanelView, panelParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTranslationPanel() {
        translationPanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
            translationPanelView = null
        }
        showTranslationPanel()
    }

    private fun hideTranslationPanel() {
        backgroundView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
            backgroundView = null
        }
        translationPanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
            translationPanelView = null
        }
        currentOriginalText = ""
        currentTranslatedText = ""
    }

    private fun removeFloatWindow() {
        hideTranslationPanel()
        floatBallView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
            floatBallView = null
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            try { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() } catch (e: Exception) { }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
