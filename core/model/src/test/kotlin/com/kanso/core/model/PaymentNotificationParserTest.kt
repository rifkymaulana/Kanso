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

    @Test
    fun `parseWithReason returns specific skip reasons and matched alerts`() {
        val otpResult = PaymentNotificationParser.parseWithReason(
            packageName = "com.bca",
            title = "Kode Verifikasi",
            text = "OTP 123456 untuk transaksi Rp 200.000 berhasil dikirim"
        )
        assertEquals(PaymentParseResult.Skipped(PaymentParseSkipReason.OTP), otpResult)

        val pendingResult = PaymentNotificationParser.parseWithReason(
            packageName = "com.shopee.id",
            title = "Menunggu Pembayaran",
            text = "Selesaikan pembayaran Rp 145.000 sebelum jam 22:00"
        )
        assertEquals(PaymentParseResult.Skipped(PaymentParseSkipReason.PENDING_OR_FAILED), pendingResult)

        val promoResult = PaymentNotificationParser.parseWithReason(
            packageName = "com.gojek.gopay",
            title = "Promo Spesial!",
            text = "Klaim voucher cashback s/d Rp 50.000 hari ini berhasil!"
        )
        assertEquals(PaymentParseResult.Skipped(PaymentParseSkipReason.PROMO_OR_LOAN), promoResult)

        val noSignalResult = PaymentNotificationParser.parseWithReason(
            packageName = "com.bca",
            title = "Info Saldo",
            text = "Saldo rekening Anda Rp 2.500.000"
        )
        assertEquals(PaymentParseResult.Skipped(PaymentParseSkipReason.NO_TRANSACTION_SIGNAL), noSignalResult)

        val noAmountResult = PaymentNotificationParser.parseWithReason(
            packageName = "com.bca",
            title = "Pembayaran Berhasil",
            text = "Transaksi QRIS Anda berhasil diproses"
        )
        assertEquals(PaymentParseResult.Skipped(PaymentParseSkipReason.NO_CURRENCY_AMOUNT), noAmountResult)

        val matched = PaymentNotificationParser.parseWithReason(
            packageName = "com.bca",
            appLabel = "BCA mobile",
            title = "Pembayaran QRIS berhasil",
            text = "Pembayaran QRIS Rp45.000 ke KOPI KENANGAN berhasil"
        )
        assertTrue(matched is PaymentParseResult.Matched)
    }

    @Test
    fun `parses BRImo single-line BRIVA alert without picking remaining balance`() {
        val alert = PaymentNotificationParser.parse(
            packageName = "id.co.bri.brimo",
            appLabel = "BRImo",
            title = "Transaksi Berhasil",
            text = "Pembayaran BRIVA berhasil! Tagihan PLN Prepaid sebesar IDR 100,000.00 telah didebet dari rekening *********1234. Saldo Anda tersisa Rp 2.450.000.",
            userCurrency = AppCurrency.IDR
        )

        assertNotNull(alert)
        alert!!
        assertEquals(TransactionType.EXPENSE, alert.type)
        assertEquals(10_000_000L, alert.amountCents)
        assertEquals(ExpenseCategory.BILLS, alert.category)
        assertEquals(WalletType.BANK, alert.wallet)
        assertEquals("PLN Prepaid", alert.note)
    }

    @Test
    fun `parses BRImo and Livin Indonesian debit and credit phrasing with a_n_ recipients`() {
        val brimoCredit = PaymentNotificationParser.parse(
            packageName = "id.co.bri.brimo",
            appLabel = "BRImo",
            title = "BRImo",
            text = "Rekening Anda *********5678 bertambah Rp 500.000,00 (Transfer In) dari JOKO SUSILO pada 28/11/2026",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(brimoCredit)
        brimoCredit!!
        assertEquals(TransactionType.INCOME, brimoCredit.type)
        assertEquals(50_000_000L, brimoCredit.amountCents)
        assertEquals(WalletType.BANK, brimoCredit.wallet)
        assertEquals("Joko Susilo", brimoCredit.note)

        val livinCredit = PaymentNotificationParser.parse(
            packageName = "id.bmri.livin",
            appLabel = "Livin' by Mandiri",
            title = "Dana Masuk (CR)",
            text = "Diterima transfer sebesar Rp 750.000,00 dari PT MAJU JAYA",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(livinCredit)
        livinCredit!!
        assertEquals(TransactionType.INCOME, livinCredit.type)
        assertEquals(75_000_000L, livinCredit.amountCents)
        assertEquals(WalletType.BANK, livinCredit.wallet)
        assertEquals("PT Maju Jaya", livinCredit.note)

        val livinOutgoingAn = PaymentNotificationParser.parse(
            packageName = "id.bmri.livin",
            appLabel = "Livin' by Mandiri",
            title = "Transfer Berhasil",
            text = "Anda telah mentransfer IDR 1,500,000.00 ke BCA a.n. ANDI WIJAYA",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(livinOutgoingAn)
        livinOutgoingAn!!
        assertEquals(TransactionType.EXPENSE, livinOutgoingAn.type)
        assertEquals(150_000_000L, livinOutgoingAn.amountCents)
        assertEquals(WalletType.BANK, livinOutgoingAn.wallet)
        assertEquals("Andi Wijaya", livinOutgoingAn.note)

        val bcaAtasNama = PaymentNotificationParser.parse(
            packageName = "com.bca.mybca.omni.android",
            appLabel = "myBCA",
            title = "Transaksi Berhasil",
            text = "Transfer ke Rekening Mandiri 1370001234 atas nama SITI AMINAH sebesar Rp 250.000 berhasil",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(bcaAtasNama)
        bcaAtasNama!!
        assertEquals(TransactionType.EXPENSE, bcaAtasNama.type)
        assertEquals(25_000_000L, bcaAtasNama.amountCents)
        assertEquals("Siti Aminah", bcaAtasNama.note)
    }

    @Test
    fun `parses GoPay DANA ShopeePay Indonesian phrasing and skips wallet top ups`() {
        val gopayIncoming = PaymentNotificationParser.parse(
            packageName = "com.gojek.gopay",
            appLabel = "GoPay",
            title = "GoPay",
            text = "Kamu menerima Rp100.000 dari Budi Santoso",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(gopayIncoming)
        gopayIncoming!!
        assertEquals(TransactionType.INCOME, gopayIncoming.type)
        assertEquals(10_000_000L, gopayIncoming.amountCents)
        assertEquals(WalletType.EWALLET, gopayIncoming.wallet)
        assertEquals("Budi Santoso", gopayIncoming.note)

        val danaSend = PaymentNotificationParser.parse(
            packageName = "id.dana",
            appLabel = "DANA",
            title = "Kirim Uang",
            text = "Kamu berhasil mengirim Rp25.000 ke Siti",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(danaSend)
        danaSend!!
        assertEquals(TransactionType.EXPENSE, danaSend.type)
        assertEquals(2_500_000L, danaSend.amountCents)
        assertEquals(WalletType.EWALLET, danaSend.wallet)
        assertEquals("Siti", danaSend.note)

        val shopeepayCashback = PaymentNotificationParser.parse(
            packageName = "com.shopeepay.id",
            appLabel = "ShopeePay",
            title = "ShopeePay",
            text = "Kamu membayar Rp35.000 di Starbucks, cashback Rp5.000",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(shopeepayCashback)
        shopeepayCashback!!
        assertEquals(TransactionType.EXPENSE, shopeepayCashback.type)
        assertEquals(3_500_000L, shopeepayCashback.amountCents)
        assertEquals(ExpenseCategory.FOOD, shopeepayCashback.category)
        assertEquals(WalletType.EWALLET, shopeepayCashback.wallet)
        assertEquals("Starbucks", shopeepayCashback.note)

        val gopayTopUp = PaymentNotificationParser.parseWithReason(
            packageName = "com.gojek.gopay",
            appLabel = "GoPay",
            title = "Top Up Berhasil",
            text = "Top Up Rp100.000 berhasil masuk ke saldo GoPay kamu"
        )
        assertEquals(PaymentParseResult.Skipped(PaymentParseSkipReason.WALLET_TOP_UP), gopayTopUp)

        val ovoIsiSaldo = PaymentNotificationParser.parseWithReason(
            packageName = "ovo.id",
            appLabel = "OVO",
            title = "Isi Saldo Berhasil",
            text = "Isi saldo OVO Rp50.000 dari BCA berhasil"
        )
        assertEquals(PaymentParseResult.Skipped(PaymentParseSkipReason.WALLET_TOP_UP), ovoIsiSaldo)

        val pulsaTopUp = PaymentNotificationParser.parse(
            packageName = "com.gojek.gopay",
            appLabel = "GoPay",
            title = "Transaksi Berhasil",
            text = "Top Up Pulsa Telkomsel Rp50.000 berhasil",
            userCurrency = AppCurrency.IDR
        )
        assertNotNull(pulsaTopUp)
        pulsaTopUp!!
        assertEquals(TransactionType.EXPENSE, pulsaTopUp.type)
        assertEquals(5_000_000L, pulsaTopUp.amountCents)
        assertEquals(ExpenseCategory.BILLS, pulsaTopUp.category)
    }

    @Test
    fun `includes expanded verified Indonesian finance package presets`() {
        val defaults = KnownFinanceApps.defaultPackages
        assertTrue("com.shopeepay.id" in defaults)
        assertTrue("co.id.bankbsi.superapp" in defaults)
        assertTrue("id.flip" in defaults)
        assertTrue("com.alloapp.yump" in defaults)
        assertTrue("com.bnc.finance" in defaults)
        assertTrue("id.superbank.app" in defaults)
    }
}
