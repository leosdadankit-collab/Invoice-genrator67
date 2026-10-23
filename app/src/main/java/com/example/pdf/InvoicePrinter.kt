package com.example.pdf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.BusinessProfile
import com.example.model.InvoiceWithDetails
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object InvoicePrinter {

    /**
     * Opens Android's native system Print Dialog using PrintManager.
     */
    fun printInvoice(
        activity: Activity,
        bundle: InvoiceWithDetails,
        business: BusinessProfile
    ) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(activity, "Printing service is unavailable on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfFile = InvoicePdfGenerator.generateInvoicePdf(activity, bundle, business)
        val jobName = "Invoice_${bundle.invoice.invoiceNumber.replace("/", "_")}"

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("Missing print destination")
                    return
                }

                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    /**
     * Shares the generated invoice PDF file using Android's native share sheet (WhatsApp, Email, etc.).
     */
    fun shareInvoicePdf(
        context: Context,
        bundle: InvoiceWithDetails,
        business: BusinessProfile
    ) {
        try {
            val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, bundle, business)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${bundle.invoice.invoiceNumber} from ${business.tradeName}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Dear ${bundle.invoice.customerName},\n\nPlease find attached Invoice ${bundle.invoice.invoiceNumber} for ₹${String.format("%.2f", bundle.invoice.grandTotal)}.\n\nThank you for your business!\n${business.tradeName}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share invoice: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
