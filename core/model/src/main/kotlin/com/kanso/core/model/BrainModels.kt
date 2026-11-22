package com.kanso.core.model

import kotlinx.serialization.Serializable

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
data class TaskItem(
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateEpochDay: Long? = null,
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
