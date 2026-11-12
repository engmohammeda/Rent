package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.ContextCompat
import com.example.data.local.ApartmentConfigEntity
import com.example.ui.MainUiState
import com.example.ui.theme.StatusPaid
import com.example.utils.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: MainUiState,
    onSaveConfig: (ApartmentConfigEntity, Boolean) -> Unit,
    onTriggerTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cfg = state.config

    var apartmentName by remember(cfg) { mutableStateOf(cfg.apartmentName) }
    var roomCountText by remember(cfg) { mutableStateOf(cfg.roomCount.toString()) }
    var rentAmountText by remember(cfg) { mutableStateOf(Formatters.round2(cfg.rentCycleAmount).toString()) }
    var rentCycleMonths by remember(cfg) { mutableIntStateOf(cfg.rentCycleMonths) }
    var collectMonthly by remember(cfg) { mutableStateOf(cfg.collectRentMonthlyInstallment) }
    var currency by remember(cfg) { mutableStateOf(cfg.currencySymbol) }

    var notificationsEnabled by remember(cfg) { mutableStateOf(cfg.notificationsEnabled) }
    var autoRolloverEnabled by remember(cfg) { mutableStateOf(cfg.autoRolloverEnabled) }
    var elecDueDayText by remember(cfg) { mutableStateOf(cfg.electricityDueDayOfMonth.toString()) }
    var rentDueDayText by remember(cfg) { mutableStateOf(cfg.rentDueDayOfMonth.toString()) }
    var reminderIntervalHours by remember(cfg) { mutableIntStateOf(cfg.reminderIntervalHours) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            notificationsEnabled = true
            onTriggerTestNotification()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text("الإعدادات والتنبيهات", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "تخصيص الإيجار، مواعيد التذكير، وسجل المدفوعات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Apartment & Rent Config Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("إعدادات الشقة والإيجار", style = MaterialTheme.typography.titleMedium)
                        }

                        OutlinedTextField(
                            value = apartmentName,
                            onValueChange = { apartmentName = it },
                            label = { Text("اسم الشقة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rentAmountText,
                                onValueChange = { rentAmountText = it },
                                label = { Text("إجمالي الإيجار ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = roomCountText,
                                onValueChange = { roomCountText = it },
                                label = { Text("عدد الغرف") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("دورة الإيجار:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = rentCycleMonths == 3,
                                onClick = { rentCycleMonths = 3 },
                                label = { Text("كل 3 أشهر (ربع سنوي)") }
                            )
                            FilterChip(
                                selected = rentCycleMonths == 1,
                                onClick = { rentCycleMonths = 1 },
                                label = { Text("كل شهر") }
                            )
                        }

                        if (rentCycleMonths > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تقسيم الإيجار على أقساط شهرية", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (collectMonthly) "يُحسب ثلث الإيجار شهرياً (مثلاً 5000 ÷ 3)"
                                        else "يُطلب كامل إيجار الـ 3 أشهر مع عرض القسط الشهري للمساعدة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = collectMonthly, onCheckedChange = { collectMonthly = it })
                            }
                        }

                        Text("العملة:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ر.س", "ر.ي", "ج.م", "د.إ", "د.أ", "$").forEach { sym ->
                                FilterChip(
                                    selected = currency == sym,
                                    onClick = { currency = sym },
                                    label = { Text(sym) }
                                )
                            }
                        }
                    }
                }
            }

            // Notifications & Auto-Rollover Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("إشعارات المالك والترحيل التلقائي", style = MaterialTheme.typography.titleMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تفعيل إشعارات المتابعة للمالك", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "تذكيرك بقرب موعد الكهرباء والإيجار والمتبقي حتى يتم السداد بالكامل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { wantEnable ->
                                    if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPerm) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@Switch
                                        }
                                    }
                                    notificationsEnabled = wantEnable
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = elecDueDayText,
                                onValueChange = { elecDueDayText = it },
                                label = { Text("يوم فاتورة الكهرباء") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rentDueDayText,
                                onValueChange = { rentDueDayText = it },
                                label = { Text("يوم استحقاق السكن") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("تكرار التذكير عند وجود متأخرات:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(12 to "كل 12 ساعة", 24 to "يومياً", 48 to "كل يومين").forEach { (hrs, label) ->
                                FilterChip(
                                    selected = reminderIntervalHours == hrs,
                                    onClick = { reminderIntervalHours = hrs },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ترحيل تلقائي عند بداية شهر جديد", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "يرحّل المبالغ غير المدفوعة تلقائياً إلى الشهر الجديد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = autoRolloverEnabled, onCheckedChange = { autoRolloverEnabled = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPerm) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@OutlinedButton
                                        }
                                    }
                                    onTriggerTestNotification()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("تجربة الإشعار")
                            }
                            Button(
                                onClick = {
                                    val updated = cfg.copy(
                                        apartmentName = apartmentName.ifBlank { "شقة الوالد" },
                                        roomCount = (roomCountText.toIntOrNull() ?: 2).coerceIn(1, 20),
                                        rentCycleAmount = (rentAmountText.toDoubleOrNull() ?: 5000.0).coerceAtLeast(0.0),
                                        rentCycleMonths = rentCycleMonths,
                                        collectRentMonthlyInstallment = collectMonthly,
                                        currencySymbol = currency,
                                        electricityDueDayOfMonth = (elecDueDayText.toIntOrNull() ?: 25).coerceIn(1, 28),
                                        rentDueDayOfMonth = (rentDueDayText.toIntOrNull() ?: 1).coerceIn(1, 28),
                                        reminderIntervalHours = reminderIntervalHours,
                                        notificationsEnabled = notificationsEnabled,
                                        autoRolloverEnabled = autoRolloverEnabled
                                    )
                                    onSaveConfig(updated, true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("save_settings_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("حفظ الإعدادات")
                            }
                        }
                    }
                }
            }

            // Recent Payment Transactions Log
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "سجل الدفعات المستلمة (${state.allTransactions.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (state.allTransactions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            "لم يتم تسجيل أي دفعات مالية بعد. اضغط على أي ساكن في الرئيسية لتسجيل دفعته.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                items(state.allTransactions.take(25), key = { it.id }) { tx ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.residentNameSnapshot, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "سكن: ${Formatters.formatMoney(tx.allocatedToRent)} • كهرباء: ${Formatters.formatMoney(tx.allocatedToElectricity)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tx.note.isNotBlank()) {
                                    Text(
                                        tx.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    Formatters.formatDateTime(tx.paidAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "+${Formatters.formatMoney(tx.amountPaid, currency)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusPaid,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
