package com.kanso.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentCaptureMode(val label: String) {
    PROMPT("1-Tap Prompt"),
    AUTO_SAVE("Auto-Save")
}

data class KnownFinanceApp(
    val packageName: String,
    val displayName: String,
    val defaultWallet: WalletType,
    val groupLabel: String
)

object KnownFinanceApps {
    val presets: List<KnownFinanceApp> = listOf(
        // Banks
        KnownFinanceApp("com.bca", "BCA mobile", WalletType.BANK, "Banks"),
        KnownFinanceApp("com.bca.mybca.omni.android", "myBCA", WalletType.BANK, "Banks"),
        KnownFinanceApp("id.bmri.livin", "Livin' by Mandiri", WalletType.BANK, "Banks"),
        KnownFinanceApp("id.co.bri.brimo", "BRImo", WalletType.BANK, "Banks"),
        KnownFinanceApp("src.com.bni", "BNI Mobile", WalletType.BANK, "Banks"),
        KnownFinanceApp("id.bni.wondr", "wondr by BNI", WalletType.BANK, "Banks"),
        KnownFinanceApp("com.jago.digitalBanking", "Bank Jago", WalletType.BANK, "Banks"),
        KnownFinanceApp("com.bcadigital.blu", "blu by BCA", WalletType.BANK, "Banks"),
        KnownFinanceApp("id.co.bankbkemobile.digitalbank", "SeaBank", WalletType.BANK, "Banks"),
        KnownFinanceApp("com.btpn.dc", "Jenius", WalletType.BANK, "Banks"),
        KnownFinanceApp("id.co.cimbniaga.octomobile", "OCTO Mobile", WalletType.BANK, "Banks"),
        KnownFinanceApp("com.bsm.activity2", "BSI Mobile", WalletType.BANK, "Banks"),
        KnownFinanceApp("net.permatabank.permatamobilex", "PermataME", WalletType.BANK, "Banks"),
        KnownFinanceApp("com.wise.android", "Wise", WalletType.BANK, "Banks"),
        // E-Wallets & Super-Apps
        KnownFinanceApp("com.gojek.gopay", "GoPay", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("com.gojek.app", "Gojek", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("ovo.id", "OVO", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("id.dana", "DANA", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("com.shopee.id", "Shopee / ShopeePay", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("com.grabtaxi.passenger", "Grab", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("com.telkom.mwallet", "LinkAja", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("com.tokopedia.tkpd", "Tokopedia", WalletType.EWALLET, "E-Wallets & Apps"),
        KnownFinanceApp("com.paypal.android.p2pmobile", "PayPal", WalletType.EWALLET, "E-Wallets & Apps"),
        // Cards
        KnownFinanceApp("com.google.android.apps.walletnfcrel", "Google Wallet", WalletType.CARD, "Cards"),
        KnownFinanceApp("com.revolut.revolut", "Revolut", WalletType.CARD, "Cards")
    )

    val defaultPackages: Set<String> = presets.map { it.packageName }.toSet()

    fun findByPackage(packageName: String): KnownFinanceApp? =
        presets.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) }
}

@Serializable
enum class PaymentParseSkipReason(val label: String) {
    EMPTY("Empty notification"),
    OTP("OTP / verification code"),
    PENDING_OR_FAILED("Unpaid, pending, or failed"),
    PROMO_OR_LOAN("Promo, voucher, or loan"),
    NO_TRANSACTION_SIGNAL("Not a completed payment"),
    NO_CURRENCY_AMOUNT("No currency amount"),
    DUPLICATE("Duplicate within 90s"),
    LOGGER_DISABLED("Auto-logger paused")
}

@Serializable
data class DetectedPaymentAlert(
    val packageName: String,
    val sourceAppLabel: String,
    val type: TransactionType,
    val amountCents: Long,
    val detectedCurrency: AppCurrency,
    val wasConvertedCurrency: Boolean,
    val category: ExpenseCategory,
    val wallet: WalletType,
    val note: String,
    val timestampMillis: Long,
    val fingerprint: String
)

sealed interface PaymentParseResult {
    data class Matched(val alert: DetectedPaymentAlert) : PaymentParseResult
    data class Skipped(val reason: PaymentParseSkipReason) : PaymentParseResult
}

@Serializable
data class PaymentNotificationLogEntry(
    val id: Long,
    val packageName: String,
    val sourceAppLabel: String,
    val title: String,
    val text: String,
    val timestampMillis: Long,
    val matchedAlert: DetectedPaymentAlert? = null,
    val skipReason: PaymentParseSkipReason? = null
) {
    val isMatched: Boolean get() = matchedAlert != null
}

object PaymentNotificationParser {

    private val marketplacePackages = setOf(
        "com.shopee.id",
        "com.tokopedia.tkpd",
        "com.grabtaxi.passenger"
    )

    private val otpKeywordsRegex = Regex(
        """\b(otp|kode verifikasi|verification code|jangan berikan kode|passcode|pin anda)\b""",
        RegexOption.IGNORE_CASE
    )

    private val pendingOrFailedKeywordsRegex = Regex(
        """\b(menunggu pembayaran|belum dibayar|selesaikan pembayaran|bayar sebelum|jatuh tempo|segera bayar|segara bayar|transaksi gagal|pembayaran gagal|gagal diproses|dibatalkan|cancelled|canceled|declined|failed|saldo tidak cukup|insufficient|payment due|awaiting payment)\b""",
        RegexOption.IGNORE_CASE
    )

    private val promoKeywordsRegex = Regex(
        """\b(promo|voucher|kupon|klaim|claim|diskon s/d|diskon hingga|cashback s/d|cashback hingga|pinjaman|kredit tanpa agunan|limit hingga|ajukan sekarang|daftar sekarang|nikmati diskon|pakai kode|undang teman|referral|undian|hadiah hingga|cair hingga|bunga rendah|cicilan 0%)\b""",
        RegexOption.IGNORE_CASE
    )

    private val completedTransactionSignalRegex = Regex(
        """\b(berhasil|sukses|successful|succeeded|telah dibayar|dibayar ke|pembayaran ke|pembayaran di|pembayaran qris|transaksi qris|paid|sent to|you sent|charged|debited|purchase|spent|payment of|payment to|transaction of|pendebetan|terdebet|dana keluar|uang keluar|mutasi debet|kirim uang|dana masuk|uang masuk|terima uang|terima transfer|transfer masuk|menerima transfer|pengkreditan|mutasi kredit|received|credited|incoming transfer|refund|pengembalian dana|gaji|salary|payroll)\b""",
        RegexOption.IGNORE_CASE
    )

    private val cardWalletRegex = Regex(
        """\b(kartu kredit|credit card|debit card|paylater|visa|mastercard|amex|jcb)\b""",
        RegexOption.IGNORE_CASE
    )

    private val bankIndicatorRegex = Regex(
        """\b(virtual account|transfer bank|bca|mandiri|bri|bni|jago|seabank|blu|livin|brimo|wondr|mybca)\b""",
        RegexOption.IGNORE_CASE
    )

    private val incomingTransactionSignalRegex = Regex(
        """\b(dana masuk|uang masuk|terima uang|terima transfer|transfer masuk|menerima transfer|pengkreditan|mutasi kredit|pembayaran diterima|received from|credited|incoming transfer|refund|pengembalian dana|gaji|salary|payroll|bonus|dividend|reimbursement)\b""",
        RegexOption.IGNORE_CASE
    )

    private val outgoingOverrideRegex = Regex(
        """\b(dibayar ke|pembayaran ke|pembayaran di|pembayaran qris|transaksi qris|kirim uang|sent to|you sent|paid to|charged|debited|pendebetan|terdebet|dana keluar|uang keluar|mutasi debet|spent)\b""",
        RegexOption.IGNORE_CASE
    )

    private val merchantClausePatterns = listOf(
        Regex(
            """\b(?:dibayar ke|bayar ke|pembayaran ke|transfer ke|kirim ke|dikirim ke|kepada|penerima|ke)\s+([A-Za-z0-9&.,'()/\-\s]{2,60})""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:pembayaran di|transaksi di|belanja di|bayar di|di|at)\s+([A-Za-z0-9&.,'()/\-\s]{2,60})""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:transfer dari|terima dari|dana masuk dari|uang masuk dari|dari|from)\s+([A-Za-z0-9&.,'()/\-\s]{2,60})""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:paid to|sent to|payment to|transfer to|to)\s+([A-Za-z0-9&.,'()/\-\s]{2,60})""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:payment for|paid for|for)\s+([A-Za-z0-9&.,'()/\-\s]{2,60})""",
            RegexOption.IGNORE_CASE
        )
    )

    private val merchantTrailingNoiseRegex = Regex(
        """\s*(?:[.,]\s*)?\b(?:berhasil|sukses|success|successful|succeeded|completed|selesai|senilai\b.*|sebesar\b.*|pada\b.*|on\s+\d.*|at\s+\d.*|ref\.?\b.*|id\s*transaksi\b.*|dengan\b.*|via\b.*|using\b.*|with\s+(?:card|visa|mastercard|gopay|ovo|dana|wallet).*|menggunakan\b.*|of\s+(?:Rp|IDR|USD|US\$|SGD|S\$|MYR|RM|EUR|GBP|JPY|\$).*)$""",
        RegexOption.IGNORE_CASE
    )

    private val startsWithAmountOrNumberRegex = Regex(
        """^(?:Rp\.?|IDR|USD|US\$|SGD|S\$|MYR|RM|EUR|€|GBP|£|JPY|¥|\$|[0-9]+)\b""",
        RegexOption.IGNORE_CASE
    )

    private val genericStatusTitleRegex = Regex(
        """^(?:transaksi berhasil|pembayaran berhasil|pembayaran qris berhasil|transfer berhasil|transaksi sukses|pembayaran sukses|payment successful|payment completed|payment notification|notifikasi transaksi|info transaksi|transaksi|pembayaran|notifikasi)[.!]?$""",
        RegexOption.IGNORE_CASE
    )

    private val genericMerchantStopWords = setOf(
        "rekening", "saldo", "qris", "aplikasi", "app", "account", "wallet", "dompet", "merchant", "anda", "kamu"
    )

    fun shouldInspectPackage(
        packageName: String,
        selfPackageName: String,
        allowAllApps: Boolean,
        allowedPackages: Set<String>
    ): Boolean {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty()) return false
        if (cleanPkg.equals(selfPackageName, ignoreCase = true)) return false
        if (cleanPkg == "android" || cleanPkg == "com.android.systemui") return false
        return allowAllApps || allowedPackages.any { it.equals(cleanPkg, ignoreCase = true) }
    }

    fun parse(
        packageName: String,
        appLabel: String = "",
        title: String,
        text: String,
        userCurrency: AppCurrency = AppCurrency.IDR,
        customRatesVsUsd: Map<String, Double> = emptyMap(),
        timestampMillis: Long = System.currentTimeMillis()
    ): DetectedPaymentAlert? {
        return (parseWithReason(
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            userCurrency = userCurrency,
            customRatesVsUsd = customRatesVsUsd,
            timestampMillis = timestampMillis
        ) as? PaymentParseResult.Matched)?.alert
    }

    fun parseWithReason(
        packageName: String,
        appLabel: String = "",
        title: String,
        text: String,
        userCurrency: AppCurrency = AppCurrency.IDR,
        customRatesVsUsd: Map<String, Double> = emptyMap(),
        timestampMillis: Long = System.currentTimeMillis()
    ): PaymentParseResult {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty() || cleanPkg.equals("com.kanso.app", ignoreCase = true)) {
            return PaymentParseResult.Skipped(PaymentParseSkipReason.EMPTY)
        }

        val cleanTitle = title.trim()
        val cleanText = text.trim()
        if (cleanTitle.isEmpty() && cleanText.isEmpty()) {
            return PaymentParseResult.Skipped(PaymentParseSkipReason.EMPTY)
        }

        val combined = listOf(cleanTitle, cleanText)
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        if (otpKeywordsRegex.containsMatchIn(combined)) {
            return PaymentParseResult.Skipped(PaymentParseSkipReason.OTP)
        }
        if (pendingOrFailedKeywordsRegex.containsMatchIn(combined)) {
            return PaymentParseResult.Skipped(PaymentParseSkipReason.PENDING_OR_FAILED)
        }
        if (promoKeywordsRegex.containsMatchIn(combined)) {
            return PaymentParseResult.Skipped(PaymentParseSkipReason.PROMO_OR_LOAN)
        }
        if (!completedTransactionSignalRegex.containsMatchIn(combined)) {
            return PaymentParseResult.Skipped(PaymentParseSkipReason.NO_TRANSACTION_SIGNAL)
        }

        val baseDraft = SharedTextParser.parse(
            rawText = cleanText,
            rawSubject = cleanTitle,
            userCurrency = userCurrency,
            customRatesVsUsd = customRatesVsUsd,
            isReceiptMedia = false
        )
        val amountCents = baseDraft.amountCents?.takeIf { it > 0L }
            ?: return PaymentParseResult.Skipped(PaymentParseSkipReason.NO_CURRENCY_AMOUNT)
        val detectedCurrency = baseDraft.detectedCurrency
            ?: return PaymentParseResult.Skipped(PaymentParseSkipReason.NO_CURRENCY_AMOUNT)

        val preset = KnownFinanceApps.findByPackage(cleanPkg)
        val resolvedSourceLabel = appLabel.trim().ifEmpty { preset?.displayName ?: cleanPkg }

        val extractedMerchant = extractMerchant(combined)
        val categoryDraft = if (extractedMerchant.isNotBlank()) {
            SharedTextParser.parse(
                rawText = "$cleanText\n$extractedMerchant",
                rawSubject = cleanTitle,
                userCurrency = userCurrency,
                customRatesVsUsd = customRatesVsUsd,
                isReceiptMedia = false
            )
        } else {
            baseDraft
        }

        val isIncome = baseDraft.target == SharedCaptureTarget.INCOME ||
            (incomingTransactionSignalRegex.containsMatchIn(combined) &&
                !outgoingOverrideRegex.containsMatchIn(combined))

        val txType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        val category = resolveCategory(
            isIncome = isIncome,
            combined = combined,
            extractedMerchant = extractedMerchant,
            suggested = categoryDraft.suggestedCategory
        )

        val wallet = resolveWallet(
            combined = combined,
            preset = preset,
            fallbackWallet = baseDraft.suggestedWallet
        )

        val note = resolveNote(
            extractedMerchant = extractedMerchant,
            draftNote = baseDraft.transactionNote,
            sourceLabel = resolvedSourceLabel,
            detectedCurrency = detectedCurrency,
            wasConvertedCurrency = baseDraft.wasConvertedCurrency,
            amountTextSuffix = if (baseDraft.wasConvertedCurrency) {
                val origIndex = baseDraft.transactionNote.lastIndexOf(" (")
                if (origIndex >= 0) baseDraft.transactionNote.substring(origIndex) else ""
            } else {
                ""
            }
        )

        val fingerprint = "${cleanPkg.lowercase()}|${txType.name}|$amountCents|${note.trim().lowercase()}"

        return PaymentParseResult.Matched(
            DetectedPaymentAlert(
                packageName = cleanPkg,
                sourceAppLabel = resolvedSourceLabel,
                type = txType,
                amountCents = amountCents,
                detectedCurrency = detectedCurrency,
                wasConvertedCurrency = baseDraft.wasConvertedCurrency,
                category = category,
                wallet = wallet,
                note = note,
                timestampMillis = timestampMillis,
                fingerprint = fingerprint
            )
        )
    }

    private fun extractMerchant(combined: String): String {
        for (pattern in merchantClausePatterns) {
            for (match in pattern.findAll(combined)) {
                var candidate = match.groupValues.getOrNull(1).orEmpty().trim()
                if (candidate.isEmpty()) continue
                candidate = candidate.substringBefore('\n').trim()
                if (startsWithAmountOrNumberRegex.containsMatchIn(candidate)) continue

                candidate = candidate.replace(merchantTrailingNoiseRegex, "").trim()
                candidate = candidate.trimEnd('.', ',', '-', ':', ';', ' ')
                if (candidate.length < 2) continue
                if (startsWithAmountOrNumberRegex.containsMatchIn(candidate)) continue
                if (candidate.lowercase() in genericMerchantStopWords) continue
                if (genericStatusTitleRegex.matches(candidate)) continue

                return normalizeMerchantCase(candidate).take(60)
            }
        }
        return ""
    }

    private fun normalizeMerchantCase(raw: String): String {
        val letters = raw.filter { it.isLetter() }
        if (letters.isEmpty()) return raw
        val isAllUpper = letters.all { it.isUpperCase() }
        if (!isAllUpper || letters.length <= 4) return raw

        val keepUpperTokens = setOf("PT", "CV", "TBK", "ATM", "QRIS", "PLN", "PDAM", "BPJS", "KFC", "XXI", "CGV", "MRT", "KRL", "LRT", "KAI", "BCA", "BRI", "BNI", "BSI")
        return raw.split(Regex("\\s+")).joinToString(" ") { token ->
            val clean = token.trim()
            if (clean.uppercase() in keepUpperTokens || clean.length <= 2) {
                clean.uppercase()
            } else {
                clean.lowercase().replaceFirstChar { ch -> ch.titlecase() }
            }
        }
    }

    private fun resolveCategory(
        isIncome: Boolean,
        combined: String,
        extractedMerchant: String,
        suggested: ExpenseCategory
    ): ExpenseCategory {
        if (!isIncome) {
            return if (suggested.isIncomeDefault) ExpenseCategory.OTHER else suggested
        }
        val lower = "$combined $extractedMerchant".lowercase()
        return when {
            lower.contains("gaji") || lower.contains("salary") || lower.contains("payroll") ->
                ExpenseCategory.SALARY
            lower.contains("freelance") || lower.contains("client") || lower.contains("invoice") ->
                ExpenseCategory.FREELANCE
            lower.contains("gift") || lower.contains("thr") || lower.contains("angpao") || lower.contains("hadiah") ->
                ExpenseCategory.GIFT
            else -> ExpenseCategory.OTHER
        }
    }

    private fun resolveWallet(
        combined: String,
        preset: KnownFinanceApp?,
        fallbackWallet: WalletType
    ): WalletType {
        if (cardWalletRegex.containsMatchIn(combined)) {
            return WalletType.CARD
        }
        if (preset != null) {
            if (preset.packageName in marketplacePackages && bankIndicatorRegex.containsMatchIn(combined)) {
                return WalletType.BANK
            }
            return preset.defaultWallet
        }
        return fallbackWallet
    }

    private fun resolveNote(
        extractedMerchant: String,
        draftNote: String,
        sourceLabel: String,
        detectedCurrency: AppCurrency,
        wasConvertedCurrency: Boolean,
        amountTextSuffix: String
    ): String {
        val cleanDraft = draftNote
            .let { note ->
                if (wasConvertedCurrency && amountTextSuffix.isNotEmpty() && note.endsWith(amountTextSuffix)) {
                    note.removeSuffix(amountTextSuffix).trim()
                } else {
                    note
                }
            }

        val chosenBase = when {
            extractedMerchant.isNotBlank() -> extractedMerchant
            cleanDraft.isNotBlank() &&
                !cleanDraft.equals(sourceLabel, ignoreCase = true) &&
                !genericStatusTitleRegex.matches(cleanDraft) -> cleanDraft
            else -> "via $sourceLabel"
        }

        val full = if (wasConvertedCurrency && amountTextSuffix.isNotEmpty()) {
            "$chosenBase$amountTextSuffix"
        } else {
            chosenBase
        }
        return full.take(80)
    }
}

class PaymentAlertDeduplicator(
    private val windowMillis: Long = 90_000L
) {
    private val seenAtMillis = LinkedHashMap<String, Long>()

    @Synchronized
    fun shouldProcess(fingerprint: String, nowMillis: Long): Boolean {
        val iterator = seenAtMillis.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (nowMillis - entry.value >= windowMillis) {
                iterator.remove()
            }
        }
        val lastSeen = seenAtMillis[fingerprint]
        if (lastSeen != null && (nowMillis - lastSeen) < windowMillis) {
            return false
        }
        seenAtMillis[fingerprint] = nowMillis
        return true
    }
}
