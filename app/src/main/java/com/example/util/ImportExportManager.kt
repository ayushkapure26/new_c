package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.Car
import com.example.data.model.Refill
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImportExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Exports both Car vehicles and Refill records to a structured CSV backup file.
     */
    fun exportDatabaseToCsv(
        context: Context,
        uri: Uri,
        cars: List<Car>,
        refills: List<Refill>
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val carsMap = cars.associate { it.id to it.name }

                    // --- SECTION 1: CARS ---
                    writer.append("# CARS\n")
                    writer.append("Car ID,Name,Registration Number,Tank Capacity (kg),Fuel Type,Current Odometer (km),Expected Mileage (km/kg),Is Default,Notes\n")

                    for (car in cars) {
                        val cleanName = car.name.replace(",", ";")
                        val cleanReg = car.regNumber.replace(",", ";")
                        val cleanFuel = car.fuelType.replace(",", ";")
                        val cleanNotes = car.notes.replace(",", ";").replace("\n", " ")

                        writer.append("${car.id},")
                        writer.append("\"$cleanName\",")
                        writer.append("\"$cleanReg\",")
                        writer.append("${car.tankCapacityKg},")
                        writer.append("\"$cleanFuel\",")
                        writer.append("${car.currentOdometer},")
                        writer.append("${car.expectedMileage},")
                        writer.append("${if (car.isDefault) "Yes" else "No"},")
                        writer.append("\"$cleanNotes\"\n")
                    }

                    writer.append("\n")

                    // --- SECTION 2: REFILLS ---
                    writer.append("# REFILLS\n")
                    writer.append("Refill ID,Car ID,Car Name,Pump ID,Pump Name,Date,Time,Odometer (km),CNG Quantity (kg),Price (INR/kg),Total Amount (INR),Full Refill,Distance (km),Mileage (km/kg),Cost per km (INR),Notes\n")

                    for (refill in refills) {
                        val carName = carsMap[refill.carId] ?: "Car #${refill.carId}"
                        val dateStr = dateFormat.format(Date(refill.date))
                        val cleanNotes = refill.notes.replace(",", ";").replace("\n", " ")
                        val cleanPump = refill.pumpName.replace(",", ";")

                        writer.append("${refill.id},")
                        writer.append("${refill.carId},")
                        writer.append("\"$carName\",")
                        writer.append("${refill.pumpId ?: ""},")
                        writer.append("\"$cleanPump\",")
                        writer.append("\"$dateStr\",")
                        writer.append("\"${refill.timeFormatted}\",")
                        writer.append("${refill.odometer},")
                        writer.append("${refill.quantityKg},")
                        writer.append("${refill.pricePerKg},")
                        writer.append("${refill.totalAmount},")
                        writer.append("${if (refill.isFullRefill) "Yes" else "No"},")
                        writer.append("${refill.distanceTravelled},")
                        writer.append("${refill.mileageKmPerKg},")
                        writer.append("${refill.costPerKm},")
                        writer.append("\"$cleanNotes\"\n")
                    }

                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class ImportResult(
        val isSuccess: Boolean,
        val importedCarsCount: Int = 0,
        val importedRefillsCount: Int = 0,
        val errorMessage: String = ""
    )

    data class ParsedImportData(
        val cars: List<Car>,
        val refills: List<Refill>
    )

    /**
     * Imports both Cars and Refills from a CSV backup file.
     * Supports multi-section CSV (# CARS and # REFILLS) as well as legacy single-table refills CSV.
     */
    fun importDatabaseFromCsv(
        context: Context,
        uri: Uri
    ): Pair<ParsedImportData, ImportResult> {
        val importedCars = mutableListOf<Car>()
        val importedRefills = mutableListOf<Refill>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var currentSection = "" // "CARS" or "REFILLS" or "LEGACY"

                    var line: String? = reader.readLine()
                    var lineNum = 0

                    while (line != null) {
                        lineNum++
                        val trimmed = line.trim()

                        if (trimmed.isEmpty()) {
                            line = reader.readLine()
                            continue
                        }

                        // Section detection
                        if (trimmed.startsWith("# CARS", ignoreCase = true)) {
                            currentSection = "CARS"
                            line = reader.readLine()
                            continue
                        } else if (trimmed.startsWith("# REFILLS", ignoreCase = true)) {
                            currentSection = "REFILLS"
                            line = reader.readLine()
                            continue
                        }

                        // Legacy header check
                        if (currentSection.isEmpty() && (trimmed.contains("Odometer") || trimmed.contains("Quantity"))) {
                            currentSection = "LEGACY"
                            line = reader.readLine()
                            continue
                        }

                        // Header rows inside sections
                        if (trimmed.startsWith("Car ID", ignoreCase = true) ||
                            trimmed.startsWith("Refill ID", ignoreCase = true) ||
                            trimmed.startsWith("ID,Date", ignoreCase = true)
                        ) {
                            line = reader.readLine()
                            continue
                        }

                        val tokens = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                            .map { it.trim().removeSurrounding("\"") }

                        when (currentSection) {
                            "CARS" -> {
                                if (tokens.size >= 3) {
                                    try {
                                        val name = tokens.getOrNull(1) ?: "Imported Vehicle"
                                        val reg = tokens.getOrNull(2) ?: "MH 01 IMP 00"
                                        val tankCap = tokens.getOrNull(3)?.toDoubleOrNull() ?: 10.0
                                        val fuelType = tokens.getOrNull(4) ?: "CNG + Petrol"
                                        val odo = tokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                                        val mileage = tokens.getOrNull(6)?.toDoubleOrNull() ?: 22.0
                                        val isDefault = tokens.getOrNull(7)?.equals("Yes", ignoreCase = true) ?: false
                                        val notes = tokens.getOrNull(8) ?: "Imported"

                                        val oldCarId = tokens.getOrNull(0)?.toLongOrNull() ?: 0L

                                        val car = Car(
                                            id = oldCarId,
                                            name = name,
                                            regNumber = reg,
                                            tankCapacityKg = tankCap,
                                            fuelType = fuelType,
                                            currentOdometer = odo,
                                            expectedMileage = mileage,
                                            notes = notes,
                                            isDefault = isDefault
                                        )
                                        importedCars.add(car)
                                    } catch (e: Exception) {
                                        // Skip faulty car row
                                    }
                                }
                            }

                            "REFILLS" -> {
                                if (tokens.size >= 10) {
                                    try {
                                        val carId = tokens.getOrNull(1)?.toLongOrNull() ?: 0L
                                        val pumpId = tokens.getOrNull(3)?.toLongOrNull()
                                        val pumpName = tokens.getOrNull(4) ?: "Imported Station"
                                        val odo = tokens.getOrNull(7)?.toDoubleOrNull() ?: 0.0
                                        val qty = tokens.getOrNull(8)?.toDoubleOrNull() ?: 0.0
                                        val price = tokens.getOrNull(9)?.toDoubleOrNull() ?: 0.0
                                        val total = tokens.getOrNull(10)?.toDoubleOrNull() ?: (qty * price)
                                        val isFull = tokens.getOrNull(11)?.equals("Yes", ignoreCase = true) ?: true
                                        val notes = tokens.getOrNull(15) ?: "Imported record"

                                        val refill = Refill(
                                            carId = carId,
                                            pumpId = pumpId,
                                            pumpName = pumpName,
                                            date = System.currentTimeMillis() - (lineNum * 3600000L),
                                            odometer = odo,
                                            quantityKg = qty,
                                            pricePerKg = price,
                                            totalAmount = total,
                                            isFullRefill = isFull,
                                            notes = notes
                                        )
                                        importedRefills.add(refill)
                                    } catch (e: Exception) {
                                        // Skip faulty refill row
                                    }
                                }
                            }

                            "LEGACY" -> {
                                if (tokens.size >= 8) {
                                    try {
                                        val odo = tokens.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                                        val qty = tokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                                        val price = tokens.getOrNull(6)?.toDoubleOrNull() ?: 0.0
                                        val total = tokens.getOrNull(7)?.toDoubleOrNull() ?: (qty * price)

                                        val refill = Refill(
                                            carId = 0L, // Will be mapped to default car
                                            pumpName = tokens.getOrNull(3) ?: "Imported Station",
                                            date = System.currentTimeMillis() - (lineNum * 3600000L),
                                            odometer = odo,
                                            quantityKg = qty,
                                            pricePerKg = price,
                                            totalAmount = total,
                                            isFullRefill = tokens.getOrNull(8)?.equals("Yes", ignoreCase = true) ?: true,
                                            notes = tokens.getOrNull(12) ?: "Imported record"
                                        )
                                        importedRefills.add(refill)
                                    } catch (e: Exception) {
                                        // Skip faulty legacy row
                                    }
                                }
                            }
                        }

                        line = reader.readLine()
                    }
                }
            }

            if (importedCars.isEmpty() && importedRefills.isEmpty()) {
                return Pair(
                    ParsedImportData(emptyList(), emptyList()),
                    ImportResult(false, 0, 0, "No valid vehicle or refill records found in CSV file.")
                )
            }

            return Pair(
                ParsedImportData(importedCars, importedRefills),
                ImportResult(
                    isSuccess = true,
                    importedCarsCount = importedCars.size,
                    importedRefillsCount = importedRefills.size,
                    errorMessage = ""
                )
            )
        } catch (e: Exception) {
            return Pair(
                ParsedImportData(emptyList(), emptyList()),
                ImportResult(false, 0, 0, "Error reading file: ${e.localizedMessage}")
            )
        }
    }
}

