package com.simulatedtez.gochat.chat.remote.models

import com.simulatedtez.gochat.remote.IResponse
import kotlinx.serialization.Serializable
import models.FetchMessagesResponse

@Serializable
data class ChatHistoryResponse(
    override val data: List<Message>? = listOf(),
    override val isSuccessful: Boolean? = false,
    override val message: String?
): FetchMessagesResponse<Message>()
