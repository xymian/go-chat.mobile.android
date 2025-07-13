package com.simulatedtez.gochat.conversations.models

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val me: String,
    val other: String,
    val chatReference: String,
    val lastMessage: String = "",
    var timestamp: String = "",
    var unreadCount: Int = 0,
    val contactAvi: String =  "",
)