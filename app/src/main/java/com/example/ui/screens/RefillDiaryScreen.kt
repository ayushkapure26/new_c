package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.Pump
import com.example.data.model.Refill
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.components.DriverRefillBottomSheet
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.RefillViewModel
import com.example.util.AppLocalization
import com.example.util.CsvExportUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material3.ExperimentalMaterial3Api

enum class RefillFilterOption {
    ALL,
    THIS_MONTH,
    HIGHEST_MILEAGE,
    FULL_TANK_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillDiaryScreen(
    refillViewModel: RefillViewModel,
    selectedLanguage: String = "English"
) {
    val context = LocalContext.current
    val strings = remember(selectedLanguage) { AppLocalization.getStrings(selectedLanguage) }
    val refills by refillViewModel.refillsList.collectAsState()
    val cars by refillViewModel.carsList.collectAsState()
    val pumps by refillViewModel.pumpsList.collectAsState()

    var showAddModal by rememberSaveable { mutableStateOf(false) }
    var selectedRefillForEdit by remember { mutableStateOf<Refill?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(RefillFilterOption.ALL) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val filteredRefills by remember(refills, searchQuery, selectedFilter) {
        derivedStateOf {
            var list = refills.filter { refill ->
                if (searchQuery.isBlank()) true
                else {
                    refill.pumpName.contains(searchQuery, ignoreCase = true) ||
                    refill.notes.contains(searchQuery, ignoreCase = true)
                }
            }

            when (selectedFilter) {
                RefillFilterOption.ALL -> list
                RefillFilterOption.THIS_MONTH -> {
                    val oneMonthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                    list.filter { it.date >= oneMonthAgo }
                }
                RefillFilterOption.HIGHEST_MILEAGE -> list.sortedByDescending { it.mileageKmPerKg }
                RefillFilterOption.FULL_TANK_ONLY -> list.filter { it.isFullRefill }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedRefillForEdit = null
                    showAddModal = true
                },
                icon = {
                    Icon(Icons.Default.Add, contentDescription = "Add Refill", tint = Color.White)
                },
                text = {
                    Text("+ Quick Refill", fontWeight = FontWeight.Bold, color = Color.White)
                },
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_refill_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Row with Export Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Refill History & Diary",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filteredRefills.size} Logged CNG Fills",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (refills.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkTeal.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, DarkTeal.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val csvContent = CsvExportUtil.buildFuelExpenseCsv(
                                    refills = refills,
                                    cars = cars,
                                    reportTitle = "CNG Mileage Diary & Fueling Backup"
                                )
                                val fileName = CsvExportUtil.getSuggestedFileName("CNG_Diary_Backup")
                                val file = CsvExportUtil.saveCsvToCache(context, csvContent, fileName)
                                if (file != null) {
                                    CsvExportUtil.shareCsvFile(context, file, chooserTitle = "Export CNG Fueling Backup")
                                } else {
                                    Toast.makeText(context, "Failed to create CSV backup", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("btn_export_diary_csv")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = DarkTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export CSV",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("refill_search_input"),
                placeholder = { Text("Search by station name or notes...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DarkTeal,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == RefillFilterOption.ALL,
                        onClick = { selectedFilter = RefillFilterOption.ALL },
                        label = { Text("All Fills", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == RefillFilterOption.THIS_MONTH,
                        onClick = { selectedFilter = RefillFilterOption.THIS_MONTH },
                        label = { Text("📅 This Month", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == RefillFilterOption.HIGHEST_MILEAGE,
                        onClick = { selectedFilter = RefillFilterOption.HIGHEST_MILEAGE },
                        label = { Text("⚡ Top Mileage", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == RefillFilterOption.FULL_TANK_ONLY,
                        onClick = { selectedFilter = RefillFilterOption.FULL_TANK_ONLY },
                        label = { Text("⛽ Full Tank", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredRefills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No Matching Refills" else "No Refill Records Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different station name" else "Tap '+ Quick Refill' to log your first CNG tank fill!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredRefills, key = { it.id }) { refill ->
                        val car = cars.find { it.id == refill.carId }
                        val carLabel = car?.let { "${it.name} (${it.regNumber})" } ?: "CNG Vehicle"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("refill_item_${refill.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Top Row: Pump Name, Date & Total Cost
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(DarkTeal.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalGasStation,
                                                contentDescription = null,
                                                tint = DarkTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = refill.pumpName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$carLabel • ${dateFormat.format(Date(refill.date))}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${refill.totalAmount.toInt()}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = EmeraldGreen
                                        )
                                        Text(
                                            text = "${refill.quantityKg} kg @ ₹${String.format("%.2f", refill.pricePerKg)}/kg",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Automotive Telemetry Metrics Bar:
                                // [ Odometer | Distance | Mileage (km/kg) | Cost/km ]
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkTeal.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, DarkTeal.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Odometer", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${refill.odometer.toInt()} km", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Column {
                                            Text("Trip Run", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${refill.distanceTravelled.toInt()} km", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Column {
                                            Text("Tank Mileage", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${refill.mileageKmPerKg} km/kg",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EmeraldGreen
                                            )
                                        }

                                        Column {
                                            Text("Cost / km", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "₹${String.format("%.2f", refill.costPerKm)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkTeal
                                            )
                                        }
                                    }
                                }

                                if (refill.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Notes: ${refill.notes}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedRefillForEdit = refill
                                            showAddModal = true
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = DarkTeal)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { refillViewModel.deleteRefill(refill) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val haptic = LocalHapticFeedback.current

    if (showAddModal) {
        DriverRefillBottomSheet(
            cars = cars,
            pumps = pumps,
            existingRefill = selectedRefillForEdit,
            onDismiss = { showAddModal = false },
            onSave = { id, carId, pumpId, pumpName, odo, qty, price, total, isFull, notes ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                refillViewModel.saveRefill(id, carId, pumpId, pumpName, odo, qty, price, total, isFull, notes)
                showAddModal = false
            }
        )
    }
}
