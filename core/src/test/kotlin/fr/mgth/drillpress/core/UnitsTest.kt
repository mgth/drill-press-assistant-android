package fr.mgth.drillpress.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class UnitsTest {
    @Test
    fun convertsInchAndMm() {
        assertEquals(25.4, 1 * MM_PER_INCH, 1e-6)
        assertEquals(12.7, IMPERIAL_DRILLS.first { it.label == "1/2" }.mm, 1e-6)
        assertEquals(25.4, IMPERIAL_DRILLS.first { it.label == "1" }.mm, 1e-6)
    }

    @Test
    fun convertsMPerMinToSfm() {
        assertEquals(98.43, 30 * SFM_PER_M_MIN, 0.05)
    }

    @Test
    fun imperialVcGridStaysMPerMinAndIncludesMaterials() {
        val metric = vcChipValues(carbide = false, imperial = false)
        val imperial = vcChipValues(carbide = false, imperial = true)
        for (sfm in SFM_GRID)
            assertTrue(imperial.any { abs(it * SFM_PER_M_MIN - sfm) < 0.01 })
        assertTrue(metric.contains(25.0))
        assertTrue(imperial.contains(25.0))
    }
}
