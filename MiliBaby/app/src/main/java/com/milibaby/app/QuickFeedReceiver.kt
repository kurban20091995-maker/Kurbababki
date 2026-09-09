package com.milibaby.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast

class QuickFeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ADD) return
        val amount = intent.getIntExtra(EXTRA_AMOUNT, 0)
        if (amount !in 1..500) return

        val db = DatabaseHelper(context)
        db.addFeeding(amount, System.currentTimeMillis(), "Быстрое добавление")
        ReminderScheduler.cancelLog(context)
        ReminderScheduler.reschedule(context)
        MiliWidget.updateAll(context, "✓ $amount мл добавлено")
        QuickFeedNotification.refresh(context)
        Toast.makeText(context, "$amount мл добавлено 🤍", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_ADD = "com.milibaby.app.ACTION_QUICK_ADD"
        const val EXTRA_AMOUNT = "amountMl"

        fun pendingAdd(context: Context, amount: Int, requestCode: Int): PendingIntent {
            val intent = Intent(context, QuickFeedReceiver::class.java).apply {
                action = ACTION_ADD
                putExtra(EXTRA_AMOUNT, amount)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

object QuickFeedNotification {
    private const val CHANNEL_ID = "mili_quick"
    private const val NOTIFICATION_ID = 301

    fun show(context: Context) {
        val db = DatabaseHelper(context)
        if (db.getSetting("persistentQuick", "0") != "1") return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Быстрое добавление Mili Baby", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Необязательная панель быстрого добавления кормления"
                    setShowBadge(false)
                }
            )
        }

        val open = PendingIntent.getActivity(
            context,
            310,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val quick = db.getSetting("quickAmounts", "30,60,90,120")
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..500 }
            .distinct()
            .take(3)
            .ifEmpty { listOf(60, 90, 120) }

        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL_ID) else Notification.Builder(context)
        builder
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("Mili Baby")
            .setContentText("Сегодня: ${db.todayTotal()} мл • добавить кормление")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        quick.forEachIndexed { index, amount ->
            builder.addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_input_add,
                    "$amount мл",
                    QuickFeedReceiver.pendingAdd(context, amount, 330 + index)
                ).build()
            )
        }

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun hide(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }

    fun refresh(context: Context) {
        if (DatabaseHelper(context).getSetting("persistentQuick", "0") == "1") show(context)
    }
}
