package fr.mgth.drillpress.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mgth.drillpress.core.CARBIDE_FACTOR
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.createThreeShaftMachine
import fr.mgth.drillpress.core.materialById
import fr.mgth.drillpress.core.vcMaterial

/**
 * État applicatif partagé entre les deux onglets : la machine (mutable, avec
 * compteur de révision), les réglages de perçage, la langue et les unités.
 * C'est aussi ce qui est sérialisé pour la persistance.
 */
class AppState {
    var machine by mutableStateOf(createThreeShaftMachine())
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
