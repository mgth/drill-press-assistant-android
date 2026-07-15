package fr.mgth.drillpress.core

/** Vitesses de coupe de perçage, valeurs prudentes pour usage amateur. */
data class Material(
    val id: String,
    val labelFr: String,
    val labelEn: String,
    /** Abréviations courtes pour les jetons de la frise Vc. */
    val abbrFr: String,
    val abbrEn: String,
    /** Vitesse de coupe Vc en m/min pour forets HSS. */
    val vcHss: Double,
)

/** Vc carbure ≈ 2,5 × Vc HSS (ordre de grandeur). */
const val CARBIDE_FACTOR = 2.5

val MATERIALS: List<Material> = listOf(
    Material("steel", "Acier (doux)", "Mild steel", "Acier", "Steel", 25.0),
    Material("stainless", "Acier inoxydable", "Stainless steel", "Inox", "Stainl.", 12.0),
    Material("cast-iron", "Fonte", "Cast iron", "Fonte", "C. iron", 20.0),
    Material("aluminum", "Aluminium", "Aluminium", "Alu", "Alu", 70.0),
    Material("brass", "Laiton / bronze", "Brass / bronze", "Laiton", "Brass", 45.0),
    Material("hardwood", "Bois dur", "Hardwood", "Bois d.", "Hardw.", 40.0),
    Material("softwood", "Bois tendre", "Softwood", "Bois t.", "Softw.", 60.0),
    Material("plastic", "Plastique", "Plastic", "Plast.", "Plast.", 35.0),
)

fun materialById(id: String): Material? = MATERIALS.firstOrNull { it.id == id }

/** Matériau dont la Vc correspond exactement à cette valeur pour ce type de foret, ou null. */
fun vcMaterial(vc: Double, carbide: Boolean): Material? {
    val factor = if (carbide) CARBIDE_FACTOR else 1.0
    return MATERIALS.firstOrNull { it.vcHss * factor == vc }
}

/** Grille de base de la frise Vc (m/min) : fine jusqu'à 50, plus espacée au-delà. */
private val VC_GRID =
    listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 125, 150, 175, 200)
        .map { it.toDouble() }

/**
 * Valeurs de la frise Vc pour un type de foret, toujours en m/min : grille de
 * base (métrique, ou grille SFM convertie en impérial) plus les Vc des
 * matériaux pour ce type.
 */
fun vcChipValues(carbide: Boolean, imperial: Boolean = false): List<Double> {
    val factor = if (carbide) CARBIDE_FACTOR else 1.0
    val grid = if (imperial) SFM_GRID.map { it / SFM_PER_M_MIN } else VC_GRID
    return (grid + MATERIALS.map { it.vcHss * factor }).distinct().sorted()
}
