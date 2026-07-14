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

class BluetoothHidController(context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val _devices = MutableStateFlow<List<DeviceTarget>>(emptyList())
    private val executor = Executors.newSingleThreadExecutor()
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    val devices: StateFlow<List<DeviceTarget>> = _devices.asStateFlow()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            registerHidApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                connectedDevice = null
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            connectedDevice = if (state == BluetoothProfile.STATE_CONNECTED) device else null
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
        ensureHidProfile()
        val device = adapter?.bondedDevices?.firstOrNull { it.address == target.address } ?: return false
        connectedDevice = device
        return hidDevice?.connect(device) ?: true
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
    private fun ensureHidProfile() {
        if (hidDevice != null || !hasBluetoothConnectPermission()) return
        adapter?.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE)
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        if (!hasBluetoothConnectPermission()) return

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

        hidDevice?.registerApp(sdp, null, qos, executor, hidCallback)
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
