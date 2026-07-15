package fr.mgth.drillpress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.core.createThreeShaftMachine

enum class AppTab(val label: String) {
    MACHINE("Machine"),
    DRILLING("Perçage"),
}

/**
 * Navigation à deux onglets. La machine (une seule pour l'instant) est mutable
 * — portée 1:1 du TS —, donc un compteur de révision `machineRev` force la
 * recomposition des deux onglets à chaque édition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var tab by remember { mutableStateOf(AppTab.MACHINE) }
    val machine = remember { createThreeShaftMachine() }
    var machineRev by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Drill Press Assistant") })
                TabRow(selectedTabIndex = tab.ordinal) {
                    AppTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (tab) {
                AppTab.MACHINE -> MachineScreen(machine, machineRev) { machineRev++ }
                AppTab.DRILLING -> DrillingScreen(machine, machineRev)
            }
        }
    }
}
