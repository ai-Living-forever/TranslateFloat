# TranslateFloat - 翻译悬浮窗 PWA

## Project Overview
- **项目名称**: TranslateFloat
- **类型**: 渐进式网页应用 (PWA)
- **核心功能**: 选中手机屏幕文字 → 翻译 → 一键保存到 Obsidian（按日期分类）
- **目标用户**: 需要在手机上翻译并保存到笔记的用户

## UI/UX Specification

### Layout Structure
- **主页面**: 设置界面（Vault名称、保存路径等配置）
- **悬浮按钮**: 屏幕边缘的圆形翻译按钮
- **翻译弹窗**: 原文 + 译文 + 保存按钮

### Visual Design
- **主色调**: #6366F1 ( Indigo )
- **辅助色**: #4F46E5 ( 深 Indigo )
- **背景色**: #F9FAFB ( 浅灰 )
- **文字色**: #1F2937 ( 深灰 )
- **成功色**: #10B981 ( 绿色 )
- **圆角**: 12px
- **字体**: system-ui, -apple-system, sans-serif

### Components
1. **设置卡片** - 配置 Obsidian Vault 和保存路径
2. **悬浮按钮** - 固定在屏幕右下角
3. **翻译弹窗** - 原文/译文显示 + 保存按钮
4. **历史记录** - 最近翻译记录列表

## Functionality Specification

### 核心功能
1. **文本选择监听** - 监听页面文本选择事件
2. **翻译功能** - 使用 MyMemory 免费 API
3. **语言识别** - 自动检测源语言
4. **保存到 Obsidian** - 通过 URL Scheme 唤起 Obsidian 并创建/追加笔记
5. **按日期分类** - 同一天的翻译保存到同一个 Markdown 文件

### 用户交互流程
1. 用户打开 App → 配置 Obsidian Vault 名称
2. 选择任意网页/应用的文字
3. 点击悬浮按钮 → 显示翻译弹窗
4. 点击保存 → 唤起 Obsidian → 保存到对应日期的笔记

### 数据存储
- 使用 localStorage 保存设置和翻译历史

## Acceptance Criteria
- [x] 用户可以配置 Obsidian Vault 名称
- [x] 选中文字后点击按钮可翻译
- [x] 翻译结果可一键保存到 Obsidian
- [x] 同一天的翻译保存在同一个文件中
- [x] 可以添加到手机桌面作为 PWA 使用
