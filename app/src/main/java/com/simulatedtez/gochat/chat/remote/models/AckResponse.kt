package com.simulatedtez.gochat.chat.remote.models

import kotlinx.serialization.Serializable
import models.ChatResponse

@Serializable
data class AckResponse(
    override val data: String?,
    override val isSuccessful: Boolean?,
    override val message: String?,
): ChatResponse()