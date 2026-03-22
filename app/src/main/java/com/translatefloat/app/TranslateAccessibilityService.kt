package com.translatefloat.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class TranslateAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 检测文本选择变化
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                // 获取选中的文本
                try {
                    val selectionStart = event.fromIndex
                    val selectionEnd = event.toIndex
                    
                    if (selectionStart >= 0 && selectionEnd > selectionStart) {
                        val source = event.source
                        val text = source?.text
                        if (text != null && selectionStart < text.length && selectionEnd <= text.length) {
                            val selectedText = text.substring(selectionStart, selectionEnd)
                            if (selectedText.isNotBlank()) {
                                // 保存选中的文本
                                FloatWindowService.lastSelectedText = selectedText
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        }
    }

    override fun onInterrupt() {
        // Nothing to do
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 服务已连接
    }
}
