package gr.logariasmoi.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import gr.logariasmoi.MainActivity
import gr.logariasmoi.R
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.overdue
import gr.logariasmoi.data.tr
import gr.logariasmoi.data.upcoming
import java.time.LocalDate

class BillsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Repository.init(context)
        ids.forEach { manager.updateAppWidget(it, build(context)) }
    }

    companion object {
        private val rows = listOf(
            Triple(R.id.name0, R.id.date0, R.id.amount0) to R.id.row0,
            Triple(R.id.name1, R.id.date1, R.id.amount1) to R.id.row1,
            Triple(R.id.name2, R.id.date2, R.id.amount2) to R.id.row2,
            Triple(R.id.name3, R.id.date3, R.id.amount3) to R.id.row3,
            Triple(R.id.name4, R.id.date4, R.id.amount4) to R.id.row4,
        )

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BillsWidget::class.java))
            if (ids.isNotEmpty()) ids.forEach { manager.updateAppWidget(it, build(context)) }
        }

        private fun build(context: Context): RemoteViews {
            val today = LocalDate.now()
            val data = Repository.current
            val items = (data.overdue(today) + data.upcoming(today)).take(rows.size)
            val views = RemoteViews(context.packageName, R.layout.widget_bills)
            views.setTextViewText(R.id.widget_title, tr("Επόμενοι λογαριασμοί", "Upcoming bills"))
            views.setTextViewText(R.id.widget_empty, tr("Δεν υπάρχουν λογαριασμοί", "No bills yet"))
            views.setViewVisibility(R.id.widget_empty, if (items.isEmpty()) View.VISIBLE else View.GONE)
            rows.forEachIndexed { i, (ids, row) ->
                val occ = items.getOrNull(i)
                if (occ == null) {
                    views.setViewVisibility(row, View.GONE)
                } else {
                    views.setViewVisibility(row, View.VISIBLE)
                    val late = occ.date.isBefore(today)
                    views.setTextViewText(ids.first, occ.bill.name)
                    views.setTextViewText(ids.second, if (late) tr("Ληξιπρόθεσμος", "Overdue") else Fmt.relative(occ.date, today))
                    views.setTextColor(ids.second, context.getColor(if (late) R.color.widget_overdue else R.color.widget_sub))
                    views.setTextViewText(ids.third, if (occ.amount > 0) Fmt.money(occ.amount) else "")
                }
            }
            val open = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }
    }
}
