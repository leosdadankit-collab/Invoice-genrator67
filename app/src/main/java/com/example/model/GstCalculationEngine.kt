package com.example.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

data class CalculatedItemResult(
    val grossAmount: Double,
    val discountAmount: Double,
    val taxableAmount: Double,
    val cgstRate: Double,
    val cgstAmount: Double,
    val sgstRate: Double,
    val sgstAmount: Double,
    val igstRate: Double,
    val igstAmount: Double,
    val cessRate: Double,
    val cessAmount: Double,
    val totalAmount: Double
)

data class InvoiceCalculationResult(
    val subTotal: Double,
    val totalDiscount: Double,
    val taxableAmount: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val igstAmount: Double,
    val cessAmount: Double,
    val additionalCharges: Double,
    val roundOff: Double,
    val grandTotal: Double,
    val balanceDue: Double,
    val paymentStatus: PaymentStatus,
    val amountInWords: String
)

object GstCalculationEngine {

    /**
     * Precision rounding helper to 2 decimal places.
     */
    fun round2(value: Double): Double {
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates line item tax and totals based on Indian GST laws.
     */
    fun calculateLineItem(
        quantity: Double,
        unitPrice: Double,
        discount: Double,
        isDiscountPercent: Boolean,
        isTaxInclusive: Boolean,
        gstRate: Double,
        cessRate: Double = 0.0,
        isInterState: Boolean = false,
        isUnregisteredSeller: Boolean = false
    ): CalculatedItemResult {
        val qty = if (quantity <= 0) 1.0 else quantity
        val rate = if (unitPrice < 0) 0.0 else unitPrice
        val gross = round2(qty * rate)

        val disc = if (isDiscountPercent) {
            val pct = discount.coerceIn(0.0, 100.0)
            round2(gross * (pct / 100.0))
        } else {
            discount.coerceIn(0.0, gross)
        }

        val netAmount = round2(gross - disc)

        // If seller is unregistered or composition, GST is zero
        val effectiveGstRate = if (isUnregisteredSeller) 0.0 else gstRate
        val effectiveCessRate = if (isUnregisteredSeller) 0.0 else cessRate

        val taxableAmount: Double
        val totalTaxRate = effectiveGstRate + effectiveCessRate

        if (isTaxInclusive && totalTaxRate > 0) {
            // Price is inclusive of tax: Taxable = Net / (1 + Rate/100)
            val factor = 1.0 + (totalTaxRate / 100.0)
            taxableAmount = round2(netAmount / factor)
        } else {
            // Price is exclusive of tax
            taxableAmount = netAmount
        }

        val cgstRate: Double
        val cgstAmount: Double
        val sgstRate: Double
        val sgstAmount: Double
        val igstRate: Double
        val igstAmount: Double

        if (effectiveGstRate > 0) {
            if (isInterState) {
                igstRate = effectiveGstRate
                igstAmount = round2(taxableAmount * (igstRate / 100.0))
                cgstRate = 0.0
                cgstAmount = 0.0
                sgstRate = 0.0
                sgstAmount = 0.0
            } else {
                cgstRate = round2(effectiveGstRate / 2.0)
                cgstAmount = round2(taxableAmount * (cgstRate / 100.0))
                sgstRate = round2(effectiveGstRate / 2.0)
                sgstAmount = round2(taxableAmount * (sgstRate / 100.0))
                igstRate = 0.0
                igstAmount = 0.0
            }
        } else {
            cgstRate = 0.0
            cgstAmount = 0.0
            sgstRate = 0.0
            sgstAmount = 0.0
            igstRate = 0.0
            igstAmount = 0.0
        }

        val cessAmount = if (effectiveCessRate > 0) {
            round2(taxableAmount * (effectiveCessRate / 100.0))
        } else {
            0.0
        }

        val totalAmount = if (isTaxInclusive) {
            netAmount
        } else {
            round2(taxableAmount + cgstAmount + sgstAmount + igstAmount + cessAmount)
        }

        return CalculatedItemResult(
            grossAmount = gross,
            discountAmount = disc,
            taxableAmount = taxableAmount,
            cgstRate = cgstRate,
            cgstAmount = cgstAmount,
            sgstRate = sgstRate,
            sgstAmount = sgstAmount,
            igstRate = igstRate,
            igstAmount = igstAmount,
            cessRate = effectiveCessRate,
            cessAmount = cessAmount,
            totalAmount = totalAmount
        )
    }

    /**
     * Computes the full invoice summary from line item results and invoice-level parameters.
     */
    fun calculateInvoice(
        items: List<CalculatedItemResult>,
        additionalCharges: Double = 0.0,
        amountPaid: Double = 0.0,
        isCancelled: Boolean = false,
        dueDate: Long = System.currentTimeMillis()
    ): InvoiceCalculationResult {
        var subTotal = 0.0
        var totalDiscount = 0.0
        var taxableAmount = 0.0
        var cgstAmount = 0.0
        var sgstAmount = 0.0
        var igstAmount = 0.0
        var cessAmount = 0.0

        for (item in items) {
            subTotal += item.grossAmount
            totalDiscount += item.discountAmount
            taxableAmount += item.taxableAmount
            cgstAmount += item.cgstAmount
            sgstAmount += item.sgstAmount
            igstAmount += item.igstAmount
            cessAmount += item.cessAmount
        }

        subTotal = round2(subTotal)
        totalDiscount = round2(totalDiscount)
        taxableAmount = round2(taxableAmount)
        cgstAmount = round2(cgstAmount)
        sgstAmount = round2(sgstAmount)
        igstAmount = round2(igstAmount)
        cessAmount = round2(cessAmount)

        val validCharges = round2(additionalCharges.coerceAtLeast(0.0))
        val rawTotal = round2(taxableAmount + cgstAmount + sgstAmount + igstAmount + cessAmount + validCharges)

        // Indian Round-Off to nearest whole Rupee
        val roundedGrandTotal = Math.round(rawTotal).toDouble()
        val roundOff = round2(roundedGrandTotal - rawTotal)
        val grandTotal = roundedGrandTotal

        val validPaid = round2(amountPaid.coerceAtLeast(0.0))
        val balanceDue = round2((grandTotal - validPaid).coerceAtLeast(0.0))

        val now = System.currentTimeMillis()
        val paymentStatus = when {
            isCancelled -> PaymentStatus.CANCELLED
            validPaid >= grandTotal && grandTotal > 0.0 -> PaymentStatus.PAID
            validPaid > 0.0 -> PaymentStatus.PARTIAL
            now > dueDate && dueDate > 0 -> PaymentStatus.OVERDUE
            else -> PaymentStatus.UNPAID
        }

        val amountInWords = convertToIndianWords(grandTotal.toLong())

        return InvoiceCalculationResult(
            subTotal = subTotal,
            totalDiscount = totalDiscount,
            taxableAmount = taxableAmount,
            cgstAmount = cgstAmount,
            sgstAmount = sgstAmount,
            igstAmount = igstAmount,
            cessAmount = cessAmount,
            additionalCharges = validCharges,
            roundOff = roundOff,
            grandTotal = grandTotal,
            balanceDue = balanceDue,
            paymentStatus = paymentStatus,
            amountInWords = amountInWords
        )
    }

    /**
     * Converts a numeric rupee amount to standard Indian numbering words (Crores, Lakhs, Thousands, Hundreds).
     * Example: 12500 -> "Rupees Twelve Thousand Five Hundred Only"
     */
    fun convertToIndianWords(amount: Long): String {
        if (amount == 0L) return "Rupees Zero Only"
        if (amount < 0L) return "Negative " + convertToIndianWords(-amount)

        val units = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun convertLessThanThousand(n: Int): String {
            if (n == 0) return ""
            var str = ""
            if (n >= 100) {
                str += units[n / 100] + " Hundred "
            }
            val rem = n % 100
            if (rem > 0) {
                if (rem < 20) {
                    str += units[rem] + " "
                } else {
                    str += tens[rem / 10] + " "
                    if (rem % 10 > 0) {
                        str += units[rem % 10] + " "
                    }
                }
            }
            return str.trim()
        }

        var num = amount
        var result = ""

        // Crores (1,00,00,000)
        val crores = num / 10000000L
        if (crores > 0) {
            result += convertLessThanThousand(crores.toInt()) + " Crore "
            num %= 10000000L
        }

        // Lakhs (1,00,000)
        val lakhs = num / 100000L
        if (lakhs > 0) {
            result += convertLessThanThousand(lakhs.toInt()) + " Lakh "
            num %= 100000L
        }

        // Thousands (1,000)
        val thousands = num / 1000L
        if (thousands > 0) {
            result += convertLessThanThousand(thousands.toInt()) + " Thousand "
            num %= 1000L
        }

        // Hundreds and tens
        if (num > 0) {
            result += convertLessThanThousand(num.toInt()) + " "
        }

        return "Rupees " + result.trim() + " Only"
    }

    /**
     * Generates Indian Financial Year string (e.g. 2026-27).
     * Fiscal year starts April 1st and ends March 31st.
     */
    fun getFinancialYear(timestamp: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0-based: 3 is April

        return if (month >= Calendar.APRIL) {
            val nextYear = (year + 1) % 100
            "$year-${String.format("%02d", nextYear)}"
        } else {
            val prevYear = year - 1
            val curYearShort = year % 100
            "$prevYear-${String.format("%02d", curYearShort)}"
        }
    }

    /**
     * Generates standard Indian invoice number sequence: INV/2026-27/0001
     */
    fun formatInvoiceNumber(prefix: String, sequenceNumber: Long, timestamp: Long = System.currentTimeMillis()): String {
        val fy = getFinancialYear(timestamp)
        val formattedSeq = String.format("%04d", sequenceNumber)
        return "$prefix/$fy/$formattedSeq"
    }
}
