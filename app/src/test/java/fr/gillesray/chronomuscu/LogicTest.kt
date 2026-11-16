package fr.gillesray.chronomuscu

import org.junit.Assert.assertEquals
import org.junit.Test

class LogicTest {

    @Test
    fun formatClock_roundsUpToTheSecond() {
        assertEquals("1:30", formatClock(90_000))
        assertEquals("0:05", formatClock(4_001))
        assertEquals("4:00", formatClock(240_000))
        assertEquals("0:01", formatClock(1))
    }

    @Test
    fun formatClock_neverNegative() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:00", formatClock(-3))
    }

    @Test
    fun formatPreset_usesMinuteAndSecondMarks() {
        assertEquals("25\"", formatPreset(25))
        assertEquals("1'00\"", formatPreset(60))
        assertEquals("1'30\"", formatPreset(90))
        assertEquals("4'00\"", formatPreset(240))
    }

    @Test
    fun remainingMs_clampsAtZero() {
        assertEquals(0L, remainingMs(endAt = 1_000, now = 1_500))
        assertEquals(500L, remainingMs(endAt = 1_500, now = 1_000))
    }

    @Test
    fun series_neverBelowZero() {
        assertEquals(0, Series.adjust(3, -5))
        assertEquals(8, Series.adjust(3, 5))
        assertEquals(2, Series.adjust(3, -1))
    }

    @Test
    fun presets_defaultsAndClamp() {
        assertEquals(listOf(25, 60, 90, 120, 180, 240), Presets.DEFAULTS)
        assertEquals(5, Presets.clamp(0))
        assertEquals(5999, Presets.clamp(10_000))
        assertEquals(90, Presets.clamp(90))
    }
}
