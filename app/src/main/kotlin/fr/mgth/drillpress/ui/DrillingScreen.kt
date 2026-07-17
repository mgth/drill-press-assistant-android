package fr.mgth.drillpress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mgth.drillpress.core.BitType
import fr.mgth.drillpress.core.Combination
import fr.mgth.drillpress.core.DiameterRange
import fr.mgth.drillpress.core.IMPERIAL_DRILLS
import fr.mgth.drillpress.core.MATERIALS
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.diameterRanges
import fr.mgth.drillpress.core.formatDeviation
import fr.mgth.drillpress.core.pairName
import fr.mgth.drillpress.core.recommend
import fr.mgth.drillpress.core.vcChipValues
import fr.mgth.drillpress.core.vcMaterial
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private val ACCENT = Color(0xFFD97706)
private val ACCENT_SOFT = Color(0xFFFDE8CC)
private val MUTED = Color(0xFF6B6B6B)
private val BORDER = Color(0xFFD8D8D8)

internal fun fmtNum(v: Double): String =
    if (v == floor(v)) v.toLong().toString() else (Math.round(v * 100) / 100.0).toString().replace(".", ",")

private fun comboKey(pairs: List<Pair<Int, Int>>): String =
    pairs.joinToString(";") { "${it.first},${it.second}" }

private fun rangeLabel(r: DiameterRange?, units: Units, lang: Lang, all: String): String {
    if (r == null) return "—"
    val min = r.min
    val max = r.max
    val f = { d: Double -> formatLen(d, units, lang) }
    return when {
        min == null && max == null -> all
        max == null -> "≥ ${f(min!!)}"
        min == null -> "≤ ${f(max)}"
        else -> "${f(min)} – ${f(max)}"
    }
}

private data class Chip(val label: String, val sub: String?, val active: Boolean, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillingScreen(app: AppState) {
    val t = app.t
    val machine = app.machine

    val reco = remember(app.materialId, app.bitType, app.vcOverride, app.diameterMm, app.rev) {
        if (app.diameterMm > 0 && app.vc > 0) recommend(machine, app.vc, app.diameterMm) else null
    }
    val ranges = remember(reco) { reco?.let { diameterRanges(it.all, app.vc) } }
    val displayed = reco?.let { r -> r.all.firstOrNull { comboKey(it.pairs) == app.selectedKey } ?: r.best }

    Text(t.drillingParams, style = MaterialTheme.typography.titleMedium)

    // Matériau
    Row {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (app.custom) t.custom else materialLabel(app.material, app.lang),
                onValueChange = {}, readOnly = true, label = { Text(t.material) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded, { expanded = false }) {
                MATERIALS.forEach { m ->
                    DropdownMenuItem(text = { Text(materialLabel(m, app.lang)) },
                        onClick = { app.chooseMaterial(m.id); expanded = false })
                }
            }
        }
    }

    // Type de foret : le facteur HSS-Co / carbure n'a de sens que pour les
    // métaux — sélecteur figé sur HSS pour bois et plastique.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(t.bitType, fontWeight = FontWeight.SemiBold)
        val metal = app.material.metal || app.custom
        SegToggle(
            listOf(t.hss, t.hssCo, t.carbide),
            when {
                app.custom -> -1
                !metal -> 0
                else -> app.bitType.ordinal
            },
            enabled = metal,
        ) { app.chooseBit(BitType.entries[it]) }
    }

    // Frise Vc
    ChipStrip(
        "${t.cuttingSpeed} (${vcUnit(app.units)})",
        vcChipValues(app.bitType.factor, app.imperial).map { v ->
            Chip(formatVc(v, app.units, app.lang), vcMaterial(v, app.bitType.factor)?.let { materialAbbr(it, app.lang) },
                v == app.vc) { app.chooseVc(v) }
        },
    )

    // Frise Ø
    val diaChips = if (app.imperial) {
        IMPERIAL_DRILLS.map { d -> Chip(d.label, null, abs(app.diameterMm - d.mm) < 0.01) { app.chooseDiameter(d.mm) } }
    } else {
        (1..20).map { i -> Chip(i.toString(), null, abs(app.diameterMm - i) < 0.01) { app.chooseDiameter(i.toDouble()) } }
    }
    ChipStrip("${t.diameter} (${lenUnit(app.units)})", diaChips)

    // Ø exact
    val diaDisplay = displayLen(app.diameterMm, app.units)
    var diaText by remember(diaDisplay) { mutableStateOf(fmtNum(diaDisplay)) }
    OutlinedTextField(
        value = diaText,
        onValueChange = { s -> diaText = s; s.replace(",", ".").toDoubleOrNull()?.let { app.chooseDiameter(parseLen(it, app.units)) } },
        label = { Text("${t.exactDiameter} (${lenUnit(app.units)})") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true, modifier = Modifier.width(200.dp),
    )

    reco?.let { r ->
        Text(
            "${t.cuttingSpeed} : ${formatVc(app.vc, app.units, app.lang)} ${vcUnit(app.units)} — " +
                "${t.idealSpeed} : ${r.ideal.roundToInt()} ${rpmUnit(app.lang)}",
            style = MaterialTheme.typography.bodyMedium, color = MUTED,
        )
    }

    if (reco != null && displayed != null) {
        if (reco.overspeed) {
            Text(t.overspeed, color = ACCENT, fontWeight = FontWeight.SemiBold)
        }
        Card(colors = CardDefaults.cardColors(containerColor = ACCENT_SOFT.copy(alpha = 0.4f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t.recommendedPosition, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${positionLabel(machine, displayed)} — ${displayed.spindleRpm.roundToInt()} ${rpmUnit(app.lang)} " +
                        formatDeviation(displayed.spindleRpm, reco.ideal),
                    style = MaterialTheme.typography.titleMedium, color = ACCENT, fontWeight = FontWeight.Bold,
                )
                PulleySchematic(
                    machine, displayed.pairs, displayed.pairIndexes, modifier = Modifier.fillMaxWidth(),
                    formatLen = { formatLen(it, app.units, app.lang) }, rpmUnit = rpmUnit(app.lang),
                )
            }
        }

        Text(t.allSpeeds, style = MaterialTheme.typography.titleMedium)
        val recommendedKey = comboKey(reco.best.pairs)
        val selKey = comboKey(displayed.pairs)
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(t.colPosition, color = MUTED, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                Text(rpmUnit(app.lang), color = MUTED, fontSize = 12.sp, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
                Text(t.colDeviation, color = MUTED, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                Text("${t.colRange} (${lenUnit(app.units)})", color = MUTED, fontSize = 12.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            reco.all.forEachIndexed { idx, combo ->
                val key = comboKey(combo.pairs)
                SpeedRow(app, machine, combo, reco.ideal, ranges?.getOrNull(idx), positionLabel(machine, combo),
                    key == recommendedKey, key == selKey) { app.selectedKey = key }
            }
        }
    }
}

private fun positionLabel(m: Machine, combo: Combination): String {
    val names = m.belts.mapIndexed { k, belt -> pairName(belt, combo.pairIndexes[k]) }.toMutableList()
    if (m.spindleLeft) names.reverse()
    return names.joinToString(" – ")
}

@Composable
private fun SegToggle(options: List<String>, selectedIndex: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Row(Modifier.alpha(if (enabled) 1f else 0.45f).border(1.dp, BORDER, RoundedCornerShape(50)).padding(2.dp)) {
        options.forEachIndexed { i, opt ->
            val active = i == selectedIndex
            Box(
                Modifier.background(if (active) ACCENT else Color.Transparent, RoundedCornerShape(50))
                    .clickable(enabled = enabled) { onSelect(i) }.padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text(opt, color = if (active) Color.White else MUTED, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ChipStrip(label: String, chips: List<Chip>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chips.forEach { c ->
                Column(
                    Modifier.background(if (c.active) ACCENT else Color.Transparent, RoundedCornerShape(6.dp))
                        .border(1.dp, if (c.active) ACCENT else BORDER, RoundedCornerShape(6.dp))
                        .clickable { c.onClick() }.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(c.label, color = if (c.active) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    if (c.sub != null) Text(c.sub, fontSize = 10.sp, color = if (c.active) Color.White else MUTED)
                }
            }
        }
    }
}

@Composable
private fun SpeedRow(
    app: AppState, machine: Machine, combo: Combination, ideal: Double, range: DiameterRange?,
    label: String, recommended: Boolean, selected: Boolean, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (recommended) Modifier.background(ACCENT_SOFT) else Modifier)
            .then(if (selected && !recommended) Modifier.border(2.dp, ACCENT) else Modifier)
            .clickable { onClick() }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PulleySchematic(machine, combo.pairs, combo.pairIndexes, mini = true, modifier = Modifier.fillMaxWidth())
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MUTED)
        }
        Text("${combo.spindleRpm.roundToInt()}", fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
        Text(formatDeviation(combo.spindleRpm, ideal), color = MUTED, fontSize = 13.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(rangeLabel(range, app.units, app.lang, app.t.allDiameters), color = MUTED, fontSize = 13.sp)
            if (recommended) Badge(app.t.recommendedBadge, ACCENT, Color.White)
            else if (selected) Badge(app.t.selectedBadge, Color.Transparent, ACCENT, ACCENT)
        }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color, borderColor: Color? = null) {
    Box(
        Modifier.background(bg, RoundedCornerShape(50))
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(50)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) { Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}
