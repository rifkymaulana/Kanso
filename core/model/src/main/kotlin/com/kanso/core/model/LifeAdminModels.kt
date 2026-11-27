package com.kanso.core.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class ExpiryCategory(val label: String) {
    IDENTITY("ID Card"),
    LICENSE("Driver's License"),
    PASSPORT("Passport"),
    VEHICLE("Vehicle Tax / Reg"),
    INSURANCE("Insurance"),
    WARRANTY("Warranty"),
    SUBSCRIPTION("Subscription"),
    OTHER("Other")
}

@Serializable
data class ExpiryItem(
    val id: Long = 0L,
    val title: String,
    val category: ExpiryCategory,
    val referenceNumber: String = "",
    val expiryEpochDay: Long,
    val remindDaysBefore: Int = 30,
    val note: String = "",
    val updatedAtMillis: Long
)

@Serializable
enum class MaintenanceCategory(val label: String) {
    CAR("Car"),
    MOTORCYCLE("Motorcycle"),
    HOME("Home"),
    APPLIANCE("Appliance"),
    OTHER("Other")
}

@Serializable
data class MaintenanceLog(
    val id: Long = 0L,
    val assetName: String,
    val category: MaintenanceCategory,
    val serviceDescription: String,
    val serviceEpochDay: Long,
    val odometerKm: Int? = null,
    val costCents: Long? = null,
    val nextDueEpochDay: Long? = null,
    val nextDueOdometerKm: Int? = null,
    val note: String = ""
)

@Serializable
enum class VaultCategory(val label: String) {
    CREDENTIAL("Login"),
    RECOVERY("Recovery Code"),
    FINANCE("Finance"),
    IDENTITY("Identity"),
    NOTE("Private Note")
}

@Serializable
data class VaultItem(
    val id: Long = 0L,
    val title: String,
    val category: VaultCategory,
    val secretContent: String,
    val updatedAtMillis: Long
)

enum class ExpiryUrgency {
    EXPIRED,
    CRITICAL_7_DAYS,
    UPCOMING,
    SAFE
}

object LifeAdminCalculations {

    fun daysUntilExpiry(expiryEpochDay: Long, todayEpochDay: Long): Long {
        return expiryEpochDay - todayEpochDay
    }

    fun urgency(item: ExpiryItem, todayEpochDay: Long): ExpiryUrgency {
        val delta = daysUntilExpiry(item.expiryEpochDay, todayEpochDay)
        return when {
            delta < 0L -> ExpiryUrgency.EXPIRED
            delta <= 7L -> ExpiryUrgency.CRITICAL_7_DAYS
            delta <= item.remindDaysBefore.toLong() -> ExpiryUrgency.UPCOMING
            else -> ExpiryUrgency.SAFE
        }
    }

    fun needsAttention(item: ExpiryItem, todayEpochDay: Long): Boolean {
        return urgency(item, todayEpochDay) != ExpiryUrgency.SAFE
    }

    fun isMaintenanceDueSoon(
        log: MaintenanceLog,
        todayEpochDay: Long,
        withinDays: Int = 14
    ): Boolean {
        val due = log.nextDueEpochDay ?: return false
        return due <= todayEpochDay + withinDays.toLong()
    }

    fun activeDueMaintenanceLogs(
        logs: List<MaintenanceLog>,
        todayEpochDay: Long,
        withinDays: Int = 14
    ): List<MaintenanceLog> {
        return logs
            .groupBy {
                Triple(
                    it.category,
                    it.assetName.trim().lowercase(),
                    it.serviceDescription.trim().lowercase()
                )
            }
            .values
            .mapNotNull { group ->
                group.maxWithOrNull(compareBy<MaintenanceLog> { it.serviceEpochDay }.thenBy { it.id })
            }
            .filter { isMaintenanceDueSoon(it, todayEpochDay, withinDays) }
            .sortedBy { it.nextDueEpochDay ?: Long.MAX_VALUE }
    }

    fun renewedExpiryEpochDay(
        currentExpiryEpochDay: Long,
        todayEpochDay: Long,
        years: Long = 1L
    ): Long {
        val stepYears = years.coerceAtLeast(1L)
        var candidate = LocalDate.ofEpochDay(currentExpiryEpochDay).plusYears(stepYears)
        while (candidate.toEpochDay() <= todayEpochDay) {
            candidate = candidate.plusYears(stepYears)
        }
        return candidate.toEpochDay()
    }

    fun mapMaintenanceExpenseCategory(category: MaintenanceCategory): ExpenseCategory {
        return when (category) {
            MaintenanceCategory.CAR, MaintenanceCategory.MOTORCYCLE -> ExpenseCategory.TRANSPORT
            MaintenanceCategory.HOME, MaintenanceCategory.APPLIANCE -> ExpenseCategory.HOUSING
            MaintenanceCategory.OTHER -> ExpenseCategory.BILLS
        }
    }

    fun buildRepeatMaintenanceTemplate(
        previous: MaintenanceLog,
        todayEpochDay: Long
    ): MaintenanceLog {
        val previousIntervalDays = previous.nextDueEpochDay
            ?.let { it - previous.serviceEpochDay }
            ?.takeIf { it > 0L }
        val projectedNextDueEpochDay = previousIntervalDays?.let { todayEpochDay + it }

        val prevOdo = previous.odometerKm
        val prevNextOdo = previous.nextDueOdometerKm
        val startingOdometerKm = prevNextOdo ?: prevOdo
        val projectedNextOdometerKm = if (prevOdo != null && prevNextOdo != null && prevNextOdo > prevOdo) {
            val intervalKm = prevNextOdo - prevOdo
            prevNextOdo + intervalKm
        } else {
            null
        }

        return MaintenanceLog(
            id = 0L,
            assetName = previous.assetName,
            category = previous.category,
            serviceDescription = previous.serviceDescription,
            serviceEpochDay = todayEpochDay,
            odometerKm = startingOdometerKm,
            costCents = previous.costCents,
            nextDueEpochDay = projectedNextDueEpochDay,
            nextDueOdometerKm = projectedNextOdometerKm,
            note = previous.note
        )
    }
}
