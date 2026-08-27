package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.ChartDataPoint
import java.util.Locale

@Composable
fun PriceHistoryChart(
    title: String = "Historical CNG Price Fluctuations",
    points: List<ChartDataPoint>,
    minPrice: Double = 0.0,
    maxPrice: Double = 0.0,
    latestPrice: Double = 0.0,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val values = points.map { it.value }
    val rawMin = if (minPrice > 0) minPrice.toFloat() else values.minOrNull() ?: 70f
    val rawMax = if (maxPrice > 0) maxPrice.toFloat() else values.maxOrNull() ?: 80f

    // Buffer range for canvas padding
    val yMin = (rawMin - 1f).coerceAtLeast(0f)
    val yMax = rawMax + 1.5f
    val yRange = if (yMax - yMin > 0f) yMax - yMin else 10f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cng_price_history_chart_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rate history trend (stored PriceHistory data)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (latestPrice > 0) {
                    Box(
                        modifier = Modifier
                            .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", latestPrice)}/kg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Line Chart Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val count = points.size

                    val xStep = if (count > 1) w / (count - 1) else w

                    // Draw Horizontal Grid Lines
                    val gridColor = Color.LightGray.copy(alpha = 0.25f)
                    for (i in 0..3) {
                        val gridY = h * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gridY),
                            end = Offset(w, gridY),
                            strokeWidth = 1f
                        )
                    }

                    // Compute Point Coordinates
                    val coordinates = points.mapIndexed { idx, point ->
                        val x = idx * xStep
                        val normalizedY = (point.value - yMin) / yRange
                        val y = h - (normalizedY * h * 0.82f) - (h * 0.08f)
                        Offset(x, y)
                    }

                    if (coordinates.isNotEmpty()) {
                        // 1. Draw Line Gradient Area
                        val fillPath = Path().apply {
                            moveTo(coordinates.first().x, h)
                            lineTo(coordinates.first().x, coordinates.first().y)

                            for (i in 0 until coordinates.size - 1) {
                                val p1 = coordinates[i]
                                val p2 = coordinates[i + 1]
                                val controlX1 = (p1.x + p2.x) / 2f
                                cubicTo(controlX1, p1.y, controlX1, p2.y, p2.x, p2.y)
                            }

                            lineTo(coordinates.last().x, h)
                            close()
                        }

                        val areaGradient = Brush.verticalGradient(
                            colors = listOf(
                                EmeraldGreen.copy(alpha = 0.35f),
                                DarkTeal.copy(alpha = 0.05f)
                            )
                        )
                        drawPath(path = fillPath, brush = areaGradient)

                        // 2. Draw Smooth Curve Line
                        val linePath = Path().apply {
                            moveTo(coordinates.first().x, coordinates.first().y)
                            for (i in 0 until coordinates.size - 1) {
                                val p1 = coordinates[i]
                                val p2 = coordinates[i + 1]
                                val controlX1 = (p1.x + p2.x) / 2f
                                cubicTo(controlX1, p1.y, controlX1, p2.y, p2.x, p2.y)
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = DarkTeal,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 3. Draw Data Point Dots & Price Text
                        coordinates.forEachIndexed { idx, point ->
                            // Outer Halo
                            drawCircle(
                                color = EmeraldGreen.copy(alpha = 0.3f),
                                radius = 7.dp.toPx(),
                                center = point
                            )
                            // Solid Inner Dot
                            drawCircle(
                                color = DarkTeal,
                                radius = 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { point ->
                    Text(
                        text = point.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Min / Max Badge
            if (rawMin > 0 && rawMax > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lowest Rate: ₹${String.format(Locale.US, "%.2f", rawMin)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Highest Rate: ₹${String.format(Locale.US, "%.2f", rawMax)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkTeal
                    )
                }
            }
        }
    }
}
