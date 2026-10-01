package gr.logariasmoi.ui

import gr.logariasmoi.data.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Occurrence
import gr.logariasmoi.data.Payment
import gr.logariasmoi.data.RepeatUnit
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.overdue
import gr.logariasmoi.data.upcoming
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(billId: String, snackbar: SnackbarHostState, onBack: () -> Unit, onEdit: () -> Unit) {
    val data by Repository.data.collectAsStateWithLifecycle()
    val bill = data.bills.firstOrNull { it.id == billId }
    if (bill == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val today = LocalDate.now()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val pending = remember(data) {
        (data.overdue(today) + data.upcoming(today)).filter { it.bill.id == billId }
    }
    val history = remember(data) { data.payments.filter { it.billId == billId }.sortedByDescending { it.dueDate } }
    var paying by remember { mutableStateOf<Occurrence?>(null) }
    var editing by remember { mutableStateOf<Payment?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun copy(label: String, value: String) {
        clipboard.setText(AnnotatedString(value))
        scope.launch { snackbar.showSnackbar(tr("$label αντιγράφηκε", "$label copied")) }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(bill.name) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Πίσω", "Back")) } },
            actions = {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, tr("Επεξεργασία", "Edit")) }
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, tr("Περισσότερα", "More")) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (bill.archived) tr("Επαναφορά", "Restore") else tr("Αρχειοθέτηση", "Archive")) },
                        leadingIcon = { Icon(if (bill.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                        onClick = { menu = false; Repository.saveBill(bill.copy(archived = !bill.archived)) },
                    )
                    DropdownMenuItem(
                        text = { Text(tr("Διαγραφή", "Delete")) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            },
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BillIcon(bill, 64.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(if (bill.amount > 0) Fmt.money(bill.amount) else "—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(bill.category.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (bill.archived) Text(tr("Αρχειοθετημένος", "Archived"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Info(tr("Επανάληψη", "Repeat"), Fmt.repeat(bill.repeatUnit, bill.repeatEvery))
                        Info(if (bill.repeatUnit == RepeatUnit.NONE) tr("Λήξη", "Due") else tr("Από", "From"), Fmt.full(bill.startDate))
                        if (bill.repeatUnit != RepeatUnit.NONE) Info(tr("Μέχρι", "Until"), bill.endDate?.let(Fmt::full) ?: tr("Χωρίς λήξη", "No end date"))
                        Info(
                            tr("Υπενθυμίσεις", "Reminders"),
                            if (bill.reminderDays.isEmpty()) tr("Καμία", "None")
                            else bill.reminderDays.sortedDescending().joinToString { Fmt.reminderDay(it) } + tr(" στις ", " at ") + Fmt.time(bill.reminderHour, bill.reminderMinute),
                        )
                        if (bill.autoDebit) Info(tr("Πληρωμή", "Payment"), tr("Πάγια εντολή", "Direct debit"))
                        if (bill.rfCode.isNotBlank()) Info(tr("Κωδικός RF", "RF code"), bill.rfCode) { copy(tr("Ο κωδικός RF", "RF code"), bill.rfCode) }
                        if (bill.accountNumber.isNotBlank()) Info(tr("Αρ. παροχής", "Account no."), bill.accountNumber) { copy(tr("Ο αριθμός", "Number"), bill.accountNumber) }
                        if (bill.notes.isNotBlank()) Info(tr("Σημειώσεις", "Notes"), bill.notes)
                    }
                }
            }
            if (pending.isNotEmpty()) {
                item { SectionTitle(tr("Εκκρεμούν", "Pending")) }
                items(pending, key = { "p${it.date}" }) { occ ->
                    OccurrenceRow(occ, onClick = { paying = occ }, onPay = { paying = occ })
                }
            }
            item {
                SectionTitle(tr("Ιστορικό πληρωμών", "Payment history"))
                if (history.isEmpty()) Text(tr("Καμία πληρωμή ακόμα.", "No payments yet."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Text(
                    tr("Σύνολο ${Fmt.money(history.sumOf { it.amount })} σε ${history.size} πληρωμές · μέσος όρος ${Fmt.money(history.sumOf { it.amount } / history.size)}", "Total ${Fmt.money(history.sumOf { it.amount })} in ${history.size} payments · average ${Fmt.money(history.sumOf { it.amount } / history.size)}"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(history, key = { it.id }) { p ->
                Card(onClick = { editing = p }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(Fmt.money(p.amount), fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(tr("Λήξη ${Fmt.num(p.dueDate)} · πληρώθηκε ${Fmt.num(p.paidDate)}", "Due ${Fmt.num(p.dueDate)} · paid ${Fmt.num(p.paidDate)}")) },
                        trailingContent = {
                            IconButton(onClick = { Repository.deletePayment(p.id) }) { Icon(Icons.Outlined.Delete, tr("Αναίρεση πληρωμής", "Undo payment")) }
                        },
                    )
                }
            }
            if (pending.isEmpty() && !bill.archived) {
                item { Text(tr("Δεν υπάρχουν άλλες πληρωμές για αυτόν τον λογαριασμό.", "No more payments for this bill."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    paying?.let { occ ->
        PayDialog(occ.bill.name, occ.date, occ.bill.amount, onDismiss = { paying = null }) { amount, paid ->
            Repository.markPaid(occ.bill.id, occ.date, amount, paid); paying = null
        }
    }
    editing?.let { p ->
        PayDialog(
            bill.name, p.dueDate, p.amount, initialPaidDate = p.paidDate, confirmText = tr("Αποθήκευση", "Save"),
            onDismiss = { editing = null },
        ) { amount, paid -> Repository.updatePayment(p.copy(amount = amount, paidDate = paid)); editing = null }
    }
    if (confirmDelete) {
        ConfirmDialog(
            tr("Διαγραφή λογαριασμού;", "Delete bill?"), tr("Θα διαγραφεί το «${bill.name}» μαζί με όλο το ιστορικό πληρωμών του. Αν θέλεις απλώς να σταματήσουν οι υπενθυμίσεις, προτίμησε την αρχειοθέτηση.", "“${bill.name}” and its whole payment history will be deleted. If you only want the reminders to stop, archive it instead."),
            tr("Διαγραφή", "Delete"), { confirmDelete = false },
        ) { Repository.deleteBill(bill.id); onBack() }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))

@Composable
private fun Info(label: String, value: String, onCopy: (() -> Unit)? = null) {
    ListItem(
        overlineContent = { Text(label) },
        headlineContent = { Text(value) },
        trailingContent = onCopy?.let { { IconButton(onClick = it) { Icon(Icons.Outlined.ContentCopy, tr("Αντιγραφή", "Copy")) } } },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    )
}
