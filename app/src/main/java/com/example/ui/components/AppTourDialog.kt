package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightMint
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen

data class TourStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val badgeText: String,
    val highlights: List<String>
)

@Composable
fun AppTourDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            TourStep(
                stepNumber = 1,
                title = "Automotive Cockpit & Gas Pressure",
                subtitle = "Live Fuel Tank & Real-Time Bar Pressure",
                description = "Monitor your active vehicle's CNG tank level, real-time gas pressure (200+ Bar optimal), and estimated driving range at a single glance.",
                icon = Icons.Default.Speed,
                badgeText = "COCKPIT DASHBOARD",
                highlights = listOf(
                    "Real-time pressure gauge in Bar",
                    "Estimated remaining range in km",
                    "Active vehicle selector with 1-tap switch"
                )
            ),
            TourStep(
                stepNumber = 2,
                title = "CNG Station Status & Live Stocks",
                subtitle = "Tri-Color Status: In Stock, Low Pressure, or Dry",
                description = "Find nearby pumps sorted by distance, price, or rating. View live open/closed timings and gas pressure before you drive.",
                icon = Icons.Default.LocalGasStation,
                badgeText = "LIVE PUMP FINDER",
                highlights = listOf(
                    "Live CNG price/kg & operating hours",
                    "Tri-color stock availability flags",
                    "1-tap Google Maps turn-by-turn navigation"
                )
            ),
            TourStep(
                stepNumber = 3,
                title = "1-Tap Quick Driver Actions",
                subtitle = "Built for Fast In-Car Driver Ergonomics",
                description = "Large, easy-to-tap driver buttons allow you to quickly log refills, add vehicles, check history, or launch navigation with zero clutter.",
                icon = Icons.Default.Navigation,
                badgeText = "QUICK ACTIONS",
                highlights = listOf(
                    "One-tap 'Find CNG Pump' & 'Navigate'",
                    "Instant 'Quick Refill' logger dialog",
                    "Direct access to Refill Diary & Analytics"
                )
            ),
            TourStep(
                stepNumber = 4,
                title = "Smart Mileage & Expense Auto-Calculator",
                subtitle = "Auto-Calculated ₹/kg, km/kg Mileage, and ₹/km Cost",
                description = "Enter amount and kilograms—the app automatically computes ₹/kg, trip fuel economy (km/kg), running cost (₹/km), and monthly savings.",
                icon = Icons.Default.Analytics,
                badgeText = "EXPENSE ENGINE",
                highlights = listOf(
                    "Instant ₹/kg and km/kg calculation",
                    "Daily, weekly, and monthly expense analytics",
                    "One-tap CSV & PDF export for expense backup"
                )
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("app_tour_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Progress Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkTeal.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "GUIDE • ${currentStepIndex + 1}/${steps.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkTeal,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        steps.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .width(if (index == currentStepIndex) 20.dp else 6.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == currentStepIndex) DarkTeal else DarkTeal.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Skip", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Animated step content
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tour_content_transition"
                ) { step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Icon Hero Box
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(DarkTeal, EmeraldGreen)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = step.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = step.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = step.subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkTeal,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = step.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Highlights List
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                step.highlights.forEach { highlight ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldGreen.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = highlight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("tour_next_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentStepIndex == steps.size - 1) "Get Started" else "Next Step",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (currentStepIndex == steps.size - 1) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        dismissButton = {
            if (currentStepIndex > 0) {
                OutlinedButton(
                    onClick = { currentStepIndex-- },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DarkTeal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", color = DarkTeal)
                }
            }
        }
    )
}
