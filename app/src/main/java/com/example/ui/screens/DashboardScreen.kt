package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Invoice
import com.example.pdf.CsvExporter
import com.example.pdf.InvoicePrinter
import com.example.ui.components.InvoiceStatusBadge
import com.example.ui.components.StatsCard
import com.example.ui.components.formatIndianCurrency
import com.example.ui.theme.Amber700
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red700
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: BillingViewModel,
    onCreateInvoiceClick: () -> Unit,
    onViewInvoiceClick: (Long) -> Unit,
    onViewAllInvoicesClick: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onAddProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val allInvoices by viewModel.invoices.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Business Header Banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_business_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = businessProfile?.tradeName ?: "Bharat Enterprise",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GSTIN: ${businessProfile?.gstin ?: "27AABCB1234D1Z8"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFC3D4E6)
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCreateInvoiceClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00838F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_invoice_banner_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Invoice", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                CsvExporter.exportInvoicesToCsv(context, allInvoices)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }
                    }
                }
            }
        }

        // 2. Financial Metrics Grid
        item {
            Text(
                text = "Financial Overview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Total Sales",
                    value = formatIndianCurrency(stats.totalSales),
                    subtitle = "${stats.totalInvoices} Invoices",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = Navy900,
                    modifier = Modifier.weight(1f)
                )

                StatsCard(
                    title = "Received",
                    value = formatIndianCurrency(stats.totalPaid),
                    subtitle = "Collected",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Green700,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Outstanding",
                    value = formatIndianCurrency(stats.totalUnpaid),
                    subtitle = "Pending Due",
                    icon = Icons.Default.PendingActions,
                    accentColor = Red700,
                    modifier = Modifier.weight(1f)
                )

                StatsCard(
                    title = "Total Bills",
                    value = "${stats.totalInvoices}",
                    subtitle = "Generated",
                    icon = Icons.Default.Description,
                    accentColor = Teal700,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Quick Actions Row
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddCustomerClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Customer", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onAddProductClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Product", fontSize = 12.sp)
                }
            }
        }

        // 4. Recent Invoices
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Invoices",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .clickable { onViewAllInvoicesClick() }
                        .padding(4.dp)
                )
            }
        }

        if (stats.recentInvoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No invoices generated yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onCreateInvoiceClick) {
                            Text("Create Your First Invoice")
                        }
                    }
                }
            }
        } else {
            items(stats.recentInvoices) { inv ->
                RecentInvoiceCard(
                    invoice = inv,
                    dateFormat = dateFormat,
                    onViewClick = { onViewInvoiceClick(inv.id) },
                    onPrintClick = {
                        val activity = context as? Activity
                        val biz = businessProfile
                        if (activity != null && biz != null) {
                            scope.launch {
                                val bundle = viewModel.getInvoiceWithDetails(inv.id)
                                if (bundle != null) {
                                    InvoicePrinter.printInvoice(activity, bundle, biz)
                                }
                            }
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecentInvoiceCard(
    invoice: Invoice,
    dateFormat: SimpleDateFormat,
    onViewClick: () -> Unit,
    onPrintClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    InvoiceStatusBadge(status = invoice.paymentStatus)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = invoice.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dateFormat.format(Date(invoice.invoiceDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatIndianCurrency(invoice.grandTotal),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (invoice.balanceDue > 0) {
                    Text(
                        text = "Bal: ${formatIndianCurrency(invoice.balanceDue)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Red700
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                IconButton(
                    onClick = onPrintClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print Invoice",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
