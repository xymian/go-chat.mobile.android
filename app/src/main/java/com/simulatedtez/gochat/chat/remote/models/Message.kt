package com.simulatedtez.gochat.chat.remote.models

import com.simulatedtez.gochat.chat.database.Message_db
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.Message

@Serializable
data class Message(
    @SerialName("id")
    val id: String? = null,
    @SerialName("messageReference")
    val messageReference: String? = null,
    @SerialName("textMessage")
    val textMessage: String? = null,
    @SerialName("senderUsername")
    val senderUsername: String? = null,
    @SerialName("receiverUsername")
    val receiverUsername: String? = null,
    @SerialName("messageTimestamp")
    val messageTimestamp: String? = null,
    @SerialName("chatReference")
    val chatReference: String? = null,
    @SerialName("seenByReceiver")
    val seenByReceiver: Boolean,
): Message(_timestamp = messageTimestamp, _messageId = messageReference,
    _sender = senderUsername, _receiver = receiverUsername, _message = textMessage)

fun com.simulatedtez.gochat.chat.remote.models.Message.toMessage_db(): Message_db {
    return Message_db(
        messageReference = messageReference,
        senderUsername = senderUsername,
        receiverUsername = receiverUsername,
        textMessage = textMessage,
        chatReference = chatReference,
        messageTimestamp = messageTimestamp,
        seenByReceiver = seenByReceiver
    )
}

fun List<com.simulatedtez.gochat.chat.remote.models.Message>.toMessages_db(): List<Message_db> {
    return map {
        it.toMessage_db()
    }
}