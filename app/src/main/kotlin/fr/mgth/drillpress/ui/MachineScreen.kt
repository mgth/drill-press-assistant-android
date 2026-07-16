package fr.mgth.drillpress.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.core.Belt
import fr.mgth.drillpress.core.IssueLevel
import fr.mgth.drillpress.core.PulleyStack
import fr.mgth.drillpress.core.defaultPairNames
import fr.mgth.drillpress.core.defaultPairs
import fr.mgth.drillpress.core.ensurePairNames
import fr.mgth.drillpress.core.isSharedIntermediate
import fr.mgth.drillpress.core.setSharedIntermediate
import fr.mgth.drillpress.core.syncBeltPairs
import fr.mgth.drillpress.core.validateMachine

/** Hauteur des OutlinedTextField Material3 : alignée sur les ancres de dropdown. */
private val FIELD_HEIGHT = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineScreen(app: AppState) {
    @Suppress("UNUSED_EXPRESSION") app.rev // relire pour recomposer à chaque édition
    val machine = app.machine
    val t = app.t
    ensurePairNames(machine)
    val issues = validateMachine(machine)

    MachinePicker(app)

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LocalTextField(machine.name, t.name, machine.id, Modifier.fillMaxWidth()) { machine.name = it; app.touch() }
            LocalNumberField(machine.motorRpm, t.motorRpm, machine.id, Modifier.width(220.dp)) { machine.motorRpm = it; app.touch() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = machine.spindleLeft, onCheckedChange = { machine.spindleLeft = it; app.touch() })
                Text(t.spindleLeft, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (issues.isNotEmpty()) {
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                issues.forEach {
                    Text(
                        t.issue(it),
                        color = if (it.level == IssueLevel.ERROR) Color(0xFFC0392B) else Color(0xFFB45309),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    val order = if (machine.spindleLeft) machine.shafts.indices.reversed().toList() else machine.shafts.indices.toList()
    order.forEach { s -> ShaftCard(app, s) }

    machine.belts.forEachIndexed { k, belt -> BeltEditor(app, belt, k) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MachinePicker(app: AppState) {
    val t = app.t
    var confirmDelete by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.weight(1f)) {
            OutlinedTextField(
                value = app.machine.name, onValueChange = {}, readOnly = true, label = { Text(t.pickerMachine) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded, { expanded = false }) {
                app.machines.forEach { m ->
                    DropdownMenuItem(text = { Text(m.name) }, onClick = { app.selectMachine(m.id); expanded = false })
                }
            }
        }
        if (app.machines.size > 1) TextButton(onClick = { confirmDelete = true }) { Text(t.deleteMachine) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { app.addTwoShaft() }) { Text(t.newTwoShaft) }
        TextButton(onClick = { app.addThreeShaft() }) { Text(t.newThreeShaft) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = { TextButton(onClick = { app.removeMachine(app.currentId); confirmDelete = false }) { Text(t.deleteMachine) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("×") } },
            text = { Text(t.deleteConfirm) },
        )
    }
}

@Composable
private fun ShaftCard(app: AppState, s: Int) {
    val machine = app.machine
    val shaft = machine.shafts[s]
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(shaft.label, fontWeight = FontWeight.SemiBold)
            if (s in 1 until machine.shafts.size - 1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(
                        checked = isSharedIntermediate(machine, s),
                        onCheckedChange = { setSharedIntermediate(machine, s, it); app.touch() },
                    )
                    Text(app.t.sharedCone, style = MaterialTheme.typography.bodySmall)
                }
            }
            shaft.stacks.forEach { stack -> StackEditor(app, stack) }
        }
    }
}

@Composable
private fun StackEditor(app: AppState, stack: PulleyStack) {
    val t = app.t
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stack.label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        stack.steps.forEachIndexed { i, d ->
            key(stack.id, i) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${t.step} ${i + 1}", color = Color(0xFF6B6B6B), modifier = Modifier.width(40.dp))
                    LocalNumberField(displayLen(d, app.units), "Ø ${lenUnit(app.units)}", app.units, Modifier.width(130.dp)) {
                        stack.steps[i] = parseLen(it, app.units); app.touch()
                    }
                    TextButton(
                        onClick = { stack.steps.removeAt(i); syncBeltPairs(app.machine); app.touch() },
                        enabled = stack.steps.size > 1,
                    ) { Text("✕") }
                }
            }
        }
        TextButton(onClick = {
            stack.steps.add(stack.steps.lastOrNull() ?: 60.0); syncBeltPairs(app.machine); app.touch()
        }) { Text(t.addStep) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeltEditor(app: AppState, belt: Belt, k: Int) {
    val machine = app.machine
    val t = app.t
    val fromStack = machine.shafts[belt.fromShaft].stacks[belt.fromStack]
    val toStack = machine.shafts[belt.toShaft].stacks[belt.toStack]
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${t.belt} ${k + 1}", fontWeight = FontWeight.SemiBold)
            Text("${fromStack.label} → ${toStack.label}", color = Color(0xFF6B6B6B), style = MaterialTheme.typography.bodySmall)
            belt.allowedPairs.forEachIndexed { i, pair ->
                key(k, i) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RepField(belt.pairNames?.getOrNull(i) ?: "${i + 1}", machine.id, Modifier.width(56.dp)) {
                            belt.pairNames?.set(i, it); app.touch()
                        }
                        StepDropdown(app, fromStack, pair.first, Modifier.weight(1f)) {
                            belt.allowedPairs[i] = it to pair.second; app.touch()
                        }
                        Text("→")
                        StepDropdown(app, toStack, pair.second, Modifier.weight(1f)) {
                            belt.allowedPairs[i] = pair.first to it; app.touch()
                        }
                        TextButton(
                            onClick = { belt.allowedPairs.removeAt(i); belt.pairNames?.removeAt(i); app.touch() },
                            enabled = belt.allowedPairs.size > 1,
                        ) { Text("✕") }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    belt.allowedPairs.add(0 to 0)
                    belt.pairNames?.add(defaultPairNames(k, belt.allowedPairs.size).last())
                    app.touch()
                }) { Text(t.addPair) }
                if (fromStack.steps.size == toStack.steps.size) {
                    TextButton(onClick = {
                        belt.allowedPairs = defaultPairs(fromStack, toStack)
                        belt.pairNames = defaultPairNames(k, belt.allowedPairs.size)
                        app.touch()
                    }) { Text(t.resetPairs) }
                }
            }
        }
    }
}

/**
 * Champ repère compact : même cadre à hauteur fixe que les ancres de
 * dropdown (pas d'étiquette flottante, elle décalerait la boîte).
 */
@Composable
private fun RepField(value: String, resetKey: Any?, modifier: Modifier, onValue: (String) -> Unit) {
    var text by remember(resetKey) { mutableStateOf(value) }
    Box(
        modifier
            .height(FIELD_HEIGHT)
            .border(1.dp, Color(0xFF79747E), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValue(it) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Sélecteur d'étage compact : ancre à hauteur fixe (celle des champs de
 * texte), libellé court sur une seule ligne (« 1 · 110 »), libellé complet
 * dans le menu ouvert — pour que chaque ligne de position garde des cadres
 * homogènes et tienne sur une ligne.
 */
@Composable
private fun StepDropdown(app: AppState, stack: PulleyStack, selected: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    fun shortLabel(idx: Int) = "${idx + 1} · ${formatLen(stack.steps[idx], app.units, app.lang)}"
    fun fullLabel(idx: Int) =
        "${app.t.step} ${idx + 1} · ${formatLen(stack.steps[idx], app.units, app.lang)} ${lenUnit(app.units)}"
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(FIELD_HEIGHT)
                .border(1.dp, Color(0xFF79747E), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                shortLabel(selected),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text("▾", color = Color(0xFF6B6B6B))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            stack.steps.indices.forEach { idx ->
                DropdownMenuItem(text = { Text(fullLabel(idx)) }, onClick = { onSelect(idx); expanded = false })
            }
        }
    }
}

@Composable
private fun LocalTextField(value: String, label: String, resetKey: Any?, modifier: Modifier, onValue: (String) -> Unit) {
    var text by remember(resetKey) { mutableStateOf(value) }
    OutlinedTextField(text, { text = it; onValue(it) }, label = { Text(label) }, singleLine = true, modifier = modifier)
}

@Composable
private fun LocalNumberField(value: Double, label: String, resetKey: Any?, modifier: Modifier, onValue: (Double) -> Unit) {
    var text by remember(resetKey) { mutableStateOf(fmtNum(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { s -> text = s; s.replace(",", ".").toDoubleOrNull()?.let { if (it > 0) onValue(it) } },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true, modifier = modifier,
    )
}
