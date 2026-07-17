package dev.pastel.pastelboard

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pastel.pastelboard.bluetooth.BluetoothHidController
import dev.pastel.pastelboard.bluetooth.HidConnectionStatus
import dev.pastel.pastelboard.bluetooth.HidDiagnostics
import dev.pastel.pastelboard.bluetooth.HidModifier
import dev.pastel.pastelboard.bluetooth.HidUsage
import dev.pastel.pastelboard.bluetooth.KeyboardReport
import dev.pastel.pastelboard.bluetooth.PointerReport
import dev.pastel.pastelboard.model.DefaultPastelPalettes
import dev.pastel.pastelboard.model.DeviceTarget
import dev.pastel.pastelboard.model.PastelPalette
import dev.pastel.pastelboard.model.customPaletteFrom
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableUiState = kotlinx.coroutines.flow.MutableStateFlow(loadInitialState())
    private val bluetoothController = BluetoothHidController(application) { message ->
        appendLog(category = "HID", message = message)
    }
    private val keyPressMutex = Mutex()
    private var pointerMoveLogCounter = 0

    val uiState: StateFlow<PastelBoardUiState> = mutableUiState
        .combine(bluetoothController.devices) { state, devices ->
            state.copy(devices = devices)
        }
        .combine(bluetoothController.diagnostics) { state, diagnostics ->
            state.copy(hidDiagnostics = diagnostics)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PastelBoardUiState(),
        )

    init {
        appendLog("APP", "启动 ${BuildConfig.VERSION_NAME} 内部测试版")
        viewModelScope.launch {
            bluetoothController.connectionState.collect { connection ->
                applyConnectionState(connection.status, connection.target)
            }
        }
    }

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
        appendLog("BT", "刷新已配对设备")
        bluetoothController.refreshPairedDevices()
        mutableUiState.update { it.copy(devices = bluetoothController.devices.value) }
        appendLog("BT", "设备数量 ${bluetoothController.devices.value.size}")
    }

    fun connect(device: DeviceTarget) {
        appendLog("BT", "请求连接 ${device.name} ${device.address}")
        val connecting = bluetoothController.connect(device)
        mutableUiState.update {
            it.copy(
                selectedDevice = if (connecting) device else null,
                connectionStatus = if (connecting) "正在连接 ${device.name}…" else "连接需要蓝牙权限或设备配对",
                screen = Screen.Home,
            )
        }
    }

    fun disconnect() {
        appendLog("BT", "请求断开连接")
        bluetoothController.disconnect()
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

    fun openLogs() {
        mutableUiState.update { it.copy(screen = Screen.Logs) }
    }

    fun openKeyboardTest() {
        mutableUiState.update { it.copy(screen = Screen.KeyboardTest) }
    }

    fun openControl() {
        mutableUiState.update { it.copy(screen = if (it.selectedDevice == null) Screen.KeyboardTest else Screen.Control) }
    }

    fun openHome() {
        mutableUiState.update { it.copy(screen = Screen.Home) }
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
                keyColor = palette.keyTop,
                keyHexInput = palette.keyTop.toHexString(),
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

    fun setKeySoundUri(uri: String?) {
        mutableUiState.update {
            val next = it.copy(
                keySoundUri = uri,
                keySoundMessage = if (uri == null) "已恢复默认 Flick 感按键音" else "自定义按键音已应用",
            )
            persistAppearance(next)
            next
        }
    }

    fun updateKeyHexInput(value: String) {
        val cleaned = value.trim().take(9)
        mutableUiState.update { it.copy(keyHexInput = cleaned, keyColorMessage = null) }
    }

    fun applyKeyHexColor() {
        val color = parseHexColor(mutableUiState.value.keyHexInput)
        mutableUiState.update {
            if (color == null) {
                it.copy(keyColorMessage = "请输入 #RRGGBB 或 #AARRGGBB")
            } else {
                val next = it.copy(
                    keyColor = color,
                    keyHexInput = color.toHexString(),
                    keyColorMessage = "按键颜色已应用",
                )
                persistAppearance(next)
                next
            }
        }
    }

    fun updateKeyColorChannel(channel: ColorChannel, value: Float) {
        val current = mutableUiState.value.keyColor
        val nextColor = when (channel) {
            ColorChannel.Red -> current.copy(red = value)
            ColorChannel.Green -> current.copy(green = value)
            ColorChannel.Blue -> current.copy(blue = value)
        }

        mutableUiState.update {
            val next = it.copy(
                keyColor = nextColor,
                keyHexInput = nextColor.toHexString(),
                keyColorMessage = "按键颜色已更新",
            )
            persistAppearance(next)
            next
        }
    }

    fun setLaunchLandscapeEnabled(enabled: Boolean) {
        mutableUiState.update {
            val next = it.copy(launchLandscapeEnabled = enabled)
            persistAppearance(next)
            next
        }
    }

    fun pressKey(usage: HidUsage?, modifiers: Set<HidModifier> = emptySet()) {
        appendLog("KEY", "按键 usage=${usage?.name ?: "None"} modifiers=${modifiers.joinToString { it.name }.ifBlank { "None" }}")
        viewModelScope.launch {
            keyPressMutex.withLock {
                bluetoothController.sendKeyboard(KeyboardReport(usage, modifiers))
                delay(KEY_PRESS_DURATION_MS)
                bluetoothController.releaseKeyboard()
            }
        }
    }

    fun movePointer(deltaX: Int, deltaY: Int) {
        pointerMoveLogCounter += 1
        if (pointerMoveLogCounter % POINTER_LOG_INTERVAL == 0) {
            appendLog("PTR", "移动 dx=$deltaX dy=$deltaY")
        }
        var remainingX = deltaX
        var remainingY = deltaY
        while (remainingX != 0 || remainingY != 0) {
            val stepX = remainingX.coerceIn(-127, 127)
            val stepY = remainingY.coerceIn(-127, 127)
            bluetoothController.sendPointer(PointerReport(deltaX = stepX, deltaY = stepY))
            remainingX -= stepX
            remainingY -= stepY
        }
    }

    fun clickPointer(button: Int = 1) {
        appendLog("PTR", "点击按钮 $button")
        bluetoothController.sendPointer(PointerReport(deltaX = 0, deltaY = 0, buttons = button))
        bluetoothController.sendPointer(PointerReport(deltaX = 0, deltaY = 0))
    }

    fun clearDiagnosticLogs() {
        mutableUiState.update { it.copy(diagnosticLogs = emptyList(), logExportMessage = null) }
        appendLog("LOG", "日志已清空")
    }

    fun setLogExportMessage(message: String?) {
        mutableUiState.update { it.copy(logExportMessage = message) }
        message?.let { appendLog("LOG", it) }
    }

    fun createLogExportFileName(): String {
        val timestamp = FILE_TIMESTAMP_FORMAT.format(Date())
        return "PastelBoard-internal-${BuildConfig.VERSION_NAME}-$timestamp.txt"
    }

    fun createDiagnosticLogText(): String {
        val state = mutableUiState.value
        val diagnostics = bluetoothController.diagnostics.value
        return buildString {
            appendLine("PastelBoard 内部测试版日志")
            appendLine("版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("导出时间: ${LOG_TIMESTAMP_FORMAT.format(Date())}")
            appendLine("连接状态: ${state.connectionStatus}")
            appendLine("当前设备: ${state.selectedDevice?.name ?: "无"}")
            appendLine("HID profile: ${diagnostics.profileReady}")
            appendLine("HID app 已注册: ${diagnostics.appRegistered}")
            appendLine("plugged device: ${diagnostics.pluggedDevice ?: "无"}")
            appendLine("connected device: ${diagnostics.connectedDevice ?: "无"}")
            appendLine("最近键盘报告: ${diagnostics.lastKeyboardReport ?: "无"}")
            appendLine("最近指针报告: ${diagnostics.lastPointerReport ?: "无"}")
            appendLine("最近失败原因: ${diagnostics.lastFailure ?: "无"}")
            appendLine("日志条数: ${state.diagnosticLogs.size}")
            appendLine("---")
            state.diagnosticLogs.forEach { entry ->
                appendLine("${entry.timestamp} [${entry.category}] ${entry.message}")
            }
        }
    }

    private fun applyConnectionState(status: HidConnectionStatus, target: DeviceTarget?) {
        appendLog("BT", "连接状态 $status ${target?.name.orEmpty()}")
        mutableUiState.update { state ->
            when (status) {
                HidConnectionStatus.Connecting -> state.copy(
                    selectedDevice = target,
                    connectionStatus = target?.let { "正在连接 ${it.name}…" } ?: "正在准备蓝牙 HID…",
                )

                HidConnectionStatus.Connected -> state.copy(
                    selectedDevice = target,
                    connectionStatus = target?.let { "已连接到 ${it.name}" } ?: "蓝牙 HID 已连接",
                    screen = Screen.Control,
                )

                HidConnectionStatus.Failed -> state.copy(
                    selectedDevice = null,
                    connectionStatus = target?.let { "无法连接 ${it.name}，请确认目标设备允许 HID 输入" } ?: "蓝牙 HID 注册或连接失败",
                    screen = Screen.Home,
                )

                HidConnectionStatus.Disconnected -> state.copy(
                    selectedDevice = null,
                    connectionStatus = "HID 已断开，选择一个设备重新开始",
                )
            }
        }
    }

    private fun appendLog(category: String, message: String) {
        val entry = DiagnosticLogEntry(
            timestamp = LOG_TIMESTAMP_FORMAT.format(Date()),
            category = category,
            message = message,
        )
        mutableUiState.update { state ->
            state.copy(
                diagnosticLogs = (state.diagnosticLogs + entry).takeLast(MAX_LOG_ENTRIES),
            )
        }
    }

    private fun loadInitialState(): PastelBoardUiState {
        val paletteId = preferences.getString(KEY_PALETTE_ID, DefaultPastelPalettes.first().id)
        val storedHex = preferences.getString(KEY_PRIMARY_HEX, null)
        val storedPalette = DefaultPastelPalettes.firstOrNull { it.id == paletteId }
        val palette = storedPalette
            ?: storedHex?.let(::parseHexColor)?.let(::customPaletteFrom)
            ?: DefaultPastelPalettes.first()
        val storedKeyColor = preferences.getString(KEY_KEY_COLOR_HEX, null)
        val keyColor = storedKeyColor?.let(::parseHexColor) ?: palette.keyTop

        return PastelBoardUiState(
            palette = palette,
            hexInput = palette.primary.toHexString(),
            keyColor = keyColor,
            keyHexInput = keyColor.toHexString(),
            backgroundImageUri = preferences.getString(KEY_BACKGROUND_URI, null),
            backgroundImageOpacity = preferences.getFloat(KEY_BACKGROUND_OPACITY, 0.34f),
            keySoundEnabled = preferences.getBoolean(KEY_KEY_SOUND_ENABLED, true),
            keySoundUri = preferences.getString(KEY_KEY_SOUND_URI, null),
            launchLandscapeEnabled = preferences.getBoolean(KEY_LAUNCH_LANDSCAPE_ENABLED, false),
        )
    }

    private fun persistAppearance(state: PastelBoardUiState) {
        val editor = preferences.edit()
            .putString(KEY_PALETTE_ID, state.palette.id)
            .putString(KEY_PRIMARY_HEX, state.palette.primary.toHexString())
            .putString(KEY_KEY_COLOR_HEX, state.keyColor.toHexString())
            .putFloat(KEY_BACKGROUND_OPACITY, state.backgroundImageOpacity)
            .putBoolean(KEY_KEY_SOUND_ENABLED, state.keySoundEnabled)
            .putBoolean(KEY_LAUNCH_LANDSCAPE_ENABLED, state.launchLandscapeEnabled)

        if (state.backgroundImageUri == null) {
            editor.remove(KEY_BACKGROUND_URI)
        } else {
            editor.putString(KEY_BACKGROUND_URI, state.backgroundImageUri)
        }

        if (state.keySoundUri == null) {
            editor.remove(KEY_KEY_SOUND_URI)
        } else {
            editor.putString(KEY_KEY_SOUND_URI, state.keySoundUri)
        }

        editor.apply()
    }

    private companion object {
        const val KEY_PRESS_DURATION_MS = 50L
        const val MAX_LOG_ENTRIES = 500
        const val POINTER_LOG_INTERVAL = 8
        const val PREFERENCES_NAME = "pastel_board_preferences"
        const val KEY_PALETTE_ID = "palette_id"
        const val KEY_PRIMARY_HEX = "primary_hex"
        const val KEY_KEY_COLOR_HEX = "key_color_hex"
        const val KEY_BACKGROUND_URI = "background_uri"
        const val KEY_BACKGROUND_OPACITY = "background_opacity"
        const val KEY_KEY_SOUND_ENABLED = "key_sound_enabled"
        const val KEY_KEY_SOUND_URI = "key_sound_uri"
        const val KEY_LAUNCH_LANDSCAPE_ENABLED = "launch_landscape_enabled"
        val LOG_TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val FILE_TIMESTAMP_FORMAT = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    }
}

data class DiagnosticLogEntry(
    val timestamp: String,
    val category: String,
    val message: String,
)

data class PastelBoardUiState(
    val screen: Screen = Screen.Home,
    val devices: List<DeviceTarget> = emptyList(),
    val selectedDevice: DeviceTarget? = null,
    val connectionStatus: String = "选择要连接的蓝牙设备",
    val controlMode: ControlMode = ControlMode.Touchpad,
    val palette: PastelPalette = DefaultPastelPalettes.first(),
    val hexInput: String = DefaultPastelPalettes.first().primary.toHexString(),
    val colorMessage: String? = null,
    val keyColor: Color = DefaultPastelPalettes.first().keyTop,
    val keyHexInput: String = DefaultPastelPalettes.first().keyTop.toHexString(),
    val keyColorMessage: String? = null,
    val backgroundImageUri: String? = null,
    val backgroundImageOpacity: Float = 0.34f,
    val keySoundEnabled: Boolean = true,
    val keySoundUri: String? = null,
    val keySoundMessage: String? = null,
    val launchLandscapeEnabled: Boolean = false,
    val diagnosticLogs: List<DiagnosticLogEntry> = emptyList(),
    val logExportMessage: String? = null,
    val hidDiagnostics: HidDiagnostics = HidDiagnostics(),
)

enum class Screen {
    Home,
    Control,
    Settings,
    KeyboardTest,
    Logs,
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
