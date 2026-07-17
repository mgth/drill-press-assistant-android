package fr.mgth.drillpress.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mgth.drillpress.core.BitType
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.createThreeShaftMachine
import fr.mgth.drillpress.core.createTwoShaftMachine
import fr.mgth.drillpress.core.materialById
import fr.mgth.drillpress.core.materialVc
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

    /**
     * Révision structurelle : incrémentée quand la FORME de la machine change
     * (étages, positions de courroie, cônes, bascules). L'onglet Machine
     * reconstruit son sous-arbre via key(structRev) — déterministe, immune aux
     * heuristiques de skipping de Compose face à notre modèle mutable. Les
     * frappes clavier n'incrémentent que rev, pour garder la saisie fluide.
     */
    var structRev by mutableIntStateOf(0)
    var lang by mutableStateOf(Lang.FR)
    var units by mutableStateOf(Units.METRIC)

    var materialId by mutableStateOf("steel")
    var bitType by mutableStateOf(BitType.HSS)
    var vcOverride by mutableStateOf<Double?>(null)
    var diameterMm by mutableStateOf(8.0)
    var selectedKey by mutableStateOf<String?>(null)

    val t: Strings get() = if (lang == Lang.EN) EN else FR
    val imperial: Boolean get() = units == Units.IMPERIAL
    val custom: Boolean get() = vcOverride != null
    val material get() = materialById(materialId) ?: materialById("steel")!!
    val vc: Double get() = vcOverride ?: materialVc(material, bitType.factor)

    /** Machine courante (la liste est garantie non vide après [ensureMachine]). */
    val machine: Machine get() = machines.firstOrNull { it.id == currentId } ?: machines.first()

    fun ensureMachine() {
        if (machines.isEmpty()) addThreeShaft()
        if (machines.none { it.id == currentId }) currentId = machines.first().id
    }

    fun addTwoShaft() {
        val m = createTwoShaftMachine(factoryLabels(lang)); machines.add(m); currentId = m.id
        touchStructure()
    }

    fun addThreeShaft() {
        val m = createThreeShaftMachine(factoryLabels(lang)); machines.add(m); currentId = m.id
        touchStructure()
    }

    fun removeMachine(id: String) {
        if (machines.size <= 1) return
        machines.removeAll { it.id == id }
        if (machines.none { it.id == currentId }) currentId = machines.first().id
        touchStructure()
    }

    fun selectMachine(id: String) { currentId = id; touchStructure() }

    /** Signale une édition en place de la machine (recomposition + reset sélection). */
    fun touch() { rev++; selectedKey = null }

    /** Signale un changement de structure (reconstruction de l'onglet Machine). */
    fun touchStructure() { structRev++; touch() }

    fun chooseMaterial(id: String) { materialId = id; vcOverride = null; selectedKey = null }
    fun chooseBit(b: BitType) { bitType = b; vcOverride = null; selectedKey = null }
    fun chooseDiameter(mm: Double) { if (mm > 0) { diameterMm = mm; selectedKey = null } }
    fun chooseVc(v: Double) {
        val m = vcMaterial(v, bitType.factor)
        if (m != null) { materialId = m.id; vcOverride = null } else vcOverride = v
        selectedKey = null
    }

    /**
     * Change la langue en traduisant au passage les libellés d'usine encore
     * intacts (noms de machine, d'arbres et de cônes issus des gabarits) —
     * les libellés personnalisés ne sont pas touchés.
     */
    fun switchLang(newLang: Lang) {
        if (newLang == lang) return
        val from = factoryLabels(lang)
        val to = factoryLabels(newLang)
        val map = mapOf(
            from.twoShaftName to to.twoShaftName, from.threeShaftName to to.threeShaftName,
            from.motorShaft to to.motorShaft, from.intermediateShaft to to.intermediateShaft,
            from.spindleShaft to to.spindleShaft,
            from.motorCone to to.motorCone, from.spindleCone to to.spindleCone,
            from.intermediateCone to to.intermediateCone,
            from.intermediateConeIn to to.intermediateConeIn,
            from.intermediateConeOut to to.intermediateConeOut,
        )
        machines.forEach { m ->
            m.name = map[m.name] ?: m.name
            m.shafts.forEach { sh ->
                sh.label = map[sh.label] ?: sh.label
                sh.stacks.forEach { st -> st.label = map[st.label] ?: st.label }
            }
        }
        lang = newLang
        touchStructure()
    }
}
