package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Visual weight of a [DetailAction]: the main action, a plain one, or a destructive one. */
enum class DetailActionTone { Primary, Neutral, Danger }

data class DetailAction(
    val icon: ImageVector,
    val label: String,
    val tone: DetailActionTone = DetailActionTone.Neutral,
    val testTag: String = "",
    val onClick: () -> Unit
)

/**
 * Bottom action bar shared by the record detail screens (daily report, monthly expense).
 *
 * Each action is a circular icon with a short caption instead of a full-width labelled button, so
 * the row stays compact and reads the same whichever actions the user's role allows.
 *
 * [navigationBarsPadding] is essential, not decorative: the app runs edge-to-edge and Material3's
 * `Scaffold` does not inset a custom `bottomBar`, so without it the buttons sit under the phone's
 * navigation / gesture bar and cannot be tapped.
 */
@Composable
fun DetailActionBar(
    actions: List<DetailAction>,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("detail_action_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                DetailActionButton(action = action)
            }
        }
    }
}

@Composable
private fun DetailActionButton(action: DetailAction) {
    val haptics = rememberHaptics()
    val containerColor = when (action.tone) {
        DetailActionTone.Primary -> MaterialTheme.colorScheme.primary
        DetailActionTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHighest
        DetailActionTone.Danger -> MaterialTheme.colorScheme.errorContainer
    }
    val iconColor = when (action.tone) {
        DetailActionTone.Primary -> MaterialTheme.colorScheme.onPrimary
        DetailActionTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        DetailActionTone.Danger -> MaterialTheme.colorScheme.error
    }
    val labelColor = when (action.tone) {
        DetailActionTone.Primary -> MaterialTheme.colorScheme.primary
        DetailActionTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        DetailActionTone.Danger -> MaterialTheme.colorScheme.error
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.widthIn(min = 68.dp)
    ) {
        // pressScale is chained ahead of clip/background so the whole circle shrinks on press, not
        // just the icon; the ripple still lands after the clip and so stays inside the circle.
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .size(50.dp)
                .pressScale(interactionSource)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple()
                ) {
                    haptics.tap()
                    action.onClick()
                }
                .testTag(action.testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = action.label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor
            ),
            maxLines = 1
        )
    }
}
