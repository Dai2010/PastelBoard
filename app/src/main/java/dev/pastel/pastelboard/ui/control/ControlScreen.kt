package dev.pastel.pastelboard.ui.control

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ControlScreen(
    state: PastelBoardUiState,
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ControlTopBar(
                state = state,
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

                ControlMode.Keyboard -> LaptopKeyboard(
                    state = state,
                    onPressKey = onPressKey,
                )
            }
        }
    }
}

@Composable
private fun ControlTopBar(
    state: PastelBoardUiState,
    onSelectMode: (ControlMode) -> Unit,
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        cornerRadius = 28,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(state.palette.gradientStart, state.palette.gradientEnd),
                            ),
                        ),
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
                        text = "固定顶部栏 · 可切换触控板和键盘",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            state.palette.keyTop.copy(alpha = 0.82f),
                            state.palette.keySide.copy(alpha = 0.82f),
                        ),
                    ),
                )
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(36.dp))
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
                    modifier = Modifier.size(62.dp),
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
            modifier = Modifier.width(148.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onClickPointer(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("左键")
            }
            Button(
                onClick = { onClickPointer(2) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) {
                Text("右键")
            }
        }
    }
}

@Composable
private fun LaptopKeyboard(
    state: PastelBoardUiState,
    onPressKey: (HidUsage?, Set<HidModifier>) -> Unit,
) {
    val playKeySound = rememberKeySoundPlayer(state.keySoundEnabled)
    val rows = listOf(
        listOf(
            KeySpec("Esc", HidUsage.Escape, 1f), KeySpec("F1", HidUsage.F1), KeySpec("F2", HidUsage.F2),
            KeySpec("F3", HidUsage.F3), KeySpec("F4", HidUsage.F4), KeySpec("F5", HidUsage.F5),
            KeySpec("F6", HidUsage.F6), KeySpec("F7", HidUsage.F7), KeySpec("F8", HidUsage.F8),
            KeySpec("F9", HidUsage.F9), KeySpec("F10", HidUsage.F10), KeySpec("F11", HidUsage.F11),
            KeySpec("F12", HidUsage.F12),
        ),
        listOf(
            KeySpec("`", HidUsage.Grave), KeySpec("1", HidUsage.Num1), KeySpec("2", HidUsage.Num2),
            KeySpec("3", HidUsage.Num3), KeySpec("4", HidUsage.Num4), KeySpec("5", HidUsage.Num5),
            KeySpec("6", HidUsage.Num6), KeySpec("7", HidUsage.Num7), KeySpec("8", HidUsage.Num8),
            KeySpec("9", HidUsage.Num9), KeySpec("0", HidUsage.Num0), KeySpec("-", HidUsage.Minus),
            KeySpec("=", HidUsage.Equal), KeySpec("⌫", HidUsage.Backspace, 1.7f),
        ),
        listOf(
            KeySpec("Tab", HidUsage.Tab, 1.4f), KeySpec("Q", HidUsage.Q), KeySpec("W", HidUsage.W),
            KeySpec("E", HidUsage.E), KeySpec("R", HidUsage.R), KeySpec("T", HidUsage.T),
            KeySpec("Y", HidUsage.Y), KeySpec("U", HidUsage.U), KeySpec("I", HidUsage.I),
            KeySpec("O", HidUsage.O), KeySpec("P", HidUsage.P), KeySpec("[", HidUsage.LeftBracket),
            KeySpec("]", HidUsage.RightBracket), KeySpec("\\", HidUsage.Backslash, 1.3f),
        ),
        listOf(
            KeySpec("Caps", HidUsage.CapsLock, 1.7f), KeySpec("A", HidUsage.A), KeySpec("S", HidUsage.S),
            KeySpec("D", HidUsage.D), KeySpec("F", HidUsage.F), KeySpec("G", HidUsage.G),
            KeySpec("H", HidUsage.H), KeySpec("J", HidUsage.J), KeySpec("K", HidUsage.K),
            KeySpec("L", HidUsage.L), KeySpec(";", HidUsage.Semicolon), KeySpec("'", HidUsage.Apostrophe),
            KeySpec("Enter", HidUsage.Enter, 1.9f),
        ),
        listOf(
            KeySpec("Shift", null, 2.1f, setOf(HidModifier.LeftShift)), KeySpec("Z", HidUsage.Z),
            KeySpec("X", HidUsage.X), KeySpec("C", HidUsage.C), KeySpec("V", HidUsage.V),
            KeySpec("B", HidUsage.B), KeySpec("N", HidUsage.N), KeySpec("M", HidUsage.M),
            KeySpec(",", HidUsage.Comma), KeySpec(".", HidUsage.Dot), KeySpec("/", HidUsage.Slash),
            KeySpec("↑", HidUsage.ArrowUp), KeySpec("Shift", null, 1.7f, setOf(HidModifier.RightShift)),
        ),
        listOf(
            KeySpec("Ctrl", null, 1.2f, setOf(HidModifier.LeftCtrl)),
            KeySpec("Alt", null, 1.2f, setOf(HidModifier.LeftAlt)),
            KeySpec("Space", HidUsage.Space, 5.8f),
            KeySpec("Alt", null, 1.2f, setOf(HidModifier.RightAlt)),
            KeySpec("←", HidUsage.ArrowLeft), KeySpec("↓", HidUsage.ArrowDown), KeySpec("→", HidUsage.ArrowRight),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.linearGradient(
                    listOf(state.palette.gradientStart, state.palette.gradientEnd),
                ),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { spec ->
                    KeyboardKey(
                        spec = spec,
                        modifier = Modifier.weight(spec.weight),
                        playKeySound = playKeySound,
                        onPressKey = onPressKey,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyboardKey(
    spec: KeySpec,
    modifier: Modifier = Modifier,
    playKeySound: () -> Unit,
    onPressKey: (HidUsage?, Set<HidModifier>) -> Unit,
) {
    var burstToken by remember { mutableStateOf(0) }
    val burstProgress = remember { Animatable(1f) }

    LaunchedEffect(burstToken) {
        if (burstToken == 0) return@LaunchedEffect
        burstProgress.snapTo(0f)
        burstProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        )
    }

    fun press() {
        burstToken += 1
        playKeySound()
        onPressKey(spec.usage, spec.modifiers)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(5.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.46f), RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { press() },
                onLongClick = { press() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        GoldenFirework(
            progress = burstProgress.value,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = spec.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun GoldenFirework(progress: Float, modifier: Modifier = Modifier) {
    if (progress >= 1f) return

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val radius = 8f + progress * size.minDimension * 0.54f
        drawCircle(
            color = Color(0xFFFFF2A8).copy(alpha = alpha * 0.34f),
            radius = radius * 0.42f,
            center = center,
        )
        repeat(14) { index ->
            val angle = (PI * 2.0 * index / 14.0).toFloat()
            val distance = radius * (0.46f + (index % 3) * 0.08f)
            val sparkX = (cos(angle.toDouble()) * distance).toFloat()
            val sparkY = (sin(angle.toDouble()) * distance).toFloat()
            val sparkCenter = Offset(
                x = center.x + sparkX,
                y = center.y + sparkY,
            )
            drawCircle(
                color = Color(0xFFFFCF4D).copy(alpha = alpha),
                radius = 2.2f + (index % 4),
                center = sparkCenter,
            )
            drawLine(
                color = Color(0xFFFFE89A).copy(alpha = alpha * 0.72f),
                start = center,
                end = sparkCenter,
                strokeWidth = 1.4f,
            )
        }
    }
}

private data class KeySpec(
    val label: String,
    val usage: HidUsage?,
    val weight: Float = 1f,
    val modifiers: Set<HidModifier> = emptySet(),
)
