package org.exthm.featuresettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.exthm.featuresettings.ui.settings.CategorySettingsScreen
import org.exthm.featuresettings.ui.settings.SettingsCategory
import org.exthm.featuresettings.ui.settings.SettingsViewModel
import org.exthm.featuresettings.ui.theme.FeatureSettingsTheme

class CategoryActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val category = SettingsCategory.fromId(intent.getStringExtra(EXTRA_CATEGORY))
        setContent {
            FeatureSettingsTheme {
                CategorySettingsScreen(
                    viewModel = viewModel,
                    category = category,
                    onNavigateBack = { finish() }
                )
            }
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
