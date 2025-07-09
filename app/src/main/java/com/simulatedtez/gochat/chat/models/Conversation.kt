package com.simulatedtez.gochat.chat.models

data class Conversation(
    val id: Long,
    val contactName: String,
    val lastMessage: String,
    var timestamp: String,
    var unreadCount: Int,
    val contactAvi: String,
)