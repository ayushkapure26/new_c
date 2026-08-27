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
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightMint
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Circular Gauge Chart for "Current Mileage"
 * Displays active vehicle mileage with an animated 240° sweep arc, tick marks,
 * center digital readout, and efficiency rating.
 */
@Composable
fun CircularMileageGauge(
    currentMileage: Double,
    maxMileageScale: Double = 35.0,
    vehicleName: String = "Active Vehicle",
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val safeMileage = currentMileage.coerceIn(0.0, maxMileageScale)
    val progress = (safeMileage / maxMileageScale).toFloat()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "mileageProgressAnim"
    )

    val efficiencyStatus = when {
        safeMileage >= 25.0 -> "Exceptional Eco" to EmeraldGreen
        safeMileage >= 20.0 -> "Optimal Efficiency" to primaryColor
        safeMileage >= 15.0 -> "Moderate Mileage" to Color(0xFFE6A100)
        else -> "Standard Economy" to Color(0xFFE65100)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_current_mileage_gauge"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
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
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Current Mileage",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = vehicleName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = efficiencyStatus.second.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, efficiencyStatus.second.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = efficiencyStatus.second,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = efficiencyStatus.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = efficiencyStatus.second
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Gauge Canvas
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val radius = (w / 2f) - 18.dp.toPx()
                    val strokeWidth = 14.dp.toPx()

                    // Gauge spans 240 degrees (from 150° to 390° / 30°)
                    val startAngle = 150f
                    val sweepAngleTotal = 240f

                    // 1. Background Arc
                    drawArc(
                        color = surfaceVariant.copy(alpha = 0.5f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngleTotal,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 2. Tick marks
                    val numTicks = 8
                    for (i in 0..numTicks) {
                        val angleDeg = startAngle + (sweepAngleTotal * (i.toFloat() / numTicks))
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val tickInnerRadius = radius - (strokeWidth / 2f) - 6.dp.toPx()
                        val tickOuterRadius = radius - (strokeWidth / 2f) - 1.dp.toPx()

                        val startX = center.x + tickInnerRadius * cos(angleRad).toFloat()
                        val startY = center.y + tickInnerRadius * sin(angleRad).toFloat()
                        val endX = center.x + tickOuterRadius * cos(angleRad).toFloat()
                        val endY = center.y + tickOuterRadius * sin(angleRad).toFloat()

                        drawLine(
                            color = surfaceVariant.copy(alpha = 0.8f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // 3. Active Gradient Sweep Arc
                    val activeSweep = sweepAngleTotal * animatedProgress
                    if (activeSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to BrightMint,
                                0.5f to primaryColor,
                                1.0f to secondaryColor,
                                center = center
                            ),
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 4. Glowing Indicator Dot at Arc Tip
                        val tipAngleDeg = startAngle + activeSweep
                        val tipAngleRad = Math.toRadians(tipAngleDeg.toDouble())
                        val dotX = center.x + radius * cos(tipAngleRad).toFloat()
                        val dotY = center.y + radius * sin(tipAngleRad).toFloat()

                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 3.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // Center Digital Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (safeMileage > 0) String.format(Locale.US, "%.1f", safeMileage) else "--.-",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "km / kg",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trip Fuel Economy",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Gauge Labels (0, 15, 35+ km/kg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0 km/kg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Target: 25 km/kg",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
                Text(text = "${maxMileageScale.toInt()} km/kg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
