package com.simulatedtez.gochat.chat.view_model.models

data class ChatInfo(
    val username: String,
    val recipientsUsernames: List<String>,
    val chatReference: String
)