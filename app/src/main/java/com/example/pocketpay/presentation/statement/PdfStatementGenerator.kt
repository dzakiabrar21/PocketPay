package com.example.pocketpay.presentation.statement

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStatementGenerator {

    private fun formatRp(amount: Long): String {
        val localeId = Locale.Builder().setLanguage("in").setRegion("ID").build()
        val formatter = NumberFormat.getCurrencyInstance(localeId)
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Generates an official PDF e-statement with bank letterhead, watermark, account summary, and transaction table.
     */
    fun generateEStatementPdf(
        context: Context,
        accountNumber: String,
        periodLabel: String,
        transactions: List<TransactionEntity>,
        currentBalance: Long
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 Width in points
        val pageHeight = 842 // A4 Height in points

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9.5f
            isAntiAlias = true
        }

        val regularTextPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 8.5f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // 1. Background Watermark
        canvas.save()
        canvas.rotate(-30f, pageWidth / 2f, pageHeight / 2f)
        canvas.drawText(
            "POCKETPAY OFFICIAL E-STATEMENT",
            pageWidth / 2f - 190f,
            pageHeight / 2f,
            watermarkPaint
        )
        canvas.restore()

        // 2. Official Bank Letterhead
        val margin = 32f
        var currentY = 48f

        canvas.drawText("POCKETPAY", margin, currentY, primaryPaint)
        currentY += 15f
        canvas.drawText("REKENING KORAN / E-STATEMENT RESMI PERBANKAN DIGITAL", margin, currentY, subTitlePaint)
        currentY += 14f

        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        currentY += 18f

        // 3. Account & Period Information
        val totalCredit = transactions.filter { it.type == TxType.CREDIT }.sumOf { it.amount }
        val totalDebit = transactions.filter { it.type == TxType.DEBIT }.sumOf { it.amount }
        val startingBalance = (currentBalance + totalDebit) - totalCredit

        canvas.drawText("INFORMASI NASABAH & PERIODE", margin, currentY, boldTextPaint)
        currentY += 14f
        canvas.drawText("No. Rekening : $accountNumber", margin, currentY, regularTextPaint)
        canvas.drawText("Periode : $periodLabel", margin + 230f, currentY, regularTextPaint)
        currentY += 12f
        canvas.drawText("Nama Nasabah : Nasabah Utama PocketPay", margin, currentY, regularTextPaint)
        canvas.drawText("Tanggal Cetak : ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}", margin + 230f, currentY, regularTextPaint)
        currentY += 16f

        // Account Summary Card
        val boxRect = RectF(margin, currentY, pageWidth - margin, currentY + 40f)
        val boxPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, 6f, 6f, boxPaint)

        val boxY = currentY + 24f
        canvas.drawText("Saldo Awal: ${formatRp(startingBalance)}", margin + 10f, boxY, regularTextPaint)
        canvas.drawText("Total Masuk (+): ${formatRp(totalCredit)}", margin + 130f, boxY, regularTextPaint)
        canvas.drawText("Total Keluar (-): ${formatRp(totalDebit)}", margin + 255f, boxY, regularTextPaint)
        canvas.drawText("Saldo Akhir: ${formatRp(currentBalance)}", margin + 380f, boxY, boldTextPaint)

        currentY += 52f

        // 4. Transaction Mutation Table
        canvas.drawText("RINCIAN MUTASI TRANSAKSI (${transactions.size} Transaksi)", margin, currentY, boldTextPaint)
        currentY += 12f

        // Table Header
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            isAntiAlias = true
        }

        val headerRect = RectF(margin, currentY, pageWidth - margin, currentY + 22f)
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBgPaint)

        val col1X = margin + 6f
        val col2X = margin + 90f
        val col3X = margin + 235f
        val col4X = margin + 335f
        val col5X = margin + 410f

        val headerTextY = currentY + 15f
        canvas.drawText("TANGGAL", col1X, headerTextY, headerTextPaint)
        canvas.drawText("DESKRIPSI / MERCHANT", col2X, headerTextY, headerTextPaint)
        canvas.drawText("SUMBER DANA", col3X, headerTextY, headerTextPaint)
        canvas.drawText("TIPE", col4X, headerTextY, headerTextPaint)
        canvas.drawText("NOMINAL (RP)", col5X, headerTextY, headerTextPaint)

        currentY += 34f

        // Table Rows
        val rowBgPaintAlt = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        transactions.take(24).forEachIndexed { index, tx ->
            if (index % 2 == 1) {
                val rowRect = RectF(margin, currentY - 13f, pageWidth - margin, currentY + 6f)
                canvas.drawRect(rowRect, rowBgPaintAlt)
            }

            canvas.drawText(formatDate(tx.timestamp), col1X, currentY, regularTextPaint)

            val titleTruncated = if (tx.title.length > 22) tx.title.take(20) + "..." else tx.title
            canvas.drawText(titleTruncated, col2X, currentY, regularTextPaint)

            val sourceTruncated = if (tx.sourceOfFund.length > 14) tx.sourceOfFund.take(12) + "..." else tx.sourceOfFund
            canvas.drawText(sourceTruncated, col3X, currentY, regularTextPaint)

            canvas.drawText(tx.type.name, col4X, currentY, regularTextPaint)

            val amountPrefix = when (tx.type) {
                TxType.DEBIT -> "- "
                TxType.CREDIT -> "+ "
                TxType.REALLOCATION -> ""
            }
            val amountPaint = Paint(regularTextPaint).apply {
                color = when (tx.type) {
                    TxType.DEBIT -> Color.parseColor("#DC2626")
                    TxType.CREDIT -> Color.parseColor("#16A34A")
                    TxType.REALLOCATION -> Color.parseColor("#0284C7")
                }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("$amountPrefix${formatRp(tx.amount)}", col5X, currentY, amountPaint)

            currentY += 22f
        }

        // 5. Official Footer
        val footerY = pageHeight - 36f
        canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)
        canvas.drawText(
            "Dokumen ini dicetak secara komputerisasi oleh sistem PocketPay dan sah tanpa tanda tangan basah.",
            margin,
            footerY,
            subTitlePaint
        )

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "e_statement_pocketpay.pdf")
        try {
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    fun shareEStatementPdf(context: Context, pdfFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "E-Statement Rekening Koran PocketPay")
                putExtra(Intent.EXTRA_TEXT, "Lampiran E-Statement Rekening Koran Resmi PocketPay.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan E-Statement PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openEStatementPdf(context: Context, pdfFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(viewIntent, "Buka E-Statement PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi pembuka PDF di perangkat ini.", Toast.LENGTH_SHORT).show()
        }
    }
}