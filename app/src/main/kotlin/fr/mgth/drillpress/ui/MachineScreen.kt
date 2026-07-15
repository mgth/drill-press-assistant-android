package fr.mgth.drillpress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.core.Belt
import fr.mgth.drillpress.core.Issue
import fr.mgth.drillpress.core.IssueCode
import fr.mgth.drillpress.core.IssueLevel
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.PulleyStack
import fr.mgth.drillpress.core.defaultPairNames
import fr.mgth.drillpress.core.defaultPairs
import fr.mgth.drillpress.core.ensurePairNames
import fr.mgth.drillpress.core.isSharedIntermediate
import fr.mgth.drillpress.core.setSharedIntermediate
import fr.mgth.drillpress.core.syncBeltPairs
import fr.mgth.drillpress.core.validateMachine

private fun issueMessage(i: Issue): String {
    val p = i.params
    return when (i.code) {
        IssueCode.MOTOR_RPM -> "La vitesse moteur doit être supérieure à 0."
        IssueCode.MIN_SHAFTS -> "Il faut au moins deux arbres (moteur et broche)."
        IssueCode.BELT_COUNT -> "Il faut exactement une courroie entre chaque paire d'arbres consécutifs."
        IssueCode.EMPTY_STACK -> "« ${p["stack"]} » (${p["shaft"]}) n'a aucun étage."
        IssueCode.BAD_DIAMETER -> "« ${p["stack"]} » (${p["shaft"]}) a un diamètre invalide (doit être > 0)."
        IssueCode.BELT_CHAIN -> "Courroie ${p["belt"]} : doit relier l'arbre ${p["from"]} à l'arbre ${p["to"]}."
        IssueCode.STACK_MISSING -> "Courroie ${p["belt"]} : cône introuvable."
        IssueCode.NO_PAIRS -> "Courroie ${p["belt"]} : aucune position définie."
        IssueCode.PAIR_OUT_OF_RANGE -> "Courroie ${p["belt"]} : position (${p["i"]}, ${p["j"]}) hors limites."
        IssueCode.DIAMETER_SUM -> "Courroie ${p["belt"]} : la somme des diamètres varie de plus de ${p["tolerance"]} % " +
            "entre positions — vérifiez la saisie (sur de vrais cônes étagés elle est quasi constante)."
        IssueCode.NO_COMBINATION -> "Aucune combinaison possible : sur un cône partagé, les deux courroies " +
            "ne peuvent pas occuper le même étage."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineScreen(machine: Machine, machineRev: Int, onChanged: () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") machineRev // relire pour recomposer à chaque édition
    ensurePairNames(machine)
    val issues = validateMachine(machine)

    // Général
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LocalTextField(machine.name, "Nom", Modifier.fillMaxWidth()) { machine.name = it; onChanged() }
            LocalNumberField(machine.motorRpm, "Vitesse moteur (tr/min)", Modifier.width(220.dp)) {
                machine.motorRpm = it; onChanged()
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = machine.spindleLeft, onCheckedChange = { machine.spindleLeft = it; onChanged() })
                Text("Broche à gauche sur le schéma", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (issues.isNotEmpty()) {
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                issues.forEach {
                    Text(
                        issueMessage(it),
                        color = if (it.level == IssueLevel.ERROR) Color(0xFFC0392B) else Color(0xFFB45309),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    // Arbres, dans l'ordre d'affichage du schéma
    val order = if (machine.spindleLeft) machine.shafts.indices.reversed().toList()
    else machine.shafts.indices.toList()
    order.forEach { s -> ShaftCard(machine, s, onChanged) }

    // Courroies
    machine.belts.forEachIndexed { k, belt -> BeltEditor(machine, belt, k, onChanged) }
}

@Composable
private fun ShaftCard(machine: Machine, s: Int, onChanged: () -> Unit) {
    val shaft = machine.shafts[s]
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(shaft.label, fontWeight = FontWeight.SemiBold)
            if (s in 1 until machine.shafts.size - 1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(
                        checked = isSharedIntermediate(machine, s),
                        onCheckedChange = { setSharedIntermediate(machine, s, it); onChanged() },
                    )
                    Text("Cône unique (entrée et sortie sur le même cône)", style = MaterialTheme.typography.bodySmall)
                }
            }
            shaft.stacks.forEach { stack -> StackEditor(machine, stack, onChanged) }
        }
    }
}

@Composable
private fun StackEditor(machine: Machine, stack: PulleyStack, onChanged: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stack.label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        stack.steps.forEachIndexed { i, d ->
            key(stack.id, i) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ét. ${i + 1}", color = Color(0xFF6B6B6B), modifier = Modifier.width(40.dp))
                    LocalNumberField(d, "Ø mm", Modifier.width(120.dp)) {
                        stack.steps[i] = it; onChanged()
                    }
                    TextButton(
                        onClick = { stack.steps.removeAt(i); syncBeltPairs(machine); onChanged() },
                        enabled = stack.steps.size > 1,
                    ) { Text("✕") }
                }
            }
        }
        TextButton(onClick = {
            stack.steps.add(stack.steps.lastOrNull() ?: 60.0); syncBeltPairs(machine); onChanged()
        }) { Text("Ajouter un étage") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeltEditor(machine: Machine, belt: Belt, k: Int, onChanged: () -> Unit) {
    val fromStack = machine.shafts[belt.fromShaft].stacks[belt.fromStack]
    val toStack = machine.shafts[belt.toShaft].stacks[belt.toStack]
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Courroie ${k + 1}", fontWeight = FontWeight.SemiBold)
            Text("${fromStack.label} → ${toStack.label}", color = Color(0xFF6B6B6B), style = MaterialTheme.typography.bodySmall)
            belt.allowedPairs.forEachIndexed { i, pair ->
                key(k, i) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LocalTextField(
                            belt.pairNames?.getOrNull(i) ?: "${i + 1}", "Rep.", Modifier.width(64.dp),
                        ) { belt.pairNames?.set(i, it); onChanged() }
                        StepDropdown(fromStack, pair.first, Modifier.weight(1f)) {
                            belt.allowedPairs[i] = it to pair.second; onChanged()
                        }
                        Text("→")
                        StepDropdown(toStack, pair.second, Modifier.weight(1f)) {
                            belt.allowedPairs[i] = pair.first to it; onChanged()
                        }
                        TextButton(
                            onClick = {
                                belt.allowedPairs.removeAt(i); belt.pairNames?.removeAt(i); onChanged()
                            },
                            enabled = belt.allowedPairs.size > 1,
                        ) { Text("✕") }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    belt.allowedPairs.add(0 to 0)
                    belt.pairNames?.add(defaultPairNames(k, belt.allowedPairs.size).last())
                    onChanged()
                }) { Text("Ajouter une position") }
                if (fromStack.steps.size == toStack.steps.size) {
                    TextButton(onClick = {
                        belt.allowedPairs = defaultPairs(fromStack, toStack)
                        belt.pairNames = defaultPairNames(k, belt.allowedPairs.size)
                        onChanged()
                    }) { Text("Réinitialiser") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepDropdown(stack: PulleyStack, selected: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = "Ét. ${selected + 1} · ${fmtNum(stack.steps[selected])} mm",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            stack.steps.forEachIndexed { idx, d ->
                DropdownMenuItem(
                    text = { Text("Ét. ${idx + 1} · ${fmtNum(d)} mm") },
                    onClick = { onSelect(idx); expanded = false },
                )
            }
        }
    }
}

/** Champ texte à état local (l'utilisateur tape librement ; poussé au modèle tel quel). */
@Composable
private fun LocalTextField(value: String, label: String, modifier: Modifier, onValue: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onValue(it) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

/** Champ numérique à état local ; ne pousse au modèle que les valeurs > 0 valides. */
@Composable
private fun LocalNumberField(value: Double, label: String, modifier: Modifier, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(fmtNum(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            text = s
            s.replace(",", ".").toDoubleOrNull()?.let { if (it > 0) onValue(it) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}
