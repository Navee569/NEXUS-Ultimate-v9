package echo.music.dsp.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayGainAudioProcessorTest {
  @Test
  fun parsesReplayGainDbWithUnit() {
    assertEquals(-7.25f, ReplayGainAudioProcessor.parseGainDb("-7.25 dB")!!, 0.001f)
  }

  @Test
  fun acceptsNumericGain() {
    assertEquals(3.0f, ReplayGainAudioProcessor.parseGainDb(3.0)!!, 0.001f)
  }

  @Test
  fun rejectsInvalidAndUnsafeGain() {
    assertNull(ReplayGainAudioProcessor.parseGainDb("not-a-number"))
    assertNull(ReplayGainAudioProcessor.parseGainDb(90.0f))
    assertNull(ReplayGainAudioProcessor.parseGainDb(Float.NaN))
  }
}
