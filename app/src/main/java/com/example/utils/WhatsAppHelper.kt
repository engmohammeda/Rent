package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.ApartmentConfigEntity
import com.example.data.local.BillingPeriodEntity
import com.example.data.local.ResidentLedgerWithDetails
import java.net.URLEncoder

enum class ReminderTone(val label: String) {
    NATURAL("ودي وطبيعي"),
    SHORT("مختصر وسريع")
}

object WhatsAppHelper {

    fun buildResidentReminderMessage(
        item: ResidentLedgerWithDetails,
        period: BillingPeriodEntity,
        config: ApartmentConfigEntity,
        tone: ReminderTone = ReminderTone.NATURAL
    ): String {
        val currency = config.currencySymbol
        val ledger = item.ledger
        val resident = item.resident
        val ezbah = config.apartmentName

        // Advance credit case (paid ahead for upcoming months)
        if (ledger.totalRemaining <= 0.05 && ledger.totalAdvanceCredit > 0.05) {
            return "مساك الله بالخير أخي ${resident.name}، حبيت أطمنك إن حسابك في $ezbah لشهر ${period.label} تمام وما عليك أي متبقي حالياً، وعندك رصيد مدفوع مقدماً قدره ${Formatters.formatMoney(ledger.totalAdvanceCredit, currency)} يغطي الفترة الجاية. الله يبارك فيك."
        }

        // Completely paid
        if (ledger.totalRemaining <= 0.05) {
            return "مساك الله بالخير أخي ${resident.name}، يعطيك العافية تم تأكيد سداد كامل مستحقاتك في $ezbah لشهر ${period.label}."
        }

        if (tone == ReminderTone.SHORT) {
            return buildString {
                appendLine("مساك الله بالخير أخي ${resident.name}،")
                appendLine("مستحقات $ezbah لشهر ${period.label}:")
                if (ledger.remainingRent > 0.05) {
                    appendLine("السكن: ${Formatters.formatMoney(ledger.remainingRent, currency)}")
                }
                if (ledger.remainingElectricity > 0.05) {
                    appendLine("الكهرباء: ${Formatters.formatMoney(ledger.remainingElectricity, currency)}")
                }
                if (!config.waterIncludedInRent && ledger.remainingWater > 0.05) {
                    appendLine("الماء: ${Formatters.formatMoney(ledger.remainingWater, currency)}")
                }
                append("المطلوب: ${Formatters.formatMoney(ledger.totalRemaining, currency)} — أرسل المتيسر لك الله يعطيك العافية.")
            }
        }

        return buildString {
            appendLine("مساك الله بالخير أخي ${resident.name}،")
            appendLine("كوننا بدأنا التحصيل في $ezbah لشهر ${period.label}، حبيت أذكرك بتفاصيل حسابك:")
            appendLine()
            if (ledger.remainingRent > 0.05) {
                appendLine("• السكن: ${Formatters.formatMoney(ledger.remainingRent, currency)}")
            }
            if (ledger.remainingElectricity > 0.05) {
                appendLine("• الكهرباء: ${Formatters.formatMoney(ledger.remainingElectricity, currency)}")
            }
            if (!config.waterIncludedInRent && ledger.remainingWater > 0.05) {
                appendLine("• الماء: ${Formatters.formatMoney(ledger.remainingWater, currency)}")
            }
            if (ledger.previousArrearsDebt > 0.05) {
                appendLine("• (تشمل متبقي من الشهر السابق: ${Formatters.formatMoney(ledger.previousArrearsDebt, currency)})")
            }
            if (ledger.totalPaid > 0.05) {
                appendLine("• المدفوع حتى الآن: ${Formatters.formatMoney(ledger.totalPaid, currency)}")
            }
            appendLine()
            appendLine("المطلوب حالياً: ${Formatters.formatMoney(ledger.totalRemaining, currency)}")
            if (config.rentCycleMonths > 1 && config.collectRentMonthlyInstallment) {
                append("تقدر تدفع قسط الشهر أو تدفع حق شهرين أو الثلاثة أشهر مرة واحدة أو حسب المتوفر عندك الآن، الله يبارك فيك.")
            } else {
                append("تقدر تدفع المبلغ كامل أو حسب استطاعتك حالياً، الله يبارك فيك ويعطيك العافية.")
            }
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
            appendLine("مساء الخير للجميع، ملخص حسابات ${config.apartmentName} لشهر ${period.label}:")
            appendLine("• حصة السكن للفترة: ${Formatters.formatMoney(period.rentPoolAmount, currency)}")
            appendLine("• فاتورة الكهرباء: ${Formatters.formatMoney(period.electricityTotalAmount, currency)}")
            if (!config.waterIncludedInRent) {
                appendLine("• فاتورة الماء: ${Formatters.formatMoney(period.waterTotalAmount, currency)}")
            } else {
                appendLine("• الماء: مشمول ضمن الإيجار")
            }
            appendLine("• المحصّل حتى الآن: ${Formatters.formatMoney(totalPaid, currency)} من أصل ${Formatters.formatMoney(totalDue, currency)}")
            appendLine("• المتبقي للتحصيل: ${Formatters.formatMoney(totalRemaining, currency)}")
            if (unpaid.isNotEmpty()) {
                appendLine()
                appendLine("المتبقي على الإخوة:")
                unpaid.forEach { entry ->
                    val r = entry.resident
                    val l = entry.ledger
                    appendLine("- ${r.name} (غرفة ${r.roomNumber}): ${Formatters.formatMoney(l.totalRemaining, currency)}")
                }
            } else {
                appendLine()
                appendLine("الحمد لله، الجميع مسدد بالكامل لهذه الفترة.")
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
                // Fallback below
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
