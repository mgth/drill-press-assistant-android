package fr.mgth.drillpress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.core.createThreeShaftMachine
import fr.mgth.drillpress.core.formatDeviation
import fr.mgth.drillpress.core.materialById
import fr.mgth.drillpress.core.pairName
import fr.mgth.drillpress.core.recommend
import kotlin.math.roundToInt

enum class AppTab(val label: String) {
    MACHINE("Machine"),
    DRILLING("Perçage"),
}

/**
 * Squelette de navigation à deux onglets, calqué sur la version web
 * (onglets « Machine » / « Perçage »). Le contenu de chaque onglet sera étoffé
 * dans les jalons suivants ; l'onglet Perçage montre déjà le schéma Canvas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var tab by remember { mutableStateOf(AppTab.MACHINE) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Drill Press Assistant") })
                TabRow(selectedTabIndex = tab.ordinal) {
                    AppTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick = { tab = t },
                            text = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (tab) {
                AppTab.MACHINE -> MachineTab()
                AppTab.DRILLING -> DrillingTab()
            }
        }
    }
}

@Composable
private fun MachineTab() {
    Text("Éditeur de machine", style = MaterialTheme.typography.titleMedium)
    Text(
        "À porter : nom, vitesse moteur, arbres et cônes étagés, positions de " +
            "courroie, cône unique, option miroir.",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun DrillingTab() {
    val machine = remember { createThreeShaftMachine() }
    val aluminum = remember { materialById("aluminum")!! }
    val reco = remember { recommend(machine, aluminum.vcHss, 6.0) }
    val best = reco.best
    val position = machine.belts
        .mapIndexed { k, belt -> pairName(belt, best.pairIndexes[k]) }
        .joinToString(" – ")

    Text("Position recommandée", style = MaterialTheme.typography.titleMedium)
    Text(
        "Aluminium · Ø 6 mm → $position — ${best.spindleRpm.roundToInt()} tr/min " +
            formatDeviation(best.spindleRpm, reco.ideal),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
    )
    PulleySchematic(
        machine = machine,
        pairs = best.pairs,
        pairIndexes = best.pairIndexes,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        "À porter : matériau, type de foret, frises Vc/Ø, table des vitesses.",
        style = MaterialTheme.typography.bodySmall,
    )
}
