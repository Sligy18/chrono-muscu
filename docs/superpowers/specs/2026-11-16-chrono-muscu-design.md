# Chrono Muscu - conception (validée le 2026-11-16)

## Intention
Remplacer l'app « Chrono Lafay » (2016, v3.0.3) par une app Android moderne, pour l'usage privé de Gilles :
un minuteur de repos entre les séries de musculation au poids du corps.

## Objectifs
- Écran principal : grand décompte, 6 boutons de chrono préréglés (25", 1'00", 1'30", 2'00", 3'00", 4'00"),
  compteur de séries (-5, -1, +1, +5). Mise en page paysage.
- Réglages : durée de chaque bouton (minutes + secondes), son de fin (Bip / Bois / Aucun),
  vibration, baisse du volume des autres sons pendant la sonnerie (audio focus « duck »).
- Le chrono sonne à l'heure écran éteint ou app en arrière-plan ; notification avec le temps restant.
- Normes actuelles : Kotlin, Jetpack Compose, Material 3 (couleurs dynamiques, thème sombre), edge-to-edge,
  targetSdk 36, permission POST_NOTIFICATIONS demandée à l'exécution.

## Non-objectifs
- Publication sur le Play Store, publicité, analytics, Firebase, Cast, app Wear OS.

## Anti-objectifs
- Reprendre l'icône, les sons ou le nom d'origine.
- Un chrono qui dérive ou se tait quand l'écran est éteint.

## Approches
1. **Compilation sur GitHub Actions, dépôt privé (retenue)** : rien à installer sur le Mac ; l'APK signé est publié
   dans les Releases et installé depuis le téléphone.
2. Android Studio en local : permet l'émulateur, mais plusieurs Go à installer. Écartée par Gilles.

## Décisions techniques (choix de Claude)
- Décompte basé sur `SystemClock.elapsedRealtime()` (heure de fin), jamais sur un cumul de ticks.
- Service de premier plan (`foregroundServiceType="specialUse"` avec sous-type déclaré) qui tient le décompte,
  met à jour la notification chaque seconde et joue le son de fin. Plus une alarme de secours non nécessaire :
  le service garde un wake lock partiel limité à la durée du chrono.
- Réglages stockés avec DataStore Preferences. Compteur de séries conservé aussi (survit au redémarrage de l'app).
- Sons de fin générés par code (synthèse PCM via `AudioTrack`) : aucun fichier audio tiers, aucun droit à gérer.
- Signature : keystore PKCS12 généré avec openssl, encodé en base64 dans les secrets GitHub ; copie locale hors Git.
- applicationId `fr.gillesray.chronomuscu`, minSdk 26.

## Laissé au constructeur
- Détails visuels (typographie du décompte, disposition exacte des boutons).
- Tests : unitaires sur la logique de format/décompte, exécutés en CI.
