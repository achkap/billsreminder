package gr.logariasmoi.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import gr.logariasmoi.notify.Notifications
import gr.logariasmoi.notify.ReminderScheduler
import gr.logariasmoi.widget.BillsWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/** Single source of truth. Everything lives in one JSON file in the app's private storage. */
object Repository {
    const val BACKUP_FILE_NAME = "logariasmoi-backup.json"

    val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    private lateinit var appContext: Context
    private lateinit var file: File
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()
    val current: AppData get() = _data.value

    @Synchronized
    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        file = File(appContext.filesDir, "data.json")
        if (file.exists()) {
            runCatching { json.decodeFromString<AppData>(file.readText()) }
                .onSuccess { _data.value = it }
        } else {
            val defaults = AppData()
            _data.value = defaults
            write(defaults)
        }
        I18n.apply(_data.value.settings.language)
    }

    @Synchronized
    fun update(transform: (AppData) -> AppData) {
        val next = transform(_data.value)
        if (next == _data.value) return
        val languageChanged = next.settings.language != _data.value.settings.language
        I18n.apply(next.settings.language)
        _data.value = next
        if (languageChanged) Notifications.createChannel(appContext)
        write(next)
        afterChange()
    }

    private fun write(d: AppData) {
        val tmp = File(file.parentFile, "data.json.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(file)) {
            file.delete(); tmp.renameTo(file)
        }
    }

    private fun afterChange() {
        ReminderScheduler.schedule(appContext)
        BillsWidget.refresh(appContext)
        autoBackup()
    }

    // ---- Bills & payments -------------------------------------------------------------------

    fun saveBill(bill: Bill) = update { d ->
        val exists = d.bills.any { it.id == bill.id }
        d.copy(bills = if (exists) d.bills.map { if (it.id == bill.id) bill else it } else d.bills + bill)
    }

    fun deleteBill(id: String) = update { d ->
        d.copy(
            bills = d.bills.filterNot { it.id == id },
            payments = d.payments.filterNot { it.billId == id },
            snoozes = d.snoozes.filterNot { it.billId == id },
        )
    }

    fun markPaid(billId: String, dueDate: LocalDate, amount: Double? = null, paidDate: LocalDate = LocalDate.now()) = update { d ->
        if (d.paymentFor(billId, dueDate) != null) return@update d
        val bill = d.bills.firstOrNull { it.id == billId } ?: return@update d
        d.copy(
            payments = d.payments + Payment(billId = billId, dueDate = dueDate, paidDate = paidDate, amount = amount ?: bill.amount),
            snoozes = d.snoozes.filterNot { it.billId == billId && it.dueDate == dueDate },
        )
    }

    fun updatePayment(p: Payment) = update { d -> d.copy(payments = d.payments.map { if (it.id == p.id) p else it }) }

    fun deletePayment(id: String) = update { d -> d.copy(payments = d.payments.filterNot { it.id == id }) }

    fun snooze(billId: String, dueDate: LocalDate, at: Long) = update { d ->
        d.copy(snoozes = d.snoozes.filterNot { it.billId == billId && it.dueDate == dueDate } + Snooze(billId, dueDate, at))
    }

    fun updateSettings(transform: (Settings) -> Settings) = update { d -> d.copy(settings = transform(d.settings)) }

    fun setLastReminderCheck(at: Long) = update { d ->
        d.copy(lastReminderCheck = at, snoozes = d.snoozes.filter { it.at > at })
    }

    // ---- Import / export ----------------------------------------------------------------------

    fun exportJson(): String = json.encodeToString(
        AppData.serializer(),
        current.copy(settings = current.settings.copy(backupFolder = null, lastBackupAt = 0)),
    )

    /** Replaces all bills and payments with the ones in [text]. Throws if the file is invalid. */
    fun importJson(text: String): Int {
        val imported = json.decodeFromString<AppData>(text)
        update { d ->
            imported.copy(
                settings = imported.settings.copy(backupFolder = d.settings.backupFolder, lastBackupAt = d.settings.lastBackupAt),
                lastReminderCheck = System.currentTimeMillis(),
                snoozes = emptyList(),
            )
        }
        return imported.bills.size
    }

    fun exportTo(uri: Uri) {
        appContext.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(exportJson().toByteArray()) }
    }

    fun importFrom(uri: Uri): Int {
        val text = appContext.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        return importJson(text)
    }

    /** Writes a copy to the user-chosen folder (if any). Failures are silent; the UI shows the last success. */
    fun autoBackup(): Boolean {
        val folder = current.settings.backupFolder ?: return false
        return runCatching {
            val tree = DocumentFile.fromTreeUri(appContext, Uri.parse(folder)) ?: error("no folder")
            val target = tree.findFile(BACKUP_FILE_NAME) ?: tree.createFile("application/json", BACKUP_FILE_NAME.removeSuffix(".json"))
                ?: error("cannot create")
            appContext.contentResolver.openOutputStream(target.uri, "wt")!!.use { it.write(exportJson().toByteArray()) }
            val now = System.currentTimeMillis()
            _data.value = _data.value.copy(settings = _data.value.settings.copy(lastBackupAt = now))
            write(_data.value)
        }.isSuccess
    }
}
