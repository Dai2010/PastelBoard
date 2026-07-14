package dev.pastel.pastelboard.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TabletMac
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.R
import dev.pastel.pastelboard.PastelBoardUiState
import dev.pastel.pastelboard.model.DeviceTarget
import dev.pastel.pastelboard.model.DeviceType
import dev.pastel.pastelboard.ui.components.PastelBackground
import dev.pastel.pastelboard.ui.components.SoftCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: PastelBoardUiState,
    onOpenSettings: () -> Unit,
    onRefreshDevices: () -> Unit,
    onConnect: (DeviceTarget) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    PastelBackground(
        palette = state.palette,
        backgroundImageUri = state.backgroundImageUri,
        backgroundImageOpacity = state.backgroundImageOpacity,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.9f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                    ) {
                        Icon(Icons.Rounded.Menu, contentDescription = "菜单")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            },
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(state.palette.gradientStart, state.palette.gradientEnd),
                                ),
                            )
                            .padding(10.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = "PastelBoard 图标",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Text(
                        text = "PastelBoard",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "像一台软萌的粉紫笔记本键盘，也像一块可远程控制的蓝牙触控板。",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeaturePill(Icons.Rounded.Keyboard, "真实键位")
                    FeaturePill(Icons.Rounded.Palette, "粉紫主题")
                    FeaturePill(Icons.Rounded.Bluetooth, "蓝牙 HID")
                }
            }

            SoftCard(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "选择连接设备",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.connectionStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onRefreshDevices,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("刷新")
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (state.devices.isEmpty()) {
                    EmptyDeviceHint(onRefreshDevices)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.devices) { device ->
                            DeviceRow(
                                device = device,
                                onConnect = { onConnect(device) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EmptyDeviceHint(onRefreshDevices: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(54.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "还没有显示设备",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "请先在系统蓝牙里配对电脑，再回到这里刷新。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onRefreshDevices, shape = RoundedCornerShape(18.dp)) {
            Text("刷新已配对设备")
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceTarget, onConnect: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = device.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column {
                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (device.isPaired) "已配对 · ${device.address}" else device.address,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Button(onClick = onConnect, shape = RoundedCornerShape(18.dp)) {
                Text("连接")
            }
        }
    }
}

private val DeviceTarget.icon: ImageVector
    get() = when (type) {
        DeviceType.Laptop -> Icons.Rounded.LaptopMac
        DeviceType.Desktop -> Icons.Rounded.Computer
        DeviceType.Tablet -> Icons.Rounded.TabletMac
        DeviceType.Unknown -> Icons.Rounded.Bluetooth
    }
