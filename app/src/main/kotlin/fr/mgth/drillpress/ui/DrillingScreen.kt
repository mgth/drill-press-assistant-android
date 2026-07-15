package fr.mgth.drillpress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mgth.drillpress.core.CARBIDE_FACTOR
import fr.mgth.drillpress.core.Combination
import fr.mgth.drillpress.core.DiameterRange
import fr.mgth.drillpress.core.MATERIALS
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.createThreeShaftMachine
import fr.mgth.drillpress.core.diameterRanges
import fr.mgth.drillpress.core.formatDeviation
import fr.mgth.drillpress.core.materialById
import fr.mgth.drillpress.core.pairName
import fr.mgth.drillpress.core.recommend
import fr.mgth.drillpress.core.vcChipValues
import fr.mgth.drillpress.core.vcMaterial
import kotlin.math.floor
import kotlin.math.roundToInt

private val ACCENT = androidx.compose.ui.graphics.Color(0xFFD97706)
private val ACCENT_SOFT = androidx.compose.ui.graphics.Color(0xFFFDE8CC)
private val MUTED = androidx.compose.ui.graphics.Color(0xFF6B6B6B)
private val BORDER = androidx.compose.ui.graphics.Color(0xFFD8D8D8)

private fun fmtNum(v: Double): String =
    if (v == floor(v)) v.toLong().toString() else (Math.round(v * 100) / 100.0).toString().replace(".", ",")

private fun comboKey(pairs: List<Pair<Int, Int>>): String =
    pairs.joinToString(";") { "${it.first},${it.second}" }

private fun rangeLabel(r: DiameterRange?): String {
    if (r == null) return "—"
    val min = r.min
    val max = r.max
    val f = { d: Double -> fmtNum(if (d >= 10) Math.round(d * 10) / 10.0 else Math.round(d * 100) / 100.0) }
    return when {
        min == null && max == null -> "tous"
        max == null -> "≥ ${f(min!!)}"
        min == null -> "≤ ${f(max)}"
        else -> "${f(min)} – ${f(max)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillingScreen(machine: Machine) {
    var materialId by remember { mutableStateOf("steel") }
    var carbide by remember { mutableStateOf(false) }
    var vcOverride by remember { mutableStateOf<Double?>(null) }
    var diameterMm by remember { mutableStateOf(8.0) }
    var selectedKey by remember { mutableStateOf<String?>(null) }

    val custom = vcOverride != null
    val material = materialById(materialId)!!
    val vc = vcOverride ?: material.vcHss * (if (carbide) CARBIDE_FACTOR else 1.0)

    fun setMaterial(id: String) { materialId = id; vcOverride = null }
    fun setCarbide(c: Boolean) { carbide = c; vcOverride = null }
    fun setVc(v: Double) {
        val m = vcMaterial(v, carbide)
        if (m != null) { materialId = m.id; vcOverride = null } else vcOverride = v
    }

    // Tout changement de paramètre réinitialise la sélection manuelle.
    LaunchedEffect(materialId, carbide, vcOverride, diameterMm) { selectedKey = null }

    val reco = remember(materialId, carbide, vcOverride, diameterMm, machine) {
        if (diameterMm > 0 && vc > 0) recommend(machine, vc, diameterMm) else null
    }
    val ranges = remember(reco) { reco?.let { diameterRanges(it.all, vc) } }
    val displayed = reco?.let { r -> r.all.firstOrNull { comboKey(it.pairs) == selectedKey } ?: r.best }

    // ---- Formulaire ----
    Text("Paramètres de perçage", style = MaterialTheme.typography.titleMedium)

    // Matériau + type de foret
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = if (custom) "Personnalisé" else material.labelFr,
                onValueChange = {},
                readOnly = true,
                label = { Text("Matériau") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MATERIALS.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.labelFr) },
                        onClick = { setMaterial(m.id); expanded = false },
                    )
                }
            }
        }
        Column {
            Text("Type de foret", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            SegToggle(
                options = listOf("HSS", "Carbure"),
                selectedIndex = if (custom) -1 else if (carbide) 1 else 0,
                onSelect = { setCarbide(it == 1) },
            )
        }
    }

    // Frise vitesse de coupe
    ChipStrip(
        label = "Vitesse de coupe (m/min)",
        values = vcChipValues(carbide).map { it to (vcMaterial(it, carbide)?.abbrFr) },
        isActive = { it == vc },
        onClick = { setVc(it) },
    )

    // Frise diamètre + saisie
    ChipStrip(
        label = "Ø de perçage (mm)",
        values = (1..20).map { it.toDouble() to null },
        isActive = { kotlin.math.abs(diameterMm - it) < 0.01 },
        onClick = { diameterMm = it },
    )
    OutlinedTextField(
        value = fmtNum(diameterMm),
        onValueChange = { s -> s.replace(",", ".").toDoubleOrNull()?.let { if (it > 0) diameterMm = it } },
        label = { Text("Ø exact (mm)") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.width(180.dp),
    )

    reco?.let { r ->
        Text(
            "Vitesse de coupe : ${fmtNum(vc)} m/min — Vitesse idéale : ${r.ideal.roundToInt()} tr/min",
            style = MaterialTheme.typography.bodyMedium,
            color = MUTED,
        )
    }

    // ---- Recommandation ----
    if (reco != null && displayed != null) {
        if (reco.overspeed) {
            Text(
                "Même la combinaison la plus lente dépasse la vitesse idéale : réduisez " +
                    "la vitesse d'avance et lubrifiez.",
                color = ACCENT, fontWeight = FontWeight.SemiBold,
            )
        }
        Card(colors = CardDefaults.cardColors(containerColor = ACCENT_SOFT.copy(alpha = 0.4f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Position recommandée", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${positionLabel(machine, displayed)} — ${displayed.spindleRpm.roundToInt()} tr/min " +
                        formatDeviation(displayed.spindleRpm, reco.ideal),
                    style = MaterialTheme.typography.titleMedium,
                    color = ACCENT, fontWeight = FontWeight.Bold,
                )
                PulleySchematic(machine, displayed.pairs, displayed.pairIndexes, modifier = Modifier.fillMaxWidth())
            }
        }

        // ---- Table des vitesses ----
        Text("Toutes les vitesses", style = MaterialTheme.typography.titleMedium)
        val recommendedKey = comboKey(reco.best.pairs)
        val selKey = comboKey(displayed.pairs)
        Column(Modifier.fillMaxWidth()) {
            reco.all.forEachIndexed { idx, combo ->
                val key = comboKey(combo.pairs)
                SpeedRow(
                    machine = machine,
                    combo = combo,
                    ideal = reco.ideal,
                    range = ranges?.getOrNull(idx),
                    recommended = key == recommendedKey,
                    selected = key == selKey,
                    onClick = { selectedKey = key },
                )
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
private fun SegToggle(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .border(1.dp, BORDER, RoundedCornerShape(50))
            .padding(2.dp),
    ) {
        options.forEachIndexed { i, opt ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .background(if (active) ACCENT else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(opt, color = if (active) androidx.compose.ui.graphics.Color.White else MUTED, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChipStrip(
    label: String,
    values: List<Pair<Double, String?>>,
    isActive: (Double) -> Boolean,
    onClick: (Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            values.forEach { (v, sub) ->
                val active = isActive(v)
                Column(
                    Modifier
                        .background(if (active) ACCENT else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(6.dp))
                        .border(1.dp, if (active) ACCENT else BORDER, RoundedCornerShape(6.dp))
                        .clickable { onClick(v) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(fmtNum(v), color = if (active) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    if (sub != null) {
                        Text(sub, fontSize = 10.sp, color = if (active) androidx.compose.ui.graphics.Color.White else MUTED)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedRow(
    machine: Machine,
    combo: Combination,
    ideal: Double,
    range: DiameterRange?,
    recommended: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (recommended) Modifier.background(ACCENT_SOFT) else Modifier)
            .then(if (selected && !recommended) Modifier.border(2.dp, ACCENT) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(110.dp)) {
            PulleySchematic(machine, combo.pairs, combo.pairIndexes, mini = true, modifier = Modifier.fillMaxWidth())
        }
        Text("${combo.spindleRpm.roundToInt()}", fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
        Text(formatDeviation(combo.spindleRpm, ideal), color = MUTED, fontSize = 13.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(rangeLabel(range), color = MUTED, fontSize = 13.sp)
            if (recommended) Badge("recommandé", ACCENT, androidx.compose.ui.graphics.Color.White)
            else if (selected) Badge("affiché", androidx.compose.ui.graphics.Color.Transparent, ACCENT, ACCENT)
        }
    }
}

@Composable
private fun Badge(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, borderColor: androidx.compose.ui.graphics.Color? = null) {
    Box(
        Modifier
            .background(bg, RoundedCornerShape(50))
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(50)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
