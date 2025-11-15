package org.exthm.featuresettings.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.provider.Settings
import android.provider.Settings.Secure
import org.exthm.featuresettings.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureSettingsScreen(viewModel: SettingsViewModel) {
    val lockscreenDimEnabled by viewModel.lockscreenDimEnabled.collectAsStateWithLifecycle()
    val launcherBlurEnabled by viewModel.launcherBlurEnabled.collectAsStateWithLifecycle()
    val screenOcrEnabled by viewModel.screenOcrEnabled.collectAsStateWithLifecycle()
    val screenOcrHighValue by viewModel.screenOcrHighValue.collectAsStateWithLifecycle()
    val disableSensorEnabled by viewModel.disableSensorEnabled.collectAsStateWithLifecycle()
    val disableSensorApps by viewModel.disableSensorApps.collectAsStateWithLifecycle()
    val showAppSelectionDialog by viewModel.showAppSelectionDialog.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val statusBarLyricEnabled by viewModel.statusBarLyricEnabled.collectAsStateWithLifecycle()
    val musicLockscreenEnabled by viewModel.musicLockscreenEnabled.collectAsStateWithLifecycle()
    val musicLockscreenUnlockEnabled by viewModel.musicLockscreenUnlockEnabled.collectAsStateWithLifecycle()
    val customLockscreenEnabled by viewModel.customLockscreenEnabled.collectAsStateWithLifecycle()
    val depthWallpaperEnabled by viewModel.depthWallpaperEnabled.collectAsStateWithLifecycle()

    if (showAppSelectionDialog) {
        AppSelectionDialog(
            allApps = installedApps,
            initiallySelectedApps = disableSensorApps,
            onDismiss = { viewModel.onDismissAppSelectionDialog() },
            onConfirm = { selectedApps -> viewModel.onAppSelectionConfirmed(selectedApps) }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.top_bar_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_features_page),
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp) 
                    .padding(16.dp)
            )
            Text(
                text = stringResource(R.string.main_hint),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.main_ps),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            SettingItem(
                title = stringResource(R.string.status_bar_lyric_title),
                description = stringResource(R.string.status_bar_lyric_summary),
                isChecked = statusBarLyricEnabled, 
                onCheckedChange = { viewModel.onStatusBarLyricChanged(it) },
                enabled = true 
            )

            SettingItem(
                title = stringResource(R.string.music_lockscreen_title),
                description = stringResource(R.string.music_lockscreen_summary),
                isChecked = musicLockscreenEnabled,
                onCheckedChange = { viewModel.onMusicLockscreenChanged(it) }
            )

            SettingItem(
                title = stringResource(R.string.music_lockscreen_unlock_title),
                description = stringResource(R.string.music_lockscreen_unlock_summary),
                isChecked = musicLockscreenUnlockEnabled,
                onCheckedChange = { viewModel.onMusicLockscreenUnlockChanged(it) },
                enabled = musicLockscreenEnabled 
            )

            SettingItemWithClickableDescription(
                title = stringResource(R.string.custom_lockscreen_title),
                description = stringResource(R.string.custom_lockscreen_summary),
                isChecked = customLockscreenEnabled,
                onCheckedChange = { viewModel.onCustomLockscreenChanged(it) },
                onDescriptionClick = { viewModel.launchCustomLockscreenApp() },
                enabled = true
            )

            SettingItemWithClickableDescription(
                title = stringResource(R.string.depth_wallpaper_title),
                description = stringResource(R.string.depth_wallpaper_summary),
                isChecked = depthWallpaperEnabled,
                onCheckedChange = { viewModel.onDepthWallpaperChanged(it) },
                onDescriptionClick = { viewModel.launchDepthWallpaperApp() },
                enabled = true
            )

            SettingItem(
                title = stringResource(R.string.lockscreen_dim_title),
                description = stringResource(R.string.lockscreen_dim_summary),
                isChecked = lockscreenDimEnabled,
                onCheckedChange = { viewModel.onLockscreenDimChanged(it) }
            )

            SettingItem(
                title = stringResource(R.string.launcher_blur_title),
                description = stringResource(R.string.launcher_blur_summary),
                isChecked = launcherBlurEnabled,
                onCheckedChange = { viewModel.onLauncherBlurChanged(it) }
            )

            val descriptionText = if (disableSensorApps.isEmpty()) {
                stringResource(R.string.disable_sensor_summary_none)
            } else {
                stringResource(R.string.disable_sensor_summary_selected, disableSensorApps.size)
            }
            SettingItemWithClickableDescription(
                title = stringResource(R.string.disable_sensor_title),
                description = descriptionText,
                isChecked = disableSensorEnabled,
                onCheckedChange = { viewModel.onDisableSensorChanged(it) },
                onDescriptionClick = { viewModel.onShowAppSelectionDialog() },
                enabled = true
            )

            SettingItem(
                title = stringResource(R.string.screen_ocr_title),
                description = stringResource(R.string.screen_ocr_summary),
                isChecked = screenOcrEnabled,
                onCheckedChange = { viewModel.onScreenOcrChanged(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SliderSettingItem(
                title = stringResource(R.string.screen_ocr_high_title),
                value = screenOcrHighValue,
                onValueChange = { viewModel.onScreenOcrHighChanged(it) },
                onValueChangeFinished = { viewModel.onScreenOcrHighChangeFinished(screenOcrHighValue) },
                valueRange = 7f..20f,
                steps = 12,
                enabled = screenOcrEnabled
            )
        }
    }
}

@Composable
fun SettingItemWithClickableDescription(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDescriptionClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = enabled) { onDescriptionClick() }
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun AppSelectionDialog(
    allApps: List<SettingsViewModel.AppInfo>,
    initiallySelectedApps: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val selectedApps = rememberSaveable { mutableStateOf(initiallySelectedApps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_selection_dialog_title)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxHeight(0.7f)) {
                items(allApps, key = { it.packageName }) { appInfo ->
                    val isSelected = selectedApps.value.contains(appInfo.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val currentSelection = selectedApps.value.toMutableSet()
                                if (isSelected) {
                                    currentSelection.remove(appInfo.packageName)
                                } else {
                                    currentSelection.add(appInfo.packageName)
                                }
                                selectedApps.value = currentSelection
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = appInfo.appName, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedApps.value) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)) 
            .clickable(enabled = enabled) {
                onCheckedChange(!isChecked)
            }
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 3, 
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}


@Composable
fun SliderSettingItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value.roundToInt().toString(), 
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled
        )
    }
}