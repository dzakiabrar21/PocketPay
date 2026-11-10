package com.example.pocketpay.di

import android.content.Context
import androidx.room.Room
import com.example.pocketpay.data.AppDatabase
import com.example.pocketpay.data.BankingDao
import com.example.pocketpay.data.DatabaseCallback
import com.example.pocketpay.data.MIGRATION_5_6
import com.example.pocketpay.data.MIGRATION_6_7
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<AppDatabase>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pocketpay_database.db"
        )
            .addCallback(DatabaseCallback(databaseProvider))
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .build()
    }

    @Provides
    @Singleton
    fun provideBankingDao(database: AppDatabase): BankingDao {
        return database.bankingDao()
    }
}