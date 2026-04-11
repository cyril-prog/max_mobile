package com.max.aiassistant.ui.common

import android.animation.ValueAnimator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.max.aiassistant.ui.theme.AccentBlue
import com.max.aiassistant.ui.theme.CompletedGreen
import com.max.aiassistant.ui.theme.ConnectionIdleColor
import com.max.aiassistant.ui.theme.DarkSurface
import com.max.aiassistant.ui.theme.DarkSurfaceVariant
import com.max.aiassistant.ui.theme.HighOrange
import com.max.aiassistant.ui.theme.Spacing
import com.max.aiassistant.ui.theme.TextPrimary
import com.max.aiassistant.ui.theme.TextSecondary
import com.max.aiassistant.ui.theme.UrgentRed

@Composable
fun rememberMotionEnabled(): Boolean = remember {
    ValueAnimator.areAnimatorsEnabled()
}

@Composable
fun LoadingStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    StateCard(
        icon = Icons.Default.Sync,
        iconTint = AccentBlue,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        iconContent = {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = AccentBlue,
                strokeWidth = 2.5.dp
            )
        }
    )
}

@Composable
fun ErrorStateView(
    title: String,
    subtitle: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    StateCard(
        icon = Icons.Default.ErrorOutline,
        iconTint = UrgentRed,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        action = {
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Réessayer",
                    modifier = Modifier.size(16.dp)
                )
                Text("Réessayer", modifier = Modifier.padding(start = 6.dp))
            }
        }
    )
}

@Composable
fun InlineStatusBanner(
    title: String,
    subtitle: String,
    tone: BannerTone,
    modifier: Modifier = Modifier
) {
    val (icon, accent) = when (tone) {
        BannerTone.Success -> Icons.Default.CheckCircle to CompletedGreen
        BannerTone.Info -> Icons.Default.CloudDone to AccentBlue
        BannerTone.Warning -> Icons.Default.Sync to HighOrange
        BannerTone.Error -> Icons.Default.ErrorOutline to UrgentRed
        BannerTone.Offline -> Icons.Default.CloudOff to ConnectionIdleColor
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = DarkSurface,
            labelColor = TextPrimary
        )
    )
}

private typealias StateAction = @Composable () -> Unit

@Composable
private fun StateCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconContent: (@Composable () -> Unit)? = null,
    action: StateAction? = null
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconContent != null) {
                        iconContent()
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = Spacing.xs.dp)
                    )
                }
                if (action != null) {
                    action()
                }
            }
        }
    }
}

enum class BannerTone {
    Success,
    Info,
    Warning,
    Error,
    Offline
}
