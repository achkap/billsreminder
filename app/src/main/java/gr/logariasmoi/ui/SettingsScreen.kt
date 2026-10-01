package gr.logariasmoi.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import gr.logariasmoi.AppIcons
import gr.logariasmoi.BuildConfig
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.tr
import gr.logariasmoi.notify.Notifications
import gr.logariasmoi.notify.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val data by Repository.data.collectAsStateWithLifecycle()
    val s = data.settings
    val scope = rememberCoroutineScope()
    fun toast(msg: String) = scope.launch { snackbar.showSnackbar(msg) }

    // Re-check permissions whenever the screen resumes (user may come back from system settings).
    var tick by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { tick++ } }
    val canNotify = remember(tick) { Notifications.canPost(context) }
    val canExact = remember(tick) { ReminderScheduler.canScheduleExact(context) }
    var icon by remember { mutableStateOf(AppIcons.current(context)) }

    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { Repository.exportTo(uri) }
            .onSuccess { toast(tr("Η εξαγωγή ολοκληρώθηκε", "Export complete")) }
            .onFailure { toast(tr("Αποτυχία εξαγωγής: ", "Export failed: ") + it.message) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            Repository.updateSettings { it.copy(backupFolder = uri.toString()) }
            toast(
                if (Repository.autoBackup()) tr("Το αυτόματο backup ενεργοποιήθηκε", "Automatic backup enabled")
                else tr("Δεν ήταν δυνατή η εγγραφή στον φάκελο", "Couldn’t write to that folder"),
            )
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(tr("Ρυθμίσεις", "Settings")) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // ---- Appearance -------------------------------------------------------------------
            Header(tr("Εμφάνιση", "Appearance"))
            Choice(
                listOf("system" to tr("Σύστημα", "System"), "light" to tr("Φωτεινό", "Light"), "dark" to tr("Σκοτεινό", "Dark")),
                s.themeMode,
            ) { v -> Repository.updateSettings { it.copy(themeMode = v) } }
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Contrast, null) },
                headlineContent = { Text("Pitch black") },
                supportingContent = { Text(tr("Κατάμαυρο φόντο στο σκοτεινό θέμα (ιδανικό για οθόνες AMOLED)", "Pure black background in dark theme (great for AMOLED screens)")) },
                trailingContent = { Switch(s.pureBlack, { v -> Repository.updateSettings { it.copy(pureBlack = v) } }) },
            )

            Header(tr("Γλώσσα", "Language"))
            Choice(
                listOf("system" to tr("Σύστημα", "System"), "el" to "Ελληνικά", "en" to "English"),
                s.language,
            ) { v ->
                if (v != s.language) {
                    Repository.updateSettings { it.copy(language = v) }
                    (context as? Activity)?.recreate()
                }
            }

            Header(tr("Εικονίδιο εφαρμογής", "App icon"))
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                AppIcons.options.forEach { o ->
                    IconChoice(o, selected = o == icon) {
                        if (o != icon) {
                            AppIcons.select(context, o)
                            icon = o
                            toast(tr("Το εικονίδιο θα αλλάξει σε λίγα δευτερόλεπτα", "The icon will change in a few seconds"))
                        }
                    }
                }
            }
            Text(
                tr(
                    "Σε κάποια κινητά η συντόμευση στην αρχική οθόνη αφαιρείται όταν αλλάζει το εικονίδιο· πρόσθεσέ τη ξανά από τη λίστα εφαρμογών.",
                    "On some phones the home-screen shortcut is removed when the icon changes; add it again from the app drawer.",
                ),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            // ---- Notifications ----------------------------------------------------------------
            Header(tr("Ειδοποιήσεις", "Notifications"))
            if (!canNotify) {
                Item(Icons.Outlined.Warning, tr("Οι ειδοποιήσεις είναι απενεργοποιημένες", "Notifications are turned off"), tr("Πάτα για να τις επιτρέψεις", "Tap to allow them")) {
                    if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
            if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Item(
                    Icons.Outlined.Alarm, tr("Ακριβής ώρα υπενθυμίσεων", "Exact reminder times"),
                    tr("Επίτρεψε «Ξυπνητήρια & υπενθυμίσεις» για να έρχονται στην ώρα τους", "Allow “Alarms & reminders” so they arrive on time"),
                ) {
                    context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            }
            Item(Icons.Outlined.Schedule, tr("Προεπιλεγμένη ώρα υπενθύμισης", "Default reminder time"), Fmt.time(s.defaultReminderHour, s.defaultReminderMinute)) {
                showTimePicker(context, s.defaultReminderHour, s.defaultReminderMinute) { h, m ->
                    Repository.updateSettings { it.copy(defaultReminderHour = h, defaultReminderMinute = m) }
                }
            }
            Text(
                tr("Προεπιλεγμένες υπενθυμίσεις για νέους λογαριασμούς", "Default reminders for new bills"),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp),
            )
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 2, 3, 5, 7, 14).forEach { d ->
                    FilterChip(
                        selected = d in s.defaultReminderDays,
                        onClick = {
                            Repository.updateSettings {
                                it.copy(defaultReminderDays = if (d in it.defaultReminderDays) it.defaultReminderDays - d else it.defaultReminderDays + d)
                            }
                        },
                        label = { Text(Fmt.reminderDay(d)) },
                    )
                }
            }
            ListItem(
                leadingContent = { Icon(Icons.Outlined.NotificationsActive, null) },
                headlineContent = { Text(tr("Καθημερινή υπενθύμιση για ληξιπρόθεσμους", "Daily reminder for overdue bills")) },
                supportingContent = { Text(tr("Μέχρι να τους σημειώσεις ως πληρωμένους (έως 30 μέρες)", "Until you mark them as paid (up to 30 days)")) },
                trailingContent = { Switch(s.overdueDaily, { v -> Repository.updateSettings { it.copy(overdueDaily = v) } }) },
            )
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Snooze, null) },
                headlineContent = { Text(tr("«Αργότερα» σημαίνει σε", "“Later” means in")) },
                supportingContent = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60 to tr("1 ώρα", "1 hour"), 180 to tr("3 ώρες", "3 hours"), 24 * 60 to tr("Αύριο", "Tomorrow")).forEach { (m, label) ->
                            FilterChip(s.snoozeMinutes == m, { Repository.updateSettings { it.copy(snoozeMinutes = m) } }, { Text(label) })
                        }
                    }
                },
            )
            Item(Icons.Outlined.NotificationsActive, tr("Δοκιμαστική ειδοποίηση", "Test notification"), tr("Για να δεις πώς θα εμφανίζονται", "See what reminders look like")) {
                val bill = data.bills.firstOrNull { !it.archived }
                if (bill == null) toast(tr("Πρόσθεσε πρώτα έναν λογαριασμό", "Add a bill first"))
                else if (!canNotify) toast(tr("Επίτρεψε πρώτα τις ειδοποιήσεις", "Allow notifications first"))
                else Notifications.show(context, bill, LocalDate.now().plusDays(1))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            // ---- Data -------------------------------------------------------------------------
            Header(tr("Δεδομένα & backup", "Data & backup"))
            Item(Icons.Outlined.FileUpload, tr("Εξαγωγή δεδομένων", "Export data"), tr("Αποθήκευση όλων των λογαριασμών και του ιστορικού σε αρχείο", "Save all bills and history to a file")) {
                exportLauncher.launch("logariasmoi-${LocalDate.now()}.json")
            }
            Item(Icons.Outlined.FileDownload, tr("Εισαγωγή δεδομένων", "Import data"), tr("Επαναφορά από αρχείο εξαγωγής ή backup", "Restore from an export or backup file")) {
                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
            }
            val folder = s.backupFolder
            Item(
                Icons.Outlined.Backup, tr("Αυτόματο backup", "Automatic backup"),
                if (folder == null) tr("Ανενεργό · διάλεξε φάκελο (π.χ. Google Drive ή Downloads)", "Off · pick a folder (e.g. Google Drive or Downloads)")
                else tr("Σε κάθε αλλαγή, στο αρχείο ", "On every change, to the file ") + Repository.BACKUP_FILE_NAME +
                    (if (s.lastBackupAt > 0) "\n" + tr("Τελευταίο: ", "Last: ") + Fmt.dateTime(s.lastBackupAt) else ""),
            ) { folderLauncher.launch(null) }
            if (folder != null) {
                Item(Icons.Outlined.Backup, tr("Backup τώρα", "Back up now"), null) {
                    toast(if (Repository.autoBackup()) tr("Το backup αποθηκεύτηκε", "Backup saved") else tr("Αποτυχία backup. Διάλεξε ξανά φάκελο.", "Backup failed. Pick the folder again."))
                }
                Item(Icons.Outlined.FolderOff, tr("Απενεργοποίηση αυτόματου backup", "Turn off automatic backup"), null) {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(folder), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                    Repository.updateSettings { it.copy(backupFolder = null, lastBackupAt = 0) }
                }
            }
            Text(
                tr(
                    "Όλα τα δεδομένα μένουν μόνο στη συσκευή σου. Πριν απεγκαταστήσεις την εφαρμογή, κάνε εξαγωγή ή ενεργοποίησε το αυτόματο backup, και μετά την επανεγκατάσταση χρησιμοποίησε την «Εισαγωγή».",
                    "All data stays on your device only. Before uninstalling, export your data or turn on automatic backup, then use “Import” after reinstalling.",
                ),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            Text(
                tr("Λογαριασμοί", "Bills") + " v" + BuildConfig.VERSION_NAME + " · " + tr("τα ονόματα εταιρειών ανήκουν στους κατόχους τους· η εφαρμογή δεν συνδέεται με καμία από αυτές.", "company names belong to their owners; this app is not affiliated with any of them."),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            )
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            tr("Εισαγωγή δεδομένων;", "Import data?"),
            tr("Οι τωρινοί λογαριασμοί και το ιστορικό θα αντικατασταθούν από αυτά του αρχείου.", "Your current bills and history will be replaced by the ones in the file."),
            tr("Εισαγωγή", "Import"), { pendingImport = null },
        ) {
            val before = Repository.current.settings.language
            runCatching { Repository.importFrom(uri) }
                .onSuccess {
                    if (Repository.current.settings.language != before) (context as? Activity)?.recreate()
                    else toast(tr("Εισήχθησαν $it λογαριασμοί", "Imported $it bills"))
                }
                .onFailure { toast(tr("Το αρχείο δεν είναι έγκυρο backup", "That file isn’t a valid backup")) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        options.forEachIndexed { i, (key, label) ->
            SegmentedButton(
                selected = value == key,
                onClick = { onChange(key) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun IconChoice(option: AppIcons.Option, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(60.dp)
                .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                .padding(4.dp).clip(CircleShape).background(colorResource(option.background)),
            contentAlignment = Alignment.Center,
        ) {
            // The foreground is drawn on a 108dp canvas of which ~72dp is visible, so scale it up.
            androidx.compose.foundation.Image(
                painterResource(option.foreground), contentDescription = option.label,
                modifier = Modifier.requiredSize(78.dp),
            )
        }
        Text(
            option.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun Header(text: String) = Text(
    text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
)

@Composable
private fun Item(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
