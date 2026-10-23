package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GstCalculationEngine
import com.example.model.InvoiceWithDetails
import com.example.pdf.InvoicePrinter
import com.example.ui.components.InvoiceStatusBadge
import com.example.ui.components.formatIndianCurrency
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red700
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit,
    onEditInvoice: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
    var bundle by remember { mutableStateOf<InvoiceWithDetails?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(invoiceId) {
        bundle = viewModel.getInvoiceWithDetails(invoiceId)
    }

    val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bundle?.invoice?.invoiceNumber ?: "Invoice Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditInvoice(invoiceId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            bundle?.let { b ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val biz = businessProfile
                                if (biz != null) {
                                    InvoicePrinter.shareInvoicePdf(context, b, biz)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF")
                        }

                        Button(
                            onClick = {
                                val activity = context as? Activity
                                val biz = businessProfile
                                if (activity != null && biz != null) {
                                    InvoicePrinter.printInvoice(activity, b, biz)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_invoice_detail_btn")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print Invoice")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (bundle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val invoice = bundle!!.invoice
            val items = bundle!!.items
            val payments = bundle!!.payments

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status & Header Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = invoice.invoiceNumber,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Navy900
                                    )
                                    Text(
                                        text = invoice.documentType.replace("_", " "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Teal700
                                    )
                                }
                                InvoiceStatusBadge(status = invoice.paymentStatus)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Invoice Date", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(dateFormat.format(Date(invoice.invoiceDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Due Date", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(dateFormat.format(Date(invoice.dueDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }

                // Customer Details Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Billed To",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Navy900)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(invoice.customerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (invoice.customerGstin.isNotBlank()) {
                                Text("GSTIN: ${invoice.customerGstin}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (invoice.customerAddress.isNotBlank()) {
                                Text(invoice.customerAddress, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text("State: ${invoice.customerState} (${invoice.customerStateCode})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                // Line Items
                item {
                    Text(
                        text = "Line Items (${items.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                }

                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "HSN: ${item.hsnSacCode} | Qty: ${item.quantity} ${item.unit} @ ₹${item.unitPrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    formatIndianCurrency(item.totalAmount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Taxable: ${formatIndianCurrency(item.taxableAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                val taxTxt = if (invoice.isInterState) "IGST (${item.gstRate}%): ₹${item.igstAmount}"
                                else "CGST: ₹${item.cgstAmount} | SGST: ₹${item.sgstAmount}"
                                Text(taxTxt, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }

                // Financial Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy900)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            Spacer(modifier = Modifier.height(10.dp))

                            SummaryLine("Subtotal", formatIndianCurrency(invoice.subTotal))
                            if (invoice.totalDiscount > 0) {
                                SummaryLine("Discount", "- ${formatIndianCurrency(invoice.totalDiscount)}")
                            }
                            SummaryLine("Taxable Amount", formatIndianCurrency(invoice.taxableAmount))

                            if (invoice.cgstAmount > 0) {
                                SummaryLine("CGST", formatIndianCurrency(invoice.cgstAmount))
                                SummaryLine("SGST", formatIndianCurrency(invoice.sgstAmount))
                            }
                            if (invoice.igstAmount > 0) {
                                SummaryLine("IGST", formatIndianCurrency(invoice.igstAmount))
                            }
                            if (invoice.additionalCharges > 0) {
                                SummaryLine("Shipping / Other Charges", formatIndianCurrency(invoice.additionalCharges))
                            }
                            if (invoice.roundOff != 0.0) {
                                SummaryLine("Round Off", formatIndianCurrency(invoice.roundOff))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Grand Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                Text(formatIndianCurrency(invoice.grandTotal), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4DD0E1)))
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Paid: ${formatIndianCurrency(invoice.amountPaid)}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC3D4E6)))
                                Text("Balance Due: ${formatIndianCurrency(invoice.balanceDue)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5)))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = GstCalculationEngine.convertToIndianWords(invoice.grandTotal.toLong()),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        }
                    }
                }

                // Payment Records Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment History (${payments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )

                        if (invoice.balanceDue > 0 && !invoice.isCancelled) {
                            OutlinedButton(
                                onClick = { showPaymentDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Payment", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (payments.isEmpty()) {
                    item {
                        Text("No payment recorded yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                } else {
                    items(payments) { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${p.paymentMethod} - ${dateFormat.format(Date(p.paymentDate))}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    if (p.transactionRef.isNotBlank()) {
                                        Text("Ref: ${p.transactionRef}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                                Text(
                                    formatIndianCurrency(p.amount),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Green700)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showPaymentDialog && bundle != null) {
        RecordPaymentDialog(
            invoice = bundle!!.invoice,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, method, ref, notes ->
                viewModel.recordPayment(invoiceId, amount, method, ref, notes)
                showPaymentDialog = false
                scope.launch {
                    bundle = viewModel.getInvoiceWithDetails(invoiceId)
                }
            }
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
    }
}
