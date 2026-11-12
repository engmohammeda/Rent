package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.ApartmentConfigEntity
import com.example.data.local.BillingPeriodEntity
import com.example.data.local.ResidentLedgerWithDetails
import java.net.URLEncoder

object WhatsAppHelper {

    fun buildResidentReminderMessage(
        item: ResidentLedgerWithDetails,
        period: BillingPeriodEntity,
        config: ApartmentConfigEntity
    ): String {
        val currency = config.currencySymbol
        val ledger = item.ledger
        val resident = item.resident

        return buildString {
            appendLine("السلام عليكم أخي ${resident.name}،")
            appendLine("تذكير بمستحقات ${config.apartmentName} (غرفة ${resident.roomNumber}) لشهر ${period.label}:")
            appendLine("━━━━━━━━━━━━━━")
            appendLine("🏠 السكن (الشهر الحالي): ${Formatters.formatMoney(ledger.currentRentShare, currency)}")
            if (ledger.previousRentBalance > 0.05) {
                appendLine("↩️ متبقي سكن سابق: ${Formatters.formatMoney(ledger.previousRentBalance, currency)}")
            }
            appendLine("⚡ الكهرباء (الشهر الحالي): ${Formatters.formatMoney(ledger.currentElectricityShare, currency)}")
            if (ledger.previousElectricityBalance > 0.05) {
                appendLine("↩️ متبقي كهرباء سابق: ${Formatters.formatMoney(ledger.previousElectricityBalance, currency)}")
            }
            if (ledger.totalPaid > 0.05) {
                appendLine("✅ المدفوع حتى الآن: ${Formatters.formatMoney(ledger.totalPaid, currency)}")
            }
            appendLine("━━━━━━━━━━━━━━")
            appendLine("💰 الإجمالي المتبقي عليك: ${Formatters.formatMoney(ledger.totalRemaining, currency)}")
            if (config.rentCycleMonths > 1 && !config.collectRentMonthlyInstallment) {
                val monthlyEquiv = ledger.currentRentShare / config.rentCycleMonths
                appendLine("📌 (ملاحظة: حصة الشهر الواحد من الإيجار هي ${Formatters.formatMoney(monthlyEquiv, currency)})")
            }
            appendLine("جزاكم الله خيراً على تعاونكم.")
        }
    }

    fun buildApartmentSummaryMessage(
        items: List<ResidentLedgerWithDetails>,
        period: BillingPeriodEntity,
        config: ApartmentConfigEntity
    ): String {
        val currency = config.currencySymbol
        val totalDue = items.sumOf { it.ledger.totalDue }
        val totalPaid = items.sumOf { it.ledger.totalPaid }
        val totalRemaining = items.sumOf { it.ledger.totalRemaining }
        val unpaid = items.filter { it.ledger.totalRemaining > 0.05 }

        return buildString {
            appendLine("📋 ملخص ${config.apartmentName} - ${period.label}")
            appendLine("⚡ فاتورة الكهرباء: ${Formatters.formatMoney(period.electricityTotalAmount, currency)} (${Formatters.formatKwh(period.consumptionKwh)})")
            appendLine("🏠 حصة الإيجار للفترة: ${Formatters.formatMoney(period.rentPoolAmount, currency)}")
            appendLine("✅ المحصل: ${Formatters.formatMoney(totalPaid, currency)} من ${Formatters.formatMoney(totalDue, currency)}")
            appendLine("⏳ المتبقي للتحصيل: ${Formatters.formatMoney(totalRemaining, currency)}")
            if (unpaid.isNotEmpty()) {
                appendLine("━━━━━━━━━━━━━━")
                appendLine("المتأخرات / المستحقات المتبقية:")
                unpaid.forEach { entry ->
                    val r = entry.resident
                    val l = entry.ledger
                    appendLine("• ${r.name} (غ${r.roomNumber}): ${Formatters.formatMoney(l.totalRemaining, currency)} [سكن: ${Formatters.formatMoney(l.remainingRent)} | كهرباء: ${Formatters.formatMoney(l.remainingElectricity)}]")
                }
            } else {
                appendLine("🎉 تم سداد جميع المستحقات بالكامل!")
            }
        }
    }

    fun openWhatsAppForResident(
        context: Context,
        phoneNumber: String,
        message: String
    ) {
        val sanitized = sanitizePhone(phoneNumber)
        val encoded = URLEncoder.encode(message, "UTF-8")

        if (sanitized.isNotEmpty()) {
            val url = "https://wa.me/$sanitized?text=$encoded"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Fallback to share sheet below
            }
        }

        shareTextViaChooser(context, message)
    }

    fun shareTextViaChooser(context: Context, message: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(sendIntent)
        } catch (_: Exception) {
            try {
                val generic = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(
                    Intent.createChooser(generic, "مشاركة التذكير عبر")
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
            } catch (_: Exception) {
                Toast.makeText(context, "تعذر فتح تطبيق المشاركة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sanitizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") && digits.length >= 9 -> digits.drop(1)
            else -> digits
        }
    }
}
