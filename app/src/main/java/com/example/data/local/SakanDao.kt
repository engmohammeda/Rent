package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SakanDao {

    // Config
    @Query("SELECT * FROM apartment_config WHERE id = 1")
    fun observeConfig(): Flow<ApartmentConfigEntity?>

    @Query("SELECT * FROM apartment_config WHERE id = 1")
    suspend fun getConfigOnce(): ApartmentConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: ApartmentConfigEntity)

    // Residents
    @Query("SELECT * FROM residents ORDER BY roomNumber ASC, sortOrder ASC, id ASC")
    fun observeResidents(): Flow<List<ResidentEntity>>

    @Query("SELECT * FROM residents ORDER BY roomNumber ASC, sortOrder ASC, id ASC")
    suspend fun getResidentsOnce(): List<ResidentEntity>

    @Query("SELECT * FROM residents WHERE isActive = 1 ORDER BY roomNumber ASC, sortOrder ASC, id ASC")
    suspend fun getActiveResidentsOnce(): List<ResidentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResident(resident: ResidentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResidents(residents: List<ResidentEntity>): List<Long>

    @Update
    suspend fun updateResident(resident: ResidentEntity)

    @Delete
    suspend fun deleteResident(resident: ResidentEntity)

    @Query("DELETE FROM residents")
    suspend fun deleteAllResidents()

    // Billing Periods
    @Query("SELECT * FROM billing_periods ORDER BY year DESC, month DESC, id DESC")
    fun observePeriods(): Flow<List<BillingPeriodEntity>>

    @Query("SELECT * FROM billing_periods ORDER BY year DESC, month DESC, id DESC")
    suspend fun getPeriodsOnce(): List<BillingPeriodEntity>

    @Query("SELECT * FROM billing_periods WHERE id = :periodId")
    suspend fun getPeriodById(periodId: Long): BillingPeriodEntity?

    @Query("SELECT * FROM billing_periods WHERE isClosed = 0 ORDER BY year DESC, month DESC, id DESC LIMIT 1")
    suspend fun getLatestOpenPeriod(): BillingPeriodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: BillingPeriodEntity): Long

    @Update
    suspend fun updatePeriod(period: BillingPeriodEntity)

    @Query("DELETE FROM billing_periods WHERE id = :periodId")
    suspend fun deletePeriodById(periodId: Long)

    // Ledger
    @Query("SELECT * FROM resident_ledger WHERE periodId = :periodId")
    fun observeLedgerForPeriod(periodId: Long): Flow<List<ResidentLedgerEntity>>

    @Query("SELECT * FROM resident_ledger WHERE periodId = :periodId")
    suspend fun getLedgerForPeriodOnce(periodId: Long): List<ResidentLedgerEntity>

    @Query("SELECT * FROM resident_ledger WHERE id = :ledgerId")
    suspend fun getLedgerById(ledgerId: Long): ResidentLedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<ResidentLedgerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLedgerEntry(entry: ResidentLedgerEntity)

    @Update
    suspend fun updateLedgerEntry(entry: ResidentLedgerEntity)

    @Query("DELETE FROM resident_ledger WHERE periodId = :periodId")
    suspend fun deleteLedgerForPeriod(periodId: Long)

    @Query("DELETE FROM resident_ledger WHERE residentId = :residentId")
    suspend fun deleteLedgerForResident(residentId: Long)

    // Transactions
    @Query("SELECT * FROM payment_transactions ORDER BY paidAt DESC")
    fun observeAllTransactions(): Flow<List<PaymentTransactionEntity>>

    @Query("SELECT * FROM payment_transactions WHERE periodId = :periodId ORDER BY paidAt DESC")
    fun observeTransactionsForPeriod(periodId: Long): Flow<List<PaymentTransactionEntity>>

    @Query("SELECT * FROM payment_transactions WHERE residentId = :residentId ORDER BY paidAt DESC")
    fun observeTransactionsForResident(residentId: Long): Flow<List<PaymentTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: PaymentTransactionEntity): Long

    @Query("DELETE FROM payment_transactions WHERE id = :txId")
    suspend fun deleteTransaction(txId: Long)
}
