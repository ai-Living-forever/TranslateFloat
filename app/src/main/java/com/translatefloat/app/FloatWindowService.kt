package com.translatefloat.app

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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

    // 悬浮球
    private var floatBallView: View? = null
    private var floatBallParams: WindowManager.LayoutParams? = null
    
    // 翻译面板
    private var panelContainer: View? = null  // 外层容器（透明背景）
    private var panelParams: WindowManager.LayoutParams? = null

    // 拖动状态
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var ballInitX = 0
    private var ballInitY = 0
    private var isDragging = false

    // 翻译状态
    private var currentOriginalText = ""
    private var currentTranslatedText = ""
    private var isPanelShowing = false
    private var isTranslating = false

    companion object {
        private const val CHANNEL_ID = "translate_float_channel"
        private const val NOTIFICATION_ID = 1
        private const val BALL_SIZE_DP = 56
        private const val EDGE_MARGIN_DP = 12

        var isRunning = false
        var lastSelectedText: String = ""
        var lastClipboardText: String = ""
        var lastTranslatedText: String = ""  // 保存上次翻译的原文
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
        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning = true
        Handler(Looper.getMainLooper()).postDelayed({ showFloatBall() }, 300)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeAllViews()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 通知
    // ═══════════════════════════════════════════════════════════════════════

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "悬浮窗翻译", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TranslateFloat")
            .setContentText("悬浮翻译运行中")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 悬浮球
    // ═══════════════════════════════════════════════════════════════════════

    private fun showFloatBall() {
        if (floatBallView != null) return

        val ballPx = dp(BALL_SIZE_DP)
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        // 圆形紫色背景
        val circleBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF6366F1.toInt())
        }

        // 白色"译"字
        val label = TextView(this).apply {
            text = "译"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val ball = FrameLayout(this).apply {
            background = circleBg
            addView(label)
            elevation = 8f
        }

        floatBallParams = WindowManager.LayoutParams(
            ballPx, ballPx,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - ballPx - dp(EDGE_MARGIN_DP)
            y = screenH / 3
        }

        ball.setOnTouchListener { _, ev -> handleBallTouch(ev) }

        floatBallView = ball
        try { windowManager.addView(ball, floatBallParams) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun handleBallTouch(ev: MotionEvent): Boolean {
        // 如果面板正在显示，忽略悬浮球点击
        if (isPanelShowing) return false

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ev.rawX
                touchStartY = ev.rawY
                ballInitX = floatBallParams?.x ?: 0
                ballInitY = floatBallParams?.y ?: 0
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - touchStartX
                val dy = ev.rawY - touchStartY
                if (!isDragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) {
                    isDragging = true
                }
                if (isDragging) {
                    floatBallParams?.x = (ballInitX + dx).toInt()
                    floatBallParams?.y = (ballInitY + dy).toInt()
                    floatBallView?.let { windowManager.updateViewLayout(it, floatBallParams) }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    snapToEdge()
                } else {
                    onBallClick()
                }
            }
        }
        return true
    }

    /** 松手后吸附到左/右边缘 */
    private fun snapToEdge() {
        val params = floatBallParams ?: return
        val ballPx = dp(BALL_SIZE_DP)
        val screenW = resources.displayMetrics.widthPixels
        val margin = dp(EDGE_MARGIN_DP)

        val targetX = if (params.x + ballPx / 2 < screenW / 2) {
            margin
        } else {
            screenW - ballPx - margin
        }

        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 200
            addUpdateListener {
                params.x = it.animatedValue as Int
                floatBallView?.let { v -> windowManager.updateViewLayout(v, params) }
            }
            start()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 点击悬浮球 -> 开始翻译
    // ═══════════════════════════════════════════════════════════════════════

    private fun onBallClick() {
        if (isPanelShowing || isTranslating) return

        // 获取要翻译的文字
        val text = getTranslatableText()
        
        if (text.isNullOrBlank()) {
            toast("请先复制要翻译的文字")
            return
        }

        // 保存原文
        currentOriginalText = text
        currentTranslatedText = ""
        isTranslating = true

        // 显示面板
        showPanel()

        // 执行翻译
        Thread {
            try {
                val result = translateApi.translate(text, settingsManager.targetLang)
                currentTranslatedText = result
                settingsManager.addToHistory(text, result)
                
                Handler(Looper.getMainLooper()).post {
                    isTranslating = false
                    updatePanelContent()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    currentTranslatedText = "翻译失败: ${e.message}"
                    isTranslating = false
                    updatePanelContent()
                }
            }
        }.start()
    }

    private fun getTranslatableText(): String? {
        // 1. 优先使用选中的文字
        if (lastSelectedText.isNotBlank()) {
            val text = lastSelectedText
            lastSelectedText = "" // 用完清空
            return text
        }
        
        // 2. 读取剪贴板
        try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = cb.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            if (!clipText.isNullOrBlank()) {
                return clipText
            }
        } catch (e: Exception) { }
        
        // 3. 使用备用内容
        if (lastClipboardText.isNotBlank()) {
            return lastClipboardText
        }
        
        return null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 翻译面板
    // ═══════════════════════════════════════════════════════════════════════

    private fun showPanel() {
        if (panelContainer != null) return
        
        isPanelShowing = true

        // 外层容器：全屏透明背景，点击关闭面板
        val container = FrameLayout(this).apply {
            setBackgroundColor(0x80000000.toInt()) // 半透明黑色背景
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismissPanel()
            }
        }

        // 内部卡片
        val card = buildPanelCard()

        // 卡片居中
        val cardParams = FrameLayout.LayoutParams(
            dp(320),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        container.addView(card, cardParams)

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        panelContainer = container
        try {
            windowManager.addView(container, panelParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildPanelCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            // 白色圆角背景
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(16).toFloat()
            }
            elevation = 16f
            // 阻止点击穿透
            isClickable = true
            isFocusable = true
        }

        // 标题栏
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }
        
        titleRow.addView(TextView(this).apply {
            text = "翻译结果"
            textSize = 18f
            setTextColor(0xFF111827.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        
        // 关闭按钮 X
        titleRow.addView(TextView(this).apply {
            text = "✕"
            textSize = 22f
            setTextColor(0xFF9CA3AF.toInt())
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener { dismissPanel() }
        })
        
        card.addView(titleRow)

        // 分割线
        card.addView(View(this).apply {
            setBackgroundColor(0xFFE5E7EB.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { bottomMargin = dp(12) }
        })

        // 原文标签
        card.addView(TextView(this).apply {
            text = "原文"
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, 0, 0, dp(4))
        })

        // 原文内容 - 确保显示
        val originalTv = TextView(this).apply {
            text = currentOriginalText.ifEmpty { "（无内容）" }
            textSize = 15f
            setTextColor(0xFF374151.toInt())
            maxLines = 5
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(0xFFF3F4F6.toInt())
                cornerRadius = dp(8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(originalTv)

        // 间距
        card.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(16))
        })

        // 译文标签
        card.addView(TextView(this).apply {
            text = "译文"
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, 0, 0, dp(4))
        })

        // 译文内容
        val translatedTv = TextView(this).apply {
            text = if (isTranslating) "翻译中..." else currentTranslatedText.ifEmpty { "" }
            textSize = 16f
            setTextColor(0xFF111827.toInt())
            maxLines = 8
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(0xFFEEF2FF.toInt())
                cornerRadius = dp(8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(translatedTv)

        // 保存按钮
        card.addView(Button(this).apply {
            text = "💾 保存到 Obsidian"
            setTextColor(0xFFFFFFFF.toInt())
            isAllCaps = false
            textSize = 16f
            background = GradientDrawable().apply {
                setColor(0xFF10B981.toInt())
                cornerRadius = dp(10).toFloat()
            }
            setPadding(0, dp(12), 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
            ).apply { topMargin = dp(20) }
            setOnClickListener {
                try {
                    obsidianHelper.saveToObsidian(currentOriginalText, currentTranslatedText)
                    toast("已保存到 Obsidian")
                    dismissPanel()
                } catch (e: Exception) {
                    toast("保存失败: ${e.message}")
                }
            }
        })

        return card
    }

    private fun updatePanelContent() {
        if (panelContainer == null) return
        
        // 移除旧卡片，添加新卡片
        val container = panelContainer as? FrameLayout ?: return
        container.removeAllViews()
        
        val card = buildPanelCard()
        val cardParams = FrameLayout.LayoutParams(
            dp(320),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }
        container.addView(card, cardParams)
    }

    private fun dismissPanel() {
        panelContainer?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
        }
        panelContainer = null
        panelParams = null
        currentOriginalText = ""
        currentTranslatedText = ""
        isPanelShowing = false
        isTranslating = false
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════════════════════════════════════

    private fun windowType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun toast(msg: String) =
        Handler(Looper.getMainLooper()).post {
            try { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } catch (_: Exception) { }
        }

    private fun removeAllViews() {
        dismissPanel()
        floatBallView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
            floatBallView = null
        }
    }
}
