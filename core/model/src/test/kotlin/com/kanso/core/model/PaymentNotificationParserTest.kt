package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationParserTest {

    @Test
    fun `parses BCA QRIS coffee payment as Bank Food expense`() {
        val alert = PaymentNotificationParser.parse(
            packageName = "com.bca",
            appLabel = "BCA mobile",
            title = "Pembayaran QRIS berhasil",
            text = "Pembayaran QRIS Rp45.000 ke KOPI KENANGAN berhasil",
            userCurrency = AppCurrency.IDR
        )

        assertNotNull(alert)
        alert!!
        assertEquals(TransactionType.EXPENSE, alert.type)
        assertEquals(4_500_000L, alert.amountCents)
        assertEquals(AppCurrency.IDR, alert.detectedCurrency)
        assertFalse(alert.wasConvertedCurrency)
        assertEquals(ExpenseCategory.FOOD, alert.category)
        assertEquals(WalletType.BANK, alert.wallet)
        assertEquals("Kopi Kenangan", alert.note)
    }

    @Test
    fun `parses GoPay and OVO completed payments as E-Wallet expenses`() {
        val gopay = PaymentNotificationParser.parse(
            packageName = "com.gojek.gopay",
            appLabel = "GoPay",
            title = "GoPay",
            text = "Pembayaran Rp 32.500 di Indomaret Point berhasil",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(gopay)
        gopay!!
        assertEquals(TransactionType.EXPENSE, gopay.type)
        assertEquals(3_250_000L, gopay.amountCents)
        assertEquals(ExpenseCategory.GROCERIES, gopay.category)
        assertEquals(WalletType.EWALLET, gopay.wallet)
        assertEquals("Indomaret Point", gopay.note)

        val ovo = PaymentNotificationParser.parse(
            packageName = "ovo.id",
            appLabel = "OVO",
            title = "Transaksi Berhasil",
            text = "Berhasil bayar Rp 28.000 ke GoFood",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(ovo)
        ovo!!
        assertEquals(2_800_000L, ovo.amountCents)
        assertEquals(ExpenseCategory.FOOD, ovo.category)
        assertEquals(WalletType.EWALLET, ovo.wallet)
    }

    @Test
    fun `parses Google Wallet USD spend and converts to IDR`() {
        val alert = PaymentNotificationParser.parse(
            packageName = "com.google.android.apps.walletnfcrel",
            appLabel = "Google Wallet",
            title = "Google Wallet",
            text = "You spent $12.50 at Netflix",
            userCurrency = AppCurrency.IDR,
            customRatesVsUsd = mapOf("IDR" to 16_000.0)
        )

        assertNotNull(alert)
        alert!!
        assertEquals(TransactionType.EXPENSE, alert.type)
        assertEquals(20_000_000L, alert.amountCents)
        assertEquals(AppCurrency.USD, alert.detectedCurrency)
        assertTrue(alert.wasConvertedCurrency)
        assertEquals(ExpenseCategory.ENTERTAINMENT, alert.category)
        assertEquals(WalletType.CARD, alert.wallet)
        assertTrue(alert.note.startsWith("Netflix"))
    }

    @Test
    fun `parses incoming bank transfer and salary as Income`() {
        val transferIn = PaymentNotificationParser.parse(
            packageName = "com.jago.digitalBanking",
            appLabel = "Bank Jago",
            title = "Uang masuk!",
            text = "Transfer masuk Rp 5.000.000 dari PT Maju Jaya berhasil",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(transferIn)
        transferIn!!
        assertEquals(TransactionType.INCOME, transferIn.type)
        assertEquals(500_000_000L, transferIn.amountCents)
        assertEquals(WalletType.BANK, transferIn.wallet)
        assertEquals("PT Maju Jaya", transferIn.note)

        val salary = PaymentNotificationParser.parse(
            packageName = "id.bmri.livin",
            appLabel = "Livin' by Mandiri",
            title = "Dana Masuk",
            text = "Pengkreditan gaji Rp 12.000.000 berhasil",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(salary)
        salary!!
        assertEquals(TransactionType.INCOME, salary.type)
        assertEquals(ExpenseCategory.SALARY, salary.category)
        assertEquals(WalletType.BANK, salary.wallet)
    }

    @Test
    fun `ignores promos OTPs pending unpaid and failed notifications`() {
        val promo = PaymentNotificationParser.parse(
            packageName = "com.gojek.gopay",
            title = "Promo Spesial!",
            text = "Klaim voucher cashback s/d Rp 50.000 hari ini berhasil!"
        )
        assertNull(promo)

        val loan = PaymentNotificationParser.parse(
            packageName = "id.dana",
            title = "Dana Cicil",
            text = "Limit hingga Rp 10.000.000 ajukan sekarang berhasil"
        )
        assertNull(loan)

        val otp = PaymentNotificationParser.parse(
            packageName = "com.bca",
            title = "Kode Verifikasi",
            text = "OTP 123456 untuk transaksi Rp 200.000 berhasil dikirim"
        )
        assertNull(otp)

        val pending = PaymentNotificationParser.parse(
            packageName = "com.shopee.id",
            title = "Menunggu Pembayaran",
            text = "Selesaikan pembayaran Rp 145.000 sebelum jam 22:00"
        )
        assertNull(pending)

        val failed = PaymentNotificationParser.parse(
            packageName = "id.dana",
            title = "Transaksi Gagal",
            text = "Pembayaran Rp 75.000 ke Kopi Kenangan gagal diproses"
        )
        assertNull(failed)
    }

    @Test
    fun `respects package whitelist and self exclusions`() {
        val allowed = KnownFinanceApps.defaultPackages
        assertFalse(
            PaymentNotificationParser.shouldInspectPackage(
                packageName = "com.kanso.app",
                selfPackageName = "com.kanso.app",
                allowAllApps = true,
                allowedPackages = allowed
            )
        )
        assertFalse(
            PaymentNotificationParser.shouldInspectPackage(
                packageName = "com.android.systemui",
                selfPackageName = "com.kanso.app",
                allowAllApps = true,
                allowedPackages = allowed
            )
        )
        assertTrue(
            PaymentNotificationParser.shouldInspectPackage(
                packageName = "com.bca",
                selfPackageName = "com.kanso.app",
                allowAllApps = false,
                allowedPackages = allowed
            )
        )
        assertFalse(
            PaymentNotificationParser.shouldInspectPackage(
                packageName = "com.whatsapp",
                selfPackageName = "com.kanso.app",
                allowAllApps = false,
                allowedPackages = allowed
            )
        )
        assertTrue(
            PaymentNotificationParser.shouldInspectPackage(
                packageName = "com.whatsapp",
                selfPackageName = "com.kanso.app",
                allowAllApps = true,
                allowedPackages = allowed
            )
        )
    }

    @Test
    fun `deduplicator blocks identical alerts within 90s window and allows later`() {
        val deduplicator = PaymentAlertDeduplicator(windowMillis = 90_000L)
        val fp = "com.bca|EXPENSE|4500000|kopi kenangan"

        assertTrue(deduplicator.shouldProcess(fp, 1_000L))
        assertFalse(deduplicator.shouldProcess(fp, 30_000L))
        assertFalse(deduplicator.shouldProcess(fp, 90_999L))
        assertTrue(deduplicator.shouldProcess(fp, 91_000L))
    }
}
