package com.makscoach

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlockerService : AccessibilityService() {

    private val blockedApps = setOf(
        "com.android.chrome",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.google.android.youtube"
    )

    private fun isUnlockedToday(): Boolean {
        val prefs = getSharedPreferences("maks_coach_prefs", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return prefs.getString("unlocked_date", "") == today
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (blockedApps.contains(pkg) && !isUnlockedToday()) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(
                applicationContext,
                "⛔ MAK'S COACH: Complete your Pre-Workout Fuel & Exercise Sets first!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onInterrupt() {}
}
