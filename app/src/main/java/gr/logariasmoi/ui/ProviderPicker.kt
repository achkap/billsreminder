package gr.logariasmoi.ui

import gr.logariasmoi.data.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gr.logariasmoi.data.Category
import gr.logariasmoi.data.Provider
import gr.logariasmoi.data.Providers
import java.text.Normalizer

private fun String.normalized(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace('ς', 'σ')

/** Full-screen list of known companies. [onPick] receives null for a custom bill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPickerScreen(onBack: () -> Unit, onPick: (Provider?) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.normalized().trim()
    val filtered = remember(q) {
        if (q.isEmpty()) Providers.all
        else Providers.all.filter { it.searchText.normalized().contains(q) || it.category.label.normalized().contains(q) || it.id.contains(q) }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(tr("Επίλεξε εταιρεία", "Choose a company")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Πίσω", "Back")) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text(tr("Αναζήτηση (π.χ. ΔΕΗ, Cosmote, Netflix)", "Search (e.g. DEI, Cosmote, Netflix)")) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CustomTile { onPick(null) }
                }
                Category.entries.forEach { cat ->
                    val list = filtered.filter { it.category == cat }
                    if (list.isEmpty()) return@forEach
                    item(span = { GridItemSpan(maxLineSpan) }, key = cat.name) {
                        Text(
                            cat.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(list, key = { it.id }) { p ->
                        Column(
                            Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPick(p) }.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ProviderIcon(p.id, p.name, p.category, 52.dp)
                            Text(
                                p.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTile(onClick: () -> Unit) {
    androidx.compose.material3.OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProviderIcon(null, "", Category.OTHER, 44.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(tr("Δικός μου λογαριασμός", "Custom bill"), style = MaterialTheme.typography.titleMedium)
                Text(
                    tr("Για οτιδήποτε δεν υπάρχει στη λίστα", "For anything that isn’t on the list"), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
