package com.milibaby.app

import android.app.*
import android.content.*
import android.os.Build
import java.time.*

object ReminderScheduler {
    const val ACTION_FEED="com.milibaby.app.FEED_REMINDER"
    const val ACTION_LOG="com.milibaby.app.LOG_REMINDER"
    private const val REQ_FEED=401
    private const val REQ_LOG=402
    fun reschedule(context:Context){
        val db=DatabaseHelper(context)
        cancel(context,REQ_FEED,ACTION_FEED)
        if(db.getSetting("feedReminder","0")!="1") return
        val trigger = if(db.getSetting("reminderMode","interval")=="fixed") nextFixed(db.getSetting("fixedTimes")) else {
            val base=db.lastFeeding()?.timeMillis ?: System.currentTimeMillis()
            base + ((db.getSetting("intervalMin","180").toLongOrNull()?:180)*60_000L)
        }
        schedule(context,trigger.coerceAtLeast(System.currentTimeMillis()+5_000),REQ_FEED,ACTION_FEED)
    }
    fun scheduleLog(context:Context){ val db=DatabaseHelper(context); if(db.getSetting("logReminder","1")!="1")return; val delay=(db.getSetting("logDelay","30").toLongOrNull()?:30)*60_000L; schedule(context,System.currentTimeMillis()+delay,REQ_LOG,ACTION_LOG) }
    fun cancelLog(context:Context)=cancel(context,REQ_LOG,ACTION_LOG)
    private fun schedule(c:Context,time:Long,req:Int,action:String){ val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val pi=PendingIntent.getBroadcast(c,req,Intent(c,ReminderReceiver::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pi) else am.set(AlarmManager.RTC_WAKEUP,time,pi) }
    private fun cancel(c:Context,req:Int,action:String){ val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val pi=PendingIntent.getBroadcast(c,req,Intent(c,ReminderReceiver::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); am.cancel(pi) }
    private fun nextFixed(times:String):Long { val z=ZonedDateTime.now(); val parsed=times.split(',').mapNotNull{runCatching{LocalTime.parse(it.trim())}.getOrNull()}.sorted(); parsed.forEach{val d=z.toLocalDate().atTime(it).atZone(z.zone); if(d.isAfter(z))return d.toInstant().toEpochMilli()}; val first=parsed.firstOrNull()?:LocalTime.of(9,0); return z.toLocalDate().plusDays(1).atTime(first).atZone(z.zone).toInstant().toEpochMilli() }
}
