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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.Surface
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
import com.example.ui.theme.StatusAdvance
import com.example.ui.theme.StatusAdvanceContainer
import com.example.ui.theme.StatusPaid
import com.example.utils.Formatters
import com.example.utils.ReminderTone
import com.example.utils.WhatsAppHelper
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentBottomSheet(
    item: ResidentLedgerWithDetails,
    config: ApartmentConfigEntity,
    monthlyRentPerPerson: Double,
    cycleRentPerPerson: Double,
    onDismiss: () -> Unit,
    onConfirmPayment: (
        amountBrought: Double,
        mode: PaymentAllocationMode,
        customRent: Double,
        customElec: Double,
        customWater: Double,
        note: String
    ) -> Unit,
    onResetPayment: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ledger = item.ledger
    val currency = config.currencySymbol

    var amountText by remember(item.ledger.id) {
        mutableStateOf(
            if (ledger.totalRemaining > 0.05) Formatters.round2(ledger.totalRemaining).toString() else ""
        )
    }
    var selectedMode by remember { mutableStateOf(PaymentAllocationMode.AUTO_UTILITIES_FIRST) }
    var customRentText by remember { mutableStateOf(Formatters.round2(ledger.remainingRent).toString()) }
    var customElecText by remember { mutableStateOf(Formatters.round2(ledger.remainingElectricity).toString()) }
    var customWaterText by remember { mutableStateOf(Formatters.round2(ledger.remainingWater).toString()) }
    var noteText by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    val typedAmount = if (selectedMode == PaymentAllocationMode.CUSTOM_SPLIT) {
        (customRentText.toDoubleOrNull() ?: 0.0) +
            (customElecText.toDoubleOrNull() ?: 0.0) +
            (customWaterText.toDoubleOrNull() ?: 0.0)
    } else {
        amountText.toDoubleOrNull() ?: 0.0
    }
    val projectedAdvance = max(0.0, typedAmount - ledger.totalRemaining)

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
                        text = "غرفة ${item.resident.roomNumber} • يدعم الدفع الجزئي أو لعدة أشهر مقدماً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            // Account Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("متبقي السكن:", style = MaterialTheme.typography.bodyMedium)
                        Text(Formatters.formatMoney(ledger.remainingRent, currency), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("متبقي الكهرباء:", style = MaterialTheme.typography.bodyMedium)
                        Text(Formatters.formatMoney(ledger.remainingElectricity, currency), fontWeight = FontWeight.Bold)
                    }
                    if (!config.waterIncludedInRent) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("متبقي الماء:", style = MaterialTheme.typography.bodyMedium)
                            Text(Formatters.formatMoney(ledger.remainingWater, currency), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (ledger.previousArrearsDebt > 0.05) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "تشمل متأخر من السابق:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                Formatters.formatMoney(ledger.previousArrearsDebt, currency),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (ledger.totalAdvanceCredit > 0.05) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "رصيد دائن مدفوع مقدماً:",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusAdvance
                            )
                            Text(
                                Formatters.formatMoney(ledger.totalAdvanceCredit, currency),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusAdvance
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المطلوب حالياً:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (ledger.totalRemaining <= 0.05) "خالص" else Formatters.formatMoney(ledger.totalRemaining, currency),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick presets for flexible real-world Ezbah payments
            Text("اختيار سريع حسب الدفعة:", style = MaterialTheme.typography.labelLarge)
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
                        label = { Text("المستحق الحالي (${Formatters.formatMoney(ledger.totalRemaining)})") }
                    )
                }
                if (config.rentCycleMonths >= 2 && monthlyRentPerPerson > 0.05) {
                    val twoMonthsPlusElec = Formatters.round2((monthlyRentPerPerson * 2) + ledger.remainingElectricity + ledger.remainingWater)
                    FilterChip(
                        selected = amountText.toDoubleOrNull() == twoMonthsPlusElec,
                        onClick = {
                            amountText = twoMonthsPlusElec.toString()
                            selectedMode = PaymentAllocationMode.AUTO_UTILITIES_FIRST
                            if (noteText.isBlank()) noteText = "دفعة شهرين"
                        },
                        label = { Text("شهرين (${Formatters.formatMoney(twoMonthsPlusElec)})") }
                    )
                }
                if (config.rentCycleMonths >= 3 && cycleRentPerPerson > 0.05) {
                    val fullCyclePlusElec = Formatters.round2(cycleRentPerPerson + ledger.remainingElectricity + ledger.remainingWater)
                    FilterChip(
                        selected = amountText.toDoubleOrNull() == fullCyclePlusElec,
                        onClick = {
                            amountText = fullCyclePlusElec.toString()
                            selectedMode = PaymentAllocationMode.AUTO_UTILITIES_FIRST
                            if (noteText.isBlank()) noteText = "دفعة ${config.rentCycleMonths} أشهر كاملة"
                        },
                        label = { Text("${config.rentCycleMonths} أشهر كاملة (${Formatters.formatMoney(fullCyclePlusElec)})") }
                    )
                }
                FilterChip(
                    selected = amountText == "200",
                    onClick = {
                        amountText = "200"
                        selectedMode = PaymentAllocationMode.AUTO_UTILITIES_FIRST
                    },
                    label = { Text("200 $currency") }
                )
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
            }

            if (selectedMode != PaymentAllocationMode.CUSTOM_SPLIT) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المدفوع ($currency)") },
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
                        label = { Text("السكن") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customElecText,
                        onValueChange = { customElecText = it },
                        label = { Text("الكهرباء") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (!config.waterIncludedInRent) {
                        OutlinedTextField(
                            value = customWaterText,
                            onValueChange = { customWaterText = it },
                            label = { Text("الماء") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (projectedAdvance > 0.05) {
                Surface(
                    color = StatusAdvanceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "سيُحتفظ بمبلغ ${Formatters.formatMoney(projectedAdvance, currency)} كرصيد مدفوع مقدماً ويُخصم تلقائياً من الأشهر القادمة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAdvance,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Text("توزيع المبلغ:", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PaymentAllocationMode.AUTO_UTILITIES_FIRST to "الكهرباء/الماء أولاً ثم السكن",
                    PaymentAllocationMode.AUTO_RENT_FIRST to "السكن أولاً ثم الكهرباء",
                    PaymentAllocationMode.RENT_ONLY to "للسكن (أو مقدم سكن)",
                    PaymentAllocationMode.ELECTRICITY_ONLY to "للكهرباء فقط",
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
                label = { Text("ملاحظة (مثال: دفع مقدم شهرين / تحويل بنكي)") },
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
                        Text("تصفير")
                    }
                }
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val cRent = customRentText.toDoubleOrNull() ?: 0.0
                        val cElec = customElecText.toDoubleOrNull() ?: 0.0
                        val cWater = customWaterText.toDoubleOrNull() ?: 0.0
                        onConfirmPayment(amt, selectedMode, cRent, cElec, cWater, noteText)
                    },
                    enabled = typedAmount > 0.0,
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
            text = { Text("هل تريد إعادة تعيين المبلغ المدفوع لـ ${item.resident.name} في هذا الشهر إلى صفر؟") },
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
fun UtilitiesBillDialog(
    initialYear: Int,
    initialMonth: Int,
    initialElecAmount: Double,
    initialWaterAmount: Double,
    initialPrevReading: Double,
    initialCurrReading: Double,
    waterIncludedInRent: Boolean,
    activeResidentsCount: Int,
    currency: String,
    allowMonthSelection: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (year: Int, month: Int, elecAmount: Double, waterAmount: Double, prevReading: Double, currReading: Double) -> Unit
) {
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }
    var elecText by remember {
        mutableStateOf(if (initialElecAmount > 0.0) Formatters.round2(initialElecAmount).toString() else "")
    }
    var waterText by remember {
        mutableStateOf(if (initialWaterAmount > 0.0) Formatters.round2(initialWaterAmount).toString() else "")
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
    val elecVal = elecText.toDoubleOrNull() ?: 0.0
    val waterVal = if (waterIncludedInRent) 0.0 else (waterText.toDoubleOrNull() ?: 0.0)
    val elecPerPerson = elecVal / max(1, activeResidentsCount)
    val waterPerPerson = waterVal / max(1, activeResidentsCount)

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
                Text("الفواتير • ${Formatters.formatMonthLabel(year, month)}")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                        }) { Text("السابق") }
                        Text(Formatters.formatMonthLabel(year, month), fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = {
                            val (ny, nm) = Formatters.nextMonth(year, month)
                            year = ny
                            month = nm
                        }) { Text("التالي") }
                    }
                }
                OutlinedTextField(
                    value = elecText,
                    onValueChange = { elecText = it },
                    label = { Text("فاتورة الكهرباء ($currency)") },
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

                if (!waterIncludedInRent) {
                    OutlinedTextField(
                        value = waterText,
                        onValueChange = { waterText = it },
                        label = { Text("فاتورة الماء ($currency)") },
                        leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "ملاحظة: الماء مضبوط حالياً على (مشمول ضمن الإيجار) — يمكنك تغييره من الإعدادات.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            "نصيب الفرد (كهرباء): ${Formatters.formatMoney(elecPerPerson, currency)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (!waterIncludedInRent) {
                            Text(
                                "نصيب الفرد (ماء): ${Formatters.formatMoney(waterPerPerson, currency)}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(year, month, elecVal, waterVal, prevVal, currVal) },
                enabled = elecVal >= 0.0,
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
    totalAdvanceCredits: Double,
    config: ApartmentConfigEntity,
    onDismiss: () -> Unit,
    onConfirmRollover: (
        nextElecAmount: Double,
        nextWaterAmount: Double,
        nextPrevReading: Double,
        nextCurrReading: Double,
        chargeRent: Boolean
    ) -> Unit
) {
    val (nextY, nextM) = Formatters.nextMonth(currentPeriod.year, currentPeriod.month)
    val nextLabel = Formatters.formatMonthLabel(nextY, nextM)
    var elecAmountText by remember { mutableStateOf("") }
    var waterAmountText by remember { mutableStateOf("") }
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
                        Text("ملخص الترحيل التلقائي:", style = MaterialTheme.typography.titleSmall)
                        if (unpaidResidentsCount > 0) {
                            Text(
                                "• متبقي غير مدفوع: ${Formatters.formatMoney(totalCarryOverAmount, config.currencySymbol)} ($unpaidResidentsCount ساكنين) سيُرحّل كمتبقي سابق.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text("• لا توجد متأخرات غير مدفوعة!", style = MaterialTheme.typography.bodySmall)
                        }
                        if (totalAdvanceCredits > 0.05) {
                            Text(
                                "• أرصدة مدفوعة مقدماً: ${Formatters.formatMoney(totalAdvanceCredits, config.currencySymbol)} ستُخصم تلقائياً من مستحقات الشهر الجديد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusAdvance
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "احتساب حصة الإيجار في $nextLabel",
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
                if (!config.waterIncludedInRent) {
                    OutlinedTextField(
                        value = waterAmountText,
                        onValueChange = { waterAmountText = it },
                        label = { Text("فاتورة ماء $nextLabel (اختياري)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    val water = waterAmountText.toDoubleOrNull() ?: 0.0
                    val prev = prevReadingText.toDoubleOrNull() ?: currentPeriod.currMeterReading
                    val curr = currReadingText.toDoubleOrNull() ?: prev
                    onConfirmRollover(amt, water, prev, curr, chargeRent)
                },
                modifier = Modifier.testTag("confirm_rollover_button")
            ) {
                Text("إغلاق وترحيل")
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
    var tone by remember { mutableStateOf(ReminderTone.NATURAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رسائل وتذكيرات واتساب (${unpaidItems.size})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderTone.entries.forEach { t ->
                        FilterChip(
                            selected = tone == t,
                            onClick = { tone = t },
                            label = { Text(t.label) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val summary = WhatsAppHelper.buildApartmentSummaryMessage(items, period, config)
                        WhatsAppHelper.shareTextViaChooser(context, summary)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("مشاركة ملخص العزبة للمجموعة")
                }

                if (unpaidItems.isEmpty()) {
                    Text(
                        "الجميع مسدد بالكامل لهذه الفترة!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusPaid
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(250.dp),
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
                                        Text(item.resident.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "المتبقي: ${Formatters.formatMoney(item.ledger.totalRemaining, config.currencySymbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (!isSent) sentIds.add(item.resident.id)
                                            val msg = WhatsAppHelper.buildResidentReminderMessage(
                                                item = item,
                                                period = period,
                                                config = config,
                                                tone = tone
                                            )
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
                                            Text("تم")
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
                    label = { Text("رقم الواتساب (مع مفتاح الدولة)") },
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
