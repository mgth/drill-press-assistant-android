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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class AppTab { MACHINE, DRILLING, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    val app = remember { AppState().also { loadState(ctx, it); it.ensureMachine() } }
    var tab by remember { mutableStateOf(AppTab.DRILLING) }
    val t = app.t

    // Sauvegarde à chaque changement d'état persistant (édition, ajout/suppression
    // ou changement de machine courante, réglages, langue, unités).
    LaunchedEffect(
        app.rev, app.machines.size, app.currentId,
        app.materialId, app.carbide, app.vcOverride, app.diameterMm, app.lang, app.units,
    ) { saveState(ctx, app) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Drill Press Assistant") },
                    actions = {
                        TextButton(onClick = { app.units = if (app.imperial) Units.METRIC else Units.IMPERIAL }) {
                            Text(app.units.code)
                        }
                        TextButton(onClick = { app.lang = if (app.lang == Lang.FR) Lang.EN else Lang.FR }) {
                            Text(app.lang.code)
                        }
                    },
                )
                TabRow(selectedTabIndex = tab.ordinal) {
                    Tab(tab == AppTab.MACHINE, { tab = AppTab.MACHINE }, text = { Text(t.tabMachine) })
                    Tab(tab == AppTab.DRILLING, { tab = AppTab.DRILLING }, text = { Text(t.tabDrilling) })
                    Tab(tab == AppTab.ABOUT, { tab = AppTab.ABOUT }, text = { Text(t.tabAbout) })
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (tab) {
                AppTab.MACHINE -> MachineScreen(app)
                AppTab.DRILLING -> DrillingScreen(app)
                AppTab.ABOUT -> AboutScreen(app)
            }
        }
    }
}
