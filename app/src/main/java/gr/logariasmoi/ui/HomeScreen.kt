package gr.logariasmoi.ui

import gr.logariasmoi.data.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Occurrence
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.occurrencesBetween
import gr.logariasmoi.data.overdue
import gr.logariasmoi.data.paymentFor
import gr.logariasmoi.data.upcoming
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(snackbar: SnackbarHostState, onOpenBill: (String) -> Unit, onAdd: () -> Unit) {
    val data by Repository.data.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val overdue = remember(data) { data.overdue(today) }
    val upcoming = remember(data) { data.upcoming(today) }
    val month = remember(data) {
        data.occurrencesBetween(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()))
    }
    val soon = upcoming.filter { ChronoUnit.DAYS.between(today, it.date) <= 7 }
    val later = upcoming - soon.toSet()
    var paying by remember { mutableStateOf<Occurrence?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text(tr("Λογαριασμοί", "Bills")) }) }) { padding ->
        if (data.bills.none { !it.archived }) {
            EmptyState(Modifier.padding(padding), onAdd)
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { MonthCard(month, overdue) }
            section(tr("Ληξιπρόθεσμοι", "Overdue"), overdue, OverdueRed, onOpenBill) { paying = it }
            section(tr("Τις επόμενες 7 μέρες", "Next 7 days"), soon, null, onOpenBill) { paying = it }
            section(tr("Αργότερα", "Later"), later, null, onOpenBill) { paying = it }
        }
    }

    paying?.let { occ ->
        PayDialog(
            title = occ.bill.name, dueDate = occ.date, amount = occ.bill.amount,
            onDismiss = { paying = null },
            onConfirm = { amount, paidDate ->
                Repository.markPaid(occ.bill.id, occ.date, amount, paidDate)
                paying = null
                scope.launch {
                    val r = snackbar.showSnackbar(tr("${occ.bill.name}: πληρώθηκε", "${occ.bill.name}: paid"), tr("Αναίρεση", "Undo"), duration = SnackbarDuration.Short)
                    if (r == SnackbarResult.ActionPerformed) {
                        Repository.current.paymentFor(occ.bill.id, occ.date)?.let { Repository.deletePayment(it.id) }
                    }
                }
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    items: List<Occurrence>,
    color: Color?,
    onOpen: (String) -> Unit,
    onPay: (Occurrence) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "h_$title") {
        Text(
            "$title (${items.size})",
            style = MaterialTheme.typography.titleSmall,
            color = color ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
        )
    }
    items(items, key = { "${title}_${it.bill.id}_${it.date}" }) { occ ->
        OccurrenceRow(occ, onClick = { onOpen(occ.bill.id) }, onPay = { onPay(occ) })
    }
}

@Composable
fun OccurrenceRow(occ: Occurrence, onClick: () -> Unit, onPay: (() -> Unit)?) {
    val today = LocalDate.now()
    val late = !occ.paid && occ.date.isBefore(today)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (late) OverdueRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BillIcon(occ.bill)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(occ.bill.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = buildString {
                    append(Fmt.short(occ.date))
                    if (!occ.paid) append(" · ").append(Fmt.relative(occ.date, today))
                    if (occ.bill.autoDebit) append(tr(" · πάγια", " · direct debit"))
                }
                Text(
                    sub, style = MaterialTheme.typography.bodySmall,
                    color = if (late) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (occ.amount > 0) {
                    Text(Fmt.money(occ.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (occ.paid) Text(tr("Πληρώθηκε", "Paid"), color = PaidGreen, style = MaterialTheme.typography.labelSmall)
            }
            if (onPay != null && !occ.paid) {
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onPay) { Icon(Icons.Outlined.Check, tr("Πλήρωσα", "Mark paid")) }
            }
        }
    }
}

@Composable
private fun MonthCard(month: List<Occurrence>, overdue: List<Occurrence>) {
    val total = month.sumOf { it.amount }
    val paid = month.filter { it.paid }.sumOf { it.amount }
    val today = LocalDate.now()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(Fmt.monthYear(today), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(tr("Υπόλοιπο ${Fmt.money(total - paid)}", "Remaining ${Fmt.money(total - paid)}"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                tr("Πληρώθηκαν ${Fmt.money(paid)} από ${Fmt.money(total)} · ${month.count { it.paid }}/${month.size} λογαριασμοί", "Paid ${Fmt.money(paid)} of ${Fmt.money(total)} · ${month.count { it.paid }}/${month.size} bills"),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) (paid / total).toFloat() else if (month.isNotEmpty() && month.all { it.paid }) 1f else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            if (overdue.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    tr("⚠ ${overdue.size} ληξιπρόθεσμοι · ${Fmt.money(overdue.sumOf { it.amount })}", "⚠ ${overdue.size} overdue · ${Fmt.money(overdue.sumOf { it.amount })}"),
                    color = OverdueRed, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onAdd: () -> Unit) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Text(tr("Δεν έχεις λογαριασμούς ακόμα", "You have no bills yet"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr("Πρόσθεσε ΔΕΗ, κινητό, Netflix ή οτιδήποτε πληρώνεις και θα σου θυμίζω πότε λήγουν.", "Add your electricity, phone, Netflix or anything else you pay and you’ll be reminded before it’s due."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(tr("Νέος λογαριασμός", "New bill")) }
        }
    }
}
