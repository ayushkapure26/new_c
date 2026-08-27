package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.ui.theme.BrightMint
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Material 3 Automotive Digital Instrument Cluster with premium CNG fuel tracker aesthetic.
 * Features:
 * - Glassmorphism surface with luminous accents
 * - Custom sweeping efficiency arc gauge with dynamic needle glow
 * - Dual digital telemetry pods (CNG Fuel Level / Pressure & Predictive Driving Range)
 * - Active vehicle ribbon with registration and tank specs
 */
@Composable
fun AutomotiveClusterCard(
    activeCar: Car?,
    activeCarMileage: Double,
    estimatedRangeKm: Double,
    tankFillPercent: Float,
    gasPressureBar: Double,
    onSwitchVehicleClick: () -> Unit,
    driverName: String? = null,
    isDriverLoggedIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Smooth animations for gauges
    val animatedMileage by animateFloatAsState(
        targetValue = activeCarMileage.toFloat().coerceIn(10f, 40f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "mileageAnim"
    )

    val animatedTankFill by animateFloatAsState(
        targetValue = tankFillPercent.coerceIn(0.1f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "tankAnim"
    )

    // Glassmorphic cockpit gradients
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F2624),
                Color(0xFF071716),
                Color(0xFF040D0C)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                DarkTeal,
                Color(0xFF082B27),
                Color(0xFF041816)
            )
        )
    }

    val glassBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.25f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("automotive_instrument_cluster"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, glassBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: Active Vehicle Ribbon & Quick Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = BrightMint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            if (isDriverLoggedIn && !driverName.isNullOrBlank()) {
                                Text(
                                    text = "DRIVER: ${driverName.uppercase()}",
                                    color = BrightMint,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = activeCar?.name ?: "Maruti WagonR CNG",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "${activeCar?.regNumber ?: "DL 3C AY 4021"} • ${activeCar?.tankCapacityKg ?: 10.0} kg Cylinder",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Switch Car Glass Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSwitchVehicleClick)
                            .testTag("cluster_switch_car_btn"),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Vehicle",
                                tint = BrightMint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Garage",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Central Cockpit Arc Gauge + Dual Telemetry Pods
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Pod: CNG Fuel Tank Level & Pressure
                    GlassTelemetryPod(
                        label = "CNG TANK",
                        mainValue = "${(animatedTankFill * 100).toInt()}%",
                        subValue = "${gasPressureBar.toInt()} Bar",
                        indicatorColor = BrightMint,
                        modifier = Modifier.weight(1f),
                        testTag = "pod_cng_tank"
                    )

                    // Center: Sweeping Digital Cockpit Dial
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .testTag("center_cockpit_arc_gauge"),
                        contentAlignment = Alignment.Center
                    ) {
                        DigitalClusterArcGauge(
                            currentMileage = animatedMileage,
                            minMileage = 10f,
                            maxMileage = 40f
                        )

                        // Center Digital Readout
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = BrightMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", animatedMileage),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "KM / KG",
                                color = BrightMint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BrightMint.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "OPTIMAL ECO",
                                    color = BrightMint,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Right Pod: Estimated Remaining Range
                    GlassTelemetryPod(
                        label = "EST. RANGE",
                        mainValue = "${estimatedRangeKm.toInt()}",
                        subValue = "KM LEFT",
                        indicatorColor = ElectricCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "pod_est_range"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Cockpit Bar: Quick Status Indicators (Eco Status & Fuel Mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrightMint)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CNG Active Mode",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "High Pressure Dispenser",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sweeping circular arc gauge representing vehicle efficiency with glowing neon accents.
 */
@Composable
private fun DigitalClusterArcGauge(
    currentMileage: Float,
    minMileage: Float,
    maxMileage: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 10.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        val startAngle = 140f
        val sweepAngle = 260f

        // 1. Background Arc Track
        drawArc(
            color = Color.White.copy(alpha = 0.12f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Active Progress Arc with Gradient
        val progressRatio = ((currentMileage - minMileage) / (maxMileage - minMileage)).coerceIn(0.05f, 1f)
        val activeSweep = sweepAngle * progressRatio

        val arcBrush = Brush.sweepGradient(
            0.0f to BrightMint,
            0.5f to ElectricCyan,
            1.0f to BrightMint
        )

        drawArc(
            brush = arcBrush,
            startAngle = startAngle,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Gauge Needle Indicator Glow Point
        val currentAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
        val radius = diameter / 2
        val centerX = size.width / 2
        val centerY = size.height / 2

        val needleX = (centerX + radius * cos(currentAngleRad)).toFloat()
        val needleY = (centerY + radius * sin(currentAngleRad)).toFloat()

        // Outer glow
        drawCircle(
            color = BrightMint.copy(alpha = 0.35f),
            radius = 10.dp.toPx(),
            center = Offset(needleX, needleY)
        )
        // Solid core
        drawCircle(
            color = Color.White,
            radius = 4.5.dp.toPx(),
            center = Offset(needleX, needleY)
        )
    }
}

/**
 * Translucent Glassmorphic Telemetry Pod
 */
@Composable
private fun GlassTelemetryPod(
    label: String,
    mainValue: String,
    subValue: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .testTag(testTag),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mainValue,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subValue,
                color = indicatorColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
