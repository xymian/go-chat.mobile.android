package com.simulatedtez.gochat.conversations.remote.api_usecases

import com.simulatedtez.gochat.conversations.remote.api_services.IConversationsService
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import kotlinx.serialization.Serializable

class AddNewChatUsecase(
    private val conversationsApiService: IConversationsService
): IEndpointCaller<StartNewChatParams, ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {

    override suspend fun call(
        params: StartNewChatParams,
        handler: IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>>?
    ) {
        handler?.onResponse(conversationsApiService.addNewConversation(params))
    }

}

data class StartNewChatParams(
    override val request: Request
): RemoteParams(request = request) {

    @Serializable
    class Request(
        val user: String,
        val other: String,
    )
}

