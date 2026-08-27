package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Refill
import com.example.ui.theme.BrightMint
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailySpend(
    val dayLabel: String,
    val amount: Double,
    val isToday: Boolean = false
)

/**
 * Material 3 Bar Chart for "Weekly Expenses"
 * Displays fuel expenditure over the past 7 days with animated rounded bars,
 * day badges, and weekly summary telemetry.
 */
@Composable
fun WeeklyExpensesBarChart(
    refills: List<Refill>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // Aggregate last 7 days expenses
    val weeklyData = remember(refills) {
        val cal = Calendar.getInstance()
        val list = mutableListOf<DailySpend>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val dayStart = dayCal.timeInMillis

            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            dayCal.set(Calendar.SECOND, 59)
            dayCal.set(Calendar.MILLISECOND, 999)
            val dayEnd = dayCal.timeInMillis

            val dayTotal = refills
                .filter { it.date in dayStart..dayEnd }
                .sumOf { it.totalAmount }

            list.add(
                DailySpend(
                    dayLabel = dayFormat.format(Date(dayStart)),
                    amount = dayTotal,
                    isToday = (i == 0)
                )
            )
        }
        list
    }

    val totalWeeklySpend = weeklyData.sumOf { it.amount }
    val maxSpend = weeklyData.maxOfOrNull { it.amount }?.takeIf { it > 0.0 } ?: 1000.0
    val avgDailySpend = totalWeeklySpend / 7.0

    val animFactor by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "barHeightAnim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_weekly_expenses_chart"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Total Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weekly Expenses",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Last 7 days fuel spending",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${totalWeeklySpend.toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                    Text(
                        text = "Avg ₹${avgDailySpend.toInt()}/day",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bar Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(125.dp)) {
                    val w = size.width
                    val h = size.height
                    val barCount = weeklyData.size
                    val slotWidth = w / barCount
                    val barWidth = slotWidth * 0.48f

                    // Dotted Average Line
                    if (avgDailySpend > 0 && maxSpend > 0) {
                        val avgY = (h - ((avgDailySpend / maxSpend) * (h * 0.85f))).toFloat()
                        drawLine(
                            color = primaryColor.copy(alpha = 0.4f),
                            start = Offset(0f, avgY),
                            end = Offset(w, avgY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Draw Bars
                    weeklyData.forEachIndexed { index, data ->
                        val centerX = (index * slotWidth) + (slotWidth / 2f)
                        val left = centerX - (barWidth / 2f)
                        val barHeight = if (maxSpend > 0) {
                            ((data.amount / maxSpend) * (h * 0.85f) * animFactor).toFloat()
                        } else 0f

                        val top = h - barHeight

                        // Background pillar slot
                        drawRoundRect(
                            color = surfaceVariant.copy(alpha = 0.35f),
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Active Filled Bar
                        if (barHeight > 4f) {
                            val barBrush = if (data.isToday) {
                                Brush.verticalGradient(
                                    listOf(BrightMint, primaryColor),
                                    startY = top,
                                    endY = h
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(primaryColor.copy(alpha = 0.9f), secondaryColor.copy(alpha = 0.7f)),
                                    startY = top,
                                    endY = h
                                )
                            }

                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Day Labels & Amount Readouts under Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weeklyData.forEach { data ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(38.dp)
                    ) {
                        Text(
                            text = if (data.amount > 0) "₹${data.amount.toInt()}" else "-",
                            fontSize = 10.sp,
                            fontWeight = if (data.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (data.isToday) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (data.isToday) primaryColor.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Text(
                                text = data.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = if (data.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (data.isToday) primaryColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
