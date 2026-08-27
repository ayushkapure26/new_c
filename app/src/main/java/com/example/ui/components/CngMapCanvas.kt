package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Pump
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.PumpDisplayItem

@Composable
fun CngMapCanvas(
    pumps: List<PumpDisplayItem>,
    onPumpSelected: (Pump) -> Unit,
    onNavigateClick: (Pump) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPumpItem by remember { mutableStateOf<PumpDisplayItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(Color(0xFF1B2A28), RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pumps) {
                    detectTapGestures { tapOffset ->
                        val w = size.width
                        val h = size.height
                        val centerX = w / 2f
                        val centerY = h / 2f

                        // Find closest pump pin to tap
                        var closest: PumpDisplayItem? = null
                        var minDistanceSq = 2500f // 50px radius squared

                        pumps.take(8).forEachIndexed { idx, item ->
                            val angle = idx * (2 * Math.PI / minOf(pumps.size, 8))
                            val distPx = minOf(w, h) * 0.32f
                            val px = (centerX + cos(angle) * distPx).toFloat()
                            val py = (centerY + sin(angle) * distPx).toFloat()

                            val dx = tapOffset.x - px
                            val dy = tapOffset.y - py
                            val dSq = dx * dx + dy * dy
                            if (dSq < minDistanceSq) {
                                minDistanceSq = dSq
                                closest = item
                            }
                        }

                        if (closest != null) {
                            selectedPumpItem = closest
                            onPumpSelected(closest!!.pump)
                        } else {
                            selectedPumpItem = null
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // Map Grid Background Lines
            val gridColor = Color(0x1AFFFFFF)
            for (i in 0..6) {
                val y = h * (i / 6f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            for (i in 0..8) {
                val x = w * (i / 8f)
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            }

            // Concentric Distance Radius Rings (5km, 10km)
            drawCircle(
                color = Color(0x3300E676),
                radius = minOf(w, h) * 0.22f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = Color(0x1A00E676),
                radius = minOf(w, h) * 0.38f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // User Location Dot (Center)
            drawCircle(
                color = Color(0x3300E5FF),
                radius = 20f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 8f,
                center = Offset(centerX, centerY)
            )

            // Render Pump Station Pins
            pumps.take(8).forEachIndexed { idx, item ->
                val angle = idx * (2 * Math.PI / minOf(pumps.size, 8))
                val distPx = minOf(w, h) * 0.32f
                val px = (centerX + cos(angle) * distPx).toFloat()
                val py = (centerY + sin(angle) * distPx).toFloat()

                val isSelected = selectedPumpItem?.pump?.id == item.pump.id

                val pinColor = when {
                    !item.pump.isOpen || !item.pump.isGasAvailable -> Color(0xFFFF5252)
                    item.pump.gasPressureBar < 190.0 -> Color(0xFFFFB300)
                    else -> Color(0xFF00E676)
                }

                // Connecting line to center user location
                drawLine(
                    color = pinColor.copy(alpha = 0.3f),
                    start = Offset(centerX, centerY),
                    end = Offset(px, py),
                    strokeWidth = 2f
                )

                // Pin Outer Halo
                drawCircle(
                    color = pinColor.copy(alpha = if (isSelected) 0.5f else 0.2f),
                    radius = if (isSelected) 24f else 18f,
                    center = Offset(px, py)
                )

                // Pin Body
                drawCircle(
                    color = pinColor,
                    radius = if (isSelected) 14f else 10f,
                    center = Offset(px, py)
                )

                // Pin Center Core
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(px, py)
                )
            }
        }

        // Overlay Label Header
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color(0xCC0D3B36), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF00E5FF), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Interactive Station Map",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Selected Pin Detail Popup Card
        selectedPumpItem?.let { item ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.pump.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹${item.pump.pricePerKg}/kg",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${item.pump.gasPressureBar.toInt()} bar",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${item.distanceKm} km away",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigateClick(item.pump) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Navigate", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun cos(rad: Double): Double = kotlin.math.cos(rad)
private fun sin(rad: Double): Double = kotlin.math.sin(rad)
