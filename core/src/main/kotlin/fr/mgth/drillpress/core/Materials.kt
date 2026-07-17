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
    /** Le facteur du type de foret (HSS-Co, carbure) ne s'applique qu'aux métaux. */
    val metal: Boolean = true,
)

/** Vc carbure ≈ 2,5 × Vc HSS (ordre de grandeur). */
const val CARBIDE_FACTOR = 2.5

/** Type de foret : le facteur multiplie la Vc HSS de base (métaux uniquement). */
enum class BitType(val factor: Double) { HSS(1.0), HSS_CO(1.25), CARBIDE(CARBIDE_FACTOR) }

val MATERIALS: List<Material> = listOf(
    Material("steel", "Acier (doux)", "Mild steel", "Acier", "Steel", 25.0),
    Material("steel-medium", "Acier (mi-dur)", "Medium steel", "Mi-dur", "Med. st.", 18.0),
    Material("steel-hard", "Acier (dur / allié)", "Hard / alloy steel", "Ac. dur", "Hard st.", 14.0),
    Material("stainless", "Acier inoxydable", "Stainless steel", "Inox", "Stainl.", 12.0),
    Material("cast-iron", "Fonte", "Cast iron", "Fonte", "C. iron", 20.0),
    Material("aluminum", "Aluminium", "Aluminium", "Alu", "Alu", 70.0),
    Material("copper", "Cuivre", "Copper", "Cuivre", "Copper", 50.0),
    Material("brass", "Laiton / bronze", "Brass / bronze", "Laiton", "Brass", 45.0),
    Material("hardwood", "Bois dur", "Hardwood", "Bois d.", "Hardw.", 40.0, metal = false),
    Material("softwood", "Bois tendre", "Softwood", "Bois t.", "Softw.", 60.0, metal = false),
    Material("plastic", "Plastique", "Plastic", "Plast.", "Plast.", 35.0, metal = false),
)

fun materialById(id: String): Material? = MATERIALS.firstOrNull { it.id == id }

/** Vc effective d'un matériau pour ce facteur de foret (les non-métaux l'ignorent). */
fun materialVc(m: Material, factor: Double): Double = m.vcHss * (if (m.metal) factor else 1.0)

/** Matériau dont la Vc correspond exactement à cette valeur pour ce facteur de foret, ou null. */
fun vcMaterial(vc: Double, factor: Double): Material? =
    MATERIALS.firstOrNull { materialVc(it, factor) == vc }

fun vcMaterial(vc: Double, carbide: Boolean): Material? =
    vcMaterial(vc, if (carbide) CARBIDE_FACTOR else 1.0)

/** Grille de base de la frise Vc (m/min) : fine jusqu'à 50, plus espacée au-delà. */
private val VC_GRID =
    listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 125, 150, 175, 200)
        .map { it.toDouble() }

/**
 * Valeurs de la frise Vc pour un facteur de foret, toujours en m/min : grille
 * de base (métrique, ou grille SFM convertie en impérial) plus les Vc des
 * matériaux pour ce facteur.
 */
fun vcChipValues(factor: Double, imperial: Boolean = false): List<Double> {
    val grid = if (imperial) SFM_GRID.map { it / SFM_PER_M_MIN } else VC_GRID
    return (grid + MATERIALS.map { materialVc(it, factor) }).distinct().sorted()
}

fun vcChipValues(carbide: Boolean, imperial: Boolean = false): List<Double> =
    vcChipValues(if (carbide) CARBIDE_FACTOR else 1.0, imperial)
