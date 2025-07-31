package com.simulatedtez.gochat.chat.models

import com.simulatedtez.gochat.chat.remote.models.Message

data class UIMessage(
    val message: Message,
    val status: MessageStatus,
    val isDelivered: Boolean
) {
    var isSent: Boolean = status == MessageStatus.SENT
}