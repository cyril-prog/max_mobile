package com.max.aiassistant.ui.chat

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.max.aiassistant.model.Message
import com.max.aiassistant.ui.common.MiniFluidOrb
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ÉCRAN CENTRAL : Messenger
 *
 * Interface de chat avec Max
 * - Barre de titre avec logo et nom "Max"
 * - Zone de messages scrollable
 * - Barre d'entrée avec champ texte, bouton image et bouton envoi
 * - Sidebar de navigation accessible par swipe vers la gauche depuis le bord droit
 */
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
    initialText: String = "",
    onInitialTextConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()
    
    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.CHAT,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.VOICE -> onNavigateToHome()
                NavigationScreen.CHAT -> { /* Déjà sur cet écran */ }
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
            }
        },
        sidebarState = sidebarState
    ) {
        ChatScreenContent(
            messages = messages,
            isWaitingForAiResponse = isWaitingForAiResponse,
            onSendMessage = onSendMessage,
            onVoiceInput = onVoiceInput,
            onNavigateToHome = onNavigateToHome,
            initialText = initialText,
            onInitialTextConsumed = onInitialTextConsumed,
            modifier = modifier
        )
    }
}

/**
 * Contenu de l'écran Chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    messages: List<Message>,
    isWaitingForAiResponse: Boolean,
    onSendMessage: (String, Uri?) -> Unit,
    onVoiceInput: () -> Unit,
    onNavigateToHome: () -> Unit,
    initialText: String = "",
    onInitialTextConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    
    // URI temporaire pour la photo prise avec la caméra
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    // Launcher pour sélectionner une image depuis la galerie
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }
    
    // Launcher pour prendre une photo avec la caméra
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = tempCameraUri
        }
    }
    
    // Launcher pour demander la permission caméra
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission accordée, on peut lancer la caméra
            val uri = createImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    
    // Gère le texte partagé depuis une autre application
    LaunchedEffect(initialText) {
        if (initialText.isNotEmpty()) {
            messageText = initialText
            onInitialTextConsumed()
        }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll vers le bas quand un nouveau message arrive ou quand l'état de chargement change
    LaunchedEffect(messages.size, isWaitingForAiResponse) {
        // Calcule l'index du dernier élément (messages + typing indicator si présent)
        val itemCount = messages.size + if (isWaitingForAiResponse) 1 else 0
        if (itemCount > 0) {
            // Défile directement vers le dernier élément
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Espace en haut pour éviter que les messages soient coupés
        Spacer(modifier = Modifier.height(32.dp))

        // Zone de messages (se compresse quand le clavier apparaît)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }

            // Indicateur de chargement si l'IA est en train de réfléchir
            if (isWaitingForAiResponse) {
                item(key = "typing_indicator") {
                    TypingIndicator()
                }
            }
        }

        // Barre d'entrée (monte avec le clavier et évite la barre Android)
        MessageInputBar(
            value = messageText,
            onValueChange = { messageText = it },
            selectedImageUri = selectedImageUri,
            onRemoveImage = { selectedImageUri = null },
            onAddImageClick = { showImagePickerDialog = true },
            onSend = {
                if (messageText.isNotBlank() || selectedImageUri != null) {
                    onSendMessage(messageText, selectedImageUri)
                    messageText = ""
                    selectedImageUri = null
                }
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.ime)
        )
    }
    
    // Dialog pour choisir entre galerie et caméra
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

/**
 * Bulle de message
 * S'aligne à gauche pour l'utilisateur, à droite pour Max
 * Peut contenir une image et/ou du texte
 */
@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser)
            Arrangement.Start
        else
            Arrangement.End
    ) {
        val bubbleShape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (message.isFromUser) 4.dp else 20.dp,
            bottomEnd = if (message.isFromUser) 20.dp else 4.dp
        )

        Surface(
            shape = bubbleShape,
            color = if (message.isFromUser) UserMessageBg else MaxMessageBg,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    width = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.3f),
                    shape = bubbleShape
                )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                )
            ) {
                // Affiche l'image si présente
                message.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Image attachée",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (message.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Affiche le texte si présent (avec formatage markdown pour les messages de l'IA)
                if (message.content.isNotBlank()) {
                    Text(
                        text = if (message.isFromUser) {
                            AnnotatedString(message.content)
                        } else {
                            parseMarkdown(message.content)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Parse le texte markdown et retourne un AnnotatedString formaté
 * Supporte : **gras**, *italique*, ***gras italique***, `code`
 */
private fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val length = text.length
        
        while (currentIndex < length) {
            when {
                // Gras + Italique: ***texte***
                currentIndex + 2 < length && text.substring(currentIndex, currentIndex + 3) == "***" -> {
                    val endIndex = text.indexOf("***", currentIndex + 3)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(text.substring(currentIndex + 3, endIndex))
                        }
                        currentIndex = endIndex + 3
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Gras: **texte**
                currentIndex + 1 < length && text.substring(currentIndex, currentIndex + 2) == "**" -> {
                    val endIndex = text.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(currentIndex + 2, endIndex))
                        }
                        currentIndex = endIndex + 2
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Italique: *texte*
                text[currentIndex] == '*' && currentIndex + 1 < length && text[currentIndex + 1] != '*' -> {
                    val endIndex = text.indexOf('*', currentIndex + 1)
                    if (endIndex != -1 && endIndex > currentIndex + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(currentIndex + 1, endIndex))
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Code inline: `code`
                text[currentIndex] == '`' -> {
                    val endIndex = text.indexOf('`', currentIndex + 1)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            background = Color(0xFF2D2D2D)
                        )) {
                            append(text.substring(currentIndex + 1, endIndex))
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Texte normal
                else -> {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
        }
    }
}

/**
 * Barre d'entrée de message avec champ texte, bouton image et bouton envoi
 */
@Composable
fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    selectedImageUri: Uri?,
    onRemoveImage: () -> Unit,
    onAddImageClick: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Prévisualisation de l'image sélectionnée
            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.size(80.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Image sélectionnée",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Bouton de suppression
                    IconButton(
                        onClick = onRemoveImage,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .background(
                                color = Color.Red.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Supprimer l'image",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bouton d'ajout d'image
                IconButton(
                    onClick = onAddImageClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Ajouter une image",
                        tint = AccentBlue
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Champ de texte multiligne (max 3 lignes visibles)
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp),
                    placeholder = {
                        Text(
                            text = "Message",
                            color = TextSecondary
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentBlue,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Bouton d'envoi (activé si texte ou image présent)
                val isEnabled = value.isNotBlank() || selectedImageUri != null
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled) AccentBlue else DarkSurfaceVariant
                        )
                        .clickable(enabled = isEnabled) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Envoyer le message",
                        tint = if (isEnabled) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Dialog pour choisir entre galerie et caméra
 */
@Composable
fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Ajouter une image",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option Galerie
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGalleryClick() },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Choisir depuis la galerie",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Option Caméra
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCameraClick() },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Prendre une photo",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        }
    )
}

/**
 * Crée un URI temporaire pour stocker la photo prise avec la caméra
 */
fun createImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "images")
    directory.mkdirs()
    val file = File(directory, "temp_camera_image_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

/**
 * Indicateur de saisie animé (typing indicator)
 * Affiche trois points qui rebondissent pour indiquer que l'IA est en train de réfléchir
 */
@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        val bubbleShape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 4.dp
        )

        Surface(
            shape = bubbleShape,
            color = MaxMessageBg,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    width = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.3f),
                    shape = bubbleShape
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animation de 3 points qui rebondissent en cascade
                repeat(3) { index ->
                    TypingDot(delayMillis = index * 150)
                }
            }
        }
    }
}

/**
 * Un point animé pour le typing indicator
 * Rebondit verticalement avec un délai pour créer un effet de cascade
 */
@Composable
fun TypingDot(delayMillis: Int) {
    // Animation infinie de translation verticale
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dot_$delayMillis")

    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translate_y"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer {
                translationY = translateY
            }
            .background(
                color = TextSecondary.copy(alpha = 0.7f),
                shape = CircleShape
            )
    )
}
