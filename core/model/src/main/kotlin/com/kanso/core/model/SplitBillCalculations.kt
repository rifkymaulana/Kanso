package com.kanso.core.model

import kotlin.math.floor
import kotlin.math.roundToLong

enum class SplitBillMode(val label: String) {
    EQUAL("Equal Split"),
    BY_ITEM("By Item")
}

enum class SplitExpenseRecording(val label: String) {
    FULL_BILL("Full Bill"),
    MY_SHARE("My Share")
}

data class SplitParticipantInput(
    val id: Long,
    val name: String,
    val isPayer: Boolean = false,
    val itemAmountCents: Long = 0L
)

data class SplitParticipantShare(
    val id: Long,
    val name: String,
    val isPayer: Boolean,
    val itemAmountCents: Long,
    val finalShareCents: Long
)

data class SplitBillResult(
    val mode: SplitBillMode,
    val subtotalCents: Long,
    val servicePercent: Double,
    val serviceCents: Long,
    val taxPercent: Double,
    val taxCents: Long,
    val discountCents: Long,
    val grandTotalCents: Long,
    val shares: List<SplitParticipantShare>,
    val payerShareCents: Long,
    val friendsTotalCents: Long
)

object SplitBillCalculations {

    fun unitStepCents(currency: AppCurrency): Long =
        if (currency.decimalDigits == 0) 100L else 1L

    fun roundToStep(rawCents: Double, stepCents: Long): Long {
        if (rawCents <= 0.0 || stepCents <= 0L) return 0L
        return (rawCents / stepCents.toDouble()).roundToLong() * stepCents
    }

    fun calculate(
        mode: SplitBillMode,
        manualSubtotalCents: Long,
        servicePercent: Double,
        taxPercent: Double,
        discountCents: Long,
        participants: List<SplitParticipantInput>,
        currency: AppCurrency
    ): SplitBillResult {
        val step = unitStepCents(currency)
        val subtotalCents = when (mode) {
            SplitBillMode.EQUAL -> manualSubtotalCents.coerceAtLeast(0L)
            SplitBillMode.BY_ITEM -> participants.sumOf { it.itemAmountCents.coerceAtLeast(0L) }
        }
        val cleanServicePct = servicePercent.coerceIn(0.0, 100.0)
        val cleanTaxPct = taxPercent.coerceIn(0.0, 100.0)
        val serviceCents = roundToStep(subtotalCents.toDouble() * (cleanServicePct / 100.0), step)
        val taxCents = roundToStep((subtotalCents + serviceCents).toDouble() * (cleanTaxPct / 100.0), step)
        val preDiscountTotal = subtotalCents + serviceCents + taxCents
        val cappedDiscountCents = discountCents.coerceIn(0L, preDiscountTotal)
        val grandTotalCents = (preDiscountTotal - cappedDiscountCents).coerceAtLeast(0L)

        val shares = allocateShares(
            mode = mode,
            subtotalCents = subtotalCents,
            grandTotalCents = grandTotalCents,
            stepCents = step,
            participants = participants
        )
        val payerShareCents = shares.filter { it.isPayer }.sumOf { it.finalShareCents }
        val friendsTotalCents = (grandTotalCents - payerShareCents).coerceAtLeast(0L)

        return SplitBillResult(
            mode = mode,
            subtotalCents = subtotalCents,
            servicePercent = cleanServicePct,
            serviceCents = serviceCents,
            taxPercent = cleanTaxPct,
            taxCents = taxCents,
            discountCents = cappedDiscountCents,
            grandTotalCents = grandTotalCents,
            shares = shares,
            payerShareCents = payerShareCents,
            friendsTotalCents = friendsTotalCents
        )
    }

    private fun allocateShares(
        mode: SplitBillMode,
        subtotalCents: Long,
        grandTotalCents: Long,
        stepCents: Long,
        participants: List<SplitParticipantInput>
    ): List<SplitParticipantShare> {
        if (participants.isEmpty()) return emptyList()
        val totalSteps = grandTotalCents / stepCents
        if (totalSteps <= 0L) {
            return participants.map {
                SplitParticipantShare(
                    id = it.id,
                    name = it.name,
                    isPayer = it.isPayer,
                    itemAmountCents = it.itemAmountCents.coerceAtLeast(0L),
                    finalShareCents = 0L
                )
            }
        }

        val count = participants.size
        val stepCounts = LongArray(count)

        when (mode) {
            SplitBillMode.EQUAL -> {
                val baseSteps = totalSteps / count.toLong()
                val remainderSteps = (totalSteps % count.toLong()).toInt()
                for (i in 0 until count) {
                    stepCounts[i] = baseSteps
                }
                // Order indices so the payer absorbs any 1-unit remainder first, keeping friend shares equal
                val orderedIndices = participants.indices.sortedWith(
                    compareByDescending<Int> { participants[it].isPayer }.thenBy { it }
                )
                for (r in 0 until remainderSteps) {
                    val idx = orderedIndices[r]
                    stepCounts[idx] = stepCounts[idx] + 1L
                }
            }

            SplitBillMode.BY_ITEM -> {
                if (subtotalCents > 0L) {
                    val remainders = DoubleArray(count)
                    var allocated = 0L
                    for (i in 0 until count) {
                        val itemCents = participants[i].itemAmountCents.coerceAtLeast(0L)
                        val exactSteps = totalSteps.toDouble() * (itemCents.toDouble() / subtotalCents.toDouble())
                        val base = floor(exactSteps).toLong()
                        stepCounts[i] = base
                        remainders[i] = exactSteps - base.toDouble()
                        allocated += base
                    }
                    val missingSteps = (totalSteps - allocated).toInt().coerceAtLeast(0)
                    val orderedIndices = participants.indices.sortedWith(
                        compareByDescending<Int> { remainders[it] }
                            .thenByDescending { participants[it].isPayer }
                            .thenBy { it }
                    )
                    for (r in 0 until missingSteps) {
                        val idx = orderedIndices[r % count]
                        stepCounts[idx] = stepCounts[idx] + 1L
                    }
                }
            }
        }

        return participants.mapIndexed { index, input ->
            SplitParticipantShare(
                id = input.id,
                name = input.name,
                isPayer = input.isPayer,
                itemAmountCents = input.itemAmountCents.coerceAtLeast(0L),
                finalShareCents = stepCounts[index] * stepCents
            )
        }
    }

    fun formatShareSummary(
        title: String,
        dateLabel: String,
        result: SplitBillResult,
        currency: AppCurrency,
        paymentSnippet: SnippetItem? = null
    ): String {
        val heading = title.trim().ifBlank { "Split Bill" }
        val lines = mutableListOf<String>()
        lines.add("Split Bill — $heading ($dateLabel)")

        val hasAdjustments = result.serviceCents > 0L || result.taxCents > 0L || result.discountCents > 0L
        if (hasAdjustments) {
            lines.add("Subtotal: ${CurrencyFormatter.format(result.subtotalCents, currency)}")
        }
        if (result.serviceCents > 0L) {
            lines.add("+ Service (${formatPercentLabel(result.servicePercent)}%): ${CurrencyFormatter.format(result.serviceCents, currency)}")
        }
        if (result.taxCents > 0L) {
            lines.add("+ Tax (${formatPercentLabel(result.taxPercent)}%): ${CurrencyFormatter.format(result.taxCents, currency)}")
        }
        if (result.discountCents > 0L) {
            lines.add("- Discount: ${CurrencyFormatter.format(result.discountCents, currency)}")
        }
        lines.add("Total: ${CurrencyFormatter.format(result.grandTotalCents, currency)}")

        val activeShares = result.shares.filter { it.finalShareCents > 0L }
        if (activeShares.isNotEmpty()) {
            lines.add("")
            activeShares.forEach { share ->
                val displayName = share.name.trim().ifBlank {
                    if (share.isPayer) "Me" else "Friend"
                }
                val formattedShare = CurrencyFormatter.format(share.finalShareCents, currency)
                if (share.isPayer) {
                    lines.add("• $displayName: $formattedShare (Paid)")
                } else {
                    lines.add("• $displayName: $formattedShare")
                }
            }
        }

        if (paymentSnippet != null) {
            lines.add("")
            lines.add("Payment — ${paymentSnippet.label}: ${paymentSnippet.value}")
        }

        return lines.joinToString("\n")
    }

    private fun formatPercentLabel(percent: Double): String {
        val roundedInt = percent.roundToLong()
        return if (kotlin.math.abs(percent - roundedInt.toDouble()) < 0.01) {
            roundedInt.toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", percent)
        }
    }
}
