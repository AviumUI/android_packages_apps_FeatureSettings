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
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import org.exthm.featuresettings.R

class TeeAppSelectionFragment : Fragment() {

    private lateinit var listView: ListView
    private var allApps: List<AppEntry> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        listView = ListView(requireContext())
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.divider = null
        return listView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadApps()
    }

    override fun onPause() {
        super.onPause()
        saveSelection()
    }

    private fun loadApps() {
        val activity = requireActivity() as TeeSoftDebugActivity
        val pm = requireContext().packageManager

        Thread {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val apps = pm.queryIntentActivities(mainIntent, 0)
                .mapNotNull { it.activityInfo?.applicationInfo }
                .distinctBy { it.packageName }
                .filter { app ->
                    if (activity.teeShowSystemApps) true
                    else (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .sortedBy { it.loadLabel(pm).toString().lowercase() }
                .map { AppEntry(it.packageName, it.loadLabel(pm).toString()) }

            Handler(Looper.getMainLooper()).post {
                if (!isAdded) return@post
                allApps = apps

                val appNames = apps.map { it.appName }.toTypedArray()
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_multiple_choice,
                    appNames
                )
                listView.adapter = adapter

                for (i in apps.indices) {
                    if (apps[i].packageName in activity.teeTargetPackages) {
                        listView.setItemChecked(i, true)
                    }
                }
            }
        }.start()
    }

    private fun saveSelection() {
        val activity = requireActivity() as TeeSoftDebugActivity
        if (!::listView.isInitialized) return

        activity.teeTargetPackages.clear()
        for (i in allApps.indices) {
            if (listView.isItemChecked(i)) {
                activity.teeTargetPackages.add(allApps[i].packageName)
            }
        }
        persistConfig(activity)
    }

    private fun persistConfig(activity: TeeSoftDebugActivity) {
        Thread {
            try {
                val dir = java.io.File(TeeSoftDebugFragment.STORE_DIR)
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
                val configFile = java.io.File(
                    TeeSoftDebugFragment.STORE_DIR,
                    TeeSoftDebugFragment.CONFIG_FILE
                )
                configFile.writeText(lines.joinToString("\n"))
                configFile.setReadable(true, false)
                configFile.setWritable(true, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private data class AppEntry(
        val packageName: String,
        val appName: String
    )
}
