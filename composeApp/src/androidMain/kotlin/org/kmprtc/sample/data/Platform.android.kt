package org.kmprtc.sample.data

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.kmprtc.sample.AppContext

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
}

actual object Prefs {
    private val prefs by lazy { AppContext.get().getSharedPreferences("kmp_rtc_sample", Context.MODE_PRIVATE) }
    actual fun get(key: String): String? = prefs.getString(key, null)
    actual fun put(key: String, value: String?) = prefs.edit().putString(key, value).apply()
}
