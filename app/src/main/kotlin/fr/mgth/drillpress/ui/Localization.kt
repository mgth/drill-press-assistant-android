package fr.mgth.drillpress.ui

import fr.mgth.drillpress.core.DEFAULT_FACTORY_LABELS
import fr.mgth.drillpress.core.FactoryLabels
import fr.mgth.drillpress.core.Issue
import fr.mgth.drillpress.core.IssueCode
import fr.mgth.drillpress.core.MM_PER_INCH
import fr.mgth.drillpress.core.Material
import fr.mgth.drillpress.core.SFM_PER_M_MIN
import kotlin.math.floor
import kotlin.math.roundToLong

enum class Lang(val code: String) { FR("FR"), EN("EN") }
enum class Units(val code: String) { METRIC("mm"), IMPERIAL("in") }

/** Chaînes de l'interface pour une langue. Issues rendues par code + params. */
class Strings(
    val tabMachine: String,
    val tabDrilling: String,
    val tabAbout: String,
    // Picker
    val pickerMachine: String,
    val newTwoShaft: String,
    val newThreeShaft: String,
    val deleteMachine: String,
    val deleteConfirm: String,
    // Machine
    val name: String,
    val motorRpm: String,
    val spindleLeft: String,
    val sharedCone: String,
    val step: String,
    val addStep: String,
    val belt: String,
    val pairRep: String,
    val addPair: String,
    val resetPairs: String,
    // Advisor
    val drillingParams: String,
    val material: String,
    val custom: String,
    val bitType: String,
    val hss: String,
    val hssCo: String,
    val carbide: String,
    val diameter: String,
    val exactDiameter: String,
    val cuttingSpeed: String,
    val idealSpeed: String,
    val recommendedPosition: String,
    val overspeed: String,
    // Table
    val allSpeeds: String,
    val allDiameters: String,
    val recommendedBadge: String,
    val selectedBadge: String,
    val colPosition: String,
    val colDeviation: String,
    val colRange: String,
    // Divers
    val cancel: String,
    val delete: String,
    val deleteStepConfirm: String,
    val deletePairConfirm: String,
    // About
    val aboutDescription: String,
    val aboutVersion: String,
    val aboutSource: String,
    val aboutIssues: String,
    val aboutLicense: String,
    private val issues: Map<IssueCode, (Map<String, Any>) -> String>,
) {
    fun issue(i: Issue): String = issues.getValue(i.code)(i.params)
}

val FR = Strings(
    tabMachine = "Machine", tabDrilling = "Perçage", tabAbout = "À propos",
    pickerMachine = "Machine", newTwoShaft = "Nouvelle (2 arbres)", newThreeShaft = "Nouvelle (3 arbres)",
    deleteMachine = "Supprimer", deleteConfirm = "Supprimer cette machine ?",
    name = "Nom", motorRpm = "Vitesse moteur (tr/min)",
    spindleLeft = "Broche à gauche sur le schéma",
    sharedCone = "Cône unique (entrée et sortie sur le même cône)",
    step = "Ét.", addStep = "Ajouter un étage", belt = "Courroie", pairRep = "Rep.",
    addPair = "Ajouter une position", resetPairs = "Réinitialiser",
    drillingParams = "Paramètres de perçage", material = "Matériau", custom = "Personnalisé",
    bitType = "Type de foret", hss = "HSS", hssCo = "HSS-Co", carbide = "Carbure",
    diameter = "Ø de perçage", exactDiameter = "Ø exact",
    cuttingSpeed = "Vitesse de coupe", idealSpeed = "Vitesse idéale",
    recommendedPosition = "Position recommandée",
    overspeed = "Même la combinaison la plus lente dépasse la vitesse idéale : réduisez la vitesse d'avance et lubrifiez.",
    allSpeeds = "Toutes les vitesses", allDiameters = "tous",
    recommendedBadge = "recommandé", selectedBadge = "affiché",
    colPosition = "Position", colDeviation = "Écart", colRange = "Plage Ø",
    cancel = "Annuler", delete = "Supprimer",
    deleteStepConfirm = "Supprimer cet étage ?",
    deletePairConfirm = "Supprimer cette position de courroie ?",
    aboutDescription = "Calcule les vitesses de broche d'une perceuse à colonne à transmission par courroies et recommande la position optimale des courroies pour un diamètre de perçage et un matériau donnés.",
    aboutVersion = "Version", aboutSource = "Code source (GitHub)",
    aboutIssues = "Signaler un problème", aboutLicense = "Licence GPL-3.0",
    issues = mapOf(
        IssueCode.MOTOR_RPM to { "La vitesse moteur doit être supérieure à 0." },
        IssueCode.MIN_SHAFTS to { "Il faut au moins deux arbres (moteur et broche)." },
        IssueCode.BELT_COUNT to { "Il faut exactement une courroie entre chaque paire d'arbres consécutifs." },
        IssueCode.EMPTY_STACK to { p -> "« ${p["stack"]} » (${p["shaft"]}) n'a aucun étage." },
        IssueCode.BAD_DIAMETER to { p -> "« ${p["stack"]} » (${p["shaft"]}) a un diamètre invalide (doit être > 0)." },
        IssueCode.BELT_CHAIN to { p -> "Courroie ${p["belt"]} : doit relier l'arbre ${p["from"]} à l'arbre ${p["to"]}." },
        IssueCode.STACK_MISSING to { p -> "Courroie ${p["belt"]} : cône introuvable." },
        IssueCode.NO_PAIRS to { p -> "Courroie ${p["belt"]} : aucune position définie." },
        IssueCode.PAIR_OUT_OF_RANGE to { p -> "Courroie ${p["belt"]} : position (${p["i"]}, ${p["j"]}) hors limites." },
        IssueCode.DIAMETER_SUM to { p -> "Courroie ${p["belt"]} : la somme des diamètres varie de plus de ${p["tolerance"]} % entre positions — vérifiez la saisie." },
        IssueCode.NO_COMBINATION to { "Aucune combinaison possible : sur un cône partagé, les deux courroies ne peuvent pas occuper le même étage." },
    ),
)

val EN = Strings(
    tabMachine = "Machine", tabDrilling = "Drilling", tabAbout = "About",
    pickerMachine = "Machine", newTwoShaft = "New (2 shafts)", newThreeShaft = "New (3 shafts)",
    deleteMachine = "Delete", deleteConfirm = "Delete this machine?",
    name = "Name", motorRpm = "Motor speed (rpm)",
    spindleLeft = "Spindle on the left of the diagram",
    sharedCone = "Single cone (input and output on the same cone)",
    step = "St.", addStep = "Add a step", belt = "Belt", pairRep = "Label",
    addPair = "Add a position", resetPairs = "Reset",
    drillingParams = "Drilling parameters", material = "Material", custom = "Custom",
    bitType = "Bit type", hss = "HSS", hssCo = "HSS-Co", carbide = "Carbide",
    diameter = "Drill Ø", exactDiameter = "Exact Ø",
    cuttingSpeed = "Cutting speed", idealSpeed = "Ideal speed",
    recommendedPosition = "Recommended position",
    overspeed = "Even the slowest combination exceeds the ideal speed: reduce the feed rate and lubricate.",
    allSpeeds = "All speeds", allDiameters = "all",
    recommendedBadge = "recommended", selectedBadge = "shown",
    colPosition = "Position", colDeviation = "Dev.", colRange = "Ø range",
    cancel = "Cancel", delete = "Delete",
    deleteStepConfirm = "Delete this step?",
    deletePairConfirm = "Delete this belt position?",
    aboutDescription = "Calculates the spindle speeds of a belt-driven drill press and recommends the optimal belt position for a given drill diameter and material.",
    aboutVersion = "Version", aboutSource = "Source code (GitHub)",
    aboutIssues = "Report an issue", aboutLicense = "GPL-3.0 license",
    issues = mapOf(
        IssueCode.MOTOR_RPM to { "Motor speed must be greater than 0." },
        IssueCode.MIN_SHAFTS to { "At least two shafts are required (motor and spindle)." },
        IssueCode.BELT_COUNT to { "Exactly one belt is required between each pair of consecutive shafts." },
        IssueCode.EMPTY_STACK to { p -> "“${p["stack"]}” (${p["shaft"]}) has no steps." },
        IssueCode.BAD_DIAMETER to { p -> "“${p["stack"]}” (${p["shaft"]}) has an invalid diameter (must be > 0)." },
        IssueCode.BELT_CHAIN to { p -> "Belt ${p["belt"]}: must connect shaft ${p["from"]} to shaft ${p["to"]}." },
        IssueCode.STACK_MISSING to { p -> "Belt ${p["belt"]}: cone not found." },
        IssueCode.NO_PAIRS to { p -> "Belt ${p["belt"]}: no position defined." },
        IssueCode.PAIR_OUT_OF_RANGE to { p -> "Belt ${p["belt"]}: position (${p["i"]}, ${p["j"]}) out of range." },
        IssueCode.DIAMETER_SUM to { p -> "Belt ${p["belt"]}: the diameter sum varies by more than ${p["tolerance"]}% across positions — check your input." },
        IssueCode.NO_COMBINATION to { "No combination is possible: on a shared cone, the two belts cannot use the same step." },
    ),
)

// ---- Unités ----

private fun num(v: Double, lang: Lang): String {
    val r = if (v == floor(v)) v.toLong().toString() else (Math.round(v * 100) / 100.0).toString()
    return if (lang == Lang.FR) r.replace(".", ",") else r
}

fun lenUnit(u: Units) = if (u == Units.IMPERIAL) "in" else "mm"
fun vcUnit(u: Units) = if (u == Units.IMPERIAL) "SFM" else "m/min"
fun rpmUnit(lang: Lang) = if (lang == Lang.EN) "rpm" else "tr/min"

/** mm → texte dans l'unité courante. */
fun formatLen(mm: Double, u: Units, lang: Lang): String =
    if (u == Units.IMPERIAL) num(Math.round(mm / MM_PER_INCH * 100) / 100.0, lang)
    else num(if (mm >= 10) Math.round(mm * 10) / 10.0 else Math.round(mm * 100) / 100.0, lang)

/** valeur affichée → mm. */
fun parseLen(display: Double, u: Units): Double = if (u == Units.IMPERIAL) display * MM_PER_INCH else display

/** mm → valeur numérique affichée (pour le champ). */
fun displayLen(mm: Double, u: Units): Double =
    if (u == Units.IMPERIAL) Math.round(mm / MM_PER_INCH * 1000) / 1000.0 else mm

/** Vc m/min → texte dans l'unité courante. */
fun formatVc(mPerMin: Double, u: Units, lang: Lang): String =
    if (u == Units.IMPERIAL) (mPerMin * SFM_PER_M_MIN).roundToLong().toString() else num(mPerMin, lang)

fun materialLabel(m: Material, lang: Lang) = if (lang == Lang.EN) m.labelEn else m.labelFr
fun materialAbbr(m: Material, lang: Lang) = if (lang == Lang.EN) m.abbrEn else m.abbrFr

/** Libellés des gabarits de machine selon la langue. */
fun factoryLabels(lang: Lang): FactoryLabels =
    if (lang == Lang.EN) FactoryLabels(
        twoShaftName = "2-shaft drill press", threeShaftName = "3-shaft drill press",
        motorShaft = "Motor", intermediateShaft = "Intermediate", spindleShaft = "Spindle",
        motorCone = "Motor cone", spindleCone = "Spindle cone", intermediateCone = "Intermediate cone",
        intermediateConeIn = "Intermediate cone (input)", intermediateConeOut = "Intermediate cone (output)",
    ) else DEFAULT_FACTORY_LABELS
