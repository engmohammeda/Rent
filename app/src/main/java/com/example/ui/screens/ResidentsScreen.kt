package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ResidentEntity
import com.example.ui.MainUiState
import com.example.ui.components.ResidentEditDialog
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidContainer
import com.example.utils.Formatters
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResidentsScreen(
    state: MainUiState,
    onGenerateResidentsAuto: (roomCount: Int, totalResidents: Int, replaceExisting: Boolean) -> Unit,
    onAddResident: (name: String, room: Int, phone: String) -> Unit,
    onUpdateResident: (ResidentEntity) -> Unit,
    onDeleteResident: (ResidentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var autoRooms by remember(state.config.roomCount) { mutableIntStateOf(state.config.roomCount) }
    var autoTotal by remember(state.residents.size) {
        mutableIntStateOf(max(1, state.residents.size))
    }
    var showGenerateConfirm by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingResident by remember { mutableStateOf<ResidentEntity?>(null) }

    val previewPerPersonCycle = state.config.rentCycleAmount / max(1, autoTotal)
    val previewPerPersonMonth = (state.config.rentCycleAmount / max(1, state.config.rentCycleMonths)) / max(1, autoTotal)

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_resident_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة ساكن جديد")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            "إدارة السكان والتوليد التلقائي",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "أدخل عدد السكان لإنشاء الشخصيات تلقائياً أو عدّل كل ساكن حسب رغبتك",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Auto-Generate Residents Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "إنشاء السكان تلقائياً حسب العدد",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Total residents stepper
                                StepperBox(
                                    label = "عدد السكان",
                                    value = autoTotal,
                                    onDecrement = { if (autoTotal > 1) autoTotal-- },
                                    onIncrement = { if (autoTotal < 60) autoTotal++ },
                                    modifier = Modifier.weight(1f)
                                )
                                // Rooms stepper
                                StepperBox(
                                    label = "عدد الغرف",
                                    value = autoRooms,
                                    onDecrement = { if (autoRooms > 1) autoRooms-- },
                                    onIncrement = { if (autoRooms < 12) autoRooms++ },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(6, 8, 10, 12, 14, 16).forEach { preset ->
                                    FilterChip(
                                        selected = autoTotal == preset,
                                        onClick = { autoTotal = preset },
                                        label = { Text("$preset أشخاص") }
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "معاينة التقسيم على $autoTotal سكان ($autoRooms غرف):",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        "• نصيب الساكن من الدورة (${state.config.rentCycleMonths} أشهر): ${Formatters.formatMoney(previewPerPersonCycle, state.config.currencySymbol)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "• القسط الشهري للساكن الواحد: ${Formatters.formatMoney(previewPerPersonMonth, state.config.currencySymbol)} / شهر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = { showGenerateConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("generate_residents_button")
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("توليد $autoTotal شخصيات تلقائياً")
                            }
                        }
                    }
                }

                // List header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "قائمة السكان (${state.residents.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "اضغط للتعديل والاسم ورقم الواتساب",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.residents.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("لا يوجد سكان حالياً")
                            }
                        }
                    }
                } else {
                    items(state.residents, key = { it.id }) { resident ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingResident = resident }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (resident.isActive) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            resident.name.take(2),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                resident.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (resident.isActive) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    color = StatusPaidContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "نشط",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = StatusPaid,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Home,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    "غرفة ${resident.roomNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (resident.phoneNumber.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Phone,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp),
                                                        tint = StatusPaid
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        resident.phoneNumber,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = StatusPaid
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { editingResident = resident },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل ${resident.name}")
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showGenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showGenerateConfirm = false },
            title = { Text("إنشاء $autoTotal شخصيات تلقائياً") },
            text = {
                Text("هل تريد استبدال القائمة الحالية بـ $autoTotal ساكنين جدد موزعين على $autoRooms غرف، أم إضافتهم على السكان الحاليين؟")
            },
            confirmButton = {
                Button(onClick = {
                    showGenerateConfirm = false
                    onGenerateResidentsAuto(autoRooms, autoTotal, true)
                }) {
                    Text("استبدال وإنشاء جديد")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { showGenerateConfirm = false }) { Text("إلغاء") }
                    OutlinedButton(onClick = {
                        showGenerateConfirm = false
                        onGenerateResidentsAuto(autoRooms, autoTotal, false)
                    }) {
                        Text("إضافة فقط")
                    }
                }
            }
        )
    }

    if (showAddDialog) {
        ResidentEditDialog(
            resident = null,
            roomCount = state.config.roomCount,
            onDismiss = { showAddDialog = false },
            onSave = { name, room, phone, _ ->
                showAddDialog = false
                onAddResident(name, room, phone)
            }
        )
    }

    editingResident?.let { target ->
        ResidentEditDialog(
            resident = target,
            roomCount = state.config.roomCount,
            onDismiss = { editingResident = null },
            onSave = { name, room, phone, active ->
                editingResident = null
                onUpdateResident(
                    target.copy(
                        name = name,
                        roomNumber = room,
                        phoneNumber = phone,
                        isActive = active
                    )
                )
            },
            onDelete = {
                editingResident = null
                onDeleteResident(target)
            }
        )
    }
}

@Composable
private fun StepperBox(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "إنقاص $label")
                }
                Text(
                    "$value",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زيادة $label")
                }
            }
        }
    }
}
