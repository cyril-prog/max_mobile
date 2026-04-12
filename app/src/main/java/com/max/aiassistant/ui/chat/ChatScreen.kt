package com.max.aiassistant.ui.chat

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.max.aiassistant.data.chat.ChatMarkdownFormatter
import com.max.aiassistant.data.local.OnDeviceAiSettings
import com.max.aiassistant.data.local.OnDeviceModelVariant
import com.max.aiassistant.data.local.OnDeviceModelProvisioningState
import com.max.aiassistant.data.local.SUPPORTED_MAX_CONTEXT_TOKENS
import com.max.aiassistant.data.local.db.ChatConversationEntity
import com.max.aiassistant.model.Message
import com.max.aiassistant.ui.common.ErrorStateView
import com.max.aiassistant.ui.common.InlineStatusBanner
import com.max.aiassistant.ui.common.LoadingStateView
import com.max.aiassistant.ui.common.BannerTone
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.theme.AccentBlue
import com.max.aiassistant.ui.theme.DarkBackground
import com.max.aiassistant.ui.theme.DarkSurface
import com.max.aiassistant.ui.theme.DarkSurfaceVariant
import com.max.aiassistant.ui.theme.Spacing
import com.max.aiassistant.ui.theme.TextPrimary
import com.max.aiassistant.ui.theme.TextSecondary
import com.max.aiassistant.ui.theme.UserMessageBg
import kotlinx.coroutines.launch
import java.io.File

private val ChatBackdrop = listOf(
    Color(0xFF05070C),
    Color(0xFF0A0F18),
    Color(0xFF0C131D)
)

private val EditorialPanel = Color(0xFF121A27)
private val EditorialPanelBorder = Color(0xFF25324A)
private val EditorialPanelMuted = Color(0xFF1A2331)
private val MaxBubbleColor = Color(0xFF151E2D)
private val UserBubbleColor = Color(0xFF114A7A)
private val ComposerShell = Color(0xFF0E141F)
private val ComposerField = Color(0xFF172131)
private val ComposerAction = Color(0xFF1D2A3D)

private enum class ChatContentPane {
    CONVERSATION,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    conversations: List<ChatConversationEntity>,
    currentConversationId: String?,
    conversationTitle: String,
    onDeviceAiSettings: OnDeviceAiSettings,
    isWaitingForAiResponse: Boolean,
    isOnDeviceModelReady: Boolean,
    isConversationLimitReached: Boolean,
    onDeviceModelStatus: String,
    onDeviceModelProvisioningState: OnDeviceModelProvisioningState,
    onSendMessage: (String, Uri?) -> Unit,
    onStartNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onUpdateOnDeviceAiSettings: (OnDeviceModelVariant, Int, String) -> Unit,
    onOpenMainSidebar: () -> Unit,
    onRetryModelDownload: () -> Unit,
    onVoiceInput: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToActu: () -> Unit = {},
    initialText: String = "",
    onInitialTextConsumed: () -> Unit = {},
    showChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (showChrome) {
        val sidebarState = rememberNavigationSidebarState()
        NavigationSidebarScaffold(
            currentScreen = NavigationScreen.CHAT,
            onNavigateToScreen = { screen ->
                when (screen) {
                    NavigationScreen.HOME -> onNavigateToHome()
                    NavigationScreen.VOICE -> onVoiceInput()
                    NavigationScreen.CHAT -> Unit
                    NavigationScreen.TASKS -> onNavigateToTasks()
                    NavigationScreen.PLANNING -> onNavigateToPlanning()
                    NavigationScreen.WEATHER -> onNavigateToWeather()
                    NavigationScreen.NOTES -> onNavigateToNotes()
                    NavigationScreen.ACTU -> onNavigateToActu()
                }
            },
            sidebarState = sidebarState
        ) {
            ChatScreenContent(
                messages = messages,
                conversations = conversations,
                currentConversationId = currentConversationId,
                conversationTitle = conversationTitle,
                onDeviceAiSettings = onDeviceAiSettings,
                isWaitingForAiResponse = isWaitingForAiResponse,
                isOnDeviceModelReady = isOnDeviceModelReady,
                isConversationLimitReached = isConversationLimitReached,
                onDeviceModelStatus = onDeviceModelStatus,
                onDeviceModelProvisioningState = onDeviceModelProvisioningState,
                onSendMessage = onSendMessage,
                onStartNewConversation = onStartNewConversation,
                onSelectConversation = onSelectConversation,
                onRenameConversation = onRenameConversation,
                onDeleteConversation = onDeleteConversation,
                onUpdateOnDeviceAiSettings = onUpdateOnDeviceAiSettings,
                onOpenMainSidebar = onOpenMainSidebar,
                onRetryModelDownload = onRetryModelDownload,
                onVoiceInput = onVoiceInput,
                showChrome = true,
                initialText = initialText,
                onInitialTextConsumed = onInitialTextConsumed,
                modifier = modifier
            )
        }
    } else {
        ChatScreenContent(
            messages = messages,
            conversations = conversations,
            currentConversationId = currentConversationId,
            conversationTitle = conversationTitle,
            onDeviceAiSettings = onDeviceAiSettings,
            isWaitingForAiResponse = isWaitingForAiResponse,
            isOnDeviceModelReady = isOnDeviceModelReady,
            isConversationLimitReached = isConversationLimitReached,
            onDeviceModelStatus = onDeviceModelStatus,
            onDeviceModelProvisioningState = onDeviceModelProvisioningState,
            onSendMessage = onSendMessage,
            onStartNewConversation = onStartNewConversation,
            onSelectConversation = onSelectConversation,
            onRenameConversation = onRenameConversation,
            onDeleteConversation = onDeleteConversation,
            onUpdateOnDeviceAiSettings = onUpdateOnDeviceAiSettings,
            onOpenMainSidebar = onOpenMainSidebar,
            onRetryModelDownload = onRetryModelDownload,
            onVoiceInput = onVoiceInput,
            showChrome = false,
            initialText = initialText,
            onInitialTextConsumed = onInitialTextConsumed,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    messages: List<Message>,
    conversations: List<ChatConversationEntity>,
    currentConversationId: String?,
    conversationTitle: String,
    onDeviceAiSettings: OnDeviceAiSettings,
    isWaitingForAiResponse: Boolean,
    isOnDeviceModelReady: Boolean,
    isConversationLimitReached: Boolean,
    onDeviceModelStatus: String,
    onDeviceModelProvisioningState: OnDeviceModelProvisioningState,
    onSendMessage: (String, Uri?) -> Unit,
    onStartNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onUpdateOnDeviceAiSettings: (OnDeviceModelVariant, Int, String) -> Unit,
    onOpenMainSidebar: () -> Unit,
    onRetryModelDownload: () -> Unit,
    onVoiceInput: () -> Unit,
    showChrome: Boolean,
    initialText: String,
    onInitialTextConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val conversationDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var conversationOptionsTarget by remember { mutableStateOf<ChatConversationEntity?>(null) }
    var conversationRenameTarget by remember { mutableStateOf<ChatConversationEntity?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var currentPane by remember { mutableStateOf(ChatContentPane.CONVERSATION) }
    var settingsModelVariant by remember { mutableStateOf(onDeviceAiSettings.modelVariant) }
    var settingsMaxTokens by remember { mutableStateOf(onDeviceAiSettings.maxContextTokens) }
    var settingsSystemPrompt by remember { mutableStateOf(onDeviceAiSettings.systemPrompt) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = tempCameraUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(initialText) {
        if (initialText.isNotBlank()) {
            messageText = initialText
            onInitialTextConsumed()
        }
    }

    LaunchedEffect(onDeviceAiSettings) {
        settingsModelVariant = onDeviceAiSettings.modelVariant
        settingsMaxTokens = onDeviceAiSettings.maxContextTokens
        settingsSystemPrompt = onDeviceAiSettings.systemPrompt
    }

    LaunchedEffect(messages.size, isWaitingForAiResponse) {
        val itemCount = messages.size + if (isWaitingForAiResponse) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = conversationDrawerState,
        gesturesEnabled = conversations.isNotEmpty(),
        drawerContent = {
            ConversationDrawerSheet(
                conversations = conversations,
                currentConversationId = currentConversationId,
                onSelectConversation = { conversationId ->
                    currentPane = ChatContentPane.CONVERSATION
                    onSelectConversation(conversationId)
                    scope.launch { conversationDrawerState.close() }
                },
                onConversationLongPress = { conversation ->
                    conversationOptionsTarget = conversation
                },
                onOpenSettings = {
                    currentPane = ChatContentPane.SETTINGS
                    scope.launch { conversationDrawerState.close() }
                },
                onOpenMainSidebar = {
                    scope.launch {
                        conversationDrawerState.close()
                        onOpenMainSidebar()
                    }
                },
                modifier = if (showChrome) Modifier.statusBarsPadding() else Modifier
            )
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(ChatBackdrop))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ConversationTopBar(
                    title = if (currentPane == ChatContentPane.SETTINGS) "Parametres IA" else conversationTitle,
                    onOpenSidebar = {
                        scope.launch { conversationDrawerState.open() }
                    },
                    actionLabel = if (currentPane == ChatContentPane.SETTINGS) "Fermer" else "Nouveau",
                    onActionClick = {
                        if (currentPane == ChatContentPane.SETTINGS) {
                            currentPane = ChatContentPane.CONVERSATION
                        } else {
                            onStartNewConversation()
                        }
                    }
                )

                if (currentPane == ChatContentPane.CONVERSATION && isConversationLimitReached) {
                    InlineStatusBanner(
                        title = "Conversation trop longue",
                        subtitle = "Les nouveaux messages sont bloques pour garder un chat local stable. Ouvre une nouvelle conversation.",
                        tone = BannerTone.Warning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }

                if (currentPane == ChatContentPane.SETTINGS) {
                    ChatSettingsScreen(
                        currentSettings = onDeviceAiSettings,
                        selectedModelVariant = settingsModelVariant,
                        onModelVariantSelected = { settingsModelVariant = it },
                        selectedMaxTokens = settingsMaxTokens,
                        onMaxTokensSelected = { settingsMaxTokens = it },
                        systemPrompt = settingsSystemPrompt,
                        onSystemPromptChange = { settingsSystemPrompt = it },
                        isOnDeviceModelReady = isOnDeviceModelReady,
                        onDeviceModelStatus = onDeviceModelStatus,
                        onDeviceModelProvisioningState = onDeviceModelProvisioningState,
                        onRetryModelDownload = onRetryModelDownload,
                        onSave = {
                            onUpdateOnDeviceAiSettings(
                                settingsModelVariant,
                                settingsMaxTokens,
                                settingsSystemPrompt
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else if (!isOnDeviceModelReady && messages.isEmpty() && !isWaitingForAiResponse) {
                    ModelProvisioningView(
                        state = onDeviceModelProvisioningState,
                        onRetry = onRetryModelDownload,
                        modifier = Modifier.weight(1f)
                    )
                } else if (messages.isEmpty() && !isWaitingForAiResponse) {
                    ChatEmptyState(
                        isOnDeviceModelReady = isOnDeviceModelReady,
                        onDeviceModelStatus = onDeviceModelStatus,
                        onVoiceInput = onVoiceInput,
                        onPromptSelected = { suggestion -> messageText = suggestion },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)
                    ) {
                        if (!isOnDeviceModelReady) {
                            item("model-banner") {
                                InlineStatusBanner(
                                    title = modelBannerTitle(onDeviceModelProvisioningState),
                                    subtitle = modelBannerSubtitle(onDeviceModelProvisioningState),
                                    tone = modelBannerTone(onDeviceModelProvisioningState)
                                )
                            }
                        }

                        items(messages, key = { it.id }) { message ->
                            EditorialMessageBubble(
                                message = message,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Reponse copiee")
                                    }
                                },
                                onRetry = {
                                    onSendMessage(
                                        "Peux-tu reformuler la derniere reponse de facon plus claire et plus concise ?",
                                        null
                                    )
                                },
                                onSummarize = {
                                    onSendMessage(
                                        "Resume la reponse precedente en trois points essentiels.",
                                        null
                                    )
                                }
                            )
                        }

                        if (isWaitingForAiResponse && messages.lastOrNull()?.isFromUser != false) {
                            item("typing") {
                                TypingPanel()
                            }
                        }
                    }
                }

                if (!isOnDeviceModelReady && currentPane == ChatContentPane.CONVERSATION) {
                    CompactModelStatus(
                        status = onDeviceModelStatus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                if (currentPane == ChatContentPane.CONVERSATION) {
                    SmartComposer(
                        value = messageText,
                        onValueChange = { messageText = it },
                        selectedImageUri = selectedImageUri,
                        onRemoveImage = { selectedImageUri = null },
                        onAddImageClick = { showImagePickerDialog = true },
                        onVoiceInput = onVoiceInput,
                        enabled = isOnDeviceModelReady && !isConversationLimitReached,
                        onSend = {
                            if (messageText.isNotBlank() || selectedImageUri != null) {
                                onSendMessage(messageText, selectedImageUri)
                                messageText = ""
                                selectedImageUri = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 88.dp)
                    .navigationBarsPadding()
            )
        }
    }

    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onGalleryClick = {
                showImagePickerDialog = false
                galleryLauncher.launch("image/*")
            },
            onCameraClick = {
                showImagePickerDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    conversationOptionsTarget?.let { conversation ->
        ConversationOptionsDialog(
            conversationTitle = conversation.title,
            onDismiss = { conversationOptionsTarget = null },
            onRename = {
                renameDraft = conversation.title
                conversationRenameTarget = conversation
                conversationOptionsTarget = null
            },
            onDelete = {
                onDeleteConversation(conversation.id)
                conversationOptionsTarget = null
            }
        )
    }

    conversationRenameTarget?.let { conversation ->
        RenameConversationDialog(
            value = renameDraft,
            onValueChange = { renameDraft = it },
            onDismiss = { conversationRenameTarget = null },
            onConfirm = {
                onRenameConversation(conversation.id, renameDraft)
                conversationRenameTarget = null
            }
        )
    }
}

@Composable
private fun ConversationTopBar(
    title: String,
    onOpenSidebar: () -> Unit,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onOpenSidebar) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Afficher les conversations",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            TextButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ConversationDrawerSheet(
    conversations: List<ChatConversationEntity>,
    currentConversationId: String?,
    onSelectConversation: (String) -> Unit,
    onConversationLongPress: (ChatConversationEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMainSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.widthIn(max = 320.dp),
        drawerContainerColor = EditorialPanel,
        drawerContentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversations",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onOpenMainSidebar) {
                    Text(
                        text = "<",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = "Touchez pour ouvrir, appuyez longtemps pour gerer.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (conversations.isEmpty()) {
                Surface(
                    color = EditorialPanelMuted,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucune conversation pour l'instant.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationDrawerItem(
                            conversation = conversation,
                            selected = conversation.id == currentConversationId,
                            onClick = { onSelectConversation(conversation.id) },
                            onLongClick = { onConversationLongPress(conversation) }
                        )
                    }
                }
            }

            Surface(
                color = EditorialPanelMuted,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = TextPrimary
                    )
                    Text(
                        text = "Parametres",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationDrawerItem(
    conversation: ChatConversationEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        color = if (selected) EditorialPanelMuted else Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = 1.dp,
                color = if (selected) AccentBlue.copy(alpha = 0.32f) else EditorialPanelBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = conversation.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = when (conversation.messageCount) {
                    0 -> "Aucun message"
                    1 -> "1 message"
                    else -> "${conversation.messageCount} messages"
                },
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ConversationOptionsDialog(
    conversationTitle: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = conversationTitle,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = EditorialPanelMuted,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRename)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                        Text(
                            text = "Renommer",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Surface(
                    color = EditorialPanelMuted,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF7A7A)
                        )
                        Text(
                            text = "Supprimer",
                            color = Color(0xFFFFB4B4),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
private fun RenameConversationDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Renommer la conversation",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            TextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = value.trim().isNotBlank()
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        containerColor = DarkSurface
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChatSettingsScreen(
    currentSettings: OnDeviceAiSettings,
    selectedModelVariant: OnDeviceModelVariant,
    onModelVariantSelected: (OnDeviceModelVariant) -> Unit,
    selectedMaxTokens: Int,
    onMaxTokensSelected: (Int) -> Unit,
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit,
    isOnDeviceModelReady: Boolean,
    onDeviceModelStatus: String,
    onDeviceModelProvisioningState: OnDeviceModelProvisioningState,
    onRetryModelDownload: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasChanges = selectedModelVariant != currentSettings.modelVariant ||
        selectedMaxTokens != currentSettings.maxContextTokens ||
        systemPrompt.trim() != currentSettings.systemPrompt.trim()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item("intro") {
            Surface(
                color = EditorialPanel,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialPanelBorder, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pilotez le modele local, la taille du contexte et le prompt systeme.",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Un changement de modele telecharge le nouveau fichier, nettoie l'ancien modele applicatif et reinitialise le moteur local.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item("model-selection") {
            SettingsSection(
                title = "Modele IA",
                subtitle = "Choisissez la variante locale a utiliser."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OnDeviceModelVariant.entries.forEach { variant ->
                        SelectableSettingCard(
                            title = variant.displayName,
                            subtitle = if (variant == OnDeviceModelVariant.GEMMA_4_E2B) {
                                "Plus leger et plus rapide a telecharger."
                            } else {
                                "Plus lourd, mais plus confortable pour les reponses riches."
                            },
                            selected = selectedModelVariant == variant,
                            onClick = { onModelVariantSelected(variant) }
                        )
                    }
                }
            }
        }

        item("token-selection") {
            SettingsSection(
                title = "Contexte maximum",
                subtitle = "Valeur actuelle: ${selectedMaxTokens} tokens"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        SUPPORTED_MAX_CONTEXT_TOKENS.forEach { tokenCount ->
                            SettingsTokenChip(
                                label = tokenCount.toTokenLabel(),
                                selected = selectedMaxTokens == tokenCount,
                                onClick = { onMaxTokensSelected(tokenCount) }
                            )
                        }
                    }
                    Text(
                        text = "Si vous modifiez cette valeur, le moteur local sera recharge au prochain message.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Plus le contexte maximum est grand, plus la reponse du modele peut prendre un peu de temps.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item("system-prompt") {
            SettingsSection(
                title = "Prompt systeme",
                subtitle = "Ce texte guide le comportement de Max."
            ) {
                TextField(
                    value = systemPrompt,
                    onValueChange = onSystemPromptChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ComposerField,
                        unfocusedContainerColor = ComposerField,
                        disabledContainerColor = ComposerField
                    )
                )
            }
        }

        item("provisioning-status") {
            if (!isOnDeviceModelReady) {
                ModelProvisioningView(
                    state = onDeviceModelProvisioningState,
                    onRetry = onRetryModelDownload
                )
            } else {
                Surface(
                    color = EditorialPanel,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialPanelBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Etat du modele",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = onDeviceModelStatus,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item("save-button") {
            Surface(
                color = if (hasChanges) AccentBlue.copy(alpha = 0.18f) else EditorialPanelMuted,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasChanges, onClick = onSave)
                    .border(
                        width = 1.dp,
                        color = if (hasChanges) AccentBlue.copy(alpha = 0.38f) else EditorialPanelBorder,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Enregistrer et appliquer",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (hasChanges) {
                            "Les nouveaux reglages seront persistés et appliques au moteur local."
                        } else {
                            "Aucune modification en attente."
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = EditorialPanel,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EditorialPanelBorder, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            content()
        }
    }
}

@Composable
private fun SelectableSettingCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.14f) else EditorialPanelMuted,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) AccentBlue.copy(alpha = 0.42f) else EditorialPanelBorder,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingsTokenChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) AccentBlue.copy(alpha = 0.4f) else EditorialPanelBorder,
                shape = RoundedCornerShape(999.dp)
            )
    ) {
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

private fun Int.toTokenLabel(): String {
    return when {
        this % 1024 == 0 -> "${this / 1024}k"
        else -> toString()
    }
}

@Composable
private fun ChatEmptyState(
    isOnDeviceModelReady: Boolean,
    onDeviceModelStatus: String,
    onVoiceInput: () -> Unit,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "Donne-moi le plan de ma journée.",
        "Résume les actualités importantes."
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Surface(
            color = EditorialPanel,
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EditorialPanelBorder, RoundedCornerShape(30.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = AccentBlue
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Commencer une vraie conversation",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Écrivez un prompt clair ou lancez directement le mode vocal.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    suggestions.forEach { suggestion ->
                        Surface(
                            color = EditorialPanelMuted,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPromptSelected(suggestion) }
                        ) {
                            Text(
                                text = suggestion,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }
                }

                if (!isOnDeviceModelReady) {
                    CompactModelStatus(status = onDeviceModelStatus)
                }
            }
        }

    }
}

@Composable
private fun ModelProvisioningView(
    state: OnDeviceModelProvisioningState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (state) {
        is OnDeviceModelProvisioningState.Checking ->
            "Preparation du chat local" to state.message

        is OnDeviceModelProvisioningState.Downloading ->
            "Telechargement du modele" to state.message

        is OnDeviceModelProvisioningState.Verifying ->
            "Verification du modele" to state.message

        is OnDeviceModelProvisioningState.Ready ->
            "Modele pret" to state.message

        is OnDeviceModelProvisioningState.Error ->
            "Telechargement interrompu" to state.message
    }

    when (state) {
        is OnDeviceModelProvisioningState.Error -> ErrorStateView(
            title = title,
            subtitle = subtitle,
            onRetry = onRetry,
            modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        else -> LoadingStateView(
            title = title,
            subtitle = subtitle,
            modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CompactModelStatus(
    status: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.9f))
        )
        Text(
            text = status,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun EditorialMessageBubble(
    message: Message,
    onCopy: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onSummarize: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeText = remember(message.timestamp) {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = message.timestamp }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        "$hour:$minute"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (message.isFromUser) "Vous" else "Max",
            color = TextSecondary.copy(alpha = 0.88f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = if (message.isFromUser) 0.dp else 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
        ) {
            val bubbleShape = if (message.isFromUser) {
                RoundedCornerShape(26.dp, 26.dp, 10.dp, 26.dp)
            } else {
                RoundedCornerShape(26.dp, 26.dp, 26.dp, 10.dp)
            }

            Surface(
                shape = bubbleShape,
                color = if (message.isFromUser) UserBubbleColor else MaxBubbleColor,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .border(
                        width = 1.dp,
                        color = if (message.isFromUser) {
                            AccentBlue.copy(alpha = 0.28f)
                        } else {
                            EditorialPanelBorder
                        },
                        shape = bubbleShape
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (message.imageUri != null) {
                        AsyncImage(
                            model = message.imageUri,
                            contentDescription = "Image jointe",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (message.content.isNotBlank()) {
                        SelectionContainer {
                            if (message.isFromUser) {
                                Text(
                                    text = message.content,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 24.sp
                                )
                            } else {
                                MarkdownContent(text = message.content)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = timeText,
            color = TextSecondary.copy(alpha = 0.66f),
            style = MaterialTheme.typography.labelSmall
        )

        if (!message.isFromUser) {
            MessageActionRow(
                onCopy = onCopy,
                onRetry = onRetry,
                onSummarize = onSummarize
            )
        }
    }
}

@Composable
private fun MessageActionRow(
    onCopy: (() -> Unit)?,
    onRetry: (() -> Unit)?,
    onSummarize: (() -> Unit)?
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onCopy != null) {
            BubbleActionChip(label = "Copier", onClick = onCopy)
        }
        if (onRetry != null) {
            BubbleActionChip(label = "Relancer", onClick = onRetry)
        }
        if (onSummarize != null) {
            BubbleActionChip(label = "Resume", onClick = onSummarize)
        }
    }
}

@Composable
private fun BubbleActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(1.dp, EditorialPanelBorder, RoundedCornerShape(999.dp))
    ) {
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun MarkdownContent(text: String) {
    val normalizedText = remember(text) { ChatMarkdownFormatter.normalizeForDisplay(text) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        normalizedText.split("\n").forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.startsWith("### ") -> Text(
                    text = parseInlineMarkdown(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                line.startsWith("## ") -> Text(
                    text = parseInlineMarkdown(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                line.startsWith("# ") -> Text(
                    text = parseInlineMarkdown(line.removePrefix("# ")),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = AccentBlue,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = parseInlineMarkdown(line.drop(2)),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
                line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                else -> Text(
                    text = parseInlineMarkdown(line),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    if (!text.contains("**")) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val open = text.indexOf("**", startIndex = cursor)
            if (open == -1) {
                append(text.substring(cursor))
                break
            }

            if (open > cursor) {
                append(text.substring(cursor, open))
            }

            val close = text.indexOf("**", startIndex = open + 2)
            if (close == -1) {
                append(text.substring(open))
                break
            }

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(text.substring(open + 2, close))
            }
            cursor = close + 2
        }
    }
}

@Composable
private fun SmartComposer(
    value: String,
    onValueChange: (String) -> Unit,
    selectedImageUri: Uri?,
    onRemoveImage: () -> Unit,
    onAddImageClick: () -> Unit,
    onVoiceInput: () -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ComposerShell,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedImageUri != null) {
                SelectedImagePreview(
                    imageUri = selectedImageUri,
                    onRemove = onRemoveImage
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ComposerActionPill(
                    label = "Joindre",
                    icon = Icons.Default.AddPhotoAlternate,
                    enabled = enabled,
                    onClick = onAddImageClick
                )
                ComposerActionPill(
                    label = "Vocal",
                    icon = Icons.Default.Mic,
                    enabled = enabled,
                    onClick = onVoiceInput
                )
            }

            Surface(
                color = ComposerField,
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp, max = 140.dp),
                        placeholder = {
                            Text(
                                text = when {
                                    !enabled -> "Preparation du modele local..."
                                    else -> "Ecrire un message"
                                },
                                color = TextSecondary
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentBlue,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(22.dp),
                        enabled = enabled,
                        singleLine = false,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        )
                    )

                    val canSend = enabled && (value.isNotBlank() || selectedImageUri != null)
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp, bottom = 4.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (canSend) AccentBlue else DarkSurfaceVariant)
                            .clickable(enabled = canSend, onClick = onSend),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = if (canSend) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedImagePreview(
    imageUri: Uri,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Image selectionnee",
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopStart)
                .offset(x = 58.dp, y = (-8).dp)
                .clip(CircleShape)
                .background(Color(0xFFD9465F))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Supprimer l'image",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ComposerActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        color = if (enabled) ComposerAction else ComposerAction.copy(alpha = 0.45f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) AccentBlue else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (enabled) TextPrimary else TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun modelBannerTitle(state: OnDeviceModelProvisioningState): String = when (state) {
    is OnDeviceModelProvisioningState.Checking -> "Preparation du modele local"
    is OnDeviceModelProvisioningState.Downloading -> "Telechargement du modele"
    is OnDeviceModelProvisioningState.Verifying -> "Verification du modele"
    is OnDeviceModelProvisioningState.Ready -> "Modele local pret"
    is OnDeviceModelProvisioningState.Error -> "Modele local indisponible"
}

private fun modelBannerSubtitle(state: OnDeviceModelProvisioningState): String = when (state) {
    is OnDeviceModelProvisioningState.Checking -> state.message
    is OnDeviceModelProvisioningState.Downloading -> state.message
    is OnDeviceModelProvisioningState.Verifying -> state.message
    is OnDeviceModelProvisioningState.Ready -> state.message
    is OnDeviceModelProvisioningState.Error -> state.message
}

private fun modelBannerTone(state: OnDeviceModelProvisioningState): BannerTone = when (state) {
    is OnDeviceModelProvisioningState.Ready -> BannerTone.Success
    is OnDeviceModelProvisioningState.Error -> BannerTone.Error
    is OnDeviceModelProvisioningState.Checking,
    is OnDeviceModelProvisioningState.Downloading,
    is OnDeviceModelProvisioningState.Verifying -> BannerTone.Warning
}

@Composable
private fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Ajouter une image") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onGalleryClick)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = AccentBlue
                    )
                    Text(text = "Galerie")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCameraClick)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = AccentBlue
                    )
                    Text(text = "Camera")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Fermer")
            }
        }
    )
}

private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile("max_chat_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Composable
private fun TypingPanel() {
    Surface(
        color = MaxBubbleColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .widthIn(max = 124.dp)
            .border(1.dp, EditorialPanelBorder, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypingDot()
            TypingDot()
            TypingDot()
        }
    }
}

@Composable
private fun TypingDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(AccentBlue.copy(alpha = 0.9f))
    )
}
