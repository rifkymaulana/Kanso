package com.kanso.core.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

enum class SharedCaptureTarget {
    EXPENSE,
    INCOME,
    NOTE,
    TASK,
    SNIPPET
}

data class OcrTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

object OcrReceiptLineMerger {
    fun mergeVisualRows(lines: List<OcrTextLine>): String {
        val cleaned = lines.filter { it.text.isNotBlank() }
        if (cleaned.isEmpty()) return ""

        val sorted = cleaned.sortedWith(
            compareBy<OcrTextLine> { (it.top + it.bottom) / 2.0 }.thenBy { it.left }
        )

        val rows = mutableListOf<MutableList<OcrTextLine>>()
        for (line in sorted) {
            val lineCenterY = (line.top + line.bottom) / 2.0
            val lineHeight = max(1, line.bottom - line.top).toDouble()
            val lastRow = rows.lastOrNull()
            if (lastRow == null) {
                rows.add(mutableListOf(line))
            } else {
                val rowCenterY = lastRow.map { (it.top + it.bottom) / 2.0 }.average()
                val rowAvgHeight = lastRow.map { max(1, it.bottom - it.top).toDouble() }.average()
                val threshold = max(rowAvgHeight, lineHeight) * 0.60
                if (abs(lineCenterY - rowCenterY) <= threshold) {
                    lastRow.add(line)
                } else {
                    rows.add(mutableListOf(line))
                }
            }
        }

        return rows.joinToString("\n") { row ->
            val leftToRight = row.sortedBy { it.left }
            if (leftToRight.size == 1) {
                leftToRight[0].text.trim()
            } else {
                buildString {
                    leftToRight.forEachIndexed { idx, segment ->
                        val segText = segment.text.trim()
                        if (idx == 0) {
                            append(segText)
                        } else {
                            val prev = leftToRight[idx - 1].text.trim()
                            if (prev.endsWith(":") || prev.endsWith("-")) {
                                append(' ')
                            } else {
                                append(" : ")
                            }
                            append(segText)
                        }
                    }
                }
            }
        }
    }
}

data class ParsedShareDraft(
    val target: SharedCaptureTarget,
    val amountCents: Long? = null,
    val detectedCurrency: AppCurrency? = null,
    val wasConvertedCurrency: Boolean = false,
    val suggestedCategory: ExpenseCategory = ExpenseCategory.OTHER,
    val suggestedWallet: WalletType = WalletType.EWALLET,
    val transactionNote: String = "",
    val noteTitle: String = "",
    val noteContent: String = "",
    val taskTitle: String = "",
    val snippetLabel: String = "",
    val snippetValue: String = ""
)

object SharedTextParser {

    private data class CurrencyPatternSpec(
        val currency: AppCurrency,
        val regex: Regex
    )

    private data class AmountCandidate(
        val currency: AppCurrency,
        val majorUnits: Double,
        val score: Int,
        val hasStrongPositiveKeyword: Boolean,
        val lineIndex: Int
    )

    private val currencySpecs = listOf(
        CurrencyPatternSpec(
            AppCurrency.SGD,
            Regex("""(?<![A-Za-z])(?:S\$|SGD)\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)
        ),
        CurrencyPatternSpec(
            AppCurrency.USD,
            Regex("""(?<![A-Za-z])(?:US\$|USD)\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)
        ),
        CurrencyPatternSpec(
            AppCurrency.IDR,
            Regex("""(?<![A-Za-z])(?:Rp\.?|IDR)\s*([0-9][0-9.,]*(?:\s*,-)?)""", RegexOption.IGNORE_CASE)
        ),
        CurrencyPatternSpec(
            AppCurrency.MYR,
            Regex("""(?<![A-Za-z])(?:RM|MYR)\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)
        ),
        CurrencyPatternSpec(
            AppCurrency.EUR,
            Regex("""(?:€|(?<![A-Za-z])EUR)\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)
        ),
        CurrencyPatternSpec(
            AppCurrency.GBP,
            Regex("""(?:£|(?<![A-Za-z])GBP)\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)
        ),
        CurrencyPatternSpec(
            AppCurrency.JPY,
            Regex("""(?:¥|(?<![A-Za-z])JPY)\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)
        ),
        CurrencyPatternSpec(
            AppCurrency.USD,
            Regex("""(?<![A-Za-z0-9])\$(?!\$)\s*([0-9][0-9.,]*)""")
        )
    )

    private val positiveAmountKeywords = Regex(
        """(?<!sub[\s\-]?)\b(total|grand total|total bayar|total transaksi|total pembayaran|total belanja|total tagihan|jumlah|nominal|amount|paid|charged|debited)\b""",
        RegexOption.IGNORE_CASE
    )
    private val localPositiveAmountKeywords = Regex(
        """\b(sebesar|senilai|bertambah|berkurang|didebet|didebit|terdebit|dikredit|dikreditkan|diterima|mentransfer|mengirim|membayar|bayar|transfer|pembayaran|transaksi|tagihan)\b""",
        RegexOption.IGNORE_CASE
    )
    private val negativeAmountKeywords = Regex(
        """\b(saldo|balance|limit|sisa|tersisa|cashback|diskon|discount|hemat|biaya admin|biaya transaksi|biaya layanan|biaya transfer|biaya kliring|biaya penanganan|admin fee|service fee|fee|potongan|poin|points|subtotal|sub[\s\-]?total|kembali|kembalian|change)\b""",
        RegexOption.IGNORE_CASE
    )
    private val thermalTotalFallbackRegex = Regex(
        """^(?:grand\s+total|total\s+bayar|total\s+belanja|total\s+pembayaran|total\s+tagihan|total\s+transaksi|jumlah\s+bayar|total)\s*[:\-]?\s*([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]+\.[0-9]{2})\b""",
        RegexOption.IGNORE_CASE
    )
    private val receiptContextKeywords = Regex(
        """\b(struk|tunai|kembali|kembalian|kasir|qris|subtotal|sub[\s\-]?total|cash|change)\b""",
        RegexOption.IGNORE_CASE
    )
    private val taskPrefixRegex = Regex(
        """^(todo|task|remind me to|reminder)\b\s*[:\-]?\s*""",
        RegexOption.IGNORE_CASE
    )
    private val snippetSubjectKeywords = Regex(
        """\b(code|kode|resi|awb|rekening|va|token)\b""",
        RegexOption.IGNORE_CASE
    )
    private val merchantLabelRegex = Regex(
        """^(?:merchant|kepada|penerima|nama penerima|tujuan|penyedia jasa|toko|outlet|to)\s*[:\-]\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val genericStatusLineRegex = Regex(
        """^(transaksi berhasil|pembayaran berhasil|transfer berhasil|payment successful|receipt|struk|detail transaksi|berhasil)\.?$""",
        RegexOption.IGNORE_CASE
    )
    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    fun parse(
        rawText: String,
        rawSubject: String = "",
        userCurrency: AppCurrency = AppCurrency.IDR,
        customRatesVsUsd: Map<String, Double> = emptyMap(),
        isReceiptMedia: Boolean = false
    ): ParsedShareDraft {
        val cleanText = rawText.trim()
        val cleanSubject = rawSubject.trim()
        val lines = cleanText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val firstNonBlankLine = lines.firstOrNull().orEmpty()

        val noteTitle = cleanSubject.ifEmpty { firstNonBlankLine.take(48) }
        val noteContent = cleanText.ifEmpty { cleanSubject }

        val taskSource = when {
            firstNonBlankLine.isBlank() -> cleanSubject
            urlRegex.matches(firstNonBlankLine) && cleanSubject.isNotBlank() ->
                "$cleanSubject ($firstNonBlankLine)"
            else -> firstNonBlankLine
        }
        val cleanedTaskTitle = taskSource
            .removePrefix("- [ ] ")
            .removePrefix("* [ ] ")
            .removePrefix("- [x] ")
            .removePrefix("* [x] ")
            .replace(taskPrefixRegex, "")
            .trim()
            .take(120)

        val snippetLabel = cleanSubject.ifEmpty { "Shared Snippet" }.take(40)
        val snippetValue = cleanText.ifEmpty { cleanSubject }

        val combinedForSearch = buildString {
            if (cleanSubject.isNotBlank()) appendLine(cleanSubject)
            if (cleanText.isNotBlank()) append(cleanText)
        }

        val bestCandidate = findBestCurrencyCandidate(
            text = combinedForSearch,
            userCurrency = userCurrency,
            allowCurrencyLessFallback = isReceiptMedia || receiptContextKeywords.containsMatchIn(combinedForSearch)
        )
        if (bestCandidate != null) {
            val detectedCurrency = bestCandidate.currency
            val majorUnits = bestCandidate.majorUnits
            val origCents = (majorUnits * 100.0).roundToLong()

            val wasConverted = detectedCurrency != userCurrency
            val finalAmountCents = if (!wasConverted) {
                if (userCurrency.decimalDigits == 0) {
                    majorUnits.roundToLong() * 100L
                } else {
                    origCents
                }
            } else {
                val convertedMajor = OfflineCurrencyConverter.convert(
                    amount = majorUnits,
                    from = detectedCurrency,
                    to = userCurrency,
                    customOverrides = customRatesVsUsd
                )
                if (userCurrency.decimalDigits == 0) {
                    convertedMajor.roundToLong() * 100L
                } else {
                    (convertedMajor * 100.0).roundToLong()
                }
            }

            val isIncome = isIncomeText(combinedForSearch)
            val target = if (isIncome) SharedCaptureTarget.INCOME else SharedCaptureTarget.EXPENSE
            val category = if (isIncome) {
                inferIncomeCategory(combinedForSearch)
            } else {
                inferExpenseCategory(combinedForSearch)
            }
            val wallet = inferWallet(combinedForSearch)

            val baseNote = extractTransactionNote(lines, cleanSubject)
            val txNote = if (wasConverted) {
                val origFormatted = CurrencyFormatter.format(origCents, detectedCurrency)
                if (baseNote.isNotBlank()) "$baseNote ($origFormatted)" else origFormatted
            } else {
                baseNote
            }.take(80)

            return ParsedShareDraft(
                target = target,
                amountCents = finalAmountCents,
                detectedCurrency = detectedCurrency,
                wasConvertedCurrency = wasConverted,
                suggestedCategory = category,
                suggestedWallet = wallet,
                transactionNote = txNote,
                noteTitle = noteTitle,
                noteContent = noteContent,
                taskTitle = cleanedTaskTitle,
                snippetLabel = snippetLabel,
                snippetValue = snippetValue
            )
        }

        val isIncome = isReceiptMedia && isIncomeText(combinedForSearch)
        val fallbackTarget = if (isReceiptMedia) {
            if (isIncome) SharedCaptureTarget.INCOME else SharedCaptureTarget.EXPENSE
        } else {
            classifyNonReceiptTarget(
                cleanText = cleanText,
                cleanSubject = cleanSubject,
                lines = lines,
                firstLine = firstNonBlankLine
            )
        }
        val fallbackNote = if (isReceiptMedia) {
            extractTransactionNote(lines, cleanSubject).take(80)
        } else {
            ""
        }
        return ParsedShareDraft(
            target = fallbackTarget,
            suggestedCategory = if (isReceiptMedia) {
                if (isIncome) inferIncomeCategory(combinedForSearch) else inferExpenseCategory(combinedForSearch)
            } else {
                ExpenseCategory.OTHER
            },
            suggestedWallet = if (isReceiptMedia) inferWallet(combinedForSearch) else WalletType.EWALLET,
            transactionNote = fallbackNote,
            noteTitle = noteTitle,
            noteContent = noteContent,
            taskTitle = cleanedTaskTitle,
            snippetLabel = snippetLabel,
            snippetValue = snippetValue
        )
    }

    private fun findBestCurrencyCandidate(
        text: String,
        userCurrency: AppCurrency = AppCurrency.IDR,
        allowCurrencyLessFallback: Boolean = false
    ): AmountCandidate? {
        data class RawLineMatch(
            val currency: AppCurrency,
            val majorUnits: Double,
            val range: IntRange
        )

        val candidates = mutableListOf<AmountCandidate>()
        text.lineSequence().forEachIndexed { lineIdx, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            val usedRanges = mutableListOf<IntRange>()
            val lineMatches = mutableListOf<RawLineMatch>()

            for (spec in currencySpecs) {
                for (match in spec.regex.findAll(trimmed)) {
                    val range = match.range
                    if (usedRanges.any { it.first <= range.last && range.first <= it.last }) {
                        continue
                    }
                    val rawNumber = match.groupValues.getOrNull(1).orEmpty()
                    val majorUnits = parseMajorUnits(rawNumber, spec.currency)
                    if (majorUnits != null && majorUnits > 0.0 && majorUnits.isFinite()) {
                        usedRanges.add(range)
                        lineMatches.add(RawLineMatch(spec.currency, majorUnits, range))
                    }
                }
            }

            lineMatches.sortBy { it.range.first }
            var prevMatchEnd = 0
            for (rawMatch in lineMatches) {
                val precedingSegment = trimmed.substring(prevMatchEnd, rawMatch.range.first)
                val immediateClause = precedingSegment
                    .substringAfterLast(". ")
                    .substringAfterLast("! ")
                    .substringAfterLast("; ")
                    .substringAfterLast(", ")
                val trailingImmediate = trimmed.substring(rawMatch.range.last + 1)
                    .take(24)
                    .substringBefore('.')
                    .substringBefore(',')
                    .substringBefore('!')
                    .substringBefore(';')

                val hasImmediateStrongPos = positiveAmountKeywords.containsMatchIn(immediateClause)
                val hasImmediateLocalPos = localPositiveAmountKeywords.containsMatchIn(immediateClause)
                val hasNeg = negativeAmountKeywords.containsMatchIn(immediateClause) ||
                    negativeAmountKeywords.containsMatchIn(trailingImmediate)

                val hasStrongPos = hasImmediateStrongPos ||
                    (!hasNeg && prevMatchEnd == 0 && positiveAmountKeywords.containsMatchIn(precedingSegment))
                val hasLocalPos = hasImmediateLocalPos ||
                    (!hasNeg && prevMatchEnd == 0 && localPositiveAmountKeywords.containsMatchIn(precedingSegment))

                val score = when {
                    hasNeg && !hasImmediateStrongPos -> -8
                    else -> (if (hasStrongPos) 10 else if (hasLocalPos) 6 else 0) + (if (hasNeg) -8 else 0)
                }

                candidates.add(
                    AmountCandidate(
                        currency = rawMatch.currency,
                        majorUnits = rawMatch.majorUnits,
                        score = score,
                        hasStrongPositiveKeyword = hasStrongPos,
                        lineIndex = lineIdx
                    )
                )
                prevMatchEnd = rawMatch.range.last + 1
            }
        }
        if (candidates.isEmpty() && allowCurrencyLessFallback) {
            text.lineSequence().forEachIndexed { lineIdx, line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || negativeAmountKeywords.containsMatchIn(trimmed)) return@forEachIndexed
                val match = thermalTotalFallbackRegex.find(trimmed) ?: return@forEachIndexed
                val rawNumber = match.groupValues.getOrNull(1).orEmpty()
                val majorUnits = parseMajorUnits(rawNumber, userCurrency)
                if (majorUnits != null && majorUnits > 0.0 && majorUnits.isFinite()) {
                    candidates.add(
                        AmountCandidate(
                            currency = userCurrency,
                            majorUnits = majorUnits,
                            score = 10,
                            hasStrongPositiveKeyword = true,
                            lineIndex = lineIdx
                        )
                    )
                }
            }
        }
        if (candidates.isEmpty()) return null
        val maxScore = candidates.maxOf { it.score }
        val topCandidates = candidates.filter { it.score == maxScore }
        return if (topCandidates.any { it.hasStrongPositiveKeyword }) {
            topCandidates.last()
        } else {
            topCandidates.first()
        }
    }

    internal fun parseMajorUnits(rawNumber: String, currency: AppCurrency): Double? {
        var s = rawNumber.trim()
        s = s.replace(Regex(""",\s*-$"""), "")
        s = s.trimEnd('.', ',')
        if (s.isEmpty()) return null

        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val normalized = when {
            lastDot >= 0 && lastComma >= 0 -> {
                if (lastDot > lastComma) {
                    val afterDot = s.length - lastDot - 1
                    if (afterDot in 1..2) {
                        s.replace(",", "")
                    } else {
                        s.replace(",", "").replace(".", "")
                    }
                } else {
                    val afterComma = s.length - lastComma - 1
                    if (afterComma in 1..2) {
                        s.replace(".", "").replace(',', '.')
                    } else {
                        s.replace(".", "").replace(",", "")
                    }
                }
            }

            lastComma >= 0 -> {
                val afterComma = s.length - lastComma - 1
                if (s.count { it == ',' } == 1 && afterComma in 1..2) {
                    s.replace(',', '.')
                } else {
                    s.replace(",", "")
                }
            }

            lastDot >= 0 -> {
                val dotCount = s.count { it == '.' }
                val afterDot = s.length - lastDot - 1
                if (currency.decimalDigits == 0) {
                    if (dotCount == 1 && afterDot in 1..2) {
                        s
                    } else {
                        s.replace(".", "")
                    }
                } else {
                    if (dotCount == 1 && afterDot in 1..2) {
                        s
                    } else {
                        s.replace(".", "")
                    }
                }
            }

            else -> s
        }

        val parsed = normalized.toDoubleOrNull() ?: return null
        return if (parsed > 0.0 && parsed.isFinite()) parsed else null
    }

    private fun isIncomeText(text: String): Boolean {
        val strongIncome = Regex(
            """\b(refund|pengembalian dana|gaji|salary|payroll|dividend|reimbursement)\b""",
            RegexOption.IGNORE_CASE
        )
        if (strongIncome.containsMatchIn(text)) return true

        val softIncome = Regex(
            """\b(dana masuk|uang masuk|terima uang|terima transfer|transfer masuk|transfer in|bertambah|diterima|diterima transfer|kamu menerima|anda menerima|dikredit|dikreditkan|received from|incoming transfer|bonus)\b|\(cr\)""",
            RegexOption.IGNORE_CASE
        )
        val outgoingIndicators = Regex(
            """\b(pembayaran|total bayar|paid to|purchase|belanja|didebet|didebit|terdebit|berkurang|mentransfer|mengirim|membayar)\b|\(db\)""",
            RegexOption.IGNORE_CASE
        )
        return softIncome.containsMatchIn(text) && !outgoingIndicators.containsMatchIn(text)
    }

    private fun inferIncomeCategory(text: String): ExpenseCategory {
        val lower = text.lowercase()
        return when {
            containsAny(lower, "gaji", "salary", "payroll", "bonus") -> ExpenseCategory.SALARY
            containsAny(lower, "freelance", "client", "invoice") -> ExpenseCategory.FREELANCE
            containsAny(lower, "gift", "thr", "angpao", "hadiah") -> ExpenseCategory.GIFT
            else -> ExpenseCategory.OTHER
        }
    }

    private fun inferExpenseCategory(text: String): ExpenseCategory {
        val lower = text.lowercase()
        return when {
            containsAny(
                lower,
                "gofood", "grabfood", "shopeefood", "starbucks", "fore", "kenangan",
                "janji jiwa", "waroeng", "resto", "restaurant", "cafe", "steak",
                "mcd", "kfc", "hokben", "mixue"
            ) -> ExpenseCategory.FOOD

            containsAny(
                lower,
                "indomaret", "alfamart", "alfamidi", "superindo", "hypermart",
                "ranch market", "farmers market", "lottemart", "astro", "sayurbox",
                "segari", "supermarket", "groceries"
            ) -> ExpenseCategory.GROCERIES

            containsAny(
                lower,
                "goride", "gocar", "grabcar", "grabbike", "grab ", "bluebird", "taxi",
                "mrt", "krl", "lrt", "transjakarta", "whoosh", "kai", "pertamina",
                "shell", "bensin", "fuel", "parkir", "parking", "e-toll", "flazz"
            ) -> ExpenseCategory.TRANSPORT

            containsAny(
                lower,
                "pln", "listrik", "pdam", "indihome", "biznet", "first media",
                "telkomsel", "xl", "indosat", "by.u", "smartfren", "pulsa",
                "paket data", "bpjs", "tagihan", "internet", "wifi", "electricity"
            ) -> ExpenseCategory.BILLS

            containsAny(
                lower,
                "netflix", "spotify", "youtube", "disney", "google play", "steam",
                "playstation", "xxi", "cgv", "cinema", "bioskop"
            ) -> ExpenseCategory.ENTERTAINMENT

            containsAny(
                lower,
                "halodoc", "apotek", "kimia farma", "k24", "century", "rumah sakit",
                "hospital", "klinik", "clinic", "dokter", "pharmacy", "obat"
            ) -> ExpenseCategory.HEALTH

            containsAny(
                lower,
                "tokopedia", "shopee", "lazada", "blibli", "tiktok shop", "uniqlo",
                "ace hardware", "miniso", "sociolla", "guardian", "watsons", "amazon"
            ) -> ExpenseCategory.SHOPPING

            containsAny(
                lower,
                "ikea", "sewa", "kos", "kost", "rent", "ipl", "apartemen"
            ) -> ExpenseCategory.HOUSING

            containsAny(
                lower,
                "udemy", "coursera", "gramedia", "tuition", "spp", "kursus", "course"
            ) -> ExpenseCategory.EDUCATION

            containsAny(
                lower,
                "kopi", "coffee", "warung", "makan", "bakery"
            ) -> ExpenseCategory.FOOD

            else -> ExpenseCategory.OTHER
        }
    }

    private fun inferWallet(text: String): WalletType {
        val lower = text.lowercase()
        return when {
            containsAny(
                lower,
                "gopay", "ovo", "dana", "shopeepay", "linkaja", "qris", "e-wallet", "ewallet"
            ) -> WalletType.EWALLET

            containsAny(
                lower,
                "kartu kredit", "credit card", "visa", "mastercard", "jcb", "amex",
                "debit card", "paylater"
            ) -> WalletType.CARD

            containsAny(
                lower,
                "bca", "mandiri", "livin", "bri", "brimo", "briva", "bni", "wondr", "cimb",
                "jenius", "jago", "seabank", "blu", "permata", "bsi", "byond", "allobank",
                "allo bank", "neobank", "superbank", "bi-fast",
                "virtual account", "transfer bank", "m-banking",
                "m-transfer", "mybca", "klikbca", "octo", "ocbc", "danamon", "btn"
            ) -> WalletType.BANK

            containsAny(lower, "tunai", "cash") -> WalletType.CASH

            else -> WalletType.EWALLET
        }
    }

    private fun extractTransactionNote(lines: List<String>, subject: String): String {
        for (line in lines) {
            val merchantMatch = merchantLabelRegex.find(line)
            if (merchantMatch != null) {
                val merchant = merchantMatch.groupValues[1].trim()
                if (merchant.isNotEmpty()) return merchant
            }
        }
        if (subject.isNotBlank() &&
            !genericStatusLineRegex.matches(subject) &&
            !isPureAmountOrDateLine(subject)
        ) {
            return subject
        }
        val informativeLine = lines.firstOrNull { line ->
            !genericStatusLineRegex.matches(line) &&
                !isPureAmountOrDateLine(line) &&
                !negativeAmountKeywords.containsMatchIn(line)
        }
        return informativeLine ?: lines.firstOrNull().orEmpty()
    }

    private fun isPureAmountOrDateLine(line: String): Boolean {
        val stripped = line.trim()
        val hasCurrencyMatch = currencySpecs.any { spec ->
            val match = spec.regex.find(stripped)
            match != null && match.value.length >= stripped.length - 2
        }
        if (hasCurrencyMatch) return true
        return stripped.all { it.isDigit() || it in setOf('/', '-', ':', '.', ',', ' ') }
    }

    private fun classifyNonReceiptTarget(
        cleanText: String,
        cleanSubject: String,
        lines: List<String>,
        firstLine: String
    ): SharedCaptureTarget {
        if (lines.size == 1 && taskPrefixRegex.containsMatchIn(firstLine)) {
            return SharedCaptureTarget.TASK
        }
        val isSingleLineNonUrl = lines.size <= 1 &&
            cleanText.length in 4..64 &&
            !urlRegex.containsMatchIn(cleanText)
        if (isSingleLineNonUrl) {
            val digitCount = cleanText.count { it.isDigit() }
            val tokenCount = cleanText.split(Regex("\\s+")).count { it.isNotEmpty() }
            val subjectSuggestsSnippet = snippetSubjectKeywords.containsMatchIn(cleanSubject)
            if (subjectSuggestsSnippet || (tokenCount <= 4 && digitCount >= 3)) {
                return SharedCaptureTarget.SNIPPET
            }
        }
        return SharedCaptureTarget.NOTE
    }

    private fun containsAny(haystack: String, vararg needles: String): Boolean {
        return needles.any { haystack.contains(it) }
    }
}
