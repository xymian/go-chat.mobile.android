package com.simulatedtez.gochat.conversations.models

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val other: String,
    val chatReference: String,
    var lastMessage: String = "",
    var timestamp: String = "",
    var unreadCount: Int = 0,
    val contactAvi: String =  "",
)