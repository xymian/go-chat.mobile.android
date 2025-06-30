package com.simulatedtez.gochat.chat.remote.api_usecases

import com.simulatedtez.gochat.chat.remote.models.ChatHistoryResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import utils.ChatEndpointCaller
import java.io.IOException

class GetMissingMessagesUsecase: ChatEndpointCaller<Message, ChatHistoryResponse> {

    private val client: OkHttpClient = OkHttpClient()

    override suspend fun call(data: Message?, handler: ChatEndpointCaller.ResponseCallback<ChatHistoryResponse>) {
        client.newCall(Request.Builder().url("").build())
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    handler.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val res = ChatHistoryResponse(
                        data = Json.decodeFromString<List<Message>>(response.body.toString()),
                        isSuccessful = response.isSuccessful,
                        error = response.message,
                    )
                    handler.onResponse(res)
                }
            })
    }
}