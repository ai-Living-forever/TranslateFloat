# TranslateFloat 📝

> 选中文字 → 翻译 → 保存到 Obsidian（按日期分类）

一个 Android 原生翻译悬浮窗应用，让你在手机上轻松翻译并保存到 Obsidian 笔记。

## ✨ 功能特性

- 🎈 **悬浮球翻译** - 点击屏幕边缘的悬浮球，自动读取剪贴板或选中的文字
- 🌐 **多语言支持** - 支持中文、英文、日语、韩语、法语、德语等
- 📁 **按日期保存** - 同一天的翻译自动保存到同一个 Markdown 文件
- 📱 **无障碍服务** - 自动检测用户选中的文字
- 💾 **本地存储** - 设置和翻译历史保存在本地

## 📱 使用方法

1. **安装 APK** - 下载并安装 TranslateFloat
2. **首次设置** - 打开应用，填写 Obsidian Vault 名称
3. **授权权限** - 授权悬浮窗权限和无障碍服务
4. **翻译** - 复制文字 → 点击悬浮球 → 查看翻译结果
5. **保存** - 点击保存按钮，自动追加到 Obsidian 对应日期的文件

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

- **语言**: Kotlin
- **UI**: Jetpack Compose
- **翻译 API**: Google Translate (免费)
- **通信**: OkHttp3 + Gson
- **保存**: Obsidian URL Scheme

## 📦 构建

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34
- Gradle 8.2

### 构建步骤

1. 克隆仓库
2. 用 Android Studio 打开项目
3. Sync Gradle
4. Build → Build APK

### APK 下载

直接下载已构建的 APK：
- [TranslateFloat.apk](http://app-andriod-play.oss-cn-guangzhou.aliyuncs.com/TranslateFloat.apk)

## 📄 许可证

MIT License
