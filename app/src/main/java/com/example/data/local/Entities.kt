package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.max

@Entity(tableName = "apartment_config")
data class ApartmentConfigEntity(
    @PrimaryKey val id: Int = 1,
    val apartmentName: String = "عزبتنا",
    val roomCount: Int = 2,
    val rentCycleMonths: Int = 3,
    val rentCycleAmount: Double = 4500.0,
    val collectRentMonthlyInstallment: Boolean = true,
    val waterIncludedInRent: Boolean = true,
    val currencySymbol: String = "ر.س",
    val electricityDueDayOfMonth: Int = 25,
    val rentDueDayOfMonth: Int = 1,
    val reminderIntervalHours: Int = 24,
    val notificationsEnabled: Boolean = true,
    val autoRolloverEnabled: Boolean = true
)

@Entity(tableName = "residents")
data class ResidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roomNumber: Int = 1,
    val phoneNumber: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "billing_periods")
data class BillingPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    val month: Int,
    val label: String,
    val rentPoolAmount: Double,
    val rentCycleTotalAmount: Double,
    val rentCycleMonths: Int,
    val electricityTotalAmount: Double = 0.0,
    val waterTotalAmount: Double = 0.0,
    val prevMeterReading: Double = 0.0,
    val currMeterReading: Double = 0.0,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val consumptionKwh: Double
        get() = max(0.0, currMeterReading - prevMeterReading)

    val avgKwhCost: Double
        get() = if (consumptionKwh > 0.0) electricityTotalAmount / consumptionKwh else 0.0
}

@Entity(tableName = "resident_ledger")
data class ResidentLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodId: Long,
    val residentId: Long,
    // Signed balances from previous periods: >0 means debt owed, <0 means advance credit paid ahead
    val previousRentBalance: Double = 0.0,
    val previousElectricityBalance: Double = 0.0,
    val previousWaterBalance: Double = 0.0,
    val currentRentShare: Double = 0.0,
    val currentElectricityShare: Double = 0.0,
    val currentWaterShare: Double = 0.0,
    val paidRent: Double = 0.0,
    val paidElectricity: Double = 0.0,
    val paidWater: Double = 0.0,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val netRentBalance: Double
        get() = previousRentBalance + currentRentShare - paidRent

    val netElectricityBalance: Double
        get() = previousElectricityBalance + currentElectricityShare - paidElectricity

    val netWaterBalance: Double
        get() = previousWaterBalance + currentWaterShare - paidWater

    val remainingRent: Double
        get() = max(0.0, netRentBalance)

    val remainingElectricity: Double
        get() = max(0.0, netElectricityBalance)

    val remainingWater: Double
        get() = max(0.0, netWaterBalance)

    val creditRent: Double
        get() = max(0.0, -netRentBalance)

    val creditElectricity: Double
        get() = max(0.0, -netElectricityBalance)

    val creditWater: Double
        get() = max(0.0, -netWaterBalance)

    val totalAdvanceCredit: Double
        get() = creditRent + creditElectricity + creditWater

    val totalRemaining: Double
        get() = remainingRent + remainingElectricity + remainingWater

    val totalCurrentCharges: Double
        get() = currentRentShare + currentElectricityShare + currentWaterShare

    val previousArrearsDebt: Double
        get() = max(0.0, previousRentBalance) + max(0.0, previousElectricityBalance) + max(0.0, previousWaterBalance)

    val previousAdvanceCredit: Double
        get() = max(0.0, -previousRentBalance) + max(0.0, -previousElectricityBalance) + max(0.0, -previousWaterBalance)

    val totalDue: Double
        get() = max(0.0, previousRentBalance + currentRentShare) +
            max(0.0, previousElectricityBalance + currentElectricityShare) +
            max(0.0, previousWaterBalance + currentWaterShare)

    val totalPaid: Double
        get() = paidRent + paidElectricity + paidWater

    val paymentStatus: PaymentStatus
        get() = when {
            totalRemaining <= 0.05 && totalAdvanceCredit > 0.05 -> PaymentStatus.ADVANCE_CREDIT
            totalRemaining <= 0.05 -> PaymentStatus.PAID
            totalPaid > 0.05 || previousAdvanceCredit > 0.05 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.UNPAID
        }
}

enum class PaymentStatus {
    ADVANCE_CREDIT,
    PAID,
    PARTIAL,
    UNPAID
}

@Entity(tableName = "payment_transactions")
data class PaymentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodId: Long,
    val residentId: Long,
    val residentNameSnapshot: String,
    val amountPaid: Double,
    val allocatedToRent: Double,
    val allocatedToElectricity: Double,
    val allocatedToWater: Double = 0.0,
    val note: String = "",
    val paidAt: Long = System.currentTimeMillis()
)

data class ResidentLedgerWithDetails(
    val ledger: ResidentLedgerEntity,
    val resident: ResidentEntity
)

enum class PaymentAllocationMode {
    FULL_REMAINING,
    AUTO_UTILITIES_FIRST,
    AUTO_RENT_FIRST,
    ELECTRICITY_ONLY,
    WATER_ONLY,
    RENT_ONLY,
    CUSTOM_SPLIT
}
