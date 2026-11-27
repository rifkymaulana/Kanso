package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitBillCalculationsTest {

    @Test
    fun `equal split in IDR divides unevenly on 100-cent steps and preserves exact total`() {
        val participants = listOf(
            SplitParticipantInput(id = 0L, name = "Me", isPayer = true),
            SplitParticipantInput(id = 1L, name = "Budi", isPayer = false),
            SplitParticipantInput(id = 2L, name = "Sari", isPayer = false)
        )

        val result = SplitBillCalculations.calculate(
            mode = SplitBillMode.EQUAL,
            manualSubtotalCents = 100_000_00L, // Rp 100.000
            servicePercent = 0.0,
            taxPercent = 0.0,
            discountCents = 0L,
            participants = participants,
            currency = AppCurrency.IDR
        )

        assertEquals(100_000_00L, result.grandTotalCents)
        // Payer absorbs the 1 Rp remainder -> 33.334, friends stay equal at 33.333 each
        assertEquals(33_334_00L, result.shares[0].finalShareCents)
        assertEquals(33_333_00L, result.shares[1].finalShareCents)
        assertEquals(33_333_00L, result.shares[2].finalShareCents)
        assertEquals(result.grandTotalCents, result.shares.sumOf { it.finalShareCents })
        assertEquals(33_334_00L, result.payerShareCents)
        assertEquals(66_666_00L, result.friendsTotalCents)
    }

    @Test
    fun `equal split in USD distributes remainder in single cents`() {
        val participants = listOf(
            SplitParticipantInput(id = 0L, name = "Me", isPayer = true),
            SplitParticipantInput(id = 1L, name = "Alex", isPayer = false),
            SplitParticipantInput(id = 2L, name = "Sam", isPayer = false)
        )

        val result = SplitBillCalculations.calculate(
            mode = SplitBillMode.EQUAL,
            manualSubtotalCents = 100_00L, // $100.00
            servicePercent = 0.0,
            taxPercent = 0.0,
            discountCents = 0L,
            participants = participants,
            currency = AppCurrency.USD
        )

        assertEquals(100_00L, result.grandTotalCents)
        assertEquals(33_34L, result.shares[0].finalShareCents)
        assertEquals(33_33L, result.shares[1].finalShareCents)
        assertEquals(33_33L, result.shares[2].finalShareCents)
        assertEquals(100_00L, result.shares.sumOf { it.finalShareCents })
    }

    @Test
    fun `service compound tax and discount calculate accurately with cap`() {
        val participants = listOf(
            SplitParticipantInput(id = 0L, name = "Me", isPayer = true),
            SplitParticipantInput(id = 1L, name = "Andi", isPayer = false)
        )

        val result = SplitBillCalculations.calculate(
            mode = SplitBillMode.EQUAL,
            manualSubtotalCents = 200_000_00L, // Rp 200.000
            servicePercent = 5.0,               // Rp 10.000
            taxPercent = 10.0,                  // 10% of 210.000 = Rp 21.000
            discountCents = 31_000_00L,         // - Rp 31.000 -> total 200.000
            participants = participants,
            currency = AppCurrency.IDR
        )

        assertEquals(200_000_00L, result.subtotalCents)
        assertEquals(10_000_00L, result.serviceCents)
        assertEquals(21_000_00L, result.taxCents)
        assertEquals(31_000_00L, result.discountCents)
        assertEquals(200_000_00L, result.grandTotalCents)
        assertEquals(100_000_00L, result.shares[0].finalShareCents)
        assertEquals(100_000_00L, result.shares[1].finalShareCents)
    }

    @Test
    fun `by item proportional split preserves exact grand total across uneven items`() {
        val participants = listOf(
            SplitParticipantInput(id = 0L, name = "Me", isPayer = true, itemAmountCents = 55_000_00L),
            SplitParticipantInput(id = 1L, name = "Budi", isPayer = false, itemAmountCents = 85_000_00L),
            SplitParticipantInput(id = 2L, name = "Dina", isPayer = false, itemAmountCents = 40_000_00L)
        )

        val result = SplitBillCalculations.calculate(
            mode = SplitBillMode.BY_ITEM,
            manualSubtotalCents = 0L,
            servicePercent = 5.0,
            taxPercent = 10.0,
            discountCents = 15_000_00L,
            participants = participants,
            currency = AppCurrency.IDR
        )

        assertEquals(180_000_00L, result.subtotalCents)
        assertEquals(9_000_00L, result.serviceCents)
        assertEquals(18_900_00L, result.taxCents)
        assertEquals(15_000_00L, result.discountCents)
        assertEquals(192_900_00L, result.grandTotalCents)
        assertEquals(result.grandTotalCents, result.shares.sumOf { it.finalShareCents })
        assertTrue(result.shares[1].finalShareCents > result.shares[0].finalShareCents)
        assertTrue(result.shares[0].finalShareCents > result.shares[2].finalShareCents)
    }

    @Test
    fun `formatShareSummary includes adjustments participants and optional payment snippet`() {
        val participants = listOf(
            SplitParticipantInput(id = 0L, name = "Me", isPayer = true),
            SplitParticipantInput(id = 1L, name = "Rani", isPayer = false)
        )
        val result = SplitBillCalculations.calculate(
            mode = SplitBillMode.EQUAL,
            manualSubtotalCents = 100_000_00L,
            servicePercent = 5.0,
            taxPercent = 10.0,
            discountCents = 0L,
            participants = participants,
            currency = AppCurrency.IDR
        )

        val plainSummary = SplitBillCalculations.formatShareSummary(
            title = "Ramen Night",
            dateLabel = "27 Nov",
            result = result,
            currency = AppCurrency.IDR,
            paymentSnippet = null
        )
        assertTrue(plainSummary.contains("Split Bill — Ramen Night (27 Nov)"))
        assertTrue(plainSummary.contains("+ Service (5%): Rp 5.000"))
        assertTrue(plainSummary.contains("+ Tax (10%): Rp 10.500"))
        assertTrue(plainSummary.contains("Total: Rp 115.500"))
        assertTrue(plainSummary.contains("• Me: Rp 57.750 (Paid)"))
        assertTrue(plainSummary.contains("• Rani: Rp 57.750"))
        assertFalse(plainSummary.contains("Payment —"))

        val withSnippet = SplitBillCalculations.formatShareSummary(
            title = "Ramen Night",
            dateLabel = "27 Nov",
            result = result,
            currency = AppCurrency.IDR,
            paymentSnippet = SnippetItem(
                id = 1L,
                label = "BCA",
                value = "1234567890",
                category = SnippetCategory.FINANCE,
                updatedAtMillis = 1L
            )
        )
        assertTrue(withSnippet.contains("Payment — BCA: 1234567890"))
    }
}
