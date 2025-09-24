package com.simulatedtez.gochat.chat.remote.models

import com.simulatedtez.gochat.chat.database.DBMessage
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.UIMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.ComparableMessage

@Serializable
open class Message(
    @SerialName("messageReference") override val id: String,
    @SerialName("textMessage") override val message: String,
    @SerialName("senderUsername") override val sender: String,
    @SerialName("receiverUsername") override val receiver: String,
    @SerialName("messageTimestamp") override var timestamp: String,
    @SerialName("chatReference") open val chatReference: String,
    @SerialName("ack") open val ack: Boolean? = null,
    @SerialName("deliveredTimestamp") open var deliveredTimestamp: String? = null,
    @SerialName("seenTimestamp") open var seenTimestamp: String? = null,
    @SerialName("messageStatus") open val messageStatus: String? = null,
    @SerialName("presenceStatus") open val presenceStatus: String? = null
): ComparableMessage()

fun Message.toUIMessage(isSent: Boolean): UIMessage {
    return UIMessage(
        message = this,
        status = when {
            !seenTimestamp.isNullOrEmpty() -> MessageStatus.SEEN
            !deliveredTimestamp.isNullOrEmpty() -> MessageStatus.DELIVERED
            else -> {
                if (isSent) MessageStatus.SENT
                else MessageStatus.SENDING
            }
        }
    )
}

fun Message.toDBMessage(): DBMessage {
    return DBMessage(
        id = id,
        sender = sender,
        receiver = receiver,
        message = message,
        chatReference = chatReference,
        timestamp = timestamp,
        ack = ack,
        deliveredTimestamp = deliveredTimestamp,
        seenTimestamp = seenTimestamp,
        isSent = null
    )
}

fun List<Message>.toDBMessages(): List<DBMessage> {
    return map {
        it.toDBMessage()
    }
}