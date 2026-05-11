package local.byok.android.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AppState(
    val models: List<String> = listOf("gpt-5.4", "gpt-5.5"),
    val settings: ModelSettings = ModelSettings(),
    val sessions: List<ChatSession> = listOf(ChatSession()),
    val activeSessionId: String = sessions.first().id,
    val usage: List<UsageDay> = emptyList(),
    val disclaimerVersionAccepted: Int = 0
)

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val pinned: Boolean = false,
    val temporary: Boolean = false,
    val settings: ModelSettings = ModelSettings(),
    val messages: List<ChatMessage> = emptyList(),
    val stats: UsageStats = UsageStats(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val meta: MessageMeta? = null,
    val error: Boolean = false
)

@Serializable
enum class MessageRole { User, Assistant }

@Serializable
data class MessageMeta(
    val model: String,
    val requestedReasoning: ReasoningChoice,
    val actualReasoning: ReasoningEffort,
    val stream: Boolean,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val reasoningTokens: Int = 0,
    val totalTokens: Int = 0,
    val latencyMs: Long = 0,
    val estimated: Boolean = true
)

@Serializable
data class UsageStats(
    val requests: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val reasoningTokens: Int = 0,
    val totalTokens: Int = 0,
    val lastTokens: Int = 0,
    val estimatedCost: Double = 0.0
)

@Serializable
data class UsageDay(
    val date: String,
    val model: String,
    val requests: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val reasoningTokens: Int = 0,
    val totalTokens: Int = 0,
    val estimatedCost: Double = 0.0
)

@Serializable
data class ModelSettings(
    val model: String = "",
    val endpointUrl: String = "",
    val theme: AppTheme = AppTheme.Light,
    val compatibilityMode: CompatibilityMode = CompatibilityMode.Generic,
    val reasoning: ReasoningChoice = ReasoningChoice.Auto,
    val strategy: QualityStrategy = QualityStrategy.Balanced,
    val temperature: Double = 0.7,
    val topP: Double = 1.0,
    val presencePenalty: Double = 0.0,
    val frequencyPenalty: Double = 0.0,
    val maxOutputTokens: Int = 4096,
    val stream: Boolean = false,
    val contextTurns: ContextTurns = ContextTurns.Five,
    val tokenLimit: TokenLimit = TokenLimit.Auto,
    val trimStrategy: TrimStrategy = TrimStrategy.Oldest,
    val timeoutSeconds: Long = 60,
    val saveHistory: Boolean = true,
    val keepOnClose: Boolean = true,
    val cancelOnBlur: Boolean = true
)

@Serializable
enum class AppTheme(val label: String) {
    Light("白色"),
    Dark("黑色")
}

@Serializable
enum class CompatibilityMode(val label: String, val description: String) {
    OpenAIGPT("OpenAI GPT", "发送推理强度和完整生成参数"),
    Generic("通用", "保留常见生成参数，兼容多数接口"),
    Minimal("最小", "只发送基础聊天请求，适合排错")
}

@Serializable
enum class ReasoningChoice(val label: String) {
    Auto("自动/智能"),
    None("快速/无推理"),
    Low("低"),
    Medium("中"),
    High("高"),
    XHigh("超高")
}

@Serializable
enum class ReasoningEffort(val apiValue: String, val label: String) {
    None("none", "快速/无推理"),
    Low("low", "低"),
    Medium("medium", "中"),
    High("high", "高"),
    XHigh("xhigh", "超高")
}

@Serializable
enum class QualityStrategy(val label: String) {
    Speed("速度优先"),
    Balanced("平衡模式"),
    Saving("省钱优先"),
    Quality("质量优先")
}

@Serializable
enum class ContextTurns(val label: String, val messageCount: Int?) {
    Three("3 轮", 6),
    Five("5 轮", 10),
    Ten("10 轮", 20),
    Unlimited("不限", null)
}

@Serializable
enum class TokenLimit(val label: String, val tokens: Int?) {
    K32("32k", 32_000),
    K128("128k", 128_000),
    K400("400k", 400_000),
    M1("1M", 1_000_000),
    Auto("自动", null)
}

@Serializable
enum class TrimStrategy(val label: String) {
    Oldest("删除最旧消息"),
    Summary("自动摘要旧上下文")
}
