package org.kmprtc.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kmprtc.sample.call.CallEngine
import org.kmprtc.sample.call.createCallEngine
import org.kmprtc.sample.data.CallRecord
import org.kmprtc.sample.data.Contact
import org.kmprtc.sample.data.CreateRoomRequest
import org.kmprtc.sample.data.LoginRequest
import org.kmprtc.sample.data.RegisterRequest
import org.kmprtc.sample.data.Room
import org.kmprtc.sample.data.RtcApi
import org.kmprtc.sample.data.SipAccount
import org.kmprtc.sample.data.User

data class UiState(
    val booting: Boolean = true,
    val me: User? = null,
    val sip: SipAccount? = null,
    val contacts: List<Contact> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val history: List<CallRecord> = emptyList(),
    val loading: Boolean = false,
    val serverUp: Boolean? = null,
    val message: String? = null,
)

/**
 * Tout le parcours d'integration en un endroit :
 * login -> GET /sip/account -> engine.start() -> GET /sip/peer/{id} -> engine.dial()
 */
class AppViewModel(
    private val api: RtcApi = RtcApi(),
    val engine: CallEngine = createCallEngine(),
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(serverUp = api.isServerUp()) }
            if (api.token != null) {
                val me = api.me().data
                if (me != null) onSignedIn(me)
            }
            _state.update { it.copy(booting = false) }
        }
    }

    // ─── Compte ───────────────────────────────────────────────────────────────

    fun login(email: String, password: String) = busy {
        val res = api.login(LoginRequest(email.trim(), password))
        val data = res.data ?: return@busy say(res.message ?: "Email ou mot de passe incorrect")
        onSignedIn(data.user)
    }

    fun register(name: String, email: String, phone: String, password: String) = busy {
        val res = api.register(RegisterRequest(name.trim(), email.trim(), phone.trim(), password))
        if (!res.success) return@busy say(res.message ?: "Inscription impossible")
        // Le compte SIP est cree par le serveur : on se connecte tout de suite.
        val login = api.login(LoginRequest(email.trim(), password))
        login.data?.let { onSignedIn(it.user) } ?: say(login.message ?: "Compte cree, connectez-vous")
    }

    fun logout() = busy {
        engine.stop()
        api.logout()
        _state.value = UiState(booting = false, serverUp = _state.value.serverUp)
    }

    private suspend fun onSignedIn(me: User) {
        _state.update { it.copy(me = me) }
        connectSip()
        refreshAll()
    }

    /** A rappeler a chaque lancement : les identifiants TURN expirent (24 h). */
    fun connectSip() = viewModelScope.launch {
        val sip = api.sipAccount().data ?: return@launch say("Compte SIP indisponible")
        _state.update { it.copy(sip = sip) }
        runCatching { engine.start(sip) }.onFailure { say("SIP : ${it.message}") }
    }

    // ─── Donnees ──────────────────────────────────────────────────────────────

    fun refreshAll() = busy {
        val me = _state.value.me ?: return@busy
        val users = api.users().data.orEmpty().filter { it.id != me.id }
        // On demande le numero SIP de chaque contact : il sert a l'appeler
        // et a afficher son nom quand il nous appelle.
        val contacts = users.map { u -> viewModelScope.async { Contact(u, api.sipPeer(u.id).data?.username) } }.awaitAll()
        _state.update {
            it.copy(
                contacts = contacts.sortedBy { c -> c.user.displayName.lowercase() },
                rooms = api.rooms().data.orEmpty(),
                history = api.history().data.orEmpty(),
            )
        }
    }

    fun refreshHistory() = viewModelScope.launch {
        _state.update { it.copy(history = api.history().data.orEmpty()) }
    }

    /** Nom lisible a partir d'un numero SIP (4821937 -> "Alice"). */
    fun nameOf(sipUsername: String): String = when {
        sipUsername == ECHO_TEST_NUMBER -> "Test d'echo"
        sipUsername.startsWith("8") && sipUsername.length == 5 ->
            _state.value.rooms.firstOrNull { it.roomNumber == sipUsername }?.name ?: "Salle $sipUsername"
        else -> _state.value.contacts.firstOrNull { it.sipUsername == sipUsername }?.user?.displayName ?: sipUsername
    }

    // ─── Appels ───────────────────────────────────────────────────────────────

    fun call(contact: Contact, video: Boolean) = dial(contact.sipUsername, video)

    fun echoTest(video: Boolean) = dial(ECHO_TEST_NUMBER, video)

    fun joinRoom(room: Room, video: Boolean) = dial(room.roomNumber, video)

    private fun dial(number: String?, video: Boolean) = viewModelScope.launch {
        val sip = _state.value.sip
        when {
            !engine.supportsCalls -> say("Les appels sont disponibles sur Android dans cet exemple")
            sip == null -> say("Connexion SIP en cours, reessayez")
            number == null -> say("Numero SIP introuvable pour ce contact")
            else -> engine.dial("sip:$number@${sip.domain}", video)
        }
    }

    fun answer(video: Boolean) = viewModelScope.launch { engine.answer(video) }
    fun hangup() = viewModelScope.launch { engine.hangup(); refreshHistory() }
    fun toggleMute() = viewModelScope.launch { engine.call.value?.let { engine.setMuted(!it.muted) } }
    fun toggleSpeaker() = viewModelScope.launch { engine.call.value?.let { engine.setSpeaker(!it.speaker) } }
    fun toggleCamera() = viewModelScope.launch { engine.call.value?.let { engine.setCamera(!it.cameraOn) } }
    fun dtmf(digit: String) = viewModelScope.launch { engine.sendDtmf(digit) }

    // ─── Conferences ──────────────────────────────────────────────────────────

    fun createRoom(name: String, pin: String?, video: Boolean) = busy {
        val res = api.createRoom(CreateRoomRequest(name.trim(), pin?.takeIf { it.isNotBlank() }, video = video))
        if (res.data == null) return@busy say(res.message ?: "Creation impossible")
        _state.update { it.copy(rooms = api.rooms().data.orEmpty()) }
        say("Salle ${res.data.roomNumber} creee")
    }

    fun deleteRoom(room: Room) = busy {
        api.deleteRoom(room.id)
        _state.update { it.copy(rooms = api.rooms().data.orEmpty()) }
    }

    // ─── Outils ───────────────────────────────────────────────────────────────

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun say(text: String) = _state.update { it.copy(message = text) }

    private fun busy(block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        try { block() } finally { _state.update { it.copy(loading = false) } }
    }
}
