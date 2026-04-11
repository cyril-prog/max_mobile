package com.max.aiassistant.ui.chat

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.max.aiassistant.model.Message
import com.max.aiassistant.ui.common.EmptyStateView
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    isWaitingForAiResponse: Boolean,
    onSendMessage: (String, Uri?) -> Unit,
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
                isWaitingForAiResponse = isWaitingForAiResponse,
                onSendMessage = onSendMessage,
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
            isWaitingForAiResponse = isWaitingForAiResponse,
            onSendMessage = onSendMessage,
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
    isWaitingForAiResponse: Boolean,
    onSendMessage: (String, Uri?) -> Unit,
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

    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

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

    LaunchedEffect(messages.size, isWaitingForAiResponse) {
        val itemCount = messages.size + if (isWaitingForAiResponse) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(ChatBackdrop))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (showChrome) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Chat",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }

            if (messages.isEmpty() && !isWaitingForAiResponse) {
                ChatEmptyState(
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

                    if (isWaitingForAiResponse) {
                        item("typing") {
                            TypingPanel()
                        }
                    }
                }
            }

            SmartComposer(
                value = messageText,
                onValueChange = { messageText = it },
                selectedImageUri = selectedImageUri,
                onRemoveImage = { selectedImageUri = null },
                onAddImageClick = { showImagePickerDialog = true },
                onVoiceInput = onVoiceInput,
                onSend = {
                    if (messageText.isNotBlank() || selectedImageUri != null) {
                        onSendMessage(messageText, selectedImageUri)
                        scope.launch { snackbarHostState.showSnackbar("Message envoye") }
                        messageText = ""
                        selectedImageUri = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 88.dp)
                .navigationBarsPadding()
        )
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
}

@Composable
private fun ChatEmptyState(
    onVoiceInput: () -> Unit,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "Donne-moi le plan de ma journee.",
        "Resume les actualites importantes.",
        "Aide-moi a preparer trois priorites."
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
                            text = "Ecrivez un prompt clair ou lancez directement le mode vocal.",
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

                ComposerActionPill(
                    label = "Passer en vocal",
                    icon = Icons.Default.Mic,
                    onClick = onVoiceInput
                )
            }
        }

        EmptyStateView(
            icon = Icons.Default.ChatBubbleOutline,
            iconTint = AccentBlue,
            title = "Le chat est pret",
            subtitle = "Vous pouvez ecrire, joindre une image ou basculer en vocal.",
            modifier = Modifier.fillMaxWidth()
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.split("\n").forEach { rawLine ->
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
                    onClick = onAddImageClick
                )
                ComposerActionPill(
                    label = "Vocal",
                    icon = Icons.Default.Mic,
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
                                text = "Ecrire un message",
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
                        singleLine = false,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        )
                    )

                    val canSend = value.isNotBlank() || selectedImageUri != null
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
    onClick: () -> Unit
) {
    Surface(
        color = ComposerAction,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = TextPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
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
