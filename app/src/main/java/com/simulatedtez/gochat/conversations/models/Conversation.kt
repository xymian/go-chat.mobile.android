package com.simulatedtez.gochat.conversations.models

data class Conversation(
    val id: Long,
    val me: String,
    val other: String,
    val chatReference: String,
    val lastMessage: String = "",
    var timestamp: String = "",
    var unreadCount: Int = 0,
    val contactAvi: String =  "",
)