package fr.mgth.drillpress.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow

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

class CalcTest {
    @Test
    fun simpleRatioTwoShafts() {
        val m = twoShaft(1420.0, listOf(60.0), listOf(120.0))
        val combos = enumerateCombinations(m)
        assertEquals(1, combos.size)
        assertEquals(710.0, combos[0].spindleRpm, 1e-5)
        assertEquals(listOf(0.5), combos[0].ratios)
    }

    @Test
    fun enumeratesAndSortsAscending() {
        val m = twoShaft(1420.0, listOf(100.0, 74.0, 48.0), listOf(48.0, 74.0, 100.0))
        val combos = enumerateCombinations(m)
        assertEquals(3, combos.size)
        assertEquals(listOf(682, 1420, 2958), combos.map { Math.round(it.spindleRpm).toInt() })
        assertEquals(listOf(2 to 2), combos[0].pairs)
        assertEquals(listOf(0 to 0), combos[2].pairs)
    }

    @Test
    fun chainsThreeShafts() {
        val m = createThreeShaftMachine()
        val combos = enumerateCombinations(m)
        assertEquals(16, combos.size)
        assertEquals(1420.0 * (50.0 / 110.0).pow(2), combos[0].spindleRpm, 1e-6)
        assertEquals(1420.0 * (110.0 / 50.0).pow(2), combos[15].spindleRpm, 1e-6)
        for (i in 1 until combos.size)
            assertTrue(combos[i].spindleRpm >= combos[i - 1].spindleRpm)
    }

    @Test
    fun shaftRpmsPerAxis() {
        val m = createThreeShaftMachine()
        val rpms = shaftRpms(m, listOf(0 to 0, 0 to 0))
        assertEquals(3, rpms.size)
        assertEquals(1420.0, rpms[0])
        assertEquals(1420.0 * (110.0 / 50.0), rpms[1], 1e-6)
        assertEquals(1420.0 * (110.0 / 50.0).pow(2), rpms[2], 1e-6)
    }

    @Test
    fun defaultPairsSameLevel() {
        val a = PulleyStack("a", "A", mutableListOf(100.0, 80.0, 60.0))
        val b = PulleyStack("b", "B", mutableListOf(60.0, 80.0, 100.0))
        assertEquals(listOf(0 to 0, 1 to 1, 2 to 2), defaultPairs(a, b))
    }

    @Test
    fun defaultPairsDifferentCounts() {
        val a = PulleyStack("a", "A", mutableListOf(100.0, 80.0))
        val b = PulleyStack("b", "B", mutableListOf(60.0, 80.0, 100.0))
        assertEquals(emptyList<Pair<Int, Int>>(), defaultPairs(a, b))
    }

    private fun sharedMachine(): Machine {
        val m = createThreeShaftMachine()
        setSharedIntermediate(m, 1, true)
        return m
    }

    @Test
    fun sharedMergesCones() {
        val m = sharedMachine()
        assertEquals(1, m.shafts[1].stacks.size)
        assertEquals(0, m.belts[0].toStack)
        assertEquals(0, m.belts[1].fromStack)
        assertTrue(isSharedIntermediate(m, 1))
        assertTrue(validateMachine(m).none { it.level == IssueLevel.ERROR })
    }

    @Test
    fun sharedExcludesSameStep() {
        val combos = enumerateCombinations(sharedMachine())
        assertEquals(12, combos.size)
        for (c in combos) assertNotEquals(c.pairs[0].second, c.pairs[1].first)
    }

    @Test
    fun doubleConeNoFilter() {
        val m = sharedMachine()
        setSharedIntermediate(m, 1, false)
        assertEquals(2, m.shafts[1].stacks.size)
        assertFalse(isSharedIntermediate(m, 1))
        assertEquals(16, enumerateCombinations(m).size)
    }

    @Test
    fun reportsNoCombination() {
        val m = sharedMachine()
        m.belts[0].allowedPairs = mutableListOf(0 to 2)
        m.belts[1].allowedPairs = mutableListOf(2 to 0)
        assertEquals(0, enumerateCombinations(m).size)
        assertTrue(validateMachine(m).any { it.level == IssueLevel.ERROR })
    }

    @Test
    fun pairNameUsesEngravedOrNumber() {
        val m = createTwoShaftMachine()
        assertEquals("B", pairName(m.belts[0], 1))
        m.belts[0].pairNames = null
        assertEquals("2", pairName(m.belts[0], 1))
        m.belts[0].pairNames = mutableListOf("", " ", "X", "", "")
        assertEquals("1", pairName(m.belts[0], 0))
        assertEquals("X", pairName(m.belts[0], 2))
    }

    @Test
    fun defaultPairNamesDigitsThenLetters() {
        assertEquals(listOf("1", "2", "3"), defaultPairNames(0, 3))
        assertEquals(listOf("A", "B", "C"), defaultPairNames(1, 3))
    }

    @Test
    fun ensurePairNamesFills() {
        val m = createThreeShaftMachine()
        m.belts[0].pairNames = null
        m.belts[1].pairNames = mutableListOf("A", "B")
        ensurePairNames(m)
        assertEquals(listOf("1", "2", "3", "4"), m.belts[0].pairNames)
        assertEquals(listOf("A", "B", "C", "D"), m.belts[1].pairNames)
    }

    @Test
    fun tracesPairIndex() {
        val m = createTwoShaftMachine()
        for (c in enumerateCombinations(m))
            assertEquals(m.belts[0].allowedPairs[c.pairIndexes[0]], c.pairs[0])
    }

    @Test
    fun acceptsTemplates() {
        assertTrue(validateMachine(createTwoShaftMachine()).isEmpty())
        assertTrue(validateMachine(createThreeShaftMachine()).isEmpty())
    }

    @Test
    fun rejectsZeroDiameterAndNoPosition() {
        val m = twoShaft(1420.0, listOf(0.0), listOf(100.0))
        m.belts[0].allowedPairs = mutableListOf()
        assertEquals(2, validateMachine(m).count { it.level == IssueLevel.ERROR })
    }

    @Test
    fun warnsOnDiameterSumVariation() {
        val m = twoShaft(1420.0, listOf(100.0, 74.0, 48.0), listOf(48.0, 74.0, 200.0))
        assertTrue(validateMachine(m).any { it.level == IssueLevel.WARNING })
    }
}
