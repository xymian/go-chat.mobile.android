package com.simulatedtez.gochat.remote.api_services

import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.api_interfaces.IConversationsService
import com.simulatedtez.gochat.remote.postWithBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class ConversationsService(private val client: HttpClient): IConversationsService {
    override suspend fun addNewConversation(params: StartNewChatParams): IResponse<ParentResponse<NewChatResponse>> {
        return Response<ParentResponse<NewChatResponse>> {
            client.postWithBaseUrl("/chatReference") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }
}