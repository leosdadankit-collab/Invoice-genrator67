package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Product
import com.example.model.StandardGstRates
import com.example.model.StandardUnits
import com.example.pdf.CsvExporter
import com.example.ui.components.SelectableDropdown
import com.example.ui.components.formatIndianCurrency
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red700
import com.example.ui.viewmodel.BillingViewModel

@Composable
fun ProductListScreen(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()

    var productDialogItem by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
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
            // Search and Export Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateProductSearchQuery(it) },
                    placeholder = { Text("Search Products, SKU, HSN...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateProductSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { CsvExporter.exportProductsToCsv(context, products) }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Navy900)
                }
            }

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No products matching '$searchQuery'" else "No products or services saved yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Product / Service")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = { productDialogItem = product },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProductEditDialog(
            product = Product(name = ""),
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveProduct(it)
                showAddDialog = false
            }
        )
    }

    productDialogItem?.let { p ->
        ProductEditDialog(
            product = p,
            onDismiss = { productDialogItem = null },
            onSave = {
                viewModel.saveProduct(it)
                productDialogItem = null
            }
        )
    }
}

@Composable
private fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (product.isService) Color(0xFFC3F3FA) else Color(0xFFDCE6F2),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (product.isService) "SERVICE" else "GOODS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "HSN/SAC: ${product.hsnSacCode.ifBlank { "N/A" }} | Unit: ${product.unit} | GST: ${product.gstRate}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (!product.isService) {
                    Text(
                        text = "Stock: ${product.stockQuantity} ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.stockQuantity <= product.lowStockThreshold) Red700 else Color(0xFF15803D)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatIndianCurrency(product.price),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red700, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductEditDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description) }
    var sku by remember { mutableStateOf(product.sku) }
    var hsnSacCode by remember { mutableStateOf(product.hsnSacCode) }
    var unit by remember { mutableStateOf(product.unit) }
    var priceText by remember { mutableStateOf(if (product.price == 0.0) "" else product.price.toString()) }
    var purchasePriceText by remember { mutableStateOf(if (product.purchasePrice == 0.0) "" else product.purchasePrice.toString()) }
    var gstRate by remember { mutableStateOf(product.gstRate) }
    var isTaxInclusive by remember { mutableStateOf(product.isTaxInclusive) }
    var isService by remember { mutableStateOf(product.isService) }
    var stockText by remember { mutableStateOf(product.stockQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product.id == 0L) "Add Product / Service" else "Edit Product") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Item Type: ${if (isService) "Service (SAC)" else "Goods (HSN)"}", fontWeight = FontWeight.Bold)
                        Switch(checked = isService, onCheckedChange = { isService = it })
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product / Service Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hsnSacCode,
                            onValueChange = { hsnSacCode = it },
                            label = { Text(if (isService) "SAC Code" else "HSN Code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        SelectableDropdown(
                            label = "Unit",
                            options = StandardUnits.ALL,
                            selectedOption = unit,
                            onOptionSelected = { unit = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Selling Price (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        SelectableDropdown(
                            label = "GST Rate",
                            options = StandardGstRates.ALL,
                            selectedOption = gstRate,
                            onOptionSelected = { gstRate = it },
                            itemLabel = { "${it}%" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Price is Tax Inclusive", fontSize = 13.sp)
                        Switch(checked = isTaxInclusive, onCheckedChange = { isTaxInclusive = it })
                    }
                }

                if (!isService) {
                    item {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("Stock Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val p = product.copy(
                            name = name,
                            description = description,
                            sku = sku,
                            hsnSacCode = hsnSacCode,
                            unit = unit,
                            price = priceText.toDoubleOrNull() ?: 0.0,
                            purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0,
                            gstRate = gstRate,
                            isTaxInclusive = isTaxInclusive,
                            isService = isService,
                            stockQuantity = stockText.toDoubleOrNull() ?: 100.0
                        )
                        onSave(p)
                    }
                }
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
