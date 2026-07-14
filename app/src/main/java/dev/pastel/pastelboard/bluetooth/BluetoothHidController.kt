package dev.pastel.pastelboard.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dev.pastel.pastelboard.model.DeviceTarget
import dev.pastel.pastelboard.model.DeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothHidController(context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val _devices = MutableStateFlow<List<DeviceTarget>>(emptyList())
    private val _connectionState = MutableStateFlow(HidConnectionState())
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var hidDevice: BluetoothHidDevice? = null
    @Volatile private var connectedDevice: BluetoothDevice? = null
    @Volatile private var pendingDevice: BluetoothDevice? = null
    @Volatile private var pendingTarget: DeviceTarget? = null
    @Volatile private var appRegistered = false
    private val appRegistrationPending = AtomicBoolean(false)

    val devices: StateFlow<List<DeviceTarget>> = _devices.asStateFlow()
    val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            if (!registerHidApp()) {
                failPendingConnection()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                appRegistered = false
                appRegistrationPending.set(false)
                connectedDevice = null
                pendingDevice = null
                pendingTarget = null
                _connectionState.value = HidConnectionState(status = HidConnectionStatus.Disconnected)
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    pendingDevice = null
                    val target = pendingTarget ?: device?.toTarget()
                    pendingTarget = null
                    _connectionState.value = HidConnectionState(
                        status = HidConnectionStatus.Connected,
                        target = target,
                    )
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = HidConnectionState(
                        status = HidConnectionStatus.Connecting,
                        target = pendingTarget,
                    )
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    val target = pendingTarget
                    pendingDevice = null
                    pendingTarget = null
                    _connectionState.value = HidConnectionState(
                        status = if (target == null) HidConnectionStatus.Disconnected else HidConnectionStatus.Failed,
                        target = target,
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            appRegistrationPending.set(false)
            if (registered) {
                _connectionState.value = HidConnectionState(
                    status = if (pendingDevice == null) HidConnectionStatus.Disconnected else HidConnectionStatus.Connecting,
                    target = pendingTarget,
                )
                pendingDevice?.let { device ->
                    if (hidDevice?.connect(device) != true) {
                        failPendingConnection()
                    }
                }
            } else {
                pendingDevice = null
                pendingTarget = null
                connectedDevice = null
                _connectionState.value = HidConnectionState(status = HidConnectionStatus.Failed)
            }
        }
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!hasBluetoothConnectPermission()) {
            _devices.value = emptyList()
            return
        }

        ensureHidProfile()

        _devices.value = adapter
            ?.bondedDevices
            ?.map { device -> device.toTarget() }
            ?.sortedWith(compareBy<DeviceTarget> { !it.isPaired }.thenBy { it.name })
            .orEmpty()
    }

    @SuppressLint("MissingPermission")
    fun connect(target: DeviceTarget): Boolean {
        if (!hasBluetoothConnectPermission() || target.address.isBlank()) return false
        val profileRequested = ensureHidProfile()
        val device = adapter?.bondedDevices?.firstOrNull { it.address == target.address } ?: return false
        if (connectedDevice?.address == device.address) {
            _connectionState.value = HidConnectionState(
                status = HidConnectionStatus.Connected,
                target = target,
            )
            return true
        }
        pendingDevice = device
        pendingTarget = target
        connectedDevice = null
        _connectionState.value = HidConnectionState(
            status = HidConnectionStatus.Connecting,
            target = target,
        )
        if (hidDevice == null) {
            if (profileRequested) return true
            failPendingConnection()
            return false
        }
        if (!appRegistered) {
            if (registerHidApp()) return true
            failPendingConnection()
            return false
        }
        return if (hidDevice?.connect(device) == true) {
            true
        } else {
            failPendingConnection(target)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        pendingDevice = null
        pendingTarget = null
        connectedDevice?.let { device -> hidDevice?.disconnect(device) }
        connectedDevice = null
        _connectionState.value = HidConnectionState(status = HidConnectionStatus.Disconnected)
    }

    fun sendKeyboard(report: KeyboardReport) {
        sendReport(reportId = KEYBOARD_REPORT_ID, data = report.bytes)
    }

    fun releaseKeyboard() {
        sendReport(reportId = KEYBOARD_REPORT_ID, data = KeyboardReport().bytes)
    }

    fun sendPointer(report: PointerReport) {
        sendReport(reportId = POINTER_REPORT_ID, data = report.bytes)
    }

    @SuppressLint("MissingPermission")
    private fun ensureHidProfile(): Boolean {
        if (hidDevice != null) return true
        if (!hasBluetoothConnectPermission()) return false
        return adapter?.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE) == true
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp(): Boolean {
        if (!hasBluetoothConnectPermission()) return false
        if (appRegistered || appRegistrationPending.get()) return true
        if (!appRegistrationPending.compareAndSet(false, true)) return true

        val sdp = BluetoothHidDeviceAppSdpSettings(
            "PastelBoard",
            "Pink-purple virtual touchpad and keyboard",
            "PastelBoard",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HID_DESCRIPTOR,
        )
        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX,
        )

        val registrationStarted = hidDevice?.registerApp(sdp, null, qos, executor, hidCallback) == true
        if (!registrationStarted) {
            appRegistrationPending.set(false)
        }
        return registrationStarted
    }

    private fun failPendingConnection(target: DeviceTarget? = pendingTarget) {
        pendingDevice = null
        pendingTarget = null
        connectedDevice = null
        _connectionState.value = HidConnectionState(
            status = HidConnectionStatus.Failed,
            target = target,
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendReport(reportId: Int, data: ByteArray) {
        val device = connectedDevice ?: return
        if (!hasBluetoothConnectPermission()) return
        hidDevice?.sendReport(device, reportId, data)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toTarget(): DeviceTarget {
        return DeviceTarget(
            name = name ?: "未命名设备",
            address = address.orEmpty(),
            type = bluetoothClass?.toDeviceType() ?: DeviceType.Unknown,
            isPaired = bondState == BluetoothDevice.BOND_BONDED,
        )
    }

    private fun BluetoothClass.toDeviceType(): DeviceType {
        return when (majorDeviceClass) {
            BluetoothClass.Device.Major.COMPUTER -> DeviceType.Laptop
            BluetoothClass.Device.Major.PHONE -> DeviceType.Tablet
            else -> DeviceType.Unknown
        }
    }

    private companion object {
        const val KEYBOARD_REPORT_ID = 1
        const val POINTER_REPORT_ID = 2

        val HID_DESCRIPTOR = byteArrayOf(
            0x05, 0x01,
            0x09, 0x06,
            0xA1.toByte(), 0x01,
            0x85.toByte(), KEYBOARD_REPORT_ID.toByte(),
            0x05, 0x07,
            0x19, 0xE0.toByte(),
            0x29, 0xE7.toByte(),
            0x15, 0x00,
            0x25, 0x01,
            0x75, 0x01,
            0x95.toByte(), 0x08,
            0x81.toByte(), 0x02,
            0x95.toByte(), 0x01,
            0x75, 0x08,
            0x81.toByte(), 0x01,
            0x95.toByte(), 0x06,
            0x75, 0x08,
            0x15, 0x00,
            0x25, 0x65,
            0x05, 0x07,
            0x19, 0x00,
            0x29, 0x65,
            0x81.toByte(), 0x00,
            0xC0.toByte(),
            0x05, 0x01,
            0x09, 0x02,
            0xA1.toByte(), 0x01,
            0x85.toByte(), POINTER_REPORT_ID.toByte(),
            0x09, 0x01,
            0xA1.toByte(), 0x00,
            0x05, 0x09,
            0x19, 0x01,
            0x29, 0x03,
            0x15, 0x00,
            0x25, 0x01,
            0x95.toByte(), 0x03,
            0x75, 0x01,
            0x81.toByte(), 0x02,
            0x95.toByte(), 0x01,
            0x75, 0x05,
            0x81.toByte(), 0x01,
            0x05, 0x01,
            0x09, 0x30,
            0x09, 0x31,
            0x09, 0x38,
            0x15, 0x81.toByte(),
            0x25, 0x7F,
            0x75, 0x08,
            0x95.toByte(), 0x03,
            0x81.toByte(), 0x06,
            0xC0.toByte(),
            0xC0.toByte(),
        )
    }
}

data class HidConnectionState(
    val status: HidConnectionStatus = HidConnectionStatus.Disconnected,
    val target: DeviceTarget? = null,
)

enum class HidConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Failed,
}
