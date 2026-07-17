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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class BluetoothHidController(
    context: Context,
    private val logger: (String) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val _devices = MutableStateFlow<List<DeviceTarget>>(emptyList())
    private val _connectionState = MutableStateFlow(HidConnectionState())
    private val _diagnostics = MutableStateFlow(HidDiagnostics())
    private val executor = Executors.newSingleThreadExecutor()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var hidDevice: BluetoothHidDevice? = null
    @Volatile private var connectedDevice: BluetoothDevice? = null
    @Volatile private var pendingDevice: BluetoothDevice? = null
    @Volatile private var pendingTarget: DeviceTarget? = null
    @Volatile private var pendingRequestId: Long? = null
    @Volatile private var connectCallRequestId: Long? = null
    @Volatile private var appRegistered = false
    @Volatile private var suppressPluggedDevice = false
    @Volatile private var profileRequestTimeout: ScheduledFuture<*>? = null
    @Volatile private var appRegistrationTimeout: ScheduledFuture<*>? = null
    @Volatile private var connectionTimeout: ScheduledFuture<*>? = null
    private val appRegistrationPending = AtomicBoolean(false)
    private val profileRequestPending = AtomicBoolean(false)
    private val requestCounter = AtomicLong(0)
    private val pointerReportLogCounter = AtomicLong(0)

    val devices: StateFlow<List<DeviceTarget>> = _devices.asStateFlow()
    val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()
    val diagnostics: StateFlow<HidDiagnostics> = _diagnostics.asStateFlow()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            profileRequestPending.set(false)
            profileRequestTimeout?.cancel(false)
            profileRequestTimeout = null
            hidDevice = proxy as? BluetoothHidDevice
            updateDiagnostics { it.copy(profileReady = hidDevice != null) }
            logEvent("PROFILE_SERVICE_CONNECTED", detail = "proxy=${hidDevice != null}")
            if (hidDevice == null || !registerHidApp()) {
                failPendingConnection(reason = "REGISTER_APP_START_FAILED")
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            logEvent("PROFILE_SERVICE_DISCONNECTED")
            profileRequestPending.set(false)
            profileRequestTimeout?.cancel(false)
            profileRequestTimeout = null
            appRegistrationTimeout?.cancel(false)
            appRegistrationTimeout = null
            connectionTimeout?.cancel(false)
            connectionTimeout = null
            hidDevice = null
            appRegistered = false
            appRegistrationPending.set(false)
            connectedDevice = null
            pendingDevice = null
            pendingTarget = null
            pendingRequestId = null
            connectCallRequestId = null
            updateDiagnostics {
                it.copy(
                    profileReady = false,
                    appRegistered = false,
                    pluggedDevice = null,
                    connectedDevice = null,
                    lastFailure = "PROFILE_SERVICE_DISCONNECTED",
                )
            }
            _connectionState.value = HidConnectionState(status = HidConnectionStatus.Disconnected)
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val stateName = state.toConnectionStateName()
            logEvent(
                "CONNECTION_STATE_$stateName",
                device = device,
                detail = "accepted=${isCurrentCallbackDevice(device)}",
            )
            if (!isCurrentCallbackDevice(device)) {
                logEvent("CONNECTION_STATE_IGNORED_STALE", device = device)
                return
            }
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    pendingDevice = null
                    val target = pendingTarget ?: device?.toTarget()
                    pendingTarget = null
                    pendingRequestId = null
                    connectCallRequestId = null
                    connectionTimeout?.cancel(false)
                    connectionTimeout = null
                    updateDiagnostics {
                        it.copy(
                            pluggedDevice = device?.safeAddress(),
                            connectedDevice = device?.safeAddress(),
                            lastFailure = null,
                        )
                    }
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
                    pendingRequestId = null
                    connectCallRequestId = null
                    connectionTimeout?.cancel(false)
                    connectionTimeout = null
                    updateDiagnostics {
                        it.copy(
                            pluggedDevice = null,
                            connectedDevice = null,
                            lastFailure = if (target == null) null else "CONNECTION_STATE_DISCONNECTED",
                        )
                    }
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
            appRegistrationTimeout?.cancel(false)
            appRegistrationTimeout = null
            updateDiagnostics {
                it.copy(
                    appRegistered = registered,
                    profileReady = hidDevice != null,
                    pluggedDevice = if (registered && pluggedDevice != null &&
                        !suppressPluggedDevice && isCompatiblePluggedDevice(pluggedDevice)
                    ) {
                        pluggedDevice.safeAddress()
                    } else {
                        null
                    },
                    lastFailure = if (registered) null else "APP_STATUS_UNREGISTERED",
                )
            }
            if (!registered) {
                logEvent("APP_STATUS_UNREGISTERED", device = pluggedDevice)
                failPendingConnection(reason = "APP_STATUS_UNREGISTERED")
                return
            }

            logEvent("APP_STATUS_REGISTERED", device = pluggedDevice)
            if (pluggedDevice != null && !suppressPluggedDevice && isCompatiblePluggedDevice(pluggedDevice)) {
                connectedDevice = pluggedDevice
                pendingDevice = null
                val target = pendingTarget ?: pluggedDevice.toTarget()
                pendingTarget = null
                pendingRequestId = null
                connectCallRequestId = null
                connectionTimeout?.cancel(false)
                connectionTimeout = null
                logEvent("APP_STATUS_PLUGGED_DEVICE", device = pluggedDevice)
                updateDiagnostics {
                    it.copy(
                        pluggedDevice = pluggedDevice.safeAddress(),
                        connectedDevice = pluggedDevice.safeAddress(),
                        lastFailure = null,
                    )
                }
                _connectionState.value = HidConnectionState(
                    status = HidConnectionStatus.Connected,
                    target = target,
                )
                return
            }

            if (pluggedDevice != null) {
                logEvent("APP_STATUS_PLUGGED_DEVICE_IGNORED", device = pluggedDevice)
            }
            if (pendingDevice == null) {
                _connectionState.value = HidConnectionState(status = HidConnectionStatus.Disconnected)
            } else {
                _connectionState.value = HidConnectionState(
                    status = HidConnectionStatus.Connecting,
                    target = pendingTarget,
                )
                connectToPendingDevice(pendingRequestId)
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
            logEvent("REPORT_DROPPED_PERMISSION", detail = "operation=REFRESH_DEVICES")
            _devices.value = emptyList()
            return
        }

        val profileRequested = ensureHidProfile()
        logEvent("PROFILE_REQUEST_RESULT", apiResult = profileRequested, detail = "source=refresh")
        _devices.value = adapter
            ?.bondedDevices
            ?.map { device -> device.toTarget() }
            ?.sortedWith(compareBy<DeviceTarget> { !it.isPaired }.thenBy { it.name })
            .orEmpty()
        logEvent("DEVICES_REFRESHED", detail = "count=${_devices.value.size}")
    }

    @SuppressLint("MissingPermission")
    fun connect(target: DeviceTarget): Boolean {
        val hasPermission = hasBluetoothConnectPermission()
        if (!hasPermission || target.address.isBlank()) {
            logEvent(
                "CONNECT_RESULT",
                deviceId = target.address,
                apiResult = false,
                detail = "permission=$hasPermission addressBlank=${target.address.isBlank()}",
            )
            failPendingConnection(target = target, reason = "PERMISSION_OR_ADDRESS_INVALID")
            return false
        }

        suppressPluggedDevice = false
        logEvent("CONNECT_REQUEST", deviceId = target.address, detail = "name=${target.name}")
        val profileRequested = ensureHidProfile()
        val device = adapter?.bondedDevices?.firstOrNull { it.address == target.address }
        if (device == null) {
            logEvent("CONNECT_RESULT", deviceId = target.address, apiResult = false, detail = "reason=NOT_PAIRED")
            failPendingConnection(target = target, reason = "NOT_PAIRED")
            return false
        }
        if (connectedDevice?.address == device.address) {
            logEvent("CONNECT_RESULT", device = device, apiResult = true, detail = "alreadyConnected=true")
            _connectionState.value = HidConnectionState(
                status = HidConnectionStatus.Connected,
                target = target,
            )
            return true
        }

        if (connectedDevice != null && !isSameDevice(connectedDevice, device)) {
            disconnectDevice(connectedDevice)
            connectedDevice = null
        }
        if (pendingDevice != null && !isSameDevice(pendingDevice, device)) {
            disconnectDevice(pendingDevice)
        }

        val requestId = requestCounter.incrementAndGet()
        pendingRequestId = requestId
        connectCallRequestId = null
        pendingDevice = device
        pendingTarget = target
        updateDiagnostics { it.copy(connectedDevice = null, lastFailure = null) }
        _connectionState.value = HidConnectionState(
            status = HidConnectionStatus.Connecting,
            target = target,
        )

        if (hidDevice == null) {
            if (!profileRequested) {
                logEvent("CONNECT_RESULT", device = device, apiResult = false, detail = "reason=PROFILE_UNAVAILABLE")
                failPendingConnection(reason = "PROFILE_UNAVAILABLE")
                return false
            }
            scheduleConnectionTimeout(requestId)
            return true
        }
        if (!appRegistered) {
            if (!registerHidApp()) {
                logEvent("CONNECT_RESULT", device = device, apiResult = false, detail = "reason=REGISTER_APP_FAILED")
                failPendingConnection(reason = "REGISTER_APP_FAILED")
                return false
            }
            scheduleConnectionTimeout(requestId)
            return true
        }
        return connectToPendingDevice(requestId)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        logEvent("DISCONNECT_REQUEST", device = connectedDevice ?: pendingDevice)
        suppressPluggedDevice = true
        requestCounter.incrementAndGet()
        listOfNotNull(connectedDevice, pendingDevice)
            .distinctBy { it.address }
            .forEach(::disconnectDevice)
        pendingDevice = null
        pendingTarget = null
        pendingRequestId = null
        connectCallRequestId = null
        connectedDevice = null
        connectionTimeout?.cancel(false)
        connectionTimeout = null
        updateDiagnostics { it.copy(pluggedDevice = null, connectedDevice = null) }
        _connectionState.value = HidConnectionState(status = HidConnectionStatus.Disconnected)
    }

    fun sendKeyboard(report: KeyboardReport) {
        sendReport(reportId = HidProtocol.KEYBOARD_REPORT_ID, data = report.bytes)
    }

    fun releaseKeyboard() {
        sendReport(reportId = HidProtocol.KEYBOARD_REPORT_ID, data = KeyboardReport().bytes)
    }

    fun sendPointer(report: PointerReport) {
        sendReport(reportId = HidProtocol.POINTER_REPORT_ID, data = report.bytes)
    }

    @SuppressLint("MissingPermission")
    private fun ensureHidProfile(): Boolean {
        if (hidDevice != null) return true
        if (!hasBluetoothConnectPermission()) return false
        if (!profileRequestPending.compareAndSet(false, true)) {
            logEvent("PROFILE_REQUEST_REUSED")
            return true
        }

        logEvent("PROFILE_REQUEST_START")
        val requested = adapter?.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE) == true
        logEvent("PROFILE_REQUEST_RESULT", apiResult = requested)
        if (!requested) {
            profileRequestPending.set(false)
            return false
        }
        profileRequestTimeout?.cancel(false)
        profileRequestTimeout = scheduler.schedule({
            if (profileRequestPending.compareAndSet(true, false)) {
                logEvent("PROFILE_REQUEST_TIMEOUT")
                failPendingConnection(reason = "PROFILE_REQUEST_TIMEOUT")
            }
        }, PROFILE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp(): Boolean {
        if (!hasBluetoothConnectPermission()) return false
        if (appRegistered || appRegistrationPending.get()) return true
        if (!appRegistrationPending.compareAndSet(false, true)) return true

        logEvent("REGISTER_APP_START")
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "PastelBoard",
            "Pink-purple virtual touchpad and keyboard",
            "PastelBoard",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidProtocol.descriptor,
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
        logEvent("REGISTER_APP_RESULT", apiResult = registrationStarted)
        if (!registrationStarted) {
            appRegistrationPending.set(false)
        } else {
            appRegistrationTimeout?.cancel(false)
            appRegistrationTimeout = scheduler.schedule({
                if (appRegistrationPending.compareAndSet(true, false)) {
                    logEvent("REGISTER_APP_TIMEOUT")
                    failPendingConnection(reason = "REGISTER_APP_TIMEOUT")
                }
            }, APP_REGISTRATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }
        return registrationStarted
    }

    @SuppressLint("MissingPermission")
    private fun connectToPendingDevice(requestId: Long?): Boolean {
        val device = pendingDevice
        val target = pendingTarget
        if (device == null || target == null || requestId == null || pendingRequestId != requestId) {
            return false
        }
        if (connectCallRequestId == requestId) return true
        connectCallRequestId = requestId
        val accepted = hidDevice?.connect(device) == true
        logEvent("CONNECT_RESULT", device = device, apiResult = accepted, detail = "requestId=$requestId")
        if (!accepted) {
            failPendingConnection(target, "CONNECT_REJECTED")
            return false
        }
        scheduleConnectionTimeout(requestId)
        return true
    }

    private fun scheduleConnectionTimeout(requestId: Long) {
        connectionTimeout?.cancel(false)
        connectionTimeout = scheduler.schedule({
            if (pendingRequestId == requestId && connectedDevice == null) {
                logEvent("CONNECT_TIMEOUT", device = pendingDevice)
                failPendingConnection(reason = "CONNECT_TIMEOUT")
            }
        }, CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    @SuppressLint("MissingPermission")
    private fun disconnectDevice(device: BluetoothDevice?) {
        device ?: return
        val accepted = hidDevice?.disconnect(device) == true
        logEvent("DISCONNECT_CALL", device = device, apiResult = accepted)
    }

    private fun failPendingConnection(
        target: DeviceTarget? = pendingTarget,
        reason: String,
    ) {
        pendingDevice = null
        pendingTarget = null
        pendingRequestId = null
        connectCallRequestId = null
        connectedDevice = null
        connectionTimeout?.cancel(false)
        connectionTimeout = null
        updateDiagnostics {
            it.copy(
                connectedDevice = null,
                lastFailure = reason,
            )
        }
        logEvent("CONNECTION_FAILED", deviceId = target?.address, detail = "reason=$reason")
        _connectionState.value = HidConnectionState(
            status = HidConnectionStatus.Failed,
            target = target,
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendReport(reportId: Int, data: ByteArray) {
        val shouldLogReport = reportId != HidProtocol.POINTER_REPORT_ID ||
            pointerReportLogCounter.incrementAndGet() % POINTER_REPORT_LOG_INTERVAL == 1L
        if (shouldLogReport) {
            logEvent("REPORT_SEND_ATTEMPT", device = connectedDevice, reportId = reportId, size = data.size)
        }
        val device = connectedDevice
        if (device == null) {
            recordReport(reportId, data.size, false)
            if (shouldLogReport) {
                logEvent("REPORT_DROPPED_NO_DEVICE", reportId = reportId, size = data.size, apiResult = false)
            }
            return
        }
        if (!hasBluetoothConnectPermission()) {
            recordReport(reportId, data.size, false)
            if (shouldLogReport) {
                logEvent("REPORT_DROPPED_PERMISSION", device = device, reportId = reportId, size = data.size, apiResult = false)
            }
            return
        }
        val sent = hidDevice?.sendReport(device, reportId, data) == true
        recordReport(reportId, data.size, sent)
        if (!sent) {
            updateDiagnostics { it.copy(lastFailure = "REPORT_SEND_REJECTED_$reportId") }
        }
        if (shouldLogReport || !sent) {
            logEvent("REPORT_SEND_RESULT", device = device, reportId = reportId, size = data.size, apiResult = sent)
        }
    }

    private fun recordReport(reportId: Int, size: Int, accepted: Boolean) {
        val status = HidReportStatus(reportId = reportId, size = size, accepted = accepted)
        updateDiagnostics {
            when (reportId) {
                HidProtocol.KEYBOARD_REPORT_ID -> it.copy(lastKeyboardReport = status)
                HidProtocol.POINTER_REPORT_ID -> it.copy(lastPointerReport = status)
                else -> it
            }
        }
    }

    private fun isCurrentCallbackDevice(device: BluetoothDevice?): Boolean {
        if (device == null) return false
        val expected = pendingDevice ?: connectedDevice ?: return false
        return isSameDevice(expected, device)
    }

    private fun isCompatiblePluggedDevice(device: BluetoothDevice): Boolean {
        val pending = pendingDevice
        return pending == null || isSameDevice(pending, device)
    }

    @SuppressLint("MissingPermission")
    private fun isSameDevice(first: BluetoothDevice?, second: BluetoothDevice?): Boolean {
        return first?.address != null && first.address == second?.address
    }

    private fun updateDiagnostics(transform: (HidDiagnostics) -> HidDiagnostics) {
        _diagnostics.value = transform(_diagnostics.value)
    }

    @SuppressLint("MissingPermission")
    private fun logEvent(
        event: String,
        device: BluetoothDevice? = null,
        deviceId: String? = null,
        reportId: Int? = null,
        size: Int? = null,
        apiResult: Boolean? = null,
        detail: String? = null,
    ) {
        val fields = buildList {
            add("profile=${hidDevice != null}")
            add("registered=$appRegistered")
            add("device=${device?.safeAddress() ?: deviceId.orEmpty()}")
            add("reportId=${reportId ?: "-"}")
            add("size=${size ?: "-"}")
            add("apiResult=${apiResult ?: "-"}")
            detail?.let { add(it) }
        }
        logger("$event ${fields.joinToString(" ")}")
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

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeAddress(): String {
        return if (hasBluetoothConnectPermission()) address.orEmpty() else "permission-missing"
    }

    private fun Int.toConnectionStateName(): String {
        return when (this) {
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
            BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            else -> toString()
        }
    }

    private companion object {
        const val PROFILE_TIMEOUT_MS = 15_000L
        const val APP_REGISTRATION_TIMEOUT_MS = 15_000L
        const val CONNECTION_TIMEOUT_MS = 15_000L
        const val POINTER_REPORT_LOG_INTERVAL = 10L
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

data class HidDiagnostics(
    val profileReady: Boolean = false,
    val appRegistered: Boolean = false,
    val pluggedDevice: String? = null,
    val connectedDevice: String? = null,
    val lastKeyboardReport: HidReportStatus? = null,
    val lastPointerReport: HidReportStatus? = null,
    val lastFailure: String? = null,
)

data class HidReportStatus(
    val reportId: Int,
    val size: Int,
    val accepted: Boolean,
)
