package com.kanso.core.database.di

import android.content.Context
import androidx.room.Room
import com.kanso.core.database.KansoDatabase
import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.dao.UtilitiesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKansoDatabase(
        @ApplicationContext context: Context
    ): KansoDatabase {
        return Room.databaseBuilder(
            context,
            KansoDatabase::class.java,
            "kanso_superapp.db"
        )
            .addMigrations(KansoDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideMoneyDao(database: KansoDatabase): MoneyDao = database.moneyDao()

    @Provides
    fun provideBrainDao(database: KansoDatabase): BrainDao = database.brainDao()

    @Provides
    fun provideHabitsDao(database: KansoDatabase): HabitsDao = database.habitsDao()

    @Provides
    fun provideLifeAdminDao(database: KansoDatabase): LifeAdminDao = database.lifeAdminDao()

    @Provides
    fun provideUtilitiesDao(database: KansoDatabase): UtilitiesDao = database.utilitiesDao()
}
