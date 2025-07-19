package com.simulatedtez.gochat.chat.database

import ILocalStorage
import com.simulatedtez.gochat.chat.remote.models.Message

interface IChatStorage: ILocalStorage<Message> {
    suspend fun getMessage(messageRef: String): Message_db?
    suspend fun getPendingMessages(chatRef: String): List<Message_db>
    suspend fun isEmpty(chatRef: String): Boolean
    suspend fun loadNextPage(chatRef: String): List<Message_db>
}