package com.translatefloat.app

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
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var translateApi: TranslateApi
    private lateinit var obsidianHelper: ObsidianHelper

    private var floatBallView: View? = null
    private var translationPanelView: View? = null
    private var floatBallParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var startX = 0f
    private var startY = 0f

    private var currentOriginalText = ""
    private var currentTranslatedText = ""
    private var isPanelLoading = false

    companion object {
        private const val CHANNEL_ID = "translate_float_channel"
        private const val NOTIFICATION_ID = 1
        var isRunning = false
        
        // 静态方法供 MainActivity 调用
        var onTranslateListener: ((String) -> Unit)? = null
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗翻译",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮窗翻译服务运行中"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TranslateFloat")
            .setContentText("悬浮窗服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFloatBall() {
        if (floatBallView != null) return

        // 创建悬浮球容器
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(56), dpToPx(56))
            setBackgroundColor(0xFF6366F1.toInt())
        }

        // 添加文字
        val textView = TextView(this).apply {
            text = "译"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        container.addView(textView)

        // 设置 LayoutParams
        floatBallParams = WindowManager.LayoutParams(
            dpToPx(56),
            dpToPx(56),
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dpToPx(72)
            y = resources.displayMetrics.heightPixels / 2 - dpToPx(28)
        }

        // 使用 OnClickListener 处理点击
        container.setOnClickListener {
            onFloatBallClick()
        }

        // 同时保留触摸用于拖动
        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    initialX = floatBallParams?.x ?: 0
                    initialY = floatBallParams?.y ?: 0
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - startX).toInt()
                    val deltaY = (event.rawY - startY).toInt()
                    // 移动超过 10px 才认为是拖动
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        floatBallParams?.x = initialX + deltaX
                        floatBallParams?.y = initialY + deltaY
                        floatBallView?.let {
                            windowManager.updateViewLayout(it, floatBallParams)
                        }
                    }
                    true
                }
                else -> true
            }
        }

        floatBallView = container

        try {
            windowManager.addView(floatBallView, floatBallParams)
            showToast("悬浮球已显示")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("悬浮球创建失败: ${e.message}")
        }
    }

    private fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun onFloatBallClick() {
        showToast("正在翻译...")
        hideTranslationPanel()
        performTranslation()
    }

    // 公开方法供 MainActivity 调用
    fun translateText(text: String) {
        currentOriginalText = text
        isPanelLoading = true
        showTranslationPanel()
        
        // 同时更新剪贴板（因为 performTranslation 会读取剪贴板）
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("translate", text)
        clipboard.setPrimaryClip(clip)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val translated = translateApi.translate(text, settingsManager.targetLang)
                currentTranslatedText = translated
                
                settingsManager.addToHistory(text, translated)

                withContext(Dispatchers.Main) {
                    isPanelLoading = false
                    updateTranslationPanel()
                    showToast("翻译完成")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentTranslatedText = "翻译失败: ${e.message}"
                    isPanelLoading = false
                    updateTranslationPanel()
                    showToast("翻译失败: ${e.message}")
                }
            }
        }
    }

    private fun performTranslation() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

        if (clipText.isBlank()) {
            showToast("请先复制要翻译的文字")
            return
        }

        currentOriginalText = clipText
        isPanelLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val translated = translateApi.translate(clipText, settingsManager.targetLang)
                currentTranslatedText = translated
                
                settingsManager.addToHistory(clipText, translated)

                withContext(Dispatchers.Main) {
                    isPanelLoading = false
                    updateTranslationPanel()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentTranslatedText = "翻译失败: ${e.message}"
                    isPanelLoading = false
                    updateTranslationPanel()
                }
            }
        }
    }

    private fun showTranslationPanel() {
        if (translationPanelView != null) return

        val panel = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // 标题
        val titleBar = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        val title = android.widget.TextView(this).apply {
            text = "翻译结果"
            textSize = 18f
            setTextColor(0xFF111827.toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = android.widget.TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(0xFF9CA3AF.toInt())
            setOnClickListener { hideTranslationPanel() }
        }
        titleBar.addView(title)
        titleBar.addView(closeBtn)
        panel.addView(titleBar)

        // 原文
        val origLabel = android.widget.TextView(this).apply {
            text = "原文"
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dpToPx(12), 0, dpToPx(4))
        }
        panel.addView(origLabel)

        val origText = android.widget.TextView(this).apply {
            text = currentOriginalText
            textSize = 14f
            setTextColor(0xFF374151.toInt())
            maxLines = 3
        }
        panel.addView(origText)

        // 译文
        val transLabel = android.widget.TextView(this).apply {
            text = "译文"
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dpToPx(12), 0, dpToPx(4))
        }
        panel.addView(transLabel)

        val transText = android.widget.TextView(this).apply {
            text = if (isPanelLoading) "翻译中..." else currentTranslatedText
            textSize = 14f
            setTextColor(0xFF000000.toInt())
            maxLines = 5
        }
        panel.addView(transText)

        // 保存按钮
        val saveBtn = android.widget.Button(this).apply {
            text = "保存到 Obsidian"
            setBackgroundColor(0xFF6366F1.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            ).apply { topMargin = dpToPx(16) }
            setOnClickListener { saveToObsidian() }
        }
        panel.addView(saveBtn)

        panelParams = WindowManager.LayoutParams(
            dpToPx(320),
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        translationPanelView = panel

        try {
            windowManager.addView(translationPanelView, panelParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTranslationPanel() {
        translationPanelView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            translationPanelView = null
        }
        showTranslationPanel()
    }

    private fun hideTranslationPanel() {
        translationPanelView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            translationPanelView = null
        }
    }

    private fun saveToObsidian() {
        try {
            obsidianHelper.saveToObsidian(currentOriginalText, currentTranslatedText)
            showToast("已保存到 Obsidian")
            hideTranslationPanel()
        } catch (e: Exception) {
            showToast("保存失败: ${e.message}")
        }
    }

    private fun removeFloatWindow() {
        hideTranslationPanel()
        floatBallView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatBallView = null
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
