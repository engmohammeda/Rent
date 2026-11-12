package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {
    private val enSymbols = DecimalFormatSymbols(Locale.US)
    private val moneyFormat = DecimalFormat("#,##0.##", enSymbols)
    private val kwhFormat = DecimalFormat("#,##0.#", enSymbols)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("ar"))

    private val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    fun formatMoney(amount: Double, currency: String = ""): String {
        val clean = if (kotlin.math.abs(amount) < 0.01) 0.0 else amount
        val formatted = moneyFormat.format(round2(clean))
        return if (currency.isBlank()) formatted else "$formatted $currency"
    }

    fun formatKwh(kwh: Double): String = "${kwhFormat.format(round2(kwh))} ك.و.س"

    fun formatMonthLabel(year: Int, month: Int): String {
        val monthName = arabicMonths.getOrElse(month - 1) { "شهر $month" }
        return "$monthName $year"
    }

    fun formatDateTime(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun nextMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month >= 12) Pair(year + 1, 1) else Pair(year, month + 1)
    }

    fun previousMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month <= 1) Pair(year - 1, 12) else Pair(year, month - 1)
    }
}
