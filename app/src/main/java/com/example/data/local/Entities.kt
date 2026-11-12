package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.max

@Entity(tableName = "apartment_config")
data class ApartmentConfigEntity(
    @PrimaryKey val id: Int = 1,
    val apartmentName: String = "شقة الوالد",
    val roomCount: Int = 2,
    val rentCycleMonths: Int = 3,
    val rentCycleAmount: Double = 5000.0,
    val collectRentMonthlyInstallment: Boolean = true,
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
    val previousRentBalance: Double = 0.0,
    val previousElectricityBalance: Double = 0.0,
    val currentRentShare: Double = 0.0,
    val currentElectricityShare: Double = 0.0,
    val paidRent: Double = 0.0,
    val paidElectricity: Double = 0.0,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalRentDue: Double
        get() = previousRentBalance + currentRentShare

    val totalElectricityDue: Double
        get() = previousElectricityBalance + currentElectricityShare

    val totalDue: Double
        get() = totalRentDue + totalElectricityDue

    val totalPaid: Double
        get() = paidRent + paidElectricity

    val remainingRent: Double
        get() = max(0.0, totalRentDue - paidRent)

    val remainingElectricity: Double
        get() = max(0.0, totalElectricityDue - paidElectricity)

    val totalRemaining: Double
        get() = remainingRent + remainingElectricity

    val previousTotalBalance: Double
        get() = previousRentBalance + previousElectricityBalance

    val paymentStatus: PaymentStatus
        get() = when {
            totalRemaining <= 0.05 -> PaymentStatus.PAID
            totalPaid > 0.05 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.UNPAID
        }
}

enum class PaymentStatus {
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
    val note: String = "",
    val paidAt: Long = System.currentTimeMillis()
)

data class ResidentLedgerWithDetails(
    val ledger: ResidentLedgerEntity,
    val resident: ResidentEntity
)

enum class PaymentAllocationMode {
    FULL_REMAINING,
    AUTO_ELECTRICITY_FIRST,
    AUTO_RENT_FIRST,
    ELECTRICITY_ONLY,
    RENT_ONLY,
    CUSTOM_SPLIT
}
