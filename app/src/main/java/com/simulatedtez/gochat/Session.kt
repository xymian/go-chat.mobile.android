package com.simulatedtez.gochat

import com.simulatedtez.gochat.chat.models.ChatInfo

open class Session private constructor() {

    var username: String = ""
        private set
    var accessToken: String = ""
        private set
    var activeChat: ChatInfo? = null
        private set

    companion object {
        var session = object: Session() {

        }
        private set

        fun clear() {
            session = object: Session() {

            }
        }
    }

    fun setActiveChat(chatInfo: ChatInfo) {
        activeChat = chatInfo
    }

    fun saveAccessToken(token: String) {
        accessToken = token
    }

    fun saveUsername(username: String) {
        this.username = username
    }
}