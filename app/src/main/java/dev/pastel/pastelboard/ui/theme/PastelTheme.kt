package dev.pastel.pastelboard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.pastel.pastelboard.R
import dev.pastel.pastelboard.model.PastelPalette

val PastelFontFamily = FontFamily(
    Font(R.font.lxgw_wenkai_regular, FontWeight.Normal),
)

@Composable
fun PastelBoardTheme(
    palette: PastelPalette,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> palette.darkScheme()
        else -> palette.lightScheme()
    }

    val typography = Typography().let { base ->
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = PastelFontFamily),
            displayMedium = base.displayMedium.copy(fontFamily = PastelFontFamily),
            displaySmall = base.displaySmall.copy(fontFamily = PastelFontFamily),
            headlineLarge = base.headlineLarge.copy(fontFamily = PastelFontFamily),
            headlineMedium = base.headlineMedium.copy(fontFamily = PastelFontFamily),
            headlineSmall = base.headlineSmall.copy(fontFamily = PastelFontFamily),
            titleLarge = base.titleLarge.copy(fontFamily = PastelFontFamily),
            titleMedium = base.titleMedium.copy(fontFamily = PastelFontFamily),
            titleSmall = base.titleSmall.copy(fontFamily = PastelFontFamily),
            bodyLarge = base.bodyLarge.copy(fontFamily = PastelFontFamily),
            bodyMedium = base.bodyMedium.copy(fontFamily = PastelFontFamily),
            bodySmall = base.bodySmall.copy(fontFamily = PastelFontFamily),
            labelLarge = base.labelLarge.copy(fontFamily = PastelFontFamily),
            labelMedium = base.labelMedium.copy(fontFamily = PastelFontFamily),
            labelSmall = base.labelSmall.copy(fontFamily = PastelFontFamily),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}

private fun PastelPalette.lightScheme(): ColorScheme {
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = keyTop,
        onPrimaryContainer = Color(0xFF32133E),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = surface,
        onSecondaryContainer = Color(0xFF3F1227),
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = keySide,
        onTertiaryContainer = Color(0xFF251255),
        background = background,
        onBackground = Color(0xFF241726),
        surface = surface,
        onSurface = Color(0xFF241726),
        surfaceVariant = keyTop,
        onSurfaceVariant = Color(0xFF5C435B),
        outline = Color(0xFF927392),
    )
}

private fun PastelPalette.darkScheme(): ColorScheme {
    return darkColorScheme(
        primary = keyTop,
        onPrimary = Color(0xFF4C145D),
        primaryContainer = primary.copy(alpha = 0.42f),
        onPrimaryContainer = Color(0xFFFFEAF8),
        secondary = secondary,
        onSecondary = Color(0xFF4A1029),
        secondaryContainer = secondary.copy(alpha = 0.34f),
        onSecondaryContainer = Color(0xFFFFE6F1),
        tertiary = tertiary,
        onTertiary = Color(0xFF251255),
        tertiaryContainer = tertiary.copy(alpha = 0.34f),
        onTertiaryContainer = Color(0xFFF0E8FF),
        background = Color(0xFF20151F),
        onBackground = Color(0xFFFFEFF8),
        surface = Color(0xFF2A1B29),
        onSurface = Color(0xFFFFEFF8),
        surfaceVariant = Color(0xFF4B394B),
        onSurfaceVariant = Color(0xFFEAD6E8),
        outline = Color(0xFFB99DB7),
    )
}

@Composable
fun keepStatusBarPastel() {
    val activity = LocalContext.current as? Activity ?: return
    activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
    activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
}

