package com.example.model

/**
 * All 36 States and Union Territories of India with official 2-digit GST state codes.
 */
data class IndianState(
    val code: String,
    val name: String
)

object IndianStates {
    val ALL = listOf(
        IndianState("01", "Jammu and Kashmir"),
        IndianState("02", "Himachal Pradesh"),
        IndianState("03", "Punjab"),
        IndianState("04", "Chandigarh"),
        IndianState("05", "Uttarakhand"),
        IndianState("06", "Haryana"),
        IndianState("07", "Delhi"),
        IndianState("08", "Rajasthan"),
        IndianState("09", "Uttar Pradesh"),
        IndianState("10", "Bihar"),
        IndianState("11", "Sikkim"),
        IndianState("12", "Arunachal Pradesh"),
        IndianState("13", "Nagaland"),
        IndianState("14", "Manipur"),
        IndianState("15", "Mizoram"),
        IndianState("16", "Tripura"),
        IndianState("17", "Meghalaya"),
        IndianState("18", "Assam"),
        IndianState("19", "West Bengal"),
        IndianState("20", "Jharkhand"),
        IndianState("21", "Odisha"),
        IndianState("22", "Chhattisgarh"),
        IndianState("23", "Madhya Pradesh"),
        IndianState("24", "Gujarat"),
        IndianState("25", "Daman and Diu"),
        IndianState("26", "Dadra and Nagar Haveli"),
        IndianState("27", "Maharashtra"),
        IndianState("28", "Andhra Pradesh (Old)"),
        IndianState("29", "Karnataka"),
        IndianState("30", "Goa"),
        IndianState("31", "Lakshadweep"),
        IndianState("32", "Kerala"),
        IndianState("33", "Tamil Nadu"),
        IndianState("34", "Puducherry"),
        IndianState("35", "Andaman and Nicobar Islands"),
        IndianState("36", "Telangana"),
        IndianState("37", "Andhra Pradesh (New)"),
        IndianState("38", "Ladakh"),
        IndianState("97", "Other Territory")
    )

    fun findByCode(code: String): IndianState? {
        val clean = code.trim().padStart(2, '0')
        return ALL.find { it.code == clean }
    }

    fun findByName(name: String): IndianState? {
        return ALL.find { it.name.equals(name.trim(), ignoreCase = true) }
    }

    /**
     * Extracts state code from a 15-character GSTIN.
     * Pattern: 2-digit state code + 10-char PAN + 1 entity num + 'Z' + 1 checksum
     */
    fun extractStateCodeFromGstin(gstin: String): String? {
        val clean = gstin.trim().uppercase()
        if (clean.length >= 2 && clean.substring(0, 2).all { it.isDigit() }) {
            return clean.substring(0, 2)
        }
        return null
    }

    /**
     * Validates 15-character Indian GSTIN format.
     */
    fun isValidGstin(gstin: String): Boolean {
        val regex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        return regex.matches(gstin.trim().uppercase())
    }

    /**
     * Validates 10-character Indian PAN format.
     */
    fun isValidPan(pan: String): Boolean {
        val regex = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")
        return regex.matches(pan.trim().uppercase())
    }
}

enum class BusinessRegistrationType(val label: String) {
    GST_REGISTERED("Regular GST Registered"),
    COMPOSITION("Composition Taxpayer"),
    UNREGISTERED("Unregistered Business")
}

enum class DocumentType(val label: String, val prefix: String) {
    TAX_INVOICE("Tax Invoice", "INV"),
    BILL_OF_SUPPLY("Bill of Supply", "BOS"),
    PROFORMA("Proforma Invoice", "PRO"),
    QUOTATION("Quotation / Estimate", "EST"),
    DELIVERY_CHALLAN("Delivery Challan", "DC")
}

enum class PaymentStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partially Paid"),
    UNPAID("Unpaid"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled")
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI / QR"),
    BANK_TRANSFER("NEFT / RTGS / IMPS"),
    CHEQUE("Cheque"),
    CARD("Debit / Credit Card")
}

enum class InvoiceTemplateType(val label: String) {
    CLASSIC("Classic Professional (A4)"),
    MODERN("Modern FinTech (A4)"),
    GST_DETAILED("GST Detailed (A4)"),
    THERMAL_RECEIPT("Thermal POS Bill (3-Inch)")
}

object StandardUnits {
    val ALL = listOf(
        "PCS", "NOS", "KG", "GM", "LTR", "ML", "MTR", "CM",
        "BOX", "PKT", "DOZ", "SET", "PAIR", "BAG", "ROLL",
        "SQF", "SQM", "HRS", "DAY", "JOB"
    )
}

object StandardGstRates {
    val ALL = listOf(0.0, 0.1, 0.25, 3.0, 5.0, 12.0, 18.0, 28.0)
}
