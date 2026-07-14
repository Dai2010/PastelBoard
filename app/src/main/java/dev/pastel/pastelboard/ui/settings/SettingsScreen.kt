package dev.pastel.pastelboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.ScreenRotation
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
    onPickKeySound: () -> Unit,
    onClearKeySound: () -> Unit,
    onKeyHexChanged: (String) -> Unit,
    onApplyKeyHex: () -> Unit,
    onKeyColorChannelChanged: (ColorChannel, Float) -> Unit,
    onLaunchLandscapeChanged: (Boolean) -> Unit,
) {
    PastelBackground(
        palette = state.palette,
        backgroundImageUri = state.backgroundImageUri,
        backgroundImageOpacity = state.backgroundImageOpacity,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            val compact = maxWidth < 760.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SettingsHeader(state = state, onBack = onBack)
                if (compact) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        AppearancePanel(state, onSelectPalette, onPickBackgroundImage, onClearBackgroundImage, onBackgroundOpacityChanged, onLaunchLandscapeChanged)
                        CustomColorPanel(state, onHexChanged, onApplyHex, onColorChannelChanged)
                        KeyboardPanel(state, onKeySoundEnabledChanged, onPickKeySound, onClearKeySound, onKeyHexChanged, onApplyKeyHex, onKeyColorChannelChanged)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.05f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            AppearancePanel(state, onSelectPalette, onPickBackgroundImage, onClearBackgroundImage, onBackgroundOpacityChanged, onLaunchLandscapeChanged)
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            CustomColorPanel(state, onHexChanged, onApplyHex, onColorChannelChanged)
                            KeyboardPanel(state, onKeySoundEnabledChanged, onPickKeySound, onClearKeySound, onKeyHexChanged, onApplyKeyHex, onKeyColorChannelChanged)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(state: PastelBoardUiState, onBack: () -> Unit) {
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
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
                    text = "外观、背景、按键颜色、按键音和启动横屏都在这里。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ColorPreview(state.palette)
    }
}

@Composable
private fun AppearancePanel(
    state: PastelBoardUiState,
    onSelectPalette: (PastelPalette) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onBackgroundOpacityChanged: (Float) -> Unit,
    onLaunchLandscapeChanged: (Boolean) -> Unit,
) {
    SoftCard {
        Text("外观与背景", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("默认仍是图标同款粉紫渐变，也可以换本地图片做毛玻璃背景。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DefaultPastelPalettes.forEach { palette ->
                PaletteCard(
                    palette = palette,
                    selected = state.palette.id == palette.id,
                    onClick = { onSelectPalette(palette) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        BackgroundSettings(
            state = state,
            onPickBackgroundImage = onPickBackgroundImage,
            onClearBackgroundImage = onClearBackgroundImage,
            onBackgroundOpacityChanged = onBackgroundOpacityChanged,
        )
        Spacer(Modifier.height(14.dp))
        ToggleRow(
            icon = { Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = "开启即横屏",
            description = "打开应用后立即进入横屏，更接近实体键盘使用姿势。",
            checked = state.launchLandscapeEnabled,
            onCheckedChange = onLaunchLandscapeChanged,
        )
    }
}

@Composable
private fun CustomColorPanel(
    state: PastelBoardUiState,
    onHexChanged: (String) -> Unit,
    onApplyHex: () -> Unit,
    onColorChannelChanged: (ColorChannel, Float) -> Unit,
) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("主题调色板", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        ColorSlider(ColorChannel.Red, state.palette.primary.red, Color(0xFFFF6F91), onColorChannelChanged)
        ColorSlider(ColorChannel.Green, state.palette.primary.green, Color(0xFF73D6A0), onColorChannelChanged)
        ColorSlider(ColorChannel.Blue, state.palette.primary.blue, Color(0xFF8EA7FF), onColorChannelChanged)
        Spacer(Modifier.height(12.dp))
        HexInputRow(
            value = state.hexInput,
            label = "主题 16 进制颜色",
            placeholder = "#D85CF5",
            onValueChange = onHexChanged,
            onApply = onApplyHex,
        )
        state.colorMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun KeyboardPanel(
    state: PastelBoardUiState,
    onKeySoundEnabledChanged: (Boolean) -> Unit,
    onPickKeySound: () -> Unit,
    onClearKeySound: () -> Unit,
    onKeyHexChanged: (String) -> Unit,
    onApplyKeyHex: () -> Unit,
    onKeyColorChannelChanged: (ColorChannel, Float) -> Unit,
) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("键盘效果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow(
            icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = "按键音",
            description = if (state.keySoundUri == null) "内置原创清脆 Flick 感音效，可替换为本地音频。" else "正在使用自定义本地音频。",
            checked = state.keySoundEnabled,
            onCheckedChange = onKeySoundEnabledChanged,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPickKeySound, shape = RoundedCornerShape(16.dp)) {
                Text("选择按键音")
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
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(state.keyColor)
                    .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
            )
            Column {
                Text("按键颜色", fontWeight = FontWeight.Bold)
                Text("单独调整键帽颜色，烟花效果保持金色。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        ColorSlider(ColorChannel.Red, state.keyColor.red, Color(0xFFFF6F91), onKeyColorChannelChanged)
        ColorSlider(ColorChannel.Green, state.keyColor.green, Color(0xFF73D6A0), onKeyColorChannelChanged)
        ColorSlider(ColorChannel.Blue, state.keyColor.blue, Color(0xFF8EA7FF), onKeyColorChannelChanged)
        Spacer(Modifier.height(12.dp))
        HexInputRow(
            value = state.keyHexInput,
            label = "键帽 16 进制颜色",
            placeholder = "#FFD8E8",
            onValueChange = onKeyHexChanged,
            onApply = onApplyKeyHex,
        )
        state.keyColorMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.32f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            icon()
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HexInputRow(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(18.dp),
        )
        Button(onClick = onApply, shape = RoundedCornerShape(18.dp), modifier = Modifier.height(58.dp)) {
            Text("应用")
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
        Text(palette.primary.toHexString(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaletteCard(
    palette: PastelPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(154.dp),
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
                    .height(52.dp)
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
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.primary)
                }
                Text(palette.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF694963))
            }
        }
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(channel.label, fontWeight = FontWeight.Bold)
            Text("${(value * 255).toInt().coerceIn(0, 255)}")
        }
        Slider(
            value = value,
            onValueChange = { onChange(channel, it) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint),
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
            .background(Color.White.copy(alpha = 0.32f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("背景图片", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (state.backgroundImageUri == null) "当前使用默认粉紫渐变" else "已使用本地图片作为背景",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
