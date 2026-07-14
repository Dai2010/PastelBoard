package dev.pastel.pastelboard.ui.control

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import dev.pastel.pastelboard.R

class KeySoundPlayer(
    private val context: Context,
    private val view: View,
    soundUri: String? = null,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(audioAttributes)
        .build()

    private var loaded = false
    private var soundId = 0
    private var customSoundDescriptor: AssetFileDescriptor? = null

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            loaded = sampleId == soundId && status == 0
        }
        soundId = loadSound(soundUri)
    }

    private fun loadSound(soundUri: String?): Int {
        if (soundUri.isNullOrBlank()) {
            return soundPool.load(context, R.raw.key_flick, 1)
        }

        return runCatching {
            customSoundDescriptor = context.contentResolver.openAssetFileDescriptor(Uri.parse(soundUri), "r")
            val descriptor = customSoundDescriptor
            descriptor?.let {
                soundPool.load(it.fileDescriptor, it.startOffset, it.length, 1)
            } ?: soundPool.load(context, R.raw.key_flick, 1)
        }.getOrElse {
            customSoundDescriptor?.close()
            customSoundDescriptor = null
            soundPool.load(context, R.raw.key_flick, 1)
        }
    }

    fun play(enabled: Boolean) {
        if (!enabled) return

        if (loaded && soundPool.play(soundId, 0.78f, 0.78f, 1, 0, 1.08f) != 0) return
        if (playMediaFallback()) return

        audioManager?.playSoundEffect(SoundEffectConstants.CLICK, 0.8f)
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    private fun playMediaFallback(): Boolean {
        return runCatching {
            val player = MediaPlayer.create(context, R.raw.key_flick) ?: return false
            player.setOnCompletionListener { finishedPlayer -> finishedPlayer.release() }
            player.setOnErrorListener { errorPlayer, _, _ ->
                errorPlayer.release()
                true
            }
            player.setVolume(0.9f, 0.9f)
            player.start()
            true
        }.getOrDefault(false)
    }

    fun release() {
        soundPool.release()
        customSoundDescriptor?.close()
        customSoundDescriptor = null
    }
}

@Composable
fun rememberKeySoundPlayer(enabled: Boolean, soundUri: String? = null): () -> Unit {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current
    val player = remember(soundUri, view) { KeySoundPlayer(context, view, soundUri) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    return remember(enabled, player) {
        { player.play(enabled) }
    }
}
