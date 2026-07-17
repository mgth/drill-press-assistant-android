package fr.mgth.drillpress.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MachineEditTest {
    @Test
    fun addStepCreatesMatchingBeltPosition() {
        val m = createTwoShaftMachine()
        val motor = m.shafts[0].stacks[0]
        val spindle = m.shafts[1].stacks[0]
        // Étage 6 des deux côtés : la position (5,5) doit apparaître au 2e ajout.
        addStackStep(m, motor)
        assertFalse((5 to 5) in m.belts[0].allowedPairs)
        addStackStep(m, spindle)
        assertTrue((5 to 5) in m.belts[0].allowedPairs)
        assertEquals(m.belts[0].allowedPairs.size, m.belts[0].pairNames!!.size)
        assertEquals("F", m.belts[0].pairNames!!.last())
        // Le nouvel étage reprend le diamètre du dernier.
        assertEquals(48.0, motor.steps.last(), 1e-9)
    }

    @Test
    fun addStepWithoutCounterpartAddsNoPosition() {
        val m = createTwoShaftMachine()
        val before = m.belts[0].allowedPairs.toList()
        addStackStep(m, m.shafts[0].stacks[0])
        assertEquals(before, m.belts[0].allowedPairs)
    }

    @Test
    fun sharedIntermediateConeFeedsBothBelts() {
        val m = createThreeShaftMachine()
        setSharedIntermediate(m, 1, true)
        val shared = m.shafts[1].stacks[m.belts[0].toStack]
        // Les deux cônes voisins (moteur et broche) reçoivent d'abord leur étage 5.
        addStackStep(m, m.shafts[0].stacks[0])
        addStackStep(m, m.shafts[2].stacks[0])
        addStackStep(m, shared)
        assertTrue((4 to 4) in m.belts[0].allowedPairs)
        assertTrue((4 to 4) in m.belts[1].allowedPairs)
    }
}
