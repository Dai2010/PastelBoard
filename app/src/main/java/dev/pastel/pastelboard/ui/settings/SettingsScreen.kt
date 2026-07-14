package dev.pastel.pastelboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.ColorChannel
import dev.pastel.pastelboard.PastelBoardUiState
import dev.pastel.pastelboard.model.DefaultPastelPalettes
import dev.pastel.pastelboard.model.PastelPalette
import dev.pastel.pastelboard.toHexString
import dev.pastel.pastelboard.ui.components.PastelBackground
import dev.pastel.pastelboard.ui.components.SoftCard

@Composable
fun SettingsScreen(
    state: PastelBoardUiState,
    onBack: () -> Unit,
    onSelectPalette: (PastelPalette) -> Unit,
    onHexChanged: (String) -> Unit,
    onApplyHex: () -> Unit,
    onColorChannelChanged: (ColorChannel, Float) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onBackgroundOpacityChanged: (Float) -> Unit,
    onKeySoundEnabledChanged: (Boolean) -> Unit,
) {
    PastelBackground(
        palette = state.palette,
        backgroundImageUri = state.backgroundImageUri,
        backgroundImageOpacity = state.backgroundImageOpacity,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                    Column {
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "换一套软萌键盘颜色，也可以输入 16 进制代码。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ColorPreview(state.palette)
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SoftCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Text(
                        text = "默认颜色配置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "默认推荐使用粉紫渐变，也提供纯粉和纯紫两套预设。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(DefaultPastelPalettes) { palette ->
                            PaletteCard(
                                palette = palette,
                                selected = state.palette.id == palette.id,
                                onClick = { onSelectPalette(palette) },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "界面风格",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StyleBadge("Material You")
                        StyleBadge("霞鹜文楷")
                        StyleBadge("圆角软糖键帽")
                    }
                    Spacer(Modifier.height(20.dp))
                    BackgroundSettings(
                        state = state,
                        onPickBackgroundImage = onPickBackgroundImage,
                        onClearBackgroundImage = onClearBackgroundImage,
                        onBackgroundOpacityChanged = onBackgroundOpacityChanged,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.28f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("按键音", fontWeight = FontWeight.Bold)
                            Text(
                                text = "原创清脆 Flick 感音效，可随时关闭。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = state.keySoundEnabled,
                            onCheckedChange = onKeySoundEnabledChanged,
                        )
                    }
                }

                SoftCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "自定义调色板",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    ColorSlider(
                        channel = ColorChannel.Red,
                        value = state.palette.primary.red,
                        tint = Color(0xFFFF6F91),
                        onChange = onColorChannelChanged,
                    )
                    ColorSlider(
                        channel = ColorChannel.Green,
                        value = state.palette.primary.green,
                        tint = Color(0xFF73D6A0),
                        onChange = onColorChannelChanged,
                    )
                    ColorSlider(
                        channel = ColorChannel.Blue,
                        value = state.palette.primary.blue,
                        tint = Color(0xFF8EA7FF),
                        onChange = onColorChannelChanged,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.hexInput,
                            onValueChange = onHexChanged,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("16 进制颜色") },
                            placeholder = { Text("#D85CF5") },
                            shape = RoundedCornerShape(18.dp),
                        )
                        Button(
                            onClick = onApplyHex,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.height(58.dp),
                        ) {
                            Text("应用")
                        }
                    }
                    state.colorMessage?.let { message ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    KeyboardPreview(state.palette)
                }
            }
        }
    }
}

@Composable
private fun ColorPreview(palette: PastelPalette) {
    Box(
        modifier = Modifier
            .size(width = 170.dp, height = 54.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)))
            .border(2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = palette.primary.toHexString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PaletteCard(palette: PastelPalette, selected: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.size(width = 186.dp, height = 166.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = palette.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd))),
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(palette.label, fontWeight = FontWeight.Bold, color = Color(0xFF2C152D))
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.primary)
                    }
                }
                Text(
                    text = palette.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF694963),
                )
            }
        }
    }
}

@Composable
private fun StyleBadge(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ColorSlider(
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

@Composable
private fun BackgroundSettings(
    state: PastelBoardUiState,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onBackgroundOpacityChanged: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.28f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("背景图片", fontWeight = FontWeight.Bold)
                Text(
                    text = if (state.backgroundImageUri == null) "当前使用默认粉紫渐变" else "已使用本地图片作为背景",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickBackgroundImage, shape = RoundedCornerShape(16.dp)) {
                    Text("选择图片")
                }
                OutlinedButton(
                    onClick = onClearBackgroundImage,
                    shape = RoundedCornerShape(16.dp),
                    enabled = state.backgroundImageUri != null,
                ) {
                    Text("清除")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("图片透明度")
            Text("${(state.backgroundImageOpacity * 100).toInt()}%")
        }
        Slider(
            value = state.backgroundImageOpacity,
            onValueChange = onBackgroundOpacityChanged,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun KeyboardPreview(palette: PastelPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(if (rowIndex == 2) 6 else 8) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.82f)),
                    )
                }
            }
        }
    }
}
