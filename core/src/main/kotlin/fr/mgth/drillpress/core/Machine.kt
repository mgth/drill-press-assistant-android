package fr.mgth.drillpress.core

import java.util.UUID

/**
 * Modèle générique d'une transmission par courroies de perceuse à colonne :
 * une chaîne linéaire d'arbres (moteur → [intermédiaire] → broche), chaque
 * arbre portant un ou deux cônes de poulies étagées. Un arbre intermédiaire à
 * cône unique se modélise en faisant référencer le même cône par la courroie
 * d'entrée et celle de sortie.
 *
 * Comme le portage TypeScript d'origine, ces classes sont mutables : plusieurs
 * fonctions du domaine modifient la machine en place.
 */

/** Cône de poulies étagées. Diamètres en mm, index 0 = étage du HAUT. */
class PulleyStack(
    var id: String,
    var label: String,
    var steps: MutableList<Double>,
)

class Shaft(
    var id: String,
    var label: String,
    /** 1 cône (cas courant) ou 2 (arbre intermédiaire à double cône). */
    var stacks: MutableList<PulleyStack>,
)

class Belt(
    /** Côté menant : indices dans machine.shafts / shaft.stacks. */
    var fromShaft: Int,
    var fromStack: Int,
    /** Côté mené. */
    var toShaft: Int,
    var toStack: Int,
    /** Paires d'étages (iMenant, iMené) physiquement possibles pour la courroie. */
    var allowedPairs: MutableList<Pair<Int, Int>>,
    /** Repères des positions tels que gravés sur la machine, alignés sur allowedPairs. */
    var pairNames: MutableList<String>? = null,
)

class Machine(
    var id: String,
    var name: String,
    /** Vitesse moteur en tr/min (typ. 1420 ou 2800). */
    var motorRpm: Double,
    /** Ordonnés moteur → broche. belts[k] relie shafts[k] à shafts[k+1]. */
    var shafts: MutableList<Shaft>,
    var belts: MutableList<Belt>,
    /** Affichage du schéma en miroir : broche à gauche, moteur à droite. */
    var spindleLeft: Boolean = false,
)

fun pairName(belt: Belt, i: Int): String {
    val name = belt.pairNames?.getOrNull(i)?.trim()
    return if (!name.isNullOrEmpty()) name else (i + 1).toString()
}

/** Repères par défaut : chiffres pour la première courroie, lettres ensuite. */
fun defaultPairNames(beltIndex: Int, count: Int): MutableList<String> =
    (0 until count).map { i ->
        if (beltIndex == 0) (i + 1).toString() else ('A' + (i % 26)).toString()
    }.toMutableList()

/** Normalise une machine chargée depuis la persistance (données d'avant les repères). */
fun ensurePairNames(m: Machine) {
    m.belts.forEachIndexed { k, belt ->
        val defaults = defaultPairNames(k, belt.allowedPairs.size)
        val existing = belt.pairNames
        belt.pairNames = belt.allowedPairs.indices
            .map { i -> existing?.getOrNull(i) ?: defaults[i] }
            .toMutableList()
    }
}

enum class IssueLevel { ERROR, WARNING }

enum class IssueCode {
    MOTOR_RPM, MIN_SHAFTS, BELT_COUNT, EMPTY_STACK, BAD_DIAMETER, BELT_CHAIN,
    STACK_MISSING, NO_PAIRS, PAIR_OUT_OF_RANGE, DIAMETER_SUM, NO_COMBINATION,
}

/** Problème de validation : le message localisé est rendu côté UI d'après le code. */
data class Issue(
    val level: IssueLevel,
    val code: IssueCode,
    val params: Map<String, Any> = emptyMap(),
)

/** Libellés des gabarits, injectés par l'UI selon la langue (défaut : français). */
data class FactoryLabels(
    val twoShaftName: String,
    val threeShaftName: String,
    val motorShaft: String,
    val intermediateShaft: String,
    val spindleShaft: String,
    val motorCone: String,
    val spindleCone: String,
    val intermediateCone: String,
    val intermediateConeIn: String,
    val intermediateConeOut: String,
)

val DEFAULT_FACTORY_LABELS = FactoryLabels(
    twoShaftName = "Perceuse 2 arbres",
    threeShaftName = "Perceuse 3 arbres",
    motorShaft = "Moteur",
    intermediateShaft = "Intermédiaire",
    spindleShaft = "Broche",
    motorCone = "Cône moteur",
    spindleCone = "Cône broche",
    intermediateCone = "Cône intermédiaire",
    intermediateConeIn = "Cône intermédiaire (entrée)",
    intermediateConeOut = "Cône intermédiaire (sortie)",
)

fun newId(): String = UUID.randomUUID().toString()

/**
 * Appariement par défaut : étage i du cône menant ↔ étage i du cône mené.
 * Si les nombres d'étages diffèrent, aucune paire par défaut.
 */
fun defaultPairs(from: PulleyStack, to: PulleyStack): MutableList<Pair<Int, Int>> {
    if (from.steps.size != to.steps.size) return mutableListOf()
    return from.steps.indices.map { it to it }.toMutableList()
}

/**
 * Sur une vraie paire de cônes étagés, la somme des diamètres en vis-à-vis est
 * quasi constante. Une variation au-delà de cette tolérance signale une erreur.
 */
const val BELT_SUM_TOLERANCE = 0.1

fun validateMachine(m: Machine): List<Issue> {
    val issues = mutableListOf<Issue>()
    fun err(code: IssueCode, params: Map<String, Any> = emptyMap()) =
        issues.add(Issue(IssueLevel.ERROR, code, params))
    fun warn(code: IssueCode, params: Map<String, Any> = emptyMap()) =
        issues.add(Issue(IssueLevel.WARNING, code, params))

    if (!(m.motorRpm > 0)) err(IssueCode.MOTOR_RPM)
    if (m.shafts.size < 2) err(IssueCode.MIN_SHAFTS)
    if (m.belts.size != maxOf(0, m.shafts.size - 1)) err(IssueCode.BELT_COUNT)

    for (shaft in m.shafts) {
        for (stack in shaft.stacks) {
            if (stack.steps.isEmpty())
                err(IssueCode.EMPTY_STACK, mapOf("stack" to stack.label, "shaft" to shaft.label))
            if (stack.steps.any { !(it > 0) })
                err(IssueCode.BAD_DIAMETER, mapOf("stack" to stack.label, "shaft" to shaft.label))
        }
    }

    m.belts.forEachIndexed belt@{ k, belt ->
        val from = m.shafts.getOrNull(belt.fromShaft)?.stacks?.getOrNull(belt.fromStack)
        val to = m.shafts.getOrNull(belt.toShaft)?.stacks?.getOrNull(belt.toStack)
        if (belt.fromShaft != k || belt.toShaft != k + 1) {
            err(IssueCode.BELT_CHAIN, mapOf("belt" to k + 1, "from" to k + 1, "to" to k + 2))
            return@belt
        }
        if (from == null || to == null) {
            err(IssueCode.STACK_MISSING, mapOf("belt" to k + 1))
            return@belt
        }
        if (belt.allowedPairs.isEmpty()) {
            err(IssueCode.NO_PAIRS, mapOf("belt" to k + 1))
            return@belt
        }
        for ((i, j) in belt.allowedPairs) {
            if (i !in from.steps.indices || j !in to.steps.indices) {
                err(IssueCode.PAIR_OUT_OF_RANGE, mapOf("belt" to k + 1, "i" to i + 1, "j" to j + 1))
                return@belt
            }
        }
        val sums = belt.allowedPairs.map { (i, j) -> from.steps[i] + to.steps[j] }
        val min = sums.min()
        val max = sums.max()
        if (min > 0 && (max - min) / max > BELT_SUM_TOLERANCE)
            warn(
                IssueCode.DIAMETER_SUM,
                mapOf("belt" to k + 1, "tolerance" to Math.round(BELT_SUM_TOLERANCE * 100)),
            )
    }

    if (issues.none { it.level == IssueLevel.ERROR } && enumerateCombinations(m).isEmpty())
        err(IssueCode.NO_COMBINATION)

    return issues
}

/** true si l'arbre intermédiaire utilise le même cône pour l'entrée et la sortie. */
fun isSharedIntermediate(m: Machine, shaftIndex: Int): Boolean {
    val beltIn = m.belts.getOrNull(shaftIndex - 1)
    val beltOut = m.belts.getOrNull(shaftIndex)
    return beltIn != null && beltOut != null && beltIn.toStack == beltOut.fromStack
}

/**
 * Bascule un arbre intermédiaire entre cône unique partagé et double cône.
 */
fun setSharedIntermediate(
    m: Machine,
    shaftIndex: Int,
    shared: Boolean,
    labels: FactoryLabels = DEFAULT_FACTORY_LABELS,
) {
    val shaft = m.shafts.getOrNull(shaftIndex) ?: return
    if (shaftIndex <= 0 || shaftIndex >= m.shafts.size - 1) return
    val beltIn = m.belts[shaftIndex - 1]
    val beltOut = m.belts[shaftIndex]
    if (shared && shaft.stacks.size > 1) {
        shaft.stacks = mutableListOf(shaft.stacks[0])
        shaft.stacks[0].label = labels.intermediateCone
        beltIn.toStack = 0
        beltOut.fromStack = 0
    } else if (!shared && shaft.stacks.size == 1) {
        val src = shaft.stacks[0]
        src.label = labels.intermediateConeIn
        shaft.stacks = mutableListOf(
            src,
            PulleyStack(newId(), labels.intermediateConeOut, src.steps.toMutableList()),
        )
        beltIn.toStack = 0
        beltOut.fromStack = 1
    }
    syncBeltPairs(m)
}

/**
 * Après modification du nombre d'étages d'un cône : retire les positions hors
 * limites (et leurs repères) et, s'il n'en reste aucune, retente
 * l'appariement par défaut.
 */
fun syncBeltPairs(m: Machine) {
    m.belts.forEachIndexed { k, belt ->
        val from = m.shafts.getOrNull(belt.fromShaft)?.stacks?.getOrNull(belt.fromStack) ?: return@forEachIndexed
        val to = m.shafts.getOrNull(belt.toShaft)?.stacks?.getOrNull(belt.toStack) ?: return@forEachIndexed
        val inRange = belt.allowedPairs.map { (i, j) -> i < from.steps.size && j < to.steps.size }
        belt.allowedPairs = belt.allowedPairs.filterIndexed { i, _ -> inRange[i] }.toMutableList()
        belt.pairNames = belt.pairNames?.filterIndexed { i, _ -> inRange.getOrElse(i) { true } }?.toMutableList()
        if (belt.allowedPairs.isEmpty()) {
            belt.allowedPairs = defaultPairs(from, to)
            belt.pairNames = defaultPairNames(k, belt.allowedPairs.size)
        }
    }
}

/**
 * Ajoute un étage à un cône (diamètre du dernier étage repris) et crée la
 * position de courroie correspondante sur chaque courroie reliée dont le cône
 * d'en face possède déjà cet étage. Si le cône d'en face est plus court, la
 * position naîtra quand on lui ajoutera son propre étage.
 */
fun addStackStep(m: Machine, stack: PulleyStack) {
    val i = stack.steps.size
    stack.steps.add(stack.steps.lastOrNull() ?: 60.0)
    m.belts.forEachIndexed { k, belt ->
        val from = m.shafts.getOrNull(belt.fromShaft)?.stacks?.getOrNull(belt.fromStack) ?: return@forEachIndexed
        val to = m.shafts.getOrNull(belt.toShaft)?.stacks?.getOrNull(belt.toStack) ?: return@forEachIndexed
        val linked = (from === stack && to.steps.size > i) || (to === stack && from.steps.size > i)
        if (!linked || (i to i) in belt.allowedPairs) return@forEachIndexed
        belt.allowedPairs.add(i to i)
        belt.pairNames?.let { it.add(nextPairName(k, it)) }
    }
}

/**
 * Prochain repère libre dans le style des repères existants de la courroie
 * (lettres ou chiffres, y compris affichés en ordre inverse), sinon le repère
 * par défaut de la convention chiffres/lettres.
 */
private fun nextPairName(beltIndex: Int, names: List<String>): String {
    val trimmed = names.map { it.trim() }
    return when {
        trimmed.isNotEmpty() && trimmed.all { it.length == 1 && it[0] in 'A'..'Z' } ->
            ('A'..'Z').firstOrNull { it.toString() !in trimmed }?.toString()
                ?: defaultPairNames(beltIndex, names.size + 1).last()
        trimmed.isNotEmpty() && trimmed.all { it.toIntOrNull() != null } ->
            generateSequence(1) { it + 1 }.first { it.toString() !in trimmed }.toString()
        else -> defaultPairNames(beltIndex, names.size + 1).last()
    }
}

/** Gabarit : perceuse simple, 2 arbres, 5 vitesses. */
fun createTwoShaftMachine(labels: FactoryLabels = DEFAULT_FACTORY_LABELS): Machine {
    val motor = PulleyStack(newId(), labels.motorCone, mutableListOf(100.0, 87.0, 74.0, 61.0, 48.0))
    val spindle = PulleyStack(newId(), labels.spindleCone, mutableListOf(48.0, 61.0, 74.0, 87.0, 100.0))
    return Machine(
        id = newId(),
        name = labels.twoShaftName,
        motorRpm = 1420.0,
        shafts = mutableListOf(
            Shaft(newId(), labels.motorShaft, mutableListOf(motor)),
            Shaft(newId(), labels.spindleShaft, mutableListOf(spindle)),
        ),
        belts = mutableListOf(
            Belt(0, 0, 1, 0, defaultPairs(motor, spindle),
                mutableListOf("A", "B", "C", "D", "E")),
        ),
    )
}

/** Gabarit : perceuse 3 arbres (poulie intermédiaire à double cône), 12/16 vitesses. */
fun createThreeShaftMachine(labels: FactoryLabels = DEFAULT_FACTORY_LABELS): Machine {
    val motor = PulleyStack(newId(), labels.motorCone, mutableListOf(110.0, 90.0, 70.0, 50.0))
    val midIn = PulleyStack(newId(), labels.intermediateConeIn, mutableListOf(50.0, 70.0, 90.0, 110.0))
    val midOut = PulleyStack(newId(), labels.intermediateConeOut, mutableListOf(110.0, 90.0, 70.0, 50.0))
    val spindle = PulleyStack(newId(), labels.spindleCone, mutableListOf(50.0, 70.0, 90.0, 110.0))
    return Machine(
        id = newId(),
        name = labels.threeShaftName,
        motorRpm = 1420.0,
        shafts = mutableListOf(
            Shaft(newId(), labels.motorShaft, mutableListOf(motor)),
            Shaft(newId(), labels.intermediateShaft, mutableListOf(midIn, midOut)),
            Shaft(newId(), labels.spindleShaft, mutableListOf(spindle)),
        ),
        belts = mutableListOf(
            Belt(0, 0, 1, 0, defaultPairs(motor, midIn), mutableListOf("1", "2", "3", "4")),
            Belt(1, 1, 2, 0, defaultPairs(midOut, spindle), mutableListOf("A", "B", "C", "D")),
        ),
    )
}
