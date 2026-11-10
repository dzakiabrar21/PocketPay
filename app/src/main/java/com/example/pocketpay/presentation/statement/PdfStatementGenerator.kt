package com.example.pocketpay.presentation.statement

import com.example.pocketpay.R
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
        var pageNumber = 1
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

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

        fun drawWatermark(c: Canvas) {
            c.save()
            c.rotate(-30f, pageWidth / 2f, pageHeight / 2f)
            c.drawText(
                context.getString(R.string.pdf_pocketpay_official_e_statement),
                pageWidth / 2f - 190f,
                pageHeight / 2f,
                watermarkPaint
            )
            c.restore()
        }

        fun drawFooter(c: Canvas, number: Int) {
            val footerY = pageHeight - 36f
            c.drawLine(32f, footerY - 10f, pageWidth - 32f, footerY - 10f, linePaint)
            c.drawText(
                context.getString(R.string.pdf_dokumen_ini_dicetak_secara_komputeri),
                32f,
                footerY,
                subTitlePaint
            )
            c.drawText(context.getString(R.string.pdf_hal, number), pageWidth - 32f - 30f, footerY + 12f, subTitlePaint)
        }

        // 1. Background Watermark
        drawWatermark(canvas)

        // 2. Official Bank Letterhead
        val margin = 32f
        var currentY = 48f

        canvas.drawText(context.getString(R.string.pdf_pocketpay), margin, currentY, primaryPaint)
        currentY += 15f
        canvas.drawText(context.getString(R.string.pdf_rekening_koran_e_statement_resmi_per), margin, currentY, subTitlePaint)
        currentY += 14f

        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        currentY += 18f

        // 3. Account & Period Information
        val totalCredit = transactions.filter { it.type == TxType.CREDIT }.sumOf { it.amount }
        val totalDebit = transactions.filter { it.type == TxType.DEBIT }.sumOf { it.amount }
        val startingBalance = (currentBalance + totalDebit) - totalCredit

        canvas.drawText(context.getString(R.string.pdf_informasi_nasabah_periode), margin, currentY, boldTextPaint)
        currentY += 14f
        canvas.drawText(context.getString(R.string.pdf_no_rekening, accountNumber), margin, currentY, regularTextPaint)
        canvas.drawText(context.getString(R.string.pdf_periode, periodLabel), margin + 230f, currentY, regularTextPaint)
        currentY += 12f
        canvas.drawText(context.getString(R.string.pdf_nama_nasabah_nasabah_utama_pocketpay), margin, currentY, regularTextPaint)
        canvas.drawText(context.getString(R.string.pdf_tanggal_cetak, SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())), margin + 230f, currentY, regularTextPaint)
        currentY += 16f

        // Account Summary Card
        val boxRect = RectF(margin, currentY, pageWidth - margin, currentY + 40f)
        val boxPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, 6f, 6f, boxPaint)

        val boxY = currentY + 24f
        canvas.drawText(context.getString(R.string.pdf_saldo_awal, formatRp(startingBalance)), margin + 10f, boxY, regularTextPaint)
        canvas.drawText(context.getString(R.string.pdf_total_masuk, formatRp(totalCredit)), margin + 130f, boxY, regularTextPaint)
        canvas.drawText(context.getString(R.string.pdf_total_keluar, formatRp(totalDebit)), margin + 255f, boxY, regularTextPaint)
        canvas.drawText(context.getString(R.string.pdf_saldo_akhir, formatRp(currentBalance)), margin + 380f, boxY, boldTextPaint)

        currentY += 52f

        // 4. Transaction Mutation Table
        canvas.drawText(context.getString(R.string.pdf_rincian_mutasi_transaksi_transaksi, transactions.size), margin, currentY, boldTextPaint)
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

        val col1X = margin + 6f
        val col2X = margin + 90f
        val col3X = margin + 235f
        val col4X = margin + 335f
        val col5X = margin + 410f

        fun drawTableHeader(c: Canvas, y: Float) {
            c.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 22f), 4f, 4f, headerBgPaint)
            val ty = y + 15f
            c.drawText(context.getString(R.string.pdf_tanggal), col1X, ty, headerTextPaint)
            c.drawText(context.getString(R.string.pdf_deskripsi_merchant), col2X, ty, headerTextPaint)
            c.drawText(context.getString(R.string.pdf_sumber_dana), col3X, ty, headerTextPaint)
            c.drawText(context.getString(R.string.pdf_tipe), col4X, ty, headerTextPaint)
            c.drawText(context.getString(R.string.pdf_nominal_rp), col5X, ty, headerTextPaint)
        }

        drawTableHeader(canvas, currentY)
        currentY += 34f

        // Table Rows
        val rowBgPaintAlt = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        transactions.forEachIndexed { index, tx ->
            // Start a new page when the next row would collide with the footer.
            if (currentY > pageHeight - 70f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                pageNumber++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                drawWatermark(canvas)
                currentY = 48f
                drawTableHeader(canvas, currentY)
                currentY += 34f
            }
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
        drawFooter(canvas, pageNumber)

        pdfDocument.finishPage(page)

        val outputFile = File(File(context.cacheDir, "statements").apply { mkdirs() }, "e_statement_pocketpay.pdf")
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
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.pdf_e_statement_rekening_koran_pocketpay))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.pdf_lampiran_e_statement_rekening_koran_))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.pdf_bagikan_e_statement_pdf)))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.pdf_gagal_membagikan_pdf, e.localizedMessage), Toast.LENGTH_SHORT).show()
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

            context.startActivity(Intent.createChooser(viewIntent, context.getString(R.string.pdf_buka_e_statement_pdf)))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.pdf_tidak_ada_aplikasi_pembuka_pdf_di), Toast.LENGTH_SHORT).show()
        }
    }
}