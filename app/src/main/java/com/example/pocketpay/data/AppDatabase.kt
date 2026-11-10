package com.example.pocketpay.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Provider

@Database(
    entities = [AccountEntity::class, PocketEntity::class, TransactionEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankingDao(): BankingDao
}

/** v5 -> v6: pockets gain `lastInterestAt` so interest can be accrued daily. Existing data is kept. */
val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pockets ADD COLUMN lastInterestAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE pockets SET lastInterestAt = ${System.currentTimeMillis()}")
    }
}

/** v6 -> v7: optional savings goal per pocket. Existing data is kept (goal = NULL). */
val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pockets ADD COLUMN targetAmount INTEGER")
    }
}

class DatabaseCallback(
    @Suppress("unused") private val databaseProvider: Provider<AppDatabase>? = null
) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        try {
            val count = db.query("SELECT COUNT(*) FROM accounts").use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }

            if (count == 0) {
                seedInitialData(db)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedInitialData(db: SupportSQLiteDatabase) {
        // Seed Primary Account (Rp 5,000,000 initial balance)
        db.execSQL(
            """
            INSERT OR REPLACE INTO accounts (id, accountNumber, balance)
            VALUES ('user_primary_account', '9012839102', 5000000)
            """.trimIndent()
        )

        // Seed Initial Spending Pocket
        db.execSQL(
            """
            INSERT OR REPLACE INTO pockets (id, name, balance, pocketType, interestRatePercent, tenorMonths, maturityDate, lastInterestAt)
            VALUES ('pocket_daily_01', 'Jajan Harian', 350000, 'SPENDING', 3.5, NULL, NULL, ${System.currentTimeMillis()})
            """.trimIndent()
        )

        // Seed Initial Locked Deposit Pocket (3-Month Tenor)
        val threeMonthsMillis = 3L * 30 * 24 * 60 * 60 * 1000
        val defaultMaturity = System.currentTimeMillis() + threeMonthsMillis

        db.execSQL(
            """
            INSERT OR REPLACE INTO pockets (id, name, balance, pocketType, interestRatePercent, tenorMonths, maturityDate, lastInterestAt)
            VALUES ('pocket_deposit_01', 'Deposito Darurat', 2000000, 'LOCKED_DEPOSIT', 6.0, 3, $defaultMaturity, ${System.currentTimeMillis()})
            """.trimIndent()
        )
    }
}