package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BillingPeriodEntity
import com.example.ui.MainUiState
import com.example.ui.components.ElectricityBillDialog
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidContainer
import com.example.ui.theme.StatusUnpaid
import com.example.ui.theme.StatusUnpaidContainer
import com.example.utils.Formatters
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

@Composable
fun ElectricityAnalyticsScreen(
    state: MainUiState,
    onUpdateCurrentElectricity: (periodId: Long, amount: Double, prev: Double, curr: Double) -> Unit,
    onAddHistoricalElectricity: (year: Int, month: Int, amount: Double, prev: Double, curr: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = state.config.currencySymbol
    val analytics = state.analytics
    val currentPeriod = analytics.currentPeriod
    val prevPeriod = analytics.previousPeriod

    var showEditCurrentDialog by remember { mutableStateOf(false) }
    var showAddHistoryDialog by remember { mutableStateOf(false) }
    var inspectedPeriod by remember(analytics.chartPeriods) {
        mutableStateOf(analytics.chartPeriods.lastOrNull())
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تحليل الكهرباء والعداد", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "مقارنة التكاليف والاستهلاك بين الشهور",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddHistoryDialog = true },
                        modifier = Modifier.testTag("add_historical_bill_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("شهر سابق")
                    }
                }
            }

            // Current vs Previous Comparison Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = AmberSecondary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "فاتورة ${currentPeriod?.label ?: "الشهر الحالي"}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (currentPeriod != null) {
                                Button(
                                    onClick = { showEditCurrentDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("edit_current_electricity_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تعديل القراءة")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    Formatters.formatMoney(currentPeriod?.electricityTotalAmount ?: 0.0, currency),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "نصيب الفرد (${state.activeResidentsCount} سكان): ${Formatters.formatMoney((currentPeriod?.electricityTotalAmount ?: 0.0) / state.activeResidentsCount, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (prevPeriod != null) {
                                val isIncrease = analytics.costDiff > 0.05
                                val badgeBg = if (isIncrease) StatusUnpaidContainer else StatusPaidContainer
                                val badgeFg = if (isIncrease) StatusUnpaid else StatusPaid
                                Surface(color = badgeBg, shape = RoundedCornerShape(12.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp
                                            else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = badgeFg,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${if (isIncrease) "+" else "-"}${Formatters.round2(abs(analytics.costDiffPercent))}%",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = badgeFg
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Previous vs Current comparison row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        "الشهر السابق (${prevPeriod?.label ?: "غير مسجل"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (prevPeriod != null) Formatters.formatMoney(prevPeriod.electricityTotalAmount, currency) else "—",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (prevPeriod != null) {
                                        Text(
                                            "الاستهلاك: ${Formatters.formatKwh(prevPeriod.consumptionKwh)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        "الفرق عن الشهر السابق",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val diffText = if (prevPeriod != null) {
                                        val sign = if (analytics.costDiff >= 0) "+" else "-"
                                        "$sign${Formatters.formatMoney(abs(analytics.costDiff), currency)}"
                                    } else "—"
                                    Text(
                                        diffText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (analytics.costDiff > 0) StatusUnpaid else StatusPaid
                                    )
                                    if (prevPeriod != null) {
                                        val kSign = if (analytics.kwhDiff >= 0) "+" else "-"
                                        Text(
                                            "فرق الطاقة: $kSign${Formatters.formatKwh(abs(analytics.kwhDiff))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Meter Readings Detail Card
            if (currentPeriod != null) {
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
                                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("تفاصيل قراءة العداد", style = MaterialTheme.typography.titleMedium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MeterStatBox("القراءة السابقة", "${Formatters.round2(currentPeriod.prevMeterReading)}", Modifier.weight(1f))
                                MeterStatBox("القراءة اللاحقة", "${Formatters.round2(currentPeriod.currMeterReading)}", Modifier.weight(1f))
                                MeterStatBox("الاستهلاك", Formatters.formatKwh(currentPeriod.consumptionKwh), Modifier.weight(1f))
                            }
                            if (currentPeriod.consumptionKwh > 0.0) {
                                Text(
                                    "متوسط تكلفة الكيلوواط: ${Formatters.formatMoney(currentPeriod.avgKwhCost, currency)} / ك.و.س",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Comparison Bar Chart (Custom Canvas)
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
                        Text("مقارنة الشهور (آخر 6 فترات)", style = MaterialTheme.typography.titleMedium)

                        val chartPeriods = analytics.chartPeriods
                        if (chartPeriods.isEmpty() || chartPeriods.all { it.electricityTotalAmount <= 0.0 }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "أدخل فاتورة الكهرباء لهذا الشهر أو الأشهر السابقة لعرض الرسم البياني",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val maxAmount = max(1.0, chartPeriods.maxOf { it.electricityTotalAmount })
                            val barPrimary = MaterialTheme.colorScheme.primary
                            val barSecondary = AmberSecondary
                            val trackColor = MaterialTheme.colorScheme.surfaceVariant

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                chartPeriods.forEach { period ->
                                    val ratio = (period.electricityTotalAmount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                                    val isSelected = inspectedPeriod?.id == period.id
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { inspectedPeriod = period },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = Formatters.formatMoney(period.electricityTotalAmount),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth(0.62f)
                                                .height(115.dp)
                                        ) {
                                            drawRoundRect(
                                                color = trackColor,
                                                topLeft = Offset.Zero,
                                                size = size,
                                                cornerRadius = CornerRadius(14f, 14f)
                                            )
                                            val barH = size.height * ratio
                                            val topY = size.height - barH
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = if (isSelected) listOf(barSecondary, EmeraldPrimary)
                                                    else listOf(barPrimary.copy(alpha = 0.85f), barPrimary.copy(alpha = 0.55f))
                                                ),
                                                topLeft = Offset(0f, topY),
                                                size = Size(size.width, barH),
                                                cornerRadius = CornerRadius(14f, 14f)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${period.month}/${period.year % 100}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            inspectedPeriod?.let { sel ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sel.label, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "${Formatters.formatMoney(sel.electricityTotalAmount, currency)} • ${Formatters.formatKwh(sel.consumptionKwh)}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary KPIs: Average / Highest / Lowest
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeterStatBox(
                        label = "متوسط الفاتورة",
                        value = Formatters.formatMoney(analytics.averageMonthlyCost, currency),
                        modifier = Modifier.weight(1f)
                    )
                    MeterStatBox(
                        label = "أعلى شهر",
                        value = analytics.maxPeriod?.let { Formatters.formatMoney(it.electricityTotalAmount, currency) } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    MeterStatBox(
                        label = "أقل شهر",
                        value = analytics.minPeriod?.let { Formatters.formatMoney(it.electricityTotalAmount, currency) } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // History list header
            item {
                Text("سجل فواتير الكهرباء الشهرية", style = MaterialTheme.typography.titleMedium)
            }

            items(state.periods, key = { it.id }) { period ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(period.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "قراءة: ${Formatters.round2(period.prevMeterReading)} ← ${Formatters.round2(period.currMeterReading)} (${Formatters.formatKwh(period.consumptionKwh)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                Formatters.formatMoney(period.electricityTotalAmount, currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "للفرد: ${Formatters.formatMoney(period.electricityTotalAmount / state.activeResidentsCount, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showEditCurrentDialog && currentPeriod != null) {
        ElectricityBillDialog(
            initialYear = currentPeriod.year,
            initialMonth = currentPeriod.month,
            initialAmount = currentPeriod.electricityTotalAmount,
            initialPrevReading = currentPeriod.prevMeterReading,
            initialCurrReading = currentPeriod.currMeterReading,
            activeResidentsCount = state.activeResidentsCount,
            currency = currency,
            allowMonthSelection = false,
            onDismiss = { showEditCurrentDialog = false },
            onSave = { _, _, amt, prev, curr ->
                showEditCurrentDialog = false
                onUpdateCurrentElectricity(currentPeriod.id, amt, prev, curr)
            }
        )
    }

    if (showAddHistoryDialog) {
        val cal = Calendar.getInstance()
        val (py, pm) = Formatters.previousMonth(
            currentPeriod?.year ?: cal.get(Calendar.YEAR),
            currentPeriod?.month ?: (cal.get(Calendar.MONTH) + 1)
        )
        ElectricityBillDialog(
            initialYear = py,
            initialMonth = pm,
            initialAmount = 0.0,
            initialPrevReading = 0.0,
            initialCurrReading = 0.0,
            activeResidentsCount = state.activeResidentsCount,
            currency = currency,
            allowMonthSelection = true,
            onDismiss = { showAddHistoryDialog = false },
            onSave = { y, m, amt, prev, curr ->
                showAddHistoryDialog = false
                onAddHistoricalElectricity(y, m, amt, prev, curr)
            }
        )
    }
}

@Composable
private fun MeterStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
