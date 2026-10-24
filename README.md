# kmp-rtc-sample

Application exemple **Kotlin Multiplatform + Compose Material 3** pour [kmp-rtc-server](../kmp-rtc-server).

Créez un compte, connectez-vous, et appelez les autres utilisateurs du même serveur :

- appels **audio**
- appels **vidéo** (caméra frontale, vidéo plein écran + vignette)
- **conférences** (salles avec ou sans PIN, clavier DTMF)
- **historique** des appels
- **test d'écho** (numéro 600)

## Démarrer

1. Lancez kmp-rtc-server (`docker compose up -d` dans `kmp-rtc-server/`).
2. Ouvrez `composeApp/src/commonMain/kotlin/org/kmprtc/sample/Config.kt` et mettez l'IP du serveur :

   ```kotlin
   const val SERVER_HOST = "192.168.1.20"
   ```

3. Installez sur deux téléphones Android :

   ```bash
   ./gradlew :composeApp:installDebug
   ```

4. Créez un compte sur chaque téléphone, puis appelez l'autre depuis l'onglet **Contacts**.

Toutes les copies de l'application avec le **même `SERVER_HOST`** peuvent s'appeler.

Desktop (interface seule, sans son) : `./gradlew :composeApp:run`

## Où regarder

| Fichier | Rôle |
|---|---|
| `Config.kt` | Adresse du serveur |
| `AppViewModel.kt` | Le parcours complet : login, compte SIP, appels, salles |
| `data/RtcApi.kt` | Client de l'API REST |
| `call/CallEngine.kt` | Interface commune du moteur d'appel |
| `androidMain/.../PjsipCallEngine.kt` | Moteur Android (PJSIP) : audio, vidéo, DTMF |
| `ui/screens/` | Écrans Material 3 : connexion, accueil, appel |

Guide pas à pas : `http://<serveur>/docs/#/09-integration-kmp`.
