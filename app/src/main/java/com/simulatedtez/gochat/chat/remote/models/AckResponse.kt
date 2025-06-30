package com.simulatedtez.gochat.chat.remote.models

import kotlinx.serialization.Serializable
import models.ChatResponse

@Serializable
data class AckResponse(
    val data: String?,
    val isSuccessful: Boolean?,
    val error: String?,
): ChatResponse(data, isSuccessful, error)