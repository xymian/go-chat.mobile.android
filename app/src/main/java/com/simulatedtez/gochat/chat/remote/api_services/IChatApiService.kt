package com.simulatedtez.gochat.chat.remote.api_services

import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.remote.IResponse

interface IChatApiService {
    suspend fun createChatRoom(params: CreateChatRoomParams): IResponse<String>
}