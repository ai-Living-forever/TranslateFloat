# TranslateFloat - 翻译悬浮窗 Android 应用

## Project Overview

- **项目名称**: TranslateFloat
- **类型**: Android 原生应用 (Kotlin)
- **核心功能**: 选中手机屏幕文字 → 翻译 → 一键保存到 Obsidian（按日期分类）
- **目标用户**: 需要在手机上翻译并保存到笔记的用户

## UI/UX Specification

### Layout Structure
- **主页面**: 首页（翻译按钮）、设置、历史记录
- **悬浮球**: 屏幕边缘的圆形翻译按钮
- **翻译面板**: 原文 + 译文 + 保存按钮
- **导航**: 底部导航栏（首页/设置/历史）

### Visual Design
- **主色调**: #6366F1 (Indigo)
- **辅助色**: #4F46E5 (深 Indigo)
- **背景色**: #F9FAFB (浅灰)
- **文字色**: #1F2937 (深灰)
- **成功色**: #10B981 (绿色)
- **圆角**: 12dp
- **字体**: system-ui, Jetpack Compose 默认字体

### Components
1. **首页卡片** - 翻译按钮、权限状态、服务开关
2. **设置页面** - Vault名称、保存路径、目标语言
3. **历史页面** - 翻译记录列表
4. **悬浮球** - 56dp 圆形按钮
5. **翻译面板** - 320dp 宽度，居中显示

## Functionality Specification

### 核心功能
1. **悬浮球服务** - 后台运行，监听点击事件
2. **翻译功能** - Google Translate 免费 API
3. **无障碍服务** - 检测用户选中的文字
4. **保存到 Obsidian** - 通过 URL Scheme 唤起 Obsidian
5. **按日期分类** - 同一天的翻译保存到同一个 Markdown 文件

### 用户交互流程
1. 用户打开 App → 配置 Obsidian Vault 名称
2. 授权悬浮窗权限和无障碍服务
3. 复制或选中文字
4. 点击悬浮球 → 显示翻译面板
5. 点击保存 → 唤起 Obsidian → 保存到对应日期的笔记

### 数据存储
- SharedPreferences 保存设置
- SQLite (Room) 保存翻译历史

## Technical Specification

### 语言与框架
- **语言**: Kotlin
- **UI**: Jetpack Compose
- **最低 SDK**: 21
- **目标 SDK**: 34

### 依赖
- androidx.compose.* (Material3)
- androidx.navigation (Compose Navigation)
- com.squareup.okhttp3 (网络请求)
- com.google.code.gson (JSON 解析)

### 权限
- SYSTEM_ALERT_WINDOW (悬浮窗)
- INTERNET (翻译 API)
- FOREGROUND_SERVICE (后台服务)
- READ_CLIPBOARD (读取剪贴板)
- ACCESSIBILITY_SERVICE (无障碍服务)

## Acceptance Criteria
- [x] 用户可以配置 Obsidian Vault 名称
- [x] 悬浮球可以读取剪贴板内容
- [x] 翻译结果可一键保存到 Obsidian
- [x] 同一天的翻译保存在同一个文件中
- [x] 无障碍服务可以检测选中的文字
- [x] APK 可直接安装使用
