package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyReportEntity
import kotlin.math.max

enum class ChartType(val title: String) {
    EGG_PRODUCTION("দৈনিক ডিম উৎপাদন"),
    DAILY_SALES("দৈনিক বিক্রয়"),
    EXPENSE_TREND("দৈনিক ব্যয়")
}

/**
 * One bar of [ProductionChartCard].
 *
 * [barValue] is scaled for drawing (sales in thousands, expense in hundreds) so all three chart
 * types share one bar height range. [readout] is the real figure, already formatted with its unit,
 * because the tapped-bar label must show what the record actually holds — not the scaled height.
 */
private data class ChartEntry(
    val dateLabel: String,
    val barValue: Float,
    val readout: String
)

@Composable
fun ProductionChartCard(
    reports: List<DailyReportEntity>,
    modifier: Modifier = Modifier
) {
    var selectedChartType by remember { mutableStateOf(ChartType.EGG_PRODUCTION) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Take last 7 days in chronological order
    val chartData = remember(reports, selectedChartType) {
        reports.sortedBy { it.date }.takeLast(7).map { r ->
            val dateLabel = BanglaNumberFormatter.formatShortDate(r.date).take(5)
            when (selectedChartType) {
                ChartType.EGG_PRODUCTION -> ChartEntry(
                    dateLabel = dateLabel,
                    barValue = r.eggProduction.toFloat(),
                    readout = "${BanglaNumberFormatter.formatNumber(r.eggProduction)} টি"
                )
                ChartType.DAILY_SALES -> ChartEntry(
                    dateLabel = dateLabel,
                    barValue = (r.totalSale / 1000).toFloat(), // in thousands
                    readout = BanglaNumberFormatter.formatCurrency(r.totalSale)
                )
                ChartType.EXPENSE_TREND -> ChartEntry(
                    dateLabel = dateLabel,
                    barValue = (r.medicineCost / 100).toFloat(), // in hundreds
                    readout = BanglaNumberFormatter.formatCurrency(r.medicineCost)
                )
            }
        }
    }

    val maxVal = remember(chartData) {
        max(chartData.maxOfOrNull { it.barValue } ?: 100f, 10f)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(chartData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 600))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("production_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedChartType.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                    )
                    Text(
                        text = if (selectedChartType == ChartType.DAILY_SALES) "হাজার টাকায় (৳'০০০)" else "গত ৭ দিনের চিত্র",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Chart options",
                            tint = onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        ChartType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.title) },
                                onClick = {
                                    selectedChartType = type
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "গ্রাফ প্রদর্শনের জন্য কমপক্ষে ১ দিনের রিপোর্ট প্রয়োজন",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                // Interactive Bar Graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .pointerInput(chartData) {
                                detectTapGestures { offset ->
                                    val spacing = size.width / chartData.size
                                    val index = (offset.x / spacing).toInt()
                                    selectedIndex = if (index in chartData.indices) index else -1
                                }
                            }
                    ) {
                        val count = chartData.size
                        if (count > 0) {
                            val slotWidth = size.width / count
                            val barWidth = slotWidth * 0.58f

                            for (i in 0 until count) {
                                val item = chartData[i]
                                val ratio = (item.barValue / maxVal) * animationProgress.value
                                val barHeight = size.height * ratio.coerceIn(0.1f, 1f)
                                val x = i * slotWidth + (slotWidth - barWidth) / 2
                                val y = size.height - barHeight

                                // Gradient tones from light green to deep emerald
                                val alpha = 0.35f + (i.toFloat() / max(count - 1, 1)) * 0.65f
                                val barColor = primaryColor.copy(alpha = alpha)

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )

                                if (selectedIndex == i) {
                                    drawRoundRect(
                                        color = primaryContainer,
                                        topLeft = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()),
                                        size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // X-Axis Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        chartData.forEachIndexed { index, entry ->
                            Text(
                                text = entry.dateLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedIndex == index) primaryColor else onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            if (selectedIndex in chartData.indices) {
                val selected = chartData[selectedIndex]
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryContainer.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "তারিখ: ${selected.dateLabel}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = primaryColor
                        )
                    )
                    Text(
                        text = "পরিমাণ: ${selected.readout}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                }
            }
        }
    }
}
