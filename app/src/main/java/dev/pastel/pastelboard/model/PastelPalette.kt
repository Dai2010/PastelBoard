package dev.pastel.pastelboard.model

import androidx.compose.ui.graphics.Color

data class PastelPalette(
    val id: String,
    val label: String,
    val description: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val keyTop: Color,
    val keySide: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

val DefaultPastelPalettes = listOf(
    PastelPalette(
        id = "pink-purple-gradient",
        label = "粉紫渐变",
        description = "默认：贴近图标的粉色到纯紫色渐变",
        primary = Color(0xFFD85CF5),
        secondary = Color(0xFFFF79A8),
        tertiary = Color(0xFF7B61FF),
        background = Color(0xFFFFF7FC),
        surface = Color(0xFFFFEAF5),
        keyTop = Color(0xFFFFD8E8),
        keySide = Color(0xFFE6B6FF),
        gradientStart = Color(0xFFFF7EA8),
        gradientEnd = Color(0xFF8767FF),
    ),
    PastelPalette(
        id = "soft-pink",
        label = "软糖粉",
        description = "温柔粉色，适合甜系键盘外观",
        primary = Color(0xFFE85D95),
        secondary = Color(0xFFFF9CC2),
        tertiary = Color(0xFFFFC2D8),
        background = Color(0xFFFFF8FB),
        surface = Color(0xFFFFEAF2),
        keyTop = Color(0xFFFFD6E5),
        keySide = Color(0xFFF4A4C1),
        gradientStart = Color(0xFFFF8DB7),
        gradientEnd = Color(0xFFFFD6E7),
    ),
    PastelPalette(
        id = "pure-purple",
        label = "纯紫梦境",
        description = "更偏紫色的默认候选配置",
        primary = Color(0xFF8B5CF6),
        secondary = Color(0xFFB993FF),
        tertiary = Color(0xFFE1CBFF),
        background = Color(0xFFFBF8FF),
        surface = Color(0xFFF1E7FF),
        keyTop = Color(0xFFE8D8FF),
        keySide = Color(0xFFB391FF),
        gradientStart = Color(0xFFC084FC),
        gradientEnd = Color(0xFF7C5CFF),
    ),
)

fun customPaletteFrom(primary: Color): PastelPalette {
    return PastelPalette(
        id = "custom",
        label = "自定义颜色",
        description = "由调色板或 16 进制色值生成",
        primary = primary,
        secondary = primary.copy(alpha = 0.72f),
        tertiary = primary.copy(alpha = 0.52f),
        background = Color(0xFFFFFBFE),
        surface = primary.copy(alpha = 0.12f).compositeOver(Color.White),
        keyTop = primary.copy(alpha = 0.18f).compositeOver(Color.White),
        keySide = primary.copy(alpha = 0.36f).compositeOver(Color.White),
        gradientStart = primary.copy(alpha = 0.70f).compositeOver(Color(0xFFFF80B5)),
        gradientEnd = primary,
    )
}

private fun Color.compositeOver(background: Color): Color {
    val foregroundAlpha = alpha
    val backgroundAlpha = background.alpha
    val outputAlpha = foregroundAlpha + backgroundAlpha * (1f - foregroundAlpha)

    if (outputAlpha == 0f) return Color.Transparent

    return Color(
        red = (red * foregroundAlpha + background.red * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        green = (green * foregroundAlpha + background.green * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        blue = (blue * foregroundAlpha + background.blue * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        alpha = outputAlpha,
    )
}

