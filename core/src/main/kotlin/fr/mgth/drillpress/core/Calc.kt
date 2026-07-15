package fr.mgth.drillpress.core

/** Une position de courroies complète et la vitesse de broche qui en résulte. */
data class Combination(
    /** pairs[k] = paire (iMenant, iMené) choisie pour belts[k]. */
    val pairs: List<Pair<Int, Int>>,
    /** pairIndexes[k] = indice de la paire dans belts[k].allowedPairs (→ repère). */
    val pairIndexes: List<Int>,
    /** d_menant / d_mené par courroie. */
    val ratios: List<Double>,
    /** motorRpm × Π ratios, en tr/min. */
    val spindleRpm: Double,
)

/** Vitesse de chaque arbre (tr/min), ordonnée moteur → broche, pour une position donnée. */
fun shaftRpms(m: Machine, pairs: List<Pair<Int, Int>>): List<Double> {
    val rpms = mutableListOf(m.motorRpm)
    m.belts.forEachIndexed { k, belt ->
        val (i, j) = pairs[k]
        val dFrom = m.shafts[belt.fromShaft].stacks[belt.fromStack].steps[i]
        val dTo = m.shafts[belt.toShaft].stacks[belt.toStack].steps[j]
        rpms.add(rpms[k] * (dFrom / dTo))
    }
    return rpms
}

/**
 * Quand deux courroies consécutives partagent le même cône (arbre intermédiaire
 * à cône unique), elles ne peuvent pas occuper le même étage.
 */
private fun hasSharedStepConflict(m: Machine, pairs: List<Pair<Int, Int>>): Boolean {
    for (k in 1 until m.belts.size) {
        val prev = m.belts[k - 1]
        val cur = m.belts[k]
        if (cur.fromShaft == prev.toShaft &&
            cur.fromStack == prev.toStack &&
            pairs[k].first == pairs[k - 1].second
        ) return true
    }
    return false
}

private data class Partial(
    val pairs: List<Pair<Int, Int>>,
    val pairIndexes: List<Int>,
    val ratios: List<Double>,
    val rpm: Double,
)

/**
 * Toutes les combinaisons de positions de courroies (produit cartésien des
 * paires autorisées, moins celles où deux courroies se disputent le même étage
 * d'un cône partagé), triées par vitesse de broche croissante.
 */
fun enumerateCombinations(m: Machine): List<Combination> {
    var partials = listOf(Partial(emptyList(), emptyList(), emptyList(), m.motorRpm))

    for (belt in m.belts) {
        val fromSteps = m.shafts[belt.fromShaft].stacks[belt.fromStack].steps
        val toSteps = m.shafts[belt.toShaft].stacks[belt.toStack].steps
        partials = partials.flatMap { p ->
            belt.allowedPairs.mapIndexed { pairIndex, pair ->
                val (i, j) = pair
                val ratio = fromSteps[i] / toSteps[j]
                Partial(
                    pairs = p.pairs + (i to j),
                    pairIndexes = p.pairIndexes + pairIndex,
                    ratios = p.ratios + ratio,
                    rpm = p.rpm * ratio,
                )
            }
        }
    }

    return partials
        .filter { !hasSharedStepConflict(m, it.pairs) }
        .map { Combination(it.pairs, it.pairIndexes, it.ratios, it.rpm) }
        .sortedBy { it.spindleRpm }
}
