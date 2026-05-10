/*
 * Copyright (C) 2025-2026 The AviumUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exthm.featuresettings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.exthm.featuresettings.ui.settings.SettingsCategory
import org.exthm.featuresettings.utils.SystemPropertiesHelper
import java.io.File

class CategorySettingsFragment : SettingsBasePreferenceFragment() {

    private var installedApps: List<AppInfo> = emptyList()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val categoryId = arguments?.getString(ARG_CATEGORY)
        val category = SettingsCategory.fromId(categoryId)
        setPreferencesFromResource(getCategoryXml(category), rootKey)

        when (category) {
            SettingsCategory.STATUS_BAR -> bindStatusBarPreferences()
            SettingsCategory.DESKTOP -> bindDesktopPreferences()
            SettingsCategory.PRIVACY_SECURITY -> bindPrivacyPreferences()
            SettingsCategory.LOCKSCREEN -> bindLockscreenPreferences()
            SettingsCategory.SYSTEM -> bindSystemPreferences()
        }
    }

    private fun getCategoryXml(category: SettingsCategory): Int {
        return when (category) {
            SettingsCategory.STATUS_BAR -> R.xml.feature_settings_ui
            SettingsCategory.DESKTOP -> R.xml.feature_settings_desktop
            SettingsCategory.PRIVACY_SECURITY -> R.xml.feature_settings_privacy
            SettingsCategory.LOCKSCREEN -> R.xml.feature_settings_lockscreen
            SettingsCategory.SYSTEM -> R.xml.feature_settings_system
        }
    }

    private fun bindStatusBarPreferences() {
        val enabled = getLyricEnabled()
        bindSwitch(KEY_STATUS_BAR_LYRIC, enabled) { isEnabled ->
            setLyricEnabled(isEnabled)
        }
        bindSettingToggle(KEY_STATUSBAR_COLORED_ICONS, STATUSBAR_COLORED_ICONS_KEY, SettingTable.SYSTEM)
        bindSettingToggle(
            KEY_SHOW_MEDIA_SQUIGGLE_ANIMATION,
            SHOW_MEDIA_SQUIGGLE_ANIMATION_KEY,
            SettingTable.SECURE,
            1
        )
        bindSettingToggle(KEY_STATUSBAR_NOTIF_COUNT, STATUSBAR_NOTIF_COUNT_KEY, SettingTable.SYSTEM)
    }

    private fun bindDesktopPreferences() {
        bindSwitch(KEY_LAUNCHER_BLUR, SystemPropertiesHelper.getBoolean(LAUNCHER_BLUR_KEY, false)) { enabled ->
            SystemPropertiesHelper.set(LAUNCHER_BLUR_KEY, enabled.toString())
        }

        val moreSettingsPref = findPreference<Preference>(KEY_DESKTOP_MORE_SETTINGS)
        val intent = Intent().setClassName(
            LAUNCHER_PACKAGE,
            LAUNCHER_SETTINGS_ACTIVITY
        )
        val canResolve = intent.resolveActivity(requireContext().packageManager) != null
        moreSettingsPref?.isEnabled = canResolve
        moreSettingsPref?.isPersistent = false
        moreSettingsPref?.setOnPreferenceClickListener {
            startActivity(intent)
            true
        }
    }

    /**
     * Bind the switches in FeatureSettings -> Security & Privacy,
     * handle dependence or display logic.
     */
    private fun bindPrivacyPreferences() {
        val appsPref = findPreference<Preference>(KEY_DISABLE_SENSOR_APPS)
        val disableSensorPref = findPreference<SwitchPreferenceCompat>(KEY_DISABLE_SENSOR)

        val enabled = SystemPropertiesHelper.getBoolean(DISABLE_SENSOR_KEY, false)
        disableSensorPref?.isPersistent = false
        disableSensorPref?.isChecked = enabled
        appsPref?.isEnabled = enabled

        updateDisableSensorSummary(appsPref, loadDisableSensorApps())

        disableSensorPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            SystemPropertiesHelper.set(DISABLE_SENSOR_KEY, isEnabled.toString())
            appsPref?.isEnabled = isEnabled
            true
        }

        appsPref?.isPersistent = false
        appsPref?.setOnPreferenceClickListener {
            showDisableSensorAppSelection(appsPref)
            true
        }
    }

    /**
     * Bind the switches in FeatureSettings -> Lockscreen,
     * handle dependence or display logic.
     */
    private fun bindLockscreenPreferences() {
        bindSwitch(KEY_MUSIC_LOCKSCREEN, SystemPropertiesHelper.getBoolean(MUSIC_LOCKSCREEN_KEY, false)) { enabled ->
            val targetValue = if (enabled) ENABLED_VALUE else DISABLED_VALUE
            SystemPropertiesHelper.set(MUSIC_LOCKSCREEN_KEY, targetValue)

            findPreference<SwitchPreferenceCompat>(KEY_MUSIC_LOCKSCREEN_UNLOCK)?.isEnabled = enabled
        }

        val unlockPref = findPreference<SwitchPreferenceCompat>(KEY_MUSIC_LOCKSCREEN_UNLOCK)
        unlockPref?.isPersistent = false
        unlockPref?.isChecked = SystemPropertiesHelper.getBoolean(MUSIC_LOCKSCREEN_UNLOCK_KEY, false)
        unlockPref?.isEnabled = SystemPropertiesHelper.getBoolean(MUSIC_LOCKSCREEN_KEY, false)
        unlockPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val targetValue = if (newValue as Boolean) "true" else "false"
            SystemPropertiesHelper.set(MUSIC_LOCKSCREEN_UNLOCK_KEY, targetValue)
            true
        }

        bindSwitch(KEY_CUSTOM_LOCKSCREEN, SystemPropertiesHelper.getBoolean(CUSTOM_LOCKSCREEN_KEY, false)) { enabled ->
            val targetValue = if (enabled) "true" else "false"
            SystemPropertiesHelper.set(CUSTOM_LOCKSCREEN_KEY, targetValue)
            sendCustomLockscreenBroadcast()
        }

        findPreference<Preference>(KEY_CUSTOM_LOCKSCREEN_SETTINGS)?.apply {
            isPersistent = false
            setOnPreferenceClickListener {
                launchCustomLockscreenApp()
                true
            }
        }

        bindSwitch(KEY_DEPTH_WALLPAPER, SystemPropertiesHelper.getBoolean(DEPTH_WALLPAPER_KEY, false)) { enabled ->
            val targetValue = if (enabled) ENABLED_VALUE else DISABLED_VALUE
            SystemPropertiesHelper.set(DEPTH_WALLPAPER_KEY, targetValue)
            sendCustomLockscreenBroadcast()
        }

        findPreference<Preference>(KEY_DEPTH_WALLPAPER_SETTINGS)?.apply {
            isPersistent = false
            setOnPreferenceClickListener {
                launchDepthWallpaperApp()
                true
            }
        }

        bindSwitch(KEY_LOCKSCREEN_DIM, SystemPropertiesHelper.getBoolean(LOCKSCREEN_DIM_KEY, false)) { enabled ->
            SystemPropertiesHelper.set(LOCKSCREEN_DIM_KEY, enabled.toString())
        }
    }

    /**
     * Bind the switches in FeatureSettings -> System,
     * handle dependence or display logic.
     */
    private fun bindSystemPreferences() {

        /*
         * Bind force screenshot settings.
         */
        bindSwitch(KEY_FORCE_SCREENSHOT, SystemPropertiesHelper.getBoolean(FORCE_SCREENSHOT_KEY, false)) { enabled ->
            val targetValue = if (enabled) ENABLED_VALUE else DISABLED_VALUE
            SystemPropertiesHelper.set(FORCE_SCREENSHOT_KEY, targetValue)
        }

        /*
         * Bind and handle bootloader unlock status spoofing settings.
         */
        val fakeBlPref = findPreference<SwitchPreferenceCompat>(KEY_FAKE_BL_UNLOCK)
        val forcedOn = isFakeBlUnlockForcedOn()
        var fakeBlEnabled = if (forcedOn) true else readFakeBlUnlockFromConfig()
        fakeBlPref?.isPersistent = false
        fakeBlPref?.isChecked = fakeBlEnabled
        fakeBlPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (forcedOn && newValue == false) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.fake_bl_unlock_locked_title)
                    .setMessage(R.string.fake_bl_unlock_locked_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@OnPreferenceChangeListener false
            }
            val enabled = newValue as Boolean
            if (enabled != fakeBlEnabled) {
                showRebootRequiredDialog()
            }
            val targetValue = if (enabled) "true" else "false"
            writeFakeBlUnlockToConfig(targetValue)
            fakeBlEnabled = enabled
            true
        }

        /*
         * Bind vbmeta update settings.
         */
        bindSwitch(KEY_VBMETA_UPDATE, SystemPropertiesHelper.getBoolean(VBMETA_UPDATE_PROP_KEY, false)) { enabled ->
            SystemPropertiesHelper.set(VBMETA_UPDATE_PROP_KEY, enabled.toString())
        }

        bindSettingToggle(
            KEY_MISTOUCH_PREVENTION,
            MISTOUCH_PREVENTION_KEY,
            SettingTable.SECURE,
            1
        )

    }

    // TODO: Move this to utils
    private fun writeFakeBlUnlockToConfig(value: String) {
        Thread {
            try {
                val file = File(AVIUM_INIT_CFG)
                val newLine = "set_fake_prop=$value"

                if (!file.exists()) {
                    file.parentFile?.mkdirs()
                    file.writeText(newLine)
                    return@Thread
                }

                val content = file.readLines().toMutableList()

                if (content.isEmpty()) {
                    file.writeText(newLine)
                    return@Thread
                }

                var replaced = false

                for (i in content.indices) {
                    if (content[i].startsWith("set_fake_prop=")) {
                        content[i] = newLine
                        replaced = true
                        break
                    }
                }

                if (!replaced) {
                    content.add(newLine)
                }
                file.writeText(content.joinToString("\n"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // TODO: Move this to utils
    private fun showRebootRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.fake_bl_unlock_reboot_message)
            .setPositiveButton(R.string.reboot_dialog_reboot_now) { _, _ ->
                startActivity(Intent(Intent.ACTION_REBOOT))
            }
            .setNegativeButton(R.string.reboot_dialog_reboot_later, null)
            .show()
    }

    // TODO: Move this to utils
    private fun readFakeBlUnlockFromConfig(): Boolean {
        return try {
            val file = File(AVIUM_INIT_CFG)
            if (!file.exists()) {
                return false
            }
            val line = file.readLines().firstOrNull { it.startsWith("set_fake_prop=") } ?: return false
            val rawValue = line.substringAfter("set_fake_prop=").trim()
            rawValue.equals("true", true) || rawValue == "1"
        } catch (e: Exception) {
            false
        }
    }

    // TODO: Move this to utils
    private fun isFakeBlUnlockForcedOn(): Boolean {
        val statusEnabled = SystemPropertiesHelper.getBoolean(FAKE_BL_UNLOCK_STATUS_KEY, false)
        if (!statusEnabled) {
            return false
        }
        return isFakeBlUnlockConfigEmpty()
    }

    // TODO: Move this to utils
    private fun isFakeBlUnlockConfigEmpty(): Boolean {
        return try {
            val file = File(AVIUM_INIT_CFG)
            if (!file.exists()) {
                return true
            }
            file.readLines().all { it.isBlank() }
        } catch (e: Exception) {
            true
        }
    }

    /*
     * A utility to bind settings
     */
    private fun bindSwitch(key: String, initialValue: Boolean, onChange: (Boolean) -> Unit) {
        val pref = findPreference<SwitchPreferenceCompat>(key) ?: return
        pref.isPersistent = false
        pref.isChecked = initialValue
        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            onChange(newValue as Boolean)
            true
        }
    }

    /*
     * A utility to bind system/secure settings
     */
    private enum class SettingTable {
        SYSTEM,
        SECURE
    }

    private fun bindSettingToggle(
        prefKey: String,
        settingKey: String,
        table: SettingTable,
        defaultValue: Int = 0
    ) {
        val resolver = requireContext().contentResolver
        val enabled = when (table) {
            SettingTable.SYSTEM -> Settings.System.getInt(resolver, settingKey, defaultValue) == 1
            SettingTable.SECURE -> Settings.Secure.getInt(resolver, settingKey, defaultValue) == 1
        }
        bindSwitch(prefKey, enabled) { isEnabled ->
            val value = if (isEnabled) 1 else 0
            when (table) {
                SettingTable.SYSTEM -> Settings.System.putInt(resolver, settingKey, value)
                SettingTable.SECURE -> Settings.Secure.putInt(resolver, settingKey, value)
            }
        }
    }

    // TODO: Move this to utils
    private fun showDisableSensorAppSelection(pref: Preference) {
        if (installedApps.isEmpty()) {
            loadInstalledApps { showDisableSensorDialog(pref) }
        } else {
            showDisableSensorDialog(pref)
        }
    }

    // TODO: Move this to utils
    private fun showDisableSensorDialog(pref: Preference) {
        if (!isAdded) return
        if (installedApps.isEmpty()) return

        val configMap = loadDisableSensorAppConfigs().toMutableMap()
        val appNames = installedApps.map { it.appName }.toTypedArray()
        val appPackages = installedApps.map { it.packageName }.toTypedArray()

        val dialogView = layoutInflater.inflate(R.layout.dialog_sensor_app_selection, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.mode_radio_group)
        val radioMode1 = dialogView.findViewById<RadioButton>(R.id.radio_mode_1)
        val radioMode2 = dialogView.findViewById<RadioButton>(R.id.radio_mode_2)
        val modeDescription = dialogView.findViewById<TextView>(R.id.mode_description)
        val appList = dialogView.findViewById<android.widget.ListView>(R.id.app_list)

        var currentMode = "1"

        appList.adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_multiple_choice,
            appNames
        )
        appList.choiceMode = android.widget.ListView.CHOICE_MODE_MULTIPLE

        fun updateCheckedItems() {
            for (i in appPackages.indices) {
                val isChecked = configMap[appPackages[i]] == currentMode
                appList.setItemChecked(i, isChecked)
            }
        }

        fun updateModeDescription() {
            modeDescription.text = if (currentMode == "1") {
                getString(R.string.sensor_mode_1_summary)
            } else {
                getString(R.string.sensor_mode_2_summary)
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentMode = if (checkedId == R.id.radio_mode_1) "1" else "2"
            updateModeDescription()
            updateCheckedItems()
        }

        appList.setOnItemClickListener { _, _, position, _ ->
            val packageName = appPackages[position]
            if (appList.isItemChecked(position)) {
                configMap[packageName] = currentMode
            } else {
                configMap.remove(packageName)
            }
        }

        updateModeDescription()
        updateCheckedItems()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.app_selection_dialog_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                saveDisableSensorAppConfigs(configMap)
                updateDisableSensorSummary(pref, configMap.keys)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // TODO: Move this to utils
    private fun loadInstalledApps(onLoaded: () -> Unit) {
        val context = context ?: return
        Thread {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfoList = pm.queryIntentActivities(mainIntent, 0)
            val apps = resolveInfoList
                .mapNotNull { it.activityInfo?.applicationInfo }
                .distinctBy { it.packageName }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString()
                    )
                }
                .sortedBy { it.appName.lowercase() }

            installedApps = apps
            Handler(Looper.getMainLooper()).post {
                if (isAdded) {
                    onLoaded()
                }
            }
        }.start()
    }

    // TODO: Move this to utils
    private fun loadDisableSensorApps(): Set<String> {
        return loadDisableSensorAppConfigs().keys
    }

    private fun loadDisableSensorAppConfigs(): Map<String, String> {
        val appsString = Settings.System.getString(requireContext().contentResolver, SHAKE_SENSORS_BLACKLIST_KEY) ?: ""
        return if (appsString.isNotBlank()) {
            appsString.split(';').map { it.trim() }
                .filter { it.isNotEmpty() }
                .associate {
                    val parts = it.split(':')
                    parts[0] to (parts.getOrNull(1) ?: "1")
                }
        } else {
            emptyMap()
        }
    }

    // TODO: Move this to utils
    private fun saveDisableSensorAppConfigs(configMap: Map<String, String>) {
        val appsString = if (configMap.isNotEmpty()) {
            configMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        } else {
            ""
        }
        Settings.System.putString(requireContext().contentResolver, SHAKE_SENSORS_BLACKLIST_KEY, appsString)
    }

    // TODO: Move this to utils
    private fun updateDisableSensorSummary(pref: Preference?, selectedApps: Set<String>) {
        pref?.summary = if (selectedApps.isEmpty()) {
            getString(R.string.disable_sensor_summary_none)
        } else {
            getString(R.string.disable_sensor_summary_selected, selectedApps.size)
        }
    }

    // TODO: Move this to utils
    private fun getLyricEnabled(): Boolean {
        val currentValue = SystemPropertiesHelper.getSecureString(
            requireContext().contentResolver,
            STATUS_BAR_LYRIC_KEY,
            LYRIC_DISABLED_VALUE
        )
        return currentValue == LYRIC_ENABLED_VALUE
    }

    // TODO: Merge this to utils
    private fun setLyricEnabled(enabled: Boolean) {
        val targetValue = if (enabled) LYRIC_ENABLED_VALUE else LYRIC_DISABLED_VALUE
        SystemPropertiesHelper.setSecureString(
            requireContext().contentResolver,
            STATUS_BAR_LYRIC_KEY,
            targetValue
        )
    }

    // TODO: Move this to utils
    private fun sendCustomLockscreenBroadcast() {
        try {
            val intent = Intent("org.avium.systemui.lockscreen.SETTINGS_CHANGED")
            intent.flags = Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
            requireContext().sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // TODO: Move this to utils
    private fun launchCustomLockscreenApp() {
        try {
            val intent = Intent().apply {
                setClassName("org.avium.lockscreenedit", "org.avium.lockscreenedit.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // TODO: Move this to utils
    private fun launchDepthWallpaperApp() {
        try {
            val intent = Intent().apply {
                setClassName("org.avium.aviumdepthwallpaper", "org.avium.aviumdepthwallpaper.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class AppInfo(
        val packageName: String,
        val appName: String
    )

    companion object {
        private const val TAG = "CategorySettingsFragment"
        private const val ARG_CATEGORY = "arg_category"

        private const val ENABLED_VALUE = "1"
        private const val DISABLED_VALUE = "0"

        // Status Bar
        private const val STATUS_BAR_LYRIC_KEY = "status_bar_show_lyric"
        private const val STATUSBAR_COLORED_ICONS_KEY = "statusbar_colored_icons"
        private const val SHOW_MEDIA_SQUIGGLE_ANIMATION_KEY = "show_media_squiggle_animation"
        private const val STATUSBAR_NOTIF_COUNT_KEY = "statusbar_notif_count"
        private const val KEY_STATUS_BAR_LYRIC = "status_bar_lyric"
        private const val KEY_STATUSBAR_COLORED_ICONS = "statusbar_colored_icons"
        private const val KEY_SHOW_MEDIA_SQUIGGLE_ANIMATION = "show_media_squiggle_animation"
        private const val KEY_STATUSBAR_NOTIF_COUNT = "statusbar_notif_count"
        private const val LYRIC_ENABLED_VALUE = "1"
        private const val LYRIC_DISABLED_VALUE = "0"

        // Desktop
        private const val LAUNCHER_BLUR_KEY = "persist.avium.launcherblur"
        private const val KEY_LAUNCHER_BLUR = "launcher_blur"
        private const val KEY_DESKTOP_MORE_SETTINGS = "desktop_more_settings"
        private const val LAUNCHER_PACKAGE = "com.android.launcher3"
        private const val LAUNCHER_SETTINGS_ACTIVITY = "com.android.launcher3.settings.SettingsActivity"

        // Privacy & Security
        private const val DISABLE_SENSOR_KEY = "persist.avium.disablesensor"
        private const val SHAKE_SENSORS_BLACKLIST_KEY = "shake_sensors_blacklist_config"
        private const val KEY_DISABLE_SENSOR = "disable_sensor"
        private const val KEY_DISABLE_SENSOR_APPS = "disable_sensor_apps"

        // Lockscreen
        private const val MUSIC_LOCKSCREEN_KEY = "persist.avium.lockscreen.music"
        private const val MUSIC_LOCKSCREEN_UNLOCK_KEY = "persist.avium.lockscreen.music.unlock"
        private const val CUSTOM_LOCKSCREEN_KEY = "persist.avium.customlockscreen.enable"
        private const val DEPTH_WALLPAPER_KEY = "persist.avium.depthwallpaper"
        private const val LOCKSCREEN_DIM_KEY = "persist.avium.lockscreendim"
        private const val KEY_MUSIC_LOCKSCREEN = "music_lockscreen"
        private const val KEY_MUSIC_LOCKSCREEN_UNLOCK = "music_lockscreen_unlock"
        private const val KEY_CUSTOM_LOCKSCREEN = "custom_lockscreen"
        private const val KEY_CUSTOM_LOCKSCREEN_SETTINGS = "custom_lockscreen_settings"
        private const val KEY_DEPTH_WALLPAPER = "depth_wallpaper"
        private const val KEY_DEPTH_WALLPAPER_SETTINGS = "depth_wallpaper_settings"
        private const val KEY_LOCKSCREEN_DIM = "lockscreen_dim"

        // System -> Spoofing
        private const val FAKE_BL_UNLOCK_STATUS_KEY = "ro.avium.status_fake_prop"
        private const val VBMETA_UPDATE_PROP_KEY = "persist.sys.vbmeta.update"
        private const val AVIUM_INIT_CFG = "/metadata/avium/avium_init.cfg"
        private const val KEY_FAKE_BL_UNLOCK = "fake_bl_unlock"
        private const val KEY_VBMETA_UPDATE = "vbmeta_update"

        // System -> Misc
        private const val FORCE_SCREENSHOT_KEY = "persist.avium.forcescreenshot"
        private const val KEY_FORCE_SCREENSHOT = "force_screenshot"

        // System -> Mistouch Prevention
        private const val MISTOUCH_PREVENTION_KEY = "nt_mistouch_prevention_enable"
        private const val KEY_MISTOUCH_PREVENTION = "nt_mistouch_prevention_enable"

        fun newInstance(categoryId: String): CategorySettingsFragment {
            return CategorySettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, categoryId)
                }
            }
        }
    }
}
