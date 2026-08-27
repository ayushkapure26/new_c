package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Pump
import com.example.data.model.PumpRating
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.PumpViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PumpDetailDialog(
    pump: Pump,
    distanceKm: Double,
    viewModel: PumpViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ratings by viewModel.getRatingsForPump(pump.id).collectAsState(initial = emptyList())

    var showAddRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    var showLiveReportDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkTeal)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pump.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (pump.isSmartPick) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFD54F)
                            ) {
                                Text(
                                    text = "★ SMART PICK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${pump.provider} • ${pump.city} • $distanceKm km away",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { viewModel.toggleFavorite(pump.id, pump.isFavorite) }) {
                    Icon(
                        imageVector = if (pump.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (pump.isFavorite) Color(0xFFFF5252) else Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // CNG Margdarshak Live Stock Banner
                item {
                    val stockBg = when (pump.stockStatus) {
                        "OUT_OF_STOCK" -> Color(0xFFFFEBEE)
                        "NEEDS_UPDATE" -> Color(0xFFFFF8E1)
                        else -> Color(0xFFE8F5E9)
                    }
                    val stockColor = when (pump.stockStatus) {
                        "OUT_OF_STOCK" -> Color(0xFFD32F2F)
                        "NEEDS_UPDATE" -> Color(0xFFF57C00)
                        else -> Color(0xFF2E7D32)
                    }
                    val stockTitle = when (pump.stockStatus) {
                        "OUT_OF_STOCK" -> "🔴 PUMP DRY / OUT OF STOCK"
                        "NEEDS_UPDATE" -> "🟡 STOCK UNVERIFIED (>2h)"
                        else -> "🟢 CNG IN STOCK & READY"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = stockBg),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, stockColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stockTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = stockColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Reported by: ${pump.reportedByDriver} • Queue: ~${pump.queueWaitMinutes} mins",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { showLiveReportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = stockColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Update", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Key Stats Banner
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Price Box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("CNG Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₹${pump.pricePerKg}/kg",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }

                        // Pressure Box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Gas Pressure", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${pump.gasPressureBar.toInt()} bar",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Highway Corridor Box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkTeal.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Corridor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    pump.highwayCorridor.split("-").first().trim(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTeal,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address Card & Timestamp
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = DarkTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pump.address,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Provider: ${pump.provider} • Highway: ${pump.highwayCorridor} • Last Sync: ${dateFormat.format(Date(pump.lastUpdatedTime))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Call / Navigate / Report Live)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.util.NavigationUtil.navigateToPump(
                                    context = context,
                                    latitude = pump.latitude,
                                    longitude = pump.longitude,
                                    pumpName = pump.name
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showLiveReportDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Report", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pump.phone}"))
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ratings Header & Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${pump.rating}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Based on ${pump.ratingCount} ratings",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { showAddRatingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Rate Pump", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Driver Reviews",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Reviews List
                items(ratings) { rating ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rating.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${rating.overallRating}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFFFB300)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (rating.reviewText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rating.reviewText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Rating Dialog
    if (showAddRatingDialog) {
        AddRatingModal(
            onDismiss = { showAddRatingDialog = false },
            onSubmit = { name, stars, reviewText ->
                viewModel.addRating(
                    pumpId = pump.id,
                    userName = name,
                    overallRating = stars,
                    gasRating = stars,
                    pressureRating = stars,
                    waitingRating = stars,
                    staffRating = stars,
                    cleanlinessRating = stars,
                    priceAccuracyRating = 5.0f,
                    reviewText = reviewText
                )
                showAddRatingDialog = false
            }
        )
    }

    // Report Incorrect Info Dialog
    if (showReportDialog) {
        ReportIncorrectInfoModal(
            onDismiss = { showReportDialog = false },
            onSubmit = { note ->
                viewModel.reportIncorrectInfo(pump.id, note)
                showReportDialog = false
            }
        )
    }

    // Driver Live Crowdsource Status Dialog
    if (showLiveReportDialog) {
        ReportLiveStatusModal(
            currentStatus = pump.stockStatus,
            currentPressure = pump.gasPressureBar,
            currentQueue = pump.queueWaitMinutes,
            onDismiss = { showLiveReportDialog = false },
            onSubmit = { status, pressure, queue, isGasAvail, driverName ->
                viewModel.reportLivePumpStatus(
                    pumpId = pump.id,
                    stockStatus = status,
                    pressureBar = pressure,
                    queueMinutes = queue,
                    isGasAvailable = isGasAvail,
                    reporterName = driverName
                )
                showLiveReportDialog = false
            }
        )
    }
}

@Composable
fun ReportLiveStatusModal(
    currentStatus: String,
    currentPressure: Double,
    currentQueue: Int,
    onDismiss: () -> Unit,
    onSubmit: (stockStatus: String, pressure: Double, queue: Int, isGasAvail: Boolean, driverName: String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var selectedPressure by remember { mutableStateOf(currentPressure) }
    var selectedQueue by remember { mutableStateOf(currentQueue) }
    var driverName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Driver Live Status Report", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Help fellow drivers know CNG stock & pressure before they arrive.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("CNG Stock Availability:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Available (Green)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedStatus == "AVAILABLE") Color(0xFF2E7D32) else Color(0xFFE8F5E9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedStatus = "AVAILABLE" }
                    ) {
                        Text(
                            text = "🟢 In Stock",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedStatus == "AVAILABLE") Color.White else Color(0xFF2E7D32),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Dry / Out of stock (Red)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedStatus == "OUT_OF_STOCK") Color(0xFFD32F2F) else Color(0xFFFFEBEE),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedStatus = "OUT_OF_STOCK" }
                    ) {
                        Text(
                            text = "🔴 Pump Dry",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedStatus == "OUT_OF_STOCK") Color.White else Color(0xFFD32F2F),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Needs Check (Amber)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedStatus == "NEEDS_UPDATE") Color(0xFFF57C00) else Color(0xFFFFF8E1),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedStatus = "NEEDS_UPDATE" }
                    ) {
                        Text(
                            text = "🟡 Slow/Low",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedStatus == "NEEDS_UPDATE") Color.White else Color(0xFFF57C00),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pressure Selector
                Text("Dispenser Pressure: ${selectedPressure.toInt()} Bar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(180.0, 200.0, 215.0, 225.0).forEach { bar ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPressure == bar) DarkTeal else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPressure = bar }
                        ) {
                            Text(
                                text = "${bar.toInt()} Bar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPressure == bar) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estimated Queue Time
                Text("Estimated Waiting Queue: $selectedQueue mins", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 5, 15, 30).forEach { mins ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedQueue == mins) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedQueue = mins }
                        ) {
                            Text(
                                text = if (mins == 0) "No Queue" else "$mins m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedQueue == mins) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = driverName,
                    onValueChange = { driverName = it },
                    label = { Text("Your Name / Taxi No (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = if (driverName.isNotBlank()) driverName else "Community Driver"
                    val isGasAvail = selectedStatus != "OUT_OF_STOCK"
                    onSubmit(selectedStatus, selectedPressure, selectedQueue, isGasAvail, name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Publish Live Update", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddRatingModal(
    onDismiss: () -> Unit,
    onSubmit: (String, Float, String) -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var stars by remember { mutableStateOf(5.0f) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate & Review Pump", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Rating: ${stars.toInt()} Stars", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (i <= stars) Color(0xFFFFB300) else Color.LightGray,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { stars = i.toFloat() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Review (Pressure, Waiting time, Staff)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(userName, stars, reviewText) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
            ) {
                Text("Submit Review", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReportIncorrectInfoModal(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Incorrect Info", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Help keep station info accurate for all drivers.", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Issue (e.g. Price changed, Closed, Low pressure)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reason.isNotBlank()) onSubmit(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
            ) {
                Text("Report Issue", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
