package dev.pastel.pastelboard.model

data class DeviceTarget(
    val name: String,
    val address: String,
    val type: DeviceType,
    val isPaired: Boolean,
)

enum class DeviceType {
    Laptop,
    Desktop,
    Tablet,
    Unknown,
}

