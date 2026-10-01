package gr.logariasmoi.ui

import gr.logariasmoi.data.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gr.logariasmoi.data.Bill
import gr.logariasmoi.data.Category
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Providers
import gr.logariasmoi.data.RepeatUnit
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.countUntil
import gr.logariasmoi.data.datesBetween
import gr.logariasmoi.data.endDateForCount
import java.time.LocalDate

private data class RepeatPreset(val label: String, val unit: RepeatUnit, val every: Int)

private val presets get() = listOf(
    RepeatPreset(tr("Εφάπαξ", "One-off"), RepeatUnit.NONE, 1),
    RepeatPreset(tr("Εβδομάδα", "Week"), RepeatUnit.WEEK, 1),
    RepeatPreset(tr("2 εβδομάδες", "2 weeks"), RepeatUnit.WEEK, 2),
    RepeatPreset(tr("Μήνας", "Month"), RepeatUnit.MONTH, 1),
    RepeatPreset(tr("2 μήνες", "2 months"), RepeatUnit.MONTH, 2),
    RepeatPreset(tr("3 μήνες", "3 months"), RepeatUnit.MONTH, 3),
    RepeatPreset(tr("6 μήνες", "6 months"), RepeatUnit.MONTH, 6),
    RepeatPreset(tr("Έτος", "Year"), RepeatUnit.YEAR, 1),
)

private val unitLabels get() = listOf(
    RepeatUnit.DAY to tr("μέρες", "days"),
    RepeatUnit.WEEK to tr("εβδομάδες", "weeks"),
    RepeatUnit.MONTH to tr("μήνες", "months"),
    RepeatUnit.YEAR to tr("χρόνια", "years"),
)

private val reminderOptions = listOf(0, 1, 2, 3, 5, 7, 14)

private val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

private enum class EndMode { NEVER, DATE, COUNT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditBillScreen(billId: String?, providerId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val settings = Repository.current.settings
    val existing = remember(billId) { Repository.current.bills.firstOrNull { it.id == billId } }
    val initialProvider = Providers.get(existing?.providerId ?: providerId)

    var provider by remember { mutableStateOf(initialProvider) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: initialProvider?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: initialProvider?.category ?: Category.OTHER) }
    var amountText by rememberSaveable { mutableStateOf(existing?.amount?.let(Fmt::amountInput) ?: "") }
    var startDate by rememberSaveable { mutableStateOf(existing?.startDate ?: LocalDate.now()) }
    var unit by rememberSaveable { mutableStateOf(existing?.repeatUnit ?: RepeatUnit.MONTH) }
    var every by rememberSaveable { mutableStateOf(existing?.repeatEvery ?: 1) }
    var custom by rememberSaveable {
        mutableStateOf(existing != null && presets.none { it.unit == existing.repeatUnit && it.every == existing.repeatEvery })
    }
    var endMode by rememberSaveable { mutableStateOf(if (existing?.endDate != null) EndMode.DATE else EndMode.NEVER) }
    var endDate by rememberSaveable { mutableStateOf(existing?.endDate ?: LocalDate.now().plusYears(1)) }
    var endCount by rememberSaveable { mutableStateOf("12") }
    var reminders by rememberSaveable { mutableStateOf(existing?.reminderDays ?: settings.defaultReminderDays) }
    var hour by rememberSaveable { mutableStateOf(existing?.reminderHour ?: settings.defaultReminderHour) }
    var minute by rememberSaveable { mutableStateOf(existing?.reminderMinute ?: settings.defaultReminderMinute) }
    var rf by rememberSaveable { mutableStateOf(existing?.rfCode ?: "") }
    var account by rememberSaveable { mutableStateOf(existing?.accountNumber ?: "") }
    var autoDebit by rememberSaveable { mutableStateOf(existing?.autoDebit ?: false) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }

    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var pickProvider by remember { mutableStateOf(false) }
    var addReminder by remember { mutableStateOf(false) }

    val amount = if (amountText.isBlank()) 0.0 else Fmt.parseAmount(amountText)
    val count = endCount.toIntOrNull()?.takeIf { it > 0 }
    val valid = name.isNotBlank() && amount != null && every > 0 && (endMode != EndMode.COUNT || count != null)

    fun build(): Bill {
        val base = (existing ?: Bill(name = name, startDate = startDate)).copy(
            providerId = provider?.id, name = Fmt.capitalizeFirst(name.trim()), category = category, amount = amount ?: 0.0,
            startDate = startDate, repeatUnit = unit, repeatEvery = every.coerceAtLeast(1),
            reminderDays = reminders.sortedDescending(), reminderHour = hour, reminderMinute = minute,
            rfCode = rf.trim().uppercase(), accountNumber = account.trim(), autoDebit = autoDebit, notes = Fmt.capitalizeFirst(notes.trim()), endDate = null,
        )
        val end = when {
            unit == RepeatUnit.NONE -> null
            endMode == EndMode.DATE -> endDate
            endMode == EndMode.COUNT -> base.endDateForCount(count ?: 1)
            else -> null
        }
        return base.copy(endDate = end)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (existing == null) tr("Νέος λογαριασμός", "New bill") else tr("Επεξεργασία", "Edit")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Πίσω", "Back")) } },
            actions = {
                TextButton(enabled = valid, onClick = {
                    val b = build(); Repository.saveBill(b); onSaved(b.id)
                }) { Text(tr("Αποθήκευση", "Save")) }
            },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Company ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderIcon(provider?.id, name, category, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(provider?.name ?: tr("Δικός μου λογαριασμός", "Custom bill"), style = MaterialTheme.typography.titleMedium)
                    Text(category.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { pickProvider = true }) { Text(tr("Αλλαγή", "Change")) }
            }
            OutlinedTextField(
                name, { name = it }, label = { Text(tr("Όνομα", "Name")) }, singleLine = true, keyboardOptions = sentences,
                supportingText = { Text(tr("π.χ. «ΔΕΗ σπίτι», «Κινητό Μαρίας»", "e.g. “Home electricity”, “Maria’s phone”")) },
                isError = name.isBlank(), modifier = Modifier.fillMaxWidth(),
            )
            CategoryDropdown(category) { category = it }
            OutlinedTextField(
                amountText, { amountText = it }, label = { Text(tr("Ποσό (€)", "Amount (€)")) }, singleLine = true,
                isError = amount == null,
                supportingText = { Text(tr("Αν αλλάζει κάθε φορά, βάλε ένα ενδεικτικό· το διορθώνεις όταν πληρώνεις.", "If it changes every time, enter an estimate; you can correct it when you pay.")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                Fmt.full(startDate), {}, readOnly = true,
                label = { Text(if (unit == RepeatUnit.NONE) tr("Ημερομηνία λήξης", "Due date") else tr("Πρώτη ημερομηνία λήξης", "First due date")) },
                trailingIcon = { IconButton(onClick = { pickStart = true }) { Icon(Icons.Outlined.CalendarMonth, tr("Επιλογή", "Pick")) } },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            // --- Repeat ---
            Text(tr("Επανάληψη", "Repeat"), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p ->
                    FilterChip(
                        selected = !custom && unit == p.unit && every == p.every,
                        onClick = { custom = false; unit = p.unit; every = p.every },
                        label = { Text(p.label) },
                    )
                }
                FilterChip(
                    selected = custom,
                    onClick = { custom = true; if (unit == RepeatUnit.NONE) unit = RepeatUnit.MONTH },
                    label = { Text(tr("Προσαρμογή…", "Custom…")) },
                )
            }
            if (custom) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Κάθε", "Every"))
                    OutlinedTextField(
                        if (every == 0) "" else every.toString(),
                        { every = it.filter(Char::isDigit).take(3).toIntOrNull() ?: 0 },
                        singleLine = true, isError = every <= 0,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                    )
                    UnitDropdown(unit, Modifier.weight(1f)) { unit = it }
                }
            }
            if (unit != RepeatUnit.NONE) {
                Text(tr("Μέχρι", "Until"), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(EndMode.NEVER to tr("Πάντα", "Forever"), EndMode.DATE to tr("Ημερομηνία", "Date"), EndMode.COUNT to tr("Αρ. πληρωμών", "No. of payments"))
                        .forEachIndexed { i, (mode, label) ->
                            SegmentedButton(
                                selected = endMode == mode,
                                onClick = {
                                    if (mode == EndMode.COUNT && endMode == EndMode.DATE) {
                                        endCount = build().copy(endDate = null).countUntil(endDate).coerceAtLeast(1).toString()
                                    }
                                    endMode = mode
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, 3),
                            ) { Text(label, maxLines = 1) }
                        }
                }
                when (endMode) {
                    EndMode.DATE -> OutlinedTextField(
                        Fmt.full(endDate), {}, readOnly = true, label = { Text(tr("Τελευταία πληρωμή έως", "Last payment by")) },
                        isError = endDate.isBefore(startDate),
                        trailingIcon = { IconButton(onClick = { pickEnd = true }) { Icon(Icons.Outlined.CalendarMonth, tr("Επιλογή", "Pick")) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    EndMode.COUNT -> OutlinedTextField(
                        endCount, { endCount = it.filter(Char::isDigit).take(4) }, label = { Text(tr("Πλήθος πληρωμών", "Number of payments")) },
                        singleLine = true, isError = count == null,
                        supportingText = { count?.let { Text(tr("Τελευταία πληρωμή: ${Fmt.full(build().endDateForCount(it))}", "Last payment: ${Fmt.full(build().endDateForCount(it))}")) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    EndMode.NEVER -> {}
                }
                val preview = remember(startDate, unit, every, endMode, endDate, endCount) {
                    if (every > 0) build().datesBetween(startDate, startDate.plusYears(50)).take(4) else emptyList()
                }
                if (preview.isNotEmpty()) {
                    Text(
                        tr("${Fmt.repeat(unit, every)} · επόμενες: ", "${Fmt.repeat(unit, every)} · next: ") + preview.joinToString { Fmt.num(it) },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()
            // --- Reminders ---
            Text(tr("Υπενθυμίσεις", "Reminders"), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (reminderOptions + reminders).distinct().sorted().forEach { d ->
                    FilterChip(
                        selected = d in reminders,
                        onClick = { reminders = if (d in reminders) reminders - d else reminders + d },
                        label = { Text(Fmt.reminderDay(d)) },
                    )
                }
                AssistChip(onClick = { addReminder = true }, label = { Text(tr("+ Άλλο", "+ Other")) })
            }
            OutlinedButton(onClick = { showTimePicker(context, hour, minute) { h, m -> hour = h; minute = m } }) {
                Icon(Icons.Outlined.Schedule, null); Spacer(Modifier.width(8.dp)); Text(tr("Ώρα υπενθύμισης: ${Fmt.time(hour, minute)}", "Reminder time: ${Fmt.time(hour, minute)}"))
            }
            if (reminders.isEmpty()) {
                Text(tr("Χωρίς υπενθυμίσεις δεν θα λάβεις ειδοποίηση για αυτόν τον λογαριασμό.", "Without reminders you won’t be notified about this bill."), color = OverdueRed, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()
            // --- Payment details ---
            Text(tr("Στοιχεία πληρωμής", "Payment details"), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(rf, { rf = it }, label = { Text(tr("Κωδικός πληρωμής (RF)", "Payment code (RF)")) }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(account, { account = it }, label = { Text(tr("Αρ. παροχής / συμβολαίου / σύνδεσης", "Account / contract / connection no.")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Πάγια εντολή", "Direct debit"))
                    Text(tr("Χρεώνεται αυτόματα από τον λογαριασμό/κάρτα", "Charged automatically to your account/card"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(autoDebit, { autoDebit = it })
            }
            OutlinedTextField(notes, { notes = it }, label = { Text(tr("Σημειώσεις", "Notes")) }, minLines = 2, keyboardOptions = sentences, modifier = Modifier.fillMaxWidth())

            Button(enabled = valid, onClick = { val b = build(); Repository.saveBill(b); onSaved(b.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Αποθήκευση", "Save"))
            }
        }
    }

    if (pickStart) DatePickerModal(startDate, { pickStart = false }) { startDate = it }
    if (pickEnd) DatePickerModal(endDate, { pickEnd = false }) { endDate = it }
    if (addReminder) {
        var t by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addReminder = false },
            title = { Text(tr("Πόσες μέρες πριν;", "How many days before?")) },
            text = {
                OutlinedTextField(
                    t, { t = it.filter(Char::isDigit).take(2) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    t.toIntOrNull()?.let { if (it !in reminders) reminders = reminders + it }
                    addReminder = false
                }) { Text(tr("Προσθήκη", "Add")) }
            },
            dismissButton = { TextButton(onClick = { addReminder = false }) { Text(tr("Άκυρο", "Cancel")) } },
        )
    }
    if (pickProvider) {
        Dialog(onDismissRequest = { pickProvider = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ProviderPickerScreen(onBack = { pickProvider = false }) { p ->
                val oldDefault = provider?.name ?: ""
                provider = p
                if (p != null) {
                    category = p.category
                    if (name.isBlank() || name == oldDefault) name = p.name
                }
                pickProvider = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(value: Category, onChange: (Category) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value.label, {}, readOnly = true, label = { Text(tr("Κατηγορία", "Category")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Category.entries.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.label) }, leadingIcon = { Icon(c.icon(), null) },
                    onClick = { onChange(c); open = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(value: RepeatUnit, modifier: Modifier, onChange: (RepeatUnit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            unitLabels.first { it.first == value || value == RepeatUnit.NONE }.second, {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            unitLabels.forEach { (u, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { onChange(u); open = false }) }
        }
    }
}
