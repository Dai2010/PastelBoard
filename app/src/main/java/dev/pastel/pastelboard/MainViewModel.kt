package dev.pastel.pastelboard

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pastel.pastelboard.bluetooth.BluetoothHidController
import dev.pastel.pastelboard.bluetooth.HidModifier
import dev.pastel.pastelboard.bluetooth.HidUsage
import dev.pastel.pastelboard.bluetooth.KeyboardReport
import dev.pastel.pastelboard.bluetooth.PointerReport
import dev.pastel.pastelboard.model.DefaultPastelPalettes
import dev.pastel.pastelboard.model.DeviceTarget
import dev.pastel.pastelboard.model.PastelPalette
import dev.pastel.pastelboard.model.customPaletteFrom
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothController = BluetoothHidController(application)
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableUiState = kotlinx.coroutines.flow.MutableStateFlow(loadInitialState())

    val uiState: StateFlow<PastelBoardUiState> = mutableUiState
        .map { state -> state.copy(devices = bluetoothController.devices.value) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PastelBoardUiState(),
        )

    val permissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            emptyArray()
        }

    fun refreshDevices() {
        bluetoothController.refreshPairedDevices()
        mutableUiState.update { it.copy(devices = bluetoothController.devices.value) }
    }

    fun connect(device: DeviceTarget) {
        val connected = bluetoothController.connect(device)
        mutableUiState.update {
            it.copy(
                selectedDevice = if (connected) device else null,
                connectionStatus = if (connected) "已连接到 ${device.name}" else "连接需要蓝牙权限或设备配对",
                screen = if (connected) Screen.Control else Screen.Home,
            )
        }
    }

    fun disconnect() {
        mutableUiState.update {
            it.copy(
                selectedDevice = null,
                connectionStatus = "已断开，选择一个设备重新开始",
                screen = Screen.Home,
            )
        }
    }

    fun openSettings() {
        mutableUiState.update { it.copy(screen = Screen.Settings) }
    }

    fun backHome() {
        mutableUiState.update { it.copy(screen = if (it.selectedDevice == null) Screen.Home else Screen.Control) }
    }

    fun selectControlMode(mode: ControlMode) {
        mutableUiState.update { it.copy(controlMode = mode) }
    }

    fun selectPalette(palette: PastelPalette) {
        mutableUiState.update {
            val next = it.copy(
                palette = palette,
                hexInput = palette.primary.toHexString(),
                colorMessage = "已切换到「${palette.label}」",
            )
            persistAppearance(next)
            next
        }
    }

    fun updateHexInput(value: String) {
        val cleaned = value.trim().take(9)
        mutableUiState.update { it.copy(hexInput = cleaned, colorMessage = null) }
    }

    fun applyHexColor() {
        val color = parseHexColor(mutableUiState.value.hexInput)
        mutableUiState.update {
            if (color == null) {
                it.copy(colorMessage = "请输入 #RRGGBB 或 #AARRGGBB")
            } else {
                val next = it.copy(
                    palette = customPaletteFrom(color),
                    hexInput = color.toHexString(),
                    colorMessage = "自定义颜色已应用",
                )
                persistAppearance(next)
                next
            }
        }
    }

    fun updateColorChannel(channel: ColorChannel, value: Float) {
        val current = mutableUiState.value.palette.primary
        val nextColor = when (channel) {
            ColorChannel.Red -> current.copy(red = value)
            ColorChannel.Green -> current.copy(green = value)
            ColorChannel.Blue -> current.copy(blue = value)
        }

        mutableUiState.update {
            val next = it.copy(
                palette = customPaletteFrom(nextColor),
                hexInput = nextColor.toHexString(),
                colorMessage = "调色板颜色已更新",
            )
            persistAppearance(next)
            next
        }
    }

    fun setBackgroundImage(uri: String?) {
        mutableUiState.update {
            val next = it.copy(
                backgroundImageUri = uri,
                colorMessage = if (uri == null) "已恢复默认粉紫渐变背景" else "本地背景图片已应用",
            )
            persistAppearance(next)
            next
        }
    }

    fun updateBackgroundImageOpacity(value: Float) {
        mutableUiState.update {
            val next = it.copy(backgroundImageOpacity = value.coerceIn(0f, 1f))
            persistAppearance(next)
            next
        }
    }

    fun setKeySoundEnabled(enabled: Boolean) {
        mutableUiState.update {
            val next = it.copy(keySoundEnabled = enabled)
            persistAppearance(next)
            next
        }
    }

    fun pressKey(usage: HidUsage?, modifiers: Set<HidModifier> = emptySet()) {
        bluetoothController.sendKeyboard(KeyboardReport(usage, modifiers))
        bluetoothController.releaseKeyboard()
    }

    fun movePointer(deltaX: Int, deltaY: Int) {
        bluetoothController.sendPointer(PointerReport(deltaX = deltaX, deltaY = deltaY))
    }

    fun clickPointer(button: Int = 1) {
        bluetoothController.sendPointer(PointerReport(deltaX = 0, deltaY = 0, buttons = button))
        bluetoothController.sendPointer(PointerReport(deltaX = 0, deltaY = 0))
    }

    private fun loadInitialState(): PastelBoardUiState {
        val paletteId = preferences.getString(KEY_PALETTE_ID, DefaultPastelPalettes.first().id)
        val storedHex = preferences.getString(KEY_PRIMARY_HEX, null)
        val storedPalette = DefaultPastelPalettes.firstOrNull { it.id == paletteId }
        val palette = storedPalette
            ?: storedHex?.let(::parseHexColor)?.let(::customPaletteFrom)
            ?: DefaultPastelPalettes.first()

        return PastelBoardUiState(
            palette = palette,
            hexInput = palette.primary.toHexString(),
            backgroundImageUri = preferences.getString(KEY_BACKGROUND_URI, null),
            backgroundImageOpacity = preferences.getFloat(KEY_BACKGROUND_OPACITY, 0.34f),
            keySoundEnabled = preferences.getBoolean(KEY_KEY_SOUND_ENABLED, true),
        )
    }

    private fun persistAppearance(state: PastelBoardUiState) {
        val editor = preferences.edit()
            .putString(KEY_PALETTE_ID, state.palette.id)
            .putString(KEY_PRIMARY_HEX, state.palette.primary.toHexString())
            .putFloat(KEY_BACKGROUND_OPACITY, state.backgroundImageOpacity)
            .putBoolean(KEY_KEY_SOUND_ENABLED, state.keySoundEnabled)

        if (state.backgroundImageUri == null) {
            editor.remove(KEY_BACKGROUND_URI)
        } else {
            editor.putString(KEY_BACKGROUND_URI, state.backgroundImageUri)
        }

        editor.apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "pastel_board_preferences"
        const val KEY_PALETTE_ID = "palette_id"
        const val KEY_PRIMARY_HEX = "primary_hex"
        const val KEY_BACKGROUND_URI = "background_uri"
        const val KEY_BACKGROUND_OPACITY = "background_opacity"
        const val KEY_KEY_SOUND_ENABLED = "key_sound_enabled"
    }
}

data class PastelBoardUiState(
    val screen: Screen = Screen.Home,
    val devices: List<DeviceTarget> = emptyList(),
    val selectedDevice: DeviceTarget? = null,
    val connectionStatus: String = "选择要连接的蓝牙设备",
    val controlMode: ControlMode = ControlMode.Touchpad,
    val palette: PastelPalette = DefaultPastelPalettes.first(),
    val hexInput: String = DefaultPastelPalettes.first().primary.toHexString(),
    val colorMessage: String? = null,
    val backgroundImageUri: String? = null,
    val backgroundImageOpacity: Float = 0.34f,
    val keySoundEnabled: Boolean = true,
)

enum class Screen {
    Home,
    Control,
    Settings,
}

enum class ControlMode(val label: String) {
    Touchpad("触控板"),
    Keyboard("键盘"),
}

enum class ColorChannel(val label: String) {
    Red("红"),
    Green("绿"),
    Blue("蓝"),
}

fun Color.toHexString(): String {
    val redValue = (red * 255).toInt().coerceIn(0, 255)
    val greenValue = (green * 255).toInt().coerceIn(0, 255)
    val blueValue = (blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(redValue, greenValue, blueValue)
}

private fun parseHexColor(input: String): Color? {
    val normalized = input.trim().removePrefix("#")
    if (normalized.length != 6 && normalized.length != 8) return null
    val value = normalized.toLongOrNull(16) ?: return null
    return if (normalized.length == 6) {
        Color((0xFF000000 or value).toInt())
    } else {
        Color(value.toInt())
    }
}
