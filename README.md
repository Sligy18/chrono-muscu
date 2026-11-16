# Chrono Muscu

Minuteur de repos entre les séries de musculation, pour usage privé.

- 6 chronos réglables (25", 1'00", 1'30", 2'00", 3'00", 4'00" par défaut)
- Compteur de séries (-5, -1, +1, +5)
- Son de fin (Bip, Bois ou aucun), vibration, baisse de la musique pendant la sonnerie
- Sonne à l'heure même écran éteint

## Installer sur le téléphone

Chaque envoi sur `main` compile l'app sur GitHub Actions et publie l'APK dans **Releases**.
Sur le téléphone : ouvrir la dernière release, toucher `ChronoMuscu-v1.0.N.apk`, autoriser l'installation.
Les mises à jour s'installent par-dessus sans perdre les réglages (même clé de signature).

## Clé de signature

La clé (`signing/release.p12`, exclue de Git) est stockée en base64 dans le secret `KEYSTORE_BASE64`,
avec `KEYSTORE_PASSWORD`, `KEY_ALIAS` et `KEY_PASSWORD`. **La garder précieusement** : sans elle,
une nouvelle version ne pourra pas s'installer par-dessus l'ancienne.
