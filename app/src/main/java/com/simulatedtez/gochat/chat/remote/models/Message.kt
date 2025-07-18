package com.simulatedtez.gochat.chat.remote.models

import com.simulatedtez.gochat.chat.database.Message_db
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.ComparableMessage

@Serializable
data class Message(
    @SerialName("id")
    val id: String? = null,
    @SerialName("messageReference")
    val messageReference: String? = null,
    @SerialName("textMessage")
    override val message: String,
    @SerialName("senderUsername")
    override val sender: String,
    @SerialName("receiverUsername")
    val receiverUsername: String,
    @SerialName("messageTimestamp")
    override var timestamp: String,
    @SerialName("chatReference")
    val chatReference: String? = null,
    @SerialName("seenByReceiver")
    val seenByReceiver: Boolean,
): ComparableMessage()

fun Message.toMessage_db(): Message_db {
    return Message_db(
        messageReference = messageReference!!,
        senderUsername = sender,
        receiverUsername = receiverUsername,
        textMessage = message,
        chatReference = chatReference,
        messageTimestamp = timestamp,
        seenByReceiver = seenByReceiver
    )
}

fun List<Message>.toMessages_db(): List<Message_db> {
    return map {
        it.toMessage_db()
    }
}