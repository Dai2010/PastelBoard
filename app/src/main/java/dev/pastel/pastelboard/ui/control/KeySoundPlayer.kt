package dev.pastel.pastelboard.ui.control

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.pastel.pastelboard.R

class KeySoundPlayer(
    private val context: Context,
    soundUri: String? = null,
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var loaded = false
    private val soundId = loadSound(soundUri)

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            loaded = sampleId == soundId && status == 0
        }
    }

    private fun loadSound(soundUri: String?): Int {
        if (soundUri.isNullOrBlank()) {
            return soundPool.load(context, R.raw.key_flick, 1)
        }

        return runCatching {
            context.contentResolver.openAssetFileDescriptor(Uri.parse(soundUri), "r")?.use { descriptor ->
                soundPool.load(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length, 1)
            } ?: soundPool.load(context, R.raw.key_flick, 1)
        }.getOrElse {
            soundPool.load(context, R.raw.key_flick, 1)
        }
    }

    fun play(enabled: Boolean) {
        if (!enabled || !loaded) return
        soundPool.play(soundId, 0.48f, 0.48f, 1, 0, 1.08f)
    }

    fun release() {
        soundPool.release()
    }
}

@Composable
fun rememberKeySoundPlayer(enabled: Boolean, soundUri: String? = null): () -> Unit {
    val context = LocalContext.current.applicationContext
    val player = remember(soundUri) { KeySoundPlayer(context, soundUri) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    return remember(enabled, player) {
        { player.play(enabled) }
    }
}
