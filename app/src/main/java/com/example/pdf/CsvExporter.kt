package com.example.pdf

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.Product
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    fun exportInvoicesToCsv(context: Context, invoices: List<Invoice>) {
        try {
            val file = File(context.cacheDir, "Sales_Register_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                writer.append("Invoice No,Date,Customer,GSTIN,Type,Taxable (INR),CGST (INR),SGST (INR),IGST (INR),Cess (INR),RoundOff (INR),Grand Total (INR),Paid (INR),Balance (INR),Status\n")
                for (inv in invoices) {
                    writer.append("\"${inv.invoiceNumber}\",")
                    writer.append("\"${dateFormat.format(Date(inv.invoiceDate))}\",")
                    writer.append("\"${inv.customerName.replace("\"", "\"\"")}\",")
                    writer.append("\"${inv.customerGstin}\",")
                    writer.append("\"${inv.documentType}\",")
                    writer.append("${inv.taxableAmount},")
                    writer.append("${inv.cgstAmount},")
                    writer.append("${inv.sgstAmount},")
                    writer.append("${inv.igstAmount},")
                    writer.append("${inv.cessAmount},")
                    writer.append("${inv.roundOff},")
                    writer.append("${inv.grandTotal},")
                    writer.append("${inv.amountPaid},")
                    writer.append("${inv.balanceDue},")
                    writer.append("\"${inv.paymentStatus}\"\n")
                }
            }

            shareCsvFile(context, file, "Sales Register CSV")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportCustomersToCsv(context: Context, customers: List<Customer>) {
        try {
            val file = File(context.cacheDir, "Customers_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                writer.append("Name,Company,Phone,Email,GSTIN,PAN,State,State Code,Address,PinCode\n")
                for (c in customers) {
                    writer.append("\"${c.name.replace("\"", "\"\"")}\",")
                    writer.append("\"${c.companyName.replace("\"", "\"\"")}\",")
                    writer.append("\"${c.phone}\",")
                    writer.append("\"${c.email}\",")
                    writer.append("\"${c.gstin}\",")
                    writer.append("\"${c.pan}\",")
                    writer.append("\"${c.state}\",")
                    writer.append("\"${c.stateCode}\",")
                    writer.append("\"${c.billingAddress.replace("\"", "\"\"")}\",")
                    writer.append("\"${c.pinCode}\"\n")
                }
            }
            shareCsvFile(context, file, "Customers Directory CSV")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportProductsToCsv(context: Context, products: List<Product>) {
        try {
            val file = File(context.cacheDir, "Products_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                writer.append("Name,SKU,HSN_SAC,Category,Unit,Selling Price,Purchase Price,GST Rate,Stock\n")
                for (p in products) {
                    writer.append("\"${p.name.replace("\"", "\"\"")}\",")
                    writer.append("\"${p.sku}\",")
                    writer.append("\"${p.hsnSacCode}\",")
                    writer.append("\"${p.category}\",")
                    writer.append("\"${p.unit}\",")
                    writer.append("${p.price},")
                    writer.append("${p.purchasePrice},")
                    writer.append("${p.gstRate},")
                    writer.append("${p.stockQuantity}\n")
                }
            }
            shareCsvFile(context, file, "Product Catalog CSV")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCsvFile(context: Context, file: File, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export / Share CSV"))
    }
}
