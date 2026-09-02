package ru.furniturecrm.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CrmDatabase(context: Context) : SQLiteOpenHelper(context, "furniture_crm.db", null, 2) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE projects(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              title TEXT NOT NULL,
              client_name TEXT NOT NULL,
              phone TEXT NOT NULL DEFAULT '',
              address TEXT NOT NULL DEFAULT '',
              furniture_type TEXT NOT NULL,
              custom_type TEXT NOT NULL DEFAULT '',
              base_price_cents INTEGER NOT NULL DEFAULT 0,
              planned_start TEXT,
              start_time TEXT NOT NULL DEFAULT '09:00',
              duration_half_days INTEGER NOT NULL DEFAULT 2,
              status TEXT NOT NULL,
              notes TEXT NOT NULL DEFAULT '',
              queue_position INTEGER NOT NULL DEFAULT 0,
              actual_start TEXT,
              actual_finish TEXT,
              created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_projects_status_queue ON projects(status, queue_position)")
        db.execSQL("CREATE INDEX idx_projects_phone ON projects(phone)")
        db.execSQL("""CREATE TABLE extras(id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, name TEXT NOT NULL, price_cents INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE payments(id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, amount_cents INTEGER NOT NULL, date TEXT NOT NULL, method TEXT NOT NULL, comment TEXT NOT NULL DEFAULT '', FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, amount_cents INTEGER NOT NULL, date TEXT NOT NULL, category TEXT NOT NULL, comment TEXT NOT NULL DEFAULT '', FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE checklist(id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE attachments(id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, uri TEXT NOT NULL, name TEXT NOT NULL, mime_type TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL DEFAULT '', FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)""")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("INSERT INTO settings(key,value) VALUES('work_days','1,2,3,4,5,6')")
        db.execSQL("INSERT INTO settings(key,value) VALUES('reminder_days','7,3,1,0')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE attachments ADD COLUMN mime_type TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE attachments ADD COLUMN created_at TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE attachments SET created_at = CURRENT_TIMESTAMP WHERE created_at = ''")
        }
    }
}
