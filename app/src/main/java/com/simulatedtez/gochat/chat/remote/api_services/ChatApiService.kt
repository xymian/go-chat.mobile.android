package com.simulatedtez.gochat.chat.remote.api_services

import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.getWithBaseUrl
import com.simulatedtez.gochat.remote.postWithBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders

class ChatApiService(private val client: HttpClient): IChatApiService {

    override suspend fun createChatRoom(params: CreateChatRoomParams): IResponse<String> {
        return Response<String> {
            client.postWithBaseUrl("/chat") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }
}