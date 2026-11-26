package com.kanso.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.dao.UtilitiesDao
import com.kanso.core.database.entity.CategoryBudgetEntity
import com.kanso.core.database.entity.CountdownEntity
import com.kanso.core.database.entity.DailyHealthLogEntity
import com.kanso.core.database.entity.ExpiryEntity
import com.kanso.core.database.entity.HabitCompletionEntity
import com.kanso.core.database.entity.HabitEntity
import com.kanso.core.database.entity.IouEntity
import com.kanso.core.database.entity.MaintenanceLogEntity
import com.kanso.core.database.entity.NoteEntity
import com.kanso.core.database.entity.NoteFtsEntity
import com.kanso.core.database.entity.RecurringBillEntity
import com.kanso.core.database.entity.SavingsGoalEntity
import com.kanso.core.database.entity.SnippetEntity
import com.kanso.core.database.entity.TaskEntity
import com.kanso.core.database.entity.TransactionEntity
import com.kanso.core.database.entity.VaultEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryBudgetEntity::class,
        RecurringBillEntity::class,
        IouEntity::class,
        SavingsGoalEntity::class,
        NoteEntity::class,
        NoteFtsEntity::class,
        TaskEntity::class,
        SnippetEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        DailyHealthLogEntity::class,
        ExpiryEntity::class,
        MaintenanceLogEntity::class,
        VaultEntity::class,
        CountdownEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KansoDatabase : RoomDatabase() {
    abstract fun moneyDao(): MoneyDao
    abstract fun brainDao(): BrainDao
    abstract fun habitsDao(): HabitsDao
    abstract fun lifeAdminDao(): LifeAdminDao
    abstract fun utilitiesDao(): UtilitiesDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetDaysPerWeek` INTEGER NOT NULL,
                        `reminderHour` INTEGER,
                        `reminderMinute` INTEGER,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_completions` (
                        `habitId` INTEGER NOT NULL,
                        `epochDay` INTEGER NOT NULL,
                        PRIMARY KEY(`habitId`, `epochDay`),
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_epochDay` ON `habit_completions` (`epochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_habitId` ON `habit_completions` (`habitId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_health_logs` (
                        `epochDay` INTEGER NOT NULL,
                        `waterMl` INTEGER NOT NULL,
                        `sleepHours` REAL,
                        `weightKg` REAL,
                        `mood` INTEGER,
                        `workoutNote` TEXT NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`epochDay`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expiry_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `referenceNumber` TEXT NOT NULL,
                        `expiryEpochDay` INTEGER NOT NULL,
                        `remindDaysBefore` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expiry_items_expiryEpochDay` ON `expiry_items` (`expiryEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `maintenance_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `serviceDescription` TEXT NOT NULL,
                        `serviceEpochDay` INTEGER NOT NULL,
                        `odometerKm` INTEGER,
                        `costCents` INTEGER,
                        `nextDueEpochDay` INTEGER,
                        `nextDueOdometerKm` INTEGER,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_logs_serviceEpochDay` ON `maintenance_logs` (`serviceEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vault_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `encryptedContent` TEXT NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `countdowns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetEpochDay` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `toWallet` TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `targetAmountCents` INTEGER NOT NULL,
                        `savedAmountCents` INTEGER NOT NULL,
                        `linkedWallet` TEXT,
                        `targetEpochDay` INTEGER,
                        `note` TEXT NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
