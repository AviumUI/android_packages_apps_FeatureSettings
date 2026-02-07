package org.exthm.featuresettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import org.exthm.featuresettings.ui.settings.SettingsCategory

class CategoryActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val categoryId = intent.getStringExtra(EXTRA_CATEGORY)
        val category = SettingsCategory.fromId(categoryId)
        title = getString(category.titleRes)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    CategorySettingsFragment.newInstance(category.name)
                )
                .commit()
        }
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"

        fun newIntent(context: Context, category: SettingsCategory): Intent {
            return Intent(context, CategoryActivity::class.java)
                .putExtra(EXTRA_CATEGORY, category.name)
        }
    }
}
