package com.simulatedtez.gochat.chat.remote.api_services

import com.simulatedtez.gochat.chat.remote.api_usecases.AckParams
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesParams
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders

class ChatApiService(private val client: HttpClient): IChatApiService {

    override suspend fun getMissingMessages(params: GetMissingMessagesParams): IResponse<ParentResponse<List<Message>>> {
        return Response<ParentResponse<List<Message>>> {
            client.get("") {
                header(HttpHeaders.Authorization, "Bearer ${params.headers.token}")
            }
        }.invoke()
    }

    override suspend fun acknowledgeMessage(params: AckParams): IResponse<ParentResponse<String>> {
        return Response< ParentResponse<String>> {
            client.post("") {
                header(HttpHeaders.Authorization, "Bearer ${params.headers.token}")
                setBody(params.request)
            }
        }.invoke()
    }
}