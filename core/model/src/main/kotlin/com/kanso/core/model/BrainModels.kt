package com.kanso.core.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class NoteItem(
    val id: Long = 0L,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Serializable
enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

@Serializable
enum class TaskRecurrence(val label: String, val shortLabel: String) {
    NONE("One-time", "Once"),
    DAILY("Daily", "Daily"),
    WEEKLY("Weekly", "Weekly"),
    MONTHLY("Monthly", "Monthly")
}

@Serializable
data class TaskItem(
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateEpochDay: Long? = null,
    val recurrence: TaskRecurrence = TaskRecurrence.NONE,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null
)

@Serializable
enum class SnippetCategory(val label: String) {
    ADDRESS("Address"),
    FINANCE("Finance"),
    IDENTITY("Identity"),
    TEMPLATE("Template"),
    OTHER("Other")
}

@Serializable
data class SnippetItem(
    val id: Long = 0L,
    val label: String,
    val value: String,
    val category: SnippetCategory = SnippetCategory.OTHER,
    val isSensitive: Boolean = false,
    val isPinned: Boolean = false,
    val updatedAtMillis: Long
)

object SnippetMasker {
    fun mask(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.length <= 4) {
            "••••"
        } else {
            "•••• •••• ${trimmed.takeLast(4)}"
        }
    }
}

object FtsQuerySanitizer {
    fun toFtsQuery(raw: String): String {
        val cleaned = buildString(raw.length) {
            for (ch in raw) {
                if (ch.isLetterOrDigit()) {
                    append(ch)
                } else {
                    append(' ')
                }
            }
        }
        val tokens = cleaned
            .split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return tokens.joinToString(" ") { "$it*" }
    }
}

object TaskCalculations {

    fun nextRecurringDueEpochDay(
        currentDueEpochDay: Long?,
        recurrence: TaskRecurrence,
        todayEpochDay: Long
    ): Long? {
        if (recurrence == TaskRecurrence.NONE) return currentDueEpochDay
        val today = LocalDate.ofEpochDay(todayEpochDay)
        val anchor = currentDueEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today
        val nextDate = when (recurrence) {
            TaskRecurrence.NONE -> anchor
            TaskRecurrence.DAILY -> {
                if (anchor.isAfter(today)) anchor.plusDays(1L) else today.plusDays(1L)
            }
            TaskRecurrence.WEEKLY -> {
                if (anchor.isAfter(today)) {
                    anchor.plusWeeks(1L)
                } else {
                    val daysBehind = todayEpochDay - anchor.toEpochDay()
                    anchor.plusWeeks((daysBehind / 7L) + 1L)
                }
            }
            TaskRecurrence.MONTHLY -> {
                var months = if (anchor.isAfter(today)) {
                    1L
                } else {
                    maxOf(
                        1L,
                        (today.year - anchor.year) * 12L + (today.monthValue - anchor.monthValue).toLong()
                    )
                }
                var candidate = anchor.plusMonths(months)
                while (!candidate.isAfter(today)) {
                    months++
                    candidate = anchor.plusMonths(months)
                }
                candidate
            }
        }
        return nextDate.toEpochDay()
    }
}

data class NoteChecklistLine(
    val lineIndex: Int,
    val text: String,
    val isChecked: Boolean
)

data class ParsedNoteChecklist(
    val items: List<NoteChecklistLine>,
    val plainPreviewLines: List<String>,
    val checkedCount: Int,
    val totalCount: Int
) {
    val hasChecklist: Boolean get() = totalCount > 0
    val isAllChecked: Boolean get() = totalCount > 0 && checkedCount == totalCount
}

object NoteChecklistParser {

    private val checklistRegex = Regex("""^(\s*[-*]\s*\[)([ xX]?)(\]\s*)(.*)$""")

    fun parse(content: String): ParsedNoteChecklist {
        if (content.isBlank()) {
            return ParsedNoteChecklist(emptyList(), emptyList(), 0, 0)
        }
        val normalized = content.replace("\r\n", "\n")
        val lines = normalized.split('\n')
        val items = mutableListOf<NoteChecklistLine>()
        val plainLines = mutableListOf<String>()

        lines.forEachIndexed { index, rawLine ->
            val match = checklistRegex.matchEntire(rawLine)
            if (match != null) {
                val checked = match.groupValues[2].equals("x", ignoreCase = true)
                val itemText = match.groupValues[4].trim()
                items += NoteChecklistLine(
                    lineIndex = index,
                    text = itemText.ifEmpty { "Item" },
                    isChecked = checked
                )
            } else if (rawLine.isNotBlank()) {
                plainLines += rawLine.trim()
            }
        }

        return ParsedNoteChecklist(
            items = items,
            plainPreviewLines = plainLines,
            checkedCount = items.count { it.isChecked },
            totalCount = items.size
        )
    }

    fun toggleLine(content: String, lineIndex: Int): String {
        val normalized = content.replace("\r\n", "\n")
        val lines = normalized.split('\n').toMutableList()
        if (lineIndex !in lines.indices) return content
        val match = checklistRegex.matchEntire(lines[lineIndex]) ?: return content
        val prefix = match.groupValues[1]
        val wasChecked = match.groupValues[2].equals("x", ignoreCase = true)
        val rawMid = match.groupValues[3]
        val body = match.groupValues[4]
        val mid = if (rawMid == "]" && body.isNotEmpty()) "] " else rawMid
        val nextMark = if (wasChecked) " " else "x"
        lines[lineIndex] = "$prefix$nextMark$mid$body"
        return lines.joinToString("\n")
    }

    fun appendChecklistItem(content: String): String {
        return when {
            content.isBlank() -> "- [ ] "
            content.endsWith("\n") -> "$content- [ ] "
            else -> "$content\n- [ ] "
        }
    }
}
