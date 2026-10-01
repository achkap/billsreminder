package gr.logariasmoi

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import gr.logariasmoi.data.tr

/** Launcher icon choices, backed by the <activity-alias> entries in the manifest. */
object AppIcons {
    data class Option(
        val alias: String,
        private val el: String,
        private val en: String,
        @param:DrawableRes val foreground: Int,
        @param:ColorRes val background: Int,
        val enabledByDefault: Boolean = false,
    ) {
        val label: String get() = tr(el, en)
    }

    val options = listOf(
        Option(".IconClassic", "Απόδειξη", "Receipt", R.drawable.ic_launcher_fg_classic, R.color.icon_bg_classic, enabledByDefault = true),
        Option(".IconCalendar", "Ημερολόγιο", "Calendar", R.drawable.ic_launcher_fg_calendar, R.color.icon_bg_calendar),
        Option(".IconGold", "Χρυσό νόμισμα", "Gold coin", R.drawable.ic_launcher_fg_gold, R.color.icon_bg_gold),
        Option(".IconMinimal", "Επανάληψη", "Recurring", R.drawable.ic_launcher_fg_minimal, R.color.icon_bg_minimal),
    )

    private fun component(context: Context, o: Option) = ComponentName(context.packageName, "gr.logariasmoi" + o.alias)

    fun current(context: Context): Option {
        val pm = context.packageManager
        return options.firstOrNull { o ->
            when (pm.getComponentEnabledSetting(component(context, o))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> o.enabledByDefault
                else -> false
            }
        } ?: options.first()
    }

    fun select(context: Context, choice: Option) {
        val pm = context.packageManager
        // Enable the new entry first so the app never ends up without a launcher icon.
        pm.setComponentEnabledSetting(component(context, choice), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        options.filter { it != choice }.forEach {
            pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
