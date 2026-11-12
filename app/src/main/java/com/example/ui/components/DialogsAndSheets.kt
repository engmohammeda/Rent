package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ApartmentConfigEntity
import com.example.data.local.BillingPeriodEntity
import com.example.data.local.PaymentAllocationMode
import com.example.data.local.ResidentEntity
import com.example.data.local.ResidentLedgerWithDetails
import com.example.ui.theme.StatusPaid
import com.example.utils.Formatters
import com.example.utils.WhatsAppHelper
import java.util.Calendar
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentBottomSheet(
    item: ResidentLedgerWithDetails,
    config: ApartmentConfigEntity,
    monthlyRentPerPerson: Double,
    onDismiss: () -> Unit,
    onConfirmPayment: (
        amountBrought: Double,
        mode: PaymentAllocationMode,
        customRent: Double,
        customElec: Double,
        note: String
    ) -> Unit,
    onResetPayment: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ledger = item.ledger
    val currency = config.currencySymbol

    var amountText by remember(item.ledger.id) {
        mutableStateOf(
            if (ledger.totalRemaining > 0.0) Formatters.round2(ledger.totalRemaining).toString() else ""
        )
    }
    var selectedMode by remember { mutableStateOf(PaymentAllocationMode.AUTO_ELECTRICITY_FIRST) }
    var customRentText by remember { mutableStateOf(Formatters.round2(ledger.remainingRent).toString()) }
    var customElecText by remember { mutableStateOf(Formatters.round2(ledger.remainingElectricity).toString()) }
    var noteText by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تسجيل دفعة • ${item.resident.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "غرفة ${item.resident.roomNumber} • سجل حسب ما أحضره الساكن",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            // Breakdown card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("متبقي السكن (شامل السابق):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Formatters.formatMoney(ledger.remainingRent, currency),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("متبقي الكهرباء (شامل السابق):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Formatters.formatMoney(ledger.remainingElectricity, currency),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (ledger.previousTotalBalance > 0.05) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "منها متبقي مرحّل من الشهر السابق:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                Formatters.formatMoney(ledger.previousTotalBalance, currency),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الإجمالي المتبقي:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            Formatters.formatMoney(ledger.totalRemaining, currency),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick amount chips
            Text("اختيار سريع للمبلغ:", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (ledger.totalRemaining > 0.05) {
                    FilterChip(
                        selected = amountText.toDoubleOrNull() == Formatters.round2(ledger.totalRemaining),
                        onClick = {
                            amountText = Formatters.round2(ledger.totalRemaining).toString()
                            selectedMode = PaymentAllocationMode.FULL_REMAINING
                        },
                        label = { Text("كامل المبلغ (${Formatters.formatMoney(ledger.totalRemaining)})") }
                    )
                }
                if (config.rentCycleMonths > 1 && monthlyRentPerPerson > 0.05) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val totalMonthlyWithElec = Formatters.round2(monthlyRentPerPerson + ledger.remainingElectricity)
                            amountText = totalMonthlyWithElec.toString()
                            selectedMode = PaymentAllocationMode.AUTO_ELECTRICITY_FIRST
                        },
                        label = { Text("قسط شهر + كهرباء (${Formatters.formatMoney(monthlyRentPerPerson + ledger.remainingElectricity)})") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            amountText = Formatters.round2(monthlyRentPerPerson).toString()
                            selectedMode = PaymentAllocationMode.RENT_ONLY
                        },
                        label = { Text("قسط سكن شهر واحد (${Formatters.formatMoney(monthlyRentPerPerson)})") }
                    )
                }
                if (ledger.remainingElectricity > 0.05) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            amountText = Formatters.round2(ledger.remainingElectricity).toString()
                            selectedMode = PaymentAllocationMode.ELECTRICITY_ONLY
                        },
                        label = { Text("الكهرباء فقط (${Formatters.formatMoney(ledger.remainingElectricity)})") }
                    )
                }
                if (ledger.remainingRent > 0.05) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            amountText = Formatters.round2(ledger.remainingRent).toString()
                            selectedMode = PaymentAllocationMode.RENT_ONLY
                        },
                        label = { Text("السكن فقط (${Formatters.formatMoney(ledger.remainingRent)})") }
                    )
                }
            }

            if (selectedMode != PaymentAllocationMode.CUSTOM_SPLIT) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ الذي أحضره الساكن ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customRentText,
                        onValueChange = { customRentText = it },
                        label = { Text("حصة السكن ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customElecText,
                        onValueChange = { customElecText = it },
                        label = { Text("حصة الكهرباء ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("طريقة توزيع الدفعة:", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PaymentAllocationMode.AUTO_ELECTRICITY_FIRST to "الكهرباء أولاً ثم السكن",
                    PaymentAllocationMode.AUTO_RENT_FIRST to "السكن أولاً ثم الكهرباء",
                    PaymentAllocationMode.ELECTRICITY_ONLY to "للكهرباء فقط",
                    PaymentAllocationMode.RENT_ONLY to "للسكن فقط",
                    PaymentAllocationMode.CUSTOM_SPLIT to "تقسيم مخصص"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("ملاحظة (اختياري)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (ledger.totalPaid > 0.05) {
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("تصفير المدفوع")
                    }
                }
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val cRent = customRentText.toDoubleOrNull() ?: 0.0
                        val cElec = customElecText.toDoubleOrNull() ?: 0.0
                        onConfirmPayment(amt, selectedMode, cRent, cElec, noteText)
                    },
                    enabled = if (selectedMode == PaymentAllocationMode.CUSTOM_SPLIT) {
                        (customRentText.toDoubleOrNull() ?: 0.0) + (customElecText.toDoubleOrNull() ?: 0.0) > 0.0
                    } else {
                        (amountText.toDoubleOrNull() ?: 0.0) > 0.0
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("confirm_payment_button")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("اعتماد الدفعة")
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("تصفير مدفوعات الفترة") },
            text = { Text("هل تريد إعادة تعيين المبلغ المدفوع لـ ${item.resident.name} إلى صفر؟") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    onResetPayment()
                }) { Text("نعم، تصفير", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun ElectricityBillDialog(
    initialYear: Int,
    initialMonth: Int,
    initialAmount: Double,
    initialPrevReading: Double,
    initialCurrReading: Double,
    activeResidentsCount: Int,
    currency: String,
    allowMonthSelection: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (year: Int, month: Int, amount: Double, prevReading: Double, currReading: Double) -> Unit
) {
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }
    var amountText by remember {
        mutableStateOf(if (initialAmount > 0.0) Formatters.round2(initialAmount).toString() else "")
    }
    var prevText by remember {
        mutableStateOf(if (initialPrevReading > 0.0) Formatters.round2(initialPrevReading).toString() else "")
    }
    var currText by remember {
        mutableStateOf(if (initialCurrReading > 0.0) Formatters.round2(initialCurrReading).toString() else "")
    }

    val prevVal = prevText.toDoubleOrNull() ?: 0.0
    val currVal = currText.toDoubleOrNull() ?: 0.0
    val consumption = max(0.0, currVal - prevVal)
    val billVal = amountText.toDoubleOrNull() ?: 0.0
    val perPerson = billVal / max(1, activeResidentsCount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text("فاتورة الكهرباء • ${Formatters.formatMonthLabel(year, month)}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowMonthSelection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = {
                            val (py, pm) = Formatters.previousMonth(year, month)
                            year = py
                            month = pm
                        }) { Text("الشهر السابق") }
                        Text(
                            Formatters.formatMonthLabel(year, month),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = {
                            val (ny, nm) = Formatters.nextMonth(year, month)
                            year = ny
                            month = nm
                        }) { Text("الشهر التالي") }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("إجمالي تكلفة الفاتورة ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("electricity_amount_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prevText,
                        onValueChange = { prevText = it },
                        label = { Text("القراءة السابقة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currText,
                        onValueChange = { currText = it },
                        label = { Text("القراءة اللاحقة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "الاستهلاك المحسوب: ${Formatters.formatKwh(consumption)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "نصيب الفرد ($activeResidentsCount سكان): ${Formatters.formatMoney(perPerson, currency)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(year, month, billVal, prevVal, currVal) },
                enabled = billVal >= 0.0,
                modifier = Modifier.testTag("save_electricity_button")
            ) {
                Text("حفظ وتقسيم")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun RolloverPeriodDialog(
    currentPeriod: BillingPeriodEntity,
    unpaidResidentsCount: Int,
    totalCarryOverAmount: Double,
    config: ApartmentConfigEntity,
    onDismiss: () -> Unit,
    onConfirmRollover: (
        nextElecAmount: Double,
        nextPrevReading: Double,
        nextCurrReading: Double,
        chargeRent: Boolean
    ) -> Unit
) {
    val (nextY, nextM) = Formatters.nextMonth(currentPeriod.year, currentPeriod.month)
    val nextLabel = Formatters.formatMonthLabel(nextY, nextM)
    var elecAmountText by remember { mutableStateOf("") }
    var prevReadingText by remember {
        mutableStateOf(if (currentPeriod.currMeterReading > 0) currentPeriod.currMeterReading.toString() else "")
    }
    var currReadingText by remember { mutableStateOf("") }
    var chargeRent by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إغلاق ${currentPeriod.label} والترحيل إلى $nextLabel") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "ملخص الترحيل التلقائي:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (unpaidResidentsCount > 0) {
                                "سيتم ترحيل متبقي بمقدار ${Formatters.formatMoney(totalCarryOverAmount, config.currencySymbol)} من $unpaidResidentsCount ساكنين تلقائياً وتوضيحه كـ (متبقي من الشهر السابق) في $nextLabel."
                            } else {
                                "جميع السكان سددوا مستحقات ${currentPeriod.label} بالكامل! 🎉"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "احتساب حصة الإيجار في الشهر الجديد",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = chargeRent, onCheckedChange = { chargeRent = it })
                }

                OutlinedTextField(
                    value = elecAmountText,
                    onValueChange = { elecAmountText = it },
                    label = { Text("فاتورة كهرباء $nextLabel (اختياري الآن)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prevReadingText,
                        onValueChange = { prevReadingText = it },
                        label = { Text("قراءة سابقة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currReadingText,
                        onValueChange = { currReadingText = it },
                        label = { Text("قراءة لاحقة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = elecAmountText.toDoubleOrNull() ?: 0.0
                    val prev = prevReadingText.toDoubleOrNull() ?: currentPeriod.currMeterReading
                    val curr = currReadingText.toDoubleOrNull() ?: prev
                    onConfirmRollover(amt, prev, curr, chargeRent)
                },
                modifier = Modifier.testTag("confirm_rollover_button")
            ) {
                Text("إغلاق وترحيل الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun WhatsAppQueueDialog(
    items: List<ResidentLedgerWithDetails>,
    period: BillingPeriodEntity,
    config: ApartmentConfigEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val unpaidItems = remember(items) { items.filter { it.ledger.totalRemaining > 0.05 } }
    val sentIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تذكيرات واتساب (${unpaidItems.size} ساكنين)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val summary = WhatsAppHelper.buildApartmentSummaryMessage(items, period, config)
                        WhatsAppHelper.shareTextViaChooser(context, summary)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("مشاركة ملخص الشقة كاملاً للمجموعة")
                }

                if (unpaidItems.isEmpty()) {
                    Text(
                        "جميع السكان مسددون بالكامل لهذه الفترة!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusPaid
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(unpaidItems, key = { it.resident.id }) { item ->
                            val isSent = item.resident.id in sentIds
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.resident.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "المتبقي: ${Formatters.formatMoney(item.ledger.totalRemaining, config.currencySymbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (!isSent) sentIds.add(item.resident.id)
                                            val msg = WhatsAppHelper.buildResidentReminderMessage(item, period, config)
                                            WhatsAppHelper.openWhatsAppForResident(
                                                context,
                                                item.resident.phoneNumber,
                                                msg
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSent) StatusPaid else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        if (isSent) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("تم الفتح")
                                        } else {
                                            Text("إرسال")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun ResidentEditDialog(
    resident: ResidentEntity?,
    roomCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, room: Int, phone: String, isActive: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(resident) { mutableStateOf(resident?.name ?: "") }
    var roomNumber by remember(resident) { mutableIntStateOf(resident?.roomNumber ?: 1) }
    var phone by remember(resident) { mutableStateOf(resident?.phoneNumber ?: "") }
    var isActive by remember(resident) { mutableStateOf(resident?.isActive ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (resident == null) "إضافة ساكن جديد" else "تعديل بيانات الساكن") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الساكن") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resident_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الواتساب (مع مفتاح الدولة مثل 9665...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("الغرفة:", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (r in 1..max(1, roomCount)) {
                        FilterChip(
                            selected = roomNumber == r,
                            onClick = { roomNumber = r },
                            label = { Text("غرفة $r") },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
                if (resident != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ساكن نشط (يدخل في التقسيم)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), roomNumber, phone.trim(), isActive) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_resident_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            Row {
                if (resident != null && onDelete != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف الساكن",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        }
    )

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف الساكن") },
            text = { Text("هل أنت متأكد من حذف ${resident?.name}؟ سيتم إعادة تقسيم السكن والكهرباء على الباقين.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") }
            }
        )
    }
}
