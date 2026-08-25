package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PasswordStrength(
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
) {
    val score: Int
        get() {
            var count = 0
            if (hasMinLength) count++
            if (hasUppercase) count++
            if (hasLowercase) count++
            if (hasDigit) count++
            if (hasSpecialChar) count++
            return count
        }

    val isStrong: Boolean
        get() = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar

    val label: String
        get() = when (score) {
            0, 1 -> "খুবই দুর্বল (Weak)"
            2 -> "দুর্বল (Fair)"
            3 -> "মাঝারি (Good)"
            4 -> "ভালো (Strong)"
            5 -> "শক্তিশালী ও নিরাপদ (Very Strong)"
            else -> ""
        }

    val color: Color
        get() = when (score) {
            0, 1 -> Color(0xFFD32F2F)
            2 -> Color(0xFFE65100)
            3 -> Color(0xFFF57C00)
            4 -> Color(0xFF388E3C)
            5 -> Color(0xFF0D631B)
            else -> Color.Gray
        }

    companion object {
        fun validate(password: String): PasswordStrength {
            return PasswordStrength(
                hasMinLength = password.length >= 8,
                hasUppercase = password.any { it.isUpperCase() },
                hasLowercase = password.any { it.isLowerCase() },
                hasDigit = password.any { it.isDigit() },
                hasSpecialChar = password.any { !it.isLetterOrDigit() }
            )
        }
    }
}

@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = PasswordStrength.validate(password)
    val animatedProgress by animateFloatAsState(targetValue = strength.score / 5f, label = "progress")
    val animatedColor by animateColorAsState(targetValue = strength.color, label = "color")

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E2E2)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পাসওয়ার্ড নিরাপত্তা স্তর:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = if (password.isEmpty()) "পাসওয়ার্ড লিখুন" else strength.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (password.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else animatedColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }

            // Segmented / Continuous Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = animatedColor,
                trackColor = Color(0xFFE0E0E0)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Requirements Checklist
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PasswordRuleItem(
                    label = "কমপক্ষে ৮টি অক্ষর (Min 8 characters)",
                    isMet = strength.hasMinLength
                )
                PasswordRuleItem(
                    label = "কমপক্ষে ১টি বড় হাতের অক্ষর (A-Z)",
                    isMet = strength.hasUppercase
                )
                PasswordRuleItem(
                    label = "কমপক্ষে ১টি ছোট হাতের অক্ষর (a-z)",
                    isMet = strength.hasLowercase
                )
                PasswordRuleItem(
                    label = "কমপক্ষে ১টি সংখ্যা (0-9)",
                    isMet = strength.hasDigit
                )
                PasswordRuleItem(
                    label = "কমপক্ষে ১টি বিশেষ চিহ্ন (!@#\$%^&*)",
                    isMet = strength.hasSpecialChar
                )
            }
        }
    }
}

@Composable
private fun PasswordRuleItem(
    label: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isMet) Color(0xFF0D631B) else Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = if (isMet) Color(0xFF0D631B) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

