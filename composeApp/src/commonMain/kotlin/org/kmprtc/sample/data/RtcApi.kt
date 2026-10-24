package org.kmprtc.sample.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import org.kmprtc.sample.API_BASE_URL

expect fun createHttpClient(): HttpClient

/**
 * Toute l'API kmp-rtc-server utilisee par l'application, en un seul fichier.
 * Chaque methode renvoie ApiResponse : success=false + message en cas d'erreur (jamais d'exception).
 */
class RtcApi(
    private val client: HttpClient = createHttpClient(),
    private val baseUrl: String = API_BASE_URL,
) {
    var token: String? = Prefs.get(TOKEN_KEY)
        private set

    // ─── Compte ───────────────────────────────────────────────────────────────
    suspend fun register(req: RegisterRequest) = send<User> { client.post("$baseUrl/auth/register") { json(req) } }

    suspend fun login(req: LoginRequest): ApiResponse<AuthData> {
        val res = send<AuthData> { client.post("$baseUrl/auth/login") { json(req) } }
        res.data?.let { saveToken(it.token) }
        return res
    }

    suspend fun logout() {
        send<JsonElement> { client.post("$baseUrl/auth/logout") { auth() } }
        saveToken(null)
    }

    suspend fun me() = send<User> { client.get("$baseUrl/users/me") { auth() } }
    suspend fun users() = send<List<User>> { client.get("$baseUrl/users") { auth() } }

    // ─── SIP ──────────────────────────────────────────────────────────────────
    suspend fun sipAccount() = send<SipAccount> { client.get("$baseUrl/sip/account") { auth() } }
    suspend fun sipPeer(userId: String) = send<SipPeer> { client.get("$baseUrl/sip/peer/$userId") { auth() } }

    // ─── Conferences ──────────────────────────────────────────────────────────
    suspend fun rooms() = send<List<Room>> { client.get("$baseUrl/rooms") { auth() } }
    suspend fun createRoom(req: CreateRoomRequest) = send<Room> { client.post("$baseUrl/rooms") { auth(); json(req) } }
    suspend fun deleteRoom(id: String) = send<JsonElement> { client.delete("$baseUrl/rooms/$id") { auth() } }

    // ─── Historique ───────────────────────────────────────────────────────────
    suspend fun history(limit: Int = 50) = send<List<CallRecord>> { client.get("$baseUrl/calls/history?limit=$limit") { auth() } }

    suspend fun isServerUp(): Boolean = runCatching { client.get("$baseUrl/health").status.value in 200..299 }.getOrDefault(false)

    // ─── Outils ───────────────────────────────────────────────────────────────
    private fun HttpRequestBuilder.auth() {
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    private fun HttpRequestBuilder.json(body: Any) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun saveToken(value: String?) {
        token = value
        Prefs.put(TOKEN_KEY, value)
    }

    private suspend inline fun <reified T> send(request: () -> HttpResponse): ApiResponse<T> = try {
        val response = request()
        if (response.status == HttpStatusCode.Unauthorized && token != null) {
            saveToken(null)
            ApiResponse(false, "Session expiree, reconnectez-vous")
        } else {
            response.body()
        }
    } catch (t: Throwable) {
        ApiResponse(false, "Serveur injoignable ($baseUrl)")
    }

    private companion object {
        const val TOKEN_KEY = "token"
    }
}
