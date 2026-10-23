package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Customer
import com.example.model.DocumentType
import com.example.model.GstCalculationEngine
import com.example.model.IndianStates
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.StandardGstRates
import com.example.model.StandardUnits
import com.example.pdf.InvoicePrinter
import com.example.ui.components.SelectableDropdown
import com.example.ui.components.formatIndianCurrency
import com.example.ui.theme.Green700
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red700
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.EditableLineItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditInvoiceScreen(
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit,
    onInvoiceSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()

    var showCustomerPicker by remember { mutableStateOf(false) }
    var productPickerItemIndex by remember { mutableStateOf<Int?>(null) }

    // Live GST calculation result
    val liveCalculation by remember(editorState) {
        derivedStateOf {
            val calcItems = editorState.items.map { item ->
                GstCalculationEngine.calculateLineItem(
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    isDiscountPercent = item.isDiscountPercent,
                    isTaxInclusive = item.isTaxInclusive,
                    gstRate = item.gstRate,
                    cessRate = item.cessRate,
                    isInterState = editorState.isInterState,
                    isUnregisteredSeller = businessProfile?.registrationType == "UNREGISTERED"
                )
            }
            GstCalculationEngine.calculateInvoice(
                items = calcItems,
                additionalCharges = editorState.additionalCharges,
                amountPaid = editorState.amountPaid,
                dueDate = editorState.dueDate
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editorState.invoiceId == 0L) "Create Invoice" else "Edit Invoice",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveInvoice { savedId ->
                                onInvoiceSaved(savedId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_invoice_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
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
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Document & Invoice Basics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Invoice Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SelectableDropdown(
                                label = "Document Type",
                                options = DocumentType.values().toList(),
                                selectedOption = editorState.documentType,
                                onOptionSelected = { viewModel.updateDocumentType(it) },
                                itemLabel = { it.label },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = editorState.invoiceNumber,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Invoice No") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Place of Supply with Indian States
                        val currentState = IndianStates.ALL.find { it.code == editorState.placeOfSupplyStateCode }
                            ?: IndianStates.ALL.first()

                        SelectableDropdown(
                            label = "Place of Supply",
                            options = IndianStates.ALL,
                            selectedOption = currentState,
                            onOptionSelected = { state ->
                                viewModel.updatePlaceOfSupply(state)
                            },
                            itemLabel = { "${it.code} - ${it.name}" },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Supply Tax Mode Banner
                        Surface(
                            color = if (editorState.isInterState) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (editorState.isInterState) Color(0xFFB45309) else Green700,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (editorState.isInterState)
                                        "Inter-State Supply: IGST applies (${editorState.placeOfSupply})"
                                    else
                                        "Intra-State Supply: CGST + SGST applies (${editorState.placeOfSupply})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (editorState.isInterState) Color(0xFFB45309) else Green700
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Customer Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Customer Details",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            )

                            OutlinedButton(
                                onClick = { showCustomerPicker = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick Customer", fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = editorState.customerName,
                            onValueChange = {
                                viewModel.selectCustomer(
                                    (editorState.selectedCustomer ?: Customer(name = it)).copy(name = it)
                                )
                            },
                            label = { Text("Customer Name / Business Name *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = editorState.customerPhone,
                                onValueChange = {
                                    viewModel.selectCustomer(
                                        (editorState.selectedCustomer ?: Customer(name = editorState.customerName)).copy(phone = it)
                                    )
                                },
                                label = { Text("Phone") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = editorState.customerGstin,
                                onValueChange = {
                                    val upper = it.uppercase()
                                    val stateCode = IndianStates.extractStateCodeFromGstin(upper)
                                    val cust = (editorState.selectedCustomer ?: Customer(name = editorState.customerName))
                                        .copy(gstin = upper)
                                    viewModel.selectCustomer(
                                        if (stateCode != null) cust.copy(stateCode = stateCode) else cust
                                    )
                                },
                                label = { Text("GSTIN (Optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = editorState.customerAddress,
                            onValueChange = {
                                viewModel.selectCustomer(
                                    (editorState.selectedCustomer ?: Customer(name = editorState.customerName)).copy(billingAddress = it)
                                )
                            },
                            label = { Text("Billing Address") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Line Items Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items & Products (${editorState.items.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )

                    Button(
                        onClick = { viewModel.addLineItem() },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp)
                    }
                }
            }

            itemsIndexed(editorState.items) { index, item ->
                LineItemCard(
                    index = index,
                    item = item,
                    isInterState = editorState.isInterState,
                    isUnregistered = businessProfile?.registrationType == "UNREGISTERED",
                    onUpdate = { updated -> viewModel.updateLineItem(index, updated) },
                    onRemove = { viewModel.removeLineItem(index) },
                    onPickProduct = { productPickerItemIndex = index },
                    canRemove = editorState.items.size > 1
                )
            }

            // 4. Additional Charges & Amount Paid
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Charges & Payments",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = if (editorState.additionalCharges == 0.0) "" else editorState.additionalCharges.toString(),
                                onValueChange = {
                                    val num = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateAdditionalCharges(num)
                                },
                                label = { Text("Shipping / Freight (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = if (editorState.amountPaid == 0.0) "" else editorState.amountPaid.toString(),
                                onValueChange = {
                                    val num = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateAmountPaid(num)
                                },
                                label = { Text("Amount Paid (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SelectableDropdown(
                            label = "Payment Method",
                            options = PaymentMethod.values().toList(),
                            selectedOption = editorState.paymentMethod,
                            onOptionSelected = { viewModel.updatePaymentMethod(it) },
                            itemLabel = { it.label },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editorState.notes,
                            onValueChange = { viewModel.updateNotes(it) },
                            label = { Text("Customer Notes") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 5. Live Calculation Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy900)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "Invoice Summary",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SummaryRow("Subtotal:", formatIndianCurrency(liveCalculation.subTotal))
                        if (liveCalculation.totalDiscount > 0) {
                            SummaryRow("Total Discount:", "- ${formatIndianCurrency(liveCalculation.totalDiscount)}")
                        }
                        SummaryRow("Taxable Value:", formatIndianCurrency(liveCalculation.taxableAmount))

                        if (liveCalculation.cgstAmount > 0) {
                            SummaryRow("CGST:", formatIndianCurrency(liveCalculation.cgstAmount))
                            SummaryRow("SGST:", formatIndianCurrency(liveCalculation.sgstAmount))
                        }
                        if (liveCalculation.igstAmount > 0) {
                            SummaryRow("IGST:", formatIndianCurrency(liveCalculation.igstAmount))
                        }
                        if (liveCalculation.additionalCharges > 0) {
                            SummaryRow("Shipping / Extra:", formatIndianCurrency(liveCalculation.additionalCharges))
                        }
                        if (liveCalculation.roundOff != 0.0) {
                            SummaryRow("Round Off:", formatIndianCurrency(liveCalculation.roundOff))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grand Total:",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = formatIndianCurrency(liveCalculation.grandTotal),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4DD0E1),
                                    fontSize = 22.sp
                                )
                            )
                        }

                        if (liveCalculation.balanceDue > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Balance Due:",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFFCA5A5))
                                )
                                Text(
                                    text = formatIndianCurrency(liveCalculation.balanceDue),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = liveCalculation.amountInWords,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Customer Picker
    if (showCustomerPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCustomerPicker = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Existing Customer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (customers.isEmpty()) {
                    Text(
                        text = "No saved customers yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customers) { c ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCustomer(c)
                                        showCustomerPicker = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = c.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (c.companyName.isNotBlank()) {
                                        Text(text = c.companyName, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        text = "State: ${c.state} (${c.stateCode}) | Phone: ${c.phone}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Product Picker for Line Item
    productPickerItemIndex?.let { itemIdx ->
        ModalBottomSheet(
            onDismissRequest = { productPickerItemIndex = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Product / Service",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (products.isEmpty()) {
                    Text(
                        text = "No saved products yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products) { p ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.populateLineItemFromProduct(itemIdx, p)
                                        productPickerItemIndex = null
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "HSN: ${p.hsnSacCode} | Unit: ${p.unit} | GST: ${p.gstRate}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = formatIndianCurrency(p.price),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineItemCard(
    index: Int,
    item: EditableLineItem,
    isInterState: Boolean,
    isUnregistered: Boolean,
    onUpdate: (EditableLineItem) -> Unit,
    onRemove: () -> Unit,
    onPickProduct: () -> Unit,
    canRemove: Boolean
) {
    val lineCalc = remember(item, isInterState, isUnregistered) {
        GstCalculationEngine.calculateLineItem(
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            discount = item.discount,
            isDiscountPercent = item.isDiscountPercent,
            isTaxInclusive = item.isTaxInclusive,
            gstRate = item.gstRate,
            cessRate = item.cessRate,
            isInterState = isInterState,
            isUnregisteredSeller = isUnregistered
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Navy900, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Item Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onPickProduct,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Product", fontSize = 11.sp)
                    }

                    if (canRemove) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Item", tint = Red700)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = item.productName,
                onValueChange = { onUpdate(item.copy(productName = it)) },
                label = { Text("Product / Service Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.hsnSacCode,
                    onValueChange = { onUpdate(item.copy(hsnSacCode = it)) },
                    label = { Text("HSN / SAC") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                SelectableDropdown(
                    label = "Unit",
                    options = StandardUnits.ALL,
                    selectedOption = item.unit,
                    onOptionSelected = { onUpdate(item.copy(unit = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                    onValueChange = {
                        val q = it.toDoubleOrNull() ?: 1.0
                        onUpdate(item.copy(quantity = q))
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )

                OutlinedTextField(
                    value = if (item.unitPrice == 0.0) "" else item.unitPrice.toString(),
                    onValueChange = {
                        val p = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(unitPrice = p))
                    },
                    label = { Text("Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.1f)
                )

                SelectableDropdown(
                    label = "GST %",
                    options = StandardGstRates.ALL,
                    selectedOption = item.gstRate,
                    onOptionSelected = { onUpdate(item.copy(gstRate = it)) },
                    itemLabel = { "${it}%" },
                    modifier = Modifier.weight(1f)
                )
            }

            // Discount & Tax Inclusive Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (item.discount == 0.0) "" else item.discount.toString(),
                    onValueChange = {
                        val d = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(discount = d))
                    },
                    label = { Text(if (item.isDiscountPercent) "Disc (%)" else "Disc (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(130.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tax Inclusive", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = item.isTaxInclusive,
                        onCheckedChange = { onUpdate(item.copy(isTaxInclusive = it)) }
                    )
                }
            }

            // Line Calculation Mini Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Taxable: ${formatIndianCurrency(lineCalc.taxableAmount)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        val taxText = if (isInterState) "IGST: ${formatIndianCurrency(lineCalc.igstAmount)}"
                        else "CGST: ${formatIndianCurrency(lineCalc.cgstAmount)} | SGST: ${formatIndianCurrency(lineCalc.sgstAmount)}"
                        Text(text = taxText, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }

                    Text(
                        text = "Total: ${formatIndianCurrency(lineCalc.totalAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
