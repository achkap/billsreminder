package gr.logariasmoi.ui

import gr.logariasmoi.data.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Occurrence
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.occurrencesBetween
import java.time.LocalDate
import java.time.YearMonth

private val weekDays get() = listOf(tr("Δε", "Mo"), tr("Τρ", "Tu"), tr("Τε", "We"), tr("Πε", "Th"), tr("Πα", "Fr"), tr("Σα", "Sa"), tr("Κυ", "Su"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onOpenBill: (String) -> Unit) {
    val data by Repository.data.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    var selected by rememberSaveable { mutableStateOf(today) }
    val occ = remember(data, month) { data.occurrencesBetween(month.atDay(1), month.atEndOfMonth()) }
    val byDay = remember(occ) { occ.groupBy { it.date } }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(tr("Ημερολόγιο", "Calendar")) },
            actions = { TextButton(onClick = { month = YearMonth.from(today); selected = today }) { Text(tr("Σήμερα", "Today")) } },
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Προηγούμενος", "Previous")) }
                    Text(Fmt.monthYear(month.atDay(1)), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, tr("Επόμενος", "Next")) }
                }
                MonthGrid(month, today, selected, byDay) { selected = it }
                Text(
                    tr("Μήνας: ${Fmt.money(occ.sumOf { it.amount })} · πληρώθηκαν ${Fmt.money(occ.filter { it.paid }.sumOf { it.amount })}", "Month: ${Fmt.money(occ.sumOf { it.amount })} · paid ${Fmt.money(occ.filter { it.paid }.sumOf { it.amount })}"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(Fmt.full(selected), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            val dayItems = if (YearMonth.from(selected) == month) byDay[selected].orEmpty() else emptyList()
            if (dayItems.isEmpty()) item { Text(tr("Τίποτα για πληρωμή αυτή τη μέρα.", "Nothing due on this day."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(dayItems, key = { it.bill.id }) {
                Box(Modifier.padding(bottom = 8.dp)) { OccurrenceRow(it, onClick = { onOpenBill(it.bill.id) }, onPay = null) }
            }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, selected: LocalDate, byDay: Map<LocalDate, List<Occurrence>>, onSelect: (LocalDate) -> Unit) {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = offset + month.lengthOfMonth()
    Column {
        Row { weekDays.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) } }
        for (week in 0 until (cells + 6) / 7) {
            Row {
                for (dow in 0..6) {
                    val dayNum = week * 7 + dow - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(0.9f).padding(2.dp)) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            DayCell(date, date == today, date == selected, byDay[date].orEmpty(), today) { onSelect(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, items: List<Occurrence>, today: LocalDate, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val primary = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxSize().clip(shape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(if (isToday) Modifier.border(1.5.dp, primary, shape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(date.dayOfMonth.toString(), fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            items.take(4).forEach { o ->
                val c = when {
                    o.paid -> PaidGreen
                    o.date.isBefore(today) -> OverdueRed
                    else -> primary
                }
                Box(Modifier.size(6.dp).clip(CircleShape).background(c))
            }
        }
    }
}
