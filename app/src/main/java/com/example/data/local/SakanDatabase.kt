package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ApartmentConfigEntity::class,
        ResidentEntity::class,
        BillingPeriodEntity::class,
        ResidentLedgerEntity::class,
        PaymentTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SakanDatabase : RoomDatabase() {
    abstract fun sakanDao(): SakanDao

    companion object {
        @Volatile
        private var INSTANCE: SakanDatabase? = null

        fun getInstance(context: Context): SakanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SakanDatabase::class.java,
                    "sakan_manager.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
