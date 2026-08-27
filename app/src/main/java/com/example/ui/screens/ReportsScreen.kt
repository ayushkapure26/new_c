package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.ExpenseChart
import com.example.ui.components.PriceHistoryChart
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BrightMint
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.ReportViewModel
import com.example.ui.viewmodel.TimePeriod
import com.example.util.AppLocalization
import com.example.util.CsvExportUtil
import com.example.util.PdfReportGenerator

@Composable
fun ReportsScreen(
    reportViewModel: ReportViewModel,
    selectedLanguage: String = "English"
) {
    val strings = remember(selectedLanguage) { AppLocalization.getStrings(selectedLanguage) }
    val context = LocalContext.current
    val reportState by reportViewModel.reportState.collectAsState()
    val allRefills by reportViewModel.allRefills.collectAsState()
    val allCars by reportViewModel.allCars.collectAsState()

    var pendingCsvContent by remember { mutableStateOf<String?>(null) }

    // SAF Document Creator for saving CSV directly to user storage
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsvContent != null) {
            val success = CsvExportUtil.writeCsvToUri(context, uri, pendingCsvContent!!)
            if (success) {
                Toast.makeText(context, "CNG Expense CSV backup saved successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to save CSV file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val estimatedPetrolCost = reportState.totalDistanceKm * 6.50
    val moneySavedVsPetrol = (estimatedPetrolCost - reportState.totalExpense).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Expense & Mileage Analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Comprehensive financial tracking & fuel comparison",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Time Period Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = reportState.selectedPeriod == TimePeriod.WEEKLY,
                    onClick = { reportViewModel.setTimePeriod(TimePeriod.WEEKLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Weekly")
                }

                SegmentedButton(
                    selected = reportState.selectedPeriod == TimePeriod.MONTHLY,
                    onClick = { reportViewModel.setTimePeriod(TimePeriod.MONTHLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Monthly")
                }

                SegmentedButton(
                    selected = reportState.selectedPeriod == TimePeriod.YEARLY,
                    onClick = { reportViewModel.setTimePeriod(TimePeriod.YEARLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Yearly")
                }
            }
        }

        // Summary Hero Card (Total CNG Cost + Total KM + Total Quantity + Refill Count)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_hero_card"),
                colors = CardDefaults.cardColors(containerColor = DarkTeal),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TOTAL CNG COST (${reportState.selectedPeriod.name})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "₹${String.format("%.2f", reportState.totalExpense)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00E676)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total CNG", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("${String.format("%.1f", reportState.totalQuantityKg)} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Column {
                            Text("Distance Driven", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("${reportState.totalDistanceKm.toInt()} km", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Column {
                            Text("Refill Count", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("${reportState.totalRefillsCount} Fills", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Comparison with Petrol Costs (Money Saved)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("petrol_comparison_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Petrol vs CNG Cost Savings",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Based on ₹104/L Petrol (16 km/L equivalent)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "SAVINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Petrol Estimated", fontSize = 10.sp, color = Color(0xFFC62828))
                                Text(
                                    text = "₹${estimatedPetrolCost.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Money Saved in Pocket", fontSize = 10.sp, color = DarkTeal)
                                Text(
                                    text = "+ ₹${moneySavedVsPetrol.toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stats Metric Cards Grid (Average Mileage, Average Cost/km, Average Price)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Average Mileage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.1f", reportState.averageMileage)} km/kg", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cost per KM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format("%.2f", reportState.averageCostPerKm)}/km", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DarkTeal)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Average Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format("%.2f", reportState.averagePricePerKg)}/kg", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Export Actions: PDF Report and CSV Backup Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generate & Share PDF Report Button
                Button(
                    onClick = {
                        val pdfFile = PdfReportGenerator.generatePdfReport(context, reportState)
                        if (pdfFile != null) {
                            PdfReportGenerator.sharePdfReport(context, pdfFile)
                        } else {
                            Toast.makeText(context, "Failed to generate PDF summary report", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_pdf_report_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF Summary",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Export to Local CSV File Button
                Button(
                    onClick = {
                        if (allRefills.isEmpty()) {
                            Toast.makeText(context, "No fueling data available to export", Toast.LENGTH_SHORT).show()
                        } else {
                            val csvContent = CsvExportUtil.buildFuelExpenseCsv(
                                refills = allRefills,
                                cars = allCars,
                                reportTitle = "CNG Fueling & Expense Backup (${reportState.selectedPeriod.name})"
                            )
                            pendingCsvContent = csvContent
                            val suggestedFileName = CsvExportUtil.getSuggestedFileName("CNG_Expense_Backup")
                            saveCsvLauncher.launch(suggestedFileName)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_csv_backup_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save CSV Backup",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick Share CSV Intent Button
            OutlinedButton(
                onClick = {
                    if (allRefills.isEmpty()) {
                        Toast.makeText(context, "No fueling records to share", Toast.LENGTH_SHORT).show()
                    } else {
                        val csvContent = CsvExportUtil.buildFuelExpenseCsv(
                            refills = allRefills,
                            cars = allCars,
                            reportTitle = "CNG Expense Tracker Backup"
                        )
                        val fileName = CsvExportUtil.getSuggestedFileName("CNG_Expense_Report")
                        val file = CsvExportUtil.saveCsvToCache(context, csvContent, fileName)
                        if (file != null) {
                            CsvExportUtil.shareCsvFile(context, file)
                        } else {
                            Toast.makeText(context, "Failed to create CSV export", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_csv_file_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkTeal)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DarkTeal
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share Expense CSV File (${allRefills.size} records)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
            }
        }

        // Price History Line Chart
        item {
            PriceHistoryChart(
                title = "Historical CNG Price Fluctuations",
                points = reportState.priceHistoryChartPoints,
                minPrice = reportState.minPriceHistory,
                maxPrice = reportState.maxPriceHistory,
                latestPrice = reportState.latestPriceHistory
            )
        }

        // Expense Canvas Bar Chart (Monthly Spending)
        item {
            ExpenseChart(
                title = "CNG Spending Trend",
                points = reportState.expenseChartPoints,
                unitLabel = "₹",
                barColor = EmeraldGreen
            )
        }

        // Mileage Canvas Bar Chart (Mileage Trends)
        item {
            if (reportState.mileageChartPoints.isNotEmpty()) {
                ExpenseChart(
                    title = "Vehicle Mileage (km/kg) Trends",
                    points = reportState.mileageChartPoints,
                    unitLabel = "",
                    barColor = DarkTeal
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
