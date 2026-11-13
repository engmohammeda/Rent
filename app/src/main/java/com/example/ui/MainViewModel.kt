package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ApartmentConfigEntity
import com.example.data.local.BillingPeriodEntity
import com.example.data.local.PaymentAllocationMode
import com.example.data.local.PaymentStatus
import com.example.data.local.PaymentTransactionEntity
import com.example.data.local.ResidentEntity
import com.example.data.local.ResidentLedgerEntity
import com.example.data.local.ResidentLedgerWithDetails
import com.example.data.local.SakanDatabase
import com.example.data.repository.SakanRepository
import com.example.notifications.ReminderScheduler
import com.example.utils.Formatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class ElectricityAnalytics(
    val currentPeriod: BillingPeriodEntity? = null,
    val previousPeriod: BillingPeriodEntity? = null,
    val costDiff: Double = 0.0,
    val costDiffPercent: Double = 0.0,
    val kwhDiff: Double = 0.0,
    val averageMonthlyCost: Double = 0.0,
    val maxPeriod: BillingPeriodEntity? = null,
    val minPeriod: BillingPeriodEntity? = null,
    val chartPeriods: List<BillingPeriodEntity> = emptyList()
)

data class RoomSummary(
    val roomNumber: Int,
    val residentCount: Int,
    val remainingTotal: Double
)

data class MainUiState(
    val isLoading: Boolean = true,
    val config: ApartmentConfigEntity = ApartmentConfigEntity(),
    val residents: List<ResidentEntity> = emptyList(),
    val periods: List<BillingPeriodEntity> = emptyList(),
    val selectedPeriod: BillingPeriodEntity? = null,
    val allLedgerItems: List<ResidentLedgerWithDetails> = emptyList(),
    val filteredLedgerItems: List<ResidentLedgerWithDetails> = emptyList(),
    val allTransactions: List<PaymentTransactionEntity> = emptyList(),
    val selectedRoomFilter: Int? = null,
    val selectedStatusFilter: PaymentStatus? = null,
    val searchQuery: String = "",
    val isMultiSelectMode: Boolean = false,
    val selectedLedgerIds: Set<Long> = emptySet(),
    val analytics: ElectricityAnalytics = ElectricityAnalytics(),
    val snackbarMessage: String? = null
) {
    val activeResidentsCount: Int
        get() = max(1, residents.count { it.isActive })

    val totalDue: Double
        get() = allLedgerItems.sumOf { it.ledger.totalDue }

    val totalPaid: Double
        get() = allLedgerItems.sumOf { it.ledger.totalPaid }

    val totalRemaining: Double
        get() = allLedgerItems.sumOf { it.ledger.totalRemaining }

    val totalAdvanceCredits: Double
        get() = allLedgerItems.sumOf { it.ledger.totalAdvanceCredit }

    val totalPreviousArrears: Double
        get() = allLedgerItems.sumOf { it.ledger.previousArrearsDebt }

    val paidOrAdvanceCount: Int
        get() = allLedgerItems.count {
            it.ledger.paymentStatus == PaymentStatus.PAID ||
                it.ledger.paymentStatus == PaymentStatus.ADVANCE_CREDIT
        }

    val advanceCreditCount: Int
        get() = allLedgerItems.count { it.ledger.paymentStatus == PaymentStatus.ADVANCE_CREDIT }

    val paidCount: Int
        get() = allLedgerItems.count { it.ledger.paymentStatus == PaymentStatus.PAID }

    val partialCount: Int
        get() = allLedgerItems.count { it.ledger.paymentStatus == PaymentStatus.PARTIAL }

    val unpaidCount: Int
        get() = allLedgerItems.count { it.ledger.paymentStatus == PaymentStatus.UNPAID }

    val collectionProgress: Float
        get() = if (totalDue > 0.01) (totalPaid / totalDue).toFloat().coerceIn(0f, 1f) else 1f

    val cycleRentPerPerson: Double
        get() = config.rentCycleAmount / activeResidentsCount

    val monthlyRentPerPerson: Double
        get() = (config.rentCycleAmount / max(1, config.rentCycleMonths)) / activeResidentsCount

    val electricityPerPerson: Double
        get() = (selectedPeriod?.electricityTotalAmount ?: 0.0) / activeResidentsCount

    val waterPerPerson: Double
        get() = if (config.waterIncludedInRent) 0.0 else (selectedPeriod?.waterTotalAmount ?: 0.0) / activeResidentsCount

    val roomSummaries: List<RoomSummary>
        get() = (1..max(1, config.roomCount)).map { room ->
            val itemsInRoom = allLedgerItems.filter { it.resident.roomNumber == room }
            RoomSummary(
                roomNumber = room,
                residentCount = itemsInRoom.size,
                remainingTotal = itemsInRoom.sumOf { it.ledger.totalRemaining }
            )
        }
}

private data class FilterState(
    val selectedPeriodId: Long? = null,
    val roomFilter: Int? = null,
    val statusFilter: PaymentStatus? = null,
    val searchQuery: String = "",
    val isMultiSelectMode: Boolean = false,
    val selectedLedgerIds: Set<Long> = emptySet(),
    val snackbarMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: SakanRepository,
    private val appContext: Context
) : ViewModel() {

    private val filterState = MutableStateFlow(FilterState())

    private val selectedPeriodIdFlow = combine(
        repository.periodsFlow,
        filterState
    ) { periods, filters ->
        val selectedId = filters.selectedPeriodId
        if (selectedId != null && periods.any { it.id == selectedId }) {
            selectedId
        } else {
            periods.firstOrNull { !it.isClosed }?.id ?: periods.firstOrNull()?.id
        }
    }

    private val ledgerForSelectedPeriodFlow = selectedPeriodIdFlow.flatMapLatest { periodId ->
        if (periodId == null) flowOf(emptyList())
        else repository.observeLedgerForPeriod(periodId)
    }

    val uiState: StateFlow<MainUiState> = combine(
        repository.configFlow,
        repository.residentsFlow,
        repository.periodsFlow,
        ledgerForSelectedPeriodFlow,
        repository.allTransactionsFlow,
        filterState
    ) { values ->
        val config = (values[0] as ApartmentConfigEntity?) ?: ApartmentConfigEntity()
        @Suppress("UNCHECKED_CAST")
        val residents = values[1] as List<ResidentEntity>
        @Suppress("UNCHECKED_CAST")
        val periods = values[2] as List<BillingPeriodEntity>
        @Suppress("UNCHECKED_CAST")
        val ledgers = values[3] as List<ResidentLedgerEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[4] as List<PaymentTransactionEntity>
        val filters = values[5] as FilterState

        val selectedPeriod = periods.find { it.id == filters.selectedPeriodId }
            ?: periods.firstOrNull { !it.isClosed }
            ?: periods.firstOrNull()

        val residentMap = residents.associateBy { it.id }
        val joined = ledgers.mapNotNull { ledger ->
            val resident = residentMap[ledger.residentId] ?: return@mapNotNull null
            ResidentLedgerWithDetails(ledger = ledger, resident = resident)
        }.sortedWith(compareBy({ it.resident.roomNumber }, { it.resident.sortOrder }, { it.resident.id }))

        val filtered = joined.filter { item ->
            val matchesRoom = filters.roomFilter == null || item.resident.roomNumber == filters.roomFilter
            val matchesStatus = filters.statusFilter == null || item.ledger.paymentStatus == filters.statusFilter
            val matchesQuery = filters.searchQuery.isBlank() ||
                item.resident.name.contains(filters.searchQuery, ignoreCase = true)
            matchesRoom && matchesStatus && matchesQuery
        }

        val analytics = buildAnalytics(periods, selectedPeriod)

        MainUiState(
            isLoading = false,
            config = config,
            residents = residents,
            periods = periods,
            selectedPeriod = selectedPeriod,
            allLedgerItems = joined,
            filteredLedgerItems = filtered,
            allTransactions = transactions,
            selectedRoomFilter = filters.roomFilter,
            selectedStatusFilter = filters.statusFilter,
            searchQuery = filters.searchQuery,
            isMultiSelectMode = filters.isMultiSelectMode,
            selectedLedgerIds = filters.selectedLedgerIds,
            analytics = analytics,
            snackbarMessage = filters.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            val cfg = uiState.value.config
            ReminderScheduler.scheduleRecurringReminders(
                appContext,
                cfg.reminderIntervalHours,
                cfg.notificationsEnabled
            )
        }
    }

    private fun buildAnalytics(
        periods: List<BillingPeriodEntity>,
        current: BillingPeriodEntity?
    ): ElectricityAnalytics {
        if (periods.isEmpty()) return ElectricityAnalytics()
        val chronological = periods.sortedWith(compareBy({ it.year }, { it.month }))
        val target = current ?: chronological.last()
        val idx = chronological.indexOfFirst { it.id == target.id }
        val prev = if (idx > 0) chronological[idx - 1] else null

        val costDiff = if (prev != null) target.electricityTotalAmount - prev.electricityTotalAmount else 0.0
        val costDiffPercent = if (prev != null && prev.electricityTotalAmount > 0.01) {
            (costDiff / prev.electricityTotalAmount) * 100.0
        } else 0.0
        val kwhDiff = if (prev != null) target.consumptionKwh - prev.consumptionKwh else 0.0

        val nonZeroPeriods = chronological.filter { it.electricityTotalAmount > 0.0 }
        val avg = if (nonZeroPeriods.isNotEmpty()) {
            nonZeroPeriods.map { it.electricityTotalAmount }.average()
        } else 0.0
        val maxP = nonZeroPeriods.maxByOrNull { it.electricityTotalAmount }
        val minP = nonZeroPeriods.minByOrNull { it.electricityTotalAmount }

        return ElectricityAnalytics(
            currentPeriod = target,
            previousPeriod = prev,
            costDiff = costDiff,
            costDiffPercent = costDiffPercent,
            kwhDiff = kwhDiff,
            averageMonthlyCost = avg,
            maxPeriod = maxP,
            minPeriod = minP,
            chartPeriods = chronological.takeLast(6)
        )
    }

    fun selectPeriod(periodId: Long) {
        filterState.update { it.copy(selectedPeriodId = periodId, selectedLedgerIds = emptySet(), isMultiSelectMode = false) }
    }

    fun setRoomFilter(room: Int?) {
        filterState.update { it.copy(roomFilter = room) }
    }

    fun setStatusFilter(status: PaymentStatus?) {
        filterState.update { it.copy(statusFilter = status) }
    }

    fun setSearchQuery(query: String) {
        filterState.update { it.copy(searchQuery = query) }
    }

    fun toggleMultiSelectMode() {
        filterState.update {
            it.copy(
                isMultiSelectMode = !it.isMultiSelectMode,
                selectedLedgerIds = emptySet()
            )
        }
    }

    fun toggleLedgerSelection(ledgerId: Long) {
        filterState.update { state ->
            val next = if (ledgerId in state.selectedLedgerIds) {
                state.selectedLedgerIds - ledgerId
            } else {
                state.selectedLedgerIds + ledgerId
            }
            state.copy(selectedLedgerIds = next)
        }
    }

    fun selectAllUnpaid() {
        val unpaidIds = uiState.value.filteredLedgerItems
            .filter { it.ledger.totalRemaining > 0.05 }
            .map { it.ledger.id }
            .toSet()
        filterState.update { it.copy(selectedLedgerIds = unpaidIds) }
    }

    fun batchPaySelected() {
        val ids = filterState.value.selectedLedgerIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.batchMarkFullPaid(ids)
            filterState.update {
                it.copy(
                    isMultiSelectMode = false,
                    selectedLedgerIds = emptySet(),
                    snackbarMessage = "تم اعتماد سداد ${ids.size} سكان بالكامل"
                )
            }
        }
    }

    fun recordPayment(
        ledgerId: Long,
        residentName: String,
        amountBrought: Double,
        mode: PaymentAllocationMode,
        customRentPart: Double = 0.0,
        customElectricityPart: Double = 0.0,
        customWaterPart: Double = 0.0,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.recordPayment(
                ledgerId = ledgerId,
                residentName = residentName,
                amountBrought = amountBrought,
                mode = mode,
                customRentPart = customRentPart,
                customElectricityPart = customElectricityPart,
                customWaterPart = customWaterPart,
                note = note
            )
            showMessage("تم تسجيل دفعة $residentName بنجاح")
        }
    }

    fun resetResidentPayment(ledgerId: Long, residentName: String) {
        viewModelScope.launch {
            repository.resetResidentPayment(ledgerId)
            showMessage("تم تصفير مدفوعات $residentName لهذه الفترة")
        }
    }

    fun updateCurrentUtilities(
        periodId: Long,
        electricityAmount: Double,
        waterAmount: Double,
        prevReading: Double,
        currReading: Double
    ) {
        viewModelScope.launch {
            repository.updatePeriodUtilities(periodId, electricityAmount, waterAmount, prevReading, currReading)
            showMessage("تم تحديث الفواتير وتقسيمها على السكان")
        }
    }

    fun addOrUpdateHistoricalElectricity(
        year: Int,
        month: Int,
        electricityAmount: Double,
        waterAmount: Double,
        prevReading: Double,
        currReading: Double
    ) {
        viewModelScope.launch {
            repository.addOrUpdateHistoricalElectricityPeriod(
                year = year,
                month = month,
                electricityAmount = electricityAmount,
                waterAmount = waterAmount,
                prevReading = prevReading,
                currReading = currReading
            )
            showMessage("تم حفظ بيانات شهر ${Formatters.formatMonthLabel(year, month)}")
        }
    }

    fun closePeriodAndRollover(
        currentPeriodId: Long,
        nextElectricityAmount: Double,
        nextWaterAmount: Double,
        nextPrevReading: Double,
        nextCurrReading: Double,
        chargeRentInNewPeriod: Boolean
    ) {
        viewModelScope.launch {
            val newPeriodId = repository.closePeriodAndRollover(
                currentPeriodId = currentPeriodId,
                nextElectricityAmount = nextElectricityAmount,
                nextWaterAmount = nextWaterAmount,
                nextPrevReading = nextPrevReading,
                nextCurrReading = nextCurrReading,
                chargeRentInNewPeriod = chargeRentInNewPeriod
            )
            filterState.update {
                it.copy(
                    selectedPeriodId = newPeriodId,
                    snackbarMessage = "تم إغلاق الشهر وترحيل المتبقي والأرصدة المقدمة للشهر الجديد"
                )
            }
        }
    }

    fun generateResidentsAuto(roomCount: Int, totalResidents: Int, replaceExisting: Boolean) {
        viewModelScope.launch {
            repository.generateResidentsAuto(roomCount, totalResidents, replaceExisting)
            showMessage("تم توليد $totalResidents ساكنين وتحديث الأنصبة")
        }
    }

    fun addResident(name: String, roomNumber: Int, phoneNumber: String) {
        viewModelScope.launch {
            repository.addResident(name, roomNumber, phoneNumber)
            showMessage("تمت إضافة $name")
        }
    }

    fun updateResident(resident: ResidentEntity) {
        viewModelScope.launch {
            repository.updateResident(resident)
            showMessage("تم تحديث بيانات ${resident.name}")
        }
    }

    fun deleteResident(resident: ResidentEntity) {
        viewModelScope.launch {
            repository.deleteResident(resident)
            showMessage("تم حذف ${resident.name}")
        }
    }

    fun updateConfig(config: ApartmentConfigEntity, applyRentToOpenPeriod: Boolean = true) {
        viewModelScope.launch {
            repository.updateConfig(config, applyRentToOpenPeriod)
            ReminderScheduler.scheduleRecurringReminders(
                appContext,
                config.reminderIntervalHours,
                config.notificationsEnabled
            )
            showMessage("تم حفظ الإعدادات وتحديث جدولة التنبيهات")
        }
    }

    fun triggerTestNotification() {
        val state = uiState.value
        val unpaid = state.unpaidCount + state.partialCount
        val remaining = Formatters.formatMoney(state.totalRemaining, state.config.currencySymbol)
        val msg = if (unpaid > 0) {
            "متبقي $remaining لدى $unpaid ساكنين في ${state.selectedPeriod?.label ?: "الشهر الحالي"}. اضغط للمطالبة عبر واتساب."
        } else {
            "جميع مستحقات ${state.selectedPeriod?.label ?: "الشهر الحالي"} مسددة بالكامل!"
        }
        ReminderScheduler.postNotification(
            appContext,
            "تذكير ${state.config.apartmentName}",
            msg
        )
        showMessage("تم إرسال إشعار تجريبي")
    }

    fun clearSnackbar() {
        filterState.update { it.copy(snackbarMessage = null) }
    }

    private fun showMessage(msg: String) {
        filterState.update { it.copy(snackbarMessage = msg) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = SakanDatabase.getInstance(context.applicationContext)
            val repo = SakanRepository(db)
            return MainViewModel(repo, context.applicationContext) as T
        }
    }
}
