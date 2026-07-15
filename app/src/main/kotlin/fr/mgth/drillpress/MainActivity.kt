package fr.mgth.drillpress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.core.createTwoShaftMachine
import fr.mgth.drillpress.core.formatDeviation
import fr.mgth.drillpress.core.materialById
import fr.mgth.drillpress.core.recommend
import kotlin.math.roundToInt

/**
 * Écran de démarrage minimal du portage Kotlin : il ne fait qu'exercer le
 * moteur `:core` pour prouver le câblage. L'interface complète (éditeur de
 * machine, conseiller, schéma) sera portée dans les jalons suivants.
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
    val machine = remember { createTwoShaftMachine() }
    val steel = remember { materialById("steel")!! }
    val reco = remember { recommend(machine, steel.vcHss, 8.0) }
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Drill Press Assistant", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Acier · Ø 8 mm → ${reco.best.spindleRpm.roundToInt()} tr/min " +
                formatDeviation(reco.best.spindleRpm, reco.ideal),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Portage Kotlin en cours — moteur de calcul opérationnel (30 tests verts). " +
                "Interface complète à venir.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
