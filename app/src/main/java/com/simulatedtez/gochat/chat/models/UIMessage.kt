package com.simulatedtez.gochat.chat.models

import com.simulatedtez.gochat.chat.remote.models.Message

data class UIMessage(
    val message: Message,
    var status: MessageStatus,
)