package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.Pump
import com.example.data.model.Refill
import com.example.ui.theme.BrightMint
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActiveInputTarget {
    TOTAL_AMOUNT,
    QUANTITY_KG,
    ODOMETER,
    CNG_PRICE
}

/**
 * Material 3 Swipe-Up ModalBottomSheet for "Log CNG Refill"
 * Features:
 * - Swipe-up gesture sheet
 * - High-contrast large custom number pad designed for rapid one-handed driver thumb entry
 * - Interactive field selection (Total Spend ₹, Gas Quantity kg, Odometer Reading km, Price ₹/kg)
 * - Real-time auto-calculation of trip mileage and running cost
 * - Subtle haptic feedback vibration on keypad taps and Save button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRefillBottomSheet(
    cars: List<Car>,
    pumps: List<Pump>,
    existingRefill: Refill? = null,
    lastOdometerReading: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (id: Long, carId: Long, pumpId: Long?, pumpName: String, odo: Double, qty: Double, price: Double, total: Double, isFull: Boolean, notes: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    var selectedCarId by remember {
        mutableStateOf(existingRefill?.carId ?: cars.find { it.isDefault }?.id ?: cars.firstOrNull()?.id ?: 1L)
    }
    val currentCar = remember(selectedCarId, cars) { cars.find { it.id == selectedCarId } }

    var selectedPumpName by remember {
        mutableStateOf(existingRefill?.pumpName ?: pumps.firstOrNull()?.name ?: "IGL CNG Station")
    }

    var totalAmountText by remember {
        mutableStateOf(if (existingRefill != null) existingRefill.totalAmount.toString() else "650")
    }
    var quantityText by remember {
        mutableStateOf(if (existingRefill != null) existingRefill.quantityKg.toString() else "8.25")
    }
    var priceText by remember {
        mutableStateOf(if (existingRefill != null) existingRefill.pricePerKg.toString() else "78.80")
    }
    var odometerText by remember {
        val initialOdo = if (existingRefill != null) {
            existingRefill.odometer
        } else {
            val base = if (lastOdometerReading > 0.0) lastOdometerReading else (currentCar?.currentOdometer ?: 0.0)
            if (base > 0) (base + 180.0) else 45280.0
        }
        mutableStateOf(initialOdo.toInt().toString())
    }
    var isFullTank by remember { mutableStateOf(existingRefill?.isFullRefill ?: true) }
    var notesText by remember { mutableStateOf(existingRefill?.notes ?: "") }
    var activeTarget by remember { mutableStateOf(ActiveInputTarget.TOTAL_AMOUNT) }

    var showCarDropdown by remember { mutableStateOf(false) }
    var showPumpDropdown by remember { mutableStateOf(false) }

    // Calculated metrics
    val totalAmount = totalAmountText.toDoubleOrNull() ?: 0.0
    val quantityKg = quantityText.toDoubleOrNull() ?: 0.0
    val pricePerKg = priceText.toDoubleOrNull() ?: 0.0
    val currentOdo = odometerText.toDoubleOrNull() ?: 0.0

    val previousOdo = if (lastOdometerReading > 0.0) lastOdometerReading else (currentCar?.currentOdometer ?: 0.0)
    val distanceDriven = (currentOdo - previousOdo).coerceAtLeast(0.0)

    val calculatedMileage = if (quantityKg > 0.0 && distanceDriven > 0.0) {
        distanceDriven / quantityKg
    } else 0.0

    val runningCostPerKm = if (distanceDriven > 0.0 && totalAmount > 0.0) {
        totalAmount / distanceDriven
    } else if (calculatedMileage > 0.0 && pricePerKg > 0.0) {
        pricePerKg / calculatedMileage
    } else 0.0

    // Keypad Input Handler
    fun handleKeypadInput(key: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        fun updateField(current: String): String {
            return when (key) {
                "CLEAR" -> ""
                "DEL" -> if (current.isNotEmpty()) current.dropLast(1) else ""
                "." -> if (!current.contains(".")) (if (current.isEmpty()) "0." else "$current.") else current
                else -> {
                    if (current == "0" && key != ".") key else current + key
                }
            }
        }

        when (activeTarget) {
            ActiveInputTarget.TOTAL_AMOUNT -> {
                totalAmountText = updateField(totalAmountText)
                val t = totalAmountText.toDoubleOrNull()
                val p = priceText.toDoubleOrNull()
                if (t != null && p != null && p > 0) {
                    quantityText = String.format(Locale.US, "%.2f", t / p)
                }
            }
            ActiveInputTarget.QUANTITY_KG -> {
                quantityText = updateField(quantityText)
                val q = quantityText.toDoubleOrNull()
                val p = priceText.toDoubleOrNull()
                if (q != null && p != null && p > 0) {
                    totalAmountText = String.format(Locale.US, "%.2f", q * p)
                }
            }
            ActiveInputTarget.ODOMETER -> {
                odometerText = updateField(odometerText)
            }
            ActiveInputTarget.CNG_PRICE -> {
                priceText = updateField(priceText)
                val p = priceText.toDoubleOrNull()
                val q = quantityText.toDoubleOrNull()
                if (p != null && q != null && q > 0) {
                    totalAmountText = String.format(Locale.US, "%.2f", q * p)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
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
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (existingRefill != null) "Edit CNG Refill" else "Log CNG Refill",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vehicle and Station Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Car Selector
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCarDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentCar?.name ?: "Select Car",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCarDropdown,
                        onDismissRequest = { showCarDropdown = false }
                    ) {
                        cars.forEach { car ->
                            DropdownMenuItem(
                                text = { Text("${car.name} (${car.regNumber})") },
                                onClick = {
                                    selectedCarId = car.id
                                    showCarDropdown = false
                                }
                            )
                        }
                    }
                }

                // Station Selector
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPumpDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedPumpName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showPumpDropdown,
                        onDismissRequest = { showPumpDropdown = false }
                    ) {
                        pumps.forEach { pump ->
                            DropdownMenuItem(
                                text = { Text(pump.name) },
                                onClick = {
                                    selectedPumpName = pump.name
                                    priceText = String.format(Locale.US, "%.2f", pump.pricePerKg)
                                    val t = totalAmountText.toDoubleOrNull()
                                    if (t != null && pump.pricePerKg > 0) {
                                        quantityText = String.format(Locale.US, "%.2f", t / pump.pricePerKg)
                                    }
                                    showPumpDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Input Field Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Amount ₹
                InputFieldCard(
                    title = "Total Spent",
                    value = if (totalAmountText.isNotEmpty()) "₹$totalAmountText" else "₹0",
                    unit = "",
                    isSelected = activeTarget == ActiveInputTarget.TOTAL_AMOUNT,
                    onClick = { activeTarget = ActiveInputTarget.TOTAL_AMOUNT },
                    modifier = Modifier.weight(1f),
                    testTag = "input_field_total_amount"
                )

                // Quantity kg
                InputFieldCard(
                    title = "Gas Filled",
                    value = if (quantityText.isNotEmpty()) quantityText else "0",
                    unit = "kg",
                    isSelected = activeTarget == ActiveInputTarget.QUANTITY_KG,
                    onClick = { activeTarget = ActiveInputTarget.QUANTITY_KG },
                    modifier = Modifier.weight(1f),
                    testTag = "input_field_quantity_kg"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Odometer Reading
                InputFieldCard(
                    title = "Odometer",
                    value = if (odometerText.isNotEmpty()) odometerText else "0",
                    unit = "km",
                    isSelected = activeTarget == ActiveInputTarget.ODOMETER,
                    onClick = { activeTarget = ActiveInputTarget.ODOMETER },
                    modifier = Modifier.weight(1f),
                    testTag = "input_field_odometer"
                )

                // CNG Rate
                InputFieldCard(
                    title = "CNG Rate",
                    value = if (priceText.isNotEmpty()) "₹$priceText" else "₹0",
                    unit = "/kg",
                    isSelected = activeTarget == ActiveInputTarget.CNG_PRICE,
                    onClick = { activeTarget = ActiveInputTarget.CNG_PRICE },
                    modifier = Modifier.weight(1f),
                    testTag = "input_field_price_per_kg"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Calculation Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = primaryColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Trip Mileage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (calculatedMileage > 0.0) "${String.format(Locale.US, "%.1f", calculatedMileage)} km/kg" else "--",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = primaryColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Running Cost", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (runningCostPerKm > 0.0) "₹${String.format(Locale.US, "%.2f", runningCostPerKm)}/km" else "--",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isFullTank,
                            onCheckedChange = { isFullTank = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Full Tank", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Custom Number Pad for Easy One-Handed Thumb Entry
            CustomOneHandedNumPad(
                onKeyPress = { handleKeypadInput(it) },
                activeTargetLabel = when (activeTarget) {
                    ActiveInputTarget.TOTAL_AMOUNT -> "Entering: Total Spent (₹)"
                    ActiveInputTarget.QUANTITY_KG -> "Entering: Gas Quantity (kg)"
                    ActiveInputTarget.ODOMETER -> "Entering: Odometer (km)"
                    ActiveInputTarget.CNG_PRICE -> "Entering: CNG Price (₹/kg)"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action Button with subtle Haptic feedback
            Button(
                onClick = {
                    // Trigger subtle haptic feedback vibration
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val pumpObj = pumps.find { it.name == selectedPumpName }
                    onSave(
                        existingRefill?.id ?: 0L,
                        selectedCarId,
                        pumpObj?.id,
                        selectedPumpName,
                        currentOdo,
                        quantityKg,
                        pricePerKg,
                        totalAmount,
                        isFullTank,
                        notesText
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_refill_bottom_sheet"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingRefill != null) "Update Refill Record" else "Save Refill & Update Mileage",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InputFieldCard(
    title: String,
    value: String,
    unit: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Large One-Handed Driver Number Pad
 */
@Composable
fun CustomOneHandedNumPad(
    onKeyPress: (String) -> Unit,
    activeTargetLabel: String,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "DEL")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = activeTargetLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        keys.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onKeyPress(key) }
                            .testTag("numpad_key_$key"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (key == "DEL") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            if (key == "DEL") {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
