package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.Invoice
import com.example.model.PaymentMethod
import com.example.pdf.InvoicePrinter
import com.example.ui.components.InvoiceStatusBadge
import com.example.ui.components.SelectableDropdown
import com.example.ui.components.formatIndianCurrency
import com.example.ui.theme.Red700
import com.example.ui.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceListScreen(
    viewModel: BillingViewModel,
    onCreateInvoiceClick: () -> Unit,
    onViewInvoiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val searchQuery by viewModel.invoiceSearchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.invoiceStatusFilter.collectAsStateWithLifecycle()
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()

    var paymentDialogInvoice by remember { mutableStateOf<Invoice?>(null) }
    var deleteConfirmInvoice by remember { mutableStateOf<Invoice?>(null) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    val filterTabs = listOf("ALL", "UNPAID", "PARTIAL", "PAID", "CANCELLED")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateInvoiceClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_create_invoice")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Invoice")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateInvoiceSearchQuery(it) },
                placeholder = { Text("Search by Invoice No or Customer Name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateInvoiceSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("invoice_search_input")
            )

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(filterTabs) { tab ->
                    FilterChip(
                        selected = statusFilter.equals(tab, ignoreCase = true),
                        onClick = { viewModel.updateInvoiceStatusFilter(tab) },
                        label = { Text(tab, fontSize = 12.sp) }
                    )
                }
            }

            // Invoices List
            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No invoices matching '$searchQuery'" else "No invoices found in this filter",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateInvoiceClick) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create New Invoice")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(invoices, key = { it.id }) { invoice ->
                        InvoiceListItemCard(
                            invoice = invoice,
                            dateFormat = dateFormat,
                            onCardClick = { onViewInvoiceClick(invoice.id) },
                            onPrint = {
                                val activity = context as? Activity
                                val biz = businessProfile
                                if (activity != null && biz != null) {
                                    scope.launch {
                                        val bundle = viewModel.getInvoiceWithDetails(invoice.id)
                                        if (bundle != null) {
                                            InvoicePrinter.printInvoice(activity, bundle, biz)
                                        }
                                    }
                                }
                            },
                            onShare = {
                                val biz = businessProfile
                                if (biz != null) {
                                    scope.launch {
                                        val bundle = viewModel.getInvoiceWithDetails(invoice.id)
                                        if (bundle != null) {
                                            InvoicePrinter.shareInvoicePdf(context, bundle, biz)
                                        }
                                    }
                                }
                            },
                            onRecordPayment = { paymentDialogInvoice = invoice },
                            onCancel = { viewModel.cancelInvoice(invoice.id) },
                            onDelete = { deleteConfirmInvoice = invoice }
                        )
                    }
                }
            }
        }
    }

    // Payment Recording Dialog
    paymentDialogInvoice?.let { inv ->
        RecordPaymentDialog(
            invoice = inv,
            onDismiss = { paymentDialogInvoice = null },
            onConfirm = { amount, method, ref, notes ->
                viewModel.recordPayment(inv.id, amount, method, ref, notes)
                paymentDialogInvoice = null
            }
        )
    }

    // Delete Confirmation Dialog
    deleteConfirmInvoice?.let { inv ->
        AlertDialog(
            onDismissRequest = { deleteConfirmInvoice = null },
            title = { Text("Delete Invoice?") },
            text = { Text("Are you sure you want to delete invoice ${inv.invoiceNumber}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInvoice(inv.id)
                        deleteConfirmInvoice = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Red700)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConfirmInvoice = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InvoiceListItemCard(
    invoice: Invoice,
    dateFormat: SimpleDateFormat,
    onCardClick: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onRecordPayment: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    InvoiceStatusBadge(status = invoice.paymentStatus)
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Print Invoice") },
                            leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onPrint()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        if (invoice.balanceDue > 0 && !invoice.isCancelled) {
                            DropdownMenuItem(
                                text = { Text("Record Payment") },
                                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRecordPayment()
                                }
                            )
                        }
                        if (!invoice.isCancelled) {
                            DropdownMenuItem(
                                text = { Text("Cancel / Void") },
                                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCancel()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = Red700) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Red700) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = invoice.customerName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (invoice.customerGstin.isNotBlank()) {
                Text(
                    text = "GSTIN: ${invoice.customerGstin}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Date: ${dateFormat.format(Date(invoice.invoiceDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Due: ${dateFormat.format(Date(invoice.dueDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatIndianCurrency(invoice.grandTotal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (invoice.balanceDue > 0) {
                        Text(
                            text = "Due: ${formatIndianCurrency(invoice.balanceDue)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Red700
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirm: (Double, PaymentMethod, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(invoice.balanceDue.toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var transactionRef by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${invoice.invoiceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Balance Due: ${formatIndianCurrency(invoice.balanceDue)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Red700
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                SelectableDropdown(
                    label = "Payment Method",
                    options = PaymentMethod.values().toList(),
                    selectedOption = selectedMethod,
                    onOptionSelected = { selectedMethod = it },
                    itemLabel = { it.label }
                )

                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("Reference / UPI UTR / Cheque No") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt, selectedMethod, transactionRef, notes)
                    }
                }
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
