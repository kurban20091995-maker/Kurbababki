package com.milibaby.app

import android.app.*
import android.content.*
import android.os.Build
import java.time.LocalTime

class ReminderReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val db=DatabaseHelper(context)
        if(inQuiet(db) && db.getSetting("allowNight","1")!="1") { ReminderScheduler.reschedule(context); return }
        createChannels(context)
        val isLog=intent.action==ReminderScheduler.ACTION_LOG
        val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java).putExtra("openAdd",true),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=Notification.Builder(context, if(isLog)"mili_log" else "mili_feed").setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(open).setAutoCancel(true).setContentTitle(if(isLog)"Не забудьте записать кормление 🍼" else "Время кормления 🍼").setContentText(if(isLog)"Сколько мл выпил малыш?" else "Ориентируйтесь на сигналы голода малыша.").build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(if(isLog)102 else 101,n)
        if(!isLog){ ReminderScheduler.scheduleLog(context); ReminderScheduler.reschedule(context) }
    }
    private fun inQuiet(db:DatabaseHelper):Boolean { if(db.getSetting("quietEnabled","0")!="1")return false; val now=LocalTime.now(); val s=runCatching{LocalTime.parse(db.getSetting("quietStart","23:00"))}.getOrDefault(LocalTime.of(23,0)); val e=runCatching{LocalTime.parse(db.getSetting("quietEnd","06:00"))}.getOrDefault(LocalTime.of(6,0)); return if(s<e) now>=s&&now<e else now>=s||now<e }
    private fun createChannels(c:Context){ if(Build.VERSION.SDK_INT>=26){ val nm=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; nm.createNotificationChannel(NotificationChannel("mili_feed","Напоминания о кормлении",NotificationManager.IMPORTANCE_DEFAULT)); nm.createNotificationChannel(NotificationChannel("mili_log","Запись объёма",NotificationManager.IMPORTANCE_DEFAULT)) } }
}
class BootReceiver:BroadcastReceiver(){ override fun onReceive(context:Context,intent:Intent){ ReminderScheduler.reschedule(context) } }
