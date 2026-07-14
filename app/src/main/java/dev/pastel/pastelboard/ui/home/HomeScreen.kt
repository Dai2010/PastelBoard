package dev.pastel.pastelboard.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.PastelBoardUiState
import dev.pastel.pastelboard.R
import dev.pastel.pastelboard.model.DeviceTarget
import dev.pastel.pastelboard.model.DeviceType
import dev.pastel.pastelboard.ui.components.PastelBackground
import dev.pastel.pastelboard.ui.components.SoftCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: PastelBoardUiState,
    onOpenMenu: () -> Unit,
    onRefreshDevices: () -> Unit,
    onConnect: (DeviceTarget) -> Unit,
) {
    PastelBackground(
        palette = state.palette,
        backgroundImageUri = state.backgroundImageUri,
        backgroundImageOpacity = state.backgroundImageOpacity,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 720.dp

            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    HomeIntroPanel(
                        state = state,
                        onOpenMenu = onOpenMenu,
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DeviceSelectionPanel(
                        state = state,
                        onRefreshDevices = onRefreshDevices,
                        onConnect = onConnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    HomeIntroPanel(
                        state = state,
                        onOpenMenu = onOpenMenu,
                        compact = false,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.95f),
                    )
                    DeviceSelectionPanel(
                        state = state,
                        onRefreshDevices = onRefreshDevices,
                        onConnect = onConnect,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1.45f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeIntroPanel(
    state: PastelBoardUiState,
    onOpenMenu: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = if (compact) Arrangement.spacedBy(20.dp) else Arrangement.SpaceBetween,
    ) {
        HomeMenu(
            onOpenMenu = onOpenMenu,
        )

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                modifier = Modifier
                    .size(if (compact) 104.dp else 128.dp)
                    .clip(RoundedCornerShape(if (compact) 30.dp else 38.dp))
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
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(if (compact) 22.dp else 30.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "PastelBoard",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "像一台软萌的粉紫笔记本键盘，也像一块可远程控制的蓝牙触控板。",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeaturePill(Icons.Rounded.Keyboard, "真实键位", Modifier.weight(1f))
            FeaturePill(Icons.Rounded.Palette, "粉紫主题", Modifier.weight(1f))
            FeaturePill(Icons.Rounded.Bluetooth, "蓝牙 HID", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeMenu(
    onOpenMenu: () -> Unit,
) {
    Box {
        IconButton(
            onClick = onOpenMenu,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
        ) {
            Icon(Icons.Rounded.Menu, contentDescription = "菜单")
        }
    }
}

@Composable
private fun DeviceSelectionPanel(
    state: PastelBoardUiState,
    onRefreshDevices: () -> Unit,
    onConnect: (DeviceTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier,
        contentPadding = PaddingValues(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "选择连接设备",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.connectionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
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

        Spacer(Modifier.height(20.dp))

        if (state.devices.isEmpty()) {
            EmptyDeviceHint(onRefreshDevices)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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

@Composable
private fun FeaturePill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyDeviceHint(onRefreshDevices: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f))
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "还没有显示设备",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "请先在系统蓝牙里配对电脑，再回到这里刷新。",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        )
        Spacer(Modifier.height(20.dp))
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
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
