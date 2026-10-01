package gr.logariasmoi.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale

object Fmt {
    private class Formatters(val locale: Locale) {
        val money: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance("EUR") }
        val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", locale)
        val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
        val numDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
        val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
        val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
    }

    private var cache = Formatters(I18n.locale)
    private val f: Formatters
        get() = cache.takeIf { it.locale == I18n.locale } ?: Formatters(I18n.locale).also { cache = it }

    val locale: Locale get() = I18n.locale

    fun money(v: Double): String = f.money.format(v)
    fun short(d: LocalDate): String = f.shortDate.format(d)
    fun full(d: LocalDate): String = f.fullDate.format(d)
    fun num(d: LocalDate): String = f.numDate.format(d)
    fun monthYear(d: LocalDate): String = f.monthYear.format(d).replaceFirstChar { it.titlecase(f.locale) }
    fun dateTime(millis: Long): String =
        f.dateTime.format(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()))
    fun time(h: Int, m: Int) = "%02d:%02d".format(h, m)

    /** "σήμερα", "αύριο", "σε 5 μέρες", "πριν 2 μέρες" */
    fun relative(d: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, d)
        return when {
            days == 0L -> tr("σήμερα", "today")
            days == 1L -> tr("αύριο", "tomorrow")
            days == -1L -> tr("χθες", "yesterday")
            days > 1 -> tr("σε $days μέρες", "in $days days")
            else -> tr("πριν ${-days} μέρες", "${-days} days ago")
        }
    }

    /** Accepts both "1.234,50" and "1234.50". */
    fun parseAmount(s: String): Double? = s.trim().replace("€", "").replace(" ", "")
        .let { if (it.count { c -> c == ',' } == 1 && it.contains('.')) it.replace(".", "") else it }
        .replace(',', '.').toDoubleOrNull()

    fun amountInput(v: Double): String =
        if (v == 0.0) "" else "%.2f".format(Locale.US, v).let { if (I18n.english) it else it.replace('.', ',') }

    fun repeat(unit: RepeatUnit, every: Int): String = when (unit) {
        RepeatUnit.NONE -> tr("Εφάπαξ", "One-off")
        RepeatUnit.DAY -> if (every == 1) tr("Κάθε μέρα", "Every day") else tr("Κάθε $every μέρες", "Every $every days")
        RepeatUnit.WEEK -> if (every == 1) tr("Κάθε εβδομάδα", "Every week") else tr("Κάθε $every εβδομάδες", "Every $every weeks")
        RepeatUnit.MONTH -> when (every) {
            1 -> tr("Κάθε μήνα", "Monthly")
            2 -> tr("Κάθε δίμηνο", "Every 2 months")
            3 -> tr("Κάθε τρίμηνο", "Quarterly")
            4 -> tr("Κάθε τετράμηνο", "Every 4 months")
            6 -> tr("Κάθε εξάμηνο", "Every 6 months")
            12 -> tr("Κάθε χρόνο", "Yearly")
            else -> tr("Κάθε $every μήνες", "Every $every months")
        }
        RepeatUnit.YEAR -> if (every == 1) tr("Κάθε χρόνο", "Yearly") else tr("Κάθε $every χρόνια", "Every $every years")
    }

    fun reminderDay(days: Int): String = when (days) {
        0 -> tr("Ανήμερα", "On the day")
        1 -> tr("1 μέρα πριν", "1 day before")
        7 -> tr("1 εβδομάδα πριν", "1 week before")
        14 -> tr("2 εβδομάδες πριν", "2 weeks before")
        else -> tr("$days μέρες πριν", "$days days before")
    }

    /** Capitalises the first letter, leaving the rest as typed. */
    fun capitalizeFirst(s: String): String = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
