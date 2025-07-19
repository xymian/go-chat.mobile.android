package com.simulatedtez.gochat.chat.remote.models

import com.simulatedtez.gochat.chat.database.Message_db
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.ComparableMessage

@Serializable
open class Message(
    @SerialName("id")
    val id: String,
    @SerialName("messageReference")
    open val messageReference: String,
    @SerialName("textMessage")
    override val message: String,
    @SerialName("senderUsername")
    override val sender: String,
    @SerialName("receiverUsername")
    open val receiverUsername: String,
    @SerialName("messageTimestamp")
    override var timestamp: String,
    @SerialName("chatReference")
    open val chatReference: String,
    @SerialName("seenByReceiver")
    open val seenByReceiver: Boolean? = null,
): ComparableMessage()

fun Message.toMessage_db(): Message_db {
    return Message_db(
        messageReference = messageReference,
        sender = sender,
        receiverUsername = receiverUsername,
        message = message,
        chatReference = chatReference,
        timestamp = timestamp,
        seenByReceiver = seenByReceiver,
        isSent = null
    )
}

fun List<Message>.toMessages_db(): List<Message_db> {
    return map {
        it.toMessage_db()
    }
}