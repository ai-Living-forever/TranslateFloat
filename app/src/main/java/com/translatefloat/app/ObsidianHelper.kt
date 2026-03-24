package com.translatefloat.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

class ObsidianHelper(private val context: Context) {
    private val settingsManager = SettingsManager(context)

    /**
     * 保存到 Obsidian
     * 使用 Uri.encode 进行 URL 编码，避免 + 号问题
     */
    fun saveToObsidian(original: String, translated: String) {
        val vault = settingsManager.vaultName
        if (vault.isBlank()) {
            throw IllegalStateException("请先设置 Obsidian Vault 名称")
        }

        val dateFileName = settingsManager.getCurrentDateFileName()
        val time = settingsManager.getCurrentTime()
        val savePath = settingsManager.savePath
        
        val filePath = "$savePath/$dateFileName.md"
        
        // 改进格式：原文和译文分开显示
        val content = buildString {
            append("## $time\n")
            append("**原文**：\n$original\n\n")
            append("**译文**：\n$translated\n")
            append("---\n\n")
        }

        // 使用 Uri.encode 而不是 URLEncoder.encode，避免空格变成 +
        val encodedVault = Uri.encode(vault)
        val encodedFile = Uri.encode(filePath)
        val encodedContent = Uri.encode(content)

        val url = "obsidian://new?vault=$encodedVault&file=$encodedFile&content=$encodedContent&append=true"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    }

    fun saveToObsidianWithRecord(record: TranslationRecord) {
        val vault = settingsManager.vaultName
        if (vault.isBlank()) {
            throw IllegalStateException("请先设置 Obsidian Vault 名称")
        }

        val dateFileName = settingsManager.getCurrentDateFileName()
        val time = settingsManager.getCurrentTime()
        val savePath = settingsManager.savePath
        
        val filePath = "$savePath/$dateFileName.md"
        
        val content = buildString {
            append("## $time\n")
            append("**原文**：\n${record.original}\n\n")
            append("**译文**：\n${record.translated}\n")
            append("---\n\n")
        }

        // 使用 Uri.encode 而不是 URLEncoder.encode
        val encodedVault = Uri.encode(vault)
        val encodedFile = Uri.encode(filePath)
        val encodedContent = Uri.encode(content)

        val url = "obsidian://new?vault=$encodedVault&file=$encodedFile&content=$encodedContent&append=true"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    }
}
