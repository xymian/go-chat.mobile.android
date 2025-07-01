package com.simulatedtez.gochat.chat.remote.models

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
