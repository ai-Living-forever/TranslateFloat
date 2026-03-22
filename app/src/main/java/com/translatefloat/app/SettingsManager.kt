package com.translatefloat.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TranslationRecord(
    val original: String,
    val translated: String,
    val timestamp: Long = System.currentTimeMillis()
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "translate_float_prefs"
        private const val KEY_VAULT_NAME = "vault_name"
        private const val KEY_SAVE_PATH = "save_path"
        private const val KEY_TARGET_LANG = "target_lang"
        private const val KEY_HISTORY = "history"

        const val DEFAULT_SAVE_PATH = "Inbox/翻译笔记"
        const val DEFAULT_TARGET_LANG = "zh-CN"

        val LANGUAGES = mapOf(
            "zh-CN" to "中文",
            "en" to "英文",
            "ja" to "日语",
            "ko" to "韩语"
        )
    }

    var vaultName: String
        get() = prefs.getString(KEY_VAULT_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VAULT_NAME, value).apply()

    var savePath: String
        get() = prefs.getString(KEY_SAVE_PATH, DEFAULT_SAVE_PATH) ?: DEFAULT_SAVE_PATH
        set(value) = prefs.edit().putString(KEY_SAVE_PATH, value).apply()

    var targetLang: String
        get() = prefs.getString(KEY_TARGET_LANG, DEFAULT_TARGET_LANG) ?: DEFAULT_TARGET_LANG
        set(value) = prefs.edit().putString(KEY_TARGET_LANG, value).apply()

    fun getHistory(): List<TranslationRecord> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<List<TranslationRecord>>() {}.type
        return try {
            gson.fromJson<List<TranslationRecord>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addToHistory(original: String, translated: String) {
        val history = getHistory().toMutableList()
        history.add(0, TranslationRecord(original, translated))
        // Keep only last 20 items
        while (history.size > 20) {
            history.removeAt(history.size - 1)
        }
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun clearHistory() {
        prefs.edit().putString(KEY_HISTORY, "[]").apply()
    }

    fun getCurrentDateFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(Date())
    }
}
