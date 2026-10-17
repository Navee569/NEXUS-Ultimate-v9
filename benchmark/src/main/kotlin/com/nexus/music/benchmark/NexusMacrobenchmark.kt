package com.nexus.music.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NexusMacrobenchmark {
  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun coldStartup() {
    benchmarkRule.measureRepeated(
      packageName = "com.nexus.music.private",
      metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
      compilationMode = CompilationMode.None(),
      iterations = 5,
      startupMode = StartupMode.COLD,
      setupBlock = { pressHome() },
      measureBlock = { startActivityAndWait() },
    )
  }

  @Test
  fun homeScrollFrames() {
    benchmarkRule.measureRepeated(
      packageName = "com.nexus.music.private",
      metrics = listOf(FrameTimingMetric()),
      compilationMode = CompilationMode.None(),
      iterations = 5,
      setupBlock = {
        pressHome()
        startActivityAndWait()
      },
      measureBlock = {
        repeat(4) {
          device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 0.78f).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * 0.28f).toInt(),
            12,
          )
        }
      },
    )
  }

  @Test
  fun playerOpenCloseFrames() {
    benchmarkRule.measureRepeated(
      packageName = "com.nexus.music.private",
      metrics = listOf(FrameTimingMetric()),
      compilationMode = CompilationMode.None(),
      iterations = 5,
      setupBlock = {
        pressHome()
        startActivityAndWait()
      },
      measureBlock = {
        // Exercise the lower player/navigation region without depending on localized labels.
        device.click(device.displayWidth / 2, (device.displayHeight * 0.88f).toInt())
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
      },
    )
  }
}
