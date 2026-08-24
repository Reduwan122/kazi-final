package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/**
 * Tap feedback routed through the platform view rather than a `Vibrator`, so it needs no
 * `VIBRATE` permission and still honours the user's system touch-vibration setting.
 */
class AppHaptics(private val view: View) {

    /** Short tick for ordinary taps: FABs, save buttons, card presses. */
    fun tap() = perform(HapticFeedbackConstants.VIRTUAL_KEY)

    /** Firmer buzz for committing something destructive, e.g. confirming a delete. */
    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.CONFIRM)
        } else {
            perform(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): AppHaptics {
    val view = LocalView.current
    return remember(view) { AppHaptics(view) }
}

/**
 * Scales content down while [interactionSource] reports a press. Use with a component that already
 * owns its interaction source; otherwise reach for [scaleClickable].
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Drop-in replacement for `Modifier.clickable { }` that adds a subtle press scale-down, keeping the
 * Material ripple. Set [haptic] to also emit a light tick on tap.
 *
 * Chain it exactly where `clickable` sat — after any `clip`, before `testTag` — so ripple clipping
 * and test tags are unchanged.
 */
@Composable
fun Modifier.scaleClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    haptic: Boolean = false,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()

    return this
        .pressScale(interactionSource = interactionSource, pressedScale = pressedScale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled
        ) {
            if (haptic) haptics.tap()
            onClick()
        }
}
