package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.model.BusinessProfile
import com.example.model.GstCalculationEngine
import com.example.model.InvoiceTemplateType
import com.example.model.InvoiceWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)

    /**
     * Generates a PDF file for an invoice and writes it to the app cache directory.
     * Returns the generated File.
     */
    fun generateInvoicePdf(
        context: Context,
        bundle: InvoiceWithDetails,
        business: BusinessProfile,
        isThermal: Boolean = (bundle.invoice.templateType == InvoiceTemplateType.THERMAL_RECEIPT.name)
    ): File {
        val invoice = bundle.invoice
        val items = bundle.items
        val payments = bundle.payments

        val fileName = "${invoice.invoiceNumber.replace("/", "_")}.pdf"
        val outputFile = File(context.cacheDir, fileName)

        val pdfDocument = PdfDocument()

        if (isThermal) {
            renderThermalReceipt(pdfDocument, bundle, business)
        } else {
            renderA4Invoice(pdfDocument, bundle, business)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun renderA4Invoice(
        document: PdfDocument,
        bundle: InvoiceWithDetails,
        business: BusinessProfile
    ) {
        val invoice = bundle.invoice
        val items = bundle.items

        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val navy = Color.rgb(15, 41, 66)
        val darkText = Color.rgb(30, 41, 59)
        val lightGray = Color.rgb(241, 245, 249)
        val borderColor = Color.rgb(203, 213, 225)
        val accentTeal = Color.rgb(0, 104, 116)

        var y = 35f

        // 1. Top Decorative Brand Bar
        paint.color = navy
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, paint)

        // 2. Business Header
        paint.color = navy
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(business.legalName, 36f, y, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = darkText
        y += 14f
        canvas.drawText("${business.address}, ${business.city}, ${business.state} - ${business.pinCode}", 36f, y, paint)
        y += 12f
        canvas.drawText("Phone: ${business.phone}  |  Email: ${business.email}", 36f, y, paint)
        y += 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GSTIN: ${business.gstin}    PAN: ${business.pan}", 36f, y, paint)

        // Document Title badge on top-right
        paint.color = lightGray
        val docTypeTitle = invoice.documentType.replace("_", " ")
        val titleWidth = paint.measureText(docTypeTitle) + 30f
        val badgeRect = RectF(pageWidth - 36f - titleWidth, 22f, pageWidth - 36f, 48f)
        canvas.drawRoundRect(badgeRect, 4f, 4f, paint)

        paint.color = navy
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(docTypeTitle, badgeRect.left + 15f, 38f, paint)

        // Divider
        y += 16f
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(36f, y, pageWidth - 36f, y, paint)

        // 3. Invoice & Customer Meta Section
        y += 12f
        val col2X = pageWidth / 2f + 10f

        // Left box: Bill To
        paint.color = navy
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO / CUSTOMER DETAILS", 36f, y, paint)
        paint.color = darkText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        y += 13f
        canvas.drawText(invoice.customerName, 36f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (invoice.customerGstin.isNotBlank()) {
            y += 11f
            canvas.drawText("GSTIN: ${invoice.customerGstin}", 36f, y, paint)
        }
        if (invoice.customerAddress.isNotBlank()) {
            y += 11f
            canvas.drawText(invoice.customerAddress, 36f, y, paint)
        }
        y += 11f
        canvas.drawText("State: ${invoice.customerState} (Code: ${invoice.customerStateCode})", 36f, y, paint)

        // Right box: Invoice Details
        var ry = y - 46f
        paint.color = navy
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INVOICE DETAILS", col2X, ry, paint)
        paint.color = darkText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        ry += 13f
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", col2X, ry, paint)
        ry += 11f
        canvas.drawText("Invoice Date: ${dateFormat.format(Date(invoice.invoiceDate))}", col2X, ry, paint)
        ry += 11f
        canvas.drawText("Due Date: ${dateFormat.format(Date(invoice.dueDate))}", col2X, ry, paint)
        ry += 11f
        canvas.drawText("Place of Supply: ${invoice.placeOfSupply} (${invoice.placeOfSupplyStateCode})", col2X, ry, paint)
        ry += 11f
        val taxMode = if (invoice.isInterState) "Inter-State (IGST)" else "Intra-State (CGST + SGST)"
        canvas.drawText("Tax Supply: $taxMode", col2X, ry, paint)

        y = maxOf(y, ry) + 16f

        // 4. Items Table Header
        val tableTop = y
        val tableBottom = y + 20f
        paint.color = navy
        canvas.drawRect(36f, tableTop, pageWidth - 36f, tableBottom, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val cSno = 42f
        val cDesc = 68f
        val cHsn = 230f
        val cQty = 280f
        val cRate = 320f
        val cDisc = 365f
        val cTaxVal = 410f
        val cGst = 465f
        val cTotal = 515f

        canvas.drawText("S.N", cSno, y + 13f, paint)
        canvas.drawText("Description", cDesc, y + 13f, paint)
        canvas.drawText("HSN", cHsn, y + 13f, paint)
        canvas.drawText("Qty", cQty, y + 13f, paint)
        canvas.drawText("Rate(₹)", cRate, y + 13f, paint)
        canvas.drawText("Disc(₹)", cDisc, y + 13f, paint)
        canvas.drawText("Taxable", cTaxVal, y + 13f, paint)
        canvas.drawText("GST%", cGst, y + 13f, paint)
        canvas.drawText("Total(₹)", cTotal, y + 13f, paint)

        y = tableBottom

        // 5. Item Rows
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        items.forEachIndexed { index, item ->
            val rowHeight = 20f
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(36f, y, pageWidth - 36f, y + rowHeight, paint)
            }

            paint.color = darkText
            val itemY = y + 13f
            canvas.drawText("${index + 1}", cSno, itemY, paint)

            val truncatedName = if (item.productName.length > 30) item.productName.take(28) + ".." else item.productName
            canvas.drawText(truncatedName, cDesc, itemY, paint)
            canvas.drawText(if (item.hsnSacCode.isNotBlank()) item.hsnSacCode else "-", cHsn, itemY, paint)
            canvas.drawText("${item.quantity} ${item.unit}", cQty, itemY, paint)
            canvas.drawText(String.format(Locale.ENGLISH, "%.2f", item.unitPrice), cRate, itemY, paint)
            canvas.drawText(String.format(Locale.ENGLISH, "%.2f", item.discount), cDisc, itemY, paint)
            canvas.drawText(String.format(Locale.ENGLISH, "%.2f", item.taxableAmount), cTaxVal, itemY, paint)
            canvas.drawText("${item.gstRate}%", cGst, itemY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(String.format(Locale.ENGLISH, "%.2f", item.totalAmount), cTotal, itemY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            y += rowHeight
        }

        // Table Bottom Border
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(36f, y, pageWidth - 36f, y, paint)

        // 6. Tax Breakdown & Totals Summary
        y += 14f
        val summaryX = pageWidth - 200f

        // Left column: Bank Details & Amount in Words
        paint.color = navy
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BANK & PAYMENT DETAILS", 36f, y, paint)

        paint.color = darkText
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var by = y + 11f
        canvas.drawText("Bank: ${business.bankName}", 36f, by, paint)
        by += 10f
        canvas.drawText("A/C No: ${business.accountNumber}  |  IFSC: ${business.ifscCode}", 36f, by, paint)
        by += 10f
        canvas.drawText("UPI ID: ${business.upiId}", 36f, by, paint)

        by += 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total in Words:", 36f, by, paint)
        by += 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val words = GstCalculationEngine.convertToIndianWords(invoice.grandTotal.toLong())
        canvas.drawText(words, 36f, by, paint)

        // Right column: Financial Summary Box
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var sy = y
        fun drawSummaryRow(label: String, value: Double, isBold: Boolean = false) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            paint.color = if (isBold) navy else darkText
            canvas.drawText(label, summaryX, sy, paint)
            val valStr = String.format(Locale.ENGLISH, "₹ %.2f", value)
            val valW = paint.measureText(valStr)
            canvas.drawText(valStr, pageWidth - 36f - valW, sy, paint)
            sy += 12f
        }

        drawSummaryRow("Taxable Amount:", invoice.taxableAmount)
        if (invoice.cgstAmount > 0) {
            drawSummaryRow("CGST:", invoice.cgstAmount)
            drawSummaryRow("SGST:", invoice.sgstAmount)
        }
        if (invoice.igstAmount > 0) {
            drawSummaryRow("IGST:", invoice.igstAmount)
        }
        if (invoice.cessAmount > 0) {
            drawSummaryRow("Cess:", invoice.cessAmount)
        }
        if (invoice.additionalCharges > 0) {
            drawSummaryRow("Shipping / Charges:", invoice.additionalCharges)
        }
        if (invoice.roundOff != 0.0) {
            drawSummaryRow("Round Off:", invoice.roundOff)
        }

        sy += 2f
        paint.color = navy
        canvas.drawRect(summaryX - 5f, sy - 2f, pageWidth - 36f, sy + 18f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GRAND TOTAL:", summaryX + 4f, sy + 12f, paint)
        val grandStr = String.format(Locale.ENGLISH, "₹ %.2f", invoice.grandTotal)
        val grandW = paint.measureText(grandStr)
        canvas.drawText(grandStr, pageWidth - 40f - grandW, sy + 12f, paint)

        sy += 26f
        paint.color = darkText
        paint.textSize = 8.5f
        val paidStr = String.format(Locale.ENGLISH, "Paid: ₹ %.2f   |   Balance: ₹ %.2f", invoice.amountPaid, invoice.balanceDue)
        canvas.drawText(paidStr, summaryX, sy, paint)

        // 7. Terms & Authorized Signatory Footer
        y = maxOf(by + 20f, sy + 25f)
        paint.color = borderColor
        canvas.drawLine(36f, y, pageWidth - 36f, y, paint)

        y += 12f
        paint.color = navy
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Terms & Conditions:", 36f, y, paint)

        paint.color = darkText
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var ty = y + 9f
        val termsList = (if (invoice.terms.isNotBlank()) invoice.terms else business.defaultTerms).lines()
        termsList.take(3).forEach { line ->
            canvas.drawText(line, 36f, ty, paint)
            ty += 9f
        }

        // Signature on right
        val sigX = pageWidth - 160f
        paint.color = darkText
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("For ${business.tradeName}", sigX, y, paint)

        // If user uploaded/drew a signature, draw bitmap if available
        if (!business.signaturePath.isNullOrBlank()) {
            val sigFile = File(business.signaturePath)
            if (sigFile.exists()) {
                val sigBmp = BitmapFactory.decodeFile(sigFile.absolutePath)
                if (sigBmp != null) {
                    val destRect = RectF(sigX, y + 4f, sigX + 100f, y + 36f)
                    canvas.drawBitmap(sigBmp, null, destRect, null)
                }
            }
        }

        canvas.drawText("_________________________", sigX, y + 42f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(business.authorizedSignatory, sigX, y + 52f, paint)

        document.finishPage(page)
    }

    private fun renderThermalReceipt(
        document: PdfDocument,
        bundle: InvoiceWithDetails,
        business: BusinessProfile
    ) {
        val invoice = bundle.invoice
        val items = bundle.items

        val pageWidth = 384 // 80mm POS width in standard points
        val baseHeight = 500 + (items.size * 25)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, baseHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var y = 25f
        canvas.drawText(business.tradeName, pageWidth / 2f, y, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        y += 12f
        canvas.drawText("${business.address}, ${business.city}", pageWidth / 2f, y, paint)
        y += 11f
        canvas.drawText("GSTIN: ${business.gstin} | Ph: ${business.phone}", pageWidth / 2f, y, paint)

        y += 12f
        canvas.drawText("--------------------------------------------------", pageWidth / 2f, y, paint)

        paint.textAlign = Paint.Align.LEFT
        y += 12f
        canvas.drawText("Inv No: ${invoice.invoiceNumber}", 15f, y, paint)
        val dateStr = dateFormat.format(Date(invoice.invoiceDate))
        canvas.drawText("Date: $dateStr", pageWidth - 100f, y, paint)
        y += 11f
        canvas.drawText("Customer: ${invoice.customerName}", 15f, y, paint)

        y += 12f
        canvas.drawText("--------------------------------------------------", 15f, y, paint)

        y += 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Item", 15f, y, paint)
        canvas.drawText("Qty", 180f, y, paint)
        canvas.drawText("Rate", 240f, y, paint)
        canvas.drawText("Total", 320f, y, paint)

        y += 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (item in items) {
            val shortName = if (item.productName.length > 20) item.productName.take(18) + ".." else item.productName
            canvas.drawText(shortName, 15f, y, paint)
            canvas.drawText("${item.quantity}", 180f, y, paint)
            canvas.drawText(String.format(Locale.ENGLISH, "%.1f", item.unitPrice), 240f, y, paint)
            canvas.drawText(String.format(Locale.ENGLISH, "%.1f", item.totalAmount), 320f, y, paint)
            y += 14f
        }

        y += 5f
        canvas.drawText("--------------------------------------------------", 15f, y, paint)
        y += 13f
        canvas.drawText("Taxable Value: ₹ ${String.format(Locale.ENGLISH, "%.2f", invoice.taxableAmount)}", 15f, y, paint)
        y += 11f
        canvas.drawText("GST Total: ₹ ${String.format(Locale.ENGLISH, "%.2f", invoice.cgstAmount + invoice.sgstAmount + invoice.igstAmount)}", 15f, y, paint)
        y += 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("NET PAYABLE: ₹ ${String.format(Locale.ENGLISH, "%.2f", invoice.grandTotal)}", 15f, y, paint)

        y += 14f
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Paid: ₹ ${String.format(Locale.ENGLISH, "%.2f", invoice.amountPaid)}  |  Bal: ₹ ${String.format(Locale.ENGLISH, "%.2f", invoice.balanceDue)}", 15f, y, paint)

        y += 14f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("UPI: ${business.upiId}", pageWidth / 2f, y, paint)
        y += 12f
        canvas.drawText("Thank you for your visit!", pageWidth / 2f, y, paint)

        document.finishPage(page)
    }
}
