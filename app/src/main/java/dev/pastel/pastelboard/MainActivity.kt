package dev.pastel.pastelboard

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.ui.control.ControlScreen
import dev.pastel.pastelboard.ui.home.HomeScreen
import dev.pastel.pastelboard.ui.settings.SettingsScreen
import dev.pastel.pastelboard.ui.test.KeyboardTestScreen
import dev.pastel.pastelboard.ui.theme.PastelBoardTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PastelBoardApp(viewModel = viewModel)
        }
    }
}

@Composable
private fun PastelBoardApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { viewModel.refreshDevices() },
    )
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setBackgroundImage(uri.toString())
        },
    )
    val keySoundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setKeySoundUri(uri.toString())
        },
    )

    fun openDrawer() {
        coroutineScope.launch { drawerState.open() }
    }

    fun navigate(action: () -> Unit) {
        action()
        coroutineScope.launch { drawerState.close() }
    }

    LaunchedEffect(Unit) {
        if (viewModel.permissions.isNotEmpty()) {
            permissionLauncher.launch(viewModel.permissions)
        } else {
            viewModel.refreshDevices()
        }
    }

    LaunchedEffect(context) {
        viewModel.refreshDevices()
    }

    LaunchedEffect(state.screen, state.selectedDevice, state.launchLandscapeEnabled) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.requestedOrientation = if (state.launchLandscapeEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (state.selectedDevice == null && state.screen == Screen.Home) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    PastelBoardTheme(palette = state.palette) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                PastelDrawerContent(
                    state = state,
                    onHome = { navigate(viewModel::openHome) },
                    onControl = { navigate(viewModel::openControl) },
                    onKeyboardTest = { navigate(viewModel::openKeyboardTest) },
                    onSettings = { navigate(viewModel::openSettings) },
                    onLaunchLandscapeChanged = viewModel::setLaunchLandscapeEnabled,
                )
            },
        ) {
            when (state.screen) {
                Screen.Home -> HomeScreen(
                    state = state,
                    onOpenMenu = ::openDrawer,
                    onRefreshDevices = viewModel::refreshDevices,
                    onConnect = viewModel::connect,
                )

                Screen.Control -> ControlScreen(
                    state = state,
                    onOpenMenu = ::openDrawer,
                    onSelectMode = viewModel::selectControlMode,
                    onOpenSettings = viewModel::openSettings,
                    onDisconnect = viewModel::disconnect,
                    onMovePointer = viewModel::movePointer,
                    onClickPointer = viewModel::clickPointer,
                    onPressKey = viewModel::pressKey,
                )

                Screen.Settings -> SettingsScreen(
                    state = state,
                    onBack = viewModel::backHome,
                    onSelectPalette = viewModel::selectPalette,
                    onHexChanged = viewModel::updateHexInput,
                    onApplyHex = viewModel::applyHexColor,
                    onColorChannelChanged = viewModel::updateColorChannel,
                    onPickBackgroundImage = { backgroundPicker.launch(arrayOf("image/*")) },
                    onClearBackgroundImage = { viewModel.setBackgroundImage(null) },
                    onBackgroundOpacityChanged = viewModel::updateBackgroundImageOpacity,
                    onKeySoundEnabledChanged = viewModel::setKeySoundEnabled,
                    onPickKeySound = { keySoundPicker.launch(arrayOf("audio/*")) },
                    onClearKeySound = { viewModel.setKeySoundUri(null) },
                    onKeyHexChanged = viewModel::updateKeyHexInput,
                    onApplyKeyHex = viewModel::applyKeyHexColor,
                    onKeyColorChannelChanged = viewModel::updateKeyColorChannel,
                    onLaunchLandscapeChanged = viewModel::setLaunchLandscapeEnabled,
                )

                Screen.KeyboardTest -> KeyboardTestScreen(
                    state = state,
                    onBack = viewModel::backHome,
                    onOpenMenu = ::openDrawer,
                    onKeySoundEnabledChanged = viewModel::setKeySoundEnabled,
                    onPickKeySound = { keySoundPicker.launch(arrayOf("audio/*")) },
                    onClearKeySound = { viewModel.setKeySoundUri(null) },
                    onKeyHexChanged = viewModel::updateKeyHexInput,
                    onApplyKeyHex = viewModel::applyKeyHexColor,
                    onKeyColorChannelChanged = viewModel::updateKeyColorChannel,
                )
            }
        }
    }
}

@Composable
private fun PastelDrawerContent(
    state: PastelBoardUiState,
    onHome: () -> Unit,
    onControl: () -> Unit,
    onKeyboardTest: () -> Unit,
    onSettings: () -> Unit,
    onLaunchLandscapeChanged: (Boolean) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PastelBoard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "侧边菜单 · v0.2.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NavigationDrawerItem(
            label = { Text("主页 / 设备选择") },
            selected = state.screen == Screen.Home,
            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
            onClick = onHome,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        NavigationDrawerItem(
            label = { Text("控制界面") },
            selected = state.screen == Screen.Control,
            icon = { Icon(Icons.Rounded.TouchApp, contentDescription = null) },
            onClick = onControl,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        NavigationDrawerItem(
            label = { Text("键盘测试页") },
            selected = state.screen == Screen.KeyboardTest,
            icon = { Icon(Icons.Rounded.Keyboard, contentDescription = null) },
            onClick = onKeyboardTest,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        NavigationDrawerItem(
            label = { Text("设置") },
            selected = state.screen == Screen.Settings,
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            onClick = onSettings,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("开启即横屏") },
            selected = state.launchLandscapeEnabled,
            icon = { Icon(Icons.Rounded.ScreenRotation, contentDescription = null) },
            badge = {
                Switch(
                    checked = state.launchLandscapeEnabled,
                    onCheckedChange = onLaunchLandscapeChanged,
                )
            },
            onClick = { onLaunchLandscapeChanged(!state.launchLandscapeEnabled) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
