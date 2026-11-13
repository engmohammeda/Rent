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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.ui.theme.EzbahGold
import com.example.ui.theme.StatusAdvance
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
    var waterIncludedInRent by remember(cfg) { mutableStateOf(cfg.waterIncludedInRent) }
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text("إعدادات العزبة والتنبيهات", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "خصص اسم العزبة، الغرف، الإيجار، الماء، ومواعيد التذكير",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ezbah & Rent Config Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("تخصيص العزبة والإيجار والماء", style = MaterialTheme.typography.titleMedium)
                        }

                        OutlinedTextField(
                            value = apartmentName,
                            onValueChange = { apartmentName = it },
                            label = { Text("اسم العزبة أو السكن") },
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                1 to "شهري",
                                2 to "كل شهرين",
                                3 to "كل 3 أشهر",
                                6 to "كل 6 أشهر",
                                12 to "سنوي"
                            ).forEach { (months, label) ->
                                FilterChip(
                                    selected = rentCycleMonths == months,
                                    onClick = { rentCycleMonths = months },
                                    label = { Text(label) }
                                )
                            }
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
                                        if (collectMonthly) "يُقسم إيجار الدورة على $rentCycleMonths أشهر (مثلاً 4,500 ÷ $rentCycleMonths)"
                                        else "يُطلب كامل إيجار الدورة دفعة واحدة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = collectMonthly, onCheckedChange = { collectMonthly = it })
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = StatusAdvance,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("الماء مشمول ضمن الإيجار", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (waterIncludedInRent) "لا تُضاف فاتورة ماء منفصلة على السكان"
                                        else "تُضاف فاتورة الماء شهرياً وتُقسم بالتساوي مثل الكهرباء",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = waterIncludedInRent,
                                onCheckedChange = { waterIncludedInRent = it },
                                modifier = Modifier.testTag("water_included_switch")
                            )
                        }

                        Text("العملة:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ر.س", "ر.ي", "ج.م", "د.إ", "د.أ", "د.ك", "$").forEach { sym ->
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
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = EzbahGold.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = EzbahGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("إشعارات المالك والترحيل التلقائي", style = MaterialTheme.typography.titleMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تفعيل إشعارات المتابعة", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "تذكيرك بمواعيد الكهرباء والإيجار والمتبقي حتى اكتمال السداد",
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
                                    "يرحّل المتبقي غير المدفوع أو الأرصدة المدفوعة مقدماً تلقائياً",
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
                                        apartmentName = apartmentName.ifBlank { "عزبتنا" },
                                        roomCount = (roomCountText.toIntOrNull() ?: 2).coerceIn(1, 20),
                                        rentCycleAmount = (rentAmountText.toDoubleOrNull() ?: 4500.0).coerceAtLeast(0.0),
                                        rentCycleMonths = rentCycleMonths,
                                        collectRentMonthlyInstallment = collectMonthly,
                                        waterIncludedInRent = waterIncludedInRent,
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
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        shape = RoundedCornerShape(16.dp),
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
                                val waterTxt = if (tx.allocatedToWater > 0.01) {
                                    " • ماء: ${Formatters.formatMoney(tx.allocatedToWater)}"
                                } else ""
                                Text(
                                    "سكن: ${Formatters.formatMoney(tx.allocatedToRent)} • كهرباء: ${Formatters.formatMoney(tx.allocatedToElectricity)}$waterTxt",
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
