package com.music.innertube

/**
 * Live YouTube search smoke checks are intentionally not JUnit tests.
 *
 * They depend on an external service and can block a deterministic CI test run.
 * Keep manual network checks in MainTest.kt, which is not discovered as a JUnit test.
 */
