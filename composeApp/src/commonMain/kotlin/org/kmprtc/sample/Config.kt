package org.kmprtc.sample

// Le SEUL reglage a changer pour tester : l'adresse de votre kmp-rtc-server.
// Toutes les applications qui utilisent la meme adresse peuvent s'appeler entre elles.
//   - Reseau local      : IP LAN du PC qui fait tourner Docker (ex: "192.168.1.20")
//   - Emulateur Android : "10.0.2.2" si Docker tourne sur le meme PC
//   - Production        : "rtc.example.com" et passez API_BASE_URL en https
const val SERVER_HOST = "192.168.56.200"
const val API_BASE_URL = "http://$SERVER_HOST"

// Numero de test d'echo du serveur (votre voix et votre video reviennent vers vous).
const val ECHO_TEST_NUMBER = "600"
