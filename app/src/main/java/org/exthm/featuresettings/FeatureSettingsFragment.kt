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

import android.os.Bundle
import android.view.View
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.LayoutPreference
import org.exthm.featuresettings.ui.settings.SettingsCategory

class FeatureSettingsFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.feature_settings_main, rootKey)
        bindGridTiles()
    }

    companion object {
        private const val KEY_FEATURE_GRID = "feature_grid"
    }

    private fun bindGridTiles() {
        val gridPref = findPreference<LayoutPreference>(KEY_FEATURE_GRID) ?: return
        gridPref.findViewById<View>(R.id.tile_status_bar)?.setOnClickListener {
            startActivity(CategoryActivity.newIntent(requireContext(), SettingsCategory.STATUS_BAR))
        }
        gridPref.findViewById<View>(R.id.tile_system)?.setOnClickListener {
            startActivity(CategoryActivity.newIntent(requireContext(), SettingsCategory.SYSTEM))
        }
        gridPref.findViewById<View>(R.id.tile_desktop)?.setOnClickListener {
            startActivity(CategoryActivity.newIntent(requireContext(), SettingsCategory.DESKTOP))
        }
        gridPref.findViewById<View>(R.id.tile_lockscreen)?.setOnClickListener {
            startActivity(CategoryActivity.newIntent(requireContext(), SettingsCategory.LOCKSCREEN))
        }
        gridPref.findViewById<View>(R.id.tile_privacy_security)?.setOnClickListener {
            startActivity(CategoryActivity.newIntent(requireContext(), SettingsCategory.PRIVACY_SECURITY))
        }
        gridPref.findViewById<View>(R.id.tile_support)?.setOnClickListener {
            startActivity(org.exthm.featuresettings.ui.support.SupportActivity.newIntent(requireContext()))
        }
    }
}
