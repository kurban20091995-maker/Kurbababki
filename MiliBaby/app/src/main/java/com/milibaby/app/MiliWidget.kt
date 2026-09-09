package com.milibaby.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MiliWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it, null) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        update(context, manager, appWidgetId, null)
    }

    companion object {
        fun updateAll(context: Context, status: String? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MiliWidget::class.java)
            manager.getAppWidgetIds(component).forEach { update(context, manager, it, status) }
        }

        private fun quickAmounts(db: DatabaseHelper): List<Int> {
            val parsed = db.getSetting("quickAmounts", "30,60,90,120")
                .split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..500 }
                .distinct()
            return if (parsed.size >= 4) parsed.take(4) else listOf(30, 60, 90, 120)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int, status: String?) {
            val db = DatabaseHelper(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_mili)
            val options = manager.getAppWidgetOptions(id)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val compact = minWidth < 220

            rv.setTextViewText(R.id.widgetToday, "${db.todayTotal()} мл сегодня")
            val last = db.lastFeeding()
            rv.setTextViewText(
                R.id.widgetLast,
                if (last == null) "Последнее: —" else "Последнее: ${last.amountMl} мл • ${Instant.ofEpochMilli(last.timeMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}"
            )
            rv.setTextViewText(R.id.widgetStatus, status ?: "")
            rv.setViewVisibility(R.id.widgetStatus, if (status.isNullOrBlank()) View.GONE else View.VISIBLE)

            val openAdd = PendingIntent.getActivity(
                context,
                402,
                Intent(context, MainActivity::class.java).putExtra("openAdd", true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.widgetAdd, openAdd)
            rv.setOnClickPendingIntent(R.id.widgetOther, openAdd)

            rv.setViewVisibility(R.id.widgetQuickArea, if (compact) View.GONE else View.VISIBLE)
            rv.setViewVisibility(R.id.widgetAdd, if (compact) View.VISIBLE else View.GONE)

            val ids = intArrayOf(R.id.widgetQ1, R.id.widgetQ2, R.id.widgetQ3, R.id.widgetQ4)
            quickAmounts(db).forEachIndexed { index, amount ->
                rv.setTextViewText(ids[index], "+$amount")
                rv.setOnClickPendingIntent(ids[index], QuickFeedReceiver.pendingAdd(context, amount, 410 + index))
            }

            manager.updateAppWidget(id, rv)

            if (!status.isNullOrBlank()) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    runCatching { update(context, manager, id, null) }
                }, 1600)
            }
        }
    }
}
