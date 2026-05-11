云外AI盒
CloudVoid AI Box
-------------------------------------------------------------------------------------------------------------
# BYOK AI Shell Android

BYOK AI Shell Android 是一个手机优先的 Android AI 聊天客户端，采用 Kotlin + Jetpack Compose 构建。应用支持用户自行配置 API Key、模型名称和接口地址，直接在本地调用 OpenAI Responses API 或兼容的 Chat Completions 接口。

## 功能特点

- 本地保存聊天会话、模型配置和使用记录
- API Key 使用 Android EncryptedSharedPreferences 安全存储
- 支持自定义模型、Endpoint、推理强度和生成参数
- 支持多会话管理、置顶、重命名、删除和分支会话
- 支持文本附件读取并随消息发送
- 支持亮色 / 暗色主题
- 记录请求次数、Token 用量和响应耗时
- 强制使用 HTTPS 接口，避免明文请求

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Kotlinx Serialization
- OkHttp
- AndroidX Security Crypto
- Gradle Kotlin DSL

## 项目结构

该仓库中的 `android-app/` 是独立 Android 工程,目前版本V1.1.4，可直接使用 Android Studio 打开、同步 Gradle 后运行。

开发者为一位在校大学生，故有时更新不及时，有问题可反馈3050172393@qq.com
-----------------------------------------------------------------------------------------------------------------
BYOK AI Shell Android
BYOK AI Shell Android is a mobile-first Android AI chat clientbuilt with Kotlin + Jetpack Compose.
The app allows users to configure their own API Key, model nameand endpoint URL locally, enabling direct calls to theOpenAI Responses API or compatible Chat Completions interfaces.
Features
Locally save chat sessions, model configurations and usage records
Securely store API Keys with Android EncryptedSharedPreferences
Support custom models, endpoints, inference strength and generation parameters
Multi-session management: pin, rename, delete and branch sessions
Read text attachments and send them along with messages
Light / Dark theme support
Record request count, token usage and response time
Enforce HTTPS only to avoid plaintext network requests
Tech Stack
Kotlin
Jetpack Compose
Material 3
Kotlinx Serialization
OkHttp
AndroidX Security Crypto
Gradle Kotlin DSL
Project Structure
The android-app/ folder in the repository is a standalone Android project.Current version: V1.1.4.
You can open it directly in Android Studio, sync Gradle,then build and run the app.
The developer is an undergraduate student,so official updates may be delayed from time to time.
For issues and feedback, please contact:3050172393@qq.com
