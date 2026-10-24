package org.kmprtc.sample.data

/** Petit stockage cle/valeur (SharedPreferences sur Android, java.util.prefs sur Desktop). */
expect object Prefs {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}
