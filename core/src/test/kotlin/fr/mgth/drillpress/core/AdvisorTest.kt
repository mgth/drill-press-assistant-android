package fr.mgth.drillpress.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun twoShaft(motorRpm: Double, motorSteps: List<Double>, spindleSteps: List<Double>): Machine {
    val motor = PulleyStack("m", "Moteur", motorSteps.toMutableList())
    val spindle = PulleyStack("s", "Broche", spindleSteps.toMutableList())
    return Machine(
        "test", "Test", motorRpm,
        mutableListOf(
            Shaft("sh0", "Moteur", mutableListOf(motor)),
            Shaft("sh1", "Broche", mutableListOf(spindle)),
        ),
        mutableListOf(Belt(0, 0, 1, 0, defaultPairs(motor, spindle))),
    )
}

class AdvisorTest {
    @Test
    fun idealSteel() {
        val vc = materialById("steel")!!.vcHss
        assertEquals(795.77, idealRpm(vc, 10.0), 0.05)
    }

    @Test
    fun idealDoublesWhenDiameterHalved() {
        assertEquals(idealRpm(25.0, 10.0) * 2, idealRpm(25.0, 5.0), 1e-6)
    }

    private val m = twoShaft(1420.0, listOf(100.0, 74.0, 48.0), listOf(48.0, 74.0, 100.0))

    @Test
    fun closestBelowWhenOverspeedTooExpensive() {
        val r = recommend(m, 70.0, 14.0)
        assertEquals(1420, Math.round(r.best.spindleRpm).toInt())
        assertFalse(r.overspeed)
    }

    @Test
    fun acceptsSlightOverspeedInGap() {
        val gap = twoShaft(1000.0, listOf(72.0, 122.0), listOf(100.0, 100.0))
        val r = recommend(gap, 11 * Math.PI, 10.0) // idéal = 1100
        assertEquals(1220, Math.round(r.best.spindleRpm).toInt())
        assertFalse(r.overspeed)
    }

    @Test
    fun staysBelowWhenIdealNearLower() {
        val gap = twoShaft(1000.0, listOf(72.0, 122.0), listOf(100.0, 100.0))
        val r = recommend(gap, 8 * Math.PI, 10.0) // idéal = 800
        assertEquals(720, Math.round(r.best.spindleRpm).toInt())
    }

    @Test
    fun slowestAndOverspeedWhenAllTooFast() {
        val r = recommend(m, 12.0, 30.0) // idéal ≈ 127
        assertEquals(682, Math.round(r.best.spindleRpm).toInt())
        assertTrue(r.overspeed)
    }

    @Test
    fun returnsAllSorted() {
        val r = recommend(m, 25.0, 10.0)
        assertEquals(listOf(682, 1420, 2958), r.all.map { Math.round(it.spindleRpm).toInt() })
    }

    @Test
    fun formatsSignedDeviation() {
        assertEquals("+11 %", formatDeviation(1220.0, 1100.0))
        assertEquals("−35 %", formatDeviation(720.0, 1100.0))
        assertEquals("+0 %", formatDeviation(1100.0, 1100.0))
    }

    @Test
    fun diameterRangesBoundary() {
        val gap = twoShaft(1000.0, listOf(72.0, 122.0), listOf(100.0, 100.0))
        val vc = 11 * Math.PI
        val all = enumerateCombinations(gap)
        val ranges = diameterRanges(all, vc)
        val dStar = 11000 / ((720.0 + 2 * 1220.0) / 3)
        assertEquals(dStar, ranges[0]!!.min!!, 1e-6)
        assertNull(ranges[0]!!.max)
        assertNull(ranges[1]!!.min)
        assertEquals(dStar, ranges[1]!!.max!!, 1e-6)
        assertEquals(1220, Math.round(recommend(gap, vc, dStar - 0.01).best.spindleRpm).toInt())
        assertEquals(720, Math.round(recommend(gap, vc, dStar + 0.01).best.spindleRpm).toInt())
    }

    @Test
    fun diameterRangesDuplicateSpeed() {
        val motor = PulleyStack("m", "M", mutableListOf(100.0, 50.0))
        val spindle = PulleyStack("s", "S", mutableListOf(50.0, 25.0))
        val m2 = Machine(
            "t", "t", 1000.0,
            mutableListOf(
                Shaft("a", "Moteur", mutableListOf(motor)),
                Shaft("b", "Broche", mutableListOf(spindle)),
            ),
            mutableListOf(Belt(0, 0, 1, 0, mutableListOf(0 to 0, 1 to 1))),
        )
        val ranges = diameterRanges(enumerateCombinations(m2), 25.0)
        assertEquals(DiameterRange(null, null), ranges[0])
        assertNull(ranges[1])
    }
}
