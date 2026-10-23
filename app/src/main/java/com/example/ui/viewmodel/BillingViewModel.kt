package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BillingRepository
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.DocumentType
import com.example.model.GstCalculationEngine
import com.example.model.IndianStates
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceWithDetails
import com.example.model.PaymentMethod
import com.example.model.PaymentRecord
import com.example.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalSales: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalUnpaid: Double = 0.0,
    val totalInvoices: Int = 0,
    val recentInvoices: List<Invoice> = emptyList()
)

data class EditableLineItem(
    val id: Long = 0L,
    val productId: Long? = null,
    val productName: String = "",
    val description: String = "",
    val hsnSacCode: String = "",
    val quantity: Double = 1.0,
    val unit: String = "PCS",
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val isDiscountPercent: Boolean = true,
    val isTaxInclusive: Boolean = false,
    val gstRate: Double = 18.0,
    val cessRate: Double = 0.0
)

data class InvoiceEditorState(
    val invoiceId: Long = 0L,
    val invoiceNumber: String = "",
    val documentType: DocumentType = DocumentType.TAX_INVOICE,
    val invoiceDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (15L * 86400000L),
    val selectedCustomer: Customer? = null,
    val customerName: String = "",
    val customerGstin: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerState: String = "Maharashtra",
    val customerStateCode: String = "27",
    val placeOfSupply: String = "Maharashtra",
    val placeOfSupplyStateCode: String = "27",
    val isInterState: Boolean = false,
    val items: List<EditableLineItem> = emptyList(),
    val additionalCharges: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val notes: String = "Thank you for your business!",
    val terms: String = ""
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillingRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = BillingRepository(db.invoiceDao())
    }

    // Business Profile
    val businessProfile: StateFlow<BusinessProfile?> = repository.businessProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Invoices
    private val _invoiceSearchQuery = MutableStateFlow("")
    val invoiceSearchQuery: StateFlow<String> = _invoiceSearchQuery.asStateFlow()

    private val _invoiceStatusFilter = MutableStateFlow("ALL")
    val invoiceStatusFilter: StateFlow<String> = _invoiceStatusFilter.asStateFlow()

    val invoices: StateFlow<List<Invoice>> = combine(
        repository.allInvoices,
        _invoiceSearchQuery,
        _invoiceStatusFilter
    ) { list, query, filter ->
        list.filter { inv ->
            val matchesQuery = query.isBlank() ||
                    inv.invoiceNumber.contains(query, ignoreCase = true) ||
                    inv.customerName.contains(query, ignoreCase = true)
            val matchesFilter = filter == "ALL" || inv.paymentStatus.equals(filter, ignoreCase = true)
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Stats
    val dashboardStats: StateFlow<DashboardStats> = repository.allInvoices.combine(repository.allPayments) { invList, _ ->
        val totalSales = invList.filter { !it.isCancelled }.sumOf { it.grandTotal }
        val totalPaid = invList.filter { !it.isCancelled }.sumOf { it.amountPaid }
        val totalUnpaid = invList.filter { !it.isCancelled }.sumOf { it.balanceDue }
        val count = invList.size
        val recent = invList.take(5)
        DashboardStats(
            totalSales = totalSales,
            totalPaid = totalPaid,
            totalUnpaid = totalUnpaid,
            totalInvoices = count,
            recentInvoices = recent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Customers
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    val customers: StateFlow<List<Customer>> = combine(
        repository.allCustomers,
        _customerSearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.companyName.contains(query, ignoreCase = true) ||
                    it.phone.contains(query, ignoreCase = true) ||
                    it.gstin.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    val products: StateFlow<List<Product>> = combine(
        repository.allProducts,
        _productSearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.sku.contains(query, ignoreCase = true) ||
                    it.hsnSacCode.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoice Editor State
    private val _editorState = MutableStateFlow(InvoiceEditorState())
    val editorState: StateFlow<InvoiceEditorState> = _editorState.asStateFlow()

    fun updateInvoiceSearchQuery(query: String) {
        _invoiceSearchQuery.value = query
    }

    fun updateInvoiceStatusFilter(filter: String) {
        _invoiceStatusFilter.value = filter
    }

    fun updateCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun updateProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    // --- Invoice Editor Actions ---

    fun initNewInvoice() {
        viewModelScope.launch {
            val biz = repository.getBusinessProfileDirect()
            val nextSeq = repository.getNextInvoiceSequence()
            val prefix = biz?.invoicePrefix ?: "INV"
            val invNumber = GstCalculationEngine.formatInvoiceNumber(prefix, nextSeq)

            _editorState.value = InvoiceEditorState(
                invoiceNumber = invNumber,
                placeOfSupply = biz?.state ?: "Maharashtra",
                placeOfSupplyStateCode = biz?.stateCode ?: "27",
                terms = biz?.defaultTerms ?: "",
                items = listOf(
                    EditableLineItem(
                        productName = "Product / Service",
                        quantity = 1.0,
                        unit = "PCS",
                        unitPrice = 1000.0,
                        gstRate = 18.0
                    )
                )
            )
        }
    }

    fun loadInvoiceForEditing(invoiceId: Long) {
        viewModelScope.launch {
            val bundle = repository.getInvoiceWithDetails(invoiceId) ?: return@launch
            val inv = bundle.invoice
            val editableItems = bundle.items.map {
                EditableLineItem(
                    id = it.id,
                    productId = it.productId,
                    productName = it.productName,
                    description = it.description,
                    hsnSacCode = it.hsnSacCode,
                    quantity = it.quantity,
                    unit = it.unit,
                    unitPrice = it.unitPrice,
                    discount = it.discount,
                    isDiscountPercent = it.isDiscountPercent,
                    isTaxInclusive = it.isTaxInclusive,
                    gstRate = it.gstRate,
                    cessRate = it.cessRate
                )
            }

            _editorState.value = InvoiceEditorState(
                invoiceId = inv.id,
                invoiceNumber = inv.invoiceNumber,
                documentType = try { DocumentType.valueOf(inv.documentType) } catch (e: Exception) { DocumentType.TAX_INVOICE },
                invoiceDate = inv.invoiceDate,
                dueDate = inv.dueDate,
                customerName = inv.customerName,
                customerGstin = inv.customerGstin,
                customerPhone = inv.customerPhone,
                customerAddress = inv.customerAddress,
                customerState = inv.customerState,
                customerStateCode = inv.customerStateCode,
                placeOfSupply = inv.placeOfSupply,
                placeOfSupplyStateCode = inv.placeOfSupplyStateCode,
                isInterState = inv.isInterState,
                items = editableItems,
                additionalCharges = inv.additionalCharges,
                amountPaid = inv.amountPaid,
                paymentMethod = try { PaymentMethod.valueOf(inv.paymentMethod) } catch (e: Exception) { PaymentMethod.UPI },
                notes = inv.notes,
                terms = inv.terms
            )
        }
    }

    fun selectCustomer(customer: Customer) {
        val biz = businessProfile.value
        val bizStateCode = biz?.stateCode ?: "27"
        val isInter = customer.stateCode.isNotBlank() && customer.stateCode != bizStateCode

        _editorState.value = _editorState.value.copy(
            selectedCustomer = customer,
            customerName = if (customer.companyName.isNotBlank()) "${customer.name} (${customer.companyName})" else customer.name,
            customerGstin = customer.gstin,
            customerPhone = customer.phone,
            customerAddress = customer.billingAddress,
            customerState = customer.state,
            customerStateCode = customer.stateCode,
            placeOfSupply = customer.state,
            placeOfSupplyStateCode = customer.stateCode,
            isInterState = isInter
        )
    }

    fun updateDocumentType(type: DocumentType) {
        _editorState.value = _editorState.value.copy(documentType = type)
    }

    fun updatePlaceOfSupply(state: com.example.model.IndianState) {
        val biz = businessProfile.value
        val bizStateCode = biz?.stateCode ?: "27"
        val isInter = state.code != bizStateCode

        _editorState.value = _editorState.value.copy(
            placeOfSupply = state.name,
            placeOfSupplyStateCode = state.code,
            isInterState = isInter
        )
    }

    fun addLineItem() {
        val current = _editorState.value.items.toMutableList()
        current.add(
            EditableLineItem(
                productName = "New Item",
                quantity = 1.0,
                unit = "PCS",
                unitPrice = 500.0,
                gstRate = 18.0
            )
        )
        _editorState.value = _editorState.value.copy(items = current)
    }

    fun removeLineItem(index: Int) {
        val current = _editorState.value.items.toMutableList()
        if (index in current.indices && current.size > 1) {
            current.removeAt(index)
            _editorState.value = _editorState.value.copy(items = current)
        }
    }

    fun updateLineItem(index: Int, updated: EditableLineItem) {
        val current = _editorState.value.items.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _editorState.value = _editorState.value.copy(items = current)
        }
    }

    fun populateLineItemFromProduct(index: Int, product: Product) {
        val current = _editorState.value.items.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(
                productId = product.id,
                productName = product.name,
                description = product.description,
                hsnSacCode = product.hsnSacCode,
                unit = product.unit,
                unitPrice = product.price,
                gstRate = product.gstRate,
                isTaxInclusive = product.isTaxInclusive,
                discount = product.defaultDiscount,
                isDiscountPercent = product.isDiscountPercent
            )
            _editorState.value = _editorState.value.copy(items = current)
        }
    }

    fun updateAdditionalCharges(charges: Double) {
        _editorState.value = _editorState.value.copy(additionalCharges = charges)
    }

    fun updateAmountPaid(amount: Double) {
        _editorState.value = _editorState.value.copy(amountPaid = amount)
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        _editorState.value = _editorState.value.copy(paymentMethod = method)
    }

    fun updateNotes(notes: String) {
        _editorState.value = _editorState.value.copy(notes = notes)
    }

    fun updateTerms(terms: String) {
        _editorState.value = _editorState.value.copy(terms = terms)
    }

    fun saveInvoice(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val state = _editorState.value
            val biz = repository.getBusinessProfileDirect() ?: BusinessProfile()
            val isUnregistered = biz.registrationType == "UNREGISTERED"

            val calculatedItems = state.items.map { item ->
                val calc = GstCalculationEngine.calculateLineItem(
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    isDiscountPercent = item.isDiscountPercent,
                    isTaxInclusive = item.isTaxInclusive,
                    gstRate = item.gstRate,
                    cessRate = item.cessRate,
                    isInterState = state.isInterState,
                    isUnregisteredSeller = isUnregistered
                )
                Pair(item, calc)
            }

            val invoiceCalc = GstCalculationEngine.calculateInvoice(
                items = calculatedItems.map { it.second },
                additionalCharges = state.additionalCharges,
                amountPaid = state.amountPaid,
                dueDate = state.dueDate
            )

            val invoice = Invoice(
                id = state.invoiceId,
                invoiceNumber = state.invoiceNumber,
                documentType = state.documentType.name,
                invoiceDate = state.invoiceDate,
                dueDate = state.dueDate,
                customerId = state.selectedCustomer?.id,
                customerName = state.customerName.ifBlank { "Cash Customer" },
                customerGstin = state.customerGstin,
                customerPhone = state.customerPhone,
                customerAddress = state.customerAddress,
                customerState = state.customerState,
                customerStateCode = state.customerStateCode,
                placeOfSupply = state.placeOfSupply,
                placeOfSupplyStateCode = state.placeOfSupplyStateCode,
                isInterState = state.isInterState,
                subTotal = invoiceCalc.subTotal,
                totalDiscount = invoiceCalc.totalDiscount,
                taxableAmount = invoiceCalc.taxableAmount,
                cgstAmount = invoiceCalc.cgstAmount,
                sgstAmount = invoiceCalc.sgstAmount,
                igstAmount = invoiceCalc.igstAmount,
                cessAmount = invoiceCalc.cessAmount,
                additionalCharges = invoiceCalc.additionalCharges,
                roundOff = invoiceCalc.roundOff,
                grandTotal = invoiceCalc.grandTotal,
                amountPaid = state.amountPaid,
                balanceDue = invoiceCalc.balanceDue,
                paymentStatus = invoiceCalc.paymentStatus.name,
                paymentMethod = state.paymentMethod.name,
                notes = state.notes,
                terms = state.terms
            )

            val entityItems = calculatedItems.map { (item, calc) ->
                InvoiceItem(
                    invoiceId = state.invoiceId,
                    productId = item.productId,
                    productName = item.productName,
                    description = item.description,
                    hsnSacCode = item.hsnSacCode,
                    quantity = item.quantity,
                    unit = item.unit,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    isDiscountPercent = item.isDiscountPercent,
                    isTaxInclusive = item.isTaxInclusive,
                    gstRate = item.gstRate,
                    taxableAmount = calc.taxableAmount,
                    cgstRate = calc.cgstRate,
                    cgstAmount = calc.cgstAmount,
                    sgstRate = calc.sgstRate,
                    sgstAmount = calc.sgstAmount,
                    igstRate = calc.igstRate,
                    igstAmount = calc.igstAmount,
                    cessRate = calc.cessRate,
                    cessAmount = calc.cessAmount,
                    totalAmount = calc.totalAmount
                )
            }

            val savedId = repository.saveFullInvoice(invoice, entityItems)
            if (state.amountPaid > 0 && state.invoiceId == 0L) {
                repository.recordPayment(
                    savedId,
                    PaymentRecord(
                        invoiceId = savedId,
                        amount = state.amountPaid,
                        paymentMethod = state.paymentMethod.name,
                        notes = "Initial payment upon invoice creation"
                    )
                )
            }
            onSuccess(savedId)
        }
    }

    suspend fun getInvoiceWithDetails(invoiceId: Long): InvoiceWithDetails? {
        return repository.getInvoiceWithDetails(invoiceId)
    }

    fun recordPayment(invoiceId: Long, amount: Double, method: PaymentMethod, ref: String, notes: String) {
        viewModelScope.launch {
            repository.recordPayment(
                invoiceId,
                PaymentRecord(
                    invoiceId = invoiceId,
                    amount = amount,
                    paymentMethod = method.name,
                    transactionRef = ref,
                    notes = notes
                )
            )
        }
    }

    fun cancelInvoice(invoiceId: Long) {
        viewModelScope.launch {
            repository.cancelInvoice(invoiceId)
        }
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch {
            repository.deleteFullInvoice(invoiceId)
        }
    }

    // --- Customer Management ---
    fun saveCustomer(customer: Customer, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (customer.id == 0L) {
                repository.insertCustomer(customer)
            } else {
                repository.updateCustomer(customer)
            }
            onDone()
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
        }
    }

    // --- Product Management ---
    fun saveProduct(product: Product, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
            onDone()
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }

    // --- Business Profile Management ---
    fun saveBusinessProfile(profile: BusinessProfile, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveBusinessProfile(profile)
            onDone()
        }
    }
}
