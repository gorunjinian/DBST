package com.gorunjinian.dbst

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors

/**
 * A singleton manager class that handles theme-related functionality.
 */
object ThemeManager {

    // Theme mode constants
    const val THEME_MODE_SYSTEM = "system"
    const val THEME_MODE_LIGHT = "light"
    const val THEME_MODE_DARK = "dark"

    // Shared preference keys
    private const val PREF_DYNAMIC_COLORS = "dynamic_theming"
    private const val PREF_THEME_MODE = "theme_mode"

    /**
     * Apply theme settings at application startup
     * @param context The application context
     */
    fun applyThemeSettings(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Apply theme mode
        val themeMode = prefs.getString(PREF_THEME_MODE, THEME_MODE_SYSTEM) ?: THEME_MODE_SYSTEM
        applyThemeMode(themeMode)

        // Apply dynamic colors if enabled
        val dynamicColorsEnabled = prefs.getBoolean(PREF_DYNAMIC_COLORS, false)
        if (dynamicColorsEnabled && isDynamicColorSupported()) {
            // Apply to the application
            DynamicColors.applyToActivitiesIfAvailable(context.applicationContext as android.app.Application)
        }
    }

    /**
     * Applies dynamic colors if enabled in preferences
     * @param context The context to apply dynamic colors to
     * @return Whether dynamic colors were applied
     */
    fun applyDynamicColorsIfEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dynamicColorsEnabled = prefs.getBoolean(PREF_DYNAMIC_COLORS, false)

        if (dynamicColorsEnabled && isDynamicColorSupported()) {
            // Apply dynamic colors immediately
            DynamicColors.applyToActivitiesIfAvailable(context.applicationContext as android.app.Application)
            return true
        }
        return false
    }

    /**
     * Apply specific dynamic colors options to a single activity
     * @param activity The activity to apply dynamic colors to
     */
    fun applyDynamicColorsToActivity(activity: Activity) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val dynamicColorsEnabled = prefs.getBoolean(PREF_DYNAMIC_COLORS, false)

        if (dynamicColorsEnabled && isDynamicColorSupported()) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }

    /**
     * Apply theme mode to the whole application
     * @param themeMode The theme mode to apply (system, light, or dark)
     */
    fun applyThemeMode(themeMode: String) {
        val nightMode = when (themeMode) {
            THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * Sets the theme mode and saves the preference
     * @param context The context to apply the theme mode to
     * @param mode The theme mode to apply (system, light, or dark)
     */
    fun setThemeMode(context: Context, mode: String) {
        // Save preference
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putString(PREF_THEME_MODE, mode)
        }

        // Apply theme mode
        applyThemeMode(mode)
    }

    /**
     * Toggles dynamic colors and applies the change immediately
     */
    fun toggleDynamicColors(context: Context, enabled: Boolean): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Save preference
        prefs.edit {
            putBoolean(PREF_DYNAMIC_COLORS, enabled)
            apply() // Make sure it's applied immediately
        }

        // For immediate effect in the current activity:
        if (context is Activity) {
            // Need to explicitly set theme based on new setting
            if (!enabled) {
                // When turning OFF dynamic colors, apply the static theme
                context.setTheme(R.style.Theme_DBST)
            }
        }

        // Always return true to indicate the activity should be recreated
        return true
    }

    /**
     * Helper method to determine if dynamic colors are supported on this device
     */
    fun isDynamicColorSupported(): Boolean {
        return DynamicColors.isDynamicColorAvailable()
    }

    /**
     * Recreates all provided activities to apply theme changes
     * @param activities Set of activities to recreate
     */
    fun recreateActivities(activities: Set<Activity>) {
        activities.forEach { activity ->
            activity.recreate()
        }
    }
}