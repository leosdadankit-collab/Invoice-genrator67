package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BusinessProfile
import com.example.model.BusinessRegistrationType
import com.example.model.IndianStates
import com.example.ui.components.SelectableDropdown
import com.example.ui.components.SignaturePad
import com.example.ui.theme.Navy900
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.BillingViewModel
import java.io.File

@Composable
fun BusinessProfileScreen(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProfile by viewModel.businessProfile.collectAsStateWithLifecycle()

    val profile = currentProfile ?: BusinessProfile()

    var legalName by remember(profile) { mutableStateOf(profile.legalName) }
    var tradeName by remember(profile) { mutableStateOf(profile.tradeName) }
    var gstin by remember(profile) { mutableStateOf(profile.gstin) }
    var pan by remember(profile) { mutableStateOf(profile.pan) }
    var registrationType by remember(profile) {
        val reg = try { BusinessRegistrationType.valueOf(profile.registrationType) } catch (e: Exception) { BusinessRegistrationType.GST_REGISTERED }
        mutableStateOf(reg)
    }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var city by remember(profile) { mutableStateOf(profile.city) }
    var pinCode by remember(profile) { mutableStateOf(profile.pinCode) }
    var selectedState by remember(profile) {
        val st = IndianStates.ALL.find { it.code == profile.stateCode } ?: IndianStates.ALL.first()
        mutableStateOf(st)
    }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var bankName by remember(profile) { mutableStateOf(profile.bankName) }
    var accountNumber by remember(profile) { mutableStateOf(profile.accountNumber) }
    var ifscCode by remember(profile) { mutableStateOf(profile.ifscCode) }
    var upiId by remember(profile) { mutableStateOf(profile.upiId) }
    var invoicePrefix by remember(profile) { mutableStateOf(profile.invoicePrefix) }
    var defaultTerms by remember(profile) { mutableStateOf(profile.defaultTerms) }
    var authorizedSignatory by remember(profile) { mutableStateOf(profile.authorizedSignatory) }
    var signaturePath by remember(profile) { mutableStateOf(profile.signaturePath) }

    var showSignaturePad by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Business Identity Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = Navy900)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Business Identity & GST",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                    }

                    SelectableDropdown(
                        label = "Business Registration Type",
                        options = BusinessRegistrationType.values().toList(),
                        selectedOption = registrationType,
                        onOptionSelected = { registrationType = it },
                        itemLabel = { it.label },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = legalName,
                        onValueChange = { legalName = it },
                        label = { Text("Legal Entity Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tradeName,
                        onValueChange = { tradeName = it },
                        label = { Text("Trade Name (Shown on Invoices) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (registrationType != BusinessRegistrationType.UNREGISTERED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = gstin,
                                onValueChange = {
                                    val upper = it.uppercase()
                                    gstin = upper
                                    val code = IndianStates.extractStateCodeFromGstin(upper)
                                    if (code != null) {
                                        IndianStates.findByCode(code)?.let { st -> selectedState = st }
                                    }
                                },
                                label = { Text("GSTIN (15 Digits) *") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )

                            OutlinedTextField(
                                value = pan,
                                onValueChange = { pan = it.uppercase() },
                                label = { Text("PAN") },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }

                    SelectableDropdown(
                        label = "State of Registration",
                        options = IndianStates.ALL,
                        selectedOption = selectedState,
                        onOptionSelected = { selectedState = it },
                        itemLabel = { "${it.code} - ${it.name}" },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Office / Shop Address") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { pinCode = it },
                            label = { Text("PIN Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
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
            }
        }

        // Bank & UPI Details Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Navy900)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bank & UPI Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                    }

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID (e.g. merchant@upi)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("Account Number") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it.uppercase() },
                            label = { Text("IFSC Code") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }
        }

        // Digital Signature & Terms Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Draw, contentDescription = null, tint = Navy900)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Signature & Invoice Formatting",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                    }

                    OutlinedTextField(
                        value = invoicePrefix,
                        onValueChange = { invoicePrefix = it.uppercase() },
                        label = { Text("Invoice Prefix (e.g. INV)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = authorizedSignatory,
                        onValueChange = { authorizedSignatory = it },
                        label = { Text("Authorized Signatory Name / Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Saved Signature Preview
                    if (!signaturePath.isNullOrBlank()) {
                        val file = File(signaturePath!!)
                        if (file.exists()) {
                            Text("Current Digital Signature:", style = MaterialTheme.typography.labelMedium)
                            val bmp = remember(signaturePath) { BitmapFactory.decodeFile(file.absolutePath) }
                            if (bmp != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "Saved Signature")
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showSignaturePad = !showSignaturePad },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showSignaturePad) "Hide Signature Pad" else "Draw / Update Digital Signature")
                    }

                    if (showSignaturePad) {
                        SignaturePad(
                            onSignatureSaved = { savedPath ->
                                signaturePath = savedPath
                                showSignaturePad = false
                                Toast.makeText(context, "Signature saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    OutlinedTextField(
                        value = defaultTerms,
                        onValueChange = { defaultTerms = it },
                        label = { Text("Standard Invoice Terms & Conditions") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val updated = profile.copy(
                        legalName = legalName,
                        tradeName = tradeName,
                        gstin = gstin,
                        pan = pan,
                        registrationType = registrationType.name,
                        address = address,
                        city = city,
                        state = selectedState.name,
                        stateCode = selectedState.code,
                        pinCode = pinCode,
                        phone = phone,
                        email = email,
                        bankName = bankName,
                        accountNumber = accountNumber,
                        ifscCode = ifscCode,
                        upiId = upiId,
                        invoicePrefix = invoicePrefix,
                        defaultTerms = defaultTerms,
                        authorizedSignatory = authorizedSignatory,
                        signaturePath = signaturePath
                    )
                    viewModel.saveBusinessProfile(updated) {
                        Toast.makeText(context, "Business Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_business_profile_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile & GST Settings", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
