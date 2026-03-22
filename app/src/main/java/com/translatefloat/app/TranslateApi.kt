package com.translatefloat.app

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.JsonArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class TranslateApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * 使用 Google Translate 免费 API
     * 非官方但广泛使用，无需 API Key
     */
    @Throws(IOException::class)
    fun translate(text: String, targetLang: String): String {
        if (text.isBlank()) {
            throw IOException("翻译文本不能为空")
        }

        // Google Translate 免费 API
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val langCode = mapTargetLang(targetLang)
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$langCode&dt=t&q=$encodedText"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("翻译请求失败: ${response.code}")
            }

            val responseBody = response.body?.string()
                ?: throw IOException("响应为空")

            // 解析 JSON 数组格式
            // 返回格式: [[翻译结果, 原文, ...], ...]
            val jsonArray = gson.fromJson(responseBody, JsonArray::class.java)
            
            if (jsonArray.size() == 0) {
                throw IOException("翻译结果为空")
            }

            val firstElement = jsonArray.get(0)
            if (firstElement is JsonArray && firstElement.size() > 0) {
                val translated = StringBuilder()
                for (item in firstElement) {
                    if (item is JsonArray && item.size() > 0) {
                        val translatedText = item.get(0)?.asString
                        if (!translatedText.isNullOrBlank()) {
                            translated.append(translatedText)
                        }
                    }
                }
                val result = translated.toString()
                if (result.isBlank()) {
                    throw IOException("无法提取翻译结果")
                }
                return result
            }

            throw IOException("无法解析翻译结果")
        }
    }

    /**
     * 映射语言代码到 Google Translate 格式
     */
    private fun mapTargetLang(lang: String): String {
        return when (lang) {
            "zh-CN", "zh" -> "zh-CN"
            "en" -> "en"
            "ja" -> "ja"
            "ko" -> "ko"
            "fr" -> "fr"
            "de" -> "de"
            "es" -> "es"
            "ru" -> "ru"
            "pt" -> "pt"
            "it" -> "it"
            "ar" -> "ar"
            else -> "zh-CN"
        }
    }
}
