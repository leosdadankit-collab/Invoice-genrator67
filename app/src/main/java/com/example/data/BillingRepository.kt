package com.example.data

import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceWithDetails
import com.example.model.PaymentRecord
import com.example.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BillingRepository(private val dao: InvoiceDao) {

    val businessProfile: Flow<BusinessProfile?> = dao.getBusinessProfiles().map { it.firstOrNull() }
    val allInvoices: Flow<List<Invoice>> = dao.getAllInvoices()
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val allPayments: Flow<List<PaymentRecord>> = dao.getAllPayments()

    suspend fun getBusinessProfileDirect(): BusinessProfile? = dao.getBusinessProfileDirect()
    suspend fun saveBusinessProfile(profile: BusinessProfile) = dao.saveBusinessProfile(profile)

    fun searchInvoices(query: String): Flow<List<Invoice>> = dao.searchInvoices(query)
    fun getInvoicesByStatus(status: String): Flow<List<Invoice>> = dao.getInvoicesByStatus(status)

    suspend fun getInvoiceById(id: Long): Invoice? = dao.getInvoiceById(id)
    suspend fun getInvoiceItemsDirect(invoiceId: Long): List<InvoiceItem> = dao.getInvoiceItemsDirect(invoiceId)
    suspend fun getPaymentsForInvoiceDirect(invoiceId: Long): List<PaymentRecord> = dao.getPaymentsForInvoiceDirect(invoiceId)

    suspend fun getInvoiceWithDetails(invoiceId: Long): InvoiceWithDetails? {
        val invoice = dao.getInvoiceById(invoiceId) ?: return null
        val items = dao.getInvoiceItemsDirect(invoiceId)
        val payments = dao.getPaymentsForInvoiceDirect(invoiceId)
        return InvoiceWithDetails(invoice, items, payments)
    }

    suspend fun getNextInvoiceSequence(): Long {
        val maxId = dao.getMaxInvoiceId()
        return maxId + 1L
    }

    suspend fun saveFullInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = if (invoice.id == 0L) {
            dao.insertInvoice(invoice)
        } else {
            dao.updateInvoice(invoice)
            dao.deleteItemsForInvoice(invoice.id)
            invoice.id
        }

        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        dao.insertInvoiceItems(itemsWithId)

        for (item in itemsWithId) {
            if (item.productId != null && item.productId > 0) {
                dao.reduceProductStock(item.productId, item.quantity)
            }
        }

        return invoiceId
    }

    suspend fun deleteFullInvoice(invoiceId: Long) {
        dao.deletePaymentsForInvoice(invoiceId)
        dao.deleteItemsForInvoice(invoiceId)
        dao.deleteInvoice(invoiceId)
    }

    suspend fun cancelInvoice(invoiceId: Long) {
        dao.cancelInvoice(invoiceId)
    }

    suspend fun recordPayment(invoiceId: Long, payment: PaymentRecord) {
        dao.insertPayment(payment.copy(invoiceId = invoiceId))
        val currentInvoice = dao.getInvoiceById(invoiceId) ?: return
        val allPayments = dao.getPaymentsForInvoiceDirect(invoiceId)
        val totalPaid = allPayments.sumOf { it.amount }
        val balance = (currentInvoice.grandTotal - totalPaid).coerceAtLeast(0.0)
        val status = if (balance <= 0.0 && currentInvoice.grandTotal > 0) {
            "PAID"
        } else if (totalPaid > 0.0) {
            "PARTIAL"
        } else {
            "UNPAID"
        }
        dao.updateInvoice(
            currentInvoice.copy(
                amountPaid = totalPaid,
                balanceDue = balance,
                paymentStatus = status,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Customers
    fun searchCustomers(query: String): Flow<List<Customer>> = dao.searchCustomers(query)
    suspend fun insertCustomer(customer: Customer): Long = dao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)
    suspend fun deleteCustomer(id: Long) = dao.deleteCustomer(id)
    suspend fun getCustomerById(id: Long): Customer? = dao.getCustomerById(id)

    // Products
    fun searchProducts(query: String): Flow<List<Product>> = dao.searchProducts(query)
    suspend fun insertProduct(product: Product): Long = dao.insertProduct(product)
    suspend fun updateProduct(product: Product) = dao.updateProduct(product)
    suspend fun deleteProduct(id: Long) = dao.deleteProduct(id)
    suspend fun getProductById(id: Long): Product? = dao.getProductById(id)
}
