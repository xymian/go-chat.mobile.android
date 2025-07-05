package com.simulatedtez.gochat.chat.models

data class ChatInfo(
    val username: String,
    val recipientsUsernames: List<String>,
    val chatReference: String,
    val socketURL: String,
)