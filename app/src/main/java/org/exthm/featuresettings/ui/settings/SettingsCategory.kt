package org.exthm.featuresettings.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.exthm.featuresettings.R

enum class SettingsCategory(
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    @DrawableRes val iconRes: Int
) {
    STATUS_BAR(
        R.string.category_status_bar,
        R.string.category_status_bar_summary,
        R.drawable.ic_statusbar
    ),
    DESKTOP(
        R.string.category_desktop,
        R.string.category_desktop_summary,
        R.drawable.ic_home
    ),
    PRIVACY_SECURITY(
        R.string.category_privacy_security,
        R.string.category_privacy_security_summary,
        R.drawable.ic_secure
    ),
    LOCKSCREEN(
        R.string.category_lockscreen,
        R.string.category_lockscreen_summary,
        R.drawable.ic_lockscreen
    ),
    SYSTEM(
        R.string.category_system,
        R.string.category_system_summary,
        R.drawable.ic_system
    );


    companion object {
        val displayOrder = listOf(
            STATUS_BAR,
            SYSTEM,
            DESKTOP,
            LOCKSCREEN,
            PRIVACY_SECURITY
        )


        fun fromId(id: String?): SettingsCategory {
            return values().firstOrNull { it.name == id } ?: SYSTEM
        }
    }
}
