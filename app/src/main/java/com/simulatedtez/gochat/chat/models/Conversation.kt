package com.simulatedtez.gochat.chat.models

import com.simulatedtez.gochat.auth.models.User
import com.simulatedtez.gochat.chat.remote.models.Message

data class Conversation(
    val id: Long,
    val contactName: String,
    val lastMessage: String,
    var timestamp: String,
    var unreadCount: Int,
    val contactAvi: String,
)