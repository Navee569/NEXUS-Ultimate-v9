package echo.music.dsp.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * Applies ReplayGain metadata to decoded PCM. A missing/invalid tag is unity gain (0 dB).
 * Metadata lookup is deliberately outside the audio callback; gain changes are a volatile scalar.
 */
@UnstableApi
class ReplayGainAudioProcessor : AudioProcessor {
  @Volatile private var linearGain = 1.0
  private var active = false
  private var outputBuffer: ByteBuffer = EMPTY_BUFFER
  private var inputEnded = false

  fun setGainDb(gainDb: Float?) {
    linearGain = gainDb
      ?.takeIf { it.isFinite() && it in MIN_GAIN_DB..MAX_GAIN_DB }
      ?.let { 10.0.pow(it.toDouble() / 20.0) }
      ?: 1.0
  }

  override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
      throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
    }
    active = true
    return inputAudioFormat
  }

  override fun isActive(): Boolean = active

  override fun queueInput(inputBuffer: ByteBuffer) {
    val size = inputBuffer.remaining()
    if (size == 0) return

    val gain = linearGain
    if (gain == 1.0) {
      // No ReplayGain tag: pass the original buffer through without a copy.
      outputBuffer = inputBuffer
      return
    }

    if (outputBuffer.capacity() < size) {
      outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
    } else {
      outputBuffer.clear()
    }

    val sampleCount = size / Short.SIZE_BYTES
    repeat(sampleCount) {
      val sample = inputBuffer.getShort().toDouble() * gain
      outputBuffer.putShort(sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
    }
    outputBuffer.flip()
  }

  override fun getOutput(): ByteBuffer {
    val output = outputBuffer
    outputBuffer = EMPTY_BUFFER
    return output
  }

  override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

  @Deprecated("Deprecated in Java")
  override fun flush() {
    outputBuffer = EMPTY_BUFFER
    inputEnded = false
  }

  override fun reset() {
    flush()
    active = false
    linearGain = 1.0
  }

  override fun queueEndOfStream() {
    inputEnded = true
  }

  companion object {
    private const val MIN_GAIN_DB = -60f
    private const val MAX_GAIN_DB = 12f
    private val EMPTY_BUFFER = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

    /** Parses common ReplayGain tag forms such as "-7.25 dB" or a numeric dB value. */
    fun parseGainDb(value: Any?): Float? {
      val text = when (value) {
        is Number -> value.toString()
        is String -> value.trim().removeSuffix("dB").removeSuffix("db").trim()
        else -> return null
      }
      return text.toFloatOrNull()?.takeIf { it.isFinite() && it in MIN_GAIN_DB..MAX_GAIN_DB }
    }
  }
}
