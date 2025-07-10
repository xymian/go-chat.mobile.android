package com.simulatedtez.gochat.conversations.remote.api_usecases

import com.simulatedtez.gochat.conversations.remote.api_services.IConversationsService
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.RemoteParams
import kotlinx.serialization.Serializable

class StartNewChatUsecase(
    private val conversationsApiService: IConversationsService
): IEndpointCaller<StartNewChatParams, NewChatResponse, IResponse<NewChatResponse>> {

    override suspend fun call(
        params: StartNewChatParams,
        handler: IResponseHandler<NewChatResponse, IResponse<NewChatResponse>>?
    ) {
        handler?.onResponse(conversationsApiService.addNewConversation(params))
    }

}

data class StartNewChatParams(
    override val headers: Headers,
    override val request: Request
): RemoteParams(headers, request) {

    class Headers(
        val accessToken: String
    )

    @Serializable
    class Request(
        val user: String,
        val other: String,
    )
}

