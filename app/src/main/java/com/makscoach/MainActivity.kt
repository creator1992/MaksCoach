package com.makscoach

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(AndroidBridge(this@MainActivity), "AndroidBridge")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    inner class AndroidBridge(private val context: Context) {
        private val prefs = context.getSharedPreferences("maks_coach_prefs", Context.MODE_PRIVATE)

        private fun today(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        @JavascriptInterface
        fun unlockForToday(reason: String) {
            prefs.edit().putString("unlocked_date", today()).apply()
            vibrate(300)
            Toast.makeText(context, "🔓 Mak's Coach: Unlocked Today ($reason)", Toast.LENGTH_LONG).show()
        }

        @JavascriptInterface
        fun lockAgain() {
            prefs.edit().remove("unlocked_date").apply()
            vibrate(150)
            Toast.makeText(context, "🔒 Mak's Coach: Browsers & YouTube Re-Locked", Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun isUnlockedToday(): Boolean {
            return prefs.getString("unlocked_date", "") == today()
        }

        @JavascriptInterface
        fun speak(text: String) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "workout_tts")
        }

        @JavascriptInterface
        fun vibrate(ms: Long) {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        @JavascriptInterface
        fun openAccessibilitySettings() {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        @JavascriptInterface
        fun askGemini(prompt: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Gemini Exercise Context", prompt))
            Toast.makeText(context, "Prompt copied! Paste or speak in Gemini Live.", Toast.LENGTH_LONG).show()

            val geminiIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
            if (geminiIntent != null) {
                context.startActivity(geminiIntent)
            } else {
                val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(voiceIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Install the Google Gemini app from Play Store.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun schedulePostWorkoutMealReminder(minutesDelay: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("title", "🔋 Mak's Coach: Post-Workout Meal!")
                putExtra("message", "30 mins since workout! Punch your Post-Workout Protein & Carbs now.")
            }
            val pending = PendingIntent.getBroadcast(
                context, 101, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = System.currentTimeMillis() + (minutesDelay * 60 * 1000L)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
