package fr.mgth.drillpress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.core.createThreeShaftMachine
import fr.mgth.drillpress.core.formatDeviation
import fr.mgth.drillpress.core.materialById
import fr.mgth.drillpress.core.pairName
import fr.mgth.drillpress.core.recommend
import fr.mgth.drillpress.ui.PulleySchematic
import kotlin.math.roundToInt

/**
 * Écran de démarrage du portage Kotlin : exerce le moteur `:core` et le schéma
 * Compose Canvas. L'interface complète (onglets, éditeur, table) suivra.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val machine = remember { createThreeShaftMachine() }
    val aluminum = remember { materialById("aluminum")!! }
    val reco = remember { recommend(machine, aluminum.vcHss, 6.0) }
    val best = reco.best
    val position = machine.belts.mapIndexed { k, belt -> pairName(belt, best.pairIndexes[k]) }.joinToString(" – ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Drill Press Assistant", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Aluminium · Ø 6 mm → position $position — ${best.spindleRpm.roundToInt()} tr/min " +
                formatDeviation(best.spindleRpm, reco.ideal),
            style = MaterialTheme.typography.titleMedium,
        )

        Text("Schéma (Compose Canvas) :", style = MaterialTheme.typography.labelLarge)
        PulleySchematic(
            machine = machine,
            pairs = best.pairs,
            pairIndexes = best.pairIndexes,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Version mini (colonne de table) :", style = MaterialTheme.typography.labelLarge)
        PulleySchematic(
            machine = machine,
            pairs = best.pairs,
            pairIndexes = best.pairIndexes,
            mini = true,
            modifier = Modifier.width(160.dp),
        )
    }
}
