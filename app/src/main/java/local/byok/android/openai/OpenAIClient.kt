package local.byok.android.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import local.byok.android.model.ChatMessage
import local.byok.android.model.CompatibilityMode
import local.byok.android.model.MessageRole
import local.byok.android.model.ModelSettings
import local.byok.android.model.ReasoningEffort
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAIClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun send(
        apiKey: String,
        model: String,
        settings: ModelSettings,
        effort: ReasoningEffort,
        messages: List<ChatMessage>
    ): OpenAIResult {
        val endpoint = normalizedEndpoint(settings.endpointUrl)
        val bodyText = if (endpoint.endsWith("/chat/completions")) {
            json.encodeToString(buildChatRequest(model, settings, effort, messages))
        } else {
            json.encodeToString(buildResponsesRequest(model, settings, effort, messages))
        }

        val body = bodyText.toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(readableError(response.code, payload))
            }
            return if (endpoint.endsWith("/chat/completions")) {
                val data = json.decodeFromString(ChatCompletionsResponse.serializer(), payload)
                OpenAIResult(
                    text = data.choices.firstOrNull()?.message?.content.orEmpty(),
                    inputTokens = data.usage?.promptTokens,
                    outputTokens = data.usage?.completionTokens,
                    reasoningTokens = null,
                    totalTokens = data.usage?.totalTokens
                )
            } else {
                val data = json.decodeFromString(ResponsesResponse.serializer(), payload)
                OpenAIResult(
                    text = data.outputText ?: data.output.orEmpty()
                        .flatMap { it.content.orEmpty() }
                        .mapNotNull { it.text }
                        .joinToString(""),
                    inputTokens = data.usage?.inputTokens,
                    outputTokens = data.usage?.outputTokens,
                    reasoningTokens = data.usage?.outputTokensDetails?.reasoningTokens,
                    totalTokens = data.usage?.totalTokens
                )
            }
        }
    }

    private fun normalizedEndpoint(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        if (trimmed.isBlank()) {
            throw IllegalStateException("请先在设置里填写请求地址")
        }
        return trimmed
    }

    private fun buildChatRequest(
        model: String,
        settings: ModelSettings,
        effort: ReasoningEffort,
        messages: List<ChatMessage>
    ) = ChatCompletionsRequest(
        model = model,
        messages = messages.map {
            ChatCompletionMessage(
                role = if (it.role == MessageRole.Assistant) "assistant" else "user",
                content = it.content
            )
        },
        temperature = settings.temperature.takeIf { settings.compatibilityMode != CompatibilityMode.Minimal },
        topP = settings.topP.takeIf { settings.compatibilityMode != CompatibilityMode.Minimal },
        presencePenalty = settings.presencePenalty.takeIf { settings.compatibilityMode == CompatibilityMode.OpenAIGPT },
        frequencyPenalty = settings.frequencyPenalty.takeIf { settings.compatibilityMode == CompatibilityMode.OpenAIGPT },
        maxTokens = settings.maxOutputTokens.takeIf { settings.compatibilityMode != CompatibilityMode.Minimal },
        reasoningEffort = effort.apiValue.takeIf { settings.compatibilityMode == CompatibilityMode.OpenAIGPT && it != "none" },
        stream = false.takeIf { settings.compatibilityMode != CompatibilityMode.Minimal }
    )

    private fun buildResponsesRequest(
        model: String,
        settings: ModelSettings,
        effort: ReasoningEffort,
        messages: List<ChatMessage>
    ) = ResponsesRequest(
        model = model,
        input = buildPrompt(messages),
        reasoning = Reasoning(effort.apiValue),
        temperature = settings.temperature,
        topP = settings.topP,
        presencePenalty = settings.presencePenalty,
        frequencyPenalty = settings.frequencyPenalty,
        maxOutputTokens = settings.maxOutputTokens,
        stream = false
    )

    private fun buildPrompt(messages: List<ChatMessage>): String =
        messages.joinToString("\n\n") {
            val role = if (it.role == MessageRole.Assistant) "助手" else "用户"
            "$role：${it.content}"
        }

    private fun readableError(code: Int, body: String): String {
        if (code == 401) return "API Key 无效或已过期"
        if (code == 402 || code == 429) return "余额不足、额度受限或请求过快"
        if (code == 404) return "模型或请求地址不可用"
        return "请求失败，HTTP $code"
    }
}

data class OpenAIResult(
    val text: String,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val reasoningTokens: Int?,
    val totalTokens: Int?
)

@Serializable
private data class ChatCompletionsRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("presence_penalty") val presencePenalty: Double? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    val stream: Boolean? = null
)

@Serializable
private data class ChatCompletionMessage(
    val role: String,
    val content: String
)

@Serializable
private data class ChatCompletionsResponse(
    val choices: List<ChatChoice> = emptyList(),
    val usage: ChatUsage? = null
)

@Serializable
private data class ChatChoice(val message: ChatChoiceMessage? = null)

@Serializable
private data class ChatChoiceMessage(val content: String? = null)

@Serializable
private data class ChatUsage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)

@Serializable
private data class ResponsesRequest(
    val model: String,
    val input: String,
    val reasoning: Reasoning,
    val temperature: Double,
    @SerialName("top_p") val topP: Double,
    @SerialName("presence_penalty") val presencePenalty: Double,
    @SerialName("frequency_penalty") val frequencyPenalty: Double,
    @SerialName("max_output_tokens") val maxOutputTokens: Int,
    val stream: Boolean
)

@Serializable
private data class Reasoning(val effort: String)

@Serializable
private data class ResponsesResponse(
    @SerialName("output_text") val outputText: String? = null,
    val output: List<ResponseOutput>? = null,
    val usage: ResponseUsage? = null
)

@Serializable
private data class ResponseOutput(val content: List<ResponseContent>? = null)

@Serializable
private data class ResponseContent(val text: String? = null)

@Serializable
private data class ResponseUsage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null,
    @SerialName("output_tokens_details") val outputTokensDetails: OutputTokensDetails? = null
)

@Serializable
private data class OutputTokensDetails(@SerialName("reasoning_tokens") val reasoningTokens: Int? = null)
