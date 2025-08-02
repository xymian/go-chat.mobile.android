package com.simulatedtez.gochat.chat.models

import com.simulatedtez.gochat.chat.remote.models.Message

data class UIMessage(
    val message: Message,
    var status: MessageStatus,
) {
    val isSent: Boolean = status == MessageStatus.SENT || status == MessageStatus.DELIVERED
    val isDelivered: Boolean = !message.deliveredTimestamp.isNullOrEmpty()
}