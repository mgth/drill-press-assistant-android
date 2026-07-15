# Drill Press Assistant — Android (Kotlin)

Portage **natif Android** (Kotlin + Jetpack Compose) de
[drill-press-assistant](https://github.com/mgth/drill-press-assistant) (version
Tauri/Svelte). Calcule les vitesses de broche d'une perceuse à colonne à partir
de sa transmission par courroies et recommande la position optimale des
courroies pour un diamètre de perçage et un matériau donnés.

Native Android (Kotlin + Jetpack Compose) port of the Tauri/Svelte
[drill-press-assistant](https://github.com/mgth/drill-press-assistant).

## État / Status

| Module | Contenu | État |
|---|---|---|
| `:core` | Moteur de calcul pur (machine, combinaisons, recommandation, plages Ø, unités) | ✅ porté, **30 tests JUnit verts** |
| `:app` | Interface Jetpack Compose | ✅ fonctionnelle — schéma Canvas, 2 onglets, conseiller + table, éditeur de machine, **i18n fr/en**, **unités métrique/impérial**, **persistance** ; reste : machines multiples, quelques finitions |

Le moteur `:core` est un module Kotlin/JVM sans dépendance Android : il se teste
sur PC (`./gradlew :core:test`) et produit des résultats identiques à la version
web (mêmes 30 cas de test que la suite vitest d'origine).

## Build

Prérequis : JDK 17+, Android SDK (Platform 35, Build-Tools 35).

```bash
./gradlew :core:test        # tests unitaires du moteur (rapide, sans SDK Android)
./gradlew :app:assembleDebug   # APK debug → app/build/outputs/apk/debug/
```

Créez `local.properties` avec `sdk.dir=/chemin/vers/Android/Sdk` (non versionné).

## Structure

```
core/   Kotlin/JVM pur — logique métier + tests JUnit
app/    Module Android — Jetpack Compose, dépend de :core
```

## Licence

GNU General Public License v3.0 or later (GPL-3.0-or-later) — voir
[LICENSE](LICENSE).
