package com.example

import com.example.model.CameraSettings
import com.example.model.VideoResolution
import com.example.vision.LandmarkOneEuroSmoother
import com.example.vision.OneEuroFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OneEuroFilterTest {

    @Test
    fun oneEuroFilter_reducesHighFrequencyJitter() {
        val filter = OneEuroFilter(freq = 30.0, minCutoff = 0.5, beta = 0.02)
        val outputs = mutableListOf<Double>()
        for (i in 0..20) {
            val noisy = if (i % 2 == 0) 0.52 else 0.48
            outputs.add(filter.filter(noisy, i * (1.0 / 30.0)))
        }
        val lastDeviation = abs(outputs.last() - 0.5)
        assertTrue("Expected smoothed value near 0.5, got ${outputs.last()}", lastDeviation < 0.015)
    }

    @Test
    fun landmarkSmoother_preservesLandmarkCountAndBounds() {
        val smoother = LandmarkOneEuroSmoother()
        val raw = FloatArray(478 * 2) { idx -> if (idx % 2 == 0) 0.45f else 0.55f }
        val smoothed = smoother.smooth(raw, 478, 1000L, 0.65f)
        assertEquals(478 * 2, smoothed.size)
        assertEquals(0.45f, smoothed[0], 0.001f)
        assertEquals(0.55f, smoothed[1], 0.001f)
    }

    @Test
    fun defaultCameraSettings_matchStartupPreset() {
        val defaults = CameraSettings()
        assertTrue(defaults.isFrontCamera)
        assertTrue(defaults.isMirrored)
        assertEquals(VideoResolution.HD_720P, defaults.resolution)
    }
}
