package dev.pastel.pastelboard.ui.components

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.R
import dev.pastel.pastelboard.model.PastelPalette

@Composable
fun PastelBackground(
    palette: PastelPalette,
    backgroundImageUri: String? = null,
    backgroundImageOpacity: Float = 0.34f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        palette.background,
                        palette.surface,
                        palette.keyTop,
                    ),
                ),
            ),
    ) {
        backgroundImageUri?.let { uri ->
            Image(
                painter = rememberAsyncUriPainter(uri),
                contentDescription = "自定义背景",
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(18.dp) else Modifier)
                    .background(Color.Transparent),
                contentScale = ContentScale.Crop,
                alpha = backgroundImageOpacity.coerceIn(0f, 1f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                palette.background.copy(alpha = 0.36f),
                                palette.surface.copy(alpha = 0.30f),
                                palette.keyTop.copy(alpha = 0.26f),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(30.dp),
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        ),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        ),
        content = {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(30.dp),
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.32f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.26f),
                            ),
                        ),
                    )
                    .padding(contentPadding),
                content = content,
            )
        },
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(cornerRadius.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.38f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.48f),
                shape = RoundedCornerShape(cornerRadius.dp),
            ),
        content = content,
    )
}

@Composable
private fun rememberAsyncUriPainter(uri: String): Painter {
    val fallback = painterResource(R.drawable.app_icon)
    val context = LocalContext.current
    var imageBitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        imageBitmap = runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
            ImageDecoder.decodeBitmap(source).asImageBitmap()
        }.getOrNull()
    }

    return imageBitmap?.let { BitmapPainter(it) } ?: fallback
}
