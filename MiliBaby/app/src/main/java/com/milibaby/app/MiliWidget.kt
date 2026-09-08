package com.milibaby.app

import android.app.*
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
import java.time.*
import java.time.format.DateTimeFormatter

class MiliWidget:AppWidgetProvider(){
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){ ids.forEach{update(context,manager,it)} }
 companion object { fun updateAll(c:Context){ val m=AppWidgetManager.getInstance(c); val cn=ComponentName(c,MiliWidget::class.java); m.getAppWidgetIds(cn).forEach{update(c,m,it)} }
  private fun update(c:Context,m:AppWidgetManager,id:Int){ val db=DatabaseHelper(c); val rv=RemoteViews(c.packageName,R.layout.widget_mili); rv.setTextViewText(R.id.widgetToday,"Сегодня: ${db.todayTotal()} мл"); val l=db.lastFeeding(); rv.setTextViewText(R.id.widgetLast, if(l==null)"Последнее: —" else "Последнее: ${l.amountMl} мл • ${Instant.ofEpochMilli(l.timeMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}"); val pi=PendingIntent.getActivity(c,2,Intent(c,MainActivity::class.java).putExtra("openAdd",true),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); rv.setOnClickPendingIntent(R.id.widgetAdd,pi); m.updateAppWidget(id,rv)} }
}
