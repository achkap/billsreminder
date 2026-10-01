package gr.logariasmoi.ui

import gr.logariasmoi.data.tr
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import gr.logariasmoi.data.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Άκυρο", "Cancel")) } },
    ) { DatePicker(state = state) }
}

fun showTimePicker(context: Context, hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPick(h, m) }, hour, minute, true).show()
}

/** Asks for the actual amount and payment date before marking a bill as paid. */
@Composable
fun PayDialog(
    title: String,
    dueDate: LocalDate,
    amount: Double,
    initialPaidDate: LocalDate = LocalDate.now(),
    confirmText: String = tr("Πληρώθηκε", "Mark paid"),
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, paidDate: LocalDate) -> Unit,
) {
    var text by remember { mutableStateOf(Fmt.amountInput(amount)) }
    var paidDate by remember { mutableStateOf(initialPaidDate) }
    var picking by remember { mutableStateOf(false) }
    val parsed = if (text.isBlank()) 0.0 else Fmt.parseAmount(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Λήξη: ${Fmt.full(dueDate)}", "Due: ${Fmt.full(dueDate)}"))
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text(tr("Ποσό που πληρώθηκε (€)", "Amount paid (€)")) },
                    isError = parsed == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = Fmt.num(paidDate), onValueChange = {}, readOnly = true,
                    label = { Text(tr("Ημερομηνία πληρωμής", "Payment date")) },
                    trailingIcon = { IconButton(onClick = { picking = true }) { Icon(Icons.Outlined.CalendarMonth, tr("Επιλογή", "Pick")) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { onConfirm(parsed ?: 0.0, paidDate) }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Άκυρο", "Cancel")) } },
    )
    if (picking) DatePickerModal(paidDate, { picking = false }) { paidDate = it }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Άκυρο", "Cancel")) } },
    )
}
