package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Visuals for the app-wide snackbar. [isError] switches the container to the error palette so
 * failures read differently from confirmations at a glance.
 */
data class AppSnackbarVisuals(
    override val message: String,
    val isError: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false
) : SnackbarVisuals

/**
 * App-wide snackbar dispatcher.
 *
 * A singleton rather than a `SnackbarHostState` threaded through each `Scaffold`, because messages
 * originate from places that have no access to one: the ViewModel, dialog button lambdas, and
 * activity-result callbacks. Emission goes through [MutableSharedFlow.tryEmit], so [showMessage]
 * and [showError] are safe to call from any thread and from non-composable code.
 */
object SnackbarController {
    private val _events = MutableSharedFlow<AppSnackbarVisuals>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    /** Height of a bottom bar the host should float above, reported by [SnackbarBottomInset]. */
    var bottomInset by mutableStateOf(0.dp)
        private set

    /** A confirmation / informational message. */
    fun showMessage(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        emit(message, isError = false, duration = duration)
    }

    /** A failure message, shown with the error palette. */
    fun showError(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        emit(message, isError = true, duration = duration)
    }

    private fun emit(message: String, isError: Boolean, duration: SnackbarDuration) {
        if (message.isBlank()) return
        _events.tryEmit(
            AppSnackbarVisuals(message = message, isError = isError, duration = duration)
        )
    }

    internal fun setBottomInset(inset: Dp) {
        bottomInset = inset
    }
}

/**
 * Lifts [AppSnackbarHost] above a bottom bar for as long as this composable stays in the tree.
 * Pass the bottom padding the bar occupies (system navigation inset included).
 */
@Composable
fun SnackbarBottomInset(inset: Dp) {
    DisposableEffect(inset) {
        SnackbarController.setBottomInset(inset)
        onDispose { SnackbarController.setBottomInset(0.dp) }
    }
}

/**
 * Single host for every [SnackbarController] message. Mounted once at the app root so it also
 * covers screens that are not built on a `Scaffold` (login, splash).
 */
@Composable
fun AppSnackbarHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bottomBarInset = SnackbarController.bottomInset

    LaunchedEffect(Unit) {
        SnackbarController.events.collect { visuals ->
            // Replace whatever is on screen so a fresh message never waits behind a stale one.
            hostState.currentSnackbarData?.dismiss()
            scope.launch { hostState.showSnackbar(visuals) }
        }
    }

    // Screens with a bottom bar report its height; elsewhere we only clear the system nav bar.
    val liftModifier = if (bottomBarInset > 0.dp) {
        Modifier.padding(bottom = bottomBarInset)
    } else {
        Modifier.navigationBarsPadding()
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .fillMaxWidth()
            .then(liftModifier)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) { data ->
        val isError = (data.visuals as? AppSnackbarVisuals)?.isError == true

        Snackbar(
            shape = RoundedCornerShape(12.dp),
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.inverseSurface
            },
            contentColor = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.inverseOnSurface
            },
            modifier = Modifier.testTag("app_snackbar")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.testTag("app_snackbar_message")
                )
            }
        }
    }
}
