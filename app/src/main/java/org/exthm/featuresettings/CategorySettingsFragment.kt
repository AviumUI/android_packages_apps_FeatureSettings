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
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.SliderPreference
import org.exthm.featuresettings.ui.settings.SettingsCategory
import org.exthm.featuresettings.utils.SystemPropertiesHelper
import java.io.File
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import android.util.Log
import android.widget.Toast

class CategorySettingsFragment : SettingsBasePreferenceFragment() {

    private var installedApps: List<AppInfo> = emptyList()
    private var keyboxFilePickerLauncher: ActivityResultLauncher<Array<String>>? = null
    private var keyboxLoadPref: Preference? = null
    private var keyboxClearPref: Preference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyboxFilePickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            handleKeyboxFileSelected(uri)
        }
    }

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
         * Bind MoonOCR settings.
         */
        val screenOcrPref = findPreference<SwitchPreferenceCompat>(KEY_SCREEN_OCR)
        val screenOcrHighPref = findPreference<SliderPreference>(KEY_SCREEN_OCR_HIGH)
        // Disable MoonOCR for gms builds
        val gmsEnabled = SystemPropertiesHelper.getBoolean(GMS_STATUS_KEY, false)
        if (gmsEnabled) {
            screenOcrPref?.isVisible = false
            screenOcrHighPref?.isVisible = false
        } else {
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

        /*
         * Bind vbmeta update settings.
         */
        bindSwitch(KEY_VBMETA_UPDATE, SystemPropertiesHelper.getBoolean(VBMETA_UPDATE_PROP_KEY, false)) { enabled ->
            SystemPropertiesHelper.set(VBMETA_UPDATE_PROP_KEY, enabled.toString())
        }

        /*
         * Bind PIF settings
         */
        bindSettingToggle(KEY_PI_ENABLE_SPOOF, Settings.Secure.PI_ENABLE_SPOOF, SettingTable.SECURE)
        bindSettingToggle(KEY_PI_GMS_CERT_CHAIN, Settings.Secure.PI_GMS_CERT_CHAIN, SettingTable.SECURE)
        bindSettingToggle(KEY_PI_GAMES_SPOOF, Settings.Secure.PI_GAMES_SPOOF, SettingTable.SECURE)
        bindSettingToggle(KEY_PI_PHOTOS_SPOOF, Settings.Secure.PI_PHOTOS_SPOOF, SettingTable.SECURE)
        bindSettingToggle(KEY_PI_NETFLIX_SPOOF, Settings.Secure.PI_NETFLIX_SPOOF, SettingTable.SECURE)
        bindKeyboxPreferences()
    }

    private fun bindKeyboxPreferences() {
        keyboxLoadPref = findPreference(KEY_KEYBOX_DATA_LOAD)
        keyboxClearPref = findPreference(KEY_KEYBOX_DATA_CLEAR)

        updateKeyboxPreferences()

        keyboxLoadPref?.isPersistent = false
        keyboxLoadPref?.setOnPreferenceClickListener {
            val launcher = keyboxFilePickerLauncher ?: return@setOnPreferenceClickListener true
            launcher.launch(arrayOf("text/xml", "application/xml", "application/*+xml"))
            true
        }

        keyboxClearPref?.isPersistent = false
        keyboxClearPref?.setOnPreferenceClickListener {
            Settings.Secure.putString(requireContext().contentResolver, Settings.Secure.KEYBOX_DATA, null)
            showKeyboxClearedDialog()
            updateKeyboxPreferences()
            true
        }
    }

    private fun showKeyboxClearedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.keybox_data_clear_title)
            .setMessage(R.string.keybox_toast_file_cleared)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showKeyboxLoadedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.keybox_data_title)
            .setMessage(R.string.keybox_toast_file_loaded)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun updateKeyboxPreferences() {
        val hasData = Settings.Secure.getString(
            requireContext().contentResolver, Settings.Secure.KEYBOX_DATA
        ) != null
        keyboxLoadPref?.summary = getString(
            if (hasData) R.string.keybox_data_loaded_summary else R.string.keybox_data_summary
        )
        keyboxClearPref?.isEnabled = hasData
        keyboxClearPref?.isVisible = hasData
    }

    private fun handleKeyboxFileSelected(uri: Uri?) {
        val ctx = requireContext()
        val cr: ContentResolver = ctx.contentResolver

        if (uri == null) {
            Toast.makeText(ctx, getString(R.string.keybox_toast_invalid_file_selected), Toast.LENGTH_SHORT).show()
            return
        }

        val type = cr.getType(uri)
        val isXmlMime = "text/xml" == type || "application/xml" == type
        val hasXmlExt = uri.path?.lowercase()?.endsWith(".xml") == true
        if (!isXmlMime && !hasXmlExt) {
            Toast.makeText(ctx, getString(R.string.keybox_toast_invalid_file_selected), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            cr.openInputStream(uri).use { inputStream ->
                if (inputStream == null) {
                    Toast.makeText(ctx, getString(R.string.keybox_toast_invalid_file_selected), Toast.LENGTH_SHORT).show()
                    return
                }
                val xml = readXml(inputStream)
                if (!validateXml(xml)) {
                    Toast.makeText(ctx, getString(R.string.keybox_toast_missing_data), Toast.LENGTH_SHORT).show()
                    return
                }
                Settings.Secure.putString(cr, Settings.Secure.KEYBOX_DATA, xml)
                updateKeyboxPreferences()
                showKeyboxLoadedDialog()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read XML file", e)
            Toast.makeText(ctx, getString(R.string.keybox_toast_invalid_file_selected), Toast.LENGTH_SHORT).show()
        }
    }

    private fun readXml(inputStream: InputStream): String {
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            val xmlContent = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                xmlContent.append(line).append('\n')
            }
            return xmlContent.toString()
        }
    }

    private fun validateXml(xml: String): Boolean {
        var hasEcdsaKey = false
        var hasRsaKey = false
        var hasEcdsaPrivKey = false
        var hasRsaPrivKey = false
        var ecdsaCertCount = 0
        var rsaCertCount = 0
        var numberOfKeyboxes = -1

        return try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))

            var currentAlg: String? = null

            var eventType = parser.next()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "NumberOfKeyboxes" -> {
                            parser.next()
                            if (parser.eventType == XmlPullParser.TEXT) {
                                numberOfKeyboxes = parser.text.trim().toIntOrNull() ?: -1
                            }
                        }
                        "Key" -> {
                            currentAlg = parser.getAttributeValue(null, "algorithm")
                            when {
                                "ecdsa".equals(currentAlg, ignoreCase = true) -> hasEcdsaKey = true
                                "rsa".equals(currentAlg, ignoreCase = true) -> hasRsaKey = true
                                else -> currentAlg = null
                            }
                        }
                        "PrivateKey" -> {
                            val format = parser.getAttributeValue(null, "format")
                            if (!"pem".equals(format, ignoreCase = true)) {
                                Log.w(TAG, "Invalid or missing format for PrivateKey")
                                return false
                            }
                            if ("ecdsa".equals(currentAlg, ignoreCase = true)) {
                                hasEcdsaPrivKey = true
                            } else if ("rsa".equals(currentAlg, ignoreCase = true)) {
                                hasRsaPrivKey = true
                            }
                        }
                        "Certificate" -> {
                            val format = parser.getAttributeValue(null, "format")
                            if (!"pem".equals(format, ignoreCase = true)) {
                                Log.w(TAG, "Invalid or missing format for Certificate")
                                return false
                            }
                            if ("ecdsa".equals(currentAlg, ignoreCase = true)) {
                                ecdsaCertCount++
                            } else if ("rsa".equals(currentAlg, ignoreCase = true)) {
                                rsaCertCount++
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && parser.name == "Key") {
                    currentAlg = null
                }
                eventType = parser.next()
            }

            val hasEcdsaBundle = hasEcdsaKey && hasEcdsaPrivKey && ecdsaCertCount >= 1
            val hasRsaBundle = hasRsaKey && hasRsaPrivKey && rsaCertCount >= 1
            val keyboxesOk = numberOfKeyboxes == -1 || numberOfKeyboxes >= 1
            keyboxesOk && (hasEcdsaBundle || hasRsaBundle)
        } catch (e: Exception) {
            Log.e(TAG, "XML validation failed", e)
            false
        }
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
        val appsString = SystemPropertiesHelper.get(DISABLE_SENSOR_APPS_KEY, "")
        return if (appsString.isNotBlank()) {
            appsString.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else {
            emptySet()
        }
    }

    // TODO: Move this to utils
    private fun saveDisableSensorApps(selectedApps: Set<String>) {
        val appsString = selectedApps.joinToString(",")
        SystemPropertiesHelper.set(DISABLE_SENSOR_APPS_KEY, appsString)
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
        private const val DISABLE_SENSOR_APPS_KEY = "persist.avium.disablesensor.apps"
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
        private const val KEY_PI_ENABLE_SPOOF = "pi_enable_spoof"
        private const val KEY_PI_GMS_CERT_CHAIN = "pi_gms_cert_chain"
        private const val KEY_PI_GAMES_SPOOF = "pi_games_spoof"
        private const val KEY_PI_PHOTOS_SPOOF = "pi_photos_spoof"
        private const val KEY_PI_NETFLIX_SPOOF = "pi_netflix_spoof"
        private const val KEY_KEYBOX_DATA_LOAD = "keybox_data_load"
        private const val KEY_KEYBOX_DATA_CLEAR = "keybox_data_clear"

        // System -> Misc
        private const val GMS_STATUS_KEY = "ro.avium.gms_status"
        private const val FORCE_SCREENSHOT_KEY = "persist.avium.forcescreenshot"
        private const val SCREEN_OCR_KEY = "persist.avium.screenocr"
        private const val SCREEN_OCR_HIGH_KEY = "persist.avium.screenocr_high"
        private const val KEY_FORCE_SCREENSHOT = "force_screenshot"
        private const val KEY_SCREEN_OCR = "screen_ocr"
        private const val KEY_SCREEN_OCR_HIGH = "screen_ocr_high"
        private const val DEFAULT_SCREEN_OCR_HIGH = 6

        fun newInstance(categoryId: String): CategorySettingsFragment {
            return CategorySettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, categoryId)
                }
            }
        }
    }
}
