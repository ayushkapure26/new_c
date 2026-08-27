package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Car
import com.example.data.model.FuelRefill
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting monthly expense tracker and fueling history data
 * to local CSV files, enabling users to create full backups of their vehicle fuel history.
 */
object CsvExportUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Converts a list of FuelRefill entries and associated Car metadata into standard CSV formatted string.
     */
    fun buildFuelExpenseCsv(
        refills: List<FuelRefill>,
        cars: List<Car>,
        reportTitle: String = "CNG Monthly Expense & Fueling Backup"
    ): String {
        val carMap = cars.associateBy { it.id }
        val sb = StringBuilder()

        // Metadata header
        val generatedDate = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US).format(Date())
        sb.append("# ${escapeCsv(reportTitle)}\n")
        sb.append("# Generated on: $generatedDate\n")
        sb.append("# Total Records: ${refills.size}\n\n")

        // CSV Column Headers
        sb.append(
            listOf(
                "Refill ID",
                "Date",
                "Time",
                "Vehicle Name",
                "Vehicle Reg No",
                "CNG Station Name",
                "Odometer (km)",
                "Quantity (kg)",
                "Price per kg (INR)",
                "Total Amount (INR)",
                "Full Tank Refill",
                "Distance Travelled (km)",
                "Mileage (km/kg)",
                "Cost per km (INR)",
                "Notes"
            ).joinToString(",") { escapeCsv(it) }
        ).append("\n")

        var totalSpend = 0.0
        var totalQuantity = 0.0
        var totalDistance = 0.0

        // Data Rows
        for (refill in refills) {
            val car = carMap[refill.carId]
            val carName = car?.name ?: "Vehicle #${refill.carId}"
            val carReg = car?.regNumber ?: "-"
            val dateStr = dateFormat.format(Date(refill.date))
            val timeStr = if (refill.timeFormatted.isNotEmpty()) refill.timeFormatted else timeFormat.format(Date(refill.date))

            totalSpend += refill.totalAmount
            totalQuantity += refill.quantityKg
            totalDistance += refill.distanceTravelled

            val row = listOf(
                refill.id.toString(),
                dateStr,
                timeStr,
                carName,
                carReg,
                refill.pumpName.ifEmpty { "CNG Station" },
                String.format(Locale.US, "%.1f", refill.odometer),
                String.format(Locale.US, "%.2f", refill.quantityKg),
                String.format(Locale.US, "%.2f", refill.pricePerKg),
                String.format(Locale.US, "%.2f", refill.totalAmount),
                if (refill.isFullRefill) "Yes" else "No",
                String.format(Locale.US, "%.1f", refill.distanceTravelled),
                String.format(Locale.US, "%.2f", refill.mileageKmPerKg),
                String.format(Locale.US, "%.2f", refill.costPerKm),
                refill.notes
            )

            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        // Summary footer
        sb.append("\n# SUMMARY\n")
        sb.append("Total Expense (INR),${String.format(Locale.US, "%.2f", totalSpend)}\n")
        sb.append("Total CNG Fuel (kg),${String.format(Locale.US, "%.2f", totalQuantity)}\n")
        sb.append("Total Distance (km),${String.format(Locale.US, "%.1f", totalDistance)}\n")
        if (totalQuantity > 0 && totalDistance > 0) {
            val avgMileage = totalDistance / totalQuantity
            sb.append("Overall Average Mileage (km/kg),${String.format(Locale.US, "%.2f", avgMileage)}\n")
        }
        val estimatedPetrolCost = totalDistance * 8.5
        val estimatedPetrolSavings = maxOf(0.0, estimatedPetrolCost - totalSpend)
        sb.append("Estimated Savings vs Petrol (INR),${String.format(Locale.US, "%.2f", estimatedPetrolSavings)}\n")

        return sb.toString()
    }

    /**
     * Generates a suggested filename for export.
     */
    fun getSuggestedFileName(prefix: String = "CNG_Expense_Backup"): String {
        val timestamp = fileTimestampFormat.format(Date())
        return "${prefix}_$timestamp.csv"
    }

    /**
     * Saves CSV content to the application's local cache directory.
     */
    fun saveCsvToCache(context: Context, csvContent: String, fileName: String): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                    writer.flush()
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Writes CSV string directly into a user-selected URI from Storage Access Framework (SAF).
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Launches Android Share Intent with the exported CSV file via FileProvider.
     */
    fun shareCsvFile(context: Context, file: File, chooserTitle: String = "Share CNG Expense Backup") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "CNG Expense & Fueling Backup")
                putExtra(Intent.EXTRA_TEXT, "Here is the backup of CNG fueling history and monthly expenses.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}
