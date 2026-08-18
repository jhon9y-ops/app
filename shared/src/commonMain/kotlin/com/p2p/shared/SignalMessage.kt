package com.p2p.shared

import kotlinx.serialization.Serializable

@Serializable
enum class SignalType {
    JOIN,
    OFFER,
    ANSWER,
    CANDIDATE,
    PEER_LEFT
}

@Serializable
data class SignalPayload(
    val type: SignalType,
    val roomId: String,
    val senderId: String,
    val sdp: String? = null,
    val candidate: IceCandidateData? = null
)

@Serializable
data class IceCandidateData(
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null
)