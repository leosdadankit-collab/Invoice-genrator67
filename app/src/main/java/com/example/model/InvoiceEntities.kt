package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Long = 1L,
    val legalName: String = "Bharat Traders & Enterprises",
    val tradeName: String = "Bharat Traders",
    val gstin: String = "27AABCB1234D1Z8",
    val pan: String = "AABCB1234D",
    val registrationType: String = BusinessRegistrationType.GST_REGISTERED.name,
    val address: String = "Shop No. 14, Commercial Market",
    val city: String = "Mumbai",
    val state: String = "Maharashtra",
    val stateCode: String = "27",
    val pinCode: String = "400001",
    val phone: String = "+91 98765 43210",
    val email: String = "contact@bharattraders.in",
    val website: String = "https://bharattraders.in",
    val bankName: String = "State Bank of India",
    val accountNumber: String = "30123456789",
    val ifscCode: String = "SBIN0001234",
    val branchName: String = "Fort Branch, Mumbai",
    val upiId: String = "bharattraders@sbi",
    val invoicePrefix: String = "INV",
    val defaultTerms: String = "1. Goods once sold will not be taken back or exchanged.\n2. Payment due within 15 days of invoice date.\n3. Subject to local jurisdiction.",
    val authorizedSignatory: String = "Authorized Signatory",
    val signaturePath: String? = null,
    val logoPath: String? = null
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["phone"]), Index(value = ["gstin"])]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val pan: String = "",
    val billingAddress: String = "",
    val shippingAddress: String = "",
    val state: String = "Maharashtra",
    val stateCode: String = "27",
    val pinCode: String = "",
    val creditPeriodDays: Int = 15,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"]), Index(value = ["hsnSacCode"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val sku: String = "",
    val hsnSacCode: String = "",
    val category: String = "General",
    val unit: String = "PCS",
    val price: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val gstRate: Double = 18.0,
    val isTaxInclusive: Boolean = false,
    val defaultDiscount: Double = 0.0,
    val isDiscountPercent: Boolean = true,
    val stockQuantity: Double = 100.0,
    val lowStockThreshold: Double = 10.0,
    val isService: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["invoiceNumber"], unique = true), Index(value = ["customerId"])]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val documentType: String = DocumentType.TAX_INVOICE.name,
    val invoiceDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000),
    val customerId: Long? = null,
    val customerName: String,
    val customerGstin: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerState: String = "Maharashtra",
    val customerStateCode: String = "27",
    val placeOfSupply: String = "Maharashtra",
    val placeOfSupplyStateCode: String = "27",
    val isInterState: Boolean = false,
    val isReverseCharge: Boolean = false,
    val vehicleNumber: String = "",
    val eWayBillNumber: String = "",
    val subTotal: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val cessAmount: Double = 0.0,
    val additionalCharges: Double = 0.0,
    val roundOff: Double = 0.0,
    val grandTotal: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val paymentStatus: String = PaymentStatus.UNPAID.name,
    val paymentMethod: String = PaymentMethod.UPI.name,
    val templateType: String = InvoiceTemplateType.CLASSIC.name,
    val notes: String = "Thank you for your business!",
    val terms: String = "",
    val isCancelled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_items",
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceId: Long = 0L,
    val productId: Long? = null,
    val productName: String,
    val description: String = "",
    val hsnSacCode: String = "",
    val quantity: Double = 1.0,
    val unit: String = "PCS",
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val isDiscountPercent: Boolean = true,
    val isTaxInclusive: Boolean = false,
    val gstRate: Double = 18.0,
    val taxableAmount: Double = 0.0,
    val cgstRate: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstRate: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstRate: Double = 0.0,
    val igstAmount: Double = 0.0,
    val cessRate: Double = 0.0,
    val cessAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["invoiceId"])]
)
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceId: Long,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = PaymentMethod.UPI.name,
    val transactionRef: String = "",
    val notes: String = ""
)

/**
 * Composite data class containing complete invoice bundle
 */
data class InvoiceWithDetails(
    val invoice: Invoice,
    val items: List<InvoiceItem>,
    val payments: List<PaymentRecord>
)
