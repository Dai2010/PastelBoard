package dev.pastel.pastelboard.ui.control

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.pastel.pastelboard.PastelBoardUiState
import dev.pastel.pastelboard.bluetooth.HidModifier
import dev.pastel.pastelboard.bluetooth.HidUsage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PastelKeyboardDeck(
    state: PastelBoardUiState,
    modifier: Modifier = Modifier,
    sendKey: (HidUsage?, Set<HidModifier>) -> Unit = { _, _ -> },
) {
    val playKeySound = rememberKeySoundPlayer(
        enabled = state.keySoundEnabled,
        soundUri = state.keySoundUri,
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(state.palette.gradientStart, state.palette.gradientEnd)))
            .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(32.dp)),
    ) {
        val compactHeight = maxHeight < 300.dp
        val outerPadding = if (compactHeight) 8.dp else 12.dp
        val rowGap = if (compactHeight) 5.dp else 7.dp
        val keyCorner = if (compactHeight) 13.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            laptopKeyboardRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(rowGap),
                ) {
                    row.forEach { spec ->
                        PastelKeyboardKey(
                            spec = spec,
                            keyColor = state.keyColor,
                            cornerRadius = keyCorner.value.toInt(),
                            modifier = Modifier.weight(spec.weight),
                            playKeySound = playKeySound,
                            sendKey = sendKey,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PastelKeyboardKey(
    spec: KeyboardKeySpec,
    keyColor: Color,
    cornerRadius: Int,
    modifier: Modifier = Modifier,
    playKeySound: () -> Unit,
    sendKey: (HidUsage?, Set<HidModifier>) -> Unit,
) {
    var burstToken by remember { mutableStateOf(0) }
    val burstProgress = remember { Animatable(1f) }
    val labelColor = if (keyColor.luminance() > 0.58f) Color(0xFF2A1730) else Color.White

    LaunchedEffect(burstToken) {
        if (burstToken == 0) return@LaunchedEffect
        burstProgress.snapTo(0f)
        burstProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        )
    }

    fun press() {
        burstToken += 1
        playKeySound()
        sendKey(spec.usage, spec.modifiers)
    }

    Box(
        modifier = modifier
            .zIndex(if (burstProgress.value < 1f) 1f else 0f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(5.dp, RoundedCornerShape(cornerRadius.dp), clip = false)
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.62f),
                            keyColor.copy(alpha = 0.48f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                        ),
                    ),
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.46f),
                            Color.White.copy(alpha = 0.10f),
                            keyColor.copy(alpha = 0.22f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.66f), RoundedCornerShape(cornerRadius.dp))
                .combinedClickable(
                    onClick = { press() },
                    onLongClick = { press() },
                ),
        ) {}
        Text(
            text = spec.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = labelColor,
        )
        GoldenKeyFirework(
            progress = burstProgress.value,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 2f
                    scaleY = 2f
                    clip = false
                },
        )
    }
}

@Composable
private fun GoldenKeyFirework(progress: Float, modifier: Modifier = Modifier) {
    if (progress >= 1f) return

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val radius = 8f + progress * size.minDimension * 0.58f
        drawCircle(
            color = Color(0xFFFFF2A8).copy(alpha = alpha * 0.34f),
            radius = radius * 0.42f,
            center = center,
        )
        repeat(24) { index ->
            val angle = (PI * 2.0 * index / 24.0).toFloat()
            val distance = radius * (0.44f + (index % 3) * 0.08f)
            val sparkX = (cos(angle.toDouble()) * distance).toFloat()
            val sparkY = (sin(angle.toDouble()) * distance).toFloat()
            val sparkCenter = Offset(center.x + sparkX, center.y + sparkY)
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

private data class KeyboardKeySpec(
    val label: String,
    val usage: HidUsage?,
    val weight: Float = 1f,
    val modifiers: Set<HidModifier> = emptySet(),
)

private val laptopKeyboardRows = listOf(
    listOf(
        KeyboardKeySpec("Esc", HidUsage.Escape, 1f), KeyboardKeySpec("F1", HidUsage.F1), KeyboardKeySpec("F2", HidUsage.F2),
        KeyboardKeySpec("F3", HidUsage.F3), KeyboardKeySpec("F4", HidUsage.F4), KeyboardKeySpec("F5", HidUsage.F5),
        KeyboardKeySpec("F6", HidUsage.F6), KeyboardKeySpec("F7", HidUsage.F7), KeyboardKeySpec("F8", HidUsage.F8),
        KeyboardKeySpec("F9", HidUsage.F9), KeyboardKeySpec("F10", HidUsage.F10), KeyboardKeySpec("F11", HidUsage.F11),
        KeyboardKeySpec("F12", HidUsage.F12),
    ),
    listOf(
        KeyboardKeySpec("`", HidUsage.Grave), KeyboardKeySpec("1", HidUsage.Num1), KeyboardKeySpec("2", HidUsage.Num2),
        KeyboardKeySpec("3", HidUsage.Num3), KeyboardKeySpec("4", HidUsage.Num4), KeyboardKeySpec("5", HidUsage.Num5),
        KeyboardKeySpec("6", HidUsage.Num6), KeyboardKeySpec("7", HidUsage.Num7), KeyboardKeySpec("8", HidUsage.Num8),
        KeyboardKeySpec("9", HidUsage.Num9), KeyboardKeySpec("0", HidUsage.Num0), KeyboardKeySpec("-", HidUsage.Minus),
        KeyboardKeySpec("=", HidUsage.Equal), KeyboardKeySpec("⌫", HidUsage.Backspace, 1.7f),
    ),
    listOf(
        KeyboardKeySpec("Tab", HidUsage.Tab, 1.4f), KeyboardKeySpec("Q", HidUsage.Q), KeyboardKeySpec("W", HidUsage.W),
        KeyboardKeySpec("E", HidUsage.E), KeyboardKeySpec("R", HidUsage.R), KeyboardKeySpec("T", HidUsage.T),
        KeyboardKeySpec("Y", HidUsage.Y), KeyboardKeySpec("U", HidUsage.U), KeyboardKeySpec("I", HidUsage.I),
        KeyboardKeySpec("O", HidUsage.O), KeyboardKeySpec("P", HidUsage.P), KeyboardKeySpec("[", HidUsage.LeftBracket),
        KeyboardKeySpec("]", HidUsage.RightBracket), KeyboardKeySpec("\\", HidUsage.Backslash, 1.3f),
    ),
    listOf(
        KeyboardKeySpec("Caps", HidUsage.CapsLock, 1.7f), KeyboardKeySpec("A", HidUsage.A), KeyboardKeySpec("S", HidUsage.S),
        KeyboardKeySpec("D", HidUsage.D), KeyboardKeySpec("F", HidUsage.F), KeyboardKeySpec("G", HidUsage.G),
        KeyboardKeySpec("H", HidUsage.H), KeyboardKeySpec("J", HidUsage.J), KeyboardKeySpec("K", HidUsage.K),
        KeyboardKeySpec("L", HidUsage.L), KeyboardKeySpec(";", HidUsage.Semicolon), KeyboardKeySpec("'", HidUsage.Apostrophe),
        KeyboardKeySpec("Enter", HidUsage.Enter, 1.9f),
    ),
    listOf(
        KeyboardKeySpec("Shift", null, 2.1f, setOf(HidModifier.LeftShift)), KeyboardKeySpec("Z", HidUsage.Z),
        KeyboardKeySpec("X", HidUsage.X), KeyboardKeySpec("C", HidUsage.C), KeyboardKeySpec("V", HidUsage.V),
        KeyboardKeySpec("B", HidUsage.B), KeyboardKeySpec("N", HidUsage.N), KeyboardKeySpec("M", HidUsage.M),
        KeyboardKeySpec(",", HidUsage.Comma), KeyboardKeySpec(".", HidUsage.Dot), KeyboardKeySpec("/", HidUsage.Slash),
        KeyboardKeySpec("↑", HidUsage.ArrowUp), KeyboardKeySpec("Shift", null, 1.7f, setOf(HidModifier.RightShift)),
    ),
    listOf(
        KeyboardKeySpec("Ctrl", null, 1.2f, setOf(HidModifier.LeftCtrl)),
        KeyboardKeySpec("Alt", null, 1.2f, setOf(HidModifier.LeftAlt)),
        KeyboardKeySpec("Space", HidUsage.Space, 5.8f),
        KeyboardKeySpec("Alt", null, 1.2f, setOf(HidModifier.RightAlt)),
        KeyboardKeySpec("←", HidUsage.ArrowLeft), KeyboardKeySpec("↓", HidUsage.ArrowDown), KeyboardKeySpec("→", HidUsage.ArrowRight),
    ),
)
