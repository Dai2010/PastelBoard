package dev.pastel.pastelboard

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.pastel.pastelboard.ui.control.ControlScreen
import dev.pastel.pastelboard.ui.home.HomeScreen
import dev.pastel.pastelboard.ui.settings.SettingsScreen
import dev.pastel.pastelboard.ui.theme.PastelBoardTheme

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

    LaunchedEffect(state.screen, state.selectedDevice) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.requestedOrientation = if (state.selectedDevice == null && state.screen == Screen.Home) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    PastelBoardTheme(palette = state.palette) {
        when (state.screen) {
            Screen.Home -> HomeScreen(
                state = state,
                onOpenSettings = viewModel::openSettings,
                onRefreshDevices = viewModel::refreshDevices,
                onConnect = viewModel::connect,
            )

            Screen.Control -> ControlScreen(
                state = state,
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
            )
        }
    }
}
