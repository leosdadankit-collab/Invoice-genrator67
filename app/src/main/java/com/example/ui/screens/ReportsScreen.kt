package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdf.CsvExporter
import com.example.ui.components.formatIndianCurrency
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red700
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.BillingViewModel

@Composable
fun ReportsScreen(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("GST Summary", "Customer Dues", "Sales Register")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Navy900,
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> GstSummaryReport(invoices = invoices)
            1 -> CustomerDuesReport(invoices = invoices, onExport = { CsvExporter.exportInvoicesToCsv(context, invoices) })
            2 -> SalesRegisterReport(invoices = invoices, onExport = { CsvExporter.exportInvoicesToCsv(context, invoices) })
        }
    }
}

@Composable
private fun GstSummaryReport(
    invoices: List<com.example.model.Invoice>
) {
    val nonCancelled = invoices.filter { !it.isCancelled }
    val totalTaxable = nonCancelled.sumOf { it.taxableAmount }
    val totalCgst = nonCancelled.sumOf { it.cgstAmount }
    val totalSgst = nonCancelled.sumOf { it.sgstAmount }
    val totalIgst = nonCancelled.sumOf { it.igstAmount }
    val totalCess = nonCancelled.sumOf { it.cessAmount }
    val totalTax = totalCgst + totalSgst + totalIgst + totalCess
    val totalTurnover = nonCancelled.sumOf { it.grandTotal }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GSTR-1 Tax Analysis",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "Aggregated tax liability on supplies for GST returns",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ReportDataRow("Total Taxable Turnover", formatIndianCurrency(totalTaxable), true)
                    ReportDataRow("Central Tax (CGST)", formatIndianCurrency(totalCgst))
                    ReportDataRow("State Tax (SGST)", formatIndianCurrency(totalSgst))
                    ReportDataRow("Integrated Tax (IGST)", formatIndianCurrency(totalIgst))
                    if (totalCess > 0) {
                        ReportDataRow("Compensation Cess", formatIndianCurrency(totalCess))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    ReportDataRow("Total Tax Liability", formatIndianCurrency(totalTax), true)
                    ReportDataRow("Total Invoice Value", formatIndianCurrency(totalTurnover), true)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "B2B vs B2C Breakdown",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val b2bInvoices = nonCancelled.filter { it.customerGstin.isNotBlank() }
                    val b2cInvoices = nonCancelled.filter { it.customerGstin.isBlank() }

                    ReportDataRow("B2B Invoices (With GSTIN)", "${b2bInvoices.size} bills | ${formatIndianCurrency(b2bInvoices.sumOf { it.grandTotal })}")
                    ReportDataRow("B2C Consumer Bills", "${b2cInvoices.size} bills | ${formatIndianCurrency(b2cInvoices.sumOf { it.grandTotal })}")
                }
            }
        }
    }
}

@Composable
private fun CustomerDuesReport(
    invoices: List<com.example.model.Invoice>,
    onExport: () -> Unit
) {
    val pendingInvoices = invoices.filter { it.balanceDue > 0 && !it.isCancelled }
    val groupedByCustomer = pendingInvoices.groupBy { it.customerName }
    val totalOutstanding = pendingInvoices.sumOf { it.balanceDue }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Pending Dues", style = MaterialTheme.typography.labelMedium, color = Red700)
                        Text(
                            text = formatIndianCurrency(totalOutstanding),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Red700)
                        )
                    }
                    Button(
                        onClick = onExport,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Red700)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV")
                    }
                }
            }
        }

        if (groupedByCustomer.isEmpty()) {
            item {
                Text(
                    text = "All invoices are fully paid! No outstanding customer dues.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(groupedByCustomer.toList()) { (customerName, invList) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(customerName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                formatIndianCurrency(invList.sumOf { it.balanceDue }),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Red700)
                            )
                        }
                        Text(
                            text = "${invList.size} unpaid invoice(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesRegisterReport(
    invoices: List<com.example.model.Invoice>,
    onExport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sales Register (${invoices.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                )

                Button(
                    onClick = onExport,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV")
                }
            }
        }

        items(invoices) { inv ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(inv.invoiceNumber, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(inv.customerName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Taxable: ${formatIndianCurrency(inv.taxableAmount)} | Tax: ${formatIndianCurrency(inv.cgstAmount + inv.sgstAmount + inv.igstAmount)}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text(formatIndianCurrency(inv.grandTotal), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy900))
                }
            }
        }
    }
}

@Composable
private fun ReportDataRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = if (isBold) Navy900 else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
