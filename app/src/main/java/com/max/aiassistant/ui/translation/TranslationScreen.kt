package com.max.aiassistant.ui.translation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.max.aiassistant.data.realtime.VoiceLanguage
import com.max.aiassistant.data.realtime.VoiceMode
import com.max.aiassistant.ui.common.BannerTone
import com.max.aiassistant.ui.common.InlineStatusBanner
import com.max.aiassistant.ui.theme.AccentBlue
import com.max.aiassistant.ui.theme.TextPrimary
import com.max.aiassistant.ui.theme.TextSecondary

private val TranslationBackground = listOf(Color(0xFF07111B), Color(0xFF03070D))
private val TranslationPanel = Color(0xFF111B29)
private val TranslationPanelSoft = Color(0xFF172334)
private val TranslationBorder = Color(0xFF283951)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslationScreen(
    isVoiceRecording: Boolean,
    isVoiceProcessing: Boolean,
    isVoiceSpeaking: Boolean,
    errorMessage: String?,
    statusMessage: String,
    isOnDeviceModelReady: Boolean,
    isUsingOpenAiVoice: Boolean,
    isOffline: Boolean,
    conversationLines: List<String>,
    voiceMode: VoiceMode,
    voiceLanguages: List<VoiceLanguage>,
    voiceSourceLanguage: VoiceLanguage,
    voiceTargetLanguage: VoiceLanguage,
    onVoiceModeChange: (VoiceMode) -> Unit,
    onVoiceSourceLanguageChange: (VoiceLanguage) -> Unit,
    onVoiceTargetLanguageChange: (VoiceLanguage) -> Unit,
    onToggleVoiceRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = isVoiceRecording || isVoiceProcessing || isVoiceSpeaking
    val scrollState = rememberScrollState()
    val selectedMode = when (voiceMode) {
        VoiceMode.AUDIO_TO_SPEECH_TRANSLATION -> VoiceMode.AUDIO_TO_SPEECH_TRANSLATION
        else -> VoiceMode.AUDIO_TO_TEXT_TRANSLATION
    }

    LaunchedEffect(Unit) {
        if (voiceMode == VoiceMode.AI_CONVERSATION) {
            onVoiceModeChange(VoiceMode.AUDIO_TO_TEXT_TRANSLATION)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(TranslationBackground))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header(isUsingOpenAiVoice = isUsingOpenAiVoice)

            if (errorMessage != null) {
                InlineStatusBanner(
                    title = "Traduction indisponible",
                    subtitle = errorMessage,
                    tone = BannerTone.Error,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!isUsingOpenAiVoice) {
                InlineStatusBanner(
                    title = "Modele OpenAI requis",
                    subtitle = "Choisis GPT-5.5 dans les reglages du chat pour activer la traduction Realtime.",
                    tone = BannerTone.Warning,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!isOnDeviceModelReady) {
                InlineStatusBanner(
                    title = if (isOffline) "Modele local indisponible hors ligne" else "Modele local en preparation",
                    subtitle = statusMessage,
                    tone = if (isOffline) BannerTone.Offline else BannerTone.Warning,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Surface(
                color = TranslationPanel,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeChip(
                            label = "Ecrite",
                            selected = selectedMode == VoiceMode.AUDIO_TO_TEXT_TRANSLATION,
                            enabled = isUsingOpenAiVoice && !isActive,
                            onClick = { onVoiceModeChange(VoiceMode.AUDIO_TO_TEXT_TRANSLATION) }
                        )
                        ModeChip(
                            label = "Orale",
                            selected = selectedMode == VoiceMode.AUDIO_TO_SPEECH_TRANSLATION,
                            enabled = isUsingOpenAiVoice && !isActive,
                            onClick = { onVoiceModeChange(VoiceMode.AUDIO_TO_SPEECH_TRANSLATION) }
                        )
                    }

                    LanguageBlock(
                        title = "Source",
                        languages = voiceLanguages,
                        selectedLanguage = voiceSourceLanguage,
                        enabled = isUsingOpenAiVoice && !isActive,
                        onLanguageChange = onVoiceSourceLanguageChange
                    )
                    LanguageBlock(
                        title = "Cible",
                        languages = voiceLanguages,
                        selectedLanguage = voiceTargetLanguage,
                        enabled = isUsingOpenAiVoice && !isActive,
                        onLanguageChange = onVoiceTargetLanguageChange
                    )
                }
            }

            TranscriptPanel(
                lines = conversationLines,
                isActive = isActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 360.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            )

            MicButton(
                mode = selectedMode,
                statusMessage = statusMessage,
                isVoiceRecording = isVoiceRecording,
                isVoiceProcessing = isVoiceProcessing,
                isVoiceSpeaking = isVoiceSpeaking,
                enabled = isUsingOpenAiVoice && isOnDeviceModelReady,
                onToggleVoiceRecording = onToggleVoiceRecording,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Header(isUsingOpenAiVoice: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                text = "Traduction",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isUsingOpenAiVoice) "Realtime audio vers texte ou voix." else "Active le modele OpenAI pour traduire.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageBlock(
    title: String,
    languages: List<VoiceLanguage>,
    selectedLanguage: VoiceLanguage,
    enabled: Boolean,
    onLanguageChange: (VoiceLanguage) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { language ->
                ModeChip(
                    label = language.label,
                    selected = selectedLanguage == language,
                    enabled = enabled,
                    onClick = { onLanguageChange(language) }
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else TranslationPanelSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun TranscriptPanel(
    lines: List<String>,
    isActive: Boolean,
    modifier: Modifier,
    contentPadding: PaddingValues
) {
    val listState = rememberLazyListState()
    val bottomAnchorKey = "transcript-bottom-anchor"
    val lastLine = lines.lastOrNull()

    LaunchedEffect(lines.size, lastLine) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TranslationPanel)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transcript",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${lines.size} lignes",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = TranslationBorder)
        Spacer(modifier = Modifier.height(12.dp))

        if (lines.isEmpty()) {
            EmptyTranscript(isActive = isActive)
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = contentPadding,
                modifier = Modifier.weight(1f)
            ) {
                items(lines) { line ->
                    Surface(
                        color = TranslationPanelSoft,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = line,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
                item(key = bottomAnchorKey) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyTranscript(isActive: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isActive) "Ecoute en cours" else "Aucune traduction",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Appuie sur le micro pour commencer.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MicButton(
    mode: VoiceMode,
    statusMessage: String,
    isVoiceRecording: Boolean,
    isVoiceProcessing: Boolean,
    isVoiceSpeaking: Boolean,
    enabled: Boolean,
    onToggleVoiceRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggleVoiceRecording,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        if (enabled) {
                            listOf(AccentBlue, Color(0xFF0A7FDF))
                        } else {
                            listOf(TranslationBorder, TranslationPanelSoft)
                        }
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = when {
                        isVoiceProcessing -> "Annuler"
                        isVoiceRecording -> "Arreter"
                        isVoiceSpeaking -> "Stopper la lecture"
                        mode == VoiceMode.AUDIO_TO_SPEECH_TRANSLATION -> "Traduire en voix"
                        else -> "Traduire en texte"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = statusMessage,
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}
