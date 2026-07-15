package fr.mgth.drillpress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.core.createThreeShaftMachine

enum class AppTab(val label: String) {
    MACHINE("Machine"),
    DRILLING("Perçage"),
}

/**
 * Squelette de navigation à deux onglets, calqué sur la version web.
 * La machine (une seule par défaut pour l'instant) est partagée entre onglets ;
 * l'éditeur de machine viendra remplacer le placeholder de l'onglet Machine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var tab by remember { mutableStateOf(AppTab.DRILLING) }
    val machine = remember { createThreeShaftMachine() }

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
                AppTab.MACHINE -> MachineTab()
                AppTab.DRILLING -> DrillingScreen(machine)
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
