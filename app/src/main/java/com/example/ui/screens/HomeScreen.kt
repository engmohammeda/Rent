package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PaymentStatus
import com.example.data.local.ResidentLedgerWithDetails
import com.example.ui.MainUiState
import com.example.ui.theme.EzbahGold
import com.example.ui.theme.EzbahGoldLight
import com.example.ui.theme.EzbahTeal
import com.example.ui.theme.EzbahTealDeep
import com.example.ui.theme.StatusAdvance
import com.example.ui.theme.StatusAdvanceContainer
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
    onOpenUtilitiesDialog: () -> Unit,
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Executive Brand Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = "شعار عزبة"
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = state.config.apartmentName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.activeResidentsCount} ساكن • ${state.config.roomCount} غرف • " +
                                    if (state.config.waterIncludedInRent) "الماء مشمول" else "ماء منفصل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box {
                        Surface(
                            onClick = { periodMenuExpanded = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("period_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = currentPeriod?.label ?: "الشهر الحالي",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = "اختيار الشهر",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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

            // Hero Executive Card
            item {
                HeroEzbahCard(state = state, currency = currency)
            }

            // 2x2 Bento Quick Action Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoActionTile(
                            title = if (state.config.waterIncludedInRent) "فاتورة الكهرباء" else "الكهرباء والماء",
                            subtitle = Formatters.formatMoney(currentPeriod?.electricityTotalAmount ?: 0.0, currency),
                            icon = Icons.Default.ElectricBolt,
                            accent = EzbahGold,
                            onClick = onOpenUtilitiesDialog,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_electricity_chip")
                        )
                        BentoActionTile(
                            title = "تذكير واتساب",
                            subtitle = "${state.unpaidCount + state.partialCount} متبقي عليهم",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            accent = StatusPaid,
                            onClick = onOpenWhatsAppQueue,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_whatsapp_queue_chip")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoActionTile(
                            title = if (state.isMultiSelectMode) "إلغاء التحديد" else "تحديد الدافعين",
                            subtitle = if (state.isMultiSelectMode) "${state.selectedLedgerIds.size} محدد" else "سداد جماعي سريع",
                            icon = Icons.Default.CheckBox,
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = onToggleMultiSelect,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_multiselect_chip")
                        )
                        BentoActionTile(
                            title = "ترحيل للشهر التالي",
                            subtitle = if (currentPeriod?.isClosed == true) "الفترة مغلقة" else "حفظ المتبقي والمقدم",
                            icon = Icons.Default.FastForward,
                            accent = MaterialTheme.colorScheme.tertiary,
                            enabled = currentPeriod != null && !currentPeriod.isClosed,
                            onClick = onOpenRolloverDialog,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_rollover_chip")
                        )
                    }
                }
            }

            // Room Overview Strip (interactive room filter)
            if (state.roomSummaries.size > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.roomSummaries.forEach { rm ->
                            val selected = state.selectedRoomFilter == rm.roomNumber
                            Surface(
                                onClick = {
                                    onRoomFilter(if (selected) null else rm.roomNumber)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            "غرفة ${rm.roomNumber} (${rm.residentCount})",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            if (rm.remainingTotal <= 0.05) "مكتملة السداد"
                                            else "متبقي ${Formatters.formatMoney(rm.remainingTotal, currency)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (rm.remainingTotal <= 0.05) StatusPaid else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Multi-select Batch Action Banner
            item {
                AnimatedVisibility(visible = state.isMultiSelectMode) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
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

            // Search & Status Filters
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedStatusFilter == null,
                            onClick = { onStatusFilter(null) },
                            label = { Text("الكل (${state.allLedgerItems.size})") }
                        )
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
                            label = { Text("جزئي (${state.partialCount})") }
                        )
                        FilterChip(
                            selected = state.selectedStatusFilter == PaymentStatus.PAID,
                            onClick = {
                                onStatusFilter(
                                    if (state.selectedStatusFilter == PaymentStatus.PAID) null else PaymentStatus.PAID
                                )
                            },
                            label = { Text("خالص (${state.paidCount})") }
                        )
                        if (state.advanceCreditCount > 0) {
                            FilterChip(
                                selected = state.selectedStatusFilter == PaymentStatus.ADVANCE_CREDIT,
                                onClick = {
                                    onStatusFilter(
                                        if (state.selectedStatusFilter == PaymentStatus.ADVANCE_CREDIT) null else PaymentStatus.ADVANCE_CREDIT
                                    )
                                },
                                label = { Text("مدفوع مقدماً (${state.advanceCreditCount})") }
                            )
                        }
                    }
                }
            }

            // Residents List
            if (state.filteredLedgerItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("لا يوجد سكان مطابقون للبحث", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                items(state.filteredLedgerItems, key = { it.ledger.id }) { item ->
                    ResidentEzbahCard(
                        item = item,
                        currency = currency,
                        rentCycleMonths = state.config.rentCycleMonths,
                        waterIncludedInRent = state.config.waterIncludedInRent,
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
private fun HeroEzbahCard(
    state: MainUiState,
    currency: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.collectionProgress,
        animationSpec = spring(stiffness = 280f),
        label = "progress"
    )
    val period = state.selectedPeriod

    Surface(
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(EzbahTealDeep, EzbahTeal, Color(0xFF153B47))
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = EzbahGoldLight.copy(alpha = 0.08f),
                        radius = size.height * 0.55f,
                        center = Offset(size.width * 0.08f, size.height * 0.12f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.height * 0.42f,
                        center = Offset(size.width * 0.94f, size.height * 0.88f)
                    )
                }
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "المتبقي للتحصيل • ${period?.label ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        Text(
                            text = Formatters.formatMoney(state.totalRemaining, currency),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = EzbahGoldLight.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = EzbahGoldLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.paidOrAdvanceCount}/${state.allLedgerItems.size} خالص",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
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
                    color = EzbahGoldLight,
                    trackColor = Color.White.copy(alpha = 0.18f),
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
                        text = "الإجمالي: ${Formatters.formatMoney(state.totalDue, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                // Split Per-Person Costs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroStatChip(
                        title = "السكن للفرد",
                        value = Formatters.formatMoney(state.monthlyRentPerPerson, currency),
                        sub = if (state.config.rentCycleMonths > 1) {
                            "كل ${state.config.rentCycleMonths} أشهر: ${Formatters.formatMoney(state.cycleRentPerPerson)}"
                        } else {
                            "الإجمالي: ${Formatters.formatMoney(state.config.rentCycleAmount)}"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatChip(
                        title = "الكهرباء للفرد",
                        value = Formatters.formatMoney(state.electricityPerPerson, currency),
                        sub = "الفاتورة: ${Formatters.formatMoney(period?.electricityTotalAmount ?: 0.0)}",
                        modifier = Modifier.weight(1f)
                    )
                    if (!state.config.waterIncludedInRent) {
                        HeroStatChip(
                            title = "الماء للفرد",
                            value = Formatters.formatMoney(state.waterPerPerson, currency),
                            sub = "الفاتورة: ${Formatters.formatMoney(period?.waterTotalAmount ?: 0.0)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (state.totalPreviousArrears > 0.05 || state.totalAdvanceCredits > 0.05) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.totalPreviousArrears > 0.05) {
                            Surface(
                                color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("متبقي سابق", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFEF3C7))
                                    }
                                    Text(
                                        Formatters.formatMoney(state.totalPreviousArrears, currency),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        if (state.totalAdvanceCredits > 0.05) {
                            Surface(
                                color = Color(0xFF38BDF8).copy(alpha = 0.22f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFBAE6FD), modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("رصيد مقدم", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE0F2FE))
                                    }
                                    Text(
                                        Formatters.formatMoney(state.totalAdvanceCredits, currency),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.11f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BentoActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accent.copy(alpha = 0.13f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ResidentEzbahCard(
    item: ResidentLedgerWithDetails,
    currency: String,
    rentCycleMonths: Int,
    waterIncludedInRent: Boolean,
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
        PaymentStatus.ADVANCE_CREDIT -> Triple("مدفوع مقدماً", StatusAdvance, StatusAdvanceContainer)
        PaymentStatus.PAID -> Triple("خالص", StatusPaid, StatusPaidContainer)
        PaymentStatus.PARTIAL -> Triple("دفع جزئي", StatusPartial, StatusPartialContainer)
        PaymentStatus.UNPAID -> Triple("لم يدفع", StatusUnpaid, StatusUnpaidContainer)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isMultiSelectMode) onToggleSelect() else onRecordPayment() }
            .testTag("resident_card_${resident.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (isMultiSelectMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(statusBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = resident.name.take(2),
                                style = MaterialTheme.typography.titleSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
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
                            Surface(color = statusBg, shape = RoundedCornerShape(8.dp)) {
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
                    when {
                        ledger.totalRemaining <= 0.05 && ledger.totalAdvanceCredit > 0.05 -> {
                            Text(
                                text = "+${Formatters.formatMoney(ledger.totalAdvanceCredit, currency)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusAdvance,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "رصيد مقدم",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusAdvance
                            )
                        }
                        ledger.totalRemaining <= 0.05 -> {
                            Text(
                                text = "خالص",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusPaid,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تم السداد",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = Formatters.formatMoney(ledger.totalRemaining, currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "المتبقي عليه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Previous Arrears OR Previous Advance Credit notice
            if (ledger.previousArrearsDebt > 0.05) {
                Surface(
                    color = StatusPartialContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("متبقي مرحّل من السابق:", style = MaterialTheme.typography.labelMedium, color = StatusPartial)
                        Text(
                            Formatters.formatMoney(ledger.previousArrearsDebt, currency),
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusPartial,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (ledger.totalAdvanceCredit > 0.05 && ledger.totalRemaining > 0.05) {
                Surface(
                    color = StatusAdvanceContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("له رصيد سكن مدفوع مقدماً:", style = MaterialTheme.typography.labelMedium, color = StatusAdvance)
                        Text(
                            Formatters.formatMoney(ledger.totalAdvanceCredit, currency),
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusAdvance,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Separated Utilities Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniBreakdownBox(
                    icon = Icons.Default.Home,
                    iconColor = MaterialTheme.colorScheme.primary,
                    label = "السكن",
                    remaining = ledger.remainingRent,
                    credit = ledger.creditRent,
                    shareText = "قسط: ${Formatters.formatMoney(ledger.currentRentShare)}",
                    currency = currency,
                    modifier = Modifier.weight(1f)
                )
                MiniBreakdownBox(
                    icon = Icons.Default.ElectricBolt,
                    iconColor = EzbahGold,
                    label = "الكهرباء",
                    remaining = ledger.remainingElectricity,
                    credit = ledger.creditElectricity,
                    shareText = "حصة: ${Formatters.formatMoney(ledger.currentElectricityShare)}",
                    currency = currency,
                    modifier = Modifier.weight(1f)
                )
                if (!waterIncludedInRent) {
                    MiniBreakdownBox(
                        icon = Icons.Default.WaterDrop,
                        iconColor = StatusAdvance,
                        label = "الماء",
                        remaining = ledger.remainingWater,
                        credit = ledger.creditWater,
                        shareText = "حصة: ${Formatters.formatMoney(ledger.currentWaterShare)}",
                        currency = currency,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
                            if (status == PaymentStatus.PAID || status == PaymentStatus.ADVANCE_CREDIT) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Payments
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (ledger.totalRemaining <= 0.05) "إضافة دفعة / تفاصيل" else "تسجيل دفعة"
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

@Composable
private fun MiniBreakdownBox(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    remaining: Double,
    credit: Double,
    shareText: String,
    currency: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = when {
                    credit > 0.05 -> "مقدم +${Formatters.formatMoney(credit)}"
                    remaining <= 0.05 -> "خالص"
                    else -> Formatters.formatMoney(remaining, currency)
                },
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    credit > 0.05 -> StatusAdvance
                    remaining <= 0.05 -> StatusPaid
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = shareText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
