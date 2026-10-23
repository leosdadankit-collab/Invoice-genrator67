package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.DocumentType
import com.example.model.GstCalculationEngine
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.PaymentRecord
import com.example.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BusinessProfile::class,
        Customer::class,
        Product::class,
        Invoice::class,
        InvoiceItem::class,
        PaymentRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bharat_billing_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.invoiceDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: InvoiceDao) {
            // 1. Initial Business Profile
            val business = BusinessProfile(
                id = 1L,
                legalName = "Bharat Enterprise & Solutions Pvt Ltd",
                tradeName = "Bharat Enterprise",
                gstin = "27AABCB1234D1Z8",
                pan = "AABCB1234D",
                registrationType = "GST_REGISTERED",
                address = "Shop 12, Ground Floor, Trade Center, Nariman Point",
                city = "Mumbai",
                state = "Maharashtra",
                stateCode = "27",
                pinCode = "400021",
                phone = "+91 98200 12345",
                email = "billing@bharatenterprise.in",
                website = "https://bharatenterprise.in",
                bankName = "HDFC Bank",
                accountNumber = "50200012345678",
                ifscCode = "HDFC0000128",
                branchName = "Nariman Point Branch",
                upiId = "bharatenterprise@hdfcbank",
                invoicePrefix = "INV",
                defaultTerms = "1. Goods once sold will not be accepted back or exchanged.\n2. Payment terms: Due within 15 days of invoice date.\n3. Interest @ 18% p.a. will be charged for delayed payments.\n4. All disputes subject to Mumbai jurisdiction only.",
                authorizedSignatory = "Authorized Signatory"
            )
            dao.saveBusinessProfile(business)

            // 2. Initial Customers
            val c1Id = dao.insertCustomer(
                Customer(
                    name = "Rajesh Sharma",
                    companyName = "Apex Retailers LLP",
                    phone = "+91 98111 22334",
                    email = "rajesh@apexretail.in",
                    gstin = "27AAACA5678B1Z2",
                    pan = "AAACA5678B",
                    billingAddress = "45, Linking Road, Bandra West",
                    shippingAddress = "45, Linking Road, Bandra West",
                    state = "Maharashtra",
                    stateCode = "27",
                    pinCode = "400050"
                )
            )

            val c2Id = dao.insertCustomer(
                Customer(
                    name = "Vikram Patel",
                    companyName = "Gujarat Tech Ventures",
                    phone = "+91 97234 56789",
                    email = "vikram@gtv.co.in",
                    gstin = "24AABCG9876H1ZX",
                    pan = "AABCG9876H",
                    billingAddress = "102, Silicon Square, SG Highway",
                    shippingAddress = "102, Silicon Square, SG Highway",
                    state = "Gujarat",
                    stateCode = "24",
                    pinCode = "380054"
                )
            )

            val c3Id = dao.insertCustomer(
                Customer(
                    name = "Anita Deshmukh",
                    companyName = "Deshmukh Mart",
                    phone = "+91 94220 88990",
                    email = "anita@deshmukhmart.com",
                    gstin = "",
                    billingAddress = "Plot 18, MIDC Industrial Area, Pune",
                    state = "Maharashtra",
                    stateCode = "27",
                    pinCode = "411019"
                )
            )

            // 3. Initial Products
            val p1Id = dao.insertProduct(
                Product(
                    name = "Wireless Keyboard & Mouse Combo",
                    description = "Ergonomic 2.4GHz wireless combo with nano receiver",
                    sku = "KB-WL-01",
                    hsnSacCode = "8471",
                    category = "Electronics",
                    unit = "SET",
                    price = 1450.0,
                    purchasePrice = 950.0,
                    gstRate = 18.0,
                    isTaxInclusive = false,
                    stockQuantity = 45.0
                )
            )

            val p2Id = dao.insertProduct(
                Product(
                    name = "USB-C Fast Charging Cable 65W",
                    description = "Braided 1.5m fast charge data cable",
                    sku = "CB-USBC-65W",
                    hsnSacCode = "8544",
                    category = "Accessories",
                    unit = "PCS",
                    price = 350.0,
                    purchasePrice = 180.0,
                    gstRate = 18.0,
                    isTaxInclusive = false,
                    stockQuantity = 120.0
                )
            )

            val p3Id = dao.insertProduct(
                Product(
                    name = "A4 Premium Copier Paper 75 GSM",
                    description = "Box of 5 reams (2500 sheets total)",
                    sku = "PPR-A4-75",
                    hsnSacCode = "4802",
                    category = "Stationery",
                    unit = "BOX",
                    price = 1100.0,
                    purchasePrice = 820.0,
                    gstRate = 12.0,
                    isTaxInclusive = false,
                    stockQuantity = 30.0
                )
            )

            val p4Id = dao.insertProduct(
                Product(
                    name = "IT Consulting & Maintenance Service",
                    description = "Monthly network audit and workstation maintenance",
                    sku = "SRV-IT-MNT",
                    hsnSacCode = "9983",
                    category = "Services",
                    unit = "JOB",
                    price = 5000.0,
                    purchasePrice = 0.0,
                    gstRate = 18.0,
                    isTaxInclusive = false,
                    isService = true,
                    stockQuantity = 999.0
                )
            )

            // 4. Initial Sample Invoices
            val now = System.currentTimeMillis()
            val inv1Number = GstCalculationEngine.formatInvoiceNumber("INV", 1, now)

            val item1 = GstCalculationEngine.calculateLineItem(
                quantity = 2.0,
                unitPrice = 1450.0,
                discount = 100.0,
                isDiscountPercent = false,
                isTaxInclusive = false,
                gstRate = 18.0,
                isInterState = false
            )

            val item2 = GstCalculationEngine.calculateLineItem(
                quantity = 4.0,
                unitPrice = 350.0,
                discount = 5.0,
                isDiscountPercent = true,
                isTaxInclusive = false,
                gstRate = 18.0,
                isInterState = false
            )

            val calcResult = GstCalculationEngine.calculateInvoice(
                items = listOf(item1, item2),
                additionalCharges = 150.0, // shipping
                amountPaid = 5000.0,
                dueDate = now + (15L * 86400000L)
            )

            val sampleInvoice = Invoice(
                invoiceNumber = inv1Number,
                documentType = DocumentType.TAX_INVOICE.name,
                invoiceDate = now - (2L * 86400000L),
                dueDate = now + (13L * 86400000L),
                customerId = c1Id,
                customerName = "Rajesh Sharma (Apex Retailers)",
                customerGstin = "27AAACA5678B1Z2",
                customerPhone = "+91 98111 22334",
                customerAddress = "45, Linking Road, Bandra West, Mumbai",
                customerState = "Maharashtra",
                customerStateCode = "27",
                placeOfSupply = "Maharashtra",
                placeOfSupplyStateCode = "27",
                isInterState = false,
                subTotal = calcResult.subTotal,
                totalDiscount = calcResult.totalDiscount,
                taxableAmount = calcResult.taxableAmount,
                cgstAmount = calcResult.cgstAmount,
                sgstAmount = calcResult.sgstAmount,
                igstAmount = calcResult.igstAmount,
                cessAmount = calcResult.cessAmount,
                additionalCharges = calcResult.additionalCharges,
                roundOff = calcResult.roundOff,
                grandTotal = calcResult.grandTotal,
                amountPaid = 5000.0,
                balanceDue = calcResult.balanceDue,
                paymentStatus = calcResult.paymentStatus.name,
                paymentMethod = "UPI"
            )

            val invId = dao.insertInvoice(sampleInvoice)
            dao.insertInvoiceItems(
                listOf(
                    InvoiceItem(
                        invoiceId = invId,
                        productId = p1Id,
                        productName = "Wireless Keyboard & Mouse Combo",
                        description = "Ergonomic 2.4GHz wireless combo",
                        hsnSacCode = "8471",
                        quantity = 2.0,
                        unit = "SET",
                        unitPrice = 1450.0,
                        discount = 100.0,
                        isDiscountPercent = false,
                        gstRate = 18.0,
                        taxableAmount = item1.taxableAmount,
                        cgstRate = item1.cgstRate,
                        cgstAmount = item1.cgstAmount,
                        sgstRate = item1.sgstRate,
                        sgstAmount = item1.sgstAmount,
                        totalAmount = item1.totalAmount
                    ),
                    InvoiceItem(
                        invoiceId = invId,
                        productId = p2Id,
                        productName = "USB-C Fast Charging Cable 65W",
                        description = "Braided 1.5m fast charge data cable",
                        hsnSacCode = "8544",
                        quantity = 4.0,
                        unit = "PCS",
                        unitPrice = 350.0,
                        discount = 5.0,
                        isDiscountPercent = true,
                        gstRate = 18.0,
                        taxableAmount = item2.taxableAmount,
                        cgstRate = item2.cgstRate,
                        cgstAmount = item2.cgstAmount,
                        sgstRate = item2.sgstRate,
                        sgstAmount = item2.sgstAmount,
                        totalAmount = item2.totalAmount
                    )
                )
            )

            dao.insertPayment(
                PaymentRecord(
                    invoiceId = invId,
                    amount = 5000.0,
                    paymentDate = now - (1L * 86400000L),
                    paymentMethod = "UPI",
                    transactionRef = "UPI/428901238491",
                    notes = "Advance payment via PhonePe QR"
                )
            )
        }
    }
}
