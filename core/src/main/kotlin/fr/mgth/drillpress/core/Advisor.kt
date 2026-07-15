package fr.mgth.drillpress.core

import kotlin.math.abs

/** Vitesse de rotation idéale (tr/min) pour une Vc (m/min) et un Ø de foret (mm). */
fun idealRpm(vcMPerMin: Double, drillDiameterMm: Double): Double =
    (vcMPerMin * 1000) / (Math.PI * drillDiameterMm)

/** Écart relatif signé à la vitesse idéale (+0,11 = 11 % trop vite). */
fun deviation(rpm: Double, ideal: Double): Double = (rpm - ideal) / ideal

/** Écart formaté pour l'affichage, ex. « +11 % » ou « −35 % ». */
fun formatDeviation(rpm: Double, ideal: Double): String {
    val pct = Math.round(deviation(rpm, ideal) * 100).toInt()
    val sign = if (pct >= 0) "+" else "−"
    return "$sign${abs(pct)} %"
}

/**
 * Dépasser la vitesse de coupe use l'outil alors qu'être en dessous ne fait que
 * ralentir : le dépassement compte double dans le choix de la combinaison.
 */
const val OVER_PENALTY_FACTOR = 2.0

data class Recommendation(
    val ideal: Double,
    /** Combinaison retenue. */
    val best: Combination,
    /** Toutes les combinaisons, triées par vitesse croissante. */
    val all: List<Combination>,
    /** true si même la combinaison la plus lente dépasse la vitesse idéale. */
    val overspeed: Boolean,
)

data class DiameterRange(
    /** Ø mini en mm, null = jusqu'aux plus petits forets. */
    val min: Double?,
    /** Ø maxi en mm, null = sans limite haute. */
    val max: Double?,
)

/**
 * Pour chaque combinaison, la plage de diamètres pour laquelle elle serait la
 * recommandée avec cette Vc. Bascule entre r₁ < r₂ à l'idéal I* = (r₁ + 2·r₂)/3
 * (dépassement compté double), converti en diamètre. null = doublon de vitesse.
 */
fun diameterRanges(all: List<Combination>, vc: Double): List<DiameterRange?> {
    val result = MutableList<DiameterRange?>(all.size) { null }
    val winners = mutableListOf<Int>()
    for (i in all.indices) {
        val prev = if (winners.isNotEmpty()) all[winners.last()].spindleRpm else null
        if (prev == null || all[i].spindleRpm - prev > prev * 1e-9) winners.add(i)
    }
    fun toDiameter(ideal: Double) = (vc * 1000) / (Math.PI * ideal)
    winners.forEachIndexed { k, comboIndex ->
        val rpm = all[comboIndex].spindleRpm
        val slower = if (k > 0) all[winners[k - 1]].spindleRpm else null
        val faster = if (k < winners.size - 1) all[winners[k + 1]].spindleRpm else null
        val idealLow = if (slower == null) null else (slower + 2 * rpm) / 3
        val idealHigh = if (faster == null) null else (rpm + 2 * faster) / 3
        result[comboIndex] = DiameterRange(
            min = idealHigh?.let { toDiameter(it) },
            max = idealLow?.let { toDiameter(it) },
        )
    }
    return result
}

/**
 * Règle : la combinaison la plus proche de la vitesse idéale, le dépassement
 * étant pénalisé double. `overspeed` si même la plus lente dépasse l'idéal.
 */
fun recommend(m: Machine, vc: Double, drillDiameterMm: Double): Recommendation {
    val ideal = idealRpm(vc, drillDiameterMm)
    val all = enumerateCombinations(m)
    fun penalty(c: Combination): Double {
        val dev = deviation(c.spindleRpm, ideal)
        return if (dev >= 0) dev * OVER_PENALTY_FACTOR else -dev
    }
    val best = all.reduce { a, b -> if (penalty(b) < penalty(a)) b else a }
    return Recommendation(ideal, best, all, all.isNotEmpty() && all[0].spindleRpm > ideal)
}
