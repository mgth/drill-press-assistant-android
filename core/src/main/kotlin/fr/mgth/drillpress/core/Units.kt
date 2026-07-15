package fr.mgth.drillpress.core

/** Conversions métrique ↔ impérial. Le modèle stocke toujours mm et m/min. */

const val MM_PER_INCH = 25.4

/** 1 m/min en pieds/min (SFM). */
const val SFM_PER_M_MIN = 3.280839895

data class ImperialDrill(val label: String, val mm: Double)

/** Forets fractionnaires courants pour la frise de diamètres en impérial. */
val IMPERIAL_DRILLS: List<ImperialDrill> = listOf(
    "1/16" to 1.0 / 16, "5/64" to 5.0 / 64, "3/32" to 3.0 / 32, "1/8" to 1.0 / 8,
    "5/32" to 5.0 / 32, "3/16" to 3.0 / 16, "7/32" to 7.0 / 32, "1/4" to 1.0 / 4,
    "5/16" to 5.0 / 16, "3/8" to 3.0 / 8, "7/16" to 7.0 / 16, "1/2" to 1.0 / 2,
    "9/16" to 9.0 / 16, "5/8" to 5.0 / 8, "3/4" to 3.0 / 4, "1" to 1.0,
).map { (label, inches) -> ImperialDrill(label, inches * MM_PER_INCH) }

/** Grille de base de la frise Vc en impérial (SFM), convertie en m/min à l'usage. */
val SFM_GRID = listOf(25, 50, 75, 100, 125, 150, 200, 250, 300, 350, 400, 500, 600, 650)
