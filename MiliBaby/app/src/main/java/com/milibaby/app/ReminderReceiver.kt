package com.milibaby.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val db = DatabaseHelper(context)
        if (inQuiet(db) && db.getSetting("allowNight", "1") != "1") {
            ReminderScheduler.reschedule(context)
            return
        }
        createChannels(context)
        val isLog = intent.action == ReminderScheduler.ACTION_LOG
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).putExtra("openAdd", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, if (isLog) "mili_log" else "mili_feed")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setContentTitle(if (isLog) "Не забудьте записать кормление 🍼" else "Время кормления 🍼")
            .setContentText(if (isLog) "Сколько мл выпил малыш?" else "Ориентируйтесь на сигналы голода малыша.")
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(if (isLog) 102 else 101, notification)
        if (!isLog) {
            ReminderScheduler.scheduleLog(context)
            ReminderScheduler.reschedule(context)
        }
    }

    private fun inQuiet(db: DatabaseHelper): Boolean {
        if (db.getSetting("quietEnabled", "0") != "1") return false
        val now = LocalTime.now()
        val start = runCatching { LocalTime.parse(db.getSetting("quietStart", "23:00")) }.getOrDefault(LocalTime.of(23, 0))
        val end = runCatching { LocalTime.parse(db.getSetting("quietEnd", "06:00")) }.getOrDefault(LocalTime.of(6, 0))
        return if (start < end) now >= start && now < end else now >= start || now < end
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel("mili_feed", "Напоминания о кормлении", NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel("mili_log", "Запись объёма", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.reschedule(context)
        MiliWidget.updateAll(context)
        QuickFeedNotification.refresh(context)
    }
}
