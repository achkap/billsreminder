package gr.logariasmoi.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.util.UUID

enum class Category(private val el: String, private val en: String) {
    ELECTRICITY("Ρεύμα", "Electricity"),
    GAS("Φυσικό αέριο", "Natural gas"),
    WATER("Νερό", "Water"),
    TELECOM("Κινητή, Internet & TV", "Mobile, Internet & TV"),
    BANK("Τράπεζες & Κάρτες", "Banks & Cards"),
    INSURANCE("Ασφάλειες", "Insurance"),
    PUBLIC("Δημόσιο, Φόροι & Διόδια", "Government, Taxes & Tolls"),
    SUBSCRIPTION("Συνδρομές", "Subscriptions"),
    HOME("Σπίτι", "Home"),
    OTHER("Άλλο", "Other");

    val label: String get() = tr(el, en)
}

enum class RepeatUnit { NONE, DAY, WEEK, MONTH, YEAR }

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

@Serializable
data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val providerId: String? = null,
    val name: String,
    val category: Category = Category.OTHER,
    val amount: Double = 0.0,
    /** First due date; all later occurrences are computed from it. */
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate,
    val repeatUnit: RepeatUnit = RepeatUnit.MONTH,
    val repeatEvery: Int = 1,
    /** Last date an occurrence may fall on; null = forever. */
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate? = null,
    /** Days before the due date to remind (0 = on the day). */
    val reminderDays: List<Int> = listOf(3, 0),
    val reminderHour: Int = 10,
    val reminderMinute: Int = 0,
    val rfCode: String = "",
    val accountNumber: String = "",
    val autoDebit: Boolean = false,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val billId: String,
    @Serializable(with = LocalDateSerializer::class) val dueDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val paidDate: LocalDate,
    val amount: Double,
    val note: String = "",
)

@Serializable
data class Snooze(
    val billId: String,
    @Serializable(with = LocalDateSerializer::class) val dueDate: LocalDate,
    val at: Long,
)

@Serializable
data class Settings(
    val overdueDaily: Boolean = true,
    val snoozeMinutes: Int = 180,
    val defaultReminderHour: Int = 10,
    val defaultReminderMinute: Int = 0,
    val defaultReminderDays: List<Int> = listOf(3, 0),
    /** Tree URI of the folder used for automatic backups (device-specific, not exported). */
    val backupFolder: String? = null,
    val lastBackupAt: Long = 0,
    /** "system", "light" or "dark". */
    val themeMode: String = "system",
    /** Pure black (AMOLED) backgrounds when the dark theme is active. */
    val pureBlack: Boolean = false,
    /** "system", "el" or "en". */
    val language: String = "system",
)

@Serializable
data class AppData(
    val version: Int = 1,
    val bills: List<Bill> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val snoozes: List<Snooze> = emptyList(),
    val settings: Settings = Settings(),
    /** Reminders at or before this instant have already been shown. */
    val lastReminderCheck: Long = System.currentTimeMillis(),
)

/** One concrete due date of a bill. */
data class Occurrence(val bill: Bill, val date: LocalDate, val payment: Payment?) {
    val paid get() = payment != null
    val amount get() = payment?.amount ?: bill.amount
}
