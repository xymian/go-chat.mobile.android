package com.simulatedtez.gochat.chat.remote.models

import kotlinx.serialization.Serializable
import models.FetchMessagesResponse

@Serializable
data class ChatHistoryResponse(
    val data: List<Message>? = listOf(),
    val isSuccessful: Boolean? = false,
    val error: String
): FetchMessagesResponse<Message>(data, isSuccessful, error)
