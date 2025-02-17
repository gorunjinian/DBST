package com.gorunjinian.dbst

import android.app.Application
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors

class MyApplication : Application() {

    var isAppInBackground = true
    private var lastPausedTime: Long = 0

    override fun onCreate() {
        super.onCreate()

        // Apply dynamic theming if enabled
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("dynamic_theming", false)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    fun markPausedTime() {
        lastPausedTime = System.currentTimeMillis()
    }

    fun isReopenFromBackground(): Boolean {
        return isAppInBackground
    }

    fun shouldAuthenticate(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isFingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)
        val lastAuthenticatedTime = prefs.getLong("last_authenticated_time", 0)
        val currentTime = System.currentTimeMillis()
        val fingerprintDelay = prefs.getInt("fingerprint_delay_value", 0) * 60 * 1000 // Convert minutes to ms

        return isFingerprintEnabled && (currentTime - lastAuthenticatedTime > fingerprintDelay)
    }
}
