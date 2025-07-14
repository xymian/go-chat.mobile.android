package com.simulatedtez.gochat.chat.remote.api_services

import com.simulatedtez.gochat.chat.remote.api_usecases.AckParams
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesParams
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
                header(HttpHeaders.Authorization, "Bearer ${params.headers.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }

    override suspend fun getMissingMessages(params: GetMissingMessagesParams): IResponse<ParentResponse<List<Message>>> {
        return Response<ParentResponse<List<Message>>> {
            client.getWithBaseUrl("/messages/${params.request.chatReference}/${params.request.yourUsername}/unacknowledged") {
                header(HttpHeaders.Authorization, "Bearer ${params.headers.accessToken}")
            }
        }.invoke()
    }

    override suspend fun acknowledgeMessage(params: AckParams): IResponse<ParentResponse<List<Message>>> {
        return Response<ParentResponse<List<Message>>> {
            client.postWithBaseUrl("/messages/acknowledge") {
                header(HttpHeaders.Authorization, "Bearer ${params.headers.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }
}