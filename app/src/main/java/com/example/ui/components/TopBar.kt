package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R

@Composable
fun FarmLogoDisplay(
    logoUri: String = "",
    logoEmoji: String = "",
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val base64Bitmap = remember(logoUri) {
        if (logoUri.isNotBlank() && (logoUri.startsWith("data:image") || logoUri.contains("base64,"))) {
            try {
                val cleanBase64 = if (logoUri.contains(",")) logoUri.substringAfter(",") else logoUri
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    when {
        base64Bitmap != null -> {
            Image(
                bitmap = base64Bitmap.asImageBitmap(),
                contentDescription = "Farm Logo",
                modifier = modifier,
                contentScale = contentScale
            )
        }
        logoUri.isNotBlank() -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Farm Logo",
                modifier = modifier,
                contentScale = contentScale
            )
        }
        logoEmoji.isNotBlank() && logoEmoji != "🐔" -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = logoEmoji,
                    fontSize = 22.sp
                )
            }
        }
        else -> {
            Image(
                painter = painterResource(id = R.drawable.kazi_agro_logo),
                contentDescription = "Farm Logo",
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String = "কাজী এগ্রোটেক",
    isRootScreen: Boolean = true,
    logoUri: String = "",
    logoEmoji: String = "",
    hasUnreadNotification: Boolean = false,
    onBackClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onLogoClick: () -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRootScreen) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onLogoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        FarmLogoDisplay(
                            logoUri = logoUri,
                            logoEmoji = logoEmoji,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 19.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (!isRootScreen) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            actions()
            if (isRootScreen) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(40.dp)
                        .testTag("top_bar_notification_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasUnreadNotification) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .androidx.compose.foundation.layout.offset(x = 2.dp, y = (-2).dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        windowInsets = TopAppBarDefaults.windowInsets,
        modifier = Modifier.testTag("main_top_app_bar")
    )
}


