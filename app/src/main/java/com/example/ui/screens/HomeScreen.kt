package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PaymentStatus
import com.example.data.local.ResidentLedgerWithDetails
import com.example.ui.MainUiState
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.SlateTertiary
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidContainer
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.StatusPartialContainer
import com.example.ui.theme.StatusUnpaid
import com.example.ui.theme.StatusUnpaidContainer
import com.example.utils.Formatters
import com.example.utils.WhatsAppHelper

@Composable
fun HomeScreen(
    state: MainUiState,
    onSelectPeriod: (Long) -> Unit,
    onRoomFilter: (Int?) -> Unit,
    onStatusFilter: (PaymentStatus?) -> Unit,
    onSearchQuery: (String) -> Unit,
    onToggleMultiSelect: () -> Unit,
    onToggleLedgerSelection: (Long) -> Unit,
    onSelectAllUnpaid: () -> Unit,
    onBatchPaySelected: () -> Unit,
    onOpenPaymentSheet: (ResidentLedgerWithDetails) -> Unit,
    onOpenElectricityDialog: () -> Unit,
    onOpenRolloverDialog: () -> Unit,
    onOpenWhatsAppQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPeriod = state.selectedPeriod
    val currency = state.config.currencySymbol
    var periodMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = state.config.apartmentName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.activeResidentsCount} ساكن • ${state.config.roomCount} غرف",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Period Selector
                    Box {
                        FilledTonalButton(
                            onClick = { periodMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("period_selector_button")
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = currentPeriod?.label ?: "الشهر الحالي",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Icon(Icons.Default.ExpandMore, contentDescription = "اختيار الشهر")
                        }
                        DropdownMenu(
                            expanded = periodMenuExpanded,
                            onDismissRequest = { periodMenuExpanded = false }
                        ) {
                            state.periods.forEach { period ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(period.label)
                                            if (period.isClosed) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "(مغلق)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        periodMenuExpanded = false
                                        onSelectPeriod(period.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Hero Summary Card
            item {
                HeroDashboardCard(state = state, currency = currency)
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = onOpenElectricityDialog,
                        label = { Text("تسجيل فاتورة الكهرباء") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("open_electricity_chip")
                    )
                    AssistChip(
                        onClick = onOpenWhatsAppQueue,
                        label = { Text("تذكير واتساب (${state.unpaidCount + state.partialCount})") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = StatusPaid,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("open_whatsapp_queue_chip")
                    )
                    AssistChip(
                        onClick = onToggleMultiSelect,
                        label = {
                            Text(if (state.isMultiSelectMode) "إلغاء التحديد" else "تحديد الدافعين")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("toggle_multiselect_chip")
                    )
                    if (currentPeriod != null && !currentPeriod.isClosed) {
                        AssistChip(
                            onClick = onOpenRolloverDialog,
                            label = { Text("إغلاق وترحيل الشهر") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FastForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("open_rollover_chip")
                        )
                    }
                }
            }

            // Multi-select Batch Action Banner
            item {
                AnimatedVisibility(visible = state.isMultiSelectMode) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = onSelectAllUnpaid) {
                                    Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تحديد الكل")
                                }
                                Text(
                                    "${state.selectedLedgerIds.size} محدد",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = onToggleMultiSelect) {
                                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                                }
                                Button(
                                    onClick = onBatchPaySelected,
                                    enabled = state.selectedLedgerIds.isNotEmpty(),
                                    modifier = Modifier.testTag("batch_pay_confirm_button")
                                ) {
                                    Text("اعتماد سداد الكل")
                                }
                            }
                        }
                    }
                }
            }

            // Search & Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQuery,
                        placeholder = { Text("ابحث عن اسم الساكن...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedRoomFilter == null,
                            onClick = { onRoomFilter(null) },
                            label = { Text("كل الغرف") }
                        )
                        for (room in 1..state.config.roomCount) {
                            FilterChip(
                                selected = state.selectedRoomFilter == room,
                                onClick = {
                                    onRoomFilter(if (state.selectedRoomFilter == room) null else room)
                                },
                                label = { Text("غرفة $room") }
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = state.selectedStatusFilter == PaymentStatus.UNPAID,
                            onClick = {
                                onStatusFilter(
                                    if (state.selectedStatusFilter == PaymentStatus.UNPAID) null else PaymentStatus.UNPAID
                                )
                            },
                            label = { Text("لم يدفع (${state.unpaidCount})") }
                        )
                        FilterChip(
                            selected = state.selectedStatusFilter == PaymentStatus.PARTIAL,
                            onClick = {
                                onStatusFilter(
                                    if (state.selectedStatusFilter == PaymentStatus.PARTIAL) null else PaymentStatus.PARTIAL
                                )
                            },
                            label = { Text("دفع جزئي (${state.partialCount})") }
                        )
                        FilterChip(
                            selected = state.selectedStatusFilter == PaymentStatus.PAID,
                            onClick = {
                                onStatusFilter(
                                    if (state.selectedStatusFilter == PaymentStatus.PAID) null else PaymentStatus.PAID
                                )
                            },
                            label = { Text("مدفوع (${state.paidCount})") }
                        )
                    }
                }
            }

            // Residents Ledger List
            if (state.filteredLedgerItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "لا يوجد سكان مطابقون للتصفية الحالية",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "أضف سكاناً من تبويب السكان أو قم بتغيير خيارات التصفية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.filteredLedgerItems, key = { it.ledger.id }) { item ->
                    ResidentLedgerCard(
                        item = item,
                        currency = currency,
                        rentCycleMonths = state.config.rentCycleMonths,
                        collectMonthly = state.config.collectRentMonthlyInstallment,
                        isMultiSelectMode = state.isMultiSelectMode,
                        isSelected = item.ledger.id in state.selectedLedgerIds,
                        onToggleSelect = { onToggleLedgerSelection(item.ledger.id) },
                        onRecordPayment = { onOpenPaymentSheet(item) },
                        onSendWhatsApp = {
                            if (currentPeriod != null) {
                                val msg = WhatsAppHelper.buildResidentReminderMessage(
                                    item = item,
                                    period = currentPeriod,
                                    config = state.config
                                )
                                WhatsAppHelper.openWhatsAppForResident(
                                    context,
                                    item.resident.phoneNumber,
                                    msg
                                )
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HeroDashboardCard(
    state: MainUiState,
    currency: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.collectionProgress,
        animationSpec = spring(stiffness = 300f),
        label = "progress"
    )
    val period = state.selectedPeriod

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(EmeraldPrimaryDark, EmeraldPrimary, SlateTertiary)
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.height * 0.65f,
                        center = Offset(size.width * 0.1f, size.height * 0.15f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.height * 0.45f,
                        center = Offset(size.width * 0.92f, size.height * 0.85f)
                    )
                }
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "المتبقي للتحصيل (${period?.label ?: ""})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = Formatters.formatMoney(state.totalRemaining, currency),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "نسبة السداد",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = Color(0xFF34D399),
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المحصّل: ${Formatters.formatMoney(state.totalPaid, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
                    )
                    Text(
                        text = "الإجمالي المطلوب: ${Formatters.formatMoney(state.totalDue, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

                // Separated Rent vs Electricity vs Carry-Over KPIs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroMiniTile(
                        title = "نصيب السكن للفرد",
                        primaryValue = Formatters.formatMoney(
                            if (state.config.collectRentMonthlyInstallment) state.monthlyRentPerPerson
                            else state.cycleRentPerPerson,
                            currency
                        ),
                        subtitle = if (state.config.rentCycleMonths > 1) {
                            if (state.config.collectRentMonthlyInstallment) {
                                "دورة ${state.config.rentCycleMonths} أشهر: ${Formatters.formatMoney(state.cycleRentPerPerson)}"
                            } else {
                                "شهرياً: ${Formatters.formatMoney(state.monthlyRentPerPerson, currency)}"
                            }
                        } else {
                            "إجمالي: ${Formatters.formatMoney(state.config.rentCycleAmount)}"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HeroMiniTile(
                        title = "نصيب الكهرباء للفرد",
                        primaryValue = Formatters.formatMoney(state.electricityPerPerson, currency),
                        subtitle = "الفاتورة: ${Formatters.formatMoney(period?.electricityTotalAmount ?: 0.0)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.totalPreviousArrears > 0.05) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "متبقي مرحّل من الأشهر السابقة:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFEF3C7)
                                )
                            }
                            Text(
                                Formatters.formatMoney(state.totalPreviousArrears, currency),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMiniTile(
    title: String,
    primaryValue: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ResidentLedgerCard(
    item: ResidentLedgerWithDetails,
    currency: String,
    rentCycleMonths: Int,
    collectMonthly: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onRecordPayment: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    val ledger = item.ledger
    val resident = item.resident
    val status = ledger.paymentStatus

    val (statusText, statusColor, statusBg) = when (status) {
        PaymentStatus.PAID -> Triple("مدفوع بالكامل", StatusPaid, StatusPaidContainer)
        PaymentStatus.PARTIAL -> Triple("دفع جزئي", StatusPartial, StatusPartialContainer)
        PaymentStatus.UNPAID -> Triple("لم يدفع", StatusUnpaid, StatusUnpaidContainer)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isMultiSelectMode) onToggleSelect() else onRecordPayment()
            }
            .testTag("resident_card_${resident.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Avatar + Name + Room + Status + Remaining Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isMultiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = resident.name.take(2),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }

                    Column {
                        Text(
                            text = resident.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "غرفة ${resident.roomNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = statusBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (ledger.totalRemaining <= 0.05) "خالص" else Formatters.formatMoney(ledger.totalRemaining, currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (ledger.totalRemaining <= 0.05) StatusPaid else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (ledger.totalRemaining <= 0.05) "تم السداد" else "المتبقي عليه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Carried-over Previous Balance Alert
            if (ledger.previousTotalBalance > 0.05) {
                Surface(
                    color = StatusPartialContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↩️ متبقي مرحّل من الشهر السابق:",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusPartial
                        )
                        Text(
                            text = Formatters.formatMoney(ledger.previousTotalBalance, currency),
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusPartial,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Separated Rent & Electricity Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rent Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("السكن", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = "متبقي: ${Formatters.formatMoney(ledger.remainingRent, currency)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (ledger.remainingRent <= 0.05) StatusPaid else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "حصة: ${Formatters.formatMoney(ledger.currentRentShare)}" +
                                if (ledger.previousRentBalance > 0.05) " + سابق ${Formatters.formatMoney(ledger.previousRentBalance)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (rentCycleMonths > 1 && !collectMonthly) {
                            Text(
                                text = "شهرياً: ${Formatters.formatMoney(ledger.currentRentShare / rentCycleMonths)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Electricity Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("الكهرباء", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = "متبقي: ${Formatters.formatMoney(ledger.remainingElectricity, currency)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (ledger.remainingElectricity <= 0.05) StatusPaid else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "حصة: ${Formatters.formatMoney(ledger.currentElectricityShare)}" +
                                if (ledger.previousElectricityBalance > 0.05) " + سابق ${Formatters.formatMoney(ledger.previousElectricityBalance)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (ledger.paidElectricity > 0.05) {
                            Text(
                                text = "مدفوع: ${Formatters.formatMoney(ledger.paidElectricity)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusPaid
                            )
                        }
                    }
                }
            }

            // Action Buttons Row
            if (!isMultiSelectMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onRecordPayment,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("pay_button_${resident.id}")
                    ) {
                        Icon(
                            if (status == PaymentStatus.PAID) Icons.Default.CheckCircle else Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (status == PaymentStatus.PAID) "تعديل / تفاصيل" else "تسجيل دفعة"
                        )
                    }
                    OutlinedButton(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusPaid),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("whatsapp_button_${resident.id}")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "تذكير واتساب",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("واتساب")
                    }
                }
            }
        }
    }
}
