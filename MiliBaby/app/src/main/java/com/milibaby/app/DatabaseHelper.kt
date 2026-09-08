package com.milibaby.app

import android.content.*
import android.database.sqlite.*
import org.json.*
import java.time.*

class DatabaseHelper(context: Context): SQLiteOpenHelper(context, "milibaby.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE profile(id INTEGER PRIMARY KEY CHECK(id=1), name TEXT NOT NULL, birthDate TEXT NOT NULL, weightGrams INTEGER NOT NULL, feedingType TEXT NOT NULL)")
        db.execSQL("CREATE TABLE weights(id INTEGER PRIMARY KEY AUTOINCREMENT, weightGrams INTEGER NOT NULL, timeMillis INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE feedings(id INTEGER PRIMARY KEY AUTOINCREMENT, amountMl INTEGER NOT NULL, timeMillis INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        defaultSettings(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion:Int, newVersion:Int) {}
    private fun defaultSettings(db:SQLiteDatabase) {
        val defaults=mapOf("theme" to "pink", "displayMode" to "system", "feedReminder" to "0", "reminderMode" to "interval", "intervalMin" to "180", "fixedTimes" to "06:00,09:00,12:00,15:00,18:00,21:00,00:00,03:00", "logReminder" to "1", "logDelay" to "30", "quietEnabled" to "0", "quietStart" to "23:00", "quietEnd" to "06:00", "allowNight" to "1")
        defaults.forEach { (k,v)-> db.execSQL("INSERT OR IGNORE INTO settings(k,v) VALUES(?,?)", arrayOf(k,v)) }
    }
    fun hasProfile():Boolean = readableDatabase.rawQuery("SELECT 1 FROM profile LIMIT 1", null).use { it.moveToFirst() }
    fun getProfile():BabyProfile? = readableDatabase.rawQuery("SELECT name,birthDate,weightGrams,feedingType FROM profile WHERE id=1",null).use { c -> if(!c.moveToFirst()) null else BabyProfile(c.getString(0),c.getString(1),c.getInt(2),c.getString(3)) }
    fun saveProfile(p:BabyProfile) { writableDatabase.beginTransaction(); try { val cv=ContentValues().apply{put("id",1);put("name",p.name);put("birthDate",p.birthDate);put("weightGrams",p.weightGrams);put("feedingType",p.feedingType)}; writableDatabase.insertWithOnConflict("profile",null,cv,SQLiteDatabase.CONFLICT_REPLACE); val w=ContentValues().apply{put("weightGrams",p.weightGrams);put("timeMillis",System.currentTimeMillis())}; writableDatabase.insert("weights",null,w); writableDatabase.setTransactionSuccessful() } finally { writableDatabase.endTransaction() } }
    fun addFeeding(amount:Int,time:Long,note:String):Long { val cv=ContentValues().apply{put("amountMl",amount);put("timeMillis",time);put("note",note)}; return writableDatabase.insert("feedings",null,cv) }
    fun updateFeeding(id:Long, amount:Int,time:Long,note:String) { val cv=ContentValues().apply{put("amountMl",amount);put("timeMillis",time);put("note",note)}; writableDatabase.update("feedings",cv,"id=?",arrayOf(id.toString())) }
    fun deleteFeeding(id:Long){ writableDatabase.delete("feedings","id=?",arrayOf(id.toString())) }
    fun feedingsBetween(start:Long,end:Long):List<Feeding>{ val out=mutableListOf<Feeding>(); readableDatabase.rawQuery("SELECT id,amountMl,timeMillis,note FROM feedings WHERE timeMillis>=? AND timeMillis<? ORDER BY timeMillis DESC",arrayOf(start.toString(),end.toString())).use{c->while(c.moveToNext())out+=Feeding(c.getLong(0),c.getInt(1),c.getLong(2),c.getString(3))}; return out }
    fun allFeedings():List<Feeding>{ val out=mutableListOf<Feeding>(); readableDatabase.rawQuery("SELECT id,amountMl,timeMillis,note FROM feedings ORDER BY timeMillis DESC",null).use{c->while(c.moveToNext())out+=Feeding(c.getLong(0),c.getInt(1),c.getLong(2),c.getString(3))}; return out }
    fun lastFeeding():Feeding? = readableDatabase.rawQuery("SELECT id,amountMl,timeMillis,note FROM feedings ORDER BY timeMillis DESC LIMIT 1",null).use{c->if(c.moveToFirst())Feeding(c.getLong(0),c.getInt(1),c.getLong(2),c.getString(3)) else null}
    fun getSetting(k:String,d:String=""):String = readableDatabase.rawQuery("SELECT v FROM settings WHERE k=?",arrayOf(k)).use{c->if(c.moveToFirst())c.getString(0) else d}
    fun setSetting(k:String,v:String){ val cv=ContentValues().apply{put("k",k);put("v",v)}; writableDatabase.insertWithOnConflict("settings",null,cv,SQLiteDatabase.CONFLICT_REPLACE) }
    fun todayBounds(now:Long=System.currentTimeMillis()):Pair<Long,Long>{ val z=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()); val s=z.toLocalDate().atStartOfDay(z.zone).toInstant().toEpochMilli(); return s to z.toLocalDate().plusDays(1).atStartOfDay(z.zone).toInstant().toEpochMilli() }
    fun todayTotal():Int { val (s,e)=todayBounds(); return feedingsBetween(s,e).sumOf{it.amountMl} }
    fun exportJson():String { val root=JSONObject(); getProfile()?.let{root.put("profile",JSONObject().put("name",it.name).put("birthDate",it.birthDate).put("weightGrams",it.weightGrams).put("feedingType",it.feedingType))}; val fs=JSONArray(); allFeedings().forEach{fs.put(JSONObject().put("id",it.id).put("amountMl",it.amountMl).put("timeMillis",it.timeMillis).put("note",it.note))}; root.put("feedings",fs); val st=JSONObject(); readableDatabase.rawQuery("SELECT k,v FROM settings",null).use{c->while(c.moveToNext())st.put(c.getString(0),c.getString(1))}; root.put("settings",st); return root.toString(2) }
    fun importJson(text:String){ val r=JSONObject(text); writableDatabase.delete("feedings",null,null); r.optJSONObject("profile")?.let{saveProfile(BabyProfile(it.getString("name"),it.getString("birthDate"),it.getInt("weightGrams"),it.getString("feedingType")))}; val a=r.optJSONArray("feedings")?:JSONArray(); for(i in 0 until a.length()){val o=a.getJSONObject(i); addFeeding(o.getInt("amountMl"),o.getLong("timeMillis"),o.optString("note"))}; r.optJSONObject("settings")?.let{o->o.keys().forEach{setSetting(it,o.getString(it))}} }
}
