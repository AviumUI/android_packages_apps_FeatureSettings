package org.exthm.featuresettings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.SliderPreference
import org.exthm.featuresettings.ui.settings.SettingsCategory
import org.exthm.featuresettings.utils.SystemPropertiesHelper

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
            SettingsCategory.STATUS_BAR -> R.xml.feature_settings_status_bar
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
    }

    private fun bindDesktopPreferences() {
        bindSwitch(KEY_LAUNCHER_BLUR, SystemPropertiesHelper.getBoolean(LAUNCHER_BLUR_KEY, false)) { enabled ->
            SystemPropertiesHelper.set(LAUNCHER_BLUR_KEY, enabled.toString())
        }
    }

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

    private fun bindSystemPreferences() {
        bindSwitch(KEY_FORCE_SCREENSHOT, SystemPropertiesHelper.getBoolean(FORCE_SCREENSHOT_KEY, false)) { enabled ->
            val targetValue = if (enabled) ENABLED_VALUE else DISABLED_VALUE
            SystemPropertiesHelper.set(FORCE_SCREENSHOT_KEY, targetValue)
        }

        val screenOcrPref = findPreference<SwitchPreferenceCompat>(KEY_SCREEN_OCR)
        val screenOcrHighPref = findPreference<SliderPreference>(KEY_SCREEN_OCR_HIGH)

        val screenOcrEnabled = SystemPropertiesHelper.getBoolean(SCREEN_OCR_KEY, false)
        screenOcrPref?.isPersistent = false
        screenOcrPref?.isChecked = screenOcrEnabled
        screenOcrHighPref?.isEnabled = screenOcrEnabled

        screenOcrPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            SystemPropertiesHelper.set(SCREEN_OCR_KEY, enabled.toString())
            screenOcrHighPref?.isEnabled = enabled
            true
        }

        screenOcrHighPref?.let { pref ->
            pref.isPersistent = false
            val minValue = pref.min
            val maxValue = pref.max
            val value = SystemPropertiesHelper.getInt(SCREEN_OCR_HIGH_KEY, DEFAULT_SCREEN_OCR_HIGH)
                .coerceIn(minValue, maxValue)
            pref.value = value
            pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val intValue = newValue as Int
                SystemPropertiesHelper.set(SCREEN_OCR_HIGH_KEY, intValue.toString())
                true
            }
        }
    }

    private fun bindSwitch(key: String, initialValue: Boolean, onChange: (Boolean) -> Unit) {
        val pref = findPreference<SwitchPreferenceCompat>(key) ?: return
        pref.isPersistent = false
        pref.isChecked = initialValue
        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            onChange(newValue as Boolean)
            true
        }
    }

    private fun showDisableSensorAppSelection(pref: Preference) {
        if (installedApps.isEmpty()) {
            loadInstalledApps { showDisableSensorDialog(pref) }
        } else {
            showDisableSensorDialog(pref)
        }
    }

    private fun showDisableSensorDialog(pref: Preference) {
        if (!isAdded) return
        if (installedApps.isEmpty()) return

        val selectedApps = loadDisableSensorApps().toMutableSet()
        val appNames = installedApps.map { it.appName }.toTypedArray()
        val appPackages = installedApps.map { it.packageName }.toTypedArray()
        val checkedItems = appPackages.map { selectedApps.contains(it) }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.app_selection_dialog_title)
            .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
                val packageName = appPackages[which]
                if (isChecked) {
                    selectedApps.add(packageName)
                } else {
                    selectedApps.remove(packageName)
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                saveDisableSensorApps(selectedApps)
                updateDisableSensorSummary(pref, selectedApps)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

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

    private fun loadDisableSensorApps(): Set<String> {
        val appsString = SystemPropertiesHelper.get(DISABLE_SENSOR_APPS_KEY, "")
        return if (appsString.isNotBlank()) {
            appsString.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else {
            emptySet()
        }
    }

    private fun saveDisableSensorApps(selectedApps: Set<String>) {
        val appsString = selectedApps.joinToString(",")
        SystemPropertiesHelper.set(DISABLE_SENSOR_APPS_KEY, appsString)
    }

    private fun updateDisableSensorSummary(pref: Preference?, selectedApps: Set<String>) {
        pref?.summary = if (selectedApps.isEmpty()) {
            getString(R.string.disable_sensor_summary_none)
        } else {
            getString(R.string.disable_sensor_summary_selected, selectedApps.size)
        }
    }

    private fun getLyricEnabled(): Boolean {
        val currentValue = SystemPropertiesHelper.getSecureString(
            requireContext().contentResolver,
            STATUS_BAR_LYRIC_KEY,
            LYRIC_DISABLED_VALUE
        )
        return currentValue == LYRIC_ENABLED_VALUE
    }

    private fun setLyricEnabled(enabled: Boolean) {
        val targetValue = if (enabled) LYRIC_ENABLED_VALUE else LYRIC_DISABLED_VALUE
        SystemPropertiesHelper.setSecureString(
            requireContext().contentResolver,
            STATUS_BAR_LYRIC_KEY,
            targetValue
        )
    }

    private fun sendCustomLockscreenBroadcast() {
        try {
            val intent = Intent("org.avium.systemui.lockscreen.SETTINGS_CHANGED")
            intent.flags = Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
            requireContext().sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
        private const val ARG_CATEGORY = "arg_category"

        private const val LOCKSCREEN_DIM_KEY = "persist.avium.lockscreendim"
        private const val LAUNCHER_BLUR_KEY = "persist.avium.launcherblur"
        private const val SCREEN_OCR_KEY = "persist.avium.screenocr"
        private const val SCREEN_OCR_HIGH_KEY = "persist.avium.screenocr_high"
        private const val DISABLE_SENSOR_KEY = "persist.avium.disablesensor"
        private const val DISABLE_SENSOR_APPS_KEY = "persist.avium.disablesensor.apps"
        private const val STATUS_BAR_LYRIC_KEY = "status_bar_show_lyric"
        private const val MUSIC_LOCKSCREEN_KEY = "persist.avium.lockscreen.music"
        private const val MUSIC_LOCKSCREEN_UNLOCK_KEY = "persist.avium.lockscreen.music.unlock"
        private const val CUSTOM_LOCKSCREEN_KEY = "persist.avium.customlockscreen.enable"
        private const val DEPTH_WALLPAPER_KEY = "persist.avium.depthwallpaper"
        private const val FORCE_SCREENSHOT_KEY = "persist.avium.forcescreenshot"

        private const val LYRIC_ENABLED_VALUE = "1"
        private const val LYRIC_DISABLED_VALUE = "0"
        private const val ENABLED_VALUE = "1"
        private const val DISABLED_VALUE = "0"

        private const val DEFAULT_SCREEN_OCR_HIGH = 6

        private const val KEY_STATUS_BAR_LYRIC = "status_bar_lyric"
        private const val KEY_LAUNCHER_BLUR = "launcher_blur"
        private const val KEY_DISABLE_SENSOR = "disable_sensor"
        private const val KEY_DISABLE_SENSOR_APPS = "disable_sensor_apps"
        private const val KEY_MUSIC_LOCKSCREEN = "music_lockscreen"
        private const val KEY_MUSIC_LOCKSCREEN_UNLOCK = "music_lockscreen_unlock"
        private const val KEY_CUSTOM_LOCKSCREEN = "custom_lockscreen"
        private const val KEY_CUSTOM_LOCKSCREEN_SETTINGS = "custom_lockscreen_settings"
        private const val KEY_DEPTH_WALLPAPER = "depth_wallpaper"
        private const val KEY_DEPTH_WALLPAPER_SETTINGS = "depth_wallpaper_settings"
        private const val KEY_LOCKSCREEN_DIM = "lockscreen_dim"
        private const val KEY_FORCE_SCREENSHOT = "force_screenshot"
        private const val KEY_SCREEN_OCR = "screen_ocr"
        private const val KEY_SCREEN_OCR_HIGH = "screen_ocr_high"

        fun newInstance(categoryId: String): CategorySettingsFragment {
            return CategorySettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, categoryId)
                }
            }
        }
    }
}
