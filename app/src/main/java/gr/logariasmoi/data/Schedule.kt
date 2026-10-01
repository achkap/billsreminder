package gr.logariasmoi.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private const val MAX_ITERATIONS = 20_000

/** The n-th due date (0-based), always computed from the start so month-end dates stay stable. */
fun Bill.occurrence(n: Int): LocalDate {
    val k = n.toLong() * repeatEvery.coerceAtLeast(1)
    return when (repeatUnit) {
        RepeatUnit.NONE -> startDate
        RepeatUnit.DAY -> startDate.plusDays(k)
        RepeatUnit.WEEK -> startDate.plusWeeks(k)
        RepeatUnit.MONTH -> startDate.plusMonths(k)
        RepeatUnit.YEAR -> startDate.plusYears(k)
    }
}

/** Rough index of the first occurrence on/after [date], used to skip ahead cheaply. */
private fun Bill.indexNear(date: LocalDate): Int {
    if (repeatUnit == RepeatUnit.NONE || !date.isAfter(startDate)) return 0
    val every = repeatEvery.coerceAtLeast(1)
    val units = when (repeatUnit) {
        RepeatUnit.DAY -> ChronoUnit.DAYS.between(startDate, date)
        RepeatUnit.WEEK -> ChronoUnit.WEEKS.between(startDate, date)
        RepeatUnit.MONTH -> ChronoUnit.MONTHS.between(startDate, date)
        RepeatUnit.YEAR -> ChronoUnit.YEARS.between(startDate, date)
        RepeatUnit.NONE -> 0
    }
    return ((units / every) - 1).coerceAtLeast(0).toInt()
}

/** All due dates within [from]..[to] (inclusive), respecting the end date. */
fun Bill.datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
    val result = mutableListOf<LocalDate>()
    var n = indexNear(from)
    var guard = 0
    while (guard++ < MAX_ITERATIONS) {
        val d = occurrence(n)
        if (d.isAfter(to)) break
        if (endDate != null && d.isAfter(endDate)) break
        if (!d.isBefore(from)) result += d
        if (repeatUnit == RepeatUnit.NONE) break
        n++
    }
    return result
}

/** End date that yields exactly [count] payments. */
fun Bill.endDateForCount(count: Int): LocalDate = occurrence((count - 1).coerceAtLeast(0))

fun Bill.countUntil(end: LocalDate): Int = datesBetween(startDate, end).size

fun AppData.paymentFor(billId: String, date: LocalDate): Payment? =
    payments.firstOrNull { it.billId == billId && it.dueDate == date }

fun AppData.occurrencesBetween(from: LocalDate, to: LocalDate, includeArchived: Boolean = false): List<Occurrence> {
    val paid = payments.associateBy { it.billId to it.dueDate }
    return bills.filter { includeArchived || !it.archived }
        .flatMap { b -> b.datesBetween(from, to).map { Occurrence(b, it, paid[b.id to it]) } }
        .sortedBy { it.date }
}

/** Unpaid due dates before today. */
fun AppData.overdue(today: LocalDate = LocalDate.now()): List<Occurrence> {
    val paid = payments.map { it.billId to it.dueDate }.toSet()
    return bills.filter { !it.archived }.flatMap { b ->
        b.datesBetween(b.startDate, today.minusDays(1))
            .filter { (b.id to it) !in paid }
            .map { Occurrence(b, it, null) }
    }.sortedBy { it.date }
}

/** Next unpaid occurrence on/after today for every active bill. */
fun AppData.upcoming(today: LocalDate = LocalDate.now()): List<Occurrence> {
    val paid = payments.map { it.billId to it.dueDate }.toSet()
    return bills.filter { !it.archived }.mapNotNull { b ->
        var from = today
        var result: Occurrence? = null
        repeat(24) {
            if (result != null) return@repeat
            val dates = b.datesBetween(from, from.plusYears(2))
            if (dates.isEmpty()) return@repeat
            val d = dates.firstOrNull { (b.id to it) !in paid }
            if (d != null) result = Occurrence(b, d, null) else from = dates.last().plusDays(1)
        }
        result
    }.sortedBy { it.date }
}

fun Bill.nextDueDate(data: AppData, today: LocalDate = LocalDate.now()): LocalDate? =
    data.overdue(today).firstOrNull { it.bill.id == id }?.date
        ?: data.upcoming(today).firstOrNull { it.bill.id == id }?.date

/** A reminder that should be shown at [at] for the due date [date] of [bill]. */
data class ReminderEvent(val bill: Bill, val date: LocalDate, val at: Long, val daysBefore: Int)

/**
 * All reminder instants for unpaid occurrences between [fromMillis] and [toMillis], including
 * daily "overdue" nudges and snoozes.
 */
fun AppData.reminderEvents(fromMillis: Long, toMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<ReminderEvent> {
    val paid = payments.map { it.billId to it.dueDate }.toSet()
    val fromDate = java.time.Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
    val toDate = java.time.Instant.ofEpochMilli(toMillis).atZone(zone).toLocalDate()
    val maxBefore = 60L
    val overdueDays = if (settings.overdueDaily) 30L else 0L
    val events = mutableListOf<ReminderEvent>()
    for (b in bills) {
        if (b.archived) continue
        for (d in b.datesBetween(fromDate.minusDays(overdueDays), toDate.plusDays(maxBefore))) {
            if ((b.id to d) in paid) continue
            val offsets = b.reminderDays.map { it.toLong() } + (1..overdueDays).map { -it }
            for (off in offsets.distinct()) {
                val at = d.minusDays(off).atTime(b.reminderHour, b.reminderMinute).atZone(zone).toInstant().toEpochMilli()
                if (at in (fromMillis + 1)..toMillis) events += ReminderEvent(b, d, at, off.toInt())
            }
        }
    }
    for (s in snoozes) {
        val b = bills.firstOrNull { it.id == s.billId } ?: continue
        if ((b.id to s.dueDate) in paid) continue
        if (s.at in (fromMillis + 1)..toMillis) events += ReminderEvent(b, s.dueDate, s.at, 0)
    }
    return events.sortedBy { it.at }
}
