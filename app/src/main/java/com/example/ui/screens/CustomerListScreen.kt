package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import com.example.model.Customer
import com.example.model.IndianStates
import com.example.pdf.CsvExporter
import com.example.ui.components.SelectableDropdown
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red700
import com.example.ui.viewmodel.BillingViewModel

@Composable
fun CustomerListScreen(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()

    var customerDialogItem by remember { mutableStateOf<Customer?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
                    onValueChange = { viewModel.updateCustomerSearchQuery(it) },
                    placeholder = { Text("Search by Name, Phone, GSTIN...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateCustomerSearchQuery("") }) {
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
                    onClick = { CsvExporter.exportCustomersToCsv(context, customers) }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Navy900)
                }
            }

            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No customers matching '$searchQuery'" else "No customers saved yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Customer")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(customers, key = { it.id }) { customer ->
                        CustomerItemCard(
                            customer = customer,
                            onEdit = { customerDialogItem = customer },
                            onDelete = { viewModel.deleteCustomer(customer.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomerEditDialog(
            customer = Customer(name = ""),
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveCustomer(it)
                showAddDialog = false
            }
        )
    }

    customerDialogItem?.let { c ->
        CustomerEditDialog(
            customer = c,
            onDismiss = { customerDialogItem = null },
            onSave = {
                viewModel.saveCustomer(it)
                customerDialogItem = null
            }
        )
    }
}

@Composable
private fun CustomerItemCard(
    customer: Customer,
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
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (customer.companyName.isNotBlank()) {
                    Text(
                        text = customer.companyName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Navy900
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Phone: ${customer.phone.ifBlank { "N/A" }} | State: ${customer.state} (${customer.stateCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (customer.gstin.isNotBlank()) {
                    Text(
                        text = "GSTIN: ${customer.gstin}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
            }

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

@Composable
fun CustomerEditDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var companyName by remember { mutableStateOf(customer.companyName) }
    var phone by remember { mutableStateOf(customer.phone) }
    var email by remember { mutableStateOf(customer.email) }
    var gstin by remember { mutableStateOf(customer.gstin) }
    var billingAddress by remember { mutableStateOf(customer.billingAddress) }
    var pinCode by remember { mutableStateOf(customer.pinCode) }

    var selectedState by remember {
        val st = IndianStates.ALL.find { it.code == customer.stateCode } ?: IndianStates.ALL.first()
        mutableStateOf(st)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer.id == 0L) "Add Customer" else "Edit Customer") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Person Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company / Business Name") },
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
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = {
                            val upper = it.uppercase()
                            gstin = upper
                            val extractedCode = IndianStates.extractStateCodeFromGstin(upper)
                            if (extractedCode != null) {
                                IndianStates.findByCode(extractedCode)?.let { st ->
                                    selectedState = st
                                }
                            }
                        },
                        label = { Text("GSTIN (15 Digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    SelectableDropdown(
                        label = "State / Place of Supply",
                        options = IndianStates.ALL,
                        selectedOption = selectedState,
                        onOptionSelected = { selectedState = it },
                        itemLabel = { "${it.code} - ${it.name}" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = billingAddress,
                        onValueChange = { billingAddress = it },
                        label = { Text("Billing Address") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("PIN Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val c = customer.copy(
                            name = name,
                            companyName = companyName,
                            phone = phone,
                            email = email,
                            gstin = gstin,
                            state = selectedState.name,
                            stateCode = selectedState.code,
                            billingAddress = billingAddress,
                            shippingAddress = billingAddress,
                            pinCode = pinCode
                        )
                        onSave(c)
                    }
                }
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
