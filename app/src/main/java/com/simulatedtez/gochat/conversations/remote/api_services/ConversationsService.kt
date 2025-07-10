package com.simulatedtez.gochat.conversations.remote.api_services

import androidx.compose.ui.autofill.ContentType
import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.postWithBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class ConversationsService(private val client: HttpClient): IConversationsService {
    override suspend fun addNewConversation(params: StartNewChatParams): IResponse<NewChatResponse> {
        return Response<NewChatResponse> {
            client.postWithBaseUrl("/chatReference") {
                contentType(io.ktor.http.ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${params.headers.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }
}