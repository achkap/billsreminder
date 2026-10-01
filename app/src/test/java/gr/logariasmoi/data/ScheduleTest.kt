package gr.logariasmoi.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ScheduleTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun bill(start: String, unit: RepeatUnit, every: Int = 1, end: String? = null) =
        Bill(id = "b", name = "Test", amount = 10.0, startDate = d(start), repeatUnit = unit, repeatEvery = every, endDate = end?.let(::d))

    @Test fun monthEndIsClampedButNotDrifting() {
        val b = bill("2026-01-31", RepeatUnit.MONTH)
        assertEquals(
            listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30").map(::d),
            b.datesBetween(d("2026-01-01"), d("2026-04-30")),
        )
    }

    @Test fun everyTwoWeeks() {
        val b = bill("2026-09-01", RepeatUnit.WEEK, 2)
        assertEquals(listOf("2026-09-01", "2026-09-15", "2026-09-29").map(::d), b.datesBetween(d("2026-09-01"), d("2026-10-05")))
    }

    @Test fun skipAheadFindsSameDatesAsFullScan() {
        val b = bill("2020-03-15", RepeatUnit.DAY, 3)
        val all = b.datesBetween(b.startDate, d("2026-12-31"))
        val window = b.datesBetween(d("2026-06-01"), d("2026-06-30"))
        assertEquals(all.filter { it in d("2026-06-01")..d("2026-06-30") }, window)
    }

    @Test fun endDateAndCount() {
        val b = bill("2026-01-10", RepeatUnit.MONTH, 3, end = "2026-12-31")
        assertEquals(4, b.countUntil(d("2030-01-01")))
        assertEquals(d("2026-10-10"), b.endDateForCount(4))
    }

    @Test fun oneOff() {
        val b = bill("2026-05-05", RepeatUnit.NONE)
        assertEquals(listOf(d("2026-05-05")), b.datesBetween(d("2020-01-01"), d("2030-01-01")))
    }

    @Test fun overdueAndUpcomingRespectPayments() {
        val b = bill("2026-07-01", RepeatUnit.MONTH)
        val data = AppData(bills = listOf(b), payments = listOf(Payment(billId = "b", dueDate = d("2026-07-01"), paidDate = d("2026-07-01"), amount = 10.0)))
        val today = d("2026-09-15")
        assertEquals(listOf(d("2026-08-01"), d("2026-09-01")), data.overdue(today).map { it.date })
        assertEquals(listOf(d("2026-10-01")), data.upcoming(today).map { it.date })
    }

    @Test fun upcomingSkipsAlreadyPaidFutureDates() {
        val b = bill("2026-10-01", RepeatUnit.MONTH)
        val data = AppData(bills = listOf(b), payments = listOf(Payment(billId = "b", dueDate = d("2026-10-01"), paidDate = d("2026-09-20"), amount = 10.0)))
        assertEquals(listOf(d("2026-11-01")), data.upcoming(d("2026-09-25")).map { it.date })
    }

    @Test fun reminderEventsAtConfiguredTime() {
        val b = bill("2026-10-10", RepeatUnit.NONE).copy(reminderDays = listOf(3, 0), reminderHour = 9, reminderMinute = 30)
        val data = AppData(bills = listOf(b), settings = Settings(overdueDaily = false))
        val z = ZoneOffset.UTC
        val from = d("2026-10-01").atStartOfDay(z).toInstant().toEpochMilli()
        val to = d("2026-10-31").atStartOfDay(z).toInstant().toEpochMilli()
        val ev = data.reminderEvents(from, to, z)
        assertEquals(listOf(d("2026-10-07").atTime(9, 30), d("2026-10-10").atTime(9, 30)), ev.map { java.time.Instant.ofEpochMilli(it.at).atZone(z).toLocalDateTime() })
    }

    @Test fun overdueDailyNudges() {
        val b = bill("2026-10-10", RepeatUnit.NONE).copy(reminderDays = listOf(0))
        val data = AppData(bills = listOf(b), settings = Settings(overdueDaily = true))
        val z = ZoneOffset.UTC
        val ev = data.reminderEvents(d("2026-10-11").atStartOfDay(z).toInstant().toEpochMilli(), d("2026-10-14").atStartOfDay(z).toInstant().toEpochMilli(), z)
        assertEquals(3, ev.size)
        assertTrue(ev.all { it.daysBefore < 0 })
    }

    @Test fun jsonRoundTrip() {
        val data = AppData(bills = listOf(bill("2026-01-31", RepeatUnit.MONTH, end = "2027-01-01").copy(rfCode = "RF12 3456")))
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val text = json.encodeToString(AppData.serializer(), data)
        assertEquals(data, json.decodeFromString(AppData.serializer(), text))
    }

    @Test fun capitalizesFirstLetterOnly() {
        assertEquals("Δεη σπίτι", Fmt.capitalizeFirst("δεη σπίτι"))
        assertEquals("Netflix", Fmt.capitalizeFirst("netflix"))
        assertEquals("", Fmt.capitalizeFirst(""))
    }

    @Test fun languageSwitch() {
        I18n.apply("en")
        assertEquals("Monthly", Fmt.repeat(RepeatUnit.MONTH, 1))
        assertEquals("Electricity", Category.ELECTRICITY.label)
        I18n.apply("el")
        assertEquals("Κάθε μήνα", Fmt.repeat(RepeatUnit.MONTH, 1))
        assertEquals("Ρεύμα", Category.ELECTRICITY.label)
    }

    @Test fun badgeInitials() {
        assertEquals("ΔΕ", gr.logariasmoi.ui.badgeText("ΔΕΗ"))
        assertEquals("Ne", gr.logariasmoi.ui.badgeText("Netflix"))
        assertEquals("AB", gr.logariasmoi.ui.badgeText("Alpha Bank"))
        assertEquals("WV", gr.logariasmoi.ui.badgeText("Watt+Volt"))
        assertEquals("", gr.logariasmoi.ui.badgeText("  "))
    }

    @Test fun parseGreekAmounts() {
        assertEquals(45.2, Fmt.parseAmount("45,20")!!, 0.001)
        assertEquals(1234.5, Fmt.parseAmount("1.234,50")!!, 0.001)
        assertEquals(12.0, Fmt.parseAmount("12 €")!!, 0.001)
    }
}
