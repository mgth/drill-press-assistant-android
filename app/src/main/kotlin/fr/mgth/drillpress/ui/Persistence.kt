package fr.mgth.drillpress.ui

import android.content.Context
import fr.mgth.drillpress.core.Belt
import fr.mgth.drillpress.core.BitType
import fr.mgth.drillpress.core.Machine
import fr.mgth.drillpress.core.PulleyStack
import fr.mgth.drillpress.core.Shaft
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistance : sérialisation JSON de l'AppState (machine + réglages + langue +
 * unités) dans les SharedPreferences. Choix volontaire de SharedPreferences —
 * synchrone et sans cérémonie pour un petit blob lu au démarrage et écrit à
 * chaque changement ; DataStore serait un remplacement direct si besoin.
 */
private const val PREFS = "drill_press_state"
private const val KEY = "state"
private const val VERSION = 2

private fun machineToJson(m: Machine): JSONObject = JSONObject().apply {
    put("id", m.id)
    put("name", m.name)
    put("motorRpm", m.motorRpm)
    put("spindleLeft", m.spindleLeft)
    put("shafts", JSONArray(m.shafts.map { sh ->
        JSONObject().apply {
            put("id", sh.id)
            put("label", sh.label)
            put("stacks", JSONArray(sh.stacks.map { st ->
                JSONObject().apply {
                    put("id", st.id)
                    put("label", st.label)
                    put("steps", JSONArray(st.steps))
                }
            }))
        }
    }))
    put("belts", JSONArray(m.belts.map { b ->
        JSONObject().apply {
            put("fromShaft", b.fromShaft); put("fromStack", b.fromStack)
            put("toShaft", b.toShaft); put("toStack", b.toStack)
            put("allowedPairs", JSONArray(b.allowedPairs.map { JSONArray(listOf(it.first, it.second)) }))
            put("pairNames", JSONArray(b.pairNames ?: emptyList<String>()))
        }
    }))
}

private fun machineFromJson(o: JSONObject): Machine {
    val shafts = o.getJSONArray("shafts").let { arr ->
        (0 until arr.length()).map { i ->
            val sh = arr.getJSONObject(i)
            val stacks = sh.getJSONArray("stacks").let { sa ->
                (0 until sa.length()).map { j ->
                    val st = sa.getJSONObject(j)
                    val steps = st.getJSONArray("steps").let { ss ->
                        (0 until ss.length()).map { ss.getDouble(it) }.toMutableList()
                    }
                    PulleyStack(st.getString("id"), st.getString("label"), steps)
                }.toMutableList()
            }
            Shaft(sh.getString("id"), sh.getString("label"), stacks)
        }.toMutableList()
    }
    val belts = o.getJSONArray("belts").let { arr ->
        (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            val pairs = b.getJSONArray("allowedPairs").let { pa ->
                (0 until pa.length()).map { pa.getJSONArray(it).let { p -> p.getInt(0) to p.getInt(1) } }.toMutableList()
            }
            val names = b.getJSONArray("pairNames").let { na ->
                (0 until na.length()).map { na.getString(it) }.toMutableList()
            }
            Belt(b.getInt("fromShaft"), b.getInt("fromStack"), b.getInt("toShaft"), b.getInt("toStack"),
                pairs, if (names.isEmpty()) null else names)
        }.toMutableList()
    }
    return Machine(o.getString("id"), o.getString("name"), o.getDouble("motorRpm"), shafts, belts,
        o.optBoolean("spindleLeft", false))
}

fun saveState(context: Context, app: AppState) {
    val json = JSONObject().apply {
        put("version", VERSION)
        put("machines", JSONArray(app.machines.map { machineToJson(it) }))
        put("currentId", app.currentId)
        put("advisor", JSONObject().apply {
            put("materialId", app.materialId)
            put("bitType", app.bitType.name)
            if (app.vcOverride != null) put("vcOverride", app.vcOverride)
            put("diameterMm", app.diameterMm)
        })
        put("lang", app.lang.name)
        put("units", app.units.name)
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
}

fun loadState(context: Context, app: AppState) {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return
    val o = runCatching { JSONObject(raw) }.getOrNull() ?: return
    if (o.optInt("version") != VERSION) return
    runCatching {
        val arr = o.getJSONArray("machines")
        app.machines.clear()
        for (i in 0 until arr.length()) app.machines.add(machineFromJson(arr.getJSONObject(i)))
        app.currentId = o.optString("currentId", app.machines.firstOrNull()?.id ?: "")
        o.optJSONObject("advisor")?.let { a ->
            app.materialId = a.optString("materialId", "steel")
            // Anciennes sauvegardes : booléen « carbide » au lieu du type de foret.
            app.bitType = runCatching { BitType.valueOf(a.getString("bitType")) }
                .getOrElse { if (a.optBoolean("carbide", false)) BitType.CARBIDE else BitType.HSS }
            app.vcOverride = if (a.has("vcOverride")) a.getDouble("vcOverride") else null
            app.diameterMm = a.optDouble("diameterMm", 8.0)
        }
        app.lang = runCatching { Lang.valueOf(o.optString("lang", "FR")) }.getOrDefault(Lang.FR)
        app.units = runCatching { Units.valueOf(o.optString("units", "METRIC")) }.getOrDefault(Units.METRIC)
    }
}
