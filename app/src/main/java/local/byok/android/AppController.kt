package local.byok.android

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import local.byok.android.model.*
import local.byok.android.openai.OpenAIClient
import local.byok.android.storage.AppStorage
import local.byok.android.storage.KeyStore
import java.time.LocalDate

data class Attachment(
    val name: String,
    val mimeType: String?,
    val text: String,
    val byteSize: Long
)

class AppController(
    context: Context,
    private val keyStore: KeyStore,
    private val openAI: OpenAIClient
) {
    private val storage = AppStorage(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(storage.load())
    val state: StateFlow<AppState> = _state
    val apiKeyPresent = MutableStateFlow(keyStore.readApiKey() != null)
    val busy = MutableStateFlow(false)

    fun activeSession(): ChatSession = _state.value.sessions.find { it.id == _state.value.activeSessionId } ?: _state.value.sessions.first()

    fun newSession(temporary: Boolean = false) {
        val session = ChatSession(temporary = temporary, settings = _state.value.settings)
        update(_state.value.copy(sessions = _state.value.sessions + session, activeSessionId = session.id))
    }

    fun selectSession(id: String) = update(_state.value.copy(activeSessionId = id))

    fun pinSession(id: String) = updateSessions { if (it.id == id) it.copy(pinned = !it.pinned) else it }

    fun renameSession(id: String, title: String) = updateSessions { if (it.id == id) it.copy(title = title.ifBlank { "新对话" }) else it }

    fun deleteSession(id: String) {
        val current = _state.value
        if (current.sessions.size == 1) {
            updateSessions { if (it.id == id) it.copy(title = "新对话", messages = emptyList(), stats = UsageStats()) else it }
            return
        }
        val nextSessions = current.sessions.filterNot { it.id == id }
        update(current.copy(sessions = nextSessions, activeSessionId = if (current.activeSessionId == id) nextSessions.first().id else current.activeSessionId))
    }

    fun branchCurrent() {
        val current = activeSession()
        val branch = current.copy(id = java.util.UUID.randomUUID().toString(), title = "${current.title} 分支", pinned = false, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        update(_state.value.copy(sessions = _state.value.sessions + branch, activeSessionId = branch.id))
    }

    fun updateSettings(transform: (ModelSettings) -> ModelSettings) {
        val current = _state.value
        val active = activeSession()
        val next = transform(active.settings)
        update(current.copy(settings = next, sessions = current.sessions.map { if (it.id == current.activeSessionId) it.copy(settings = next) else it }))
    }

    fun addModel(model: String) {
        val trimmed = model.trim()
        if (trimmed.isBlank() || _state.value.models.contains(trimmed)) return
        val current = _state.value
        val nextSettings = activeSession().settings.copy(model = trimmed)
        update(current.copy(
            models = current.models + trimmed,
            settings = nextSettings,
            sessions = current.sessions.map { if (it.id == current.activeSessionId) it.copy(settings = nextSettings) else it }
        ))
    }

    fun saveApiKey(value: String) {
        if (value.isBlank()) return
        keyStore.saveApiKey(value)
        apiKeyPresent.value = true
    }

    fun deleteApiKey() {
        keyStore.deleteApiKey()
        apiKeyPresent.value = false
    }

    fun clearAll() {
        storage.clear()
        keyStore.deleteApiKey()
        _state.value = AppState()
        apiKeyPresent.value = false
    }

    fun acceptDisclaimer(version: Int) = update(_state.value.copy(disclaimerVersionAccepted = version))

    fun send(prompt: String, attachments: List<Attachment> = emptyList()) {
        if ((prompt.isBlank() && attachments.isEmpty()) || busy.value) return
        val apiKey = keyStore.readApiKey() ?: run {
            appendMessage(ChatMessage(role = MessageRole.Assistant, content = "还没有保存 API Key。请先在设置里填写。", error = true))
            return
        }
        val session = activeSession()
        if (session.settings.endpointUrl.isBlank()) {
            appendMessage(ChatMessage(role = MessageRole.Assistant, content = "还没有填写请求地址。请先在设置里填写完整接口地址。", error = true))
            return
        }
        if (!session.settings.endpointUrl.startsWith("https://", ignoreCase = true)) {
            appendMessage(ChatMessage(role = MessageRole.Assistant, content = "请求地址必须使用 HTTPS，已阻止非加密连接。", error = true))
            return
        }
        if (session.settings.model.isBlank()) {
            appendMessage(ChatMessage(role = MessageRole.Assistant, content = "还没有选择模型。请先在设置里选择或添加模型。", error = true))
            return
        }

        val finalPrompt = buildPromptWithAttachments(prompt.trim(), attachments)
        val effort = ReasoningPolicy.resolve(finalPrompt, session.settings)
        val displayPrompt = buildDisplayPrompt(prompt.trim(), attachments)
        val userMessage = ChatMessage(role = MessageRole.User, content = displayPrompt)
        val titleSource = prompt.trim().ifBlank { attachments.firstOrNull()?.name ?: "附件" }
        val title = if (session.title == "新对话") titleSource.take(28) else session.title
        val requestMessages = session.messages + ChatMessage(role = MessageRole.User, content = finalPrompt)

        updateSessions {
            if (it.id == session.id) it.copy(title = title, messages = it.messages + userMessage, updatedAt = System.currentTimeMillis()) else it
        }

        val placeholder = ChatMessage(
            role = MessageRole.Assistant,
            content = "正在请求 OpenAI...",
            meta = MessageMeta(
                model = session.settings.model,
                requestedReasoning = session.settings.reasoning,
                actualReasoning = effort,
                stream = false
            )
        )
        appendMessage(placeholder)
        busy.value = true
        val startedAt = System.currentTimeMillis()

        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    openAI.send(
                        apiKey,
                        session.settings.model,
                        session.settings,
                        effort,
                        buildContext(session.copy(messages = requestMessages), finalPrompt)
                    )
                }
            }.onSuccess { result ->
                val inputTokens = result.inputTokens ?: TokenEstimator.estimate(finalPrompt)
                val outputTokens = result.outputTokens ?: TokenEstimator.estimate(result.text)
                val reasoningTokens = result.reasoningTokens ?: fallbackReasoning(outputTokens, effort)
                val totalTokens = result.totalTokens ?: inputTokens + outputTokens + reasoningTokens

                replaceMessage(placeholder.id) {
                    it.copy(
                        content = result.text.ifBlank { "接口返回了空文本。" },
                        meta = it.meta?.copy(
                            inputTokens = inputTokens,
                            outputTokens = outputTokens,
                            reasoningTokens = reasoningTokens,
                            totalTokens = totalTokens,
                            latencyMs = System.currentTimeMillis() - startedAt,
                            estimated = result.totalTokens == null
                        )
                    )
                }
                if (!session.temporary) {
                    recordUsage(session.settings.model, inputTokens, outputTokens, reasoningTokens, totalTokens)
                }
            }.onFailure { error ->
                replaceMessage(placeholder.id) { it.copy(content = "请求失败：${error.readableMessage()}", error = true) }
            }
            busy.value = false
            persist()
        }
    }

    private fun buildPromptWithAttachments(prompt: String, attachments: List<Attachment>): String {
        if (attachments.isEmpty()) return prompt
        val files = attachments.joinToString("\n\n") { file ->
            """
            [附件: ${file.name}]
            MIME: ${file.mimeType ?: "unknown"}
            大小: ${file.byteSize} bytes
            内容:
            ${file.text}
            [/附件]
            """.trimIndent()
        }
        return "$prompt\n\n$files"
    }

    private fun buildDisplayPrompt(prompt: String, attachments: List<Attachment>): String {
        if (attachments.isEmpty()) return prompt
        val summary = attachments.joinToString("\n") { "已附加文件：${it.name} (${it.byteSize} bytes)" }
        return listOf(prompt, summary).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun Throwable.readableMessage(): String {
        val text = message?.takeIf { it.isNotBlank() }
        return text ?: this::class.simpleName ?: "未知错误"
    }

    private fun buildContext(session: ChatSession, prompt: String): List<ChatMessage> {
        val keepCount = session.settings.contextTurns.messageCount
        val limit = resolvedContextLimit(session.settings.model, session.settings.tokenLimit)
        val history = keepCount?.let { session.messages.takeLast(it) } ?: session.messages
        val selected = history.toMutableList()
        while (selected.isNotEmpty() && (selected.sumOf { TokenEstimator.estimate(it.content) } + TokenEstimator.estimate(prompt)) > limit) {
            selected.removeAt(0)
        }
        return selected
    }

    private fun resolvedContextLimit(model: String, tokenLimit: TokenLimit): Int {
        tokenLimit.tokens?.let { return it }
        val name = model.lowercase()
        return when {
            name.contains("gpt-5.5") || name.contains("gpt-5.4") -> 1_000_000
            name.contains("gpt-5") -> 400_000
            else -> 128_000
        }
    }

    private fun fallbackReasoning(outputTokens: Int, effort: ReasoningEffort): Int = when (effort) {
        ReasoningEffort.None -> 0
        ReasoningEffort.Low -> (outputTokens * 0.15).toInt()
        ReasoningEffort.Medium -> (outputTokens * 0.35).toInt()
        ReasoningEffort.High -> (outputTokens * 0.7).toInt()
        ReasoningEffort.XHigh -> (outputTokens * 1.1).toInt()
    }

    private fun recordUsage(model: String, input: Int, output: Int, reasoning: Int, total: Int) {
        val date = LocalDate.now().toString()
        val usage = _state.value.usage.toMutableList()
        val index = usage.indexOfFirst { it.date == date && it.model == model }
        val row = if (index >= 0) usage[index] else UsageDay(date = date, model = model)
        val nextRow = row.copy(
            requests = row.requests + 1,
            inputTokens = row.inputTokens + input,
            outputTokens = row.outputTokens + output,
            reasoningTokens = row.reasoningTokens + reasoning,
            totalTokens = row.totalTokens + total
        )
        if (index >= 0) usage[index] = nextRow else usage += nextRow

        val activeId = _state.value.activeSessionId
        val sessions = _state.value.sessions.map { session ->
            if (session.id == activeId) {
                val stats = session.stats
                session.copy(stats = stats.copy(
                    requests = stats.requests + 1,
                    inputTokens = stats.inputTokens + input,
                    outputTokens = stats.outputTokens + output,
                    reasoningTokens = stats.reasoningTokens + reasoning,
                    totalTokens = stats.totalTokens + total,
                    lastTokens = total
                ))
            } else session
        }
        update(_state.value.copy(sessions = sessions, usage = usage))
    }

    private fun appendMessage(message: ChatMessage) = updateSessions {
        if (it.id == _state.value.activeSessionId) it.copy(messages = it.messages + message, updatedAt = System.currentTimeMillis()) else it
    }

    private fun replaceMessage(id: String, transform: (ChatMessage) -> ChatMessage) = updateSessions {
        it.copy(messages = it.messages.map { message -> if (message.id == id) transform(message) else message })
    }

    private fun updateSessions(transform: (ChatSession) -> ChatSession) = update(_state.value.copy(sessions = _state.value.sessions.map(transform)))

    private fun update(next: AppState) {
        _state.value = next
        persist()
    }

    private fun persist() {
        if (_state.value.settings.saveHistory) {
            storage.save(
                _state.value.copy(
                    sessions = _state.value.sessions.filterNot { it.temporary },
                    activeSessionId = _state.value.sessions.firstOrNull { !it.temporary }?.id ?: _state.value.activeSessionId
                )
            )
        }
    }
}
