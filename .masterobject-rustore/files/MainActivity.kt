package ru.furniturecrm.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val licenseManager: RuStoreLicenseManager by lazy { RuStoreLicenseManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.schedule(this)
        if (savedInstanceState == null) licenseManager.proceedIntent(intent)
        val openProjectId = intent?.getLongExtra("projectId", -1L)?.takeIf { it > 0 }
        setContent { FurnitureCrmApp(vm, licenseManager, openProjectId) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        licenseManager.proceedIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        licenseManager.onResume()
    }
}
