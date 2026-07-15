package fr.mgth.drillpress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.pairName
import fr.mgth.drillpress.core.shaftRpms
import kotlin.math.max
import kotlin.math.min

/**
 * Portage du schéma SVG (PulleySchematic.svelte) : le calcul de géométrie est
 * une fonction pure — les positions se transposent directement du SVG — et le
 * dessin est fait sur un Compose Canvas. Vue de côté, arbres verticaux,
 * moteur → broche de gauche à droite (option miroir).
 */

data class SchematicGeometry(
    val px: Float, val stepH: Float, val stepGap: Float, val stackGap: Float,
    val shaftGap: Float, val marginX: Float, val top: Float, val bottomPad: Float, val labelDy: Float,
) {
    companion object {
        val FULL = SchematicGeometry(1.1f, 16f, 3f, 28f, 46f, 14f, 30f, 34f, 7f)
        val MINI = SchematicGeometry(0.32f, 5f, 1.5f, 9f, 18f, 4f, 13f, 4f, 4f)
    }
}

data class SchematicStep(
    val x: Float, val y: Float, val w: Float, val h: Float,
    val label: String, val selected: Boolean,
)

data class SchematicBelt(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val label: String?)

data class SchematicAxis(
    val x: Float, val y1: Float, val y2: Float,
    val labelX: Float, val shaftLabel: String, val rpmLabel: String?,
)

data class SchematicLayout(
    val width: Float, val height: Float,
    val axes: List<SchematicAxis>, val steps: List<SchematicStep>, val belts: List<SchematicBelt>,
)

fun formatMm(mm: Double): String {
    val v = if (mm >= 10) Math.round(mm * 10) / 10.0 else Math.round(mm * 100) / 100.0
    return if (v == Math.floor(v)) v.toLong().toString() else v.toString()
}

fun computeSchematicLayout(
    machine: Machine,
    pairs: List<Pair<Int, Int>>,
    pairIndexes: List<Int>?,
    geo: SchematicGeometry,
    formatLen: (Double) -> String,
    rpmUnit: String,
): SchematicLayout {
    val shafts = machine.shafts
    val belts = machine.belts

    val maxRadius = shafts.map { shaft ->
        (shaft.stacks.flatMap { it.steps }.maxOrNull() ?: 0.0).toFloat() / 2f * geo.px
    }

    // Position x de l'axe de chaque arbre, moteur → broche de gauche à droite.
    val shaftX = FloatArray(shafts.size)
    for (s in shafts.indices) {
        shaftX[s] = if (s == 0) geo.marginX + maxRadius[0]
        else shaftX[s - 1] + maxRadius[s - 1] + maxRadius[s] + geo.shaftGap
    }
    val width = shaftX.last() + maxRadius.last() + geo.marginX
    if (machine.spindleLeft) for (s in shaftX.indices) shaftX[s] = width - shaftX[s]

    // Rangée verticale de chaque cône : les deux cônes reliés par une courroie
    // sont à la même hauteur ; les cônes d'un même arbre s'empilent.
    val stackRow = shafts.map { shaft -> IntArray(shaft.stacks.size) { it } }.toMutableList()
    for (belt in belts) {
        val base = stackRow[belt.fromShaft][belt.fromStack]
        stackRow[belt.toShaft] = IntArray(shafts[belt.toShaft].stacks.size) { st -> base + (st - belt.toStack) }
    }
    val allRows = stackRow.flatMap { it.toList() }
    val minRow = allRows.min()
    val rowCount = allRows.max() - minRow + 1

    val rowHeight = FloatArray(rowCount) { r ->
        var h = 0f
        shafts.forEachIndexed { s, shaft ->
            shaft.stacks.forEachIndexed { st, stack ->
                if (stackRow[s][st] - minRow == r) h = max(h, stack.steps.size * (geo.stepH + geo.stepGap))
            }
        }
        h
    }
    val rowY = FloatArray(rowCount)
    for (r in 0 until rowCount) rowY[r] = if (r == 0) geo.top else rowY[r - 1] + rowHeight[r - 1] + geo.stackGap

    val stackY = shafts.mapIndexed { s, shaft -> FloatArray(shaft.stacks.size) { st -> rowY[stackRow[s][st] - minRow] } }
    fun stepMidY(s: Int, st: Int, i: Int) = stackY[s][st] + i * (geo.stepH + geo.stepGap) + geo.stepH / 2

    val bottom = shafts.mapIndexed { s, shaft ->
        shaft.stacks.mapIndexed { st, stack -> stackY[s][st] + stack.steps.size * (geo.stepH + geo.stepGap) }.max()
    }.max()

    fun selectedAt(s: Int, st: Int, i: Int): Boolean = belts.indices.any { k ->
        val (pi, pj) = pairs.getOrNull(k) ?: belts[k].allowedPairs[0]
        (belts[k].fromShaft == s && belts[k].fromStack == st && pi == i) ||
            (belts[k].toShaft == s && belts[k].toStack == st && pj == i)
    }

    val steps = buildList {
        shafts.forEachIndexed { s, shaft ->
            shaft.stacks.forEachIndexed { st, stack ->
                stack.steps.forEachIndexed { i, d ->
                    val w = d.toFloat() * geo.px
                    val y = stackY[s][st] + i * (geo.stepH + geo.stepGap)
                    add(SchematicStep(shaftX[s] - w / 2, y, w, geo.stepH, formatLen(d), selectedAt(s, st, i)))
                }
            }
        }
    }

    val beltShapes = belts.mapIndexed { k, belt ->
        val (i, j) = pairs.getOrNull(k) ?: belt.allowedPairs[0]
        val dFrom = shafts[belt.fromShaft].stacks[belt.fromStack].steps[i]
        val dTo = shafts[belt.toShaft].stacks[belt.toStack].steps[j]
        val dir = if (shaftX[belt.fromShaft] <= shaftX[belt.toShaft]) 1 else -1
        SchematicBelt(
            x1 = shaftX[belt.fromShaft] + dir * (dFrom.toFloat() / 2f * geo.px),
            y1 = stepMidY(belt.fromShaft, belt.fromStack, i),
            x2 = shaftX[belt.toShaft] - dir * (dTo.toFloat() / 2f * geo.px),
            y2 = stepMidY(belt.toShaft, belt.toStack, j),
            label = pairIndexes?.let { pairName(belt, it[k]) },
        )
    }

    val rpms = if (pairs.size == belts.size) shaftRpms(machine, pairs) else emptyList()
    val height = bottom + geo.bottomPad
    val axes = shafts.mapIndexed { s, shaft ->
        SchematicAxis(
            x = shaftX[s], y1 = geo.top - 8f, y2 = height - 30f, labelX = shaftX[s],
            shaftLabel = shaft.label,
            rpmLabel = rpms.getOrNull(s)?.let { "${Math.round(it)} $rpmUnit" },
        )
    }

    return SchematicLayout(width, height, axes, steps, beltShapes)
}

private val ACCENT = Color(0xFFD97706)
private val MUTED = Color(0xFF6B6B6B)
private val FG = Color(0xFF0F0F0F)
private val STEP_FILL = Color(0xFFDBDBDB)

@Composable
fun PulleySchematic(
    machine: Machine,
    pairs: List<Pair<Int, Int>>,
    pairIndexes: List<Int>? = null,
    mini: Boolean = false,
    modifier: Modifier = Modifier,
    formatLen: (Double) -> String = { formatMm(it) },
    rpmUnit: String = "tr/min",
) {
    val geo = if (mini) SchematicGeometry.MINI else SchematicGeometry.FULL
    val layout = computeSchematicLayout(machine, pairs, pairIndexes, geo, formatLen, rpmUnit)

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(layout.width / layout.height)) {
        val s = size.width / layout.width
        val dash = PathEffect.dashPathEffect(
            if (mini) floatArrayOf(2f * s, 2f * s) else floatArrayOf(5f * s, 4f * s),
        )
        // Formes vectorielles : dessinées dans le repère mis à l'échelle.
        scale(s, s, pivot = Offset.Zero) {
            for (a in layout.axes) {
                drawLine(MUTED, Offset(a.x, a.y1), Offset(a.x, a.y2),
                    strokeWidth = if (mini) 0.75f else 1.5f, pathEffect = dash)
            }
            for (st in layout.steps) {
                val r = CornerRadius(if (mini) 1f else 2f)
                drawRoundRect(if (st.selected) ACCENT else STEP_FILL,
                    topLeft = Offset(st.x, st.y), size = Size(st.w, st.h), cornerRadius = r)
                drawRoundRect(if (st.selected) ACCENT else MUTED,
                    topLeft = Offset(st.x, st.y), size = Size(st.w, st.h), cornerRadius = r,
                    style = Stroke(width = if (mini) 0.5f else 1f))
            }
            for (b in layout.belts) {
                drawLine(ACCENT, Offset(b.x1, b.y1), Offset(b.x2, b.y2),
                    strokeWidth = if (mini) 2f else 5f, cap = StrokeCap.Round)
            }
        }
        // Texte via nativeCanvas (coords et tailles multipliées par s à la main).
        if (!mini) {
            val nc = drawContext.canvas.nativeCanvas
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
            }
            fun text(str: String, x: Float, y: Float, sizePx: Float, color: Int, bold: Boolean) {
                paint.color = color
                paint.textSize = sizePx * s
                paint.typeface =
                    if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                nc.drawText(str, x * s, y * s, paint)
            }
            val fgInt = 0xFF0F0F0F.toInt()
            val accentInt = 0xFFD97706.toInt()
            for (a in layout.axes) {
                text(a.shaftLabel, a.labelX, geo.top - 14f, 12f, fgInt, true)
                a.rpmLabel?.let { text(it, a.labelX, layout.height - 12f, 11f, accentInt, true) }
            }
            for (st in layout.steps) {
                text(st.label, st.x + st.w / 2, st.y + st.h / 2 + 3f, 9f, fgInt, false)
            }
            for (b in layout.belts) {
                b.label?.let {
                    text(it, (b.x1 + b.x2) / 2, min(b.y1, b.y2) - geo.labelDy, 13f, accentInt, true)
                }
            }
        }
    }
}
