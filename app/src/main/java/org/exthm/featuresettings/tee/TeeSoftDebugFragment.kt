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

package org.exthm.featuresettings.tee

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.exthm.featuresettings.R
import java.io.File
import java.io.FileOutputStream

class TeeSoftDebugFragment : SettingsBasePreferenceFragment() {

    private val documentPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != android.app.Activity.RESULT_OK || result.data == null) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            onKeyboxSelected(uri)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tee_soft_debug_settings, rootKey)
        loadConfigFromStore()
        bindPreferences()
    }

    override fun onResume() {
        super.onResume()
        loadConfigFromStore()
        bindPreferences()
    }

    private fun bindPreferences() {
        val activity = requireActivity() as TeeSoftDebugActivity

        val enablePref = findPreference<SwitchPreferenceCompat>(KEY_ENABLED) ?: return
        enablePref.isPersistent = false
        enablePref.isChecked = activity.teeEnabled
        enablePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            activity.teeEnabled = newValue as Boolean
            persistConfigToStore()
            refreshKeyboxSummary()
            true
        }

        val modePref = findPreference<ListPreference>(KEY_MODE) ?: return
        modePref.isPersistent = false
        modePref.value = activity.teeMode
        modePref.summary = modePref.entry
        modePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val mode = normalizeMode(newValue as String)
            activity.teeMode = mode
            modePref.value = mode
            modePref.summary = modePref.entry
            persistConfigToStore()
            true
        }

        val keyboxPref = findPreference<Preference>(KEY_KEYBOX_PATH) ?: return
        keyboxPref.isPersistent = false
        refreshKeyboxSummary()
        keyboxPref.setOnPreferenceClickListener {
            launchDocumentPicker()
            true
        }

        val showSystemPref = findPreference<SwitchPreferenceCompat>(KEY_SHOW_SYSTEM_APPS) ?: return
        showSystemPref.isPersistent = false
        showSystemPref.isChecked = activity.teeShowSystemApps
        showSystemPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            activity.teeShowSystemApps = newValue as Boolean
            persistConfigToStore()
            true
        }

        val addAppPref = findPreference<Preference>(KEY_ADD_APP) ?: return
        addAppPref.isPersistent = false
        addAppPref.setOnPreferenceClickListener {
            activity.navigateToAppSelection()
            true
        }

        refreshTargetAppPrefs()
    }

    private fun loadConfigFromStore() {
        val activity = requireActivity() as TeeSoftDebugActivity
        activity.teeEnabled = false
        activity.teeMode = MODE_PATCH
        activity.teeKeyboxPath = ""
        activity.teeShowSystemApps = false
        activity.teeTargetPackages.clear()

        val configFile = File(STORE_DIR, CONFIG_FILE)
        if (!configFile.exists()) return

        try {
            for (line in configFile.readText().lines()) {
                val sep = line.indexOf('=')
                if (sep <= 0) continue
                val key = line.substring(0, sep).trim()
                val value = line.substring(sep + 1).trim()
                when (key) {
                    "enabled" -> activity.teeEnabled = value == "1" || value.equals("true", ignoreCase = true)
                    "mode" -> activity.teeMode = normalizeMode(value)
                    "keybox_path" -> activity.teeKeyboxPath = value
                    "show_system_apps" -> activity.teeShowSystemApps = value == "1" || value.equals("true", ignoreCase = true)
                    "target_packages" -> {
                        if (value.isNotEmpty()) {
                            activity.teeTargetPackages.addAll(
                                value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun persistConfigToStore() {
        val activity = requireActivity() as TeeSoftDebugActivity
        Thread {
            try {
                val dir = File(STORE_DIR)
                if (!dir.exists()) {
                    dir.mkdirs()
                    dir.setReadable(true, false)
                    dir.setWritable(true, true)
                    dir.setExecutable(true, false)
                }
                val lines = listOf(
                    "enabled=${if (activity.teeEnabled) "true" else "false"}",
                    "mode=${activity.teeMode}",
                    "keybox_path=${activity.teeKeyboxPath}",
                    "show_system_apps=${if (activity.teeShowSystemApps) "true" else "false"}",
                    "target_packages=${activity.teeTargetPackages.joinToString(",")}"
                )
                val configFile = File(STORE_DIR, CONFIG_FILE)
                configFile.writeText(lines.joinToString("\n"))
                configFile.setReadable(true, false)
                configFile.setWritable(true, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun normalizeMode(mode: String): String {
        return when (mode) {
            MODE_PATCH, MODE_AUTO, MODE_GENERATE -> mode
            else -> MODE_PATCH
        }
    }

    private fun refreshKeyboxSummary() {
        val activity = requireActivity() as TeeSoftDebugActivity
        val pref = findPreference<Preference>(KEY_KEYBOX_PATH) ?: return
        pref.summary = if (activity.teeKeyboxPath.isEmpty()) {
            getString(R.string.tee_soft_debug_keybox_path_summary)
        } else {
            activity.teeKeyboxPath
        }
    }

    private fun launchDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/xml"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/xml", "application/xml"))
        }
        documentPickerLauncher.launch(intent)
    }

    private fun onKeyboxSelected(uri: Uri) {
        val activity = requireActivity() as TeeSoftDebugActivity
        val context = requireContext()

        try {
            requireActivity().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        try {
            val content = requireActivity().contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: ""

            if (content.isEmpty()) {
                Toast.makeText(context, R.string.tee_soft_debug_keybox_pick_failed, Toast.LENGTH_LONG).show()
                return
            }

            val targetDir = File(STORE_DIR)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Toast.makeText(context, R.string.tee_soft_debug_keybox_pick_failed, Toast.LENGTH_LONG).show()
                return
            }
            targetDir.setReadable(true, false)
            targetDir.setWritable(true, true)
            targetDir.setExecutable(true, false)

            val target = File(targetDir, KEYBOX_FILE)
            requireActivity().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            target.setReadable(true, false)
            target.setWritable(true, true)

            activity.teeKeyboxPath = target.absolutePath
            persistConfigToStore()
            refreshKeyboxSummary()
            Toast.makeText(
                context,
                getString(R.string.tee_soft_debug_keybox_pick_success, activity.teeKeyboxPath),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, R.string.tee_soft_debug_keybox_pick_failed, Toast.LENGTH_LONG).show()
        }
    }

    fun refreshTargetAppPrefs() {
        val activity = requireActivity() as TeeSoftDebugActivity
        val appsCategory = findPreference<PreferenceCategory>(KEY_APPLICATIONS) ?: return

        val toRemove = mutableListOf<Preference>()
        for (i in 0 until appsCategory.preferenceCount) {
            val pref = appsCategory.getPreference(i)
            if (pref.key != KEY_ADD_APP && pref.key != KEY_SHOW_SYSTEM_APPS) {
                toRemove.add(pref)
            }
        }
        for (pref in toRemove) {
            appsCategory.removePreference(pref)
        }

        if (activity.teeTargetPackages.isEmpty()) {
            val hint = Preference(requireContext())
            hint.setSummary(R.string.tee_soft_debug_applications_empty)
            hint.isEnabled = false
            appsCategory.addPreference(hint)
            return
        }

        val pm = requireContext().packageManager
        val sorted = activity.teeTargetPackages.sortedBy { it.lowercase() }

        for (pkg in sorted) {
            val pref = Preference(requireContext())
            pref.key = pkg
            pref.setSummary(pkg)
            pref.isPersistent = false
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                pref.title = info.loadLabel(pm)
                pref.icon = info.loadIcon(pm)
            } catch (e: Exception) {
                pref.title = pkg
            }
            pref.setOnPreferenceClickListener {
                confirmRemoveTargetPackage(pkg)
                true
            }
            appsCategory.addPreference(pref)
        }
    }

    private fun confirmRemoveTargetPackage(packageName: String) {
        val activity = requireActivity() as TeeSoftDebugActivity
        val builder = android.app.AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.tee_soft_debug_remove_app_message, packageName))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                activity.teeTargetPackages.remove(packageName)
                persistConfigToStore()
                refreshTargetAppPrefs()
            }
            .setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    companion object {
        const val STORE_DIR = "/data/system/avium/tee_soft_debug"
        const val CONFIG_FILE = "config.conf"
        const val KEYBOX_FILE = "keybox.xml"

        const val MODE_PATCH = "patch"
        const val MODE_AUTO = "auto"
        const val MODE_GENERATE = "generate"

        const val KEY_ENABLED = "tee_soft_debug_enabled"
        const val KEY_MODE = "tee_soft_debug_mode"
        const val KEY_KEYBOX_PATH = "tee_soft_debug_keybox_path"
        const val KEY_APPLICATIONS = "tee_soft_debug_applications"
        const val KEY_SHOW_SYSTEM_APPS = "tee_soft_debug_show_system_apps"
        const val KEY_ADD_APP = "tee_soft_debug_add_app"
    }
}
