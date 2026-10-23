package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.PaymentRecord
import com.example.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    // --- Business Profile ---
    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getBusinessProfiles(): Flow<List<BusinessProfile>>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    suspend fun getBusinessProfileDirect(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBusinessProfile(profile: BusinessProfile)

    // --- Invoices ---
    @Query("SELECT * FROM invoices ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%' ORDER BY invoiceDate DESC")
    fun searchInvoices(query: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE paymentStatus = :status ORDER BY invoiceDate DESC")
    fun getInvoicesByStatus(status: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT COUNT(*) FROM invoices")
    fun getInvoiceCount(): Flow<Int>

    @Query("SELECT COALESCE(MAX(id), 0) FROM invoices")
    suspend fun getMaxInvoiceId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: Long)

    @Query("UPDATE invoices SET isCancelled = 1, paymentStatus = 'CANCELLED' WHERE id = :id")
    suspend fun cancelInvoice(id: Long)

    // --- Invoice Items ---
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getInvoiceItemsDirect(invoiceId: Long): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long)

    // --- Customers ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR gstin LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR hsnSacCode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty WHERE id = :productId AND isService = 0")
    suspend fun reduceProductStock(productId: Long, qty: Double)

    // --- Payments ---
    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC")
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForInvoiceDirect(invoiceId: Long): List<PaymentRecord>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord): Long

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query("DELETE FROM payments WHERE invoiceId = :invoiceId")
    suspend fun deletePaymentsForInvoice(invoiceId: Long)
}
