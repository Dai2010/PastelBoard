package dev.pastel.pastelboard.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.ControlMode
import dev.pastel.pastelboard.PastelBoardUiState
import dev.pastel.pastelboard.bluetooth.HidModifier
import dev.pastel.pastelboard.bluetooth.HidUsage
import dev.pastel.pastelboard.ui.components.GlassSurface
import dev.pastel.pastelboard.ui.components.PastelBackground

@Composable
fun ControlScreen(
    state: PastelBoardUiState,
    onOpenMenu: () -> Unit,
    onSelectMode: (ControlMode) -> Unit,
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onMovePointer: (Int, Int) -> Unit,
    onClickPointer: (Int) -> Unit,
    onPressKey: (HidUsage?, Set<HidModifier>) -> Unit,
) {
    PastelBackground(
        palette = state.palette,
        backgroundImageUri = state.backgroundImageUri,
        backgroundImageOpacity = state.backgroundImageOpacity,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ControlTopBar(
                state = state,
                onOpenMenu = onOpenMenu,
                onSelectMode = onSelectMode,
                onOpenSettings = onOpenSettings,
                onDisconnect = onDisconnect,
            )
            when (state.controlMode) {
                ControlMode.Touchpad -> TouchpadPanel(
                    state = state,
                    onMovePointer = onMovePointer,
                    onClickPointer = onClickPointer,
                )

                ControlMode.Keyboard -> PastelKeyboardDeck(
                    state = state,
                    sendKey = onPressKey,
                )
            }
        }
    }
}

@Composable
private fun ControlTopBar(
    state: PastelBoardUiState,
    onOpenMenu: () -> Unit,
    onSelectMode: (ControlMode) -> Unit,
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        cornerRadius = 24,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Rounded.Menu, contentDescription = "菜单")
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(state.palette.gradientStart, state.palette.gradientEnd))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.BluetoothConnected,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Column {
                    Text(
                        text = state.selectedDevice?.name ?: "未连接设备",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "固定顶部栏 · 触控板 / 键盘",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModeChip(
                    selected = state.controlMode == ControlMode.Touchpad,
                    label = ControlMode.Touchpad.label,
                    icon = { Icon(Icons.Rounded.TouchApp, contentDescription = null) },
                    onClick = { onSelectMode(ControlMode.Touchpad) },
                )
                ModeChip(
                    selected = state.controlMode == ControlMode.Keyboard,
                    label = ControlMode.Keyboard.label,
                    icon = { Icon(Icons.Rounded.Keyboard, contentDescription = null) },
                    onClick = { onSelectMode(ControlMode.Keyboard) },
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "设置")
                }
                IconButton(onClick = onDisconnect) {
                    Icon(Icons.Rounded.Close, contentDescription = "断开")
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon,
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun TouchpadPanel(
    state: PastelBoardUiState,
    onMovePointer: (Int, Int) -> Unit,
    onClickPointer: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            state.palette.keyTop.copy(alpha = 0.84f),
                            state.palette.keySide.copy(alpha = 0.84f),
                        ),
                    ),
                )
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(34.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount: Offset ->
                        change.consume()
                        onMovePointer(dragAmount.x.toInt(), dragAmount.y.toInt())
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "大面积触控板",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "拖动移动指针 · 右侧按钮模拟鼠标按键",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.width(138.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { onClickPointer(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("左键")
            }
            Button(
                onClick = { onClickPointer(2) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) {
                Text("右键")
            }
        }
    }
}
