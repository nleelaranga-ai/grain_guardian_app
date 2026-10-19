package com.vrsec.grainguardian.domain.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generateCertificate(context: Context, inspection: GrainInspectionEntity): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // 1. Header Banner (Forest Green)
        paint.color = Color.rgb(27, 94, 32) // #1B5E20
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("GRAINGUARDIAN", 36f, 38f, paint)

        // Subtitle
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.color = Color.rgb(232, 245, 233)
        canvas.drawText("PADDY STORAGE QUALITY & HEALTH INSPECTION CERTIFICATE", 36f, 58f, paint)

        paint.textSize = 8.5f
        paint.color = Color.rgb(200, 230, 201)
        canvas.drawText("Agronomic Protocol: ICAR / BIS 4333 Storage Quality Standard", 36f, 76f, paint)

        // Status Badge Top Right
        val badgeColor = when (inspection.status.uppercase(Locale.US)) {
            "SAFE", "NORMAL" -> Color.rgb(46, 125, 50)
            "WARNING", "ATTENTION" -> Color.rgb(230, 81, 0)
            else -> Color.rgb(198, 40, 40)
        }
        val badgeBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(420f, 25f, 560f, 70f, 8f, 8f, badgeBgPaint)

        val statusTextPaint = Paint().apply {
            color = badgeColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(inspection.status.uppercase(Locale.US), 490f, 52f, statusTextPaint)

        var curY = 120f

        // Section 1: Lot & Inspection Metadata
        curY = drawSectionHeader(canvas, paint, "1. LOT & INSPECTION METADATA", curY)
        curY += 6f

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT

        drawMetaRow(canvas, paint, "Certificate UUID:", inspection.uuid.take(24) + "...", "Date & Time:", inspection.formattedDate, curY)
        curY += 18f
        drawMetaRow(canvas, paint, "Crop Variety:", "${inspection.cropName} (Paddy)", "Inspection Mode:", inspection.mode, curY)
        curY += 18f
        val sourceLabel = if (inspection.source == "SIMULATED") "Simulated (Sandbox)" else "Validated Sensor Probe"
        drawMetaRow(canvas, paint, "Telemetry Source:", sourceLabel, "Sync State:", inspection.syncState, curY)
        curY += 28f

        // Section 2: Sensor Telemetry & Thermometry
        curY = drawSectionHeader(canvas, paint, "2. SENSOR TELEMETRY & 3-DEPTH THERMOMETRY", curY)
        curY += 10f

        // Draw Table Box
        val tableLeft = 36f
        val tableRight = PAGE_WIDTH - 36f
        val tableTop = curY
        val tableBottom = curY + 68f

        val boxPaint = Paint().apply {
            color = Color.rgb(240, 244, 240)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(tableLeft, tableTop, tableRight, tableBottom, 6f, 6f, boxPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(200, 220, 200)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(tableLeft, tableTop, tableRight, tableBottom, 6f, 6f, borderPaint)

        // Draw Table Cells
        val col1 = 50f
        val col2 = 140f
        val col3 = 230f
        val col4 = 320f
        val col5 = 410f
        val col6 = 500f

        val labelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        val valPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val row1LabelY = tableTop + 22f
        val row1ValY = tableTop + 46f

        canvas.drawText("Moisture", col1, row1LabelY, labelPaint)
        canvas.drawText(String.format(Locale.US, "%.1f%%", inspection.moisture), col1, row1ValY, valPaint)

        canvas.drawText("Top (T1)", col2, row1LabelY, labelPaint)
        canvas.drawText(String.format(Locale.US, "%.1f°C", inspection.t1Top), col2, row1ValY, valPaint)

        canvas.drawText("Mid (T2)", col3, row1LabelY, labelPaint)
        canvas.drawText(String.format(Locale.US, "%.1f°C", inspection.t2Middle), col3, row1ValY, valPaint)

        canvas.drawText("Bottom (T3)", col4, row1LabelY, labelPaint)
        canvas.drawText(String.format(Locale.US, "%.1f°C", inspection.t3Bottom), col4, row1ValY, valPaint)

        val tempGradient = abs(inspection.t3Bottom - inspection.t1Top)
        canvas.drawText("Gradient (TGI)", col5, row1LabelY, labelPaint)
        canvas.drawText(String.format(Locale.US, "%.1f°C", tempGradient), col5, row1ValY, valPaint)

        canvas.drawText("Humidity", col6, row1LabelY, labelPaint)
        canvas.drawText(String.format(Locale.US, "%.0f%% RH", inspection.humidity), col6, row1ValY, valPaint)

        curY = tableBottom + 26f

        // Section 3: Diagnostic Scoring & Risk Evaluation
        curY = drawSectionHeader(canvas, paint, "3. AGRONOMIC DIAGNOSTIC SCORING & RISKS", curY)
        curY += 8f

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT

        val bdrsScore = inspection.riskScore
        val gshiScore = max(0, 100 - bdrsScore)
        drawMetaRow(canvas, paint, "Biological Risk (BDRS):", "$bdrsScore / 100", "Storage Health (GSHI):", "$gshiScore / 100", curY)
        curY += 18f

        val hotspotText = if (tempGradient >= 3.0f || inspection.t3Bottom >= 35.0f) "DETECTED (High Thermal Gradient)" else "None (Uniform Core)"
        drawMetaRow(canvas, paint, "Hotspot Status:", hotspotText, "Dominant Risk:", inspection.dominantRisk, curY)
        curY += 28f

        // Section 4: Actionable Advisory & Recommendations
        curY = drawSectionHeader(canvas, paint, "4. ACTIONABLE AGRONOMIC RECOMMENDATIONS", curY)
        curY += 10f

        val recList = inspection.recommendationsJson.split("|").filter { it.isNotBlank() }
        if (recList.isEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("No special corrective interventions required. Maintain current storage conditions.", 46f, curY, paint)
            curY += 16f
        } else {
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            paint.color = Color.rgb(33, 33, 33)
            for (rec in recList) {
                // Bullet icon
                paint.color = Color.rgb(46, 125, 50)
                canvas.drawCircle(46f, curY - 3.5f, 2.5f, paint)

                paint.color = Color.rgb(33, 33, 33)
                canvas.drawText(rec.trim(), 56f, curY, paint)
                curY += 16f
            }
        }

        curY += 16f

        // Section 5: Standards Compliance Note
        curY = drawSectionHeader(canvas, paint, "5. REGULATORY STANDARDS & PROTOCOL", curY)
        curY += 10f

        val notePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("• Maximum safe paddy moisture threshold: 13.5% (IS 4333 compliant).", 46f, curY, notePaint)
        curY += 13f
        canvas.drawText("• Temperature gradient index >= 3.0°C denotes localized biological respiration or insect activity.", 46f, curY, notePaint)
        curY += 13f
        canvas.drawText("• BDRS (Biological Degradation Risk Score) synthesizes moisture, thermometry, and ambient humidity.", 46f, curY, notePaint)
        curY += 28f

        // Footer Verification Box
        val footerY = PAGE_HEIGHT - 65f
        paint.color = Color.rgb(230, 235, 230)
        canvas.drawLine(36f, footerY, PAGE_WIDTH - 36f, footerY, paint)

        val footerTextPaint = Paint().apply {
            color = Color.rgb(120, 135, 120)
            textSize = 8f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }
        val genDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        canvas.drawText("GrainGuardian Smart Post-Harvest Intelligence System — Offline Verifiable Report", 36f, footerY + 18f, footerTextPaint)
        canvas.drawText("Generated on: $genDate | Verification Hash: ${inspection.uuid.hashCode().toString(16)}", 36f, footerY + 30f, footerTextPaint)

        footerTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page 1 of 1", PAGE_WIDTH - 36f, footerY + 18f, footerTextPaint)

        document.finishPage(page)

        // Save to cacheDir / exports
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, "grainguardian_certificate_${inspection.id}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return pdfFile
    }

    private fun drawSectionHeader(canvas: Canvas, paint: Paint, title: String, y: Float): Float {
        paint.color = Color.rgb(27, 94, 32)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        canvas.drawText(title, 36f, y, paint)

        val linePaint = Paint().apply {
            color = Color.rgb(210, 225, 210)
            strokeWidth = 1f
        }
        canvas.drawLine(36f, y + 4f, PAGE_WIDTH - 36f, y + 4f, linePaint)
        return y + 12f
    }

    private fun drawMetaRow(
        canvas: Canvas,
        paint: Paint,
        label1: String,
        val1: String,
        label2: String,
        val2: String,
        y: Float
    ) {
        val labelColor = Color.rgb(100, 116, 139)
        val valColor = Color.rgb(15, 23, 42)

        paint.color = labelColor
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(label1, 46f, y, paint)

        paint.color = valColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(val1, 150f, y, paint)

        paint.color = labelColor
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(label2, 310f, y, paint)

        paint.color = valColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(val2, 400f, y, paint)
    }
}
