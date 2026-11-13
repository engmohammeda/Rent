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
    fun `initialization seeds 12 residents across 2 rooms with 4500 quarterly rent default`() = runBlocking {
        repository.ensureInitialized()
        val config = repository.configFlow.first()!!
        val residents = repository.residentsFlow.first()
        val periods = repository.periodsFlow.first()

        assertEquals(4500.0, config.rentCycleAmount, 0.01)
        assertEquals(3, config.rentCycleMonths)
        assertTrue(config.waterIncludedInRent)
        assertEquals(12, residents.size)
        assertEquals(6, residents.count { it.roomNumber == 1 })
        assertEquals(6, residents.count { it.roomNumber == 2 })
        assertEquals(1, periods.size)

        val period = periods.first()
        val ledgers = repository.observeLedgerForPeriod(period.id).first()
        assertEquals(12, ledgers.size)
        // 4500 / 3 = 1500 monthly pool, divided by 12 = 125.0 per person per month
        assertEquals(125.0, ledgers.first().currentRentShare, 0.01)
    }

    @Test
    fun `flexible payments including 200 cash, 3-month prepayment, partial debt, and signed rollover work accurately`() = runBlocking {
        repository.ensureInitialized()
        val period1 = repository.periodsFlow.first().first()

        // Update electricity bill to 600 (50 per resident for 12 residents)
        repository.updatePeriodUtilities(period1.id, 600.0, 0.0, 1000.0, 1400.0)
        val ledgersAfterElec = repository.observeLedgerForPeriod(period1.id).first()
        assertEquals(50.0, ledgersAfterElec.first().currentElectricityShare, 0.01)

        // Resident 0 brings 200 -> 50 electricity + 150 rent (creates 25 advance credit over 125 monthly rent)
        val res0 = ledgersAfterElec[0]
        repository.recordPayment(
            ledgerId = res0.id,
            residentName = "ساكن 1",
            amountBrought = 200.0,
            mode = PaymentAllocationMode.AUTO_UTILITIES_FIRST
        )

        // Resident 1 pays full 3 months rent (375) + electricity (50) = 425 (creates 250 advance credit)
        val res1 = ledgersAfterElec[1]
        repository.recordPayment(
            ledgerId = res1.id,
            residentName = "ساكن 2",
            amountBrought = 425.0,
            mode = PaymentAllocationMode.AUTO_UTILITIES_FIRST
        )

        // Resident 2 pays only 100 -> 50 electricity + 50 rent (leaves 75 debt)
        val res2 = ledgersAfterElec[2]
        repository.recordPayment(
            ledgerId = res2.id,
            residentName = "ساكن 3",
            amountBrought = 100.0,
            mode = PaymentAllocationMode.AUTO_UTILITIES_FIRST
        )

        // Resident 3 pays exact remaining via batchPay
        val res3 = ledgersAfterElec[3]
        repository.batchMarkFullPaid(listOf(res3.id))

        val updated = repository.observeLedgerForPeriod(period1.id).first().associateBy { it.id }
        assertEquals(PaymentStatus.ADVANCE_CREDIT, updated[res0.id]?.paymentStatus)
        assertEquals(25.0, updated[res0.id]?.creditRent ?: 0.0, 0.01)
        assertEquals(PaymentStatus.ADVANCE_CREDIT, updated[res1.id]?.paymentStatus)
        assertEquals(250.0, updated[res1.id]?.creditRent ?: 0.0, 0.01)
        assertEquals(PaymentStatus.PARTIAL, updated[res2.id]?.paymentStatus)
        assertEquals(75.0, updated[res2.id]?.remainingRent ?: 0.0, 0.01)
        assertEquals(PaymentStatus.PAID, updated[res3.id]?.paymentStatus)

        // Close month and roll over to next month (elec 720 = 60 each)
        val period2Id = repository.closePeriodAndRollover(
            currentPeriodId = period1.id,
            nextElectricityAmount = 720.0,
            nextWaterAmount = 0.0,
            nextPrevReading = 1400.0,
            nextCurrReading = 1900.0,
            chargeRentInNewPeriod = true
        )

        val p2 = repository.observeLedgerForPeriod(period2Id).first().associateBy { it.residentId }

        val carried0 = p2[res0.residentId]!!
        assertEquals(-25.0, carried0.previousRentBalance, 0.01)
        assertEquals(100.0, carried0.remainingRent, 0.01)
        assertEquals(60.0, carried0.remainingElectricity, 0.01)

        val carried1 = p2[res1.residentId]!!
        assertEquals(-250.0, carried1.previousRentBalance, 0.01)
        assertEquals(0.0, carried1.remainingRent, 0.01)
        assertEquals(125.0, carried1.creditRent, 0.01)
        assertEquals(60.0, carried1.remainingElectricity, 0.01)

        val carried2 = p2[res2.residentId]!!
        assertEquals(75.0, carried2.previousRentBalance, 0.01)
        assertEquals(200.0, carried2.remainingRent, 0.01)
        assertEquals(260.0, carried2.totalRemaining, 0.01)

        val carried3 = p2[res3.residentId]!!
        assertEquals(0.0, carried3.previousRentBalance, 0.01)
        assertEquals(125.0, carried3.remainingRent, 0.01)
    }

    @Test
    fun `separate water bill splits across residents when waterIncludedInRent is false`() = runBlocking {
        repository.ensureInitialized()
        val cfg = repository.configFlow.first()!!
        repository.updateConfig(cfg.copy(waterIncludedInRent = false), applyRentToOpenPeriod = true)

        val period = repository.periodsFlow.first().first()
        repository.updatePeriodUtilities(period.id, electricityAmount = 600.0, waterAmount = 240.0, 0.0, 200.0)

        val ledgers = repository.observeLedgerForPeriod(period.id).first()
        assertEquals(50.0, ledgers.first().currentElectricityShare, 0.01)
        assertEquals(20.0, ledgers.first().currentWaterShare, 0.01)
        assertEquals(195.0, ledgers.first().totalRemaining, 0.01)
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
        // 1500 / 10 = 150.0 per person per month
        assertEquals(150.0, ledgers.first().currentRentShare, 0.01)
    }
}
