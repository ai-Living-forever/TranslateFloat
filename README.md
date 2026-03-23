# TranslateFloat 📝

> 选中文字 → 翻译 → 保存到 Obsidian（按日期分类）

一个轻量级的 PWA 翻译应用，让你在手机上轻松翻译并保存到 Obsidian 笔记。

## ✨ 功能特性

- 📋 **一键翻译** - 输入或粘贴文字，点击按钮即可翻译
- 🌐 **多语言支持** - 支持中文、英文、日语、韩语、法语、德语等
- 📁 **按日期保存** - 同一天的翻译自动保存到同一个 Markdown 文件
- 📱 **PWA 应用** - 可添加到手机主屏幕，像原生 App 一样使用
- 💾 **本地存储** - 设置和历史记录保存在浏览器本地

## 🚀 快速开始

1. 打开应用
2. 设置你的 Obsidian Vault 名称
3. 输入要翻译的文字
4. 点击「翻译」按钮
5. 查看翻译结果，点击保存

## 📱 使用方法

1. **首次使用**：在设置中填写你的 Obsidian Vault 名称
2. **翻译**：在翻译框中输入文字 → 点击「翻译」按钮 → 查看结果
3. **保存**：点击「保存」自动创建/追加笔记
4. **PWA**：浏览器点击「添加到主屏幕」

## 📂 Obsidian 保存格式

```
Inbox/翻译笔记/
├── 2026-03-23.md
├── 2026-03-24.md
└── ...
```

笔记内容格式：
```markdown
## 18:30
**原文**（English）：
Hello world

**译文**（中文）：
你好世界
```

## 🛠 技术栈

- HTML5 + CSS3 + Vanilla JavaScript
- MyMemory Translation API（免费）
- PWA (Web Manifest)
- Obsidian URL Scheme

## 📄 许可证

MIT License
