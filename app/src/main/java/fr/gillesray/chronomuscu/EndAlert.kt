package fr.gillesray.chronomuscu

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Son et vibration de fin de décompte. Les sons sont synthétisés : aucun fichier audio. */
object EndAlert {

    private const val SAMPLE_RATE = 44_100

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Joue l'alerte et rend la main quand elle est terminée. */
    suspend fun play(context: Context, sound: EndSound, vibrate: Boolean, duck: Boolean) {
        if (vibrate) vibrate(context)
        if (sound == EndSound.AUCUN) {
            if (vibrate) delay(1_200)
            return
        }

        val samples = when (sound) {
            EndSound.BIP -> bip()
            EndSound.BOIS -> bois()
            EndSound.AUCUN -> return
        }

        val audioManager = context.getSystemService(AudioManager::class.java)
        val focusRequest = if (duck) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .build()
                .also { audioManager.requestAudioFocus(it) }
        } else {
            null
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        try {
            track.write(samples, 0, samples.size)
            track.play()
            delay(samples.size * 1000L / SAMPLE_RATE + 150)
        } finally {
            track.stop()
            track.release()
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
    }

    /** Aperçu dans les réglages. */
    suspend fun preview(context: Context, sound: EndSound) = play(context, sound, vibrate = false, duck = false)

    private fun vibrate(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 600), -1))
    }

    /** Trois bips à 880 Hz, le dernier plus long. */
    private fun bip(): ShortArray = sequence(
        listOf(0.0 to 0.15, 0.30 to 0.15, 0.60 to 0.45),
        totalSeconds = 1.1,
    ) { t, length ->
        val envelope = min(1.0, min(t, length - t) / 0.005)
        0.6 * envelope * sin(2 * PI * 880.0 * t)
    }

    /** Trois coups de bloc de bois : attaque sèche, décroissance rapide. */
    private fun bois(): ShortArray = sequence(
        listOf(0.0 to 0.12, 0.30 to 0.12, 0.60 to 0.12),
        totalSeconds = 0.8,
    ) { t, _ ->
        val envelope = exp(-t * 45.0)
        envelope * (0.7 * sin(2 * PI * 1_150.0 * t) + 0.3 * sin(2 * PI * 2_430.0 * t))
    }

    /** Place des notes (début, durée en secondes) sur une piste mono 16 bits. */
    private fun sequence(
        notes: List<Pair<Double, Double>>,
        totalSeconds: Double,
        wave: (t: Double, length: Double) -> Double,
    ): ShortArray {
        val out = ShortArray((totalSeconds * SAMPLE_RATE).toInt())
        for ((start, length) in notes) {
            val first = (start * SAMPLE_RATE).toInt()
            val count = (length * SAMPLE_RATE).toInt()
            for (i in 0 until count) {
                val index = first + i
                if (index >= out.size) break
                val value = wave(i.toDouble() / SAMPLE_RATE, length).coerceIn(-1.0, 1.0)
                out[index] = (value * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }
}
