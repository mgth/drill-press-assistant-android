package fr.mgth.drillpress.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mgth.drillpress.core.CARBIDE_FACTOR
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.createThreeShaftMachine
import fr.mgth.drillpress.core.createTwoShaftMachine
import fr.mgth.drillpress.core.materialById
import fr.mgth.drillpress.core.vcMaterial

/**
 * État applicatif partagé : liste de machines + machine courante (mutables,
 * avec compteur de révision), réglages de perçage, langue, unités. C'est ce
 * qui est sérialisé pour la persistance.
 */
class AppState {
    val machines = mutableStateListOf<Machine>()
    var currentId by mutableStateOf("")
    var rev by mutableIntStateOf(0)
    var lang by mutableStateOf(Lang.FR)
    var units by mutableStateOf(Units.METRIC)

    var materialId by mutableStateOf("steel")
    var carbide by mutableStateOf(false)
    var vcOverride by mutableStateOf<Double?>(null)
    var diameterMm by mutableStateOf(8.0)
    var selectedKey by mutableStateOf<String?>(null)

    val t: Strings get() = if (lang == Lang.EN) EN else FR
    val imperial: Boolean get() = units == Units.IMPERIAL
    val custom: Boolean get() = vcOverride != null
    val material get() = materialById(materialId) ?: materialById("steel")!!
    val vc: Double get() = vcOverride ?: material.vcHss * (if (carbide) CARBIDE_FACTOR else 1.0)

    /** Machine courante (la liste est garantie non vide après [ensureMachine]). */
    val machine: Machine get() = machines.firstOrNull { it.id == currentId } ?: machines.first()

    fun ensureMachine() {
        if (machines.isEmpty()) addThreeShaft()
        if (machines.none { it.id == currentId }) currentId = machines.first().id
    }

    fun addTwoShaft() {
        val m = createTwoShaftMachine(factoryLabels(lang)); machines.add(m); currentId = m.id; selectedKey = null
    }

    fun addThreeShaft() {
        val m = createThreeShaftMachine(factoryLabels(lang)); machines.add(m); currentId = m.id; selectedKey = null
    }

    fun removeMachine(id: String) {
        if (machines.size <= 1) return
        machines.removeAll { it.id == id }
        if (machines.none { it.id == currentId }) currentId = machines.first().id
        selectedKey = null
    }

    fun selectMachine(id: String) { currentId = id; selectedKey = null }

    /** Signale une édition en place de la machine (recomposition + reset sélection). */
    fun touch() { rev++; selectedKey = null }

    fun chooseMaterial(id: String) { materialId = id; vcOverride = null; selectedKey = null }
    fun chooseCarbide(c: Boolean) { carbide = c; vcOverride = null; selectedKey = null }
    fun chooseDiameter(mm: Double) { if (mm > 0) { diameterMm = mm; selectedKey = null } }
    fun chooseVc(v: Double) {
        val m = vcMaterial(v, carbide)
        if (m != null) { materialId = m.id; vcOverride = null } else vcOverride = v
        selectedKey = null
    }
}
