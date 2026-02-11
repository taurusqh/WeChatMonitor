# 微信群消息监听助手

Android应用，监听指定微信群消息，智能分析重要性并及时提醒。

## 功能特性

- **无障碍服务监听**：通过Android无障碍服务监听微信群消息
- **关键词分析**：支持正则表达式匹配关键词
- **AI智能分析**：集成智谱AI GLM-4-Flash模型分析消息重要性
- **发送者过滤**：支持白名单/黑名单过滤发送者
- **重要消息提醒**：声音、震动和系统通知
- **每日摘要**：定时生成当天重要消息摘要，支持分享

## 技术栈

- **语言**：Kotlin
- **UI框架**：Jetpack Compose + Material3
- **架构**：MVVM + Clean Architecture
- **数据库**：Room
- **网络请求**：Retrofit + OkHttp
- **异步处理**：Kotlin Coroutines + Flow
- **后台任务**：WorkManager
- **AI模型**：智谱AI GLM-4-Flash

## 项目结构

```
WeChatMonitor/
├── app/
│   ├── src/main/
│   │   ├── java/com/wechatmonitor/
│   │   │   ├── MainActivity.kt              # 主Activity
│   │   │   ├── WeChatMonitorApplication.kt  # Application类
│   │   │   ├── service/
│   │   │   │   ├── WeChatAccessibilityService.kt  # 无障碍服务
│   │   │   │   └── MessageProcessor.kt             # 消息处理器
│   │   │   ├── viewmodel/
│   │   │   │   └── MainViewModel.kt         # 主界面ViewModel
│   │   │   ├── model/
│   │   │   │   ├── ChatMessage.kt           # 消息数据模型
│   │   │   │   ├── MonitorSettings.kt       # 设置数据模型
│   │   │   │   └── DailySummary.kt          # 每日摘要模型
│   │   │   ├── repository/
│   │   │   │   ├── MessageRepository.kt     # 消息仓库
│   │   │   │   └── SettingsRepository.kt    # 设置仓库
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt           # Room数据库
│   │   │   │   ├── MessageDao.kt            # 消息DAO
│   │   │   │   └── entities/
│   │   │   ├── network/
│   │   │   │   ├── GLMApiService.kt         # GLM API接口
│   │   │   │   └── NetworkModule.kt         # 网络模块
│   │   │   ├── notification/
│   │   │   │   ├── NotificationHelper.kt    # 通知助手
│   │   │   │   └── SoundPlayer.kt           # 声音播放器
│   │   │   ├── analyzer/
│   │   │   │   ├── KeywordAnalyzer.kt       # 关键词分析器
│   │   │   │   ├── GLMAnalyzer.kt           # GLM分析器
│   │   │   │   └── MessageAnalyzer.kt       # 综合分析器
│   │   │   ├── worker/
│   │   │   │   └── DailySummaryWorker.kt    # 每日摘要Worker
│   │   │   ├── utils/
│   │   │   │   └── Scheduler.kt             # 任务调度器
│   │   │   └── ui/
│   │   │       ├── MainScreen.kt            # 主界面
│   │   │       └── theme/                   # 主题配置
```

## 使用说明

### 1. 构建项目

```bash
cd WeChatMonitor
./gradlew assembleDebug
```

### 2. 安装应用

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 配置权限

1. **开启无障碍服务**：设置 → 无障碍 → 微信监听助手 → 开启
2. **允许通知权限**：设置 → 应用 → 微信监听助手 → 通知 → 允许
3. **添加监听群组**：在应用设置中添加要监听的微信群名称

### 4. 配置AI分析（可选）

1. 获取智谱AI API Key：https://open.bigmodel.cn/
2. 在应用设置中输入API Key
3. 选择分析模式（关键词/AI/两者结合）

## 核心功能说明

### 消息监听

无障碍服务监听微信应用的界面变化，捕获聊天消息并解析：
- 群名称
- 发送者
- 消息内容
- 时间戳

### 重要性分析

**关键词模式**：
- 支持普通关键词匹配
- 支持正则表达式
- 可设置关键词权重
- 支持发送者白名单/黑名单

**AI分析模式**：
- 使用GLM-4-Flash模型分析消息
- 根据工作相关性、紧急程度等因素判断
- 返回0-1的重要性分数

**综合模式**：
- 同时使用关键词和AI分析
- 取两者中较高的分数

### 每日摘要

- 定时任务自动执行
- 使用GLM生成摘要
- 支持按群分类
- 一键分享功能

## 注意事项

1. **无障碍服务**：不同微信版本可能需要调整监听逻辑
2. **隐私保护**：所有数据仅存储在本地设备
3. **电池优化**：建议将应用加入电池优化白名单
4. **后台运行**：Android 8.0+需要处理后台服务限制
5. **API限流**：GLM API调用有频率限制

## 权限说明

- `BIND_ACCESSIBILITY_SERVICE`：无障碍服务权限
- `POST_NOTIFICATIONS`：发送通知权限
- `VIBRATE`：震动权限
- `INTERNET`：网络请求权限（AI分析）
- `ACCESS_NETWORK_STATE`：网络状态检查
- `FOREGROUND_SERVICE`：前台服务权限

## 开发者信息

- 使用 Kotlin + Jetpack Compose 开发
- 最小SDK版本：API 24 (Android 7.0)
- 目标SDK版本：API 34 (Android 14)

## License

本项目仅供学习和研究使用。
