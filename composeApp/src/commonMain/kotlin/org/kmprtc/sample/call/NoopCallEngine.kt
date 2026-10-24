package org.kmprtc.sample.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kmprtc.sample.data.SipAccount

/** Moteur vide : l'UI, les comptes, les salles et l'historique marchent, mais pas le son. */
class NoopCallEngine : CallEngine {
    override val supportsCalls = false
    override val registration = MutableStateFlow(Registration.OFFLINE).asStateFlow()
    override val call = MutableStateFlow<ActiveCall?>(null).asStateFlow()

    override suspend fun start(account: SipAccount) = Unit
    override suspend fun stop() = Unit
    override suspend fun dial(sipUri: String, video: Boolean) = Unit
    override suspend fun answer(video: Boolean) = Unit
    override suspend fun hangup() = Unit
    override suspend fun setMuted(muted: Boolean) = Unit
    override suspend fun setSpeaker(on: Boolean) = Unit
    override suspend fun setCamera(on: Boolean) = Unit
    override suspend fun sendDtmf(digits: String) = Unit
}
