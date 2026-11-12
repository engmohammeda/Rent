package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.ApartmentConfigEntity
import com.example.data.local.BillingPeriodEntity
import com.example.data.local.PaymentAllocationMode
import com.example.data.local.PaymentTransactionEntity
import com.example.data.local.ResidentEntity
import com.example.data.local.ResidentLedgerEntity
import com.example.data.local.SakanDatabase
import com.example.utils.Formatters
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class SakanRepository(private val database: SakanDatabase) {
    private val dao = database.sakanDao()

    val configFlow: Flow<ApartmentConfigEntity?> = dao.observeConfig()
    val residentsFlow: Flow<List<ResidentEntity>> = dao.observeResidents()
    val periodsFlow: Flow<List<BillingPeriodEntity>> = dao.observePeriods()
    val allTransactionsFlow: Flow<List<PaymentTransactionEntity>> = dao.observeAllTransactions()

    fun observeLedgerForPeriod(periodId: Long): Flow<List<ResidentLedgerEntity>> =
        dao.observeLedgerForPeriod(periodId)

    fun observeTransactionsForPeriod(periodId: Long): Flow<List<PaymentTransactionEntity>> =
        dao.observeTransactionsForPeriod(periodId)

    suspend fun ensureInitialized() {
        database.withTransaction {
            var config = dao.getConfigOnce()
            if (config == null) {
                config = ApartmentConfigEntity()
                dao.upsertConfig(config)
            }

            var residents = dao.getResidentsOnce()
            if (residents.isEmpty()) {
                val initialResidents = buildGeneratedResidentsList(
                    roomCount = config.roomCount,
                    totalResidents = 12
                )
                dao.insertResidents(initialResidents)
                residents = dao.getResidentsOnce()
            }

            val existingPeriods = dao.getPeriodsOnce()
            if (existingPeriods.isEmpty()) {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val rentPool = computeRentPoolForPeriod(config)
                val period = BillingPeriodEntity(
                    year = year,
                    month = month,
                    label = Formatters.formatMonthLabel(year, month),
                    rentPoolAmount = rentPool,
                    rentCycleTotalAmount = config.rentCycleAmount,
                    rentCycleMonths = config.rentCycleMonths,
                    electricityTotalAmount = 0.0,
                    prevMeterReading = 0.0,
                    currMeterReading = 0.0,
                    isClosed = false
                )
                val periodId = dao.insertPeriod(period)
                val activeResidents = residents.filter { it.isActive }
                val count = max(1, activeResidents.size)
                val rentShare = Formatters.round2(rentPool / count)
                val entries = activeResidents.map { r ->
                    ResidentLedgerEntity(
                        periodId = periodId,
                        residentId = r.id,
                        currentRentShare = rentShare,
                        currentElectricityShare = 0.0
                    )
                }
                dao.insertLedgerEntries(entries)
            } else if (config.autoRolloverEnabled) {
                checkAndAutoRolloverIfMonthChanged(config)
            }
        }
    }

    private suspend fun checkAndAutoRolloverIfMonthChanged(config: ApartmentConfigEntity) {
        val openPeriod = dao.getLatestOpenPeriod() ?: return
        val cal = Calendar.getInstance()
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH) + 1
        val openKey = openPeriod.year * 12 + openPeriod.month
        val nowKey = nowYear * 12 + nowMonth
        if (nowKey > openKey) {
            rolloverInternal(
                currentPeriod = openPeriod,
                targetYear = nowYear,
                targetMonth = nowMonth,
                electricityAmount = 0.0,
                prevReading = openPeriod.currMeterReading,
                currReading = openPeriod.currMeterReading,
                config = config
            )
        }
    }

    private fun computeRentPoolForPeriod(config: ApartmentConfigEntity): Double {
        return if (config.rentCycleMonths > 1 && config.collectRentMonthlyInstallment) {
            Formatters.round2(config.rentCycleAmount / config.rentCycleMonths)
        } else {
            Formatters.round2(config.rentCycleAmount)
        }
    }

    private fun buildGeneratedResidentsList(
        roomCount: Int,
        totalResidents: Int
    ): List<ResidentEntity> {
        val safeRooms = max(1, roomCount)
        val safeTotal = max(1, totalResidents)
        val list = mutableListOf<ResidentEntity>()
        val perRoomBase = safeTotal / safeRooms
        val remainder = safeTotal % safeRooms
        var counter = 1
        for (room in 1..safeRooms) {
            val countInRoom = perRoomBase + if (room <= remainder) 1 else 0
            for (idx in 1..countInRoom) {
                list.add(
                    ResidentEntity(
                        name = "ساكن $counter",
                        roomNumber = room,
                        phoneNumber = "",
                        isActive = true,
                        sortOrder = counter
                    )
                )
                counter++
            }
        }
        return list
    }

    suspend fun generateResidentsAuto(
        roomCount: Int,
        totalResidents: Int,
        replaceExisting: Boolean
    ) {
        database.withTransaction {
            val existingConfig = dao.getConfigOnce() ?: ApartmentConfigEntity()
            val updatedConfig = existingConfig.copy(roomCount = max(1, roomCount))
            dao.upsertConfig(updatedConfig)

            val openPeriod = dao.getLatestOpenPeriod()

            if (replaceExisting) {
                dao.deleteAllResidents()
                val newResidents = buildGeneratedResidentsList(roomCount, totalResidents)
                dao.insertResidents(newResidents)
                if (openPeriod != null) {
                    dao.deleteLedgerForPeriod(openPeriod.id)
                    val savedResidents = dao.getActiveResidentsOnce()
                    val count = max(1, savedResidents.size)
                    val rentShare = Formatters.round2(openPeriod.rentPoolAmount / count)
                    val elecShare = Formatters.round2(openPeriod.electricityTotalAmount / count)
                    val entries = savedResidents.map { r ->
                        ResidentLedgerEntity(
                            periodId = openPeriod.id,
                            residentId = r.id,
                            currentRentShare = rentShare,
                            currentElectricityShare = elecShare
                        )
                    }
                    dao.insertLedgerEntries(entries)
                }
            } else {
                val currentCount = dao.getResidentsOnce().size
                val generated = buildGeneratedResidentsList(roomCount, totalResidents).mapIndexed { index, r ->
                    r.copy(
                        name = "ساكن ${currentCount + index + 1}",
                        sortOrder = currentCount + index + 1
                    )
                }
                dao.insertResidents(generated)
                if (openPeriod != null) {
                    recalculateOpenPeriodSharesInternal(openPeriod.id)
                }
            }
        }
    }

    suspend fun addResident(name: String, roomNumber: Int, phoneNumber: String) {
        database.withTransaction {
            val count = dao.getResidentsOnce().size
            dao.insertResident(
                ResidentEntity(
                    name = name.trim(),
                    roomNumber = max(1, roomNumber),
                    phoneNumber = phoneNumber.trim(),
                    isActive = true,
                    sortOrder = count + 1
                )
            )
            dao.getLatestOpenPeriod()?.let { recalculateOpenPeriodSharesInternal(it.id) }
        }
    }

    suspend fun updateResident(resident: ResidentEntity) {
        database.withTransaction {
            dao.updateResident(resident)
            dao.getLatestOpenPeriod()?.let { recalculateOpenPeriodSharesInternal(it.id) }
        }
    }

    suspend fun deleteResident(resident: ResidentEntity) {
        database.withTransaction {
            dao.deleteLedgerForResident(resident.id)
            dao.deleteResident(resident)
            dao.getLatestOpenPeriod()?.let { recalculateOpenPeriodSharesInternal(it.id) }
        }
    }

    suspend fun updateConfig(newConfig: ApartmentConfigEntity, applyRentToOpenPeriod: Boolean = true) {
        database.withTransaction {
            dao.upsertConfig(newConfig)
            if (applyRentToOpenPeriod) {
                val openPeriod = dao.getLatestOpenPeriod()
                if (openPeriod != null) {
                    val newRentPool = computeRentPoolForPeriod(newConfig)
                    dao.updatePeriod(
                        openPeriod.copy(
                            rentPoolAmount = newRentPool,
                            rentCycleTotalAmount = newConfig.rentCycleAmount,
                            rentCycleMonths = newConfig.rentCycleMonths
                        )
                    )
                    recalculateOpenPeriodSharesInternal(openPeriod.id)
                }
            }
        }
    }

    suspend fun updatePeriodElectricity(
        periodId: Long,
        electricityAmount: Double,
        prevReading: Double,
        currReading: Double
    ) {
        database.withTransaction {
            val period = dao.getPeriodById(periodId) ?: return@withTransaction
            dao.updatePeriod(
                period.copy(
                    electricityTotalAmount = max(0.0, Formatters.round2(electricityAmount)),
                    prevMeterReading = max(0.0, prevReading),
                    currMeterReading = max(prevReading, currReading)
                )
            )
            recalculateOpenPeriodSharesInternal(periodId)
        }
    }

    suspend fun addOrUpdateHistoricalElectricityPeriod(
        year: Int,
        month: Int,
        electricityAmount: Double,
        prevReading: Double,
        currReading: Double
    ) {
        database.withTransaction {
            val config = dao.getConfigOnce() ?: ApartmentConfigEntity()
            val existing = dao.getPeriodsOnce().find { it.year == year && it.month == month }
            if (existing != null) {
                dao.updatePeriod(
                    existing.copy(
                        electricityTotalAmount = max(0.0, Formatters.round2(electricityAmount)),
                        prevMeterReading = max(0.0, prevReading),
                        currMeterReading = max(prevReading, currReading)
                    )
                )
                recalculateOpenPeriodSharesInternal(existing.id)
            } else {
                val rentPool = computeRentPoolForPeriod(config)
                val newPeriodId = dao.insertPeriod(
                    BillingPeriodEntity(
                        year = year,
                        month = month,
                        label = Formatters.formatMonthLabel(year, month),
                        rentPoolAmount = rentPool,
                        rentCycleTotalAmount = config.rentCycleAmount,
                        rentCycleMonths = config.rentCycleMonths,
                        electricityTotalAmount = max(0.0, Formatters.round2(electricityAmount)),
                        prevMeterReading = max(0.0, prevReading),
                        currMeterReading = max(prevReading, currReading),
                        isClosed = true
                    )
                )
                val activeResidents = dao.getActiveResidentsOnce()
                val count = max(1, activeResidents.size)
                val rentShare = Formatters.round2(rentPool / count)
                val elecShare = Formatters.round2(electricityAmount / count)
                dao.insertLedgerEntries(
                    activeResidents.map { r ->
                        ResidentLedgerEntity(
                            periodId = newPeriodId,
                            residentId = r.id,
                            currentRentShare = rentShare,
                            currentElectricityShare = elecShare,
                            paidRent = rentShare,
                            paidElectricity = elecShare
                        )
                    }
                )
            }
        }
    }

    private suspend fun recalculateOpenPeriodSharesInternal(periodId: Long) {
        val period = dao.getPeriodById(periodId) ?: return
        val activeResidents = dao.getActiveResidentsOnce()
        val existingLedgers = dao.getLedgerForPeriodOnce(periodId).associateBy { it.residentId }

        val activeIds = activeResidents.map { it.id }.toSet()
        // Keep ledgers for inactive residents only if they have payments/previous balance, else remove
        val count = max(1, activeResidents.size)
        val rentShare = Formatters.round2(period.rentPoolAmount / count)
        val elecShare = Formatters.round2(period.electricityTotalAmount / count)

        val updatedEntries = mutableListOf<ResidentLedgerEntity>()
        for (resident in activeResidents) {
            val prev = existingLedgers[resident.id]
            if (prev != null) {
                updatedEntries.add(
                    prev.copy(
                        currentRentShare = rentShare,
                        currentElectricityShare = elecShare,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                updatedEntries.add(
                    ResidentLedgerEntity(
                        periodId = periodId,
                        residentId = resident.id,
                        currentRentShare = rentShare,
                        currentElectricityShare = elecShare
                    )
                )
            }
        }
        // Preserve any inactive resident who still has balance/paid in this period
        for ((residentId, entry) in existingLedgers) {
            if (residentId !in activeIds && (entry.totalPaid > 0.0 || entry.previousTotalBalance > 0.0)) {
                updatedEntries.add(entry.copy(currentRentShare = 0.0, currentElectricityShare = 0.0))
            }
        }
        dao.deleteLedgerForPeriod(periodId)
        dao.insertLedgerEntries(updatedEntries)
    }

    suspend fun recordPayment(
        ledgerId: Long,
        residentName: String,
        amountBrought: Double,
        mode: PaymentAllocationMode,
        customRentPart: Double = 0.0,
        customElectricityPart: Double = 0.0,
        note: String = ""
    ) {
        database.withTransaction {
            val entry = dao.getLedgerById(ledgerId) ?: return@withTransaction
            val remRent = entry.remainingRent
            val remElec = entry.remainingElectricity

            val (addRent, addElec) = when (mode) {
                PaymentAllocationMode.FULL_REMAINING -> Pair(remRent, remElec)
                PaymentAllocationMode.AUTO_ELECTRICITY_FIRST -> {
                    val toElec = min(amountBrought, remElec)
                    val leftover = max(0.0, amountBrought - toElec)
                    val toRent = leftover // allow overpayment to reduce rent / credit
                    Pair(Formatters.round2(toRent), Formatters.round2(toElec))
                }
                PaymentAllocationMode.AUTO_RENT_FIRST -> {
                    val toRent = min(amountBrought, remRent)
                    val leftover = max(0.0, amountBrought - toRent)
                    val toElec = leftover
                    Pair(Formatters.round2(toRent), Formatters.round2(toElec))
                }
                PaymentAllocationMode.ELECTRICITY_ONLY -> Pair(0.0, Formatters.round2(amountBrought))
                PaymentAllocationMode.RENT_ONLY -> Pair(Formatters.round2(amountBrought), 0.0)
                PaymentAllocationMode.CUSTOM_SPLIT -> Pair(
                    Formatters.round2(max(0.0, customRentPart)),
                    Formatters.round2(max(0.0, customElectricityPart))
                )
            }

            val totalAdded = Formatters.round2(addRent + addElec)
            if (totalAdded <= 0.0) return@withTransaction

            dao.updateLedgerEntry(
                entry.copy(
                    paidRent = Formatters.round2(entry.paidRent + addRent),
                    paidElectricity = Formatters.round2(entry.paidElectricity + addElec),
                    note = if (note.isNotBlank()) note.trim() else entry.note,
                    updatedAt = System.currentTimeMillis()
                )
            )

            dao.insertTransaction(
                PaymentTransactionEntity(
                    periodId = entry.periodId,
                    residentId = entry.residentId,
                    residentNameSnapshot = residentName,
                    amountPaid = totalAdded,
                    allocatedToRent = addRent,
                    allocatedToElectricity = addElec,
                    note = note.trim()
                )
            )
        }
    }

    suspend fun batchMarkFullPaid(ledgerIds: List<Long>) {
        if (ledgerIds.isEmpty()) return
        database.withTransaction {
            val residents = dao.getResidentsOnce().associateBy { it.id }
            for (id in ledgerIds) {
                val entry = dao.getLedgerById(id) ?: continue
                val remRent = entry.remainingRent
                val remElec = entry.remainingElectricity
                val total = Formatters.round2(remRent + remElec)
                if (total <= 0.01) continue
                dao.updateLedgerEntry(
                    entry.copy(
                        paidRent = Formatters.round2(entry.paidRent + remRent),
                        paidElectricity = Formatters.round2(entry.paidElectricity + remElec),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                val name = residents[entry.residentId]?.name ?: "ساكن"
                dao.insertTransaction(
                    PaymentTransactionEntity(
                        periodId = entry.periodId,
                        residentId = entry.residentId,
                        residentNameSnapshot = name,
                        amountPaid = total,
                        allocatedToRent = remRent,
                        allocatedToElectricity = remElec,
                        note = "سداد كامل (دفعة جماعية)"
                    )
                )
            }
        }
    }

    suspend fun resetResidentPayment(ledgerId: Long) {
        database.withTransaction {
            val entry = dao.getLedgerById(ledgerId) ?: return@withTransaction
            dao.updateLedgerEntry(
                entry.copy(
                    paidRent = 0.0,
                    paidElectricity = 0.0,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun closePeriodAndRollover(
        currentPeriodId: Long,
        nextElectricityAmount: Double,
        nextPrevReading: Double,
        nextCurrReading: Double,
        chargeRentInNewPeriod: Boolean = true
    ): Long {
        return database.withTransaction {
            val currentPeriod = dao.getPeriodById(currentPeriodId)
                ?: throw IllegalStateException("Period not found")
            val config = dao.getConfigOnce() ?: ApartmentConfigEntity()
            val (nextYear, nextMonth) = Formatters.nextMonth(currentPeriod.year, currentPeriod.month)
            rolloverInternal(
                currentPeriod = currentPeriod,
                targetYear = nextYear,
                targetMonth = nextMonth,
                electricityAmount = nextElectricityAmount,
                prevReading = nextPrevReading,
                currReading = nextCurrReading,
                config = config,
                chargeRent = chargeRentInNewPeriod
            )
        }
    }

    private suspend fun rolloverInternal(
        currentPeriod: BillingPeriodEntity,
        targetYear: Int,
        targetMonth: Int,
        electricityAmount: Double,
        prevReading: Double,
        currReading: Double,
        config: ApartmentConfigEntity,
        chargeRent: Boolean = true
    ): Long {
        dao.updatePeriod(currentPeriod.copy(isClosed = true))

        val oldLedgers = dao.getLedgerForPeriodOnce(currentPeriod.id).associateBy { it.residentId }
        val activeResidents = dao.getActiveResidentsOnce()
        val count = max(1, activeResidents.size)

        val rentPool = if (chargeRent) computeRentPoolForPeriod(config) else 0.0
        val rentShare = Formatters.round2(rentPool / count)
        val elecShare = Formatters.round2(max(0.0, electricityAmount) / count)

        val newPeriodId = dao.insertPeriod(
            BillingPeriodEntity(
                year = targetYear,
                month = targetMonth,
                label = Formatters.formatMonthLabel(targetYear, targetMonth),
                rentPoolAmount = rentPool,
                rentCycleTotalAmount = config.rentCycleAmount,
                rentCycleMonths = config.rentCycleMonths,
                electricityTotalAmount = max(0.0, Formatters.round2(electricityAmount)),
                prevMeterReading = max(0.0, prevReading),
                currMeterReading = max(prevReading, currReading),
                isClosed = false
            )
        )

        val newEntries = activeResidents.map { resident ->
            val old = oldLedgers[resident.id]
            val carryRent = Formatters.round2(old?.remainingRent ?: 0.0)
            val carryElec = Formatters.round2(old?.remainingElectricity ?: 0.0)
            ResidentLedgerEntity(
                periodId = newPeriodId,
                residentId = resident.id,
                previousRentBalance = carryRent,
                previousElectricityBalance = carryElec,
                currentRentShare = rentShare,
                currentElectricityShare = elecShare
            )
        }
        dao.insertLedgerEntries(newEntries)
        return newPeriodId
    }
}
