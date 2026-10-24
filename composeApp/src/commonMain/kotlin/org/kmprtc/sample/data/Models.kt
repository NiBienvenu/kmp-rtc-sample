package org.kmprtc.sample.data

import kotlinx.serialization.Serializable

// Les DTO reprennent exactement les champs de l'API kmp-rtc-server (voir docs > API REST).

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class RegisterRequest(val displayName: String, val email: String, val phone: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class User(
    val id: String,
    val displayName: String,
    val email: String,
    val phone: String = "",
    val role: String = "user",
    val status: String = "",
)

@Serializable
data class AuthData(val token: String, val user: User)

@Serializable
data class SipAccount(
    val sipUri: String,
    val username: String,
    val password: String,
    val domain: String,
    val transport: String,
    val stun: String? = null,
    val turn: String? = null,
    val turnUsername: String? = null,
    val turnPassword: String? = null,
)

@Serializable
data class SipPeer(val userId: String, val sipUri: String, val username: String, val domain: String)

@Serializable
data class Room(
    val id: String,
    val roomNumber: String,
    val name: String,
    val hasPin: Boolean,
    val pin: String? = null,
    val maxParticipants: Int,
    val video: Boolean,
    val ownerId: String,
    val sipUri: String,
)

@Serializable
data class CreateRoomRequest(val name: String, val pin: String? = null, val maxParticipants: Int = 10, val video: Boolean = true)

@Serializable
data class CallRecord(
    val id: Long,
    val calldate: String,
    val src: String,
    val dst: String,
    val direction: String,   // outgoing | incoming | conference | other
    val duration: Int,
    val billsec: Int,
    val disposition: String, // ANSWERED | NO ANSWER | BUSY | FAILED
)

/** Un utilisateur de l'annuaire + son numero SIP (pour l'appeler et le reconnaitre). */
data class Contact(val user: User, val sipUsername: String?)
