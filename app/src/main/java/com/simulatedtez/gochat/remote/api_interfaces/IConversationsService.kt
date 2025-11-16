package com.simulatedtez.gochat.remote.api_interfaces

import com.simulatedtez.gochat.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse

interface IConversationsService {

    suspend fun addNewConversation(params: StartNewChatParams): IResponse<ParentResponse<NewChatResponse>>
}