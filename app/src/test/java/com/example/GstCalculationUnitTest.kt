package com.example

import com.example.model.GstCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class GstCalculationUnitTest {

    @Test
    fun testIntraStateGstCalculation_Exclusive() {
        // Qty: 2, UnitPrice: 1000, 18% GST -> 9% CGST, 9% SGST
        val item = GstCalculationEngine.calculateLineItem(
            quantity = 2.0,
            unitPrice = 1000.0,
            discount = 0.0,
            isDiscountPercent = false,
            isTaxInclusive = false,
            gstRate = 18.0,
            isInterState = false
        )

        assertEquals(2000.0, item.taxableAmount, 0.01)
        assertEquals(9.0, item.cgstRate, 0.01)
        assertEquals(180.0, item.cgstAmount, 0.01)
        assertEquals(9.0, item.sgstRate, 0.01)
        assertEquals(180.0, item.sgstAmount, 0.01)
        assertEquals(0.0, item.igstAmount, 0.01)
        assertEquals(2360.0, item.totalAmount, 0.01)
    }

    @Test
    fun testInterStateGstCalculation_Exclusive() {
        // Interstate supply: full 18% IGST
        val item = GstCalculationEngine.calculateLineItem(
            quantity = 1.0,
            unitPrice = 5000.0,
            discount = 500.0,
            isDiscountPercent = false,
            isTaxInclusive = false,
            gstRate = 18.0,
            isInterState = true
        )

        assertEquals(4500.0, item.taxableAmount, 0.01)
        assertEquals(0.0, item.cgstAmount, 0.01)
        assertEquals(0.0, item.sgstAmount, 0.01)
        assertEquals(18.0, item.igstRate, 0.01)
        assertEquals(810.0, item.igstAmount, 0.01)
        assertEquals(5310.0, item.totalAmount, 0.01)
    }

    @Test
    fun testTaxInclusiveCalculation() {
        // Net: 1180 inclusive of 18% GST -> Taxable = 1000, Tax = 180
        val item = GstCalculationEngine.calculateLineItem(
            quantity = 1.0,
            unitPrice = 1180.0,
            discount = 0.0,
            isDiscountPercent = false,
            isTaxInclusive = true,
            gstRate = 18.0,
            isInterState = false
        )

        assertEquals(1000.0, item.taxableAmount, 0.01)
        assertEquals(90.0, item.cgstAmount, 0.01)
        assertEquals(90.0, item.sgstAmount, 0.01)
        assertEquals(1180.0, item.totalAmount, 0.01)
    }

    @Test
    fun testNumberToIndianWords() {
        assertEquals("Rupees Twelve Thousand Five Hundred Only", GstCalculationEngine.convertToIndianWords(12500L))
        assertEquals("Rupees One Lakh Only", GstCalculationEngine.convertToIndianWords(100000L))
        assertEquals("Rupees Two Crore Fifty Lakh Only", GstCalculationEngine.convertToIndianWords(25000000L))
        assertEquals("Rupees Zero Only", GstCalculationEngine.convertToIndianWords(0L))
    }

    @Test
    fun testInvoiceFullCalculationAndRounding() {
        val item = GstCalculationEngine.calculateLineItem(
            quantity = 3.0,
            unitPrice = 333.33,
            discount = 0.0,
            isDiscountPercent = false,
            isTaxInclusive = false,
            gstRate = 5.0,
            isInterState = false
        )

        val result = GstCalculationEngine.calculateInvoice(
            items = listOf(item),
            additionalCharges = 0.0,
            amountPaid = 500.0
        )

        // Verifies rounding and balance
        assertEquals(result.grandTotal - 500.0, result.balanceDue, 0.01)
    }
}
