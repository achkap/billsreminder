package gr.logariasmoi

import android.Manifest
import android.content.Intent
import android.os.Build
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.tr
import gr.logariasmoi.notify.Notifications
import gr.logariasmoi.notify.ReminderScheduler
import gr.logariasmoi.ui.AppTheme
import gr.logariasmoi.ui.BillDetailScreen
import gr.logariasmoi.ui.CalendarScreen
import gr.logariasmoi.ui.EditBillScreen
import gr.logariasmoi.ui.HomeScreen
import gr.logariasmoi.ui.isAppInDarkTheme
import gr.logariasmoi.ui.ProviderPickerScreen
import gr.logariasmoi.ui.SettingsScreen
import gr.logariasmoi.ui.SummaryScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs get() = listOf(
    Tab("home", tr("Αρχική", "Home"), Icons.Outlined.Home),
    Tab("calendar", tr("Ημερολόγιο", "Calendar"), Icons.Outlined.CalendarMonth),
    Tab("summary", tr("Σύνοψη", "Summary"), Icons.Outlined.BarChart),
    Tab("settings", tr("Ρυθμίσεις", "Settings"), Icons.Outlined.Settings),
)

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_BILL_ID = "bill_id"
    }

    private val openBill = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openBill.value = intent?.getStringExtra(EXTRA_BILL_ID)

        setContent {
            val data by Repository.data.collectAsStateWithLifecycle()
            val settings = data.settings
            val dark = isAppInDarkTheme(settings.themeMode)
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            AppTheme(settings.themeMode, settings.pureBlack) {
                val nav = rememberNavController()
                val snackbar = remember { SnackbarHostState() }
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route
                val onTab = tabs.any { it.route == route }

                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(this@MainActivity)) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                val pending by openBill
                LaunchedEffect(pending) {
                    pending?.let { nav.navigate("bill/$it"); openBill.value = null }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    floatingActionButton = {
                        if (route == "home") {
                            ExtendedFloatingActionButton(
                                onClick = { nav.navigate("new") },
                                icon = { Icon(Icons.Default.Add, null) },
                                text = { Text(tr("Νέος", "New")) },
                            )
                        }
                    },
                    bottomBar = {
                        if (onTab) NavigationBar {
                            tabs.forEach { t ->
                                NavigationBarItem(
                                    selected = route == t.route,
                                    onClick = {
                                        nav.navigate(t.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(t.icon, null) },
                                    label = { Text(t.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
                        composable("home") {
                            HomeScreen(snackbar, onOpenBill = { nav.navigate("bill/$it") }, onAdd = { nav.navigate("new") })
                        }
                        composable("calendar") { CalendarScreen(onOpenBill = { nav.navigate("bill/$it") }) }
                        composable("summary") { SummaryScreen(onOpenBill = { nav.navigate("bill/$it") }) }
                        composable("settings") { SettingsScreen(snackbar) }
                        composable("new") {
                            ProviderPickerScreen(onBack = { nav.popBackStack() }) { p ->
                                nav.navigate("edit?provider=${p?.id ?: ""}") { popUpTo("new") { inclusive = true } }
                            }
                        }
                        composable(
                            "edit?id={id}&provider={provider}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType; nullable = true },
                                navArgument("provider") { type = NavType.StringType; nullable = true },
                            ),
                        ) { entry ->
                            val id = entry.arguments?.getString("id")
                            EditBillScreen(
                                billId = id,
                                providerId = entry.arguments?.getString("provider")?.ifBlank { null },
                                onBack = { nav.popBackStack() },
                                onSaved = { savedId ->
                                    if (id != null) nav.popBackStack()
                                    else nav.navigate("bill/$savedId") { popUpTo("home") }
                                },
                            )
                        }
                        composable("bill/{id}") { entry ->
                            val id = entry.arguments?.getString("id")!!
                            BillDetailScreen(
                                id, snackbar,
                                onBack = { nav.popBackStack() },
                                onEdit = { nav.navigate("edit?id=$id") },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_BILL_ID)?.let { openBill.value = it }
    }

    override fun onResume() {
        super.onResume()
        // Catch up on reminders that might have been missed (e.g. alarm delayed by the system).
        ReminderScheduler.schedule(this)
    }
}
