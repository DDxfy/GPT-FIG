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


----------------------------------------------------------------------------------------------------------------------

BYOK AI Shell Android
BYOK AI Shell Android is a mobile-first Android AI chat client built with Kotlin + Jetpack Compose. It allows users to configure their own API keys, model names and API endpoints locally, supporting direct calls to the OpenAI Responses API or compatible Chat Completions interfaces.
Core Features
Locally store chat conversations, model configurations and usage records
Secure storage of API keys via Android EncryptedSharedPreferences
Customizable models, endpoints, inference intensity and generation parameters
Multi-session management: pin, rename, delete and branch chat sessions
Support for reading text attachments and sending them with messages
Light / Dark theme mode available
Track request count, token consumption and response latency
Enforce HTTPS-only API access to prevent unencrypted plaintext requests
Tech Stack
Kotlin
Jetpack Compose
Material 3
Kotlinx Serialization
OkHttp
AndroidX Security Crypto
Gradle Kotlin DSL
Project Structure
The android-app/ directory in this repository is a standalone Android project (current version: V1.1.4). You can directly open it with Android Studio, sync Gradle, and run the project.
The developer is a college student, so project updates may be delayed occasionally. For any issues or feedback, please contact: 3050172393@qq.com
