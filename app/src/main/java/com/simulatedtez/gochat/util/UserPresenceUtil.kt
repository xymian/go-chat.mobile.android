package com.simulatedtez.gochat.util

import ChatEngine
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.Message
import java.time.LocalDateTime
import java.util.UUID

open class UserPresenceHelper(
    val chatEngine: ChatEngine<Message>?,
    statusToBeSent: PresenceStatus,
    private val chatInfo: ChatInfo?
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
        if (session.canSharePresenceStatus) {
            this.presenceStatus = presenceStatus
            presenceId = UUID.randomUUID().toString()
            postPresence(presenceStatus)
        }
    }

    fun postPresence(presenceStatus: PresenceStatus, chatRef: String) {
        if (session.canSharePresenceStatus) {
            this.presenceStatus = presenceStatus
            val message = Message(
                id = presenceId,
                message = "",
                sender = session.username,
                receiver = chatInfo?.recipientsUsernames[0] ?: "",
                timestamp = LocalDateTime.now().toISOString(),
                chatReference = chatRef,
                presenceStatus = presenceStatus.name
            )
            chatEngine?.sendMessage(message)
        }
    }

    private fun postPresence(presenceStatus: PresenceStatus) {
        if (session.canSharePresenceStatus) {
            this.presenceStatus = presenceStatus
            val message = Message(
                id = presenceId,
                message = "",
                sender = chatInfo?.username ?: session.username,
                receiver = chatInfo?.recipientsUsernames[0] ?: "",
                timestamp = LocalDateTime.now().toISOString(),
                chatReference = chatInfo?.chatReference ?: "",
                presenceStatus = presenceStatus.name
            )
            chatEngine?.sendMessage(message)
        }
    }

    fun onPresenceSent(id: String) {
        presenceIdPair = (id to presenceIdPair.second)
    }
}