package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SharedTextParserTest {

    @Test
    fun `parses indonesian QRIS receipt and ignores admin fee and balance lines`() {
        val receipt = """
            Transaksi Berhasil
            Kepada: Kopi Kenangan Grand Indonesia
            QRIS GoPay
            Nominal Rp 75.000,00
            Biaya Admin Rp 2.000
            Total Bayar Rp 77.000
            Saldo Rp 4.500.000
        """.trimIndent()

        val draft = SharedTextParser.parse(
            rawText = receipt,
            userCurrency = AppCurrency.IDR
        )

        assertEquals(SharedCaptureTarget.EXPENSE, draft.target)
        assertEquals(77_000_00L, draft.amountCents)
        assertEquals(AppCurrency.IDR, draft.detectedCurrency)
        assertFalse(draft.wasConvertedCurrency)
        assertEquals(ExpenseCategory.FOOD, draft.suggestedCategory)
        assertEquals(WalletType.EWALLET, draft.suggestedWallet)
        assertEquals("Kopi Kenangan Grand Indonesia", draft.transactionNote)
        assertTrue(draft.noteContent.contains("Kopi Kenangan"))
    }

    @Test
    fun `parses Rp decimal comma and IDR formats accurately`() {
        val commaDecimal = SharedTextParser.parse(
            rawText = "GoFood Warung Mbak Yanti Rp85.000,00",
            userCurrency = AppCurrency.IDR
        )
        assertEquals(85_000_00L, commaDecimal.amountCents)
        assertEquals(ExpenseCategory.FOOD, commaDecimal.suggestedCategory)

        val idrCommaThousands = SharedTextParser.parse(
            rawText = "Tokopedia Pembayaran IDR 1,250,000 via BCA",
            userCurrency = AppCurrency.IDR
        )
        assertEquals(1_250_000_00L, idrCommaThousands.amountCents)
        assertEquals(ExpenseCategory.SHOPPING, idrCommaThousands.suggestedCategory)
        assertEquals(WalletType.BANK, idrCommaThousands.suggestedWallet)
    }

    @Test
    fun `converts foreign currency receipt using offline rates and preserves original in note`() {
        val draft = SharedTextParser.parse(
            rawText = "GrabCar Singapore Trip\nPaid S$ 10.00 via Visa",
            userCurrency = AppCurrency.IDR,
            customRatesVsUsd = mapOf("USD" to 1.0, "SGD" to 1.34, "IDR" to 15_800.0)
        )

        val expectedMajorIdr = Math.round((10.0 / 1.34) * 15_800.0)
        assertEquals(SharedCaptureTarget.EXPENSE, draft.target)
        assertEquals(expectedMajorIdr * 100L, draft.amountCents)
        assertEquals(AppCurrency.SGD, draft.detectedCurrency)
        assertTrue(draft.wasConvertedCurrency)
        assertEquals(ExpenseCategory.TRANSPORT, draft.suggestedCategory)
        assertEquals(WalletType.CARD, draft.suggestedWallet)
        assertTrue(draft.transactionNote.contains("S$10.00"))
    }

    @Test
    fun `classifies salary and refund receipts as income`() {
        val salary = SharedTextParser.parse(
            rawText = "Gaji bulanan diterima Rp 12.000.000 via BCA",
            userCurrency = AppCurrency.IDR
        )
        assertEquals(SharedCaptureTarget.INCOME, salary.target)
        assertEquals(12_000_000_00L, salary.amountCents)
        assertEquals(ExpenseCategory.SALARY, salary.suggestedCategory)
        assertEquals(WalletType.BANK, salary.suggestedWallet)

        val refund = SharedTextParser.parse(
            rawText = "Pengembalian dana Refund Tokopedia Rp 45.000",
            userCurrency = AppCurrency.IDR
        )
        assertEquals(SharedCaptureTarget.INCOME, refund.target)
        assertEquals(45_000_00L, refund.amountCents)
    }

    @Test
    fun `routes urls, tasks, and compact codes without hallucinating amounts`() {
        val urlDraft = SharedTextParser.parse(
            rawText = "https://tokopedia.link/desk12345",
            rawSubject = "Ergonomic Desk Lamp"
        )
        assertEquals(SharedCaptureTarget.NOTE, urlDraft.target)
        assertNull(urlDraft.amountCents)
        assertEquals("Ergonomic Desk Lamp", urlDraft.noteTitle)
        assertEquals("https://tokopedia.link/desk12345", urlDraft.noteContent)
        assertEquals("Ergonomic Desk Lamp", urlDraft.snippetLabel)
        assertEquals("https://tokopedia.link/desk12345", urlDraft.snippetValue)

        val taskDraft = SharedTextParser.parse("TODO: Renew passport photos")
        assertEquals(SharedCaptureTarget.TASK, taskDraft.target)
        assertEquals("Renew passport photos", taskDraft.taskTitle)

        val snippetDraft = SharedTextParser.parse(
            rawText = "887081234567890",
            rawSubject = "VA Tagihan Internet"
        )
        assertEquals(SharedCaptureTarget.SNIPPET, snippetDraft.target)
        assertEquals("VA Tagihan Internet", snippetDraft.snippetLabel)
        assertEquals("887081234567890", snippetDraft.snippetValue)
    }

    @Test
    fun `countdown and life admin helpers filter upcoming dates and compute renewals`() {
        val today = LocalDate.of(2026, 11, 27).toEpochDay()
        val countdowns = listOf(
            CountdownItem(id = 1L, title = "Past", targetEpochDay = today - 1L, createdAtMillis = 1L),
            CountdownItem(id = 2L, title = "In 5 days", targetEpochDay = today + 5L, createdAtMillis = 1L),
            CountdownItem(id = 3L, title = "Today", targetEpochDay = today, createdAtMillis = 1L),
            CountdownItem(id = 4L, title = "Too far", targetEpochDay = today + 10L, createdAtMillis = 1L)
        )
        val upcoming = CountdownCalculations.upcomingCountdowns(countdowns, today, withinDays = 7)
        assertEquals(listOf(3L, 2L), upcoming.map { it.id })

        val expiredLastMonth = LocalDate.of(2026, 10, 15).toEpochDay()
        val renewedFromLastMonth = LifeAdminCalculations.renewedExpiryEpochDay(expiredLastMonth, today)
        assertEquals(LocalDate.of(2027, 10, 15).toEpochDay(), renewedFromLastMonth)

        val expiredLongAgo = LocalDate.of(2024, 10, 15).toEpochDay()
        val renewedIntoFuture = LifeAdminCalculations.renewedExpiryEpochDay(expiredLongAgo, today)
        assertEquals(LocalDate.of(2027, 10, 15).toEpochDay(), renewedIntoFuture)

        assertEquals(
            ExpenseCategory.TRANSPORT,
            LifeAdminCalculations.mapMaintenanceExpenseCategory(MaintenanceCategory.MOTORCYCLE)
        )
        assertEquals(
            ExpenseCategory.HOUSING,
            LifeAdminCalculations.mapMaintenanceExpenseCategory(MaintenanceCategory.APPLIANCE)
        )

        val prev = MaintenanceLog(
            id = 9L,
            assetName = "Honda PCX",
            category = MaintenanceCategory.MOTORCYCLE,
            serviceDescription = "Oil Change",
            serviceEpochDay = today - 90L,
            odometerKm = 12_000,
            costCents = 150_000_00L,
            nextDueEpochDay = today,
            nextDueOdometerKm = 15_000,
            note = "Synthetic"
        )
        val repeat = LifeAdminCalculations.buildRepeatMaintenanceTemplate(prev, today)
        assertEquals(0L, repeat.id)
        assertEquals(today, repeat.serviceEpochDay)
        assertEquals(today + 90L, repeat.nextDueEpochDay)
        assertEquals(15_000, repeat.odometerKm)
        assertEquals(18_000, repeat.nextDueOdometerKm)
        assertEquals(150_000_00L, repeat.costCents)
    }

    @Test
    fun `merges two-column OCR bounding boxes and ignores subtotal or thermal change`() {
        val twoColumnOcr = OcrReceiptLineMerger.mergeVisualRows(
            listOf(
                OcrTextLine("Transaksi Berhasil", left = 40, top = 20, right = 340, bottom = 50),
                OcrTextLine("Penerima", left = 40, top = 80, right = 180, bottom = 108),
                OcrTextLine("WAROENG STEAK & SHAKE", left = 320, top = 82, right = 680, bottom = 110),
                OcrTextLine("Sub Total", left = 40, top = 140, right = 180, bottom = 168),
                OcrTextLine("Rp 75.000", left = 520, top = 142, right = 680, bottom = 170),
                OcrTextLine("Biaya Admin", left = 40, top = 190, right = 210, bottom = 218),
                OcrTextLine("Rp 2.000", left = 530, top = 191, right = 680, bottom = 219),
                OcrTextLine("Total Pembayaran", left = 40, top = 240, right = 260, bottom = 270),
                OcrTextLine("Rp 82.000", left = 515, top = 242, right = 680, bottom = 272),
                OcrTextLine("Livin' by Mandiri", left = 40, top = 310, right = 290, bottom = 338)
            )
        )
        assertTrue(twoColumnOcr.contains("Penerima : WAROENG STEAK & SHAKE"))
        assertTrue(twoColumnOcr.contains("Total Pembayaran : Rp 82.000"))

        val mergedDraft = SharedTextParser.parse(
            rawText = twoColumnOcr,
            userCurrency = AppCurrency.IDR,
            isReceiptMedia = true
        )
        assertEquals(SharedCaptureTarget.EXPENSE, mergedDraft.target)
        assertEquals(82_000_00L, mergedDraft.amountCents)
        assertEquals(WalletType.BANK, mergedDraft.suggestedWallet)
        assertEquals("WAROENG STEAK & SHAKE", mergedDraft.transactionNote)

        val thermalStruk = """
            INDOMARET SUDIRMAN
            ROTI COKLAT       14,000
            KOPI SUSU         14,000
            SUB TOTAL : 28,000
            TOTAL : 28,000
            TUNAI : 50,000
            KEMBALI : 22,000
        """.trimIndent()
        val thermalDraft = SharedTextParser.parse(
            rawText = thermalStruk,
            userCurrency = AppCurrency.IDR,
            isReceiptMedia = true
        )
        assertEquals(SharedCaptureTarget.EXPENSE, thermalDraft.target)
        assertEquals(28_000_00L, thermalDraft.amountCents)
        assertEquals(ExpenseCategory.GROCERIES, thermalDraft.suggestedCategory)

        val unreadableImageDraft = SharedTextParser.parse(
            rawText = "",
            userCurrency = AppCurrency.IDR,
            isReceiptMedia = true
        )
        assertEquals(SharedCaptureTarget.EXPENSE, unreadableImageDraft.target)
        assertNull(unreadableImageDraft.amountCents)
    }
}
