package com.p2p.client

import com.p2p.shared.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.WebSocket
import kotlin.js.json

var socket: WebSocket? = null
var peerConnection: dynamic = null
var localStream: dynamic = null
var currentRoomId: String = "sala-1"
val peerId = "peer-" + (1..10000).random()

 fun main() {
    window.onload = {
        val startBtn = document.getElementById("startBtn") as HTMLButtonElement
        val callBtn = document.getElementById("callBtn") as HTMLButtonElement
        val roomIdInput = document.getElementById("roomIdInput") as HTMLInputElement

        startBtn.addEventListener("click", {
            currentRoomId = roomIdInput.value.ifBlank { "sala-1" }

            startLocalStream {
                connectWebSocket(currentRoomId)
                startBtn.style.display = "none"
                callBtn.style.display = "inline-block"
            }
        })

        callBtn.addEventListener("click", {
            createOffer(currentRoomId)
        })
    }
}


fun startLocalStream(onSuccess: () -> Unit) {
    val constraints = json(
        "video" to true,
        "audio" to true
    )

    val mediaDevices = window.navigator.asDynamic().mediaDevices

    mediaDevices.getUserMedia(constraints).then { stream: dynamic ->
        localStream = stream

        val localVideo: dynamic = document.getElementById("localVideo")
        localVideo.srcObject = stream

        initPeerConnection()
        onSuccess()
    }.catch { err: dynamic ->
        println("Erro WebRTC Media: $err")
    }
}

fun initPeerConnection() {
    val rtcConfig = json(
        "iceServers" to arrayOf(
            json(
                "urls" to "stun:stun.l.google.com:19302"
            )
        )
    )

    peerConnection = js(
        "new window.RTCPeerConnection(rtcConfig)"
    )

    if (localStream != null) {
        val tracks = localStream.getTracks()
        val trackCount = tracks.length as Int

        for (i in 0 until trackCount) {
            peerConnection.addTrack(
                tracks[i],
                localStream
            )
        }
    }

    peerConnection.ontrack = { event: dynamic ->
        val remoteVideo: dynamic = document.getElementById("remoteVideo")
        remoteVideo.srcObject = event.streams[0]
    }

    peerConnection.onicecandidate = { event: dynamic ->
        if (event.candidate != null) {
            val cand = event.candidate

            val payload = SignalPayload(
                type = SignalType.CANDIDATE,
                roomId = currentRoomId,
                senderId = peerId,
                candidate = IceCandidateData(
                    candidate = cand.candidate.toString(),
                    sdpMid = cand.sdpMid?.toString(),
                    sdpMLineIndex = (cand.sdpMLineIndex as? Number)?.toInt()
                )
            )

            socket?.send(Json.encodeToString(payload))
        }
    }
}

fun connectWebSocket(roomId: String) {
    val protocol = if (window.location.protocol == "https:") "wss:" else "ws:"
    val host = window.location.host.ifBlank { "localhost:8080" }

    socket = WebSocket("$protocol//$host/rtc/$roomId")

    socket?.onmessage = { event ->
        val text = event.data.toString()
        val payload = Json.decodeFromString<SignalPayload>(text)

        if (payload.senderId != peerId) {
            handleSignalMessage(payload, roomId)
        }
    }
}

fun createOffer(roomId: String) {
    peerConnection.createOffer().then { offer: dynamic ->
        peerConnection.setLocalDescription(offer).then {
            val payload = SignalPayload(
                type = SignalType.OFFER,
                roomId = roomId,
                senderId = peerId,
                sdp = JSON.stringify(offer)
            )
            socket?.send(Json.encodeToString(payload))
        }
    }
}

fun handleSignalMessage(payload: SignalPayload, roomId: String) {
    when (payload.type) {
        SignalType.OFFER -> {
            val offerObj = JSON.parse<dynamic>(payload.sdp ?: "")
            val sessionDesc = js("new window.RTCSessionDescription(offerObj)")

            peerConnection.setRemoteDescription(sessionDesc).then {
                peerConnection.createAnswer().then { answer: dynamic ->
                    peerConnection.setLocalDescription(answer).then {
                        val response = SignalPayload(
                            type = SignalType.ANSWER,
                            roomId = roomId,
                            senderId = peerId,
                            sdp = JSON.stringify(answer)
                        )
                        socket?.send(Json.encodeToString(response))
                    }
                }
            }
        }

        SignalType.ANSWER -> {
            val answerObj = JSON.parse<dynamic>(payload.sdp ?: "")
            val sessionDesc = js("new window.RTCSessionDescription(answerObj)")
            peerConnection.setRemoteDescription(sessionDesc)
        }

        SignalType.CANDIDATE -> {
            payload.candidate?.let {
                val candidateInit = json(
                    "candidate" to it.candidate,
                    "sdpMid" to it.sdpMid,
                    "sdpMLineIndex" to it.sdpMLineIndex
                )
                val iceCandidate = js("new window.RTCIceCandidate(candidateInit)")
                peerConnection.addIceCandidate(iceCandidate)
            }
        }
        else -> Unit
    }
}
