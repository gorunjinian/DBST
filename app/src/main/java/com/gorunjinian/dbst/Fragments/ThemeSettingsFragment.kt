package com.gorunjinian.dbst.fragments

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.ThemeManager

/**
 * A dialog fragment for theme settings
 */
class ThemeSettingsFragment : DialogFragment() {

    private lateinit var prefs: SharedPreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_theme_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Initialize UI components
        val themeToggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.theme_toggle_group)
        val lightThemeButton = view.findViewById<MaterialButton>(R.id.light_theme_button)
        val darkThemeButton = view.findViewById<MaterialButton>(R.id.dark_theme_button)
        val systemThemeButton = view.findViewById<MaterialButton>(R.id.system_theme_button)
        val dynamicColorsSwitch = view.findViewById<SwitchMaterial>(R.id.dynamic_colors_switch)
        val applyButton = view.findViewById<MaterialButton>(R.id.apply_button)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancel_button)

        // Set up current values
        val currentTheme = prefs.getString("theme_mode", ThemeManager.THEME_MODE_SYSTEM)
            ?: ThemeManager.THEME_MODE_SYSTEM
        val dynamicColorsEnabled = prefs.getBoolean("dynamic_theming", false)

        // Select the correct theme button
        when (currentTheme) {
            ThemeManager.THEME_MODE_LIGHT -> lightThemeButton.isChecked = true
            ThemeManager.THEME_MODE_DARK -> darkThemeButton.isChecked = true
            else -> systemThemeButton.isChecked = true
        }

        // Set dynamic colors switch
        dynamicColorsSwitch.isChecked = dynamicColorsEnabled

        // Check if dynamic colors are supported
        if (!ThemeManager.isDynamicColorSupported()) {
            dynamicColorsSwitch.isEnabled = false
            dynamicColorsSwitch.text = getString(R.string.dynamic_colors_not_supported)
        }

        // Set click listeners
        cancelButton.setOnClickListener { dismiss() }

        applyButton.setOnClickListener {
            // Get the selected theme mode
            val selectedThemeMode = when (themeToggleGroup.checkedButtonId) {
                R.id.light_theme_button -> ThemeManager.THEME_MODE_LIGHT
                R.id.dark_theme_button -> ThemeManager.THEME_MODE_DARK
                else -> ThemeManager.THEME_MODE_SYSTEM
            }

            // Save and apply theme mode if changed
            if (currentTheme != selectedThemeMode) {
                ThemeManager.setThemeMode(requireContext(), selectedThemeMode)
                prefs.edit { putString("theme_mode", selectedThemeMode) }
            }

            // Save and apply dynamic colors if changed
            if (dynamicColorsEnabled != dynamicColorsSwitch.isChecked) {
                ThemeManager.toggleDynamicColors(requireContext(), dynamicColorsSwitch.isChecked)
                prefs.edit { putBoolean("dynamic_theming", dynamicColorsSwitch.isChecked) }
            }

            Toast.makeText(requireContext(), "Theme settings applied", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}