package local.byok.android

import android.os.Bundle
import android.provider.OpenableColumns
import android.content.Context
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextFieldDefaults
import local.byok.android.model.AppState
import local.byok.android.model.AppTheme
import local.byok.android.model.ChatMessage
import local.byok.android.model.ChatSession
import local.byok.android.model.CompatibilityMode
import local.byok.android.model.ContextTurns
import local.byok.android.model.MessageRole
import local.byok.android.model.ReasoningChoice
import local.byok.android.model.TokenEstimator
import local.byok.android.model.TokenLimit
import local.byok.android.openai.OpenAIClient
import local.byok.android.storage.SecureKeyStore
import local.byok.android.Attachment
import java.nio.charset.Charset
import kotlinx.coroutines.delay

private data class AppColors(
    val bg: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val userBubble: Color,
    val error: Color
)

private fun colors(theme: AppTheme) = if (theme == AppTheme.Dark) {
    AppColors(
        bg = Color(0xff171717),
        panel = Color(0xff242424),
        ink = Color(0xfff4f4f4),
        muted = Color(0xffa3a3a3),
        line = Color(0xff3a3a3a),
        accent = Color(0xff10a37f),
        userBubble = Color(0xff2f2f2f),
        error = Color(0xffff8a80)
    )
} else {
    AppColors(
        bg = Color(0xfffaf9f6),
        panel = Color.White,
        ink = Color(0xff1f1f1f),
        muted = Color(0xff737373),
        line = Color(0xffe3e1dc),
        accent = Color(0xff0f766e),
        userBubble = Color(0xfff1f5f3),
        error = Color(0xffb4534b)
    )
}

private val LocalAppColors = staticCompositionLocalOf { colors(AppTheme.Light) }
private val Bg: Color @Composable get() = LocalAppColors.current.bg
private val Panel: Color @Composable get() = LocalAppColors.current.panel
private val Ink: Color @Composable get() = LocalAppColors.current.ink
private val Muted: Color @Composable get() = LocalAppColors.current.muted
private val Line: Color @Composable get() = LocalAppColors.current.line
private val Accent: Color @Composable get() = LocalAppColors.current.accent
private val UserBubble: Color @Composable get() = LocalAppColors.current.userBubble
private val ErrorTone: Color @Composable get() = LocalAppColors.current.error

private val OutlineSendIcon: ImageVector = ImageVector.Builder(
    name = "OutlineSend",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).path(
    fill = SolidColor(Color.Transparent),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
) {
    moveTo(22f, 2f)
    lineTo(11f, 13f)
    moveTo(22f, 2f)
    lineTo(15f, 22f)
    lineTo(11f, 13f)
    lineTo(2f, 9f)
    close()
}.build()

private const val MaxAttachmentCount = 3
private const val MaxAttachmentBytes = 512 * 1024
private const val CurrentDisclaimerVersion = 2
private const val DisclaimerWaitSeconds = 15

private data class PendingAttachment(
    val name: String,
    val mimeType: String?,
    val text: String,
    val byteSize: Long
) {
    fun toControllerAttachment() = Attachment(name, mimeType, text, byteSize)
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val controller = remember {
                AppController(context, SecureKeyStore(context), OpenAIClient())
            }
            val state by controller.state.collectAsState()
            val busy by controller.busy.collectAsState()
            val apiKeyPresent by controller.apiKeyPresent.collectAsState()
            val active = state.sessions.find { it.id == state.activeSessionId } ?: state.sessions.first()
            var showSettings by remember { mutableStateOf(false) }
            var showSessions by remember { mutableStateOf(false) }
            var renameTarget by remember { mutableStateOf<ChatSession?>(null) }
            var renameText by remember { mutableStateOf("") }
            var deleteTarget by remember { mutableStateOf<ChatSession?>(null) }

            CompositionLocalProvider(LocalAppColors provides colors(active.settings.theme)) {
            MaterialTheme {
                Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = {
                                Text(
                                    active.title.ifBlank { "新对话" },
                                    color = Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            navigationIcon = {
                                TextButton(onClick = { showSessions = true }) {
                                    Text("历史", color = Muted)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                            actions = {
                                IconButton(onClick = { controller.newSession() }) {
                                    Icon(Icons.Default.Add, contentDescription = "新对话")
                                }
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "设置")
                                }
                            }
                        )

                        ChatArea(active, busy, controller)
                    }
                }

                if (showSettings) {
                    SecureScreenEffect()
                    SettingsSheet(active, state, apiKeyPresent, controller) {
                        showSettings = false
                    }
                }

                if (showSessions) {
                    SessionsSheet(
                        sessions = state.sessions,
                        activeId = active.id,
                        controller = controller,
                        onRename = {
                            renameTarget = it
                            renameText = it.title
                        },
                        onDelete = { deleteTarget = it },
                        onDismiss = { showSessions = false }
                    )
                }

                if (state.disclaimerVersionAccepted < CurrentDisclaimerVersion) {
                    DisclaimerDialog(
                        onAgree = { controller.acceptDisclaimer(CurrentDisclaimerVersion) },
                        onDisagree = { this@MainActivity.finish() }
                    )
                }

                renameTarget?.let { session ->
                    AlertDialog(
                        onDismissRequest = { renameTarget = null },
                        confirmButton = {
                            Button(onClick = {
                                controller.renameSession(session.id, renameText)
                                renameTarget = null
                            }) { Text("保存") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { renameTarget = null }) { Text("取消") }
                        },
                        title = { Text("重命名会话") },
                        text = { OutlinedTextField(renameText, { renameText = it }, singleLine = true) }
                    )
                }

                deleteTarget?.let { session ->
                    AlertDialog(
                        onDismissRequest = { deleteTarget = null },
                        confirmButton = {
                            Button(onClick = {
                                controller.deleteSession(session.id)
                                deleteTarget = null
                            }) { Text("删除") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { deleteTarget = null }) { Text("取消") }
                        },
                        title = { Text("删除会话") },
                        text = { Text("确定删除「${session.title}」？") }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun SecureScreenEffect() {
    val activity = LocalContext.current as? ComponentActivity ?: return
    DisposableEffect(activity) {
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
private fun ChatArea(session: ChatSession, busy: Boolean, controller: AppController) {
    val context = LocalContext.current
    var prompt by remember(session.id) { mutableStateOf("") }
    var attachments by remember(session.id) { mutableStateOf<List<PendingAttachment>>(emptyList()) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    var showFileAccessNotice by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val next = attachments.toMutableList()
        attachmentError = null
        uris.take(MaxAttachmentCount - next.size).forEach { uri ->
            runCatching { context.readAttachment(uri) }
                .onSuccess { next += it }
                .onFailure { attachmentError = it.message ?: "文件读取失败" }
        }
        if (uris.size > MaxAttachmentCount - attachments.size) {
            attachmentError = "最多只能添加 ${MaxAttachmentCount} 个文件"
        }
        attachments = next
    }
    val estimate = session.messages
        .takeLast(session.settings.contextTurns.messageCount ?: session.messages.size)
        .sumOf { TokenEstimator.estimate(it.content) } +
        TokenEstimator.estimate(prompt) +
        attachments.sumOf { TokenEstimator.estimate(it.text) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (session.messages.isEmpty()) {
                item { EmptyState(session.settings.model.ifBlank { "未选择模型" }) }
            }
            items(session.messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        Box(Modifier.imePadding()) {
            ComposerBar(
                prompt = prompt,
                onPromptChange = { prompt = it },
                busy = busy,
                estimate = estimate,
                limitLabel = session.settings.tokenLimit.label,
                attachments = attachments,
                attachmentError = attachmentError,
                onAttach = {
                    if (attachments.size >= MaxAttachmentCount) {
                        attachmentError = "最多只能添加 ${MaxAttachmentCount} 个文件"
                    } else {
                        showFileAccessNotice = true
                    }
                },
                onRemoveAttachment = { target ->
                    attachments = attachments.filterNot { it === target }
                    attachmentError = null
                },
                onBranch = { controller.branchCurrent() },
                onSend = {
                    controller.send(prompt, attachments.map { it.toControllerAttachment() })
                    prompt = ""
                    attachments = emptyList()
                    attachmentError = null
                }
            )
        }
    }

    if (showFileAccessNotice) {
        FileAccessNoticeDialog(
            onDismiss = { showFileAccessNotice = false },
            onContinue = {
                showFileAccessNotice = false
                picker.launch(arrayOf("*/*"))
            }
        )
    }
}

@Composable
private fun FileAccessNoticeDialog(onDismiss: () -> Unit, onContinue: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择文件权限说明") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("下一步将打开 Android 系统文件选择器。文件访问边界由系统界面决定：你选择哪个文件，本应用只能读取那个文件。")
                Text("本应用不会申请读取全部文件、照片或外部存储的长期权限；不会在后台扫描文件。")
                Text("请选择确认允许发送给 AI 服务的文本文件。不要选择涉密、敏感、隐私或工作内部文件。")
            }
        },
        confirmButton = {
            Button(onClick = onContinue) { Text("打开系统选择器") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EmptyState(model: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("有什么可以帮忙的？", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(model, color = Muted, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComposerBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    busy: Boolean,
    estimate: Int,
    limitLabel: String,
    attachments: List<PendingAttachment>,
    attachmentError: String?,
    onAttach: () -> Unit,
    onRemoveAttachment: (PendingAttachment) -> Unit,
    onBranch: () -> Unit,
    onSend: () -> Unit
) {
    Surface(color = Bg, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("上下文 ${formatToken(estimate)} / $limitLabel", color = Muted, fontSize = 11.sp)
                TextButton(onClick = onBranch) { Text("分支") }
            }
            if (attachments.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEach { file ->
                        AttachmentPill(file, onRemove = { onRemoveAttachment(file) })
                    }
                }
            }
            attachmentError?.let {
                Text(it, color = ErrorTone, fontSize = 12.sp)
            }
            Text("文件由系统选择器提供；本应用只读取你选中的文本文件。", color = Muted, fontSize = 11.sp)
            Surface(color = Panel, shape = RoundedCornerShape(24.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                    BasicTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 16.sp),
                        minLines = 1,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (prompt.isBlank()) {
                        Text("询问任何问题", color = Muted, fontSize = 16.sp)
                    }
                    }
                    IconButton(onClick = onAttach, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.AttachFile, contentDescription = "添加文件", tint = Muted, modifier = Modifier.size(20.dp))
                    }
                    Button(
                        enabled = !busy && (prompt.isNotBlank() || attachments.isNotEmpty()),
                        onClick = onSend,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(OutlineSendIcon, contentDescription = "发送", modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPill(file: PendingAttachment, onRemove: () -> Unit) {
    Surface(
        color = Accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(file.name, color = Ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "移除文件", tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DisclaimerDialog(onAgree: () -> Unit, onDisagree: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(DisclaimerWaitSeconds) }
    val scrollState = rememberScrollState()
    val readToEnd = scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue
    val canAct = secondsLeft == 0 && readToEnd

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("使用声明与风险告知") },
        text = {
            Column(
                Modifier
                    .height(360.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("请完整阅读。本声明用于说明本软件的数据边界、使用风险和你的责任。继续使用即表示你已理解并同意。", fontWeight = FontWeight.SemiBold)
                Text("1. 软件性质：本软件是本地 BYOK 客户端，不自带 AI 云服务，不提供模型服务，不承诺第三方接口的可用性、准确性、合规性或数据处理方式。")
                Text("2. 本地数据：API Key 使用 Android Keystore/EncryptedSharedPreferences 保存；普通设置和会话历史保存在本机应用私有目录。软件开发者不主动收集、上传、同步或保存你的 API Key、聊天内容、附件、请求地址、模型配置或个人隐私。")
                Text("3. 第三方服务：你手动填写的请求地址、API Key、聊天内容和附件内容会发送给对应 AI 服务或中转服务。请自行确认服务提供方身份、数据政策、跨境传输、日志留存、账号权限、费用和合规要求。")
                Text("4. 文件与附件：文件由 Android 系统选择器授权，本应用只读取你明确选择的文件；附件正文仅用于本次请求，历史记录只保存文件名和大小摘要。不要选择不应上传的文件。")
                Text("5. 禁止上传：不得上传国家秘密、工作秘密、商业秘密、内部资料、未公开数据、源代码机密、合同、财务、医疗、教育、身份证件、账号密码、密钥、证书、个人隐私、客户资料、受版权或保密协议限制的内容。")
                Text("6. 特殊职业：公职人员、涉密岗位、军工、政务、金融、医疗、教育、科研、能源、通信、法律、企业核心岗位等用户，应仅在合规设备、合规网络和合规场景使用。请勿使用境外 AI 或未经单位批准的第三方服务处理工作材料、内部材料或敏感文件。")
                Text("7. 网络与安全：本软件阻止非 HTTPS 请求地址，但无法保证第三方接口不会记录、转发、训练或泄露数据。恶意输入法、剪贴板、无障碍服务、屏幕录制、root/越狱环境、系统漏洞或被劫持网络仍可能造成泄露。")
                Text("8. 输出风险：AI 输出可能错误、过时、虚构、带偏见或不适用。不要把输出直接用于医疗、法律、金融、政务、安全生产、考试、招聘、处罚、授信等高风险决策。")
                Text("9. 费用与账号：调用第三方接口可能产生费用、限额、封号或合规风险。你应自行管理 API Key、余额、权限和请求地址。")
                Text("10. 未成年人：未成年人应在监护人指导下使用，不得上传本人或他人的隐私、照片、身份信息、学校资料或敏感内容。")
                Text("11. 责任边界：你对输入内容、选择的服务、上传文件、输出使用方式及其后果承担责任。若不同意本声明，请不要继续使用本程序。")
                Text("请滚动到末尾并等待倒计时结束后再选择。", color = Muted)
            }
        },
        confirmButton = {
            Button(enabled = canAct, onClick = onAgree) {
                Text(if (secondsLeft > 0) "请等待 ${secondsLeft}s" else "同意并继续")
            }
        },
        dismissButton = {
            OutlinedButton(enabled = canAct, onClick = onDisagree) {
                Text("不同意，退出")
            }
        }
    )
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.User
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier.fillMaxWidth(if (isUser) 0.82f else 0.94f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isUser) {
                Surface(color = UserBubble, shape = RoundedCornerShape(18.dp)) {
                    Text(message.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Ink, fontSize = 16.sp)
                }
            } else {
                Text(message.content, color = if (message.error) ErrorTone else Ink, fontSize = 16.sp, lineHeight = 23.sp)
            }
            MessageMetaText(message)
        }
    }
}

@Composable
private fun MessageMetaText(message: ChatMessage) {
    val meta = message.meta
    val text = if (meta == null) {
        "约 ${TokenEstimator.estimate(message.content)} token"
    } else {
        "${meta.model} · ${meta.actualReasoning.apiValue} · 入 ${meta.inputTokens} · 出 ${meta.outputTokens} · 总 ${meta.totalTokens}${if (meta.estimated) " · 估算" else ""}"
    }
    Text(text, color = Muted, fontSize = 11.sp, maxLines = 2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsSheet(
    sessions: List<ChatSession>,
    activeId: String,
    controller: AppController,
    onRename: (ChatSession) -> Unit,
    onDelete: (ChatSession) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Bg) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("历史", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            sessions.forEach { session ->
                SessionRow(session, selected = session.id == activeId, controller, onRename, onDelete, onDismiss)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSession,
    selected: Boolean,
    controller: AppController,
    onRename: (ChatSession) -> Unit,
    onDelete: (ChatSession) -> Unit,
    onDismiss: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                controller.selectSession(session.id)
                onDismiss()
            }
            .background(if (selected) Color(0xffecefed) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (session.pinned) "置顶 · ${session.title}" else session.title,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "会话操作", tint = Muted)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(if (session.pinned) "取消置顶" else "置顶") }, leadingIcon = {
                    Icon(Icons.Default.PushPin, contentDescription = null)
                }, onClick = {
                    controller.pinSession(session.id)
                    menuOpen = false
                })
                DropdownMenuItem(text = { Text("重命名") }, leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }, onClick = {
                    onRename(session)
                    menuOpen = false
                })
                DropdownMenuItem(text = { Text("删除") }, leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }, onClick = {
                    onDelete(session)
                    menuOpen = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheet(
    active: ChatSession,
    state: AppState,
    apiKeyPresent: Boolean,
    controller: AppController,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var apiKeyStatus by remember { mutableStateOf(if (apiKeyPresent) "API Key 已保存" else "尚未保存 API Key") }
    var endpointUrl by remember(active.id, active.settings.endpointUrl) { mutableStateOf(active.settings.endpointUrl) }
    var customModel by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Bg) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("设置", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            SettingsSection("外观") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Panel)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("黑色主题", color = Ink, fontSize = 16.sp)
                        Text(if (active.settings.theme == AppTheme.Dark) "当前：黑色" else "当前：白色", color = Muted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = active.settings.theme == AppTheme.Dark,
                        onCheckedChange = { checked ->
                            controller.updateSettings { it.copy(theme = if (checked) AppTheme.Dark else AppTheme.Light) }
                        }
                    )
                }
            }
            SettingsSection("连接") {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text(if (apiKeyPresent) "已保存，输入新 Key 可覆盖" else "粘贴 sk-...") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "隐藏 API Key" else "显示 API Key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = apiKey.isNotBlank(),
                        onClick = {
                            controller.saveApiKey(apiKey)
                            apiKey = ""
                            apiKeyStatus = "已保存 API Key"
                        }
                    ) { Text("保存") }
                    OutlinedButton(onClick = {
                        controller.deleteApiKey()
                        apiKey = ""
                        apiKeyStatus = "已删除 API Key"
                    }) { Text("删除") }
                }
                Text(apiKeyStatus, color = Muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("请求地址") },
                    placeholder = { Text("手动填写完整接口地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )
                Text("若不知晓请求地址，可查询对应AI厂商或你选择的中转站", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val next = endpointUrl.trim()
                        endpointUrl = next
                        controller.updateSettings { it.copy(endpointUrl = next) }
                    }) { Text("保存地址") }
                    OutlinedButton(onClick = {
                        endpointUrl = ""
                        controller.updateSettings { it.copy(endpointUrl = "") }
                    }) { Text("清空") }
                }
                Text("接口兼容模式", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompatibilityMode.entries.forEach { mode ->
                        ChoicePill(mode.label, active.settings.compatibilityMode == mode) {
                            controller.updateSettings { it.copy(compatibilityMode = mode) }
                        }
                    }
                }
                Text(active.settings.compatibilityMode.description, color = Muted, fontSize = 12.sp)
            }

            SettingsSection("模型") {
                Text(
                    if (active.settings.model.isBlank()) "当前未选择模型" else "当前模型：${active.settings.model}",
                    color = Muted,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    label = { Text("添加自定义模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.models.forEach { model ->
                        ChoicePill(model, active.settings.model == model) {
                            controller.updateSettings { it.copy(model = model) }
                        }
                    }
                    ChoicePill("不选择", active.settings.model.isBlank()) {
                        controller.updateSettings { it.copy(model = "") }
                    }
                    OutlinedButton(onClick = {
                        controller.addModel(customModel)
                        customModel = ""
                    }) { Text("添加") }
                }
            }

            SettingsSection("推理强度") {
                if (active.settings.compatibilityMode != CompatibilityMode.OpenAIGPT) {
                    Text("当前兼容模式不会发送推理强度。", color = Muted, fontSize = 12.sp)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReasoningChoice.entries.forEach { choice ->
                        ChoicePill(choice.label, active.settings.reasoning == choice) {
                            controller.updateSettings { it.copy(reasoning = choice) }
                        }
                    }
                }
            }

            SettingsSection("上下文") {
                Text("消息轮数", color = Muted, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContextTurns.entries.forEach { turns ->
                        ChoicePill(turns.label, active.settings.contextTurns == turns) {
                            controller.updateSettings { it.copy(contextTurns = turns) }
                        }
                    }
                }
                Text("Token 上限", color = Muted, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TokenLimit.entries.forEach { limit ->
                        ChoicePill(limit.label, active.settings.tokenLimit == limit) {
                            controller.updateSettings { it.copy(tokenLimit = limit) }
                        }
                    }
                }
            }

            SettingsSection("生成") {
                if (active.settings.compatibilityMode == CompatibilityMode.Minimal) {
                    Text("最小模式不会发送 temperature、top_p、max_tokens 等生成参数。", color = Muted, fontSize = 12.sp)
                } else if (active.settings.compatibilityMode == CompatibilityMode.Generic) {
                    Text("通用模式会发送 temperature、top_p、max_tokens，不发送惩罚参数。", color = Muted, fontSize = 12.sp)
                }
                Text("temperature ${"%.1f".format(active.settings.temperature)}", color = Muted, fontSize = 12.sp)
                Slider(
                    value = active.settings.temperature.toFloat(),
                    onValueChange = { controller.updateSettings { settings -> settings.copy(temperature = it.toDouble()) } },
                    valueRange = 0f..2f
                )
                Text("max_output_tokens ${active.settings.maxOutputTokens}", color = Muted, fontSize = 12.sp)
                Slider(
                    value = active.settings.maxOutputTokens.coerceIn(256, 32768) / 32768f,
                    onValueChange = {
                        controller.updateSettings { settings -> settings.copy(maxOutputTokens = (it * 32768).toInt().coerceAtLeast(256)) }
                    }
                )
            }

            SettingsSection("数据") {
                OutlinedButton(onClick = { controller.clearAll() }) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("清空本地数据")
                }
            }
            SettingsSection("关于") {
                Text("版本号：V1.1.4", color = Muted, fontSize = 12.sp)
                Text("作者：DDxfy", color = Muted, fontSize = 12.sp)
                Text("GitHub：https://github.com/DDxfy/GPT-FIG", color = Muted, fontSize = 12.sp)
                Text("联系方式：3050172393@qq.com", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScopeContent.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Divider(color = Line)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ColumnScopeContent.content()
        }
    }
}

private object ColumnScopeContent

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedLabelColor = Accent,
    unfocusedLabelColor = Muted,
    focusedPlaceholderColor = Muted,
    unfocusedPlaceholderColor = Muted,
    cursorColor = Accent,
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

@Composable
private fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Accent.copy(alpha = 0.22f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) Accent else Line),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
            }
            Text(text, color = Ink, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

private fun formatToken(value: Int): String = when {
    value >= 1_000_000 -> "${(value / 100_000.0).toInt() / 10.0}M"
    value >= 1000 -> "${(value / 100.0).toInt() / 10.0}k"
    else -> value.toString()
}

private fun Context.readAttachment(uri: Uri): PendingAttachment {
    val resolver = contentResolver
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: "attachment"
    val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst() && sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
    } ?: -1L
    if (size > MaxAttachmentBytes) {
        throw IllegalArgumentException("文件过大：${name}，单文件上限 512KB")
    }
    val mime = resolver.getType(uri)
    if (mime == "application/pdf") {
        throw IllegalArgumentException("暂不解析 PDF，请先转为 txt/markdown 再上传")
    }
    if (!isTextLike(name, mime)) {
        throw IllegalArgumentException("暂只支持文本类文件：txt、md、json、xml、csv、log、代码文件")
    }
    val bytes = resolver.openInputStream(uri)?.use { stream ->
        stream.readBytes(MaxAttachmentBytes + 1)
    } ?: throw IllegalArgumentException("无法读取文件")
    if (bytes.size > MaxAttachmentBytes) {
        throw IllegalArgumentException("文件过大：${name}，单文件上限 512KB")
    }
    val text = bytes.toString(Charset.forName("UTF-8"))
    return PendingAttachment(name = name, mimeType = mime, text = text.take(200_000), byteSize = if (size >= 0) size else bytes.size.toLong())
}

private fun java.io.InputStream.readBytes(limit: Int): ByteArray {
    val buffer = ByteArray(8192)
    val out = java.io.ByteArrayOutputStream()
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        if (total > limit) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

private fun isTextLike(name: String, mime: String?): Boolean {
    if (mime?.startsWith("text/") == true) return true
    if (mime in setOf("application/json", "application/xml", "application/javascript")) return true
    val lower = name.lowercase()
    return listOf(".txt", ".md", ".json", ".xml", ".csv", ".log", ".kt", ".java", ".js", ".ts", ".py", ".html", ".css", ".yaml", ".yml").any { lower.endsWith(it) }
}
