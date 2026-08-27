package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.ReportSummaryState
import com.example.ui.viewmodel.TimePeriod
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generatePdfReport(context: Context, reportState: ReportSummaryState): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US)
        val currentDate = dateFormat.format(Date())

        // Background / Header Palette
        val tealColor = Color.parseColor("#004D40")
        val emeraldColor = Color.parseColor("#00C853")
        val darkGray = Color.parseColor("#212121")
        val lightBg = Color.parseColor("#F4F6F7")
        val borderGray = Color.parseColor("#E0E0E0")

        // 1. Top Decorative Header Banner
        paint.color = tealColor
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Title Text
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("CNGमित्र", 36f, 45f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Expense & Mileage Summary Report", 36f, 70f, paint)

        // Period Tag in top right
        paint.color = emeraldColor
        paint.textSize = 12f
        paint.isFakeBoldText = true
        val periodText = "Period: ${reportState.selectedPeriod.name}"
        canvas.drawText(periodText, 440f, 45f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Date: $currentDate", 380f, 70f, paint)

        // 2. Main Executive Summary Cards Grid
        var currentY = 130f

        paint.color = darkGray
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText("Key Financial & Mileage Metrics", 36f, currentY, paint)

        currentY += 15f

        // Total Expense Hero Box
        paint.color = tealColor
        val heroRect = RectF(36f, currentY, 559f, currentY + 80f)
        canvas.drawRoundRect(heroRect, 10f, 10f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("TOTAL CNG SPENDING", 54f, currentY + 28f, paint)

        paint.color = Color.parseColor("#00E676")
        paint.textSize = 26f
        paint.isFakeBoldText = true
        canvas.drawText("₹${String.format(Locale.US, "%.2f", reportState.totalExpense)}", 54f, currentY + 62f, paint)

        // Total Distance & Refill Count in Hero Box right side
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Total Refills: ${reportState.totalRefillsCount}", 380f, currentY + 30f, paint)
        canvas.drawText("Total Distance: ${reportState.totalDistanceKm.toInt()} km", 380f, currentY + 48f, paint)
        canvas.drawText("Total Fuel: ${String.format(Locale.US, "%.1f", reportState.totalQuantityKg)} kg", 380f, currentY + 66f, paint)

        currentY += 95f

        // 3 Secondary Metric Boxes (Row)
        val boxWidth = 166f
        val boxHeight = 60f

        // Box 1: Average Price
        paint.color = lightBg
        canvas.drawRoundRect(RectF(36f, currentY, 36f + boxWidth, currentY + boxHeight), 8f, 8f, paint)
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(36f, currentY, 36f + boxWidth, currentY + boxHeight), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("AVERAGE PRICE", 48f, currentY + 22f, paint)
        paint.color = darkGray
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("₹${String.format(Locale.US, "%.2f", reportState.averagePricePerKg)}/kg", 48f, currentY + 46f, paint)

        // Box 2: Average Mileage
        val box2X = 36f + boxWidth + 12f
        paint.color = lightBg
        canvas.drawRoundRect(RectF(box2X, currentY, box2X + boxWidth, currentY + boxHeight), 8f, 8f, paint)
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(box2X, currentY, box2X + boxWidth, currentY + boxHeight), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("VEHICLE MILEAGE", box2X + 12f, currentY + 22f, paint)
        paint.color = tealColor
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("${String.format(Locale.US, "%.1f", reportState.averageMileage)} km/kg", box2X + 12f, currentY + 46f, paint)

        // Box 3: Cost Per KM
        val box3X = box2X + boxWidth + 12f
        paint.color = lightBg
        canvas.drawRoundRect(RectF(box3X, currentY, box3X + boxWidth, currentY + boxHeight), 8f, 8f, paint)
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(box3X, currentY, box3X + boxWidth, currentY + boxHeight), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("COST PER KM", box3X + 12f, currentY + 22f, paint)
        paint.color = emeraldColor
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("₹${String.format(Locale.US, "%.2f", reportState.averageCostPerKm)}/km", box3X + 12f, currentY + 46f, paint)

        currentY += 80f

        // 3. Interval Breakdown Table Header
        paint.color = darkGray
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText("Breakdown History Details", 36f, currentY, paint)

        currentY += 15f

        // Table Header
        paint.color = tealColor
        canvas.drawRect(36f, currentY, 559f, currentY + 28f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Interval / Month", 48f, currentY + 18f, paint)
        canvas.drawText("Spending (₹)", 260f, currentY + 18f, paint)
        canvas.drawText("Mileage (km/kg)", 430f, currentY + 18f, paint)

        currentY += 28f

        // Table Rows
        val expensePoints = reportState.expenseChartPoints
        val mileagePointsMap = reportState.mileageChartPoints.associate { it.label to it.value }

        var rowBgAlt = false
        for (point in expensePoints) {
            if (currentY > 750f) break // Maintain clean page layout

            val mileageVal = mileagePointsMap[point.label] ?: 0f

            paint.color = if (rowBgAlt) Color.parseColor("#FAFAFA") else Color.WHITE
            canvas.drawRect(36f, currentY, 559f, currentY + 24f, paint)

            paint.color = borderGray
            paint.style = Paint.Style.STROKE
            canvas.drawLine(36f, currentY + 24f, 559f, currentY + 24f, paint)
            paint.style = Paint.Style.FILL

            paint.color = darkGray
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText(point.label, 48f, currentY + 16f, paint)
            canvas.drawText("₹${String.format(Locale.US, "%.2f", point.value)}", 260f, currentY + 16f, paint)
            
            val mileageText = if (mileageVal > 0) "${String.format(Locale.US, "%.1f", mileageVal)} km/kg" else "N/A"
            canvas.drawText(mileageText, 430f, currentY + 16f, paint)

            currentY += 24f
            rowBgAlt = !rowBgAlt
        }

        // 4. Printable Footer
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        canvas.drawLine(36f, 800f, 559f, 800f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.GRAY
        paint.textSize = 9f
        canvas.drawText("Generated by CNG Tracker Mobile App • Confidential & Local Records", 36f, 818f, paint)
        canvas.drawText("Page 1 of 1", 500f, 818f, paint)

        pdfDocument.finishPage(page)

        // Write to cache file
        return try {
            val file = File(context.cacheDir, "CNG_Summary_Report.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun sharePdfReport(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CNG Expense & Mileage Summary Report")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is my CNG Tracker expense and mileage summary report."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share PDF Report"))
    }
}
