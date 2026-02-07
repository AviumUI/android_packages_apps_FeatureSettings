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
    }
}
