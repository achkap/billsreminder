package gr.logariasmoi.ui

import gr.logariasmoi.data.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.nextDueDate
import gr.logariasmoi.data.occurrencesBetween
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(onOpenBill: (String) -> Unit) {
    val data by Repository.data.collectAsStateWithLifecycle()
    var yearly by rememberSaveable { mutableStateOf(false) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val from = if (yearly) LocalDate.of(month.year, 1, 1) else month.atDay(1)
    val to = if (yearly) LocalDate.of(month.year, 12, 31) else month.atEndOfMonth()
    val occ = remember(data, from, to) { data.occurrencesBetween(from, to, includeArchived = true) }
    val total = occ.sumOf { it.amount }
    val paid = occ.filter { it.paid }.sumOf { it.amount }
    val byCat = occ.groupBy { it.bill.category }.mapValues { (_, v) -> v.sumOf { it.amount } }.toList().sortedByDescending { it.second }
    val maxCat = byCat.maxOfOrNull { it.second } ?: 0.0
    val today = LocalDate.now()

    Scaffold(topBar = { TopAppBar(title = { Text(tr("Σύνοψη", "Summary")) }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(!yearly, { yearly = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(tr("Μήνας", "Month")) }
                    SegmentedButton(yearly, { yearly = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(tr("Έτος", "Year")) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    IconButton(onClick = { month = if (yearly) month.minusYears(1) else month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Προηγούμενο", "Previous"))
                    }
                    Text(
                        if (yearly) month.year.toString() else Fmt.monthYear(month.atDay(1)),
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { month = if (yearly) month.plusYears(1) else month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, tr("Επόμενο", "Next"))
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        Stat(tr("Σύνολο", "Total"), Fmt.money(total), Modifier.weight(1f))
                        Stat(tr("Πληρώθηκαν", "Paid"), Fmt.money(paid), Modifier.weight(1f))
                        Stat(tr("Απομένουν", "Remaining"), Fmt.money(total - paid), Modifier.weight(1f))
                    }
                }
            }
            if (byCat.isNotEmpty()) {
                item { Text(tr("Ανά κατηγορία", "By category"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
                items(byCat, key = { it.first.name }) { (cat, sum) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(cat.icon(), null, tint = cat.color(), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(cat.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(Fmt.money(sum), fontWeight = FontWeight.SemiBold)
                            }
                            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Box(
                                    Modifier.fillMaxHeight().fillMaxWidth(if (maxCat > 0) (sum / maxCat).toFloat() else 0f)
                                        .clip(RoundedCornerShape(4.dp)).background(cat.color()),
                                )
                            }
                        }
                    }
                }
            }
            if (!yearly) {
                item { Text(tr("Πληρωμές μήνα", "This month’s payments"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
                if (occ.isEmpty()) item { Text(tr("Καμία πληρωμή.", "No payments."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(occ, key = { "${it.bill.id}_${it.date}" }) { OccurrenceRow(it, onClick = { onOpenBill(it.bill.id) }, onPay = null) }
            }
            item { Text(tr("Όλοι οι λογαριασμοί (${data.bills.size})", "All bills (${data.bills.size})"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp)) }
            items(data.bills.sortedWith(compareBy({ it.archived }, { it.name.lowercase() })), key = { "all_${it.id}" }) { b ->
                Card(onClick = { onOpenBill(b.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BillIcon(b, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, style = MaterialTheme.typography.titleSmall)
                            val next = if (b.archived) null else b.nextDueDate(data, today)
                            Text(
                                when {
                                    b.archived -> tr("Αρχειοθετημένος", "Archived")
                                    next != null -> tr("${Fmt.repeat(b.repeatUnit, b.repeatEvery)} · επόμενη ${Fmt.num(next)}", "${Fmt.repeat(b.repeatUnit, b.repeatEvery)} · next ${Fmt.num(next)}")
                                    else -> tr("${Fmt.repeat(b.repeatUnit, b.repeatEvery)} · ολοκληρώθηκε", "${Fmt.repeat(b.repeatUnit, b.repeatEvery)} · completed")
                                },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (b.amount > 0) Text(Fmt.money(b.amount))
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
