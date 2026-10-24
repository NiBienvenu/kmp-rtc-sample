package org.kmprtc.sample.call

import kotlinx.coroutines.flow.StateFlow
import org.kmprtc.sample.data.SipAccount

/**
 * Le contrat commun du moteur d'appel. L'UI ne connait que cette interface :
 * Android l'implemente avec PJSIP (PjsipCallEngine), les autres cibles avec NoopCallEngine.
 */
interface CallEngine {
    /** false si la plateforme n'a pas encore de moteur SIP (Desktop dans cet exemple). */
    val supportsCalls: Boolean
    val registration: StateFlow<Registration>
    val call: StateFlow<ActiveCall?>

    /** Configure le compte renvoye par GET /sip/account et s'enregistre (REGISTER). */
    suspend fun start(account: SipAccount)
    suspend fun stop()

    suspend fun dial(sipUri: String, video: Boolean)
    suspend fun answer(video: Boolean)
    suspend fun hangup()

    suspend fun setMuted(muted: Boolean)
    suspend fun setSpeaker(on: Boolean)
    suspend fun setCamera(on: Boolean)
    suspend fun sendDtmf(digits: String)
}

enum class Registration { OFFLINE, CONNECTING, ONLINE, FAILED }

enum class CallState { CALLING, RINGING, CONNECTING, CONNECTED, ENDED }

data class ActiveCall(
    val remoteUri: String,
    val incoming: Boolean,
    val state: CallState,
    val video: Boolean,
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val cameraOn: Boolean = video,
    val endReason: String? = null,
)

/** "Alice" <sip:4821937@host> -> 4821937 */
fun sipUser(uri: String): String = uri.substringAfter("sip:").substringBefore("@").substringBefore(">").trim()

expect fun createCallEngine(): CallEngine
