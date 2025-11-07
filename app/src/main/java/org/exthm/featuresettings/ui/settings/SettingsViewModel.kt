package org.exthm.featuresettings.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.exthm.featuresettings.utils.SystemPropertiesHelper
import android.provider.Settings
import android.provider.Settings.Secure
import java.io.File
import java.io.FileWriter
import java.io.IOException

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    data class AppInfo(val packageName: String,
                       val appName: String)

    companion object {
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
        private const val FAKE_BL_UNLOCK_KEY = "persist.avium.fakeblunlock"
        private const val FAKE_BL_UNLOCK_KEY_STATUS = "ro.avium.status_fake_prop"
        private const val DEPTH_WALLPAPER_KEY = "persist.avium.depthwallpaper"
        private const val FORCE_SCREENSHOT_KEY = "persist.avium.forcescreenshot"
        private const val LYRIC_ENABLED_VALUE = "1"  
        private const val LYRIC_DISABLED_VALUE = "0"  
        private const val ENABLED_VALUE = "1"
        private const val DISABLED_VALUE = "0"
    }

    private val _lockscreenDimEnabled = MutableStateFlow(false)
    val lockscreenDimEnabled: StateFlow<Boolean> = _lockscreenDimEnabled

    private val _launcherBlurEnabled = MutableStateFlow(false)
    val launcherBlurEnabled: StateFlow<Boolean> = _launcherBlurEnabled

    private val _screenOcrEnabled = MutableStateFlow(false)
    val screenOcrEnabled: StateFlow<Boolean> = _screenOcrEnabled

    private val _screenOcrHighValue = MutableStateFlow(0f)
    val screenOcrHighValue: StateFlow<Float> = _screenOcrHighValue

    private val _disableSensorEnabled = MutableStateFlow(false)
    val disableSensorEnabled: StateFlow<Boolean> = _disableSensorEnabled

    private val _disableSensorApps = MutableStateFlow<Set<String>>(emptySet())
    val disableSensorApps: StateFlow<Set<String>> = _disableSensorApps

    private val _showAppSelectionDialog = MutableStateFlow(false)
    val showAppSelectionDialog: StateFlow<Boolean> = _showAppSelectionDialog

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps

    private val _statusBarLyricEnabled = MutableStateFlow(false)
    val statusBarLyricEnabled: StateFlow<Boolean> = _statusBarLyricEnabled

    private val _musicLockscreenEnabled = MutableStateFlow(false)
    val musicLockscreenEnabled: StateFlow<Boolean> = _musicLockscreenEnabled

    private val _musicLockscreenUnlockEnabled = MutableStateFlow(false)
    val musicLockscreenUnlockEnabled: StateFlow<Boolean> = _musicLockscreenUnlockEnabled

    private val _customLockscreenEnabled = MutableStateFlow(false)
    val customLockscreenEnabled: StateFlow<Boolean> = _customLockscreenEnabled

    private val _fakeBlUnlockEnabled = MutableStateFlow(false)
    val fakeBlUnlockEnabled: StateFlow<Boolean> = _fakeBlUnlockEnabled

    private val _depthWallpaperEnabled = MutableStateFlow(false)
    val depthWallpaperEnabled: StateFlow<Boolean> = _depthWallpaperEnabled

    private val _forceScreenshotEnabled = MutableStateFlow(false)
    val forceScreenshotEnabled: StateFlow<Boolean> = _forceScreenshotEnabled

    init {
        loadInitialSettings()
        loadInstalledApps()
    }

    private fun loadInitialSettings() {
        _lockscreenDimEnabled.value = SystemPropertiesHelper.getBoolean(LOCKSCREEN_DIM_KEY, false)
        _launcherBlurEnabled.value = SystemPropertiesHelper.getBoolean(LAUNCHER_BLUR_KEY, false)
        _screenOcrEnabled.value = SystemPropertiesHelper.getBoolean(SCREEN_OCR_KEY, false)
        _screenOcrHighValue.value = SystemPropertiesHelper.getInt(SCREEN_OCR_HIGH_KEY, 6).toFloat()

        _disableSensorEnabled.value = SystemPropertiesHelper.getBoolean(DISABLE_SENSOR_KEY, false)
        val appsString = SystemPropertiesHelper.get(DISABLE_SENSOR_APPS_KEY, "")
        _disableSensorApps.value = if (appsString.isNotBlank()) {
            appsString.split(',').toSet()
        } else {
            emptySet()
        }

        val lyricCurrentValue = SystemPropertiesHelper.getSecureString(
            getApplication<Application>().contentResolver,
            STATUS_BAR_LYRIC_KEY,
            LYRIC_DISABLED_VALUE 
        )
        _statusBarLyricEnabled.value = lyricCurrentValue == LYRIC_ENABLED_VALUE

        _musicLockscreenEnabled.value = SystemPropertiesHelper.getBoolean(MUSIC_LOCKSCREEN_KEY, false)
        _musicLockscreenUnlockEnabled.value = SystemPropertiesHelper.getBoolean(MUSIC_LOCKSCREEN_UNLOCK_KEY, false)
        
        _customLockscreenEnabled.value = SystemPropertiesHelper.getBoolean(CUSTOM_LOCKSCREEN_KEY, false)
        _fakeBlUnlockEnabled.value = SystemPropertiesHelper.getBoolean(FAKE_BL_UNLOCK_KEY, false)
        
        _depthWallpaperEnabled.value = SystemPropertiesHelper.getBoolean(DEPTH_WALLPAPER_KEY, false)
        
        _forceScreenshotEnabled.value = SystemPropertiesHelper.getBoolean(FORCE_SCREENSHOT_KEY, false)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
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

            withContext(Dispatchers.Main) {
                _installedApps.value = apps
            }
        }
    }

    fun onDisableSensorChanged(enabled: Boolean) {
        _disableSensorEnabled.value = enabled
        viewModelScope.launch {
            SystemPropertiesHelper.set(DISABLE_SENSOR_KEY, enabled.toString())
        }
    }

    fun onShowAppSelectionDialog() {
        _showAppSelectionDialog.value = true
    }

    fun onDismissAppSelectionDialog() {
        _showAppSelectionDialog.value = false
    }

    fun onAppSelectionConfirmed(selectedApps: Set<String>) {
        _disableSensorApps.value = selectedApps
        viewModelScope.launch {
            val appsString = selectedApps.joinToString(",")
            SystemPropertiesHelper.set(DISABLE_SENSOR_APPS_KEY, appsString)
        }
        onDismissAppSelectionDialog()
    }


    fun onLockscreenDimChanged(enabled: Boolean) {
        _lockscreenDimEnabled.value = enabled
        viewModelScope.launch {
            SystemPropertiesHelper.set(LOCKSCREEN_DIM_KEY, enabled.toString())
        }
    }

    fun onLauncherBlurChanged(enabled: Boolean) {
        _launcherBlurEnabled.value = enabled
        viewModelScope.launch {
            SystemPropertiesHelper.set(LAUNCHER_BLUR_KEY, enabled.toString())
        }
    }

    fun onScreenOcrChanged(enabled: Boolean) {
        _screenOcrEnabled.value = enabled
        viewModelScope.launch {
            SystemPropertiesHelper.set(SCREEN_OCR_KEY, enabled.toString())
        }
    }

    fun onScreenOcrHighChanged(value: Float) {
        _screenOcrHighValue.value = value
    }

    fun onScreenOcrHighChangeFinished(value: Float) {
        viewModelScope.launch {
            SystemPropertiesHelper.set(SCREEN_OCR_HIGH_KEY, value.toInt().toString())
        }
    }

    fun onStatusBarLyricChanged(enabled: Boolean) {
        _statusBarLyricEnabled.value = enabled
        viewModelScope.launch {
            val targetValue = if (enabled) LYRIC_ENABLED_VALUE else LYRIC_DISABLED_VALUE
            SystemPropertiesHelper.setSecureString(
                getApplication<Application>().contentResolver,
                STATUS_BAR_LYRIC_KEY,
                targetValue
            )
        }
    }

    fun onMusicLockscreenChanged(enabled: Boolean) {
        _musicLockscreenEnabled.value = enabled
        viewModelScope.launch {
            val targetValue = if (enabled) ENABLED_VALUE else DISABLED_VALUE
            SystemPropertiesHelper.set(MUSIC_LOCKSCREEN_KEY, targetValue)
        }
    }

    fun onMusicLockscreenUnlockChanged(enabled: Boolean) {
        _musicLockscreenUnlockEnabled.value = enabled
        viewModelScope.launch {
            val targetValue = if (enabled) "true" else "false"
            SystemPropertiesHelper.set(MUSIC_LOCKSCREEN_UNLOCK_KEY, targetValue)
        }
    }

    fun onCustomLockscreenChanged(enabled: Boolean) {
        _customLockscreenEnabled.value = enabled
        viewModelScope.launch {
            val targetValue = if (enabled) "true" else "false"
            SystemPropertiesHelper.set(CUSTOM_LOCKSCREEN_KEY, targetValue)
            sendCustomLockscreenBroadcast()
        }
    }

    fun onFakeBlUnlockChanged(enabled: Boolean) {
        _fakeBlUnlockEnabled.value = enabled
        viewModelScope.launch {
            val targetValue = if (enabled) "true" else "false"
            SystemPropertiesHelper.set(FAKE_BL_UNLOCK_KEY, targetValue)
            writeFakeBlUnlockToConfig(targetValue)
        }
    }

    private fun sendCustomLockscreenBroadcast() {
        try {
            val intent = Intent("org.avium.systemui.lockscreen.SETTINGS_CHANGED")
            intent.flags = Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
            getApplication<Application>().sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun launchCustomLockscreenApp() {
        try {
            val intent = Intent()
            intent.setClassName("org.avium.lockscreenedit", "org.avium.lockscreenedit.MainActivity")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeFakeBlUnlockToConfig(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val configFile = File("/metadata/avium/avium_init.cfg")
                val parentDir = configFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        SystemPropertiesHelper.set("persist.avium.fakeblunlock.value", value)
                        return@launch
                    }
                }
                val existingContent = try {
                    if (configFile.exists()) {
                        configFile.readText()
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    ""
                }

                val lines = existingContent.split("\n").toMutableList()
                val fakePropLine = "set_fake_prop=$value"

                var found = false
                for (i in lines.indices) {
                    if (lines[i].startsWith("set_fake_prop=")) {
                        lines[i] = fakePropLine
                        found = true
                        break
                    }
                }

                if (!found) {
                    lines.add(fakePropLine)
                }

                try {
                    FileWriter(configFile).use { writer ->
                        writer.write(lines.joinToString("\n"))
                    }
                } catch (e: IOException) {
                    SystemPropertiesHelper.set("persist.avium.fakeblunlock.value", value)
                }
            } catch (e: Exception) {
                SystemPropertiesHelper.set("persist.avium.fakeblunlock.value", value)
            }
        }
    }

    fun onDepthWallpaperChanged(enabled: Boolean) {
        _depthWallpaperEnabled.value = enabled
        viewModelScope.launch {
            val targetValue = if (enabled) ENABLED_VALUE else DISABLED_VALUE
            SystemPropertiesHelper.set(DEPTH_WALLPAPER_KEY, targetValue)
            sendCustomLockscreenBroadcast()
        }
    }

    fun launchDepthWallpaperApp() {
        try {
            val intent = Intent()
            intent.setClassName("org.avium.aviumdepthwallpaper", "org.avium.aviumdepthwallpaper.MainActivity")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onForceScreenshotChanged(enabled: Boolean) {
        _forceScreenshotEnabled.value = enabled
        viewModelScope.launch {
            val targetValue = if (enabled) ENABLED_VALUE else DISABLED_VALUE
            SystemPropertiesHelper.set(FORCE_SCREENSHOT_KEY, targetValue)
        }
    }
}
