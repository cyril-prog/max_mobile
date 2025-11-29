package com.max.aiassistant.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.max.aiassistant.model.Message
import com.max.aiassistant.ui.common.MiniFluidOrb
import com.max.aiassistant.ui.theme.*
import kotlinx.coroutines.launch

/**
 * ÉCRAN CENTRAL : Messenger
 *
 * Interface de chat avec Max
 * - Barre de titre avec logo et nom "Max"
 * - Zone de messages scrollable
 * - Barre d'entrée avec champ texte et bouton micro
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onVoiceInput: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll vers le bas quand un nouveau message arrive ou quand on charge la liste
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Défile directement vers le dernier message
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(onNavigateToHome) {
                var cumulativeDrag = 0f
                var swipeTriggered = false
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) {
                            cumulativeDrag += dragAmount
                            if (!swipeTriggered && cumulativeDrag >= 80f) {
                                swipeTriggered = true
                                onNavigateToHome()
                            }
                        }
                    },
                    onDragEnd = {
                        cumulativeDrag = 0f
                        swipeTriggered = false
                    },
                    onDragCancel = {
                        cumulativeDrag = 0f
                        swipeTriggered = false
                    }
                )
            }
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
        }

        // Barre d'entrée (monte avec le clavier et évite la barre Android)
        MessageInputBar(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                if (messageText.isNotBlank()) {
                    onSendMessage(messageText)
                    messageText = ""
                }
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.ime)
        )
    }
}

/**
 * Bulle de message
 * S'aligne à gauche pour l'utilisateur, à droite pour Max
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
            Text(
                text = message.content,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }
    }
}

/**
 * Barre d'entrée de message avec champ texte et bouton envoi
 */
@Composable
fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Champ de texte
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
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
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSend() }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton d'envoi (toujours visible, désactivé si le champ est vide)
            val isEnabled = value.isNotBlank()
            IconButton(
                onClick = { if (isEnabled) onSend() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnabled) AccentBlue else DarkSurfaceVariant
                    )
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
