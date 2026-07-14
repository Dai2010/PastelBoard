package dev.pastel.pastelboard.ui.test

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.ColorChannel
import dev.pastel.pastelboard.PastelBoardUiState
import dev.pastel.pastelboard.ui.components.PastelBackground
import dev.pastel.pastelboard.ui.components.SoftCard
import dev.pastel.pastelboard.ui.control.PastelKeyboardDeck

@Composable
fun KeyboardTestScreen(
    state: PastelBoardUiState,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
    onKeySoundEnabledChanged: (Boolean) -> Unit,
    onPickKeySound: () -> Unit,
    onClearKeySound: () -> Unit,
    onKeyHexChanged: (String) -> Unit,
    onApplyKeyHex: () -> Unit,
    onKeyColorChannelChanged: (ColorChannel, Float) -> Unit,
) {
    PastelBackground(
        palette = state.palette,
        backgroundImageUri = state.backgroundImageUri,
        backgroundImageOpacity = state.backgroundImageOpacity,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1.55f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TestHeader(
                    onBack = onBack,
                    onOpenMenu = onOpenMenu,
                )
                PastelKeyboardDeck(
                    state = state,
                    modifier = Modifier.weight(1f),
                )
            }

            SoftCard(
                modifier = Modifier
                    .weight(0.72f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "键盘测试",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "在这里点击键帽，只预览动画和声音，不会向蓝牙设备发送按键。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    KeySoundPanel(
                        state = state,
                        onKeySoundEnabledChanged = onKeySoundEnabledChanged,
                        onPickKeySound = onPickKeySound,
                        onClearKeySound = onClearKeySound,
                    )

                    KeyColorPanel(
                        state = state,
                        onKeyHexChanged = onKeyHexChanged,
                        onApplyKeyHex = onApplyKeyHex,
                        onKeyColorChannelChanged = onKeyColorChannelChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun TestHeader(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
            ) {
                Icon(Icons.Rounded.Menu, contentDescription = "菜单")
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
            }
            Column {
                Text(
                    text = "键盘效果测试页",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "放大键盘 · 测试烟花、键帽色和按键音",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KeySoundPanel(
    state: PastelBoardUiState,
    onKeySoundEnabledChanged: (Boolean) -> Unit,
    onPickKeySound: () -> Unit,
    onClearKeySound: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.34f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("按键音", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (state.keySoundUri == null) "使用内置原创 Flick 感音效" else "正在使用自定义音频",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = state.keySoundEnabled,
                onCheckedChange = onKeySoundEnabledChanged,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPickKeySound, shape = RoundedCornerShape(16.dp)) {
                Text("选择音频")
            }
            OutlinedButton(
                onClick = onClearKeySound,
                enabled = state.keySoundUri != null,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("恢复默认")
            }
        }
        state.keySoundMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun KeyColorPanel(
    state: PastelBoardUiState,
    onKeyHexChanged: (String) -> Unit,
    onApplyKeyHex: () -> Unit,
    onKeyColorChannelChanged: (ColorChannel, Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.34f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(state.keyColor),
            )
            Column {
                Text("按键颜色", fontWeight = FontWeight.Bold)
                Text("调键帽，不影响整体主题", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        CompactColorSlider(ColorChannel.Red, state.keyColor.red, Color(0xFFFF6F91), onKeyColorChannelChanged)
        CompactColorSlider(ColorChannel.Green, state.keyColor.green, Color(0xFF73D6A0), onKeyColorChannelChanged)
        CompactColorSlider(ColorChannel.Blue, state.keyColor.blue, Color(0xFF8EA7FF), onKeyColorChannelChanged)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.keyHexInput,
                onValueChange = onKeyHexChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("键帽色") },
                placeholder = { Text("#FFD8E8") },
                shape = RoundedCornerShape(18.dp),
            )
            Button(
                onClick = onApplyKeyHex,
                modifier = Modifier.height(58.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("应用")
            }
        }
        state.keyColorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CompactColorSlider(
    channel: ColorChannel,
    value: Float,
    tint: Color,
    onChange: (ColorChannel, Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(channel.label, fontWeight = FontWeight.Bold)
            Text("${(value * 255).toInt().coerceIn(0, 255)}")
        }
        Slider(
            value = value,
            onValueChange = { onChange(channel, it) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint,
            ),
        )
    }
}
