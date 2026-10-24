package org.kmprtc.sample.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
}

actual object Prefs {
    private val node = Preferences.userRoot().node("org.kmprtc.sample")
    actual fun get(key: String): String? = node.get(key, null)
    actual fun put(key: String, value: String?) = if (value == null) node.remove(key) else node.put(key, value)
}
