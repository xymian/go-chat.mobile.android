package com.simulatedtez.gochat.auth.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken: String
)