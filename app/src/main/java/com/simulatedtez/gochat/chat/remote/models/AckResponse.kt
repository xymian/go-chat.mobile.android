package com.simulatedtez.gochat.chat.remote.models

import kotlinx.serialization.Serializable
import models.MessagesResponse

@Serializable
data class AckResponse(
    override val data: List<Message>?,
    override val isSuccessful: Boolean?,
    override val message: String?,
): MessagesResponse<Message>()