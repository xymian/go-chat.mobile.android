package com.simulatedtez.gochat.utils

import ChatEngine
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.remote.models.Message
import java.time.LocalDateTime
import java.util.UUID

open class UserPresenceHelper(
    private val chatEngine: ChatEngine<Message>?, statusToBeSent: PresenceStatus
) {

    var presenceStatus = statusToBeSent
        private set

    var presenceIdPair: Pair<String?, String?> = null to null
    var presenceId = UUID.randomUUID().toString()

    fun handlePresenceMessage(
        receivedPresence: PresenceStatus,
        messageId: String,
        chatRef: String,
        onResolve: (status: PresenceStatus) -> Unit
    ) {
        if (presenceIdPair.second != messageId) {
            postPresence(presenceStatus, chatRef)
        }
        presenceIdPair = (presenceIdPair.first to messageId)
        onResolve(receivedPresence)
    }

    fun postNewUserPresence(presenceStatus: PresenceStatus) {
        this.presenceStatus = presenceStatus
        presenceId = UUID.randomUUID().toString()
        session.lastActiveChat?.let {
            postPresence(presenceStatus, it.chatReference)
        }
    }

    fun postPresence(presenceStatus: PresenceStatus, chatRef: String) {
        this.presenceStatus = presenceStatus
        val message = Message(
            id = presenceId,
            message = "",
            sender = session.username,
            receiver = "",
            timestamp = LocalDateTime.now().toISOString(),
            chatReference = chatRef,
            ack = false,
            presenceStatus = presenceStatus.name
        )
        chatEngine?.sendMessage(message)
    }

    fun onPresenceSent(id: String) {
        presenceIdPair = (id to presenceIdPair.second)
    }
}