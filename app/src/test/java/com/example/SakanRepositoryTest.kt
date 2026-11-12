package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PaymentAllocationMode
import com.example.data.local.PaymentStatus
import com.example.data.local.SakanDatabase
import com.example.data.repository.SakanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SakanRepositoryTest {

    private lateinit var db: SakanDatabase
    private lateinit var repository: SakanRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SakanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SakanRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `initialization seeds 12 residents across 2 rooms and calculates monthly rent split`() = runBlocking {
        repository.ensureInitialized()
        val residents = repository.residentsFlow.first()
        val periods = repository.periodsFlow.first()
        assertEquals(12, residents.size)
        assertEquals(6, residents.count { it.roomNumber == 1 })
        assertEquals(6, residents.count { it.roomNumber == 2 })
        assertEquals(1, periods.size)

        val period = periods.first()
        val ledgers = repository.observeLedgerForPeriod(period.id).first()
        assertEquals(12, ledgers.size)
        // 5000 / 3 = 1666.67, divided by 12 = 138.89 per person per month
        assertEquals(138.89, ledgers.first().currentRentShare, 0.05)
    }

    @Test
    fun `electricity bill split, partial payment, batch full payment, and carry-over rollover work accurately`() = runBlocking {
        repository.ensureInitialized()
        val period1 = repository.periodsFlow.first().first()

        // Update electricity bill to 600 (50 per resident for 12 residents)
        repository.updatePeriodElectricity(period1.id, 600.0, 1000.0, 1400.0)
        val ledgersAfterElec = repository.observeLedgerForPeriod(period1.id).first()
        assertEquals(50.0, ledgersAfterElec.first().currentElectricityShare, 0.01)

        // Resident 0 pays only 100 ("حسب ما أحضره"), electricity first (50 elec + 50 rent)
        val firstLedger = ledgersAfterElec[0]
        val expectedRentRemaining = firstLedger.currentRentShare - 50.0
        repository.recordPayment(
            ledgerId = firstLedger.id,
            residentName = "ساكن 1",
            amountBrought = 100.0,
            mode = PaymentAllocationMode.AUTO_ELECTRICITY_FIRST
        )

        // Resident 1 pays in full via batchPay
        val secondLedger = ledgersAfterElec[1]
        repository.batchMarkFullPaid(listOf(secondLedger.id))

        val updatedLedgers = repository.observeLedgerForPeriod(period1.id).first().associateBy { it.id }
        assertEquals(PaymentStatus.PARTIAL, updatedLedgers[firstLedger.id]?.paymentStatus)
        assertEquals(PaymentStatus.PAID, updatedLedgers[secondLedger.id]?.paymentStatus)

        // Close month and roll over to next month
        val period2Id = repository.closePeriodAndRollover(
            currentPeriodId = period1.id,
            nextElectricityAmount = 720.0,
            nextPrevReading = 1400.0,
            nextCurrReading = 1900.0,
            chargeRentInNewPeriod = true
        )

        val period2Ledgers = repository.observeLedgerForPeriod(period2Id).first()
            .associateBy { it.residentId }

        val carriedFirst = period2Ledgers[firstLedger.residentId]!!
        assertEquals(expectedRentRemaining, carriedFirst.previousRentBalance, 0.05)
        assertEquals(0.0, carriedFirst.previousElectricityBalance, 0.05)
        assertEquals(60.0, carriedFirst.currentElectricityShare, 0.05)

        val carriedSecond = period2Ledgers[secondLedger.residentId]!!
        assertEquals(0.0, carriedSecond.previousTotalBalance, 0.05)
        assertTrue(carriedSecond.currentRentShare > 138.0)
    }

    @Test
    fun `auto generating residents updates count and recalculates open period shares`() = runBlocking {
        repository.ensureInitialized()
        repository.generateResidentsAuto(roomCount = 2, totalResidents = 10, replaceExisting = true)
        val residents = repository.residentsFlow.first()
        assertEquals(10, residents.size)
        val period = repository.periodsFlow.first().first()
        val ledgers = repository.observeLedgerForPeriod(period.id).first()
        assertEquals(10, ledgers.size)
        // 1666.67 / 10 = 166.67
        assertEquals(166.67, ledgers.first().currentRentShare, 0.05)
    }
}
